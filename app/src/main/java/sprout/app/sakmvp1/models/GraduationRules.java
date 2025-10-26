package sprout.app.sakmvp1.models;

import android.util.Log;
import com.google.firebase.Timestamp;
import sprout.app.sakmvp1.CourseInputActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 통합 졸업요건 규칙
 * 특정 학번/학과/트랙의 모든 졸업요건을 포함
 * Firestore graduation_requirements 컬렉션과 매핑
 */
public class GraduationRules {
    private static final String TAG = "GraduationRules";

    private String docId;
    private long cohort;  // 학번은 숫자 타입 (Firestore에 Long으로 저장됨)
    private String department;
    private String track;
    private String version;
    private Timestamp updatedAt;
    private String sourceDocumentName;
    private int totalCredits;  // Firestore의 totalCredits 필드

    private CreditRequirements creditRequirements;
    private String overflowDestination;
    private List<RequirementCategory> categories;
    private List<ReplacementRule> replacementRules;

    // Firestore 역직렬화를 위한 빈 생성자
    public GraduationRules() {
        this.categories = new ArrayList<>();
        this.replacementRules = new ArrayList<>();
    }

    public GraduationRules(long cohort, String department, String track) {
        this.cohort = cohort;
        this.department = department;
        this.track = track;
        this.docId = cohort + "_" + department + "_" + track;
        this.categories = new ArrayList<>();
        this.replacementRules = new ArrayList<>();
    }

    /**
     * 졸업요건 분석 (메인 메서드)
     * @param takenCourses 사용자가 수강한 과목 목록
     * @return 졸업요건 분석 결과
     */
    public GraduationAnalysisResult analyze(List<CourseInputActivity.Course> takenCourses) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "Starting graduation analysis for: " + docId);
        Log.d(TAG, "Taken courses: " + takenCourses.size());

        // Log each input course with details
        for (int i = 0; i < takenCourses.size(); i++) {
            CourseInputActivity.Course course = takenCourses.get(i);
            Log.d(TAG, "  Input course #" + (i+1) + ": [" + course.getCategory() + "] " +
                  course.getName() + " (" + course.getCredits() + "학점)");
        }

        Log.d(TAG, "========================================");

        GraduationAnalysisResult result = new GraduationAnalysisResult();
        result.setDocId(docId);
        result.setCohort(String.valueOf(cohort));  // long을 String으로 변환
        result.setDepartment(department);
        result.setTrack(track);

        // 1. 대체과목 적용
        List<CourseInputActivity.Course> adjustedCourses = applyReplacementRules(takenCourses);

        // 2. 각 카테고리 분석
        Map<String, CategoryAnalysisResult> categoryResults = new HashMap<>();
        for (RequirementCategory category : categories) {
            Log.d(TAG, "Analyzing category: " + category.getName() + " (id=" + category.getId() +
                  ", type=" + category.getType() + ", required=" + category.getRequired() + ")");

            CategoryAnalysisResult categoryResult = category.analyze(adjustedCourses);
            categoryResults.put(category.getId(), categoryResult);
            result.addCategoryResult(categoryResult);

            Log.d(TAG, "Category analyzed: " + category.getName() + " -> " +
                  categoryResult.getEarnedCredits() + "/" + categoryResult.getRequiredCredits() +
                  " (" + (categoryResult.isCompleted() ? "✓" : "✗") + ")" +
                  " [completed: " + categoryResult.getCompletedCourses().size() +
                  ", missing: " + categoryResult.getMissingCourses().size() + "]");
        }

        // 3. 총 학점 계산
        int totalEarnedCredits = 0;
        Log.d(TAG, "Calculating total earned credits:");
        for (CategoryAnalysisResult categoryResult : categoryResults.values()) {
            int credits = categoryResult.getEarnedCredits();
            totalEarnedCredits += credits;
            Log.d(TAG, "  + " + categoryResult.getCategoryName() + ": " + credits + "학점");
        }
        Log.d(TAG, "Total earned credits (before overflow): " + totalEarnedCredits);
        result.setTotalEarnedCredits(totalEarnedCredits);

        // totalCredits 필드 우선 사용 (Firestore 문서의 totalCredits)
        if (totalCredits > 0) {
            result.setTotalRequiredCredits(totalCredits);
            Log.d(TAG, "Using totalCredits from document: " + totalCredits);
        } else if (creditRequirements != null && creditRequirements.getTotal() > 0) {
            // totalCredits가 없으면 creditRequirements.total 사용 (하위 호환)
            result.setTotalRequiredCredits(creditRequirements.getTotal());
            Log.d(TAG, "Using creditRequirements.total: " + creditRequirements.getTotal());
        } else {
            Log.w(TAG, "No total credits found! totalCredits=" + totalCredits +
                  ", creditRequirements=" + (creditRequirements != null ? creditRequirements.toString() : "null"));
        }

        // 4. 넘치는 학점 처리
        handleOverflowCredits(result, categoryResults);

        // 5. 졸업 가능 여부 계산
        result.calculateGraduationReadiness();

        Log.d(TAG, "========================================");
        Log.d(TAG, "Analysis complete: Total " + result.getTotalEarnedCredits() + "/" +
              result.getTotalRequiredCredits() + " credits");
        Log.d(TAG, "Graduation ready: " + result.isGraduationReady());
        Log.d(TAG, "========================================");

        return result;
    }

    /**
     * 대체과목 규칙 적용
     * 폐강된 과목을 대체 과목으로 인정
     *
     * 중복 인정 방지:
     * - 같은 폐강 과목의 대체과목을 여러 개 수강해도, 첫 번째만 대체로 인정
     * - 나머지 대체과목은 원래 카테고리 그대로 유지 (예: 전공선택 → 전공선택)
     *
     * 적용 범위:
     * - scope가 "document"이면 해당 문서(학번/학과/트랙)에만 적용
     * - scope가 "department"이면 같은 학부의 모든 문서에 적용
     */
    private List<CourseInputActivity.Course> applyReplacementRules(List<CourseInputActivity.Course> takenCourses) {
        if (replacementRules == null || replacementRules.isEmpty()) {
            Log.d(TAG, "No replacement rules to apply");
            return takenCourses;
        }

        List<CourseInputActivity.Course> adjustedCourses = new ArrayList<>(takenCourses);
        List<String> takenCourseNames = new ArrayList<>();
        for (CourseInputActivity.Course course : takenCourses) {
            takenCourseNames.add(course.getName());
        }

        Log.d(TAG, "========================================");
        Log.d(TAG, "Applying " + replacementRules.size() + " replacement rules...");
        Log.d(TAG, "Current document: " + docId + " (department: " + department + ")");

        for (ReplacementRule rule : replacementRules) {
            // 적용 범위 체크
            String scope = rule.getScope() != null ? rule.getScope() : "document";
            boolean shouldApply = false;

            if ("document".equals(scope)) {
                // 해당 문서에만 적용
                shouldApply = true;
                Log.d(TAG, "Rule scope: document (applies to this document)");
            } else if ("department".equals(scope)) {
                // 학부 전체에 적용 (department가 같으면 적용)
                shouldApply = true; // 현재 문서의 규칙이므로 department는 자동으로 같음
                Log.d(TAG, "Rule scope: department (applies to entire department: " + department + ")");
            }

            if (!shouldApply) {
                Log.d(TAG, "Skipping rule due to scope mismatch");
                continue;
            }

            if (rule.canApply(takenCourseNames)) {
                // 대체과목 적용 가능
                ReplacementRule.CourseInfo discontinuedCourse = rule.getDiscontinuedCourse();
                String takenReplacement = rule.getTakenReplacementCourse(takenCourseNames);
                List<String> allTakenReplacements = rule.getAllTakenReplacementCourses(takenCourseNames);

                if (discontinuedCourse != null && takenReplacement != null) {
                    // 폐강된 과목을 가상으로 추가
                    CourseInputActivity.Course virtualCourse = new CourseInputActivity.Course(
                        discontinuedCourse.getCategory(),
                        discontinuedCourse.getName(),
                        discontinuedCourse.getCredits()
                    );
                    adjustedCourses.add(virtualCourse);

                    Log.d(TAG, "✓ Replacement applied:");
                    Log.d(TAG, "  Discontinued: " + discontinuedCourse.getName() + " (" + discontinuedCourse.getCategory() + ")");
                    Log.d(TAG, "  Used for replacement: " + takenReplacement);

                    // 여러 대체과목 수강 시 경고 로그
                    if (allTakenReplacements.size() > 1) {
                        Log.d(TAG, "  ⚠️ Multiple replacements taken (" + allTakenReplacements.size() + "): " + allTakenReplacements);
                        Log.d(TAG, "  → Only '" + takenReplacement + "' counted as replacement");
                        Log.d(TAG, "  → Other courses remain in their original categories");
                    }
                }
            }
        }

        Log.d(TAG, "========================================");
        return adjustedCourses;
    }

    /**
     * 넘치는 학점 처리
     * 각 카테고리에서 요구 학점을 초과한 경우 일반선택/잔여학점으로 이동
     */
    private void handleOverflowCredits(GraduationAnalysisResult result,
                                       Map<String, CategoryAnalysisResult> categoryResults) {
        if (overflowDestination == null || creditRequirements == null) {
            return;
        }

        Log.d(TAG, "Handling overflow credits to: " + overflowDestination);

        int totalOverflow = 0;

        // 각 카테고리의 넘치는 학점 계산
        for (RequirementCategory category : categories) {
            CategoryAnalysisResult categoryResult = categoryResults.get(category.getId());
            if (categoryResult == null) {
                continue;
            }

            int earned = categoryResult.getEarnedCredits();
            int required = creditRequirements.getRequiredCredits(category.getName());

            if (earned > required && required > 0) {
                int overflow = earned - required;
                totalOverflow += overflow;
                Log.d(TAG, "  " + category.getName() + ": +" + overflow + " overflow credits");
            }
        }

        if (totalOverflow > 0) {
            // 넘치는 학점을 목적지 카테고리에 추가
            CategoryAnalysisResult overflowCategory = categoryResults.get(overflowDestination);
            if (overflowCategory == null) {
                // 목적지 카테고리가 없으면 생성
                overflowCategory = new CategoryAnalysisResult(overflowDestination, overflowDestination);
                overflowCategory.setRequiredCredits(creditRequirements.getRequiredCredits(overflowDestination));
                result.addCategoryResult(overflowCategory);
            }

            int currentEarned = overflowCategory.getEarnedCredits();
            overflowCategory.setEarnedCredits(currentEarned + totalOverflow);
            overflowCategory.calculateCompletion();

            Log.d(TAG, "  Total overflow: " + totalOverflow + " credits added to " + overflowDestination);
        }
    }

    // Getters and Setters
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public long getCohort() {
        return cohort;
    }

    public void setCohort(long cohort) {
        this.cohort = cohort;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CreditRequirements getCreditRequirements() {
        return creditRequirements;
    }

    public void setCreditRequirements(CreditRequirements creditRequirements) {
        this.creditRequirements = creditRequirements;
    }

    public String getOverflowDestination() {
        return overflowDestination;
    }

    public void setOverflowDestination(String overflowDestination) {
        this.overflowDestination = overflowDestination;
    }

    public List<RequirementCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<RequirementCategory> categories) {
        this.categories = categories;
    }

    public List<ReplacementRule> getReplacementRules() {
        return replacementRules;
    }

    public void setReplacementRules(List<ReplacementRule> replacementRules) {
        this.replacementRules = replacementRules;
    }

    public String getSourceDocumentName() {
        return sourceDocumentName;
    }

    public void setSourceDocumentName(String sourceDocumentName) {
        this.sourceDocumentName = sourceDocumentName;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(int totalCredits) {
        this.totalCredits = totalCredits;
    }

    /**
     * 이 문서가 학부공통을 사용하는지 확인
     * @return 학부공통 카테고리가 있으면 true
     */
    public boolean hasUndergraduateCommon() {
        if (categories == null) {
            return false;
        }
        for (RequirementCategory category : categories) {
            if ("학부공통".equals(category.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 이 문서가 전공심화를 사용하는지 확인
     * @return 전공심화 카테고리가 있으면 true
     */
    public boolean hasMajorAdvanced() {
        if (categories == null) {
            return false;
        }
        for (RequirementCategory category : categories) {
            if ("전공심화".equals(category.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 전공 카테고리 타입 확인 (학부공통 vs 전공심화)
     * @return "학부공통", "전공심화", 또는 null
     */
    public String getMajorCategoryType() {
        if (hasUndergraduateCommon()) {
            return "학부공통";
        } else if (hasMajorAdvanced()) {
            return "전공심화";
        }
        return null;
    }

    @Override
    public String toString() {
        return "GraduationRules{" +
                "docId='" + docId + '\'' +
                ", cohort='" + cohort + '\'' +
                ", department='" + department + '\'' +
                ", track='" + track + '\'' +
                ", version='" + version + '\'' +
                ", categories=" + (categories != null ? categories.size() : 0) +
                ", replacementRules=" + (replacementRules != null ? replacementRules.size() : 0) +
                '}';
    }
}
