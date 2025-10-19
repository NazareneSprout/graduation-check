package sprout.app.sakmvp1.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 카테고리별 졸업요건 분석 결과
 */
public class CategoryAnalysisResult {
    private String categoryId;
    private String categoryName;
    private int earnedCredits;
    private int requiredCredits;
    private int earnedCourses;
    private int requiredCourses;
    private boolean isCompleted;

    private List<String> completedCourses;
    private List<String> missingCourses;
    private List<SubgroupResult> subgroupResults;
    private Map<String, Integer> courseCreditsMap;  // 과목 이름 -> 학점 매핑

    public CategoryAnalysisResult() {
        this.completedCourses = new ArrayList<>();
        this.missingCourses = new ArrayList<>();
        this.subgroupResults = new ArrayList<>();
        this.courseCreditsMap = new HashMap<>();
    }

    public CategoryAnalysisResult(String categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.completedCourses = new ArrayList<>();
        this.missingCourses = new ArrayList<>();
        this.subgroupResults = new ArrayList<>();
        this.courseCreditsMap = new HashMap<>();
    }

    // 완료 여부 계산
    public void calculateCompletion() {
        if (requiredCourses > 0) {
            // 과목 수 기준
            isCompleted = earnedCourses >= requiredCourses;
        } else {
            // 학점 기준
            isCompleted = earnedCredits >= requiredCredits;
        }
    }

    // Getters and Setters
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getEarnedCredits() {
        return earnedCredits;
    }

    public void setEarnedCredits(int earnedCredits) {
        this.earnedCredits = earnedCredits;
    }

    public int getRequiredCredits() {
        return requiredCredits;
    }

    public void setRequiredCredits(int requiredCredits) {
        this.requiredCredits = requiredCredits;
    }

    public int getEarnedCourses() {
        return earnedCourses;
    }

    public void setEarnedCourses(int earnedCourses) {
        this.earnedCourses = earnedCourses;
    }

    public int getRequiredCourses() {
        return requiredCourses;
    }

    public void setRequiredCourses(int requiredCourses) {
        this.requiredCourses = requiredCourses;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public List<String> getCompletedCourses() {
        return completedCourses;
    }

    public void setCompletedCourses(List<String> completedCourses) {
        this.completedCourses = completedCourses;
    }

    public void addCompletedCourse(String courseName) {
        if (!this.completedCourses.contains(courseName)) {
            this.completedCourses.add(courseName);
        }
    }

    public List<String> getMissingCourses() {
        return missingCourses;
    }

    public void setMissingCourses(List<String> missingCourses) {
        this.missingCourses = missingCourses;
    }

    public void addMissingCourse(String courseName) {
        if (!this.missingCourses.contains(courseName)) {
            this.missingCourses.add(courseName);
        }
    }

    public List<SubgroupResult> getSubgroupResults() {
        return subgroupResults;
    }

    public void setSubgroupResults(List<SubgroupResult> subgroupResults) {
        this.subgroupResults = subgroupResults;
    }

    public void addSubgroupResult(SubgroupResult result) {
        this.subgroupResults.add(result);
    }

    public Map<String, Integer> getCourseCreditsMap() {
        return courseCreditsMap;
    }

    public void setCourseCreditsMap(Map<String, Integer> courseCreditsMap) {
        this.courseCreditsMap = courseCreditsMap;
    }

    public void addCourseCredit(String courseName, int credits) {
        this.courseCreditsMap.put(courseName, credits);
    }

    @Override
    public String toString() {
        return categoryName + ": " + earnedCredits + "/" + requiredCredits + "학점 " +
                (isCompleted ? "✓" : "✗");
    }

    /**
     * 하위 그룹 분석 결과 (교양필수의 세부 그룹 등)
     */
    public static class SubgroupResult {
        private String groupId;
        private String groupName;
        private int earnedCredits;
        private int requiredCredits;
        private boolean isCompleted;
        private List<String> completedCourses;
        private String selectedCourse;  // oneOf 타입의 경우 선택된 과목
        private List<String> availableCourses;  // oneOf 타입의 경우 선택 가능한 모든 과목

        public SubgroupResult() {
            this.completedCourses = new ArrayList<>();
            this.availableCourses = new ArrayList<>();
        }

        public SubgroupResult(String groupId, String groupName) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.completedCourses = new ArrayList<>();
            this.availableCourses = new ArrayList<>();
        }

        // Getters and Setters
        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public int getEarnedCredits() {
            return earnedCredits;
        }

        public void setEarnedCredits(int earnedCredits) {
            this.earnedCredits = earnedCredits;
        }

        public int getRequiredCredits() {
            return requiredCredits;
        }

        public void setRequiredCredits(int requiredCredits) {
            this.requiredCredits = requiredCredits;
        }

        public boolean isCompleted() {
            return isCompleted;
        }

        public void setCompleted(boolean completed) {
            isCompleted = completed;
        }

        public List<String> getCompletedCourses() {
            return completedCourses;
        }

        public void setCompletedCourses(List<String> completedCourses) {
            this.completedCourses = completedCourses;
        }

        public String getSelectedCourse() {
            return selectedCourse;
        }

        public void setSelectedCourse(String selectedCourse) {
            this.selectedCourse = selectedCourse;
        }

        public List<String> getAvailableCourses() {
            return availableCourses;
        }

        public void setAvailableCourses(List<String> availableCourses) {
            this.availableCourses = availableCourses;
        }

        @Override
        public String toString() {
            return groupName + ": " + earnedCredits + "/" + requiredCredits + "학점 " +
                    (isCompleted ? "✓" : "✗");
        }
    }
}
