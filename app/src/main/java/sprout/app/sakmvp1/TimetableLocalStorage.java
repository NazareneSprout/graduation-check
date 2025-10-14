package sprout.app.sakmvp1;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 로컬 저장소를 사용하여 시간표를 저장/불러오기/삭제하는 클래스
 */
public class TimetableLocalStorage {
    private static final String PREF_NAME = "saved_timetables";
    private static final String KEY_TIMETABLES = "timetables";
    private static final String KEY_ACTIVE_ID = "active_timetable_id";

    private SharedPreferences prefs;
    private Gson gson;

    public TimetableLocalStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * 시간표 저장
     */
    public void saveTimetable(SavedTimetable timetable) {
        List<SavedTimetable> timetables = getAllTimetables();

        // ID 자동 생성
        if (timetable.getId() == null || timetable.getId().isEmpty()) {
            timetable.setId(String.valueOf(System.currentTimeMillis()));
        }

        timetables.add(timetable);
        saveAllTimetables(timetables);
    }

    /**
     * 모든 시간표 조회
     */
    public List<SavedTimetable> getAllTimetables() {
        String json = prefs.getString(KEY_TIMETABLES, null);
        if (json == null) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<SavedTimetable>>(){}.getType();
        List<SavedTimetable> timetables = gson.fromJson(json, type);
        return timetables != null ? timetables : new ArrayList<>();
    }

    /**
     * 시간표 삭제
     */
    public boolean deleteTimetable(String timetableId) {
        List<SavedTimetable> timetables = getAllTimetables();
        boolean removed = timetables.removeIf(t -> t.getId().equals(timetableId));

        if (removed) {
            saveAllTimetables(timetables);
        }
        return removed;
    }

    /**
     * 시간표 이름 수정
     */
    public boolean updateTimetableName(String timetableId, String newName) {
        List<SavedTimetable> timetables = getAllTimetables();
        boolean updated = false;

        for (SavedTimetable timetable : timetables) {
            if (timetable.getId().equals(timetableId)) {
                timetable.setName(newName);
                updated = true;
                break;
            }
        }

        if (updated) {
            saveAllTimetables(timetables);
        }
        return updated;
    }

    /**
     * 특정 시간표 조회
     */
    public SavedTimetable getTimetable(String timetableId) {
        List<SavedTimetable> timetables = getAllTimetables();
        for (SavedTimetable timetable : timetables) {
            if (timetable.getId().equals(timetableId)) {
                return timetable;
            }
        }
        return null;
    }

    /**
     * 모든 시간표 저장 (내부용)
     */
    private void saveAllTimetables(List<SavedTimetable> timetables) {
        String json = gson.toJson(timetables);
        prefs.edit().putString(KEY_TIMETABLES, json).apply();
    }

    /**
     * 활성 시간표 ID 설정
     */
    public void setActiveTimetableId(String id) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply();
    }

    /**
     * 활성 시간표 ID 조회
     */
    public String getActiveTimetableId() {
        return prefs.getString(KEY_ACTIVE_ID, null);
    }

    /**
     * 모든 데이터 삭제 (디버그용)
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
