package sprout.app.sakmvp1.models;

/**
 * 개별 과목 요구사항을 나타내는 클래스
 * Firestore의 courses 배열 요소와 매핑됨
 */
public class CourseRequirement {
    private String name;
    private int credits;
    private String semester;  // "1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2"
    private boolean mandatory;  // 필수 여부

    // Firestore 역직렬화를 위한 빈 생성자
    public CourseRequirement() {
    }

    public CourseRequirement(String name, int credits) {
        this.name = name;
        this.credits = credits;
        this.mandatory = false;
    }

    public CourseRequirement(String name, int credits, String semester, boolean mandatory) {
        this.name = name;
        this.credits = credits;
        this.semester = semester;
        this.mandatory = mandatory;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public String toString() {
        return name + " (" + credits + "학점" + (mandatory ? ", 필수" : "") + ")";
    }
}
