package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
import android.content.Intent; // Intent 임포트 확인
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

// Firestore 및 FirebaseAuth 임포트
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import sprout.app.sakmvp1.R;

/**
 * 시간표 Fragment (자동 저장 방식, Nested Collection)
 */
public class TimeTableFragment extends Fragment {

    private RelativeLayout timetableLayout;
    private Toolbar toolbar;
    private ImageButton btnTimetableMenu;
    private ImageButton btnCommonCalendar; // [추가됨] 공통 캘린더 버튼 변수 선언
    private FloatingActionButton fabAddSchedule;
    private LinearLayout emptyStateLayout;
    private androidx.core.widget.NestedScrollView timetableScrollView;
    private MaterialButton btnCreateFirstTimetable;

    private int startHour = 9, startMinute = 0, endHour = 10, endMinute = 0;

    private static final int START_TIME_HOUR = 9;
    private static final int END_TIME_HOUR = 24;

    private TimetableLocalStorage localStorage;
    private String activeTimetableId;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final ArrayList<ScheduleData> scheduleList = new ArrayList<>();
    private final Map<String, View> scheduleViewMap = new HashMap<>();

    // (ScheduleData 내부 클래스는 동일)
    public static class ScheduleData {
        public String documentId;
        public int dayIndex;
        public int startHour;
        public int startMinute;
        public int endHour;
        public int endMinute;
        public String subjectName;
        public String professorName;
        public String location;
        public ScheduleData() {}
        public ScheduleData(int dayIndex, int startHour, int startMinute, int endHour, int endMinute, String subjectName, String professorName, String location) {
            this.dayIndex = dayIndex;
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.endHour = endHour;
            this.endMinute = endMinute;
            this.subjectName = subjectName;
            this.professorName = professorName;
            this.location = location;
        }
        public int getStartTotalMinutes() { return startHour * 60 + startMinute; }
        public int getEndTotalMinutes() { return endHour * 60 + endMinute; }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_time_table, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        timetableLayout = view.findViewById(R.id.timetable_layout);
        btnTimetableMenu = view.findViewById(R.id.btnTimetableMenu);

        // [추가됨] XML에서 공통 캘린더 버튼 연결
        btnCommonCalendar = view.findViewById(R.id.btnCommonCalendar);

        fabAddSchedule = view.findViewById(R.id.fab_add_schedule);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        timetableScrollView = view.findViewById(R.id.timetable_scroll_view);
        btnCreateFirstTimetable = view.findViewById(R.id.btn_create_first_timetable);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        localStorage = new TimetableLocalStorage(requireContext());

        drawTimetableBase();

        btnTimetableMenu.setOnClickListener(v -> showTimetableMenu(v));
        fabAddSchedule.setOnClickListener(v -> handleAddScheduleClick());
        btnCreateFirstTimetable.setOnClickListener(v -> createDefaultTimetableAndProceed());

        // 기존: 이전 시간표 목록 보기
        view.findViewById(R.id.btnPreviousTimetable).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SavedTimetablesActivity.class);
            startActivity(intent);
        });

        // [추가됨] 공통 캘린더 버튼 클릭 리스너
        if (btnCommonCalendar != null) {
            btnCommonCalendar.setOnClickListener(v -> {
                // 공통 캘린더 액티비티로 이동 (CommonCalendarActivity 클래스를 새로 만드셔야 합니다)
                Intent intent = new Intent(requireContext(), CommonCalendarActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadActiveTimetableFromFirestore();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        } else {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void drawTimetableBase() {
        int hourHeight_dp = 50;
        int totalHours = END_TIME_HOUR - START_TIME_HOUR + 1;
        timetableLayout.setMinimumHeight(dpToPx(totalHours * hourHeight_dp));
        for (int i = START_TIME_HOUR; i <= END_TIME_HOUR; i++) {
            TextView timeLabel = new TextView(requireContext());
            timeLabel.setText(String.format("%02d", i));
            timeLabel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dpToPx(40), dpToPx(hourHeight_dp));
            params.topMargin = dpToPx((i - START_TIME_HOUR) * hourHeight_dp);
            timetableLayout.addView(timeLabel, params);
        }
        for (int i = START_TIME_HOUR; i <= END_TIME_HOUR; i++) {
            View line = new View(requireContext());
            line.setBackgroundColor(Color.LTGRAY);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 1);
            params.topMargin = dpToPx((i - START_TIME_HOUR) * hourHeight_dp);
            timetableLayout.addView(line, params);
        }
    }

    private int dpToPx(float dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

    private void showAddScheduleBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add_schedule, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextInputEditText editSubjectName = bottomSheetView.findViewById(R.id.edit_subject_name);
        TextInputEditText editProfessorName = bottomSheetView.findViewById(R.id.edit_professor_name);
        TextInputEditText editLocation = bottomSheetView.findViewById(R.id.edit_location);
        Spinner spinnerDayOfWeek = bottomSheetView.findViewById(R.id.spinner_day_of_week);
        TextView textTime = bottomSheetView.findViewById(R.id.text_time);
        MaterialButton buttonCancel = bottomSheetView.findViewById(R.id.button_cancel);
        MaterialButton buttonAdd = bottomSheetView.findViewById(R.id.button_add);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(adapter);

        textTime.setOnClickListener(v -> showTimePickerDialog(textTime));
        buttonCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        buttonAdd.setOnClickListener(v -> {
            String subjectName = editSubjectName.getText() != null ? editSubjectName.getText().toString().trim() : "";
            String professorName = editProfessorName.getText() != null ? editProfessorName.getText().toString().trim() : "";
            String location = editLocation.getText() != null ? editLocation.getText().toString().trim() : "";
            int dayIndex = spinnerDayOfWeek.getSelectedItemPosition();

            if (subjectName.isEmpty()) {
                Toast.makeText(requireContext(), "수업명을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            ScheduleData newSchedule = new ScheduleData(dayIndex, startHour, startMinute, endHour, endMinute, subjectName, professorName, location);
            if (checkOverlap(newSchedule)) {
                Toast.makeText(requireContext(), "⚠️ 기존 수업과 시간이 겹칩니다!", Toast.LENGTH_LONG).show();
                return;
            }

            saveScheduleToFirestore(newSchedule);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void showTimePickerDialog(TextView textTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_time_picker, null);
        builder.setView(dialogView);
        final NumberPicker startHourPicker = dialogView.findViewById(R.id.picker_start_hour);
        final NumberPicker startMinutePicker = dialogView.findViewById(R.id.picker_start_minute);
        final NumberPicker endHourPicker = dialogView.findViewById(R.id.picker_end_hour);
        final NumberPicker endMinutePicker = dialogView.findViewById(R.id.picker_end_minute);
        final MaterialButton dialogCancelButton = dialogView.findViewById(R.id.button_dialog_cancel);
        final MaterialButton dialogCompleteButton = dialogView.findViewById(R.id.button_dialog_complete);
        startHourPicker.setMinValue(START_TIME_HOUR);
        startHourPicker.setMaxValue(END_TIME_HOUR - 1);
        endHourPicker.setMinValue(START_TIME_HOUR);
        endHourPicker.setMaxValue(END_TIME_HOUR - 1);
        final String[] minuteValues = {"00", "30"};
        startMinutePicker.setDisplayedValues(minuteValues);
        startMinutePicker.setMinValue(0);
        startMinutePicker.setMaxValue(minuteValues.length - 1);
        endMinutePicker.setDisplayedValues(minuteValues);
        endMinutePicker.setMinValue(0);
        endMinutePicker.setMaxValue(minuteValues.length - 1);
        startHourPicker.setValue(startHour);
        startMinutePicker.setValue(startMinute == 30 ? 1 : 0);
        endHourPicker.setValue(endHour);
        endMinutePicker.setValue(endMinute == 30 ? 1 : 0);
        final AlertDialog dialog = builder.create();
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());
        dialogCompleteButton.setOnClickListener(v -> {
            startHour = startHourPicker.getValue();
            startMinute = Integer.parseInt(minuteValues[startMinutePicker.getValue()]);
            endHour = endHourPicker.getValue();
            endMinute = Integer.parseInt(minuteValues[endMinutePicker.getValue()]);
            if (startHour > endHour || (startHour == endHour && startMinute >= endMinute)) {
                Toast.makeText(requireContext(), "종료 시간은 시작 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            textTime.setText(String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute));
            dialog.dismiss();
        });
        dialog.show();
    }

    private boolean checkOverlap(ScheduleData newSchedule) {
        int newStartTotalMinutes = newSchedule.getStartTotalMinutes();
        int newEndTotalMinutes = newSchedule.getEndTotalMinutes();
        for (ScheduleData existingSchedule : scheduleList) {
            if (existingSchedule.dayIndex == newSchedule.dayIndex) {
                if (newStartTotalMinutes < existingSchedule.getEndTotalMinutes() && newEndTotalMinutes > existingSchedule.getStartTotalMinutes()) {
                    return true;
                }
            }
        }
        return false;
    }


    private void addScheduleBlockToView(ScheduleData scheduleData) {
        if (scheduleData.documentId == null || scheduleViewMap.containsKey(scheduleData.documentId)) return;

        TextView scheduleBlock = new TextView(requireContext());
        String professorName = scheduleData.professorName;
        scheduleBlock.setText(scheduleData.subjectName + "\n" + scheduleData.location + "\n" + professorName);
        scheduleBlock.setTextColor(Color.WHITE);
        scheduleBlock.setGravity(Gravity.CENTER);
        scheduleBlock.setTextSize(12);
        scheduleBlock.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        Random rnd = new Random(scheduleData.subjectName.hashCode());
        int color = Color.argb(200, rnd.nextInt(200), rnd.nextInt(200), rnd.nextInt(200));
        scheduleBlock.setBackgroundColor(color);

        RelativeLayout.LayoutParams params = calculateBlockParams(scheduleData);
        timetableLayout.addView(scheduleBlock, params);

        scheduleViewMap.put(scheduleData.documentId, scheduleBlock);

        scheduleBlock.setOnLongClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("'" + scheduleData.subjectName + "' 수업 삭제")
                    .setMessage("이 수업을 시간표에서 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        deleteScheduleFromFirestore(scheduleData);
                    })
                    .setNegativeButton("취소", null)
                    .show();
            return true;
        });
    }

    private RelativeLayout.LayoutParams calculateBlockParams(ScheduleData schedule) {
        int left_margin_dp = 40;
        int dayWidth = (getResources().getDisplayMetrics().widthPixels - dpToPx(left_margin_dp)) / 5;
        int left = dpToPx(left_margin_dp) + dayWidth * schedule.dayIndex;
        float minuteHeight_dp = 50.0f / 60.0f;
        float top_dp = (schedule.startHour - START_TIME_HOUR) * 60 * minuteHeight_dp + schedule.startMinute * minuteHeight_dp;
        float height_dp = (schedule.getEndTotalMinutes() - schedule.getStartTotalMinutes()) * minuteHeight_dp;
        int top = dpToPx(top_dp);
        int height = dpToPx(height_dp);
        if(height < 0) height = 0;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dayWidth, height);
        params.leftMargin = left;
        params.topMargin = top;
        return params;
    }

    private void saveScheduleToFirestore(ScheduleData newSchedule) {
        String userId = getCurrentUserId();
        if (userId == null || activeTimetableId == null) {
            Toast.makeText(requireContext(), "저장할 활성 시간표가 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        sprout.app.sakmvp1.ScheduleItem scheduleItem = new sprout.app.sakmvp1.ScheduleItem(
                newSchedule.dayIndex, newSchedule.startHour, newSchedule.startMinute,
                newSchedule.endHour, newSchedule.endMinute, newSchedule.subjectName,
                newSchedule.professorName, newSchedule.location
        );

        db.collection("users").document(userId)
                .collection("timetables").document(activeTimetableId)
                .update("schedules", FieldValue.arrayUnion(scheduleItem))
                .addOnSuccessListener(aVoid -> {
                    newSchedule.documentId = String.valueOf(System.currentTimeMillis());
                    scheduleList.add(newSchedule);
                    addScheduleBlockToView(newSchedule);
                    Toast.makeText(requireContext(), "수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w("TimeTableFragment", "Error adding schedule", e);
                    Toast.makeText(requireContext(), "수업 추가에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }


    private void loadActiveTimetableFromFirestore() {
        clearTimetableViews();
        activeTimetableId = localStorage.getActiveTimetableId();
        String userId = getCurrentUserId();

        if (userId == null || activeTimetableId == null) {
            Log.d("TimeTableFragment", "No active user or timetable set.");
            updateEmptyState(true);
            return;
        }

        updateEmptyState(false);

        db.collection("users").document(userId)
                .collection("timetables").document(activeTimetableId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        SavedTimetable activeTimetable = documentSnapshot.toObject(SavedTimetable.class);
                        if (activeTimetable != null && activeTimetable.getSchedules() != null) {
                            for (sprout.app.sakmvp1.ScheduleItem item : activeTimetable.getSchedules()) {
                                ScheduleData data = new ScheduleData(
                                        item.getDayIndex(), item.getStartHour(), item.getStartMinute(),
                                        item.getEndHour(), item.getEndMinute(), item.getSubjectName(),
                                        item.getProfessorName(), item.getLocation()
                                );
                                data.documentId = String.valueOf(System.currentTimeMillis() + scheduleList.size());
                                scheduleList.add(data);
                                addScheduleBlockToView(data);
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "활성 시간표를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        localStorage.setActiveTimetableId(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TimeTableFragment", "Error loading active timetable", e);
                });
    }


    private void deleteScheduleFromFirestore(ScheduleData scheduleData) {
        String userId = getCurrentUserId();
        if (userId == null || activeTimetableId == null) {
            Toast.makeText(requireContext(), "오류: 활성 시간표 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        sprout.app.sakmvp1.ScheduleItem scheduleItemToRemove = new sprout.app.sakmvp1.ScheduleItem(
                scheduleData.dayIndex, scheduleData.startHour, scheduleData.startMinute,
                scheduleData.endHour, scheduleData.endMinute, scheduleData.subjectName,
                scheduleData.professorName, scheduleData.location
        );

        db.collection("users").document(userId)
                .collection("timetables").document(activeTimetableId)
                .update("schedules", FieldValue.arrayRemove(scheduleItemToRemove))
                .addOnSuccessListener(aVoid -> {
                    View viewToRemove = scheduleViewMap.remove(scheduleData.documentId);
                    if (viewToRemove != null) {
                        timetableLayout.removeView(viewToRemove);
                    }
                    scheduleList.removeIf(schedule -> schedule.documentId != null && schedule.documentId.equals(scheduleData.documentId));
                    Toast.makeText(requireContext(), "수업이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w("TimeTableFragment", "Error deleting schedule", e);
                    Toast.makeText(requireContext(), "수업 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearTimetableViews() {
        for (View view : scheduleViewMap.values()) {
            timetableLayout.removeView(view);
        }
        scheduleViewMap.clear();
        scheduleList.clear();
    }

    private void showTimetableMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_timetable, popup.getMenu());
        MenuItem saveItem = popup.getMenu().findItem(R.id.action_save_timetable);
        if (saveItem != null) {
            saveItem.setVisible(false);
        }
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_new_timetable) {
                showCreateNewTimetableDialog();
                return true;
            } else if (itemId == R.id.action_edit_timetable) {
                showEditActiveTimetableDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showCreateNewTimetableDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_timetable, null);
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
                Toast.makeText(requireContext(), "시간표 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            SavedTimetable newTimetable = new SavedTimetable();
            newTimetable.setName(timetableName);
            newTimetable.setSavedDate(System.currentTimeMillis());
            newTimetable.setSchedules(new java.util.ArrayList<>());

            db.collection("users").document(userId)
                    .collection("timetables")
                    .add(newTimetable)
                    .addOnSuccessListener(documentReference -> {
                        String newTimetableId = documentReference.getId();
                        localStorage.setActiveTimetableId(newTimetableId);
                        activeTimetableId = newTimetableId;
                        clearTimetableViews();
                        Toast.makeText(requireContext(), "'" + timetableName + "'이(가) 생성되고 활성화되었습니다", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("TimeTableFragment", "Error creating new timetable", e);
                        Toast.makeText(requireContext(), "생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showEditActiveTimetableDialog() {
        String currentActiveId = localStorage.getActiveTimetableId();
        String userId = getCurrentUserId();

        if (userId == null || currentActiveId == null || currentActiveId.isEmpty()) {
            Toast.makeText(requireContext(), "활성화된 시간표가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_timetable, null);
        builder.setView(dialogView);
        com.google.android.material.textfield.TextInputEditText editTimetableName = dialogView.findViewById(R.id.edit_timetable_name);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);
        editTimetableName.setHint("새 시간표 이름 입력");
        btnSave.setText("수정");
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = editTimetableName.getText() != null
                    ? editTimetableName.getText().toString().trim()
                    : "";

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "시간표 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(userId)
                    .collection("timetables").document(currentActiveId)
                    .update("name", newName)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "시간표 이름이 수정되었습니다", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("TimeTableFragment", "Error updating timetable name", e);
                        Toast.makeText(requireContext(), "수정에 실패했습니다", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            timetableScrollView.setVisibility(View.GONE);
            fabAddSchedule.hide();
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            timetableScrollView.setVisibility(View.VISIBLE);
            fabAddSchedule.show();
        }
    }

    private void handleAddScheduleClick() {
        if (activeTimetableId == null) {
            createDefaultTimetableAndProceed();
        } else {
            showAddScheduleBottomSheet();
        }
    }

    private void createDefaultTimetableAndProceed() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return;
        }

        String currentDate = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String timetableName = "내 시간표 " + currentDate;

        SavedTimetable newTimetable = new SavedTimetable();
        newTimetable.setName(timetableName);
        newTimetable.setSavedDate(System.currentTimeMillis());
        newTimetable.setSchedules(new java.util.ArrayList<>());

        db.collection("users").document(userId)
                .collection("timetables")
                .add(newTimetable)
                .addOnSuccessListener(documentReference -> {
                    String newTimetableId = documentReference.getId();
                    localStorage.setActiveTimetableId(newTimetableId);
                    activeTimetableId = newTimetableId;

                    updateEmptyState(false);

                    Toast.makeText(requireContext(),
                            "'" + timetableName + "'이(가) 생성되었습니다",
                            Toast.LENGTH_SHORT).show();

                    showAddScheduleBottomSheet();
                })
                .addOnFailureListener(e -> {
                    Log.w("TimeTableFragment", "Error creating default timetable", e);
                    Toast.makeText(requireContext(), "시간표 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }
}