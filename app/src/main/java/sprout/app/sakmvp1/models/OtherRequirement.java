package sprout.app.sakmvp1.models;

import java.io.Serializable;

/**
 * 기타 졸업요건 모델
 *
 * 봉사활동, 채플, 인성교육 등 학점 외의 졸업 요건을 나타냅니다.
 */
public class OtherRequirement implements Serializable {
    private String id;              // Firestore 문서 ID
    private String name;            // 요건명 (예: "봉사활동")
    private String description;     // 설명 (예: "30시간 이상")
    private String studentYear;     // 적용 학번
    private String department;      // 적용 학과
    private String track;           // 적용 트랙
    private long timestamp;         // 생성/수정 시간

    public OtherRequirement() {
        // Firestore용 빈 생성자
    }

    public OtherRequirement(String name, String description, String studentYear, String department, String track) {
        this.name = name;
        this.description = description;
        this.studentYear = studentYear;
        this.department = department;
        this.track = track;
        this.timestamp = System.currentTimeMillis();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
