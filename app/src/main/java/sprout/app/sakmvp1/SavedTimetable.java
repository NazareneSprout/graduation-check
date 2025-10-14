package sprout.app.sakmvp1;

import java.util.List;

/**
 * 저장된 시간표 데이터 모델 클래스
 */
public class SavedTimetable {
    private String id;
    private String name;              // 시간표 이름 (예: "2024-2학기 시간표")
    private String userId;            // 사용자 ID
    private long savedDate;           // 저장된 날짜 (Unix timestamp)
    private List<ScheduleItem> schedules;  // 시간표 수업 목록
    private int courseCount;          // 수업 개수

    public SavedTimetable() {
        // 기본 생성자
    }

    public SavedTimetable(String name, String userId, long savedDate, List<ScheduleItem> schedules) {
        this.name = name;
        this.userId = userId;
        this.savedDate = savedDate;
        this.schedules = schedules;
        this.courseCount = schedules != null ? schedules.size() : 0;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getSavedDate() {
        return savedDate;
    }

    public void setSavedDate(long savedDate) {
        this.savedDate = savedDate;
    }

    public List<ScheduleItem> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ScheduleItem> schedules) {
        this.schedules = schedules;
        this.courseCount = schedules != null ? schedules.size() : 0;
    }

    public int getCourseCount() {
        return courseCount;
    }

    public void setCourseCount(int courseCount) {
        this.courseCount = courseCount;
    }
}
