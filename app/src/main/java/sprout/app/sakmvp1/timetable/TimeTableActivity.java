package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.material.appbar.MaterialToolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.UserProfileActivity;

public class TimeTableActivity extends AppCompatActivity {

    private RelativeLayout timetableLayout;
    private BottomNavigationView bottomNavigation;
    private MaterialToolbar toolbar;
    private FloatingActionButton fabAddSchedule;

    // 시간 선택을 위한 변수
    private int startHour = 9, startMinute = 0, endHour = 10, endMinute = 0;

    // 시간표의 시간 범위
    private static final int START_TIME_HOUR = 9;
    private static final int END_TIME_HOUR = 24;

    // --- Firebase Firestore 관련 변수 ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration scheduleListener; // 실시간 리스너를 제어하기 위한 변수
    private CollectionReference userSchedulesCollection; // 사용자별 시간표 컬렉션을 가리키는 변수

    // 수업 정보를 저장할 데이터 리스트 (로컬 캐시 및 UI 관리용)
    private final ArrayList<ScheduleData> scheduleList = new ArrayList<>();
    // Firestore 문서 ID와 화면의 View를 매핑하여 관리
    private final Map<String, View> scheduleViewMap = new HashMap<>();

    // 수업 정보를 담을 POJO 클래스 (Firestore 연동용)
    public static class ScheduleData {
        @DocumentId // Firestore 문서 ID를 이 필드에 자동으로 매핑
        public String documentId;

        public int dayIndex; // 0=월, 1=화...
        public int startHour;
        public int startMinute;
        public int endHour;
        public int endMinute;
        public String subjectName;
        public String professorName;
        public String location;

        // Firestore가 데이터를 객체로 변환할 때 필요한 기본 생성자
        public ScheduleData() {}

        // 데이터를 생성할 때 사용할 생성자
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

        // 겹침 검사를 위한 유틸리티 메서드
        public int getStartTotalMinutes() { return startHour * 60 + startMinute; }
        public int getEndTotalMinutes() { return endHour * 60 + endMinute; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_table);

        // 툴바 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 뷰 초기화
        timetableLayout = findViewById(R.id.timetable_layout);
        fabAddSchedule = findViewById(R.id.fab_add_schedule);

        // Firebase 초기화
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firestore 서브컬렉션 경로 설정
        userSchedulesCollection = db.collection("schedules")
                .document(currentUser.getUid())
                .collection("user_schedules");

        // 시간표 기본 배경 그리기
        drawTimetableBase();

        // FAB 클릭 리스너 설정
        fabAddSchedule.setOnClickListener(v -> showAddScheduleBottomSheet());

        // 하단 내비게이션 설정
        setupBottomNavigation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 화면이 사용자에게 보일 때 데이터 로딩 및 실시간 감지를 시작
        loadSchedulesFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 시간표 화면으로 돌아올 때 네비게이션 상태 초기화
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_2);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 화면이 가려질 때 실시간 리스너를 중지하여 불필요한 리소스 사용 방지
        if (scheduleListener != null) {
            scheduleListener.remove();
        }
        clearTimetableViews(); // 화면 전환 시 뷰가 중복되지 않도록 초기화
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_2);

            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_button_1) {
                    // 홈 화면으로 이동
                    Intent intent = new Intent(this, sprout.app.sakmvp1.MainActivityNew.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_button_2) {
                    // 이미 시간표 화면이므로 아무 동작 안 함
                    return true;
                } else if (itemId == R.id.nav_button_3) {
                    Toast.makeText(this, "기능3 - 준비중", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_button_4) {
                    // 내 정보 화면으로 이동
                    Intent intent = new Intent(this, sprout.app.sakmvp1.UserProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
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
            String subjectName = editSubjectName.getText() != null ? editSubjectName.getText().toString().trim() : "";
            String professorName = editProfessorName.getText() != null ? editProfessorName.getText().toString().trim() : "";
            String location = editLocation.getText() != null ? editLocation.getText().toString().trim() : "";
            int dayIndex = spinnerDayOfWeek.getSelectedItemPosition();

            if (subjectName.isEmpty()) {
                Toast.makeText(this, "수업명을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            ScheduleData newSchedule = new ScheduleData(dayIndex, startHour, startMinute, endHour, endMinute, subjectName, professorName, location);

            // 겹침 검사
            if (checkOverlap(newSchedule)) {
                Toast.makeText(this, "⚠️ 기존 수업과 시간이 겹칩니다!", Toast.LENGTH_LONG).show();
                return;
            }

            // 수업 추가 (Firestore에 저장)
            saveScheduleToFirestore(newSchedule);
            bottomSheetDialog.dismiss();
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

        TextView scheduleBlock = new TextView(this);
        String professorName = scheduleData.professorName;
        scheduleBlock.setText(scheduleData.subjectName + "\n" + scheduleData.location + "\n" + professorName);
        scheduleBlock.setTextColor(Color.WHITE);
        scheduleBlock.setGravity(Gravity.CENTER);
        scheduleBlock.setTextSize(12);
        scheduleBlock.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        // 과목명에 따라 일관된 색상을 지정하여 리로드 시에도 색이 유지되도록 함
        Random rnd = new Random(scheduleData.subjectName.hashCode());
        int color = Color.argb(200, rnd.nextInt(200), rnd.nextInt(200), rnd.nextInt(200));
        scheduleBlock.setBackgroundColor(color);

        // 블록의 위치와 크기 계산
        RelativeLayout.LayoutParams params = calculateBlockParams(scheduleData);
        timetableLayout.addView(scheduleBlock, params);

        // 맵에 뷰 저장
        scheduleViewMap.put(scheduleData.documentId, scheduleBlock);

        // 뷰를 길게 눌렀을 때 삭제 다이얼로그 표시
        scheduleBlock.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("'" + scheduleData.subjectName + "' 수업 삭제")
                    .setMessage("이 수업을 시간표에서 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        // Firestore 서브컬렉션 경로에서 문서 삭제
                        userSchedulesCollection.document(scheduleData.documentId).delete();
                    })
                    .setNegativeButton("취소", null)
                    .show();
            return true;
        });
    }

    private RelativeLayout.LayoutParams calculateBlockParams(ScheduleData schedule) {
        int left_margin_dp = 40; // 시간 표시 라벨 너비
        int dayWidth = (getResources().getDisplayMetrics().widthPixels - dpToPx(left_margin_dp)) / 5;

        int left = dpToPx(left_margin_dp) + dayWidth * schedule.dayIndex;

        // 1시간의 높이 = 50dp, 1분의 높이 = 50/60 dp
        float minuteHeight_dp = 50.0f / 60.0f;

        float top_dp = (schedule.startHour - START_TIME_HOUR) * 60 * minuteHeight_dp + schedule.startMinute * minuteHeight_dp;
        float height_dp = (schedule.getEndTotalMinutes() - schedule.getStartTotalMinutes()) * minuteHeight_dp;

        int top = dpToPx(top_dp);
        int height = dpToPx(height_dp);
        if(height < 0) height = 0; // 높이가 음수가 되지 않도록 방지

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dayWidth, height);
        params.leftMargin = left;
        params.topMargin = top;

        return params;
    }

    // --- Firestore 관련 메서드 ---

    private void saveScheduleToFirestore(ScheduleData scheduleData) {
        // 사용자별 서브컬렉션에 새로운 문서를 추가
        userSchedulesCollection.add(scheduleData)
                .addOnSuccessListener(documentReference -> Toast.makeText(TimeTableActivity.this, "수업이 추가되었습니다.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(TimeTableActivity.this, "저장에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadSchedulesFromFirestore() {
        // 사용자별 서브컬렉션의 변경을 실시간으로 감지
        scheduleListener = userSchedulesCollection.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e);
                return;
            }

            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                ScheduleData data = dc.getDocument().toObject(ScheduleData.class);
                switch (dc.getType()) {
                    case ADDED: // 데이터가 새로 추가되었을 때
                        if (!scheduleViewMap.containsKey(data.documentId)) {
                            scheduleList.add(data);
                            addScheduleBlockToView(data);
                        }
                        break;
                    case MODIFIED: // 데이터가 수정되었을 때 (필요 시 구현)
                        // 예: 기존 뷰를 제거하고 새로 그리기
                        break;
                    case REMOVED: // 데이터가 삭제되었을 때
                        View viewToRemove = scheduleViewMap.remove(data.documentId);
                        if (viewToRemove != null) {
                            timetableLayout.removeView(viewToRemove);
                        }
                        scheduleList.removeIf(schedule -> schedule.documentId.equals(data.documentId));
                        break;
                }
            }
        });
    }

    // 화면 전환 또는 리스너 중지 시 뷰를 정리하는 메서드
    private void clearTimetableViews() {
        for (View view : scheduleViewMap.values()) {
            timetableLayout.removeView(view);
        }
        scheduleViewMap.clear();
        scheduleList.clear();
    }
}