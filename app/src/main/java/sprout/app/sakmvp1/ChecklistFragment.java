package sprout.app.sakmvp1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChecklistFragment extends Fragment {

    private TextView tvSelectedDate;
    private RecyclerView recyclerChecklist;
    private LinearLayout emptyState;
    private MaterialButton btnPrevDay;
    private MaterialButton btnToday;
    private MaterialButton btnNextDay;

    private ChecklistAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dayFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_checklist, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.KOREAN);
        dayFormat = new SimpleDateFormat("(E)", Locale.KOREAN);

        initViews(view);
        setupRecyclerView();
        loadChecklistData();

        return view;
    }

    private void initViews(View view) {
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        recyclerChecklist = view.findViewById(R.id.recycler_checklist);
        emptyState = view.findViewById(R.id.empty_state);
        btnPrevDay = view.findViewById(R.id.btn_prev_day);
        btnToday = view.findViewById(R.id.btn_today);
        btnNextDay = view.findViewById(R.id.btn_next_day);

        MaterialButton btnAddTask = view.findViewById(R.id.btn_add_task);

        if (btnAddTask != null) {
            btnAddTask.setOnClickListener(v -> showAddTaskDialog());
        }

        btnPrevDay.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
            loadChecklistData();
        });

        btnToday.setOnClickListener(v -> {
            selectedDate = Calendar.getInstance();
            updateDateDisplay();
            loadChecklistData();
        });

        btnNextDay.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
            loadChecklistData();
        });

        updateDateDisplay();
    }

    private void setupRecyclerView() {
        adapter = new ChecklistAdapter();
        recyclerChecklist.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerChecklist.setAdapter(adapter);
    }

    private void updateDateDisplay() {
        String dateStr = dateFormat.format(selectedDate.getTime()) + " " + dayFormat.format(selectedDate.getTime());
        tvSelectedDate.setText(dateStr);
    }

    private void loadChecklistData() {
        if (mAuth.getCurrentUser() == null) {
            showEmptyState(true);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        List<ChecklistItem> items = new ArrayList<>();

        // 선택된 날짜의 요일 가져오기 (0=월, 1=화, 2=수, 3=목, 4=금, 5=토, 6=일)
        int dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
        int targetDayIndex = getDayIndex(dayOfWeek);

        android.util.Log.d("ChecklistFragment", "Loading data for dayIndex: " + targetDayIndex + " (" + getDayName(dayOfWeek) + ")");

        // 1. 시간표에서 해당 요일의 수업 가져오기
        db.collection("users").document(userId)
                .collection("timetables")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("ChecklistFragment", "Found " + queryDocumentSnapshots.size() + " timetables");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("schedules")) {
                            List<Map<String, Object>> schedules = (List<Map<String, Object>>) doc.get("schedules");
                            if (schedules != null) {
                                android.util.Log.d("ChecklistFragment", "Found " + schedules.size() + " schedules in timetable");

                                for (Map<String, Object> schedule : schedules) {
                                    // dayIndex는 숫자 (0=월, 1=화...)
                                    Long dayIndexLong = (Long) schedule.get("dayIndex");
                                    int scheduleDayIndex = dayIndexLong != null ? dayIndexLong.intValue() : -1;

                                    android.util.Log.d("ChecklistFragment", "Schedule dayIndex: " + scheduleDayIndex + ", target: " + targetDayIndex);

                                    if (scheduleDayIndex == targetDayIndex) {
                                        ChecklistItem item = new ChecklistItem();
                                        item.type = ChecklistItem.TYPE_CLASS;
                                        item.title = (String) schedule.get("subjectName");

                                        // 시간 변환 (숫자 -> 문자열)
                                        Long startHour = (Long) schedule.get("startHour");
                                        Long startMinute = (Long) schedule.get("startMinute");
                                        Long endHour = (Long) schedule.get("endHour");
                                        Long endMinute = (Long) schedule.get("endMinute");

                                        if (startHour != null && startMinute != null) {
                                            item.startTime = String.format(Locale.getDefault(), "%02d:%02d", startHour.intValue(), startMinute.intValue());
                                        }
                                        if (endHour != null && endMinute != null) {
                                            item.endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour.intValue(), endMinute.intValue());
                                        }

                                        item.description = (String) schedule.get("location");
                                        item.isChecked = false;
                                        items.add(item);

                                        android.util.Log.d("ChecklistFragment", "Added class: " + item.title);
                                    }
                                }
                            }
                        }
                    }

                    // 2. 추가 일정 가져오기
                    loadCustomTasks(items);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChecklistFragment", "Failed to load timetables", e);
                    Toast.makeText(requireContext(), "데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    // Calendar.DAY_OF_WEEK를 dayIndex(0=월, 1=화...)로 변환
    private int getDayIndex(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return -1;
        }
    }

    private void loadCustomTasks(List<ChecklistItem> items) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = dateKeyFormat.format(selectedDate.getTime());

        db.collection("users").document(userId)
                .collection("custom_tasks")
                .whereEqualTo("date", dateKey)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ChecklistItem item = new ChecklistItem();
                        item.type = ChecklistItem.TYPE_TASK;
                        item.id = doc.getId();
                        item.title = doc.getString("title");
                        item.startTime = doc.getString("startTime");
                        item.endTime = doc.getString("endTime");
                        item.description = doc.getString("description");
                        item.isChecked = doc.getBoolean("isChecked") != null ? doc.getBoolean("isChecked") : false;
                        items.add(item);
                    }

                    // 3. 그룹 일정 가져오기
                    loadGroupEvents(items);
                })
                .addOnFailureListener(e -> {
                    // 개인 일정 로드 실패 시 그룹 일정만이라도 로드
                    loadGroupEvents(items);
                });
    }

    private void loadGroupEvents(List<ChecklistItem> items) {
        if (mAuth.getCurrentUser() == null) {
            finalizeItemsAndDisplay(items);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = dateKeyFormat.format(selectedDate.getTime());

        // 내가 속한 그룹 찾기
        db.collection("groups")
                .whereArrayContains("members", userId)
                .get()
                .addOnSuccessListener(groupSnapshots -> {
                    if (groupSnapshots.isEmpty()) {
                        finalizeItemsAndDisplay(items);
                        return;
                    }

                    // 각 그룹의 일정 가져오기
                    int[] loadedCount = {0};
                    int totalGroups = groupSnapshots.size();

                    for (QueryDocumentSnapshot groupDoc : groupSnapshots) {
                        String groupId = groupDoc.getId();
                        String groupName = groupDoc.getString("name");

                        db.collection("groups").document(groupId)
                                .collection("calendar_events")
                                .get()
                                .addOnSuccessListener(eventSnapshots -> {
                                    for (QueryDocumentSnapshot eventDoc : eventSnapshots) {
                                        String startDate = eventDoc.getString("startDate");
                                        String endDate = eventDoc.getString("endDate");

                                        // 선택된 날짜가 일정 범위에 포함되는지 확인
                                        if (isDateInRange(dateKey, startDate, endDate)) {
                                            ChecklistItem item = new ChecklistItem();
                                            item.type = ChecklistItem.TYPE_GROUP_EVENT;
                                            item.id = eventDoc.getId();
                                            item.title = eventDoc.getString("title");
                                            item.startTime = eventDoc.getString("startTime");
                                            item.endTime = eventDoc.getString("endTime");
                                            item.description = eventDoc.getString("description");
                                            item.groupName = groupName;
                                            item.isChecked = false;  // 그룹 일정은 체크 불가
                                            items.add(item);
                                        }
                                    }

                                    loadedCount[0]++;
                                    if (loadedCount[0] >= totalGroups) {
                                        finalizeItemsAndDisplay(items);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    loadedCount[0]++;
                                    if (loadedCount[0] >= totalGroups) {
                                        finalizeItemsAndDisplay(items);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    finalizeItemsAndDisplay(items);
                });
    }

    private boolean isDateInRange(String targetDate, String startDate, String endDate) {
        if (startDate == null || endDate == null) return false;
        return targetDate.compareTo(startDate) >= 0 && targetDate.compareTo(endDate) <= 0;
    }

    private void finalizeItemsAndDisplay(List<ChecklistItem> items) {
        // 시간순 정렬
        Collections.sort(items, (a, b) -> {
            String timeA = a.startTime != null ? a.startTime : "00:00";
            String timeB = b.startTime != null ? b.startTime : "00:00";
            return timeA.compareTo(timeB);
        });

        adapter.setItems(items);
        showEmptyState(items.isEmpty());
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "월";
            case Calendar.TUESDAY: return "화";
            case Calendar.WEDNESDAY: return "수";
            case Calendar.THURSDAY: return "목";
            case Calendar.FRIDAY: return "금";
            case Calendar.SATURDAY: return "토";
            case Calendar.SUNDAY: return "일";
            default: return "";
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerChecklist.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerChecklist.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showAddTaskDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.et_task_title);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_task_description);
        TextInputEditText etStartTime = dialogView.findViewById(R.id.et_start_time);
        TextInputEditText etEndTime = dialogView.findViewById(R.id.et_end_time);

        final String[] startTime = {""};
        final String[] endTime = {""};

        etStartTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(9)
                    .setMinute(0)
                    .setTitleText("시작 시간 선택")
                    .build();
            timePicker.addOnPositiveButtonClickListener(dialog -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                startTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                etStartTime.setText(startTime[0]);
            });
            timePicker.show(getChildFragmentManager(), "start_time_picker");
        });

        etEndTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(10)
                    .setMinute(0)
                    .setTitleText("종료 시간 선택")
                    .build();
            timePicker.addOnPositiveButtonClickListener(dialog -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                endTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                etEndTime.setText(endTime[0]);
            });
            timePicker.show(getChildFragmentManager(), "end_time_picker");
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("일정 추가")
                .setView(dialogView)
                .setPositiveButton("추가", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "제목을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveCustomTask(title, etDescription.getText().toString(), startTime[0], endTime[0]);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveCustomTask(String title, String description, String startTime, String endTime) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = dateKeyFormat.format(selectedDate.getTime());

        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("description", description);
        task.put("startTime", startTime);
        task.put("endTime", endTime);
        task.put("date", dateKey);
        task.put("isChecked", false);
        task.put("createdAt", new Date());

        db.collection("users").document(userId)
                .collection("custom_tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "일정이 추가되었습니다", Toast.LENGTH_SHORT).show();
                    loadChecklistData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "일정 추가 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTaskCheckStatus(String taskId, boolean isChecked) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .collection("custom_tasks")
                .document(taskId)
                .update("isChecked", isChecked)
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "체크 상태 업데이트 실패", Toast.LENGTH_SHORT).show();
                });
    }

    // ChecklistItem 클래스
    private static class ChecklistItem {
        static final int TYPE_CLASS = 0;
        static final int TYPE_TASK = 1;
        static final int TYPE_GROUP_EVENT = 2;

        String id;
        int type;
        String title;
        String startTime;
        String endTime;
        String description;
        boolean isChecked;
        String groupName;  // 그룹 일정인 경우 그룹명
    }

    // Adapter 클래스
    private class ChecklistAdapter extends RecyclerView.Adapter<ChecklistAdapter.ViewHolder> {
        private List<ChecklistItem> items = new ArrayList<>();

        void setItems(List<ChecklistItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_checklist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChecklistItem item = items.get(position);

            // 제목 설정
            holder.tvTitle.setText(item.title);

            holder.tvDescription.setText(item.description != null ? item.description : "");

            if (item.startTime != null && item.endTime != null) {
                holder.tvTime.setText(item.startTime + " - " + item.endTime);
            } else if (item.startTime != null) {
                holder.tvTime.setText(item.startTime);
            } else {
                holder.tvTime.setText("시간 미정");
            }

            // 체크박스 설정 (그룹 일정과 시간표 수업은 체크 불가)
            // 중요: 리스너를 먼저 제거한 후 setChecked를 호출해야 함 (RecyclerView 재활용 문제 방지)
            holder.checkbox.setOnCheckedChangeListener(null);

            if (item.type == ChecklistItem.TYPE_TASK) {
                holder.checkbox.setEnabled(true);
                holder.checkbox.setChecked(item.isChecked);
                holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    // 사용자 클릭으로 인한 변경만 처리 (프로그래밍 방식 변경은 무시)
                    if (buttonView.isPressed()) {
                        item.isChecked = isChecked;
                        if (item.id != null) {
                            updateTaskCheckStatus(item.id, isChecked);
                        }
                    }
                });
            } else {
                holder.checkbox.setEnabled(false);
                holder.checkbox.setChecked(false);
            }

            // 타입별 라벨 텍스트 설정
            if (item.type == ChecklistItem.TYPE_CLASS) {
                holder.tvTypeLabel.setText("수업");
                holder.tvTypeLabel.getBackground().setTint(0xFF2196F3);  // 파란색
            } else if (item.type == ChecklistItem.TYPE_GROUP_EVENT) {
                // 그룹 일정은 그룹명 표시
                holder.tvTypeLabel.setText(item.groupName != null ? item.groupName : "그룹");
                holder.tvTypeLabel.getBackground().setTint(0xFF4CAF50);  // 초록색
            } else {
                holder.tvTypeLabel.setText("개인");
                holder.tvTypeLabel.getBackground().setTint(0xFF6200EE);  // 보라색
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            com.google.android.material.checkbox.MaterialCheckBox checkbox;
            TextView tvTime;
            TextView tvTitle;
            TextView tvDescription;
            TextView tvTypeLabel;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkbox = itemView.findViewById(R.id.checkbox);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvDescription = itemView.findViewById(R.id.tv_description);
                tvTypeLabel = itemView.findViewById(R.id.tv_type_label);
            }
        }
    }
}
