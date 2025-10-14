package sprout.app.sakmvp1.models;

import android.util.Log;
import sprout.app.sakmvp1.CourseInputActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 졸업요건 카테고리 (전공필수, 교양필수 등)
 * type에 따라 다른 분석 로직 적용
 */
public class RequirementCategory {
    private static final String TAG = "RequirementCategory";

    private String id;
    private String name;
    private String displayName;
    private String type;  // "list", "oneOf", "group", "competency"
    private int required;
    private String requiredType;  // "credits", "courses", "any"

    private List<CourseRequirement> courses;
    private List<RequirementCategory> subgroups;
    private List<String> competencies;

    // Firestore 역직렬화를 위한 빈 생성자
    public RequirementCategory() {
        this.courses = new ArrayList<>();
        this.subgroups = new ArrayList<>();
        this.competencies = new ArrayList<>();
    }

    public RequirementCategory(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.displayName = name;
        this.type = type;
        this.courses = new ArrayList<>();
        this.subgroups = new ArrayList<>();
        this.competencies = new ArrayList<>();
    }

    /**
     * 수강한 과목을 기반으로 이 카테고리의 충족 여부 분석
     * @param takenCourses 사용자가 수강한 과목 목록
     * @return 분석 결과
     */
    public CategoryAnalysisResult analyze(List<CourseInputActivity.Course> takenCourses) {
        if (type == null) {
            Log.w(TAG, "Category type is null for: " + name);
            return new CategoryAnalysisResult(id, name);
        }

        Log.d(TAG, "Analyzing category: " + name + " (type: " + type + ")");

        switch (type) {
            case "list":
                return analyzeList(takenCourses);
            case "oneOf":
                return analyzeOneOf(takenCourses);
            case "group":
                return analyzeGroup(takenCourses);
            case "competency":
                return analyzeCompetency(takenCourses);
            default:
                Log.w(TAG, "Unknown category type: " + type + " for " + name);
                return new CategoryAnalysisResult(id, name);
        }
    }

    /**
     * list 타입 분석: 모든 과목을 체크, mandatory=true는 필수
     */
    private CategoryAnalysisResult analyzeList(List<CourseInputActivity.Course> takenCourses) {
        CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);
        result.setRequiredCredits(required);

        // 수강한 과목 이름 맵 생성 (빠른 조회)
        Map<String, CourseInputActivity.Course> takenCoursesMap = new HashMap<>();
        for (CourseInputActivity.Course course : takenCourses) {
            takenCoursesMap.put(course.getName(), course);
        }

        int earnedCredits = 0;
        int earnedCourses = 0;

        // 각 과목 체크
        for (CourseRequirement req : courses) {
            if (takenCoursesMap.containsKey(req.getName())) {
                // 수강함
                earnedCredits += req.getCredits();
                earnedCourses++;
                result.addCompletedCourse(req.getName());
                Log.d(TAG, "  ✓ Completed: " + req.getName() + " (" + req.getCredits() + "학점)");
            } else if (req.isMandatory()) {
                // 필수인데 수강 안 함
                result.addMissingCourse(req.getName());
                Log.d(TAG, "  ✗ Missing (mandatory): " + req.getName());
            }
        }

        result.setEarnedCredits(earnedCredits);
        result.setEarnedCourses(earnedCourses);

        // 완료 여부 계산
        if ("courses".equals(requiredType)) {
            result.setRequiredCourses(required);
            result.setCompleted(earnedCourses >= required);
        } else {
            result.setCompleted(earnedCredits >= required);
        }

        Log.d(TAG, "  Result: " + earnedCredits + "/" + required + " credits, completed=" + result.isCompleted());

        return result;
    }

    /**
     * oneOf 타입 분석: 목록 중 하나만 수강하면 충족
     */
    private CategoryAnalysisResult analyzeOneOf(List<CourseInputActivity.Course> takenCourses) {
        CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);
        result.setRequiredCredits(required);

        // 수강한 과목 이름 리스트
        List<String> takenCourseNames = new ArrayList<>();
        for (CourseInputActivity.Course course : takenCourses) {
            takenCourseNames.add(course.getName());
        }

        // 하나라도 수강했는지 확인
        boolean foundAny = false;
        int earnedCredits = 0;
        String selectedCourse = null;

        for (CourseRequirement req : courses) {
            if (takenCourseNames.contains(req.getName())) {
                foundAny = true;
                earnedCredits = req.getCredits();
                selectedCourse = req.getName();
                result.addCompletedCourse(req.getName());
                Log.d(TAG, "  ✓ Selected from oneOf: " + req.getName());
                break;  // 하나만 찾으면 됨
            }
        }

        result.setEarnedCredits(earnedCredits);
        result.setEarnedCourses(foundAny ? 1 : 0);

        // 완료 여부
        if ("courses".equals(requiredType)) {
            result.setRequiredCourses(required);
            result.setCompleted(foundAny && result.getEarnedCourses() >= required);
        } else {
            result.setCompleted(earnedCredits >= required);
        }

        if (!foundAny) {
            // 하나도 수강 안 함 - 모든 과목을 미이수로 표시
            for (CourseRequirement req : courses) {
                result.addMissingCourse(req.getName());
            }
            Log.d(TAG, "  ✗ None selected from oneOf group");
        }

        // oneOf는 서브그룹 결과에 선택된 과목 저장
        CategoryAnalysisResult.SubgroupResult subResult = new CategoryAnalysisResult.SubgroupResult(id, name);
        subResult.setEarnedCredits(earnedCredits);
        subResult.setRequiredCredits(required);
        subResult.setCompleted(result.isCompleted());
        if (selectedCourse != null) {
            subResult.setSelectedCourse(selectedCourse);
        }
        result.addSubgroupResult(subResult);

        return result;
    }

    /**
     * group 타입 분석: 하위 서브그룹들을 재귀적으로 분석
     */
    private CategoryAnalysisResult analyzeGroup(List<CourseInputActivity.Course> takenCourses) {
        CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);
        result.setRequiredCredits(required);

        int totalEarnedCredits = 0;
        int totalEarnedCourses = 0;
        boolean allSubgroupsCompleted = true;

        Log.d(TAG, "  Analyzing " + subgroups.size() + " subgroups...");

        // 각 서브그룹 재귀 분석
        for (RequirementCategory subgroup : subgroups) {
            CategoryAnalysisResult subResult = subgroup.analyze(takenCourses);
            result.addSubgroupResult(convertToSubgroupResult(subResult));

            totalEarnedCredits += subResult.getEarnedCredits();
            totalEarnedCourses += subResult.getEarnedCourses();

            if (!subResult.isCompleted()) {
                allSubgroupsCompleted = false;
            }

            // 완료/미완료 과목 병합
            result.getCompletedCourses().addAll(subResult.getCompletedCourses());
            result.getMissingCourses().addAll(subResult.getMissingCourses());
        }

        result.setEarnedCredits(totalEarnedCredits);
        result.setEarnedCourses(totalEarnedCourses);

        // 완료 여부: 모든 서브그룹 완료 AND 총 학점 충족
        result.setCompleted(allSubgroupsCompleted && totalEarnedCredits >= required);

        Log.d(TAG, "  Group result: " + totalEarnedCredits + "/" + required + " credits, completed=" + result.isCompleted());

        return result;
    }

    /**
     * competency 타입 분석: 역량 기반 선택 (교양선택)
     */
    private CategoryAnalysisResult analyzeCompetency(List<CourseInputActivity.Course> takenCourses) {
        CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);
        result.setRequiredCredits(required);

        // 역량별 교양선택은 사용자가 자유롭게 선택
        // 단순히 해당 카테고리 과목의 총 학점만 계산
        int earnedCredits = 0;
        int earnedCourses = 0;

        for (CourseInputActivity.Course course : takenCourses) {
            // 카테고리가 일치하는 과목 찾기
            if (name.equals(course.getCategory())) {
                earnedCredits += course.getCredits();
                earnedCourses++;
                result.addCompletedCourse(course.getName());
            }
        }

        result.setEarnedCredits(earnedCredits);
        result.setEarnedCourses(earnedCourses);
        result.setCompleted(earnedCredits >= required);

        Log.d(TAG, "  Competency result: " + earnedCredits + "/" + required + " credits");

        return result;
    }

    /**
     * CategoryAnalysisResult를 SubgroupResult로 변환
     */
    private CategoryAnalysisResult.SubgroupResult convertToSubgroupResult(CategoryAnalysisResult categoryResult) {
        CategoryAnalysisResult.SubgroupResult subResult =
            new CategoryAnalysisResult.SubgroupResult(categoryResult.getCategoryId(), categoryResult.getCategoryName());

        subResult.setEarnedCredits(categoryResult.getEarnedCredits());
        subResult.setRequiredCredits(categoryResult.getRequiredCredits());
        subResult.setCompleted(categoryResult.isCompleted());
        subResult.setCompletedCourses(categoryResult.getCompletedCourses());

        return subResult;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRequired() {
        return required;
    }

    public void setRequired(int required) {
        this.required = required;
    }

    public String getRequiredType() {
        return requiredType;
    }

    public void setRequiredType(String requiredType) {
        this.requiredType = requiredType;
    }

    public List<CourseRequirement> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseRequirement> courses) {
        this.courses = courses;
    }

    public List<RequirementCategory> getSubgroups() {
        return subgroups;
    }

    public void setSubgroups(List<RequirementCategory> subgroups) {
        this.subgroups = subgroups;
    }

    public List<String> getCompetencies() {
        return competencies;
    }

    public void setCompetencies(List<String> competencies) {
        this.competencies = competencies;
    }

    @Override
    public String toString() {
        return displayName + " (" + type + ", " + required + requiredType + ")";
    }
}
