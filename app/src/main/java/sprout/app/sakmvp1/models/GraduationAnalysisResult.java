package sprout.app.sakmvp1.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 졸업요건 분석 결과
 * GraduationRules.analyze()의 반환 타입
 */
public class GraduationAnalysisResult {
    private String docId;
    private String cohort;
    private String department;
    private String track;

    private int totalEarnedCredits;
    private int totalRequiredCredits;
    private boolean isGraduationReady;

    private Map<String, CategoryAnalysisResult> categoryResults;
    private List<String> warnings;
    private List<String> recommendations;

    public GraduationAnalysisResult() {
        this.categoryResults = new HashMap<>();
        this.warnings = new ArrayList<>();
        this.recommendations = new ArrayList<>();
    }

    /**
     * 카테고리 분석 결과 추가
     */
    public void addCategoryResult(CategoryAnalysisResult result) {
        if (result != null && result.getCategoryId() != null) {
            categoryResults.put(result.getCategoryId(), result);
        }
    }

    /**
     * 졸업 가능 여부 계산
     */
    public void calculateGraduationReadiness() {
        // 1. 모든 카테고리가 충족되었는지 확인
        boolean allCategoriesComplete = true;
        for (CategoryAnalysisResult categoryResult : categoryResults.values()) {
            if (!categoryResult.isCompleted()) {
                allCategoriesComplete = false;
                break;
            }
        }

        // 2. 총 학점 충족 여부 확인
        boolean totalCreditsComplete = totalEarnedCredits >= totalRequiredCredits;

        // 3. 졸업 가능 여부 결정
        isGraduationReady = allCategoriesComplete && totalCreditsComplete;

        // 4. 경고/추천사항 생성
        generateWarningsAndRecommendations();
    }

    /**
     * 경고 및 추천사항 생성
     */
    private void generateWarningsAndRecommendations() {
        warnings.clear();
        recommendations.clear();

        // 미완료 카테고리 찾기
        for (CategoryAnalysisResult categoryResult : categoryResults.values()) {
            if (!categoryResult.isCompleted()) {
                int shortage = categoryResult.getRequiredCredits() - categoryResult.getEarnedCredits();
                if (shortage > 0) {
                    warnings.add(categoryResult.getCategoryName() + ": " + shortage + "학점 부족");
                }

                // 필수 과목 미이수
                if (!categoryResult.getMissingCourses().isEmpty()) {
                    warnings.add(categoryResult.getCategoryName() + " 필수 과목 미이수: " +
                                String.join(", ", categoryResult.getMissingCourses()));
                }
            }
        }

        // 총 학점 부족
        if (totalEarnedCredits < totalRequiredCredits) {
            int totalShortage = totalRequiredCredits - totalEarnedCredits;
            warnings.add("총 이수학점 " + totalShortage + "학점 부족 (현재: " +
                        totalEarnedCredits + "/" + totalRequiredCredits + ")");
        }

        // 추천사항
        if (!isGraduationReady) {
            if (!warnings.isEmpty()) {
                recommendations.add("부족한 학점을 채우기 위해 다음 학기 수강 계획을 세우세요");
            }
        } else {
            recommendations.add("축하합니다! 모든 졸업요건을 충족했습니다");
        }
    }

    /**
     * 특정 카테고리의 분석 결과 조회
     */
    public CategoryAnalysisResult getCategoryResult(String categoryId) {
        return categoryResults.get(categoryId);
    }

    /**
     * 모든 카테고리 결과 목록
     */
    public List<CategoryAnalysisResult> getAllCategoryResults() {
        return new ArrayList<>(categoryResults.values());
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

    public int getTotalEarnedCredits() {
        return totalEarnedCredits;
    }

    public void setTotalEarnedCredits(int totalEarnedCredits) {
        this.totalEarnedCredits = totalEarnedCredits;
    }

    public int getTotalRequiredCredits() {
        return totalRequiredCredits;
    }

    public void setTotalRequiredCredits(int totalRequiredCredits) {
        this.totalRequiredCredits = totalRequiredCredits;
    }

    public boolean isGraduationReady() {
        return isGraduationReady;
    }

    public void setGraduationReady(boolean graduationReady) {
        isGraduationReady = graduationReady;
    }

    public Map<String, CategoryAnalysisResult> getCategoryResults() {
        return categoryResults;
    }

    public void setCategoryResults(Map<String, CategoryAnalysisResult> categoryResults) {
        this.categoryResults = categoryResults;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    @Override
    public String toString() {
        return "GraduationAnalysisResult{" +
                "totalEarnedCredits=" + totalEarnedCredits +
                "/" + totalRequiredCredits +
                ", isGraduationReady=" + isGraduationReady +
                ", categories=" + categoryResults.size() +
                ", warnings=" + warnings.size() +
                '}';
    }
}
