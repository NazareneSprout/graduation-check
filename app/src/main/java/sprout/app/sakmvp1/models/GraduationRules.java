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
 * Firestore graduation_requirements_v2 컬렉션과 매핑
 */
public class GraduationRules {
    private static final String TAG = "GraduationRules";

    private String docId;
    private String cohort;
    private String department;
    private String track;
    private String version;
    private Timestamp updatedAt;

    private CreditRequirements creditRequirements;
    private String overflowDestination;
    private List<RequirementCategory> categories;
    private List<ReplacementRule> replacementRules;

    // Firestore 역직렬화를 위한 빈 생성자
    public GraduationRules() {
        this.categories = new ArrayList<>();
        this.replacementRules = new ArrayList<>();
    }

    public GraduationRules(String cohort, String department, String track) {
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
        Log.d(TAG, "========================================");

        GraduationAnalysisResult result = new GraduationAnalysisResult();
        result.setDocId(docId);
        result.setCohort(cohort);
        result.setDepartment(department);
        result.setTrack(track);

        // 1. 대체과목 적용
        List<CourseInputActivity.Course> adjustedCourses = applyReplacementRules(takenCourses);

        // 2. 각 카테고리 분석
        Map<String, CategoryAnalysisResult> categoryResults = new HashMap<>();
        for (RequirementCategory category : categories) {
            CategoryAnalysisResult categoryResult = category.analyze(adjustedCourses);
            categoryResults.put(category.getId(), categoryResult);
            result.addCategoryResult(categoryResult);

            Log.d(TAG, "Category analyzed: " + category.getName() + " -> " +
                  categoryResult.getEarnedCredits() + "/" + categoryResult.getRequiredCredits() +
                  " (" + (categoryResult.isCompleted() ? "✓" : "✗") + ")");
        }

        // 3. 총 학점 계산
        int totalEarnedCredits = 0;
        for (CategoryAnalysisResult categoryResult : categoryResults.values()) {
            totalEarnedCredits += categoryResult.getEarnedCredits();
        }
        result.setTotalEarnedCredits(totalEarnedCredits);

        if (creditRequirements != null) {
            result.setTotalRequiredCredits(creditRequirements.getTotal());
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

        Log.d(TAG, "Applying " + replacementRules.size() + " replacement rules...");

        for (ReplacementRule rule : replacementRules) {
            if (rule.canApply(takenCourseNames)) {
                // 대체과목 적용 가능
                ReplacementRule.CourseInfo discontinuedCourse = rule.getDiscontinuedCourse();
                String takenReplacement = rule.getTakenReplacementCourse(takenCourseNames);

                if (discontinuedCourse != null && takenReplacement != null) {
                    // 폐강된 과목을 가상으로 추가
                    CourseInputActivity.Course virtualCourse = new CourseInputActivity.Course(
                        discontinuedCourse.getCategory(),
                        discontinuedCourse.getName(),
                        discontinuedCourse.getCredits()
                    );
                    adjustedCourses.add(virtualCourse);

                    Log.d(TAG, "✓ Replacement applied: " + discontinuedCourse.getName() +
                          " ← " + takenReplacement);
                }
            }
        }

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

    public String getCohort() {
        return cohort;
    }

    public void setCohort(String cohort) {
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
