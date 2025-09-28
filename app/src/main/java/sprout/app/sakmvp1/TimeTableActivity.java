package sprout.app.sakmvp1;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Random;

public class TimeTableActivity extends AppCompatActivity {

    private RelativeLayout timetableLayout;
    private BottomNavigationView bottomNavigation;
    private Toolbar toolbar;
    private FloatingActionButton fabAddSchedule;

    // 시간 선택을 위한 변수
    private int startHour = 9, startMinute = 0, endHour = 10, endMinute = 0;

    // 시간표의 시간 범위
    private static final int START_TIME_HOUR = 9;
    private static final int END_TIME_HOUR = 24;

    // 수업 정보를 저장할 데이터 리스트
    private ArrayList<ScheduleData> scheduleList = new ArrayList<>();

    // 수업 정보를 담을 간단한 내부 클래스
    private static class ScheduleData {
        int dayIndex; // 0=월, 1=화...
        int startTotalMinutes;
        int endTotalMinutes;
        String subjectName;
        String professorName;
        String location;

        ScheduleData(int dayIndex, int startHour, int startMinute, int endHour, int endMinute, String subjectName, String professorName, String location) {
            this.dayIndex = dayIndex;
            this.startTotalMinutes = startHour * 60 + startMinute;
            this.endTotalMinutes = endHour * 60 + endMinute;
            this.subjectName = subjectName;
            this.professorName = professorName;
            this.location = location;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_time_table);

        // 시스템 바 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.time_table_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 툴바 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // 뷰 초기화
        timetableLayout = findViewById(R.id.timetable_layout);
        fabAddSchedule = findViewById(R.id.fab_add_schedule);

        // 시간표 기본 배경 그리기
        drawTimetableBase();

        // FAB 클릭 리스너 설정
        fabAddSchedule.setOnClickListener(v -> showAddScheduleBottomSheet());

        // 하단 내비게이션 설정
        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_2);

            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_button_1) {
                    finish();
                    return true;
                } else if (itemId == R.id.nav_button_2) {
                    return true;
                } else if (itemId == R.id.nav_button_3) {
                    Toast.makeText(this, "기능3 - 준비중", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_button_4) {
                    Toast.makeText(this, "기능4 - 준비중", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
        }
    }


    private void drawTimetableBase() {
        // 1시간의 높이를 dp 단위로 설정
        int hourHeight_dp = 50;

        // 시간표 전체 높이를 계산해서 레이아웃의 최소 높이로 설정
        int totalHours = END_TIME_HOUR - START_TIME_HOUR + 1;
        timetableLayout.setMinimumHeight(dpToPx(totalHours * hourHeight_dp));

        // 시간 표시 레이블 추가
        for (int i = START_TIME_HOUR; i <= END_TIME_HOUR; i++) {
            TextView timeLabel = new TextView(this);
            timeLabel.setText(String.format("%02d", i));
            timeLabel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(hourHeight_dp)
            );
            params.topMargin = dpToPx((i - START_TIME_HOUR) * hourHeight_dp);
            timetableLayout.addView(timeLabel, params);
        }

        // 가로 구분선 추가
        for (int i = START_TIME_HOUR; i <= END_TIME_HOUR; i++) {
            View line = new View(this);
            line.setBackgroundColor(Color.LTGRAY);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    1 // 1px
            );
            params.topMargin = dpToPx((i - START_TIME_HOUR) * hourHeight_dp);
            timetableLayout.addView(line, params);
        }
    }

    // DP 단위를 Pixel로 변환하는 유틸리티 메소드
    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showAddScheduleBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_schedule, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // 뷰 참조 가져오기
        TextInputEditText editSubjectName = bottomSheetView.findViewById(R.id.edit_subject_name);
        TextInputEditText editProfessorName = bottomSheetView.findViewById(R.id.edit_professor_name);
        TextInputEditText editLocation = bottomSheetView.findViewById(R.id.edit_location);
        Spinner spinnerDayOfWeek = bottomSheetView.findViewById(R.id.spinner_day_of_week);
        TextView textTime = bottomSheetView.findViewById(R.id.text_time);
        MaterialButton buttonCancel = bottomSheetView.findViewById(R.id.button_cancel);
        MaterialButton buttonAdd = bottomSheetView.findViewById(R.id.button_add);

        // 스피너 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(adapter);

        // 시간 선택 클릭 리스너
        textTime.setOnClickListener(v -> showTimePickerDialog(textTime));

        // 취소 버튼
        buttonCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // 추가 버튼
        buttonAdd.setOnClickListener(v -> {
            String subjectName = editSubjectName.getText() != null ? editSubjectName.getText().toString() : "";
            String professorName = editProfessorName.getText() != null ? editProfessorName.getText().toString() : "";
            String location = editLocation.getText() != null ? editLocation.getText().toString() : "";
            int dayIndex = spinnerDayOfWeek.getSelectedItemPosition();

            if (subjectName.isEmpty()) {
                Toast.makeText(this, "수업명을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 겹침 검사
            if (checkOverlap(dayIndex, startHour, startMinute, endHour, endMinute)) {
                Toast.makeText(this, "⚠️ 기존 수업과 시간이 겹칩니다!", Toast.LENGTH_LONG).show();
                return;
            }

            // 수업 추가
            addScheduleBlockToView(dayIndex, startHour, startMinute, endHour, endMinute, subjectName, professorName, location);
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void showTimePickerDialog(TextView textTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_time_picker, null);
        builder.setView(dialogView);

        final NumberPicker startHourPicker = dialogView.findViewById(R.id.picker_start_hour);
        final NumberPicker startMinutePicker = dialogView.findViewById(R.id.picker_start_minute);
        final NumberPicker endHourPicker = dialogView.findViewById(R.id.picker_end_hour);
        final NumberPicker endMinutePicker = dialogView.findViewById(R.id.picker_end_minute);
        final MaterialButton dialogCancelButton = dialogView.findViewById(R.id.button_dialog_cancel);
        final MaterialButton dialogCompleteButton = dialogView.findViewById(R.id.button_dialog_complete);

        // 시간 범위 설정
        startHourPicker.setMinValue(START_TIME_HOUR);
        startHourPicker.setMaxValue(END_TIME_HOUR - 1);
        endHourPicker.setMinValue(START_TIME_HOUR);
        endHourPicker.setMaxValue(END_TIME_HOUR - 1);

        // 분 단위 설정 (0, 30)
        final String[] minuteValues = {"00", "30"};
        startMinutePicker.setDisplayedValues(minuteValues);
        startMinutePicker.setMinValue(0);
        startMinutePicker.setMaxValue(minuteValues.length - 1);
        endMinutePicker.setDisplayedValues(minuteValues);
        endMinutePicker.setMinValue(0);
        endMinutePicker.setMaxValue(minuteValues.length - 1);

        // 현재 선택된 값으로 초기화
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
                Toast.makeText(this, "종료 시간은 시작 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            textTime.setText(String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute));
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean checkOverlap(int newDay, int newStartHour, int newStartMinute, int newEndHour, int newEndMinute) {
        int newStartTotalMinutes = newStartHour * 60 + newStartMinute;
        int newEndTotalMinutes = newEndHour * 60 + newEndMinute;

        for (ScheduleData existingSchedule : scheduleList) {
            if (existingSchedule.dayIndex == newDay) {
                if (newStartTotalMinutes < existingSchedule.endTotalMinutes && newEndTotalMinutes > existingSchedule.startTotalMinutes) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addScheduleBlockToView(int dayIndex, int startH, int startM, int endH, int endM, String subjectName, String professorName, String location) {
        TextView scheduleBlock = new TextView(this);
        scheduleBlock.setText(subjectName + "\n" + location);
        scheduleBlock.setTextColor(Color.WHITE);
        scheduleBlock.setGravity(Gravity.CENTER);
        scheduleBlock.setPadding(8, 8, 8, 8);

        // 랜덤 색상 지정
        Random rnd = new Random();
        int color = Color.argb(200, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        scheduleBlock.setBackgroundColor(color);

        // 블록의 위치와 크기 계산
        RelativeLayout.LayoutParams params = calculateBlockParams(dayIndex, startH, startM, endH, endM);
        timetableLayout.addView(scheduleBlock, params);

        // 데이터 리스트에 저장
        ScheduleData newScheduleData = new ScheduleData(dayIndex, startH, startM, endH, endM, subjectName, professorName, location);
        scheduleList.add(newScheduleData);
    }

    private RelativeLayout.LayoutParams calculateBlockParams(int dayIndex, int startH, int startM, int endH, int endM) {
        int left_margin_dp = 40; // 시간 표시 라벨 너비
        int dayWidth = (getResources().getDisplayMetrics().widthPixels - dpToPx(left_margin_dp)) / 5;

        int left = dpToPx(left_margin_dp) + dayWidth * dayIndex;

        // 1시간의 높이 = 50dp
        // 1분의 높이 = 50/60 dp
        float minuteHeight_dp = 50.0f / 60.0f;

        float top_dp = (startH - START_TIME_HOUR) * 60 * minuteHeight_dp + startM * minuteHeight_dp;
        float height_dp = ((endH * 60 + endM) - (startH * 60 + startM)) * minuteHeight_dp;

        int top = dpToPx(top_dp);
        int height = dpToPx(height_dp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dayWidth, height);
        params.leftMargin = left;
        params.topMargin = top;

        return params;
    }
}