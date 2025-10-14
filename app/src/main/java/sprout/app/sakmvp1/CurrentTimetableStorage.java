package sprout.app.sakmvp1;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import sprout.app.sakmvp1.timetable.TimeTableFragment;

/**
 * 현재 작업 중인 시간표를 로컬에 저장/불러오기하는 클래스
 */
public class CurrentTimetableStorage {
    private static final String PREF_NAME = "current_timetable";
    private static final String KEY_SCHEDULES = "schedules";

    private SharedPreferences prefs;
    private Gson gson;

    public CurrentTimetableStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * 현재 시간표 저장
     */
    public void saveCurrentTimetable(List<TimeTableFragment.ScheduleData> schedules) {
        String json = gson.toJson(schedules);
        prefs.edit().putString(KEY_SCHEDULES, json).apply();
    }

    /**
     * 현재 시간표 불러오기
     */
    public List<TimeTableFragment.ScheduleData> loadCurrentTimetable() {
        String json = prefs.getString(KEY_SCHEDULES, null);
        if (json == null) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<TimeTableFragment.ScheduleData>>(){}.getType();
        List<TimeTableFragment.ScheduleData> schedules = gson.fromJson(json, type);
        return schedules != null ? schedules : new ArrayList<>();
    }

    /**
     * 현재 시간표 전체 삭제
     */
    public void clearCurrentTimetable() {
        prefs.edit().remove(KEY_SCHEDULES).apply();
    }

    /**
     * 특정 수업 추가
     */
    public void addSchedule(TimeTableFragment.ScheduleData schedule) {
        List<TimeTableFragment.ScheduleData> schedules = loadCurrentTimetable();
        schedules.add(schedule);
        saveCurrentTimetable(schedules);
    }

    /**
     * 특정 수업 삭제 (documentId로)
     */
    public void removeSchedule(String documentId) {
        List<TimeTableFragment.ScheduleData> schedules = loadCurrentTimetable();
        schedules.removeIf(s -> s.documentId != null && s.documentId.equals(documentId));
        saveCurrentTimetable(schedules);
    }
}
