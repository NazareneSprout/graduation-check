package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import sprout.app.sakmvp1.R;

/**
 * 시간표 Fragment
 */
public class TimeTableFragment extends Fragment {

    private RelativeLayout timetableLayout;
    private Toolbar toolbar;
    private ImageButton btnTimetableMenu;
    private FloatingActionButton fabAddSchedule;

    private int startHour = 9, startMinute = 0, endHour = 10, endMinute = 0;

    private static final int START_TIME_HOUR = 9;
    private static final int END_TIME_HOUR = 24;

    private sprout.app.sakmvp1.CurrentTimetableStorage currentStorage;

    private final ArrayList<ScheduleData> scheduleList = new ArrayList<>();
    private final Map<String, View> scheduleViewMap = new HashMap<>();

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
        fabAddSchedule = view.findViewById(R.id.fab_add_schedule);

        // 로컬 저장소 초기화
        currentStorage = new sprout.app.sakmvp1.CurrentTimetableStorage(requireContext());

        drawTimetableBase();

        // 연필 모양 메뉴 버튼 클릭 - PopupMenu 표시
        btnTimetableMenu.setOnClickListener(v -> showTimetableMenu(v));

        // 수업 추가 FAB 클릭 - BottomSheet 표시
        fabAddSchedule.setOnClickListener(v -> showAddScheduleBottomSheet());

        // 이전 시간표 조회 버튼 클릭
        view.findViewById(R.id.btnPreviousTimetable).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), sprout.app.sakmvp1.SavedTimetablesActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        loadSchedulesFromLocal();
    }

    @Override
    public void onStop() {
        super.onStop();
        // 로컬 저장소에 현재 상태 저장
        currentStorage.saveCurrentTimetable(scheduleList);
    }

    private void drawTimetableBase() {
        int hourHeight_dp = 50;

        int totalHours = END_TIME_HOUR - START_TIME_HOUR + 1;
        timetableLayout.setMinimumHeight(dpToPx(totalHours * hourHeight_dp));

        for (int i = START_TIME_HOUR; i <= END_TIME_HOUR; i++) {
            TextView timeLabel = new TextView(requireContext());
            timeLabel.setText(String.format("%02d", i));
            timeLabel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(hourHeight_dp)
            );
            params.topMargin = dpToPx((i - START_TIME_HOUR) * hourHeight_dp);
            timetableLayout.addView(timeLabel, params);
        }

        for (int i = START_TIME_HOUR; i <= END_TIME_HOUR; i++) {
            View line = new View(requireContext());
            line.setBackgroundColor(Color.LTGRAY);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    1
            );
            params.topMargin = dpToPx((i - START_TIME_HOUR) * hourHeight_dp);
            timetableLayout.addView(line, params);
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

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

            saveScheduleToLocal(newSchedule);
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
                        deleteScheduleFromLocal(scheduleData);
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

    /**
     * 수업을 로컬 저장소에 추가
     */
    private void saveScheduleToLocal(ScheduleData scheduleData) {
        // documentId 생성
        scheduleData.documentId = String.valueOf(System.currentTimeMillis());

        scheduleList.add(scheduleData);
        addScheduleBlockToView(scheduleData);
        currentStorage.saveCurrentTimetable(scheduleList);

        Toast.makeText(requireContext(), "수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
    }

    /**
     * 로컬 저장소에서 시간표 불러오기
     */
    private void loadSchedulesFromLocal() {
        clearTimetableViews();

        List<ScheduleData> loadedSchedules = currentStorage.loadCurrentTimetable();
        scheduleList.addAll(loadedSchedules);

        for (ScheduleData data : scheduleList) {
            addScheduleBlockToView(data);
        }
    }

    /**
     * 수업 삭제
     */
    private void deleteScheduleFromLocal(ScheduleData scheduleData) {
        // UI에서 제거
        View viewToRemove = scheduleViewMap.remove(scheduleData.documentId);
        if (viewToRemove != null) {
            timetableLayout.removeView(viewToRemove);
        }

        // 리스트에서 제거
        scheduleList.removeIf(schedule -> schedule.documentId != null && schedule.documentId.equals(scheduleData.documentId));

        // 로컬 저장소 업데이트
        currentStorage.saveCurrentTimetable(scheduleList);

        Toast.makeText(requireContext(), "수업이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void clearTimetableViews() {
        for (View view : scheduleViewMap.values()) {
            timetableLayout.removeView(view);
        }
        scheduleViewMap.clear();
        scheduleList.clear();
    }

    /**
     * 현재 시간표 저장 다이얼로그 표시
     */
    private void showSaveTimetableDialog() {
        if (scheduleList.isEmpty()) {
            Toast.makeText(requireContext(), "저장할 시간표가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_timetable, null);
        builder.setView(dialogView);

        TextInputEditText editTimetableName = dialogView.findViewById(R.id.edit_timetable_name);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // 기본 이름 제안
        String defaultName = new java.text.SimpleDateFormat("yyyy-M학기 시간표", java.util.Locale.KOREA)
                .format(new java.util.Date());
        editTimetableName.setText(defaultName);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String timetableName = editTimetableName.getText() != null
                    ? editTimetableName.getText().toString().trim()
                    : "";

            if (timetableName.isEmpty()) {
                Toast.makeText(requireContext(), "시간표 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            saveTimetableToFirestore(timetableName);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 시간표를 로컬 저장소에 저장
     */
    private void saveTimetableToFirestore(String timetableName) {
        // ScheduleData를 ScheduleItem으로 변환
        java.util.List<sprout.app.sakmvp1.ScheduleItem> scheduleItems = new java.util.ArrayList<>();
        for (ScheduleData data : scheduleList) {
            sprout.app.sakmvp1.ScheduleItem item = new sprout.app.sakmvp1.ScheduleItem(
                    data.dayIndex,
                    data.startHour,
                    data.startMinute,
                    data.endHour,
                    data.endMinute,
                    data.subjectName,
                    data.professorName,
                    data.location
            );
            scheduleItems.add(item);
        }

        sprout.app.sakmvp1.SavedTimetable savedTimetable = new sprout.app.sakmvp1.SavedTimetable(
                timetableName,
                "",  // userId는 로컬 저장에서 필요없음
                System.currentTimeMillis(),
                scheduleItems
        );

        sprout.app.sakmvp1.TimetableLocalStorage localStorage =
                new sprout.app.sakmvp1.TimetableLocalStorage(requireContext());
        localStorage.saveTimetable(savedTimetable);

        Toast.makeText(requireContext(), "'" + timetableName + "'이(가) 저장되었습니다", Toast.LENGTH_SHORT).show();
    }

    /**
     * 시간표 메뉴 표시 (PopupMenu)
     */
    private void showTimetableMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_timetable, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_save_timetable) {
                showSaveTimetableDialog();
                return true;
            } else if (itemId == R.id.action_new_timetable) {
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

    /**
     * 새 시간표 만들기 다이얼로그
     */
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

            if (timetableName.isEmpty()) {
                Toast.makeText(requireContext(), "시간표 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 새 시간표 생성
            sprout.app.sakmvp1.SavedTimetable newTimetable = new sprout.app.sakmvp1.SavedTimetable();
            newTimetable.setName(timetableName);
            newTimetable.setSavedDate(System.currentTimeMillis());
            newTimetable.setSchedules(new java.util.ArrayList<>());

            sprout.app.sakmvp1.TimetableLocalStorage localStorage =
                    new sprout.app.sakmvp1.TimetableLocalStorage(requireContext());
            localStorage.saveTimetable(newTimetable);

            // 생성한 시간표를 활성화
            localStorage.setActiveTimetableId(newTimetable.getId());

            // 현재 시간표를 빈 시간표로 초기화
            scheduleList.clear();
            clearTimetableViews();
            currentStorage.saveCurrentTimetable(scheduleList);

            Toast.makeText(requireContext(), "'" + timetableName + "'이(가) 생성되고 활성화되었습니다", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 현재 활성화된 시간표 이름 수정 다이얼로그
     */
    private void showEditActiveTimetableDialog() {
        sprout.app.sakmvp1.TimetableLocalStorage localStorage =
                new sprout.app.sakmvp1.TimetableLocalStorage(requireContext());
        String activeTimetableId = localStorage.getActiveTimetableId();

        if (activeTimetableId == null) {
            Toast.makeText(requireContext(), "활성화된 시간표가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        sprout.app.sakmvp1.SavedTimetable activeTimetable = localStorage.getTimetable(activeTimetableId);
        if (activeTimetable == null) {
            Toast.makeText(requireContext(), "시간표를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_timetable, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputEditText editTimetableName = dialogView.findViewById(R.id.edit_timetable_name);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        editTimetableName.setText(activeTimetable.getName());
        editTimetableName.setSelection(activeTimetable.getName().length());
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

            if (newName.equals(activeTimetable.getName())) {
                dialog.dismiss();
                return;
            }

            boolean success = localStorage.updateTimetableName(activeTimetableId, newName);
            if (success) {
                Toast.makeText(requireContext(), "시간표 이름이 수정되었습니다", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "수정에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
