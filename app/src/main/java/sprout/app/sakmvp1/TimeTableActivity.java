package sprout.app.sakmvp1;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Random;

public class TimeTableActivity extends AppCompatActivity {

    private RelativeLayout timetableLayout;
    private FloatingActionButton fabAddSchedule;
    private BottomSheetDialog bottomSheetDialog;
    private Toolbar toolbar;

    // BottomSheet 내부 요소들
    private EditText editSubjectName, editProfessorName, editLocation;
    private Spinner spinnerDayOfWeek;
    private TextView textTime;
    private Button buttonComplete, buttonCancel;

    // 선택된 시간을 저장할 변수
    private int startHour = 9, startMinute = 0, endHour = 10, endMinute = 0;

    // 24시간 시간표 범위
    private static final int START_TIME_HOUR = 0;
    private static final int END_TIME_HOUR = 23;

    // 수업 정보를 저장할 데이터 리스트
    private ArrayList<ScheduleData> scheduleList = new ArrayList<>();

    // 수업 정보를 담을 간단한 내부 클래스
    private static class ScheduleData {
        int dayIndex; // 0=월, 1=화...
        int startTotalMinutes;
        int endTotalMinutes;
        String subjectName;
        String location;

        ScheduleData(int dayIndex, int startHour, int startMinute, int endHour, int endMinute, String subjectName, String location) {
            this.dayIndex = dayIndex;
            this.startTotalMinutes = startHour * 60 + startMinute;
            this.endTotalMinutes = endHour * 60 + endMinute;
            this.subjectName = subjectName;
            this.location = location;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_table);

        // UI 요소 초기화
        initViews();

        // 툴바 설정
        setupToolbar();

        // 시간표 기본 배경 그리기 (24시간)
        drawTimetableBase();

        // FAB 클릭 리스너 설정
        setupFabListener();
    }

    private void initViews() {
        timetableLayout = findViewById(R.id.timetable_layout);
        fabAddSchedule = findViewById(R.id.fab_add_schedule);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFabListener() {
        fabAddSchedule.setOnClickListener(v -> showBottomSheet());
    }

    private void showBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_schedule, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // BottomSheet 내부 요소들 초기화
        initBottomSheetViews(bottomSheetView);
        setupBottomSheetListeners();
        setupSpinner();

        bottomSheetDialog.show();
    }

    private void initBottomSheetViews(View view) {
        editSubjectName = view.findViewById(R.id.edit_subject_name);
        editProfessorName = view.findViewById(R.id.edit_professor_name);
        editLocation = view.findViewById(R.id.edit_location);
        spinnerDayOfWeek = view.findViewById(R.id.spinner_day_of_week);
        textTime = view.findViewById(R.id.text_time);
        buttonComplete = view.findViewById(R.id.button_complete);
        buttonCancel = view.findViewById(R.id.button_cancel);
    }

    private void setupBottomSheetListeners() {
        textTime.setOnClickListener(v -> showTimePickerDialog());
        buttonComplete.setOnClickListener(v -> addScheduleBlockToView());
        buttonCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(adapter);
    }

    private void drawTimetableBase() {
        // 1시간의 높이를 dp 단위로 설정 (24시간이므로 조금 더 작게)
        int hourHeight_dp = 40;

        // 시간표 전체 높이를 계산해서 레이아웃의 최소 높이로 설정
        int totalHours = 24;
        timetableLayout.setMinimumHeight(dpToPx(totalHours * hourHeight_dp));

        // 시간 표시 레이블 추가 (24시간)
        for (int i = START_TIME_HOUR; i <= END_TIME_HOUR; i++) {
            TextView timeLabel = new TextView(this);
            timeLabel.setText(String.format("%02d", i));
            timeLabel.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            timeLabel.setTextSize(10);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(hourHeight_dp)
            );
            params.topMargin = dpToPx(i * hourHeight_dp);
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
            params.topMargin = dpToPx(i * hourHeight_dp);
            timetableLayout.addView(line, params);
        }
    }

    private void showTimePickerDialog() {
        // 간단한 시간 선택을 위해 기본값으로 설정
        textTime.setText(String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute));
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

    private void addScheduleBlockToView() {
        String subjectName = editSubjectName.getText().toString();
        String professorName = editProfessorName.getText().toString();
        String location = editLocation.getText().toString();
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

        TextView scheduleBlock = new TextView(this);
        scheduleBlock.setText(subjectName + "\n" + location);
        scheduleBlock.setTextColor(Color.WHITE);
        scheduleBlock.setGravity(Gravity.CENTER);
        scheduleBlock.setPadding(8, 8, 8, 8);
        scheduleBlock.setTextSize(10);

        // 랜덤 색상 지정
        Random rnd = new Random();
        int color = Color.argb(200, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        scheduleBlock.setBackgroundColor(color);

        // 블록의 위치와 크기 계산
        RelativeLayout.LayoutParams params = calculateBlockParams(dayIndex, startHour, startMinute, endHour, endMinute);
        timetableLayout.addView(scheduleBlock, params);

        // 데이터 리스트에 저장
        ScheduleData newScheduleData = new ScheduleData(dayIndex, startHour, startMinute, endHour, endMinute, subjectName, location);
        scheduleList.add(newScheduleData);

        Toast.makeText(this, "수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
        bottomSheetDialog.dismiss();
    }

    private RelativeLayout.LayoutParams calculateBlockParams(int dayIndex, int startH, int startM, int endH, int endM) {
        int left_margin_dp = 40;
        int dayWidth = (getResources().getDisplayMetrics().widthPixels - dpToPx(left_margin_dp)) / 5;

        int left = dpToPx(left_margin_dp) + dayWidth * dayIndex;

        // 24시간 시간표용 계산
        float minuteHeight_dp = 40.0f / 60.0f;

        float top_dp = startH * 60 * minuteHeight_dp + startM * minuteHeight_dp;
        float height_dp = ((endH * 60 + endM) - (startH * 60 + startM)) * minuteHeight_dp;

        int top = dpToPx(top_dp);
        int height = dpToPx(height_dp);

        Log.d("TimeTable", "dayIndex: " + dayIndex + ", left: " + left + ", top: " + top + ", width: " + dayWidth + ", height: " + height);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dayWidth, height);
        params.leftMargin = left;
        params.topMargin = top;

        return params;
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}