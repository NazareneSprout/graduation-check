package sprout.app.sakmvp1.timetable;

import com.google.firebase.firestore.Exclude;
import java.util.List;

import sprout.app.sakmvp1.ScheduleItem;

/**
 * 저장된 시간표 데이터 모델 클래스 (userId 필드 없음)
 */
public class SavedTimetable {

    @Exclude // Firestore 자동 직렬화에서 제외
    private String id;

    private String name;              // 시간표 이름
    private long savedDate;           // 저장된 날짜 (Unix timestamp)
    private List<ScheduleItem> schedules;  // 시간표 수업 목록

    // userId 필드 제거됨

    public SavedTimetable() {
        // 기본 생성자
    }

    // 이 생성자는 참조용이며, 앱 코드에서 직접 사용되지는 않습니다.
    public SavedTimetable(String name, String userId, long savedDate, List<ScheduleItem> schedules) {
        this.name = name;
        // userId 필드 제거됨
        this.savedDate = savedDate;
        this.schedules = schedules;
    }

    // --- Getters and Setters ---

    @Exclude
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

    // getUserId() / setUserId() 메서드 제거됨

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
    }

    @Exclude
    public int getCourseCount() {
        // Getter에서 직접 계산하여 반환
        return schedules != null ? schedules.size() : 0;
    }

    // setCourseCount() 제거 (getCourseCount가 계산하므로)
}