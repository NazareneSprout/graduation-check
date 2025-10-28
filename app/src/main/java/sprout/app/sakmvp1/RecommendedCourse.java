package sprout.app.sakmvp1;

import java.util.ArrayList;
import java.util.List;

/**
 * 추천 과목 데이터 모델
 */
public class RecommendedCourse {
    private String courseName;        // 과목명
    private String category;          // 카테고리 (전공필수, 전공선택, 교양필수 등)
    private int credits;              // 학점
    private int priority;             // 추천 우선순위 (1~6)
    private String reason;            // 추천 이유
    private String semester;          // 개설 학기 (1학기, 2학기, 연중 등)
    private List<String> alternativeCourses;  // 대체 가능한 과목 리스트 (oneOf 그룹)

    public RecommendedCourse() {
        this.alternativeCourses = new ArrayList<>();
    }

    public RecommendedCourse(String courseName, String category, int credits, int priority, String reason) {
        this.courseName = courseName;
        this.category = category;
        this.credits = credits;
        this.priority = priority;
        this.reason = reason;
        this.alternativeCourses = new ArrayList<>();
    }

    // Getters and Setters
    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public List<String> getAlternativeCourses() {
        return alternativeCourses;
    }

    public void setAlternativeCourses(List<String> alternativeCourses) {
        this.alternativeCourses = alternativeCourses;
    }

    /**
     * 대체 가능한 과목이 있는지 확인
     */
    public boolean hasAlternatives() {
        return alternativeCourses != null && !alternativeCourses.isEmpty();
    }

    /**
     * 카테고리를 우선순위로 변환
     * 1: 교양필수, 2: 전공필수, 3: 학부공통/전공심화, 4: 전공선택, 5: 소양, 6: 교양선택
     */
    public static int getCategoryPriority(String category) {
        if (category == null) return 99;

        switch (category) {
            case "교양필수":
                return 1;
            case "전공필수":
                return 2;
            case "학부공통":
            case "전공심화":
                return 3;
            case "전공선택":
                return 4;
            case "소양":
                return 5;
            case "교양선택":
                return 6;
            default:
                return 99; // 알 수 없는 카테고리
        }
    }

    /**
     * 카테고리 한글 표시명
     */
    public static String getCategoryDisplayName(String category) {
        if (category == null) return "";

        switch (category) {
            case "교양필수":
                return "교양필수";
            case "전공필수":
                return "전공필수";
            case "학부공통":
                return "학부공통";
            case "전공심화":
                return "전공심화";
            case "전공선택":
                return "전공선택";
            case "소양":
                return "소양";
            case "교양선택":
                return "교양선택";
            default:
                return category;
        }
    }
}
