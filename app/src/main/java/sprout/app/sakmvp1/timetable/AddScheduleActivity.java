package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Random;

import sprout.app.sakmvp1.BaseActivity;
import sprout.app.sakmvp1.R;

public class AddScheduleActivity extends BaseActivity {

    private RelativeLayout timetableLayout;
    private EditText editSubjectName, editProfessorName, editLocation;
    private Spinner spinnerDayOfWeek;
    private TextView textTime;
    private Button buttonComplete;

    // 선택된 시간을 저장할 변수
    private int startHour = 9, startMinute = 0, endHour = 10, endMinute = 0;
    // 시간표의 시간 범위
    private static final int START_TIME_HOUR = 9;
    private static final int END_TIME_HOUR = 18;

    // 수업 정보를 저장할 데이터 리스트
    private ArrayList<ScheduleData> scheduleList = new ArrayList<>();

    // 수업 정보를 담을 간단한 내부 클래스
    private static class ScheduleData {
        int dayIndex; // 0=월, 1=화...
        int startTotalMinutes;
        int endTotalMinutes;

        ScheduleData(int dayIndex, int startHour, int startMinute, int endHour, int endMinute) {
            this.dayIndex = dayIndex;
            this.startTotalMinutes = startHour * 60 + startMinute;
            this.endTotalMinutes = endHour * 60 + endMinute;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);

        // UI 요소 초기화
        initViews();

        // 시간표 기본 배경 그리기 (시간, 구분선)
        drawTimetableBase();

        // 스피너 설정
        setupSpinner();

        // 리스너 설정
        setupListeners();
    }

    private void initViews() {
        timetableLayout = findViewById(R.id.timetable_layout);
        editSubjectName = findViewById(R.id.edit_subject_name);
        editProfessorName = findViewById(R.id.edit_professor_name);
        editLocation = findViewById(R.id.edit_location);
        spinnerDayOfWeek = findViewById(R.id.spinner_day_of_week);
        textTime = findViewById(R.id.text_time);
        buttonComplete = findViewById(R.id.button_complete);
    }

    private void setupSpinner() {
        // res/values/arrays.xml 에 정의된 string-array를 사용
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(adapter);
    }

    private void setupListeners() {
        textTime.setOnClickListener(v -> showTimePickerDialog());
        buttonComplete.setOnClickListener(v -> addScheduleBlockToView());
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

    private void showTimePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_time_picker, null);
        builder.setView(dialogView);

        final NumberPicker startHourPicker = dialogView.findViewById(R.id.picker_start_hour);
        final NumberPicker startMinutePicker = dialogView.findViewById(R.id.picker_start_minute);
        final NumberPicker endHourPicker = dialogView.findViewById(R.id.picker_end_hour);
        final NumberPicker endMinutePicker = dialogView.findViewById(R.id.picker_end_minute);
        final Button dialogCancelButton = dialogView.findViewById(R.id.button_dialog_cancel);
        final Button dialogCompleteButton = dialogView.findViewById(R.id.button_dialog_complete);

        // 시간 범위 설정
        startHourPicker.setMinValue(START_TIME_HOUR);
        startHourPicker.setMaxValue(END_TIME_HOUR);
        endHourPicker.setMinValue(START_TIME_HOUR);
        endHourPicker.setMaxValue(END_TIME_HOUR);

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
            // 선택된 시간 저장
            startHour = startHourPicker.getValue();
            startMinute = Integer.parseInt(minuteValues[startMinutePicker.getValue()]);
            endHour = endHourPicker.getValue();
            endMinute = Integer.parseInt(minuteValues[endMinutePicker.getValue()]);

            // 종료 시간이 시작 시간보다 빠르면 안됨
            if (startHour > endHour || (startHour == endHour && startMinute >= endMinute)) {
                Toast.makeText(this, "종료 시간은 시작 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 메인 화면의 TextView 업데이트
            textTime.setText(String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute));
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 새로 추가할 수업이 기존 수업과 겹치는지 확인하는 메소드
     * @return 겹치면 true, 겹치지 않으면 false
     */
    private boolean checkOverlap(int newDay, int newStartHour, int newStartMinute, int newEndHour, int newEndMinute) {
        int newStartTotalMinutes = newStartHour * 60 + newStartMinute;
        int newEndTotalMinutes = newEndHour * 60 + newEndMinute;

        for (ScheduleData existingSchedule : scheduleList) {
            // 1. 요일이 같은지 확인
            if (existingSchedule.dayIndex == newDay) {
                // 2. 시간이 겹치는지 확인 (새 수업 시작시간 < 기존 수업 종료시간 AND 새 수업 종료시간 > 기존 수업 시작시간)
                if (newStartTotalMinutes < existingSchedule.endTotalMinutes && newEndTotalMinutes > existingSchedule.startTotalMinutes) {
                    return true; // 시간이 겹칩니다!
                }
            }
        }
        return false; // 겹치는 시간이 없습니다.
    }

    private void addScheduleBlockToView() {
        String subjectName = editSubjectName.getText().toString();
        String professorName = editProfessorName.getText().toString();
        String location = editLocation.getText().toString();
        int dayIndex = spinnerDayOfWeek.getSelectedItemPosition(); // 0:월, 1:화...

        if (subjectName.isEmpty()) {
            Toast.makeText(this, "수업명을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 겹침 검사 로직 ---
        if (checkOverlap(dayIndex, startHour, startMinute, endHour, endMinute)) {
            // 겹치는 경우, 경고 메시지를 보여주고 함수를 즉시 종료합니다.
            Toast.makeText(this, "⚠️ 기존 수업과 시간이 겹칩니다!", Toast.LENGTH_LONG).show();
            return;
        }
        // --- 검사 로직 끝 ---

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
        RelativeLayout.LayoutParams params = calculateBlockParams(dayIndex, startHour, startMinute, endHour, endMinute);
        timetableLayout.addView(scheduleBlock, params);

        // 겹치지 않아 정상 추가된 경우, 데이터 리스트에도 저장합니다.
        ScheduleData newScheduleData = new ScheduleData(dayIndex, startHour, startMinute, endHour, endMinute);
        scheduleList.add(newScheduleData);

        Toast.makeText(this, "수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private RelativeLayout.LayoutParams calculateBlockParams(int dayIndex, int startH, int startM, int endH, int endM) {
        int left_margin_dp = 40; // 시간 표시 라벨 너비
        int dayWidth = (getResources().getDisplayMetrics().widthPixels - dpToPx(left_margin_dp)) / 5;

        int left = dpToPx(left_margin_dp) + dayWidth * dayIndex;

        // 1시간의 높이 = 50dp
        // 1분의 높이 = 50/60 dp
        float minuteHeight_dp = 50.0f / 60.0f;

        // float 값을 그대로 dpToPx에 전달
        float top_dp = (startH - START_TIME_HOUR) * 60 * minuteHeight_dp + startM * minuteHeight_dp;
        float height_dp = ((endH * 60 + endM) - (startH * 60 + startM)) * minuteHeight_dp;

        int top = dpToPx(top_dp);
        int height = dpToPx(height_dp);

        Log.d("MyTimetable", "dayIndex: " + dayIndex + ", left: " + left + ", top: " + top + ", width: " + dayWidth + ", height: " + height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dayWidth, height);
        params.leftMargin = left;
        params.topMargin = top;

        return params;
    }

    // DP 단위를 Pixel로 변환하는 유틸리티 메소드 (float 지원)
    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}