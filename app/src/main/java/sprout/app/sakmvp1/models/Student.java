package sprout.app.sakmvp1.models;

/**
 * 학생 데이터 모델
 */
public class Student {
    private String userId;  // Firebase Auth UID
    private String name;    // 사용자 이름 (Firebase Auth에서 가져옴)
    private String email;   // 이메일
    private String studentYear;  // 학번 (예: "2023")
    private String department;   // 학과 (예: "IT학부")
    private String track;        // 트랙 (예: "멀티미디어")
    private long updatedAt;      // 마지막 업데이트 시간
    private Long lastGraduationCheckDate;  // 마지막 졸업요건 검사 날짜 (timestamp)
    private Boolean hasGraduationCheckHistory;  // 졸업요건 검사 이력 여부

    public Student() {
        // Firestore 역직렬화를 위한 기본 생성자
    }

    public Student(String userId, String name, String email, String studentYear,
                   String department, String track, long updatedAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.studentYear = studentYear;
        this.department = department;
        this.track = track;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name != null ? name : "이름 없음";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentYear() {
        return studentYear;
    }

    public void setStudentYear(String studentYear) {
        this.studentYear = studentYear;
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

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getLastGraduationCheckDate() {
        return lastGraduationCheckDate;
    }

    public void setLastGraduationCheckDate(Long lastGraduationCheckDate) {
        this.lastGraduationCheckDate = lastGraduationCheckDate;
    }

    public Boolean getHasGraduationCheckHistory() {
        return hasGraduationCheckHistory != null && hasGraduationCheckHistory;
    }

    public void setHasGraduationCheckHistory(Boolean hasGraduationCheckHistory) {
        this.hasGraduationCheckHistory = hasGraduationCheckHistory;
    }

    /**
     * 표시용 학번 (2자리)
     */
    public String getDisplayYear() {
        if (studentYear != null && studentYear.length() == 4) {
            return studentYear.substring(2); // "2023" -> "23"
        }
        return studentYear;
    }

    /**
     * 마지막 졸업요건 검사 날짜를 포맷팅된 문자열로 반환
     */
    public String getFormattedLastCheckDate() {
        if (lastGraduationCheckDate == null) {
            return "검사 이력 없음";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA);
        return sdf.format(new java.util.Date(lastGraduationCheckDate));
    }
}
