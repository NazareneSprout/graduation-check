package sprout.app.sakmvp1.models;

import java.util.ArrayList;
import java.util.List;

/**
 * 교양 과목 그룹 (단일 과목 또는 선택 과목 그룹)
 */
public class GeneralCourseGroup {

    public enum Type {
        SINGLE,     // 단일 과목
        ONE_OF,     // 선택 과목 (oneOf - 1개만 선택)
        MULTIPLE    // 선택 과목 (여러 개 가능)
    }

    private Type type;
    private int credit;
    private List<String> courseNames;  // 과목명 리스트
    private String semester;  // 학기 정보 (예: "1학년 1학기")

    /**
     * 단일 과목 생성자
     */
    public GeneralCourseGroup(String courseName, int credit) {
        this.type = Type.SINGLE;
        this.credit = credit;
        this.courseNames = new ArrayList<>();
        this.courseNames.add(courseName);
    }

    /**
     * 선택 과목 그룹 생성자
     */
    public GeneralCourseGroup(Type type, List<String> courseNames, int credit) {
        this.type = type;
        this.courseNames = new ArrayList<>(courseNames);
        this.credit = credit;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getCredit() {
        return credit;
    }

    public void setCredit(int credit) {
        this.credit = credit;
    }

    public List<String> getCourseNames() {
        return courseNames;
    }

    public void setCourseNames(List<String> courseNames) {
        this.courseNames = courseNames;
    }

    /**
     * 단일 과목인지 확인
     */
    public boolean isSingle() {
        return type == Type.SINGLE;
    }

    /**
     * 선택 과목 그룹인지 확인
     */
    public boolean isOptionGroup() {
        return type == Type.ONE_OF || type == Type.MULTIPLE;
    }

    /**
     * 타입 표시 텍스트
     */
    public String getTypeLabel() {
        switch (type) {
            case SINGLE:
                return "단일";
            case ONE_OF:
                return "택1";
            case MULTIPLE:
                return "선택";
            default:
                return "";
        }
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    @Override
    public String toString() {
        if (isSingle()) {
            return courseNames.get(0) + " (" + credit + "학점)";
        } else {
            return getTypeLabel() + ": " + String.join(", ", courseNames) + " (" + credit + "학점)";
        }
    }
}
