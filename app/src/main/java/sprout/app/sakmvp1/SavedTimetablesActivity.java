package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Firestore 및 Auth 임포트
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 저장된 시간표 목록을 표시하는 Activity (Firestore 연동, Nested Collection)
 */
public class SavedTimetablesActivity extends AppCompatActivity implements SavedTimetableAdapter.OnTimetableActionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private FloatingActionButton fabNewTimetable;
    private SavedTimetableAdapter adapter;
    private List<SavedTimetable> timetableList;

    private TimetableLocalStorage localStorage;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        setContentView(R.layout.activity_saved_timetables);

        localStorage = new TimetableLocalStorage(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_saved_timetables);
        emptyView = findViewById(R.id.empty_view);

        timetableList = new ArrayList<>();
        adapter = new SavedTimetableAdapter(this, timetableList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabNewTimetable = findViewById(R.id.fab_new_timetable);
        fabNewTimetable.setOnClickListener(v -> showCreateTimetableDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedTimetablesFromFirestore();
    }

    private String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * [수정됨] Firestore 쿼리 경로 변경 (하위 컬렉션 조회)
     */
    private void loadSavedTimetablesFromFirestore() {
        String userId = getCurrentUserId();
        if (userId == null) {
            updateEmptyView();
            return;
        }

        // [수정됨] 쿼리 경로 변경 (timetables -> userId -> user_timetables)
        // 1. whereEqualTo("userId")가 더 이상 필요 없음
        // 2. orderBy("savedDate")가 복합 색인 없이도 작동함!
        db.collection("timetables").document(userId)
                .collection("user_timetables") // <-- 하위 컬렉션 지정
                .orderBy("savedDate", Query.Direction.DESCENDING) // 정렬 재활성화
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    timetableList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        SavedTimetable timetable = document.toObject(SavedTimetable.class);
                        timetable.setId(document.getId());
                        timetableList.add(timetable);
                    }

                    // [삭제] Collections.sort(...) // Firestore에서 이미 정렬했으므로 필요 없음

                    String activeTimetableId = localStorage.getActiveTimetableId();
                    adapter.setActiveTimetableId(activeTimetableId);
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    Log.w("SavedTimetablesActivity", "Error loading timetables", e);
                    Toast.makeText(this, "시간표 로드 실패", Toast.LENGTH_SHORT).show();
                    updateEmptyView();
                });
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
     * 시간표 활성화 버튼 클릭 리스너 (로컬 작업이므로 수정 필요 없음)
     */
    @Override
    public void onActivateTimetable(SavedTimetable timetable, int position) {
        localStorage.setActiveTimetableId(timetable.getId());

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

        CurrentTimetableStorage currentStorage = new CurrentTimetableStorage(this);
        currentStorage.saveCurrentTimetable(scheduleDataList);

        adapter.setActiveTimetableId(timetable.getId());
        Toast.makeText(this, "'" + timetable.getName() + "'이(가) 활성화되었습니다", Toast.LENGTH_SHORT).show();
    }

    /**
     * [수정됨] Firestore 경로 변경 (시간표 삭제)
     */
    @Override
    public void onDeleteTimetable(SavedTimetable timetable, int position) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("시간표 삭제")
                .setMessage(timetable.getName() + "을(를) 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {

                    // [수정됨] 경로 변경
                    db.collection("timetables").document(userId)
                            .collection("user_timetables").document(timetable.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                timetableList.remove(position);
                                adapter.notifyItemRemoved(position);
                                adapter.notifyItemRangeChanged(position, timetableList.size());
                                updateEmptyView();
                                Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show();

                                if (timetable.getId().equals(localStorage.getActiveTimetableId())) {
                                    localStorage.setActiveTimetableId(null);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "삭제에 실패했습니다", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * [수정됨] Firestore 경로 변경 (시간표 이름 수정)
     */
    @Override
    public void onEditTimetable(SavedTimetable timetable, int position) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_save_timetable, null);
        builder.setView(dialogView);
        com.google.android.material.textfield.TextInputEditText editTimetableName = dialogView.findViewById(R.id.edit_timetable_name);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);
        editTimetableName.setText(timetable.getName());
        editTimetableName.setSelection(timetable.getName().length());
        btnSave.setText("수정");
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = editTimetableName.getText() != null
                    ? editTimetableName.getText().toString().trim()
                    : "";

            if (newName.isEmpty() || newName.equals(timetable.getName())) {
                dialog.dismiss();
                return;
            }

            // [수정됨] 경로 변경
            db.collection("timetables").document(userId)
                    .collection("user_timetables").document(timetable.getId())
                    .update("name", newName)
                    .addOnSuccessListener(aVoid -> {
                        timetable.setName(newName);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "시간표 이름이 수정되었습니다", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "수정에 실패했습니다", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    /**
     * [수정됨] Firestore 경로 변경 (새 시간표 생성)
     */
    private void showCreateTimetableDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_save_timetable, null);
        builder.setView(dialogView);
        com.google.android.material.textfield.TextInputEditText editTimetableName = dialogView.findViewById(R.id.edit_timetable_name);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);
        btnSave.setText("만들기");
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String timetableName = editTimetableName.getText() != null
                    ? editTimetableName.getText().toString().trim()
                    : "";

            String userId = getCurrentUserId();
            if (userId == null) {
                dialog.dismiss();
                return;
            }

            if (timetableName.isEmpty()) {
                Toast.makeText(this, "시간표 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            SavedTimetable newTimetable = new SavedTimetable();
            newTimetable.setName(timetableName);
            newTimetable.setSavedDate(System.currentTimeMillis());
            newTimetable.setSchedules(new ArrayList<>());
            // [삭제] setUserId() 호출 제거

            // [수정됨] 경로 변경
            db.collection("timetables").document(userId)
                    .collection("user_timetables")
                    .add(newTimetable)
                    .addOnSuccessListener(documentReference -> {
                        String newId = documentReference.getId();
                        localStorage.setActiveTimetableId(newId);

                        CurrentTimetableStorage currentStorage = new CurrentTimetableStorage(this);
                        currentStorage.saveCurrentTimetable(new ArrayList<>());

                        Toast.makeText(this, "'" + timetableName + "'이(가) 생성되었습니다", Toast.LENGTH_SHORT).show();
                        loadSavedTimetablesFromFirestore();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "생성에 실패했습니다", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}