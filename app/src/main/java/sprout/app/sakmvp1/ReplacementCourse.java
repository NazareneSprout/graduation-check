package sprout.app.sakmvp1;

import java.util.ArrayList;
import java.util.List;

/**
 * 대체과목 데이터 모델
 *
 * 과거 교육과정에 있었으나 현재는 폐지된 과목과
 * 해당 과목을 대체할 수 있는 과목들의 정보를 담는 클래스
 *
 * [대체 과목 적용 규칙]
 * - 폐지된 과목을 수강한 학생은 해당 과목의 학점을 인정받을 수 있음
 * - 대체 과목 중 하나만 수강해도 폐지된 과목의 학점으로 인정됨
 * - 예시: "데이터베이스설계(3학점, 폐지)" → 대체 과목 ["데이터베이스", "데이터베이스개론"]
 *   → 학생이 "데이터베이스" 또는 "데이터베이스개론" 중 하나를 들었다면,
 *      "데이터베이스설계" 3학점을 이수한 것으로 간주
 *
 * TODO: GraduationAnalysisResultActivity에서 졸업요건 분석 시 이 로직 적용 필요
 */
public class ReplacementCourse {

    private String id;                          // Firestore 문서 ID (자동 생성)
    private String department;                  // 학과
    private String discontinuedCourseName;      // 폐지된 과목명
    private int discontinuedCourseCredit;       // 폐지된 과목 학점
    private List<String> replacementCourseNames; // 대체 가능한 과목명 목록
    private String note;                        // 비고 (추가 설명)
    private long createdAt;                     // 생성 시간
    private long updatedAt;                     // 수정 시간

    public ReplacementCourse() {
        // Firestore 역직렬화를 위한 기본 생성자
        this.replacementCourseNames = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public ReplacementCourse(String department,
                           String discontinuedCourseName,
                           int discontinuedCourseCredit,
                           List<String> replacementCourseNames,
                           String note) {
        this.department = department;
        this.discontinuedCourseName = discontinuedCourseName;
        this.discontinuedCourseCredit = discontinuedCourseCredit;
        this.replacementCourseNames = replacementCourseNames != null ?
            new ArrayList<>(replacementCourseNames) : new ArrayList<>();
        this.note = note;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDiscontinuedCourseName() {
        return discontinuedCourseName;
    }

    public void setDiscontinuedCourseName(String discontinuedCourseName) {
        this.discontinuedCourseName = discontinuedCourseName;
    }

    public int getDiscontinuedCourseCredit() {
        return discontinuedCourseCredit;
    }

    public void setDiscontinuedCourseCredit(int discontinuedCourseCredit) {
        this.discontinuedCourseCredit = discontinuedCourseCredit;
    }

    public List<String> getReplacementCourseNames() {
        return replacementCourseNames;
    }

    public void setReplacementCourseNames(List<String> replacementCourseNames) {
        this.replacementCourseNames = replacementCourseNames;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 대체 과목명들을 콤마로 구분된 문자열로 반환
     */
    public String getReplacementCoursesAsString() {
        if (replacementCourseNames == null || replacementCourseNames.isEmpty()) {
            return "없음";
        }
        return String.join(", ", replacementCourseNames);
    }

    /**
     * 대체 과목 추가
     */
    public void addReplacementCourse(String courseName) {
        if (courseName != null && !courseName.trim().isEmpty()) {
            if (replacementCourseNames == null) {
                replacementCourseNames = new ArrayList<>();
            }
            if (!replacementCourseNames.contains(courseName.trim())) {
                replacementCourseNames.add(courseName.trim());
                this.updatedAt = System.currentTimeMillis();
            }
        }
    }

    /**
     * 대체 과목 제거
     */
    public void removeReplacementCourse(String courseName) {
        if (replacementCourseNames != null) {
            replacementCourseNames.remove(courseName);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "ReplacementCourse{" +
                "id='" + id + '\'' +
                ", department='" + department + '\'' +
                ", discontinuedCourseName='" + discontinuedCourseName + '\'' +
                ", discontinuedCourseCredit=" + discontinuedCourseCredit +
                ", replacementCourseNames=" + replacementCourseNames +
                ", note='" + note + '\'' +
                '}';
    }
}
