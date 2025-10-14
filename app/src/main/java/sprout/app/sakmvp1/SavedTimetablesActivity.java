package sprout.app.sakmvp1;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 저장된 시간표 목록을 표시하는 Activity
 */
public class SavedTimetablesActivity extends AppCompatActivity implements SavedTimetableAdapter.OnTimetableActionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private FloatingActionButton fabNewTimetable;
    private SavedTimetableAdapter adapter;
    private List<SavedTimetable> timetableList;
    private TimetableLocalStorage localStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge 디스플레이 설정
        getWindow().setDecorFitsSystemWindows(false);

        setContentView(R.layout.activity_saved_timetables);

        // 로컬 저장소 초기화
        localStorage = new TimetableLocalStorage(this);

        // Toolbar 설정
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // RecyclerView 설정
        recyclerView = findViewById(R.id.recycler_saved_timetables);
        emptyView = findViewById(R.id.empty_view);

        timetableList = new ArrayList<>();
        adapter = new SavedTimetableAdapter(this, timetableList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // FAB 설정
        fabNewTimetable = findViewById(R.id.fab_new_timetable);
        fabNewTimetable.setOnClickListener(v -> showCreateTimetableDialog());

        // 저장된 시간표 불러오기
        loadSavedTimetables();
    }

    /**
     * 로컬 저장소에서 저장된 시간표 목록 불러오기
     */
    private void loadSavedTimetables() {
        timetableList.clear();
        timetableList.addAll(localStorage.getAllTimetables());

        // 저장 날짜 기준 내림차순 정렬 (최신순)
        Collections.sort(timetableList, new Comparator<SavedTimetable>() {
            @Override
            public int compare(SavedTimetable t1, SavedTimetable t2) {
                return Long.compare(t2.getSavedDate(), t1.getSavedDate());
            }
        });

        // 활성 시간표 ID를 어댑터에 전달
        String activeTimetableId = localStorage.getActiveTimetableId();
        adapter.setActiveTimetableId(activeTimetableId);

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    /**
     * 빈 상태 뷰 업데이트
     */
    private void updateEmptyView() {
        if (timetableList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * 시간표 활성화 버튼 클릭 리스너
     */
    @Override
    public void onActivateTimetable(SavedTimetable timetable, int position) {
        // 활성 시간표 ID 설정
        localStorage.setActiveTimetableId(timetable.getId());

        // ScheduleItem을 ScheduleData로 변환하여 현재 시간표에 저장
        List<sprout.app.sakmvp1.timetable.TimeTableFragment.ScheduleData> scheduleDataList = new ArrayList<>();

        if (timetable.getSchedules() != null) {
            for (ScheduleItem item : timetable.getSchedules()) {
                sprout.app.sakmvp1.timetable.TimeTableFragment.ScheduleData data =
                        new sprout.app.sakmvp1.timetable.TimeTableFragment.ScheduleData(
                                item.getDayIndex(),
                                item.getStartHour(),
                                item.getStartMinute(),
                                item.getEndHour(),
                                item.getEndMinute(),
                                item.getSubjectName(),
                                item.getProfessorName(),
                                item.getLocation()
                        );
                data.documentId = String.valueOf(System.currentTimeMillis() + scheduleDataList.size());
                scheduleDataList.add(data);
            }
        }

        // 현재 시간표 저장소에 저장
        CurrentTimetableStorage currentStorage = new CurrentTimetableStorage(this);
        currentStorage.saveCurrentTimetable(scheduleDataList);

        // UI 업데이트
        adapter.setActiveTimetableId(timetable.getId());

        Toast.makeText(this, "'" + timetable.getName() + "'이(가) 활성화되었습니다", Toast.LENGTH_SHORT).show();
    }

    /**
     * 시간표 삭제 버튼 클릭 리스너
     */
    @Override
    public void onDeleteTimetable(SavedTimetable timetable, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("시간표 삭제")
                .setMessage(timetable.getName() + "을(를) 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    boolean success = localStorage.deleteTimetable(timetable.getId());
                    if (success) {
                        timetableList.remove(position);
                        adapter.notifyItemRemoved(position);
                        updateEmptyView();
                        Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "삭제에 실패했습니다", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 시간표 이름 수정 버튼 클릭 리스너
     */
    @Override
    public void onEditTimetable(SavedTimetable timetable, int position) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_save_timetable, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputEditText editTimetableName = dialogView.findViewById(R.id.edit_timetable_name);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // 현재 이름으로 초기화
        editTimetableName.setText(timetable.getName());
        editTimetableName.setSelection(timetable.getName().length());

        // 저장 버튼 텍스트 변경
        btnSave.setText("수정");

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = editTimetableName.getText() != null
                    ? editTimetableName.getText().toString().trim()
                    : "";

            if (newName.isEmpty()) {
                Toast.makeText(this, "시간표 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newName.equals(timetable.getName())) {
                dialog.dismiss();
                return;
            }

            // 로컬 저장소 업데이트
            boolean success = localStorage.updateTimetableName(timetable.getId(), newName);
            if (success) {
                timetable.setName(newName);
                adapter.notifyItemChanged(position);
                Toast.makeText(this, "시간표 이름이 수정되었습니다", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "수정에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    /**
     * 새 시간표 만들기 다이얼로그 표시
     */
    private void showCreateTimetableDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_save_timetable, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputEditText editTimetableName = dialogView.findViewById(R.id.edit_timetable_name);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // 버튼 텍스트 변경
        btnSave.setText("만들기");

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String timetableName = editTimetableName.getText() != null
                    ? editTimetableName.getText().toString().trim()
                    : "";

            if (timetableName.isEmpty()) {
                Toast.makeText(this, "시간표 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 새 시간표 생성
            SavedTimetable newTimetable = new SavedTimetable();
            newTimetable.setName(timetableName);
            newTimetable.setSavedDate(System.currentTimeMillis());
            newTimetable.setSchedules(new ArrayList<>());

            // 저장
            localStorage.saveTimetable(newTimetable);

            // 생성한 시간표를 활성화
            localStorage.setActiveTimetableId(newTimetable.getId());

            // 현재 시간표를 빈 시간표로 초기화
            CurrentTimetableStorage currentStorage = new CurrentTimetableStorage(this);
            currentStorage.saveCurrentTimetable(new ArrayList<>());

            Toast.makeText(this, "'" + timetableName + "'이(가) 생성되었습니다", Toast.LENGTH_SHORT).show();

            // 목록 새로고침
            loadSavedTimetables();

            dialog.dismiss();
        });

        dialog.show();
    }
}
