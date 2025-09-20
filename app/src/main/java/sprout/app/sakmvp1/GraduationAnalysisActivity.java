package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraduationAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "GraduationAnalysis";

    private Spinner spinnerStudentId;
    private Spinner spinnerDepartment;
    private Spinner spinnerTrack;
    private Button btnStartAnalysis;
    private Toolbar toolbar;
    private ProgressBar progressBar;

    private FirebaseDataManager dataManager;
    private ArrayAdapter<String> studentIdAdapter;
    private ArrayAdapter<String> departmentAdapter;
    private ArrayAdapter<String> trackAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_graduation_analysis);

        // 시스템 UI 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 툴바 설정
        toolbar = findViewById(R.id.toolbar_graduation_analysis);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("졸업 요건 분석");

        // 뒤로가기 버튼 클릭 리스너
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Firebase 데이터 매니저 초기화
        dataManager = new FirebaseDataManager();

        // 스피너 초기화
        initSpinners();
        setupSpinnerData();
    }

    private void initSpinners() {
        spinnerStudentId = findViewById(R.id.spinner_student_id);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        btnStartAnalysis = findViewById(R.id.btn_start_analysis);
        progressBar = findViewById(R.id.progress_bar);

        // 초기에는 프로그레스바 숨김
        progressBar.setVisibility(View.GONE);
    }

    private void setupSpinnerData() {
        // 학번 스피너 (Firebase에서 로드)
        List<String> studentIdList = new ArrayList<>();
        studentIdList.add("학번을 선택하세요");
        studentIdAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, studentIdList);
        studentIdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStudentId.setAdapter(studentIdAdapter);

        // 학부 스피너 (Firebase에서 로드)
        List<String> departmentList = new ArrayList<>();
        departmentList.add("학부를 선택하세요");
        departmentAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, departmentList);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        // 트랙 스피너 (학부 선택 후 로드)
        List<String> trackList = new ArrayList<>();
        trackList.add("트랙을 선택하세요");
        trackAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, trackList);
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);

        // Firebase에서 데이터 로드
        loadAllInitialData();

        // 스피너 선택 리스너
        setupSpinnerListeners();

        // 분석 시작 버튼 리스너
        btnStartAnalysis.setOnClickListener(v -> startGraduationAnalysis());
    }

    private void loadAllInitialData() {
        showLoading(true);

        // 학번 데이터 로드
        loadStudentYears();

        // 학부 데이터 로드
        loadDepartments();
    }

    private void loadStudentYears() {
        dataManager.loadStudentYears(new FirebaseDataManager.OnStudentYearsLoadedListener() {
            @Override
            public void onSuccess(List<String> years) {
                studentIdAdapter.clear();
                studentIdAdapter.add("학번을 선택하세요");
                studentIdAdapter.addAll(years);
                studentIdAdapter.notifyDataSetChanged();
                Log.d(TAG, "학번 데이터 로드 성공: " + years.size() + "개");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "학번 데이터 로드 실패", e);
                Toast.makeText(GraduationAnalysisActivity.this,
                    "학번 데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDepartments() {
        dataManager.loadDepartments(new FirebaseDataManager.OnDepartmentsLoadedListener() {
            @Override
            public void onSuccess(List<String> departments) {
                departmentAdapter.clear();
                departmentAdapter.add("학부를 선택하세요");
                departmentAdapter.addAll(departments);
                departmentAdapter.notifyDataSetChanged();
                Log.d(TAG, "학부 데이터 로드 성공: " + departments.size() + "개");
                showLoading(false);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "학부 데이터 로드 실패", e);
                Toast.makeText(GraduationAnalysisActivity.this,
                    "학부 데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void setupSpinnerListeners() {
        spinnerDepartment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) {
                    String selectedDepartment = departmentAdapter.getItem(position);
                    loadTracksByDepartment(selectedDepartment);
                } else {
                    // 기본값으로 초기화
                    trackAdapter.clear();
                    trackAdapter.add("트랙을 선택하세요");
                    trackAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadTracksByDepartment(String departmentName) {
        dataManager.loadTracksByDepartment(departmentName, new FirebaseDataManager.OnTracksLoadedListener() {
            @Override
            public void onSuccess(List<String> tracks) {
                trackAdapter.clear();
                trackAdapter.add("트랙을 선택하세요");
                trackAdapter.addAll(tracks);
                trackAdapter.notifyDataSetChanged();
                Log.d(TAG, departmentName + " 트랙 데이터 로드 성공: " + tracks.size() + "개");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "트랙 데이터 로드 실패", e);
                Toast.makeText(GraduationAnalysisActivity.this,
                    "트랙 데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void startGraduationAnalysis() {
        String selectedYear = spinnerStudentId.getSelectedItem().toString();
        String selectedDepartment = spinnerDepartment.getSelectedItem().toString();
        String selectedTrack = spinnerTrack.getSelectedItem().toString();

        if (selectedYear.equals("학번을 선택하세요") ||
            selectedDepartment.equals("학부를 선택하세요") ||
            selectedTrack.equals("트랙을 선택하세요")) {
            Toast.makeText(this, "모든 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        btnStartAnalysis.setEnabled(false);

        // Firebase에서 졸업 요건 조회
        dataManager.loadGraduationRequirements(selectedDepartment, selectedTrack, selectedYear,
            new FirebaseDataManager.OnGraduationRequirementsLoadedListener() {
                @Override
                public void onSuccess(Map<String, Object> requirements) {
                    showLoading(false);
                    btnStartAnalysis.setEnabled(true);

                    Log.d(TAG, "졸업 요건 로드 성공: " + requirements);
                    // TODO: 졸업 요건 결과 화면으로 이동
                    Toast.makeText(GraduationAnalysisActivity.this,
                        "졸업 요건 분석 완료!\n" + selectedDepartment + " " + selectedTrack + " " + selectedYear + "학번",
                        Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Exception e) {
                    showLoading(false);
                    btnStartAnalysis.setEnabled(true);

                    Log.e(TAG, "졸업 요건 로드 실패", e);
                    Toast.makeText(GraduationAnalysisActivity.this,
                        "해당 조건의 졸업 요건을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}