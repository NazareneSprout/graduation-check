package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
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

/**
 * 졸업 요건 분석 진입 화면
 *
 * 역할
 * - 학번(연도), 학부, 트랙을 선택받아 유효성을 검증
 * - 선택값으로 Firestore의 졸업요건이 존재하는지 1차 확인
 * - 이후 추가 요건 입력 화면(AdditionalRequirementsActivity)로 이동
 *
 * 특징
 * - 학번은 UI에서 2자리 표기(예: "25"), 내부 로직에서 4자리로 역변환(예: "2025")
 * - 스피너는 안내문구 없이 실제 데이터만 표시하는 CleanArrayAdapter 사용
 * - Edge-to-Edge + 고대비 테마 적용
 */
public class GraduationAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "GraduationAnalysis";

    // ---------- UI ----------
    private Spinner spinnerStudentId;    // 학번(연도) 선택 (UI: 2자리, 내부: 4자리로 변환)
    private Spinner spinnerDepartment;   // 학부 선택
    private Spinner spinnerTrack;        // 트랙 선택
    private Button btnStartAnalysis;     // 분석 시작 버튼
    private Toolbar toolbar;             // 툴바
    private FrameLayout loadingContainer; // 개선된 로딩 컨테이너
    private ProgressBar progressBar;     // 초기 로딩 표시
    private TextView loadingText;        // 로딩 메시지
    private TextView loadingDescription; // 로딩 상세 설명

    // ---------- Data ----------
    private FirebaseDataManager dataManager;

    // 초기 데이터 로딩 상태 추적
    private boolean studentYearsLoaded = false;
    private boolean departmentsLoaded = false;
    private List<String> loadedStudentYears = null;
    private List<String> loadedDepartments = null;

    // 스피너 어댑터(실데이터 전용)
    private CleanArrayAdapter<String> studentIdAdapter;
    private CleanArrayAdapter<String> departmentAdapter;
    private CleanArrayAdapter<String> trackAdapter;

    /**
     * 깔끔한 스피너 어댑터
     * - 안내 문구(placeholder) 없이 실제 데이터만 다룸
     */
    private static class CleanArrayAdapter<T> extends ArrayAdapter<T> {
        public CleanArrayAdapter(android.content.Context context, int resource) {
            super(context, resource);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 접근성: 고대비 테마 적용
        HighContrastHelper.applyHighContrastTheme(this);

        // 시스템 창 컨텐츠가 status/navigation bar 아래로 확장되도록
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_graduation_analysis);

        // 시스템 바 인셋만큼 상단/하단 패딩 적용 (툴바 겹침 방지)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 툴바 설정
        toolbar = findViewById(R.id.toolbar_graduation_analysis);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("졸업 요건 분석");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Firebase 데이터 매니저
        dataManager = FirebaseDataManager.getInstance();

        // 뷰 바인딩 & 스피너 어댑터 초기화
        initSpinners();
        setupSpinnerData();
    }

    /** View 참조 및 기본 UI 상태 설정 */
    private void initSpinners() {
        spinnerStudentId = findViewById(R.id.spinner_student_id);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        btnStartAnalysis = findViewById(R.id.btn_start_analysis);

        // 개선된 로딩 UI 요소들
        loadingContainer = findViewById(R.id.loading_container);
        progressBar = findViewById(R.id.progress_bar);
        loadingText = findViewById(R.id.loading_text);
        loadingDescription = findViewById(R.id.loading_description);

        // 최초엔 로딩 컨테이너 숨김
        loadingContainer.setVisibility(View.GONE);
    }

    /** 스피너 어댑터 구성, 데이터 로드, 리스너 연결 */
    private void setupSpinnerData() {
        // 학번 어댑터
        studentIdAdapter = new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        studentIdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStudentId.setAdapter(studentIdAdapter);

        // 학부 어댑터
        departmentAdapter = new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        // 트랙 어댑터
        trackAdapter = new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);

        // 초기 데이터 로드 (학번/학부)
        loadAllInitialData();

        // 스피너 상호작용 설정
        setupSpinnerListeners();

        // 분석 시작
        btnStartAnalysis.setOnClickListener(v -> startGraduationAnalysis());
    }

    /** 초기 로딩(학번 + 학부) */
    private void loadAllInitialData() {
        // 로딩 상태 초기화
        studentYearsLoaded = false;
        departmentsLoaded = false;
        loadedStudentYears = null;
        loadedDepartments = null;

        showLoading(true, "초기 데이터 로딩 중...", "학번과 학부 정보를 가져오고 있어요");
        loadStudentYears();  // 학번(연도) 목록
        loadDepartments();   // 학부 목록
    }

    /** 모든 초기 데이터 로딩 완료 시 UI 업데이트 */
    private void checkAndUpdateInitialDataUI() {
        if (studentYearsLoaded && departmentsLoaded) {
            // 학번 데이터 적용
            studentIdAdapter.clear();
            if (loadedStudentYears != null) {
                // 4자리 -> 2자리 변환 (예: "2025" -> "25")
                List<String> shortYears = new ArrayList<>();
                for (String year : loadedStudentYears) {
                    if (year.length() >= 4) {
                        shortYears.add(year.substring(2));
                    } else {
                        shortYears.add(year);
                    }
                }
                studentIdAdapter.addAll(shortYears);
            }
            studentIdAdapter.notifyDataSetChanged();

            // 학부 데이터 적용
            departmentAdapter.clear();
            if (loadedDepartments != null) {
                departmentAdapter.addAll(loadedDepartments);
            }
            departmentAdapter.notifyDataSetChanged();

            showLoading(false);
            Log.d(TAG, "모든 초기 데이터 로딩 완료 - UI 업데이트");
        }
    }

    /**
     * 학번(연도) 목록 로드
     * - Firestore 문서ID에서 연도 추출
     * - UI는 2자리(예: 2025 -> "25")로 보여주고, 분석 시 다시 4자리로 복구
     */
    private void loadStudentYears() {
        dataManager.loadStudentYears(new FirebaseDataManager.OnStudentYearsLoadedListener() {
            @Override
            public void onSuccess(List<String> years) {
                loadedStudentYears = years;
                studentYearsLoaded = true;
                Log.d(TAG, "학번 데이터 로드 성공: " + years.size() + "개 (UI 업데이트 대기 중)");
                checkAndUpdateInitialDataUI();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "학번 데이터 로드 실패", e);
                studentYearsLoaded = true; // 실패해도 다른 데이터는 표시하기 위해
                checkAndUpdateInitialDataUI();
                Toast.makeText(GraduationAnalysisActivity.this,
                        "학번 데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 학부 목록 로드
     * - graduation_requirements 문서ID의 prefix에서 고유 학부 추출
     */
    private void loadDepartments() {
        dataManager.loadDepartments(new FirebaseDataManager.OnDepartmentsLoadedListener() {
            @Override
            public void onSuccess(List<String> departments) {
                loadedDepartments = departments;
                departmentsLoaded = true;
                Log.d(TAG, "학부 데이터 로드 성공: " + departments.size() + "개 (UI 업데이트 대기 중)");
                checkAndUpdateInitialDataUI();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "학부 데이터 로드 실패", e);
                departmentsLoaded = true; // 실패해도 다른 데이터는 표시하기 위해
                checkAndUpdateInitialDataUI();
                Toast.makeText(GraduationAnalysisActivity.this,
                        "학부 데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 스피너 상호작용
     * - 학부 선택 → 해당 학부의 트랙 재조회
     */
    private void setupSpinnerListeners() {
        spinnerDepartment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0) {
                    String selectedDepartment = departmentAdapter.getItem(position);
                    loadTracksByDepartment(selectedDepartment);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    /**
     * 특정 학부의 트랙 목록 로드
     */
    private void loadTracksByDepartment(String departmentName) {
        dataManager.loadTracksByDepartment(departmentName, new FirebaseDataManager.OnTracksLoadedListener() {
            @Override
            public void onSuccess(List<String> tracks) {
                trackAdapter.clear();
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

    /**
     * [분석 시작] 클릭 처리
     * - 필수 선택값 검증
     * - 선택값으로 졸업요건 문서 존재 여부를 확인(간단 조회)
     * - 성공 시 AdditionalRequirementsActivity로 전환
     */
    private void startGraduationAnalysis() {
        // 어댑터가 placeholder 없이 실제 데이터만 담으므로 위치로 검증
        if (spinnerStudentId.getSelectedItemPosition() == -1 ||
                spinnerDepartment.getSelectedItemPosition() == -1 ||
                spinnerTrack.getSelectedItemPosition() == -1) {
            Toast.makeText(this, "모든 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // UI는 2자리(예: "25"), 실제 Firestore는 4자리("2025")
        final String selectedShortYear = spinnerStudentId.getSelectedItem().toString();
        final String selectedDepartment = spinnerDepartment.getSelectedItem().toString();
        final String selectedTrack = spinnerTrack.getSelectedItem().toString();
        final String selectedYear = (selectedShortYear.length() == 2) ? ("20" + selectedShortYear) : selectedShortYear;

        showLoading(true, "졸업 요건 확인 중...", "선택하신 조건의 졸업 요건을 확인하고 있어요");
        btnStartAnalysis.setEnabled(false);

        // 졸업 요건 문서 존재 여부 확인 (성공 시 다음 화면 이동)
        dataManager.loadGraduationRequirements(selectedDepartment, selectedTrack, selectedYear,
                new FirebaseDataManager.OnGraduationRequirementsLoadedListener() {
                    @Override
                    public void onSuccess(Map<String, Object> requirements) {
                        showLoading(false);
                        btnStartAnalysis.setEnabled(true);

                        // 추가 요건 입력 화면으로 이동
                        android.content.Intent intent = new android.content.Intent(
                                GraduationAnalysisActivity.this,
                                AdditionalRequirementsActivity.class
                        );
                        intent.putExtra("year", selectedYear);
                        intent.putExtra("department", selectedDepartment);
                        intent.putExtra("track", selectedTrack);
                        startActivity(intent);

                        Log.d(TAG, "졸업 요건 로드 성공: " + requirements);
                        Toast.makeText(GraduationAnalysisActivity.this,
                                "졸업 요건 분석 준비 완료!\n" + selectedDepartment + " " + selectedTrack + " " + selectedShortYear + "학번",
                                Toast.LENGTH_SHORT).show();
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

    /** 개선된 로딩 UI 표시/숨김 */
    private void showLoading(boolean show) {
        showLoading(show, "데이터를 불러오는 중...", "잠시만 기다려주세요");
    }

    /** 로딩 UI 표시/숨김 (커스텀 메시지) */
    private void showLoading(boolean show, String message, String description) {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show && loadingText != null && loadingDescription != null) {
                loadingText.setText(message);
                loadingDescription.setText(description);
            }
        }
    }
}
