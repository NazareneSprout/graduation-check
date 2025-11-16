package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference; // [필수 임포트]
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import sprout.app.sakmvp1.BaseActivity;
import sprout.app.sakmvp1.R;

public class CommonCalendarActivity extends BaseActivity {

    private TextView textMonthYear;
    private RecyclerView recyclerCalendar;
    private LocalDate selectedDate;
    private FloatingActionButton fabAddEvent;
    private ImageButton btnPrevMonth, btnNextMonth;

    private FirebaseFirestore db;
    private String userId; // 내 ID
    private String currentCalendarId; // 현재 보고 있는 캘린더 ID (내 ID 혹은 그룹 ID)
    private List<CalendarEvent> allEvents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_calendar);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else { finish(); return; }

        // Intent로 groupId 받기
        String groupId = getIntent().getStringExtra("groupId");
        if (groupId != null) {
            currentCalendarId = groupId; // 그룹 캘린더
        } else {
            currentCalendarId = userId; // 내 캘린더
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        textMonthYear = findViewById(R.id.textMonthYear);
        recyclerCalendar = findViewById(R.id.recyclerCalendar);
        fabAddEvent = findViewById(R.id.fabAddEvent);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        selectedDate = LocalDate.now();

        // 새 글 쓰기
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarDetailActivity.class);
            intent.putExtra("selected_date", selectedDate.toString());
            intent.putExtra("IS_GROUP_CALENDAR", !currentCalendarId.equals(userId));
            intent.putExtra("CALENDAR_ID_TO_SAVE", currentCalendarId);
            startActivity(intent);
        });

        btnPrevMonth.setOnClickListener(v -> {
            selectedDate = selectedDate.minusMonths(1);
            updateCalendarView();
        });
        btnNextMonth.setOnClickListener(v -> {
            selectedDate = selectedDate.plusMonths(1);
            updateCalendarView();
        });
        textMonthYear.setOnClickListener(v -> showYearMonthPicker());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsFromFirestore(); // 화면 돌아올 때마다 새로고침
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * [수정됨] Firestore 로드 경로를 올바르게 수정
     */
    private void loadEventsFromFirestore() {
        CollectionReference eventsCollectionRef; // [수정] 경로를 담을 변수

        if (currentCalendarId.equals(userId)) {
            // "내 캘린더": users -> {내ID} -> calendar_events
            eventsCollectionRef = db.collection("users").document(userId).collection("calendar_events");
        } else {
            // "그룹 캘린더": groups -> {그룹ID} -> calendar_events
            eventsCollectionRef = db.collection("groups").document(currentCalendarId).collection("calendar_events");
        }

        // [수정] 올바른 경로(eventsCollectionRef)에서 데이터 가져오기
        eventsCollectionRef.get()
                .addOnSuccessListener(qs -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        CalendarEvent ev = doc.toObject(CalendarEvent.class);
                        ev.documentId = doc.getId();
                        allEvents.add(ev);
                    }
                    updateCalendarView(); // 캘린더 UI 갱신
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "일정을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    allEvents.clear();
                    updateCalendarView();
                });
    }

    private void updateCalendarView() {
        textMonthYear.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월")));
        ArrayList<LocalDate> days = daysInMonthArray(selectedDate);

        CalendarAdapter adapter = new CalendarAdapter(days, allEvents, event -> {
            // 일정 수정 화면으로 이동
            Intent intent = new Intent(this, CalendarDetailActivity.class);
            intent.putExtra("event_data", event);
            // 수정 시에도 이 이벤트가 어느 캘린더 소속인지 알려줘야 함
            intent.putExtra("IS_GROUP_CALENDAR", !currentCalendarId.equals(userId));
            intent.putExtra("CALENDAR_ID_TO_SAVE", currentCalendarId);
            startActivity(intent);
        });
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        recyclerCalendar.setAdapter(adapter);
    }

    // (showYearMonthPicker, daysInMonthArray 메서드는 변경 없음)
    private void showYearMonthPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_year_month_picker, null);
        builder.setView(view);
        NumberPicker pickerYear = view.findViewById(R.id.pickerYear);
        NumberPicker pickerMonth = view.findViewById(R.id.pickerMonth);
        int cy = selectedDate.getYear();
        pickerYear.setMinValue(cy - 10); pickerYear.setMaxValue(cy + 10); pickerYear.setValue(cy);
        pickerMonth.setMinValue(1); pickerMonth.setMaxValue(12); pickerMonth.setValue(selectedDate.getMonthValue());

        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            selectedDate = LocalDate.of(pickerYear.getValue(), pickerMonth.getValue(), 1);
            updateCalendarView();
            dialog.dismiss();
        });
        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private ArrayList<LocalDate> daysInMonthArray(LocalDate date) {
        ArrayList<LocalDate> list = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);
        int len = yearMonth.lengthOfMonth();
        LocalDate first = selectedDate.withDayOfMonth(1);
        int padding = (first.getDayOfWeek().getValue() == 7) ? 0 : first.getDayOfWeek().getValue();
        for (int i = 0; i < padding; i++) list.add(null);
        for (int i = 1; i <= len; i++) list.add(LocalDate.of(selectedDate.getYear(), selectedDate.getMonth(), i));
        return list;
    }

}