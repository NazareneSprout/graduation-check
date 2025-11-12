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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import sprout.app.sakmvp1.R;

public class CommonCalendarActivity extends AppCompatActivity {

    private TextView textMonthYear;
    private RecyclerView recyclerCalendar;
    private LocalDate selectedDate;
    private FloatingActionButton fabAddEvent;
    private ImageButton btnPrevMonth, btnNextMonth;

    private FirebaseFirestore db;
    private String userId;
    private List<CalendarEvent> allEvents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_calendar);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else { finish(); return; }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
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
        loadEventsFromFirestore();
    }

    private void loadEventsFromFirestore() {
        db.collection("users").document(userId).collection("calendar_events").get()
                .addOnSuccessListener(qs -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        CalendarEvent ev = doc.toObject(CalendarEvent.class);
                        ev.documentId = doc.getId();
                        allEvents.add(ev);
                    }
                    updateCalendarView();
                });
    }

    private void updateCalendarView() {
        textMonthYear.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월")));
        ArrayList<LocalDate> days = daysInMonthArray(selectedDate);
        // 어댑터 클릭 시 -> 수정 화면 이동
        CalendarAdapter adapter = new CalendarAdapter(days, allEvents, event -> {
            Intent intent = new Intent(this, CalendarDetailActivity.class);
            intent.putExtra("event_data", event);
            startActivity(intent);
        });
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        recyclerCalendar.setAdapter(adapter);
    }

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
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