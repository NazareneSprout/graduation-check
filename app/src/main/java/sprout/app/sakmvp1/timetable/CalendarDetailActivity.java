package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
// [중요] XML과 맞추기 위해 SwitchCompat 사용
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random; // 랜덤 색상용

import sprout.app.sakmvp1.R;

public class CalendarDetailActivity extends AppCompatActivity {

    private EditText editTitle, editDesc;
    private TextView btnStartDate, btnStartTime, btnEndDate, btnEndTime;
    private SwitchCompat switchYearly; // [수정됨] MaterialSwitch -> SwitchCompat
    private MaterialButton btnSave, btnDelete;

    private CalendarEvent currentEvent;
    private LocalDate tempStartDate, tempEndDate;
    private int startHour = 20, startMinute = 0;
    private int endHour = 21, endMinute = 0;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_detail);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else { finish(); return; }

        // 툴바 설정
        MaterialToolbar toolbar = findViewById(R.id.detailToolbar);
        toolbar.setNavigationOnClickListener(v -> finish()); // 뒤로가기 버튼

        // 뷰 연결
        editTitle = findViewById(R.id.detailTitle);
        editDesc = findViewById(R.id.detailDesc);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnEndTime = findViewById(R.id.btnEndTime);

        // [수정됨] XML의 SwitchCompat ID와 연결
        switchYearly = findViewById(R.id.switchYearly);

        btnSave = findViewById(R.id.btnDetailSave);
        btnDelete = findViewById(R.id.btnDetailDelete);

        // 데이터 초기화 (수정 vs 신규)
        if (getIntent().hasExtra("event_data")) {
            // [수정 모드]
            currentEvent = (CalendarEvent) getIntent().getSerializableExtra("event_data");
            editTitle.setText(currentEvent.title);
            editDesc.setText(currentEvent.description);
            switchYearly.setChecked(currentEvent.isYearly);

            tempStartDate = LocalDate.parse(currentEvent.startDate);
            tempEndDate = LocalDate.parse(currentEvent.endDate);
            parseTime(currentEvent.startTime, true);
            parseTime(currentEvent.endTime, false);

            btnDelete.setVisibility(View.VISIBLE);
            toolbar.setTitle("이벤트 수정");
            btnSave.setText("수정");
        } else {
            // [신규 모드]
            String selectedDateStr = getIntent().getStringExtra("selected_date");
            tempStartDate = (selectedDateStr != null) ? LocalDate.parse(selectedDateStr) : LocalDate.now();
            tempEndDate = tempStartDate;

            btnDelete.setVisibility(View.GONE);
            toolbar.setTitle("신규 이벤트");
        }

        updateUI();

        // 리스너 설정
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnStartTime.setOnClickListener(v -> showTimePicker(true));
        btnEndTime.setOnClickListener(v -> showTimePicker(false));

        btnSave.setOnClickListener(v -> saveEvent());

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this).setMessage("삭제하시겠습니까?")
                    .setPositiveButton("삭제", (d, w) -> deleteEvent())
                    .setNegativeButton("취소", null).show();
        });
    }

    private void parseTime(String timeStr, boolean isStart) {
        try {
            String[] t = timeStr.split(":");
            if (isStart) {
                startHour = Integer.parseInt(t[0]); startMinute = Integer.parseInt(t[1]);
            } else {
                endHour = Integer.parseInt(t[0]); endMinute = Integer.parseInt(t[1]);
            }
        } catch (Exception e) { }
    }

    private void updateUI() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy. MM. dd.");
        btnStartDate.setText(tempStartDate.format(dtf));
        btnEndDate.setText(tempEndDate.format(dtf));
        btnStartTime.setText(String.format("%02d:%02d", startHour, startMinute));
        btnEndTime.setText(String.format("%02d:%02d", endHour, endMinute));
    }

    private void showDatePicker(boolean isStart) {
        LocalDate target = isStart ? tempStartDate : tempEndDate;
        new DatePickerDialog(this, (view, y, m, d) -> {
            if (isStart) tempStartDate = LocalDate.of(y, m + 1, d);
            else tempEndDate = LocalDate.of(y, m + 1, d);
            updateUI();
        }, target.getYear(), target.getMonthValue()-1, target.getDayOfMonth()).show();
    }

    // 커스텀 스피너(Wheel) 시간 선택 다이얼로그
    private void showTimePicker(boolean isStart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_time_spinner, null);
        builder.setView(view);

        NumberPicker pickerHour = view.findViewById(R.id.pickerHour);
        NumberPicker pickerMinute = view.findViewById(R.id.pickerMinute);

        pickerHour.setMinValue(0); pickerHour.setMaxValue(23);
        pickerHour.setFormatter(v -> String.format("%02d", v));
        pickerMinute.setMinValue(0); pickerMinute.setMaxValue(59);
        pickerMinute.setFormatter(v -> String.format("%02d", v));

        if (isStart) { pickerHour.setValue(startHour); pickerMinute.setValue(startMinute); }
        else { pickerHour.setValue(endHour); pickerMinute.setValue(endMinute); }

        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            if (isStart) { startHour = pickerHour.getValue(); startMinute = pickerMinute.getValue(); }
            else { endHour = pickerHour.getValue(); endMinute = pickerMinute.getValue(); }
            updateUI();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveEvent() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tempEndDate.isBefore(tempStartDate)) {
            Toast.makeText(this, "종료 날짜가 시작 날짜보다 빠릅니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String startTimeStr = String.format("%02d:%02d", startHour, startMinute);
        String endTimeStr = String.format("%02d:%02d", endHour, endMinute);
        boolean isYearly = switchYearly.isChecked();

        if (currentEvent == null) {
            // [신규 추가] -> 랜덤 파스텔 색상 생성
            Random rnd = new Random();
            int randomColor = Color.argb(255, rnd.nextInt(50) + 180, rnd.nextInt(50) + 180, rnd.nextInt(50) + 180);

            CalendarEvent newEvent = new CalendarEvent(title, tempStartDate.toString(), tempEndDate.toString(),
                    startTimeStr, endTimeStr, editDesc.getText().toString(), randomColor, isYearly);

            db.collection("users").document(userId).collection("calendar_events").add(newEvent)
                    .addOnSuccessListener(doc -> finish());
        } else {
            // [수정] -> 기존 색상 유지
            db.collection("users").document(userId).collection("calendar_events").document(currentEvent.documentId)
                    .update("title", title, "description", editDesc.getText().toString(),
                            "startDate", tempStartDate.toString(), "endDate", tempEndDate.toString(),
                            "startTime", startTimeStr, "endTime", endTimeStr,
                            "isYearly", isYearly,
                            "color", currentEvent.color)
                    .addOnSuccessListener(v -> finish());
        }
    }

    private void deleteEvent() {
        if (currentEvent == null) return;
        db.collection("users").document(userId).collection("calendar_events")
                .document(currentEvent.documentId).delete().addOnSuccessListener(v -> finish());
    }
}