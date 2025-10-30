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
            case "elective":
                return analyzeElective(takenCourses);
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

        Log.d(TAG, "  Analyzing list-type category: " + name);
        Log.d(TAG, "  Requirements in this category: " + courses.size() + " courses");
        for (CourseRequirement req : courses) {
            Log.d(TAG, "    - Required: " + req.getName() + " (" + req.getCredits() + "학점)" +
                  (req.isMandatory() ? " [MANDATORY]" : ""));
        }

        // 수강한 과목 이름 맵 생성 (빠른 조회)
        Map<String, CourseInputActivity.Course> takenCoursesMap = new HashMap<>();
        for (CourseInputActivity.Course course : takenCourses) {
            takenCoursesMap.put(course.getName(), course);
        }

        Log.d(TAG, "  Taken courses available for matching: " + takenCoursesMap.size());
        for (String courseName : takenCoursesMap.keySet()) {
            CourseInputActivity.Course course = takenCoursesMap.get(courseName);
            Log.d(TAG, "    - Taken: [" + course.getCategory() + "] " + courseName);
        }

        int earnedCredits = 0;
        int earnedCourses = 0;

        // 각 과목 체크
        for (CourseRequirement req : courses) {
            // 과목의 학점 정보를 courseCreditsMap에 추가
            result.addCourseCredit(req.getName(), req.getCredits());

            if (takenCoursesMap.containsKey(req.getName())) {
                // 수강함
                earnedCredits += req.getCredits();
                earnedCourses++;
                result.addCompletedCourse(req.getName());
                Log.d(TAG, "  ✓ Completed: " + req.getName() + " (" + req.getCredits() + "학점)");
            } else {
                // 수강 안 함 - 세부 탭 표시를 위해 모든 미이수 과목 추가
                result.addMissingCourse(req.getName());
                if (req.isMandatory()) {
                    Log.d(TAG, "  ✗ Missing (mandatory): " + req.getName());
                } else {
                    Log.d(TAG, "  - Missing (optional): " + req.getName());
                }
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
        Log.d(TAG, "  >>> analyzeOneOf 시작: " + name + " (id: " + id + ")");
        Log.d(TAG, "      oneOf 그룹 내 과목 수: " + courses.size());
        for (CourseRequirement req : courses) {
            Log.d(TAG, "        - " + req.getName() + " (" + req.getCredits() + "학점)");
        }

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

        // oneOf는 서브그룹 결과에 선택된 과목과 선택 가능한 모든 과목 저장
        CategoryAnalysisResult.SubgroupResult subResult = new CategoryAnalysisResult.SubgroupResult(id, name);
        subResult.setEarnedCredits(earnedCredits);
        subResult.setRequiredCredits(required);
        subResult.setCompleted(result.isCompleted());
        if (selectedCourse != null) {
            subResult.setSelectedCourse(selectedCourse);
        }

        // oneOf 그룹의 선택 가능한 모든 과목 이름을 저장하고 학점 정보도 추가
        List<String> availableCourseNames = new ArrayList<>();
        for (CourseRequirement req : courses) {
            availableCourseNames.add(req.getName());
            // 과목의 학점 정보를 courseCreditsMap에 추가
            result.addCourseCredit(req.getName(), req.getCredits());
        }
        subResult.setAvailableCourses(availableCourseNames);

        Log.d(TAG, "      availableCourses 설정: " + availableCourseNames.size() + "개 과목");
        for (String courseName : availableCourseNames) {
            Log.d(TAG, "        * " + courseName);
        }
        Log.d(TAG, "      SubgroupResult에 추가 (groupId: " + id + ", name: " + name + ")");

        result.addSubgroupResult(subResult);

        return result;
    }

    /**
     * group 타입 분석: 하위 서브그룹들을 재귀적으로 분석
     */
    private CategoryAnalysisResult analyzeGroup(List<CourseInputActivity.Course> takenCourses) {
        Log.d(TAG, "  >>> analyzeGroup 시작: " + name + " (id: " + id + ")");

        CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);
        result.setRequiredCredits(required);

        int totalEarnedCredits = 0;
        int totalEarnedCourses = 0;
        boolean allSubgroupsCompleted = true;

        Log.d(TAG, "      서브그룹 개수: " + subgroups.size());

        // 각 서브그룹 재귀 분석
        for (RequirementCategory subgroup : subgroups) {
            Log.d(TAG, "      → 서브그룹 분석: " + subgroup.getName() + " (type: " + subgroup.getType() + ")");
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

            // 서브그룹의 학점 정보를 병합
            if (subResult.getCourseCreditsMap() != null) {
                for (Map.Entry<String, Integer> entry : subResult.getCourseCreditsMap().entrySet()) {
                    result.addCourseCredit(entry.getKey(), entry.getValue());
                }
            }
        }

        result.setEarnedCredits(totalEarnedCredits);
        result.setEarnedCourses(totalEarnedCourses);

        // 완료 여부: 모든 서브그룹 완료 AND 총 학점 충족
        result.setCompleted(allSubgroupsCompleted && totalEarnedCredits >= required);

        Log.d(TAG, "  Group result: " + totalEarnedCredits + "/" + required + " credits, completed=" + result.isCompleted());

        return result;
    }

    /**
     * elective 타입 분석: 교양선택, 소양, 자율선택 등
     * 과목 목록이 없고, 사용자가 해당 카테고리로 입력한 모든 과목의 학점을 합산
     */
    private CategoryAnalysisResult analyzeElective(List<CourseInputActivity.Course> takenCourses) {
        CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);
        result.setRequiredCredits(required);

        Log.d(TAG, "  Analyzing elective-type category: " + name);
        Log.d(TAG, "  Required credits: " + required);
        Log.d(TAG, "  Total taken courses to check: " + takenCourses.size());

        int earnedCredits = 0;
        int earnedCourses = 0;
        int checkedCourses = 0;

        // 사용자가 이 카테고리로 입력한 모든 과목의 학점 합산
        for (CourseInputActivity.Course course : takenCourses) {
            checkedCourses++;
            Log.d(TAG, "    Checking course #" + checkedCourses + ": [" + course.getCategory() +
                  "] " + course.getName() + " (" + course.getCredits() + "학점)" +
                  " - matches '" + name + "'? " + name.equals(course.getCategory()));

            if (name.equals(course.getCategory())) {
                earnedCredits += course.getCredits();
                earnedCourses++;
                result.addCompletedCourse(course.getName());
                result.addCourseCredit(course.getName(), course.getCredits());
                Log.d(TAG, "  ✓ COUNTED: " + course.getName() + " (" + course.getCredits() + "학점)");
            }
        }

        result.setEarnedCredits(earnedCredits);
        result.setEarnedCourses(earnedCourses);
        result.setCompleted(earnedCredits >= required);

        Log.d(TAG, "  Result: " + earnedCredits + "/" + required + " credits, " +
              earnedCourses + " courses, completed=" + result.isCompleted());

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
        Log.d(TAG, "  >>> convertToSubgroupResult: " + categoryResult.getCategoryName() + " (id: " + categoryResult.getCategoryId() + ")");

        CategoryAnalysisResult.SubgroupResult subResult =
            new CategoryAnalysisResult.SubgroupResult(categoryResult.getCategoryId(), categoryResult.getCategoryName());

        subResult.setEarnedCredits(categoryResult.getEarnedCredits());
        subResult.setRequiredCredits(categoryResult.getRequiredCredits());
        subResult.setCompleted(categoryResult.isCompleted());
        subResult.setCompletedCourses(categoryResult.getCompletedCourses());

        // oneOf 타입의 경우 availableCourses와 selectedCourse 정보 복사
        if (categoryResult.getSubgroupResults() != null && !categoryResult.getSubgroupResults().isEmpty()) {
            Log.d(TAG, "      서브그룹 결과 개수: " + categoryResult.getSubgroupResults().size());
            // oneOf 타입은 첫 번째 서브그룹에 availableCourses와 selectedCourse가 저장되어 있음
            CategoryAnalysisResult.SubgroupResult firstSubgroup = categoryResult.getSubgroupResults().get(0);
            if (firstSubgroup.getAvailableCourses() != null) {
                subResult.setAvailableCourses(firstSubgroup.getAvailableCourses());
                Log.d(TAG, "      ✓ availableCourses 복사: " +
                      firstSubgroup.getAvailableCourses().size() + " courses for " + categoryResult.getCategoryName());
                for (String courseName : firstSubgroup.getAvailableCourses()) {
                    Log.d(TAG, "          * " + courseName);
                }
            } else {
                Log.d(TAG, "      ✗ firstSubgroup.availableCourses is NULL");
            }
            if (firstSubgroup.getSelectedCourse() != null) {
                subResult.setSelectedCourse(firstSubgroup.getSelectedCourse());
                Log.d(TAG, "      ✓ selectedCourse 복사: " + firstSubgroup.getSelectedCourse());
            }
        } else {
            Log.d(TAG, "      서브그룹 결과 없음 (빈 리스트 또는 null)");
        }

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
