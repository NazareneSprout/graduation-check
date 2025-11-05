package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sprout.app.sakmvp1.models.Student;

/**
 * 학생 데이터 조회 Activity (관리자용)
 */
public class StudentDataActivity extends AppCompatActivity {

    private static final String TAG = "StudentDataActivity";

    private MaterialToolbar toolbar;
    private Spinner spinnerStudentYear, spinnerDepartment, spinnerTrack;
    private MaterialButton btnSearch;
    private ProgressBar progressBar;
    private RecyclerView rvStudents;
    private TextView tvStudentCount;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StudentAdapter adapter;

    // 전체 학생 데이터
    private List<Student> allStudents = new ArrayList<>();

    // 스피너 어댑터
    private ArrayAdapter<String> studentYearAdapter;
    private ArrayAdapter<String> departmentAdapter;
    private ArrayAdapter<String> trackAdapter;

    // 학과별 트랙 매핑
    private Map<String, Set<String>> departmentTrackMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_data);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSpinners();
        setupListeners();

        // 초기 전체 데이터 로드
        loadAllStudents();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerStudentYear = findViewById(R.id.spinner_student_year);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        btnSearch = findViewById(R.id.btn_search);
        progressBar = findViewById(R.id.progress_bar);
        rvStudents = findViewById(R.id.rv_students);
        tvStudentCount = findViewById(R.id.tv_student_count);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new StudentAdapter();
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(adapter);

        adapter.setOnItemClickListener(student -> {
            // 학생 상세 정보 화면으로 이동
            Intent intent = new Intent(this, StudentDetailActivity.class);
            intent.putExtra(StudentDetailActivity.EXTRA_STUDENT, student);
            startActivity(intent);
        });
    }

    private void setupSpinners() {
        // 초기 어댑터 설정 (빈 리스트)
        studentYearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        studentYearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStudentYear.setAdapter(studentYearAdapter);

        departmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        trackAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);
    }

    private void setupListeners() {
        // 학과 선택 시 트랙 업데이트
        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDepartment = parent.getItemAtPosition(position).toString();
                updateTrackSpinner(selectedDepartment);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 검색 버튼
        btnSearch.setOnClickListener(v -> filterStudents());
    }

    /**
     * 전체 학생 로드 (users 컬렉션에서)
     */
    private void loadAllStudents() {
        showLoading(true);

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allStudents.clear();
                    Set<String> years = new HashSet<>();
                    Set<String> departments = new HashSet<>();
                    Set<String> tracks = new HashSet<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Student student = new Student();
                        student.setUserId(document.getId());

                        // Firestore 필드 값 로깅
                        String studentYear = document.getString("studentYear");
                        String department = document.getString("department");
                        String track = document.getString("track");
                        String name = document.getString("name");
                        String email = document.getString("email");

                        Log.d(TAG, "========== 사용자 데이터 로드 ==========");
                        Log.d(TAG, "userId: " + document.getId());
                        Log.d(TAG, "name: " + (name != null ? name : "NULL"));
                        Log.d(TAG, "email: " + (email != null ? email : "NULL"));
                        Log.d(TAG, "studentYear: " + (studentYear != null ? studentYear : "NULL"));
                        Log.d(TAG, "department: " + (department != null ? department : "NULL"));
                        Log.d(TAG, "track: " + (track != null ? track : "NULL"));

                        student.setStudentYear(studentYear);
                        student.setDepartment(department);
                        student.setTrack(track);
                        student.setName(name);  // name이 null이면 Student.getName()에서 "이름 없음" 반환
                        student.setEmail(email);

                        Long updatedAt = document.getLong("updatedAt");
                        if (updatedAt != null) {
                            student.setUpdatedAt(updatedAt);
                        }

                        // 졸업요건 검사 이력 데이터 로드
                        Long lastCheckDate = document.getLong("lastGraduationCheckDate");
                        if (lastCheckDate != null) {
                            student.setLastGraduationCheckDate(lastCheckDate);
                            student.setHasGraduationCheckHistory(true);
                        } else {
                            student.setHasGraduationCheckHistory(false);
                        }

                        allStudents.add(student);

                        // 스피너 옵션 수집
                        if (student.getStudentYear() != null) {
                            years.add(student.getStudentYear());
                        }
                        if (student.getDepartment() != null) {
                            departments.add(student.getDepartment());

                            // 학과별 트랙 매핑 구축
                            if (student.getTrack() != null) {
                                if (!departmentTrackMap.containsKey(student.getDepartment())) {
                                    departmentTrackMap.put(student.getDepartment(), new HashSet<>());
                                }
                                departmentTrackMap.get(student.getDepartment()).add(student.getTrack());
                            }
                        }
                        if (student.getTrack() != null) {
                            tracks.add(student.getTrack());
                        }
                    }

                    Log.d(TAG, "전체 학생 로드: " + allStudents.size() + "명");
                    updateStudentCount(allStudents.size());

                    // 스피너 데이터 설정
                    updateSpinnerData(years, departments, tracks);

                    // 로딩 종료
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "학생 데이터 로드 실패", e);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 스피너 데이터 업데이트
     */
    private void updateSpinnerData(Set<String> years, Set<String> departments, Set<String> tracks) {
        // 학번 스피너 (4자리 -> 2자리 변환)
        List<String> yearList = new ArrayList<>();
        yearList.add("전체");
        for (String year : years) {
            if (year != null && year.length() == 4) {
                yearList.add(year.substring(2)); // "2023" -> "23"
            } else {
                yearList.add(year);
            }
        }
        studentYearAdapter.clear();
        studentYearAdapter.addAll(yearList);
        studentYearAdapter.notifyDataSetChanged();

        // 학과 스피너
        List<String> departmentList = new ArrayList<>();
        departmentList.add("전체");
        departmentList.addAll(new ArrayList<>(departments));
        departmentAdapter.clear();
        departmentAdapter.addAll(departmentList);
        departmentAdapter.notifyDataSetChanged();

        // 트랙 스피너는 초기에 "전체"만 표시
        trackAdapter.clear();
        trackAdapter.add("전체");
        trackAdapter.notifyDataSetChanged();
    }

    /**
     * 선택한 학과에 따라 트랙 스피너 업데이트
     */
    private void updateTrackSpinner(String selectedDepartment) {
        trackAdapter.clear();
        trackAdapter.add("전체");

        if (!"전체".equals(selectedDepartment) && departmentTrackMap.containsKey(selectedDepartment)) {
            Set<String> tracks = departmentTrackMap.get(selectedDepartment);
            if (tracks != null) {
                trackAdapter.addAll(new ArrayList<>(tracks));
            }
        } else if ("전체".equals(selectedDepartment)) {
            // "전체" 선택 시 모든 트랙 표시
            Set<String> allTracks = new HashSet<>();
            for (Set<String> tracks : departmentTrackMap.values()) {
                allTracks.addAll(tracks);
            }
            trackAdapter.addAll(new ArrayList<>(allTracks));
        }

        trackAdapter.notifyDataSetChanged();
    }

    /**
     * 선택된 조건으로 필터링
     */
    private void filterStudents() {
        String selectedYear = spinnerStudentYear.getSelectedItem() != null ?
                spinnerStudentYear.getSelectedItem().toString() : "전체";
        String selectedDepartment = spinnerDepartment.getSelectedItem() != null ?
                spinnerDepartment.getSelectedItem().toString() : "전체";
        String selectedTrack = spinnerTrack.getSelectedItem() != null ?
                spinnerTrack.getSelectedItem().toString() : "전체";

        // 2자리 학번을 4자리로 변환 ("23" -> "2023")
        if (!"전체".equals(selectedYear) && selectedYear.length() == 2) {
            selectedYear = "20" + selectedYear;
        }

        Log.d(TAG, "필터링: " + selectedYear + "/" + selectedDepartment + "/" + selectedTrack);

        List<Student> filtered = new ArrayList<>();

        for (Student student : allStudents) {
            boolean matches = true;

            if (!"전체".equals(selectedYear) && !selectedYear.equals(student.getStudentYear())) {
                matches = false;
            }
            if (!"전체".equals(selectedDepartment) && !selectedDepartment.equals(student.getDepartment())) {
                matches = false;
            }
            if (!"전체".equals(selectedTrack) && !selectedTrack.equals(student.getTrack())) {
                matches = false;
            }

            if (matches) {
                filtered.add(student);
            }
        }

        Log.d(TAG, "필터링 결과: " + filtered.size() + "명");
        displayResults(filtered);
    }

    /**
     * 검색 결과 표시
     */
    private void displayResults(List<Student> students) {
        showLoading(false);

        if (students.isEmpty()) {
            rvStudents.setVisibility(View.GONE);
            Toast.makeText(this, "조회된 학생이 없습니다", Toast.LENGTH_SHORT).show();
        } else {
            rvStudents.setVisibility(View.VISIBLE);
            adapter.setStudents(students);
            updateStudentCount(students.size());
        }
    }

    /**
     * 학생 수 표시 업데이트
     */
    private void updateStudentCount(int count) {
        tvStudentCount.setText("총 " + count + "명의 학생이 등록되어 있습니다");
    }

    /**
     * 로딩 표시 제어
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvStudents.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }
}
