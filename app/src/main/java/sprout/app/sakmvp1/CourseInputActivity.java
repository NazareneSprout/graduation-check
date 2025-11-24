package sprout.app.sakmvp1;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 수강 강의 입력 메인 화면
 *
 * <p>이 화면은 사용자가 자신의 수강 강의를 카테고리별로 입력하고 관리할 수 있는
 * 핵심 기능을 제공합니다. 전공과 교양으로 나누어 세분화된 카테고리별로
 * 강의를 추가/삭제할 수 있으며, 실시간으로 진도율을 확인할 수 있습니다.</p>
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><strong>카테고리 관리:</strong> 전공(필수/선택/심화/공통) 및 교양(필수/선택) 분류</li>
 *   <li><strong>강의 추가:</strong> 다이얼로그를 통한 직관적인 강의 선택 및 추가</li>
 *   <li><strong>실시간 집계:</strong> 각 카테고리별 이수 학점 및 필수 학점 자동 계산</li>
 *   <li><strong>진도율 시각화:</strong> 도넛 차트를 통한 직관적인 졸업 진행 상황 표시</li>
 * </ul>
 *
 * <h3>성능 최적화:</h3>
 * <ul>
 *   <li><strong>In-Flight 요청 병합:</strong> 동일한 강의 데이터 요청 시 네트워크 호출 병합</li>
 *   <li><strong>500ms 로드 간격:</strong> 버튼 연속 클릭 방지 및 성능 최적화</li>
 *   <li><strong>2초 버튼 가드:</strong> 중복 제출 방지 시스템</li>
 *   <li><strong>캐시 우선 전략:</strong> 이미 로드된 데이터는 캐시에서 즉시 표시</li>
 * </ul>
 *
 * <h3>데이터 흐름:</h3>
 * <ol>
 *   <li><strong>이전 화면에서 전달:</strong> 학번/학과/트랙 + 추가 졸업 요건</li>
 *   <li><strong>강의 데이터 로드:</strong> 선택된 조건에 맞는 강의 목록 Firestore에서 조회</li>
 *   <li><strong>사용자 입력:</strong> 다이얼로그를 통한 수강 강의 선택 및 추가</li>
 *   <li><strong>다음 단계:</strong> 분석 버튼 클릭 시 GraduationAnalysisResultActivity로 이동</li>
 * </ol>
 *
 * <h3>상태 관리 및 안정성:</h3>
 * <ul>
 *   <li><strong>SavedInstanceState:</strong> 앱/백그라운드 전환 시 상태 보존</li>
 *   <li><strong>WindowInsets 대응:</strong> 노치/네비게이션 바 영역 안전 처리</li>
 *   <li><strong>중복 방지:</strong> 로딩 중 버튼 비활성화 및 중복 클릭 차단</li>
 * </ul>
 *
 * @see GraduationAnalysisResultActivity 결과 화면
 * @see FirebaseDataManager 강의 데이터 제공
 * @see AdditionalRequirementsActivity 이전 단계 화면
 */
public class CourseInputActivity extends BaseActivity {

    private static final String TAG = "CourseInput";

    // ── SavedInstanceState 키(상태 복원) ───────────────────────────────────────
    private static final String S_IS_MAJOR_GROUP = "s_is_major_group";
    private static final String S_CURRENT_TAB = "s_current_tab";
    private static final String S_COURSE_LIST = "s_course_list";
    private static final String S_LAST_IS_MAJOR = "s_last_is_major";
    private static final String S_LAST_CAT_POS = "s_last_cat_pos";
    private static final String S_LAST_COMP_POS = "s_last_comp_pos";

    // ── UI 컴포넌트 ──────────────────────────────────────────────────────────
    private TextView textViewStudentInfo;
    private Button btnAddCourse;
    private LinearLayout layoutSelectedCategoryCourses;
    private TextView textEmptyCourses;
    private Button btnAnalyzeGraduation;
    private Toolbar toolbar;

    // 그룹 전환 버튼들
    private com.google.android.material.button.MaterialButton btnMajorGroup;
    private com.google.android.material.button.MaterialButton btnGeneralGroup;
    private View majorTabsContainer;
    private View generalTabsContainer;

    // 탭 버튼들 (Chip)
    private com.google.android.material.chip.Chip tabMajorRequired;
    private com.google.android.material.chip.Chip tabMajorElective;
    private com.google.android.material.chip.Chip tabMajorAdvanced;
    private com.google.android.material.chip.Chip tabGeneralRequired;
    private com.google.android.material.chip.Chip tabGeneralElective;

    // 현재 선택 상태
    private boolean isMajorGroupSelected = true;
    private String currentSelectedTab = "전공필수";

    // Firebase 데이터 매니저
    private FirebaseDataManager dataManager;

    // 인텐트로 받은 데이터
    private String selectedYear, selectedDepartment, selectedTrack;
    private AdditionalRequirementsActivity.AdditionalRequirements additionalRequirements;

    // 수집된 강의 목록
    private List<Course> courseList;

    // 다이얼로그 선택 상태(복원용)
    private boolean lastSelectedIsMajor = true;
    private int lastSelectedCategoryPosition = 0;
    private int lastSelectedCompetencyPosition = 0;

    // 동일 요청 In-Flight 합치기
    private boolean isLoadingCourses = false;
    private String lastLoadedCategory = null;
    private long lastLoadTime = 0;
    private static final long MIN_LOAD_INTERVAL = 500;
    private final Map<String, List<CleanArrayAdapter<FirebaseDataManager.CourseInfo>>> pendingRequests = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_course_input);

        // 기본 액션바 숨김(커스텀 Toolbar 사용)
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        getIntentData();      // 인텐트 데이터 파싱
        initViews();          // 뷰 바인딩/매니저 초기화
        loadSavedDataIfNeeded(); // 저장된 데이터 로드 (courseList 초기화 후)
        setupSystemUI();      // 안전영역 패딩
        setupToolbar();       // 툴바/뒤로가기
        setupListeners();     // 버튼/탭 리스너
        displayStudentInfo(); // 상단 학생정보 + 탭 텍스트 반영

        // 상태 복원
        if (savedInstanceState != null) {
            restoreActivityState(savedInstanceState);
        } else {
            // 초기 상태: 전공 그룹, 전공필수 탭
            updateTabDisplay();
            switchToMajorGroup();
        }
    }

    /** WindowInsetsCompat: 시스템 바 영역만큼 패딩 적용 */
    private void setupSystemUI() {
        View root = findViewById(R.id.main);
        if (root == null) return; // NPE 방지

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    /** 인텐트 데이터 파싱(이전 화면의 상수 사용) */
    private void getIntentData() {
        Intent intent = getIntent();
        selectedYear = intent.getStringExtra(AdditionalRequirementsActivity.EXTRA_YEAR);
        selectedDepartment = intent.getStringExtra(AdditionalRequirementsActivity.EXTRA_DEPARTMENT);
        selectedTrack = intent.getStringExtra(AdditionalRequirementsActivity.EXTRA_TRACK);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            additionalRequirements = intent.getParcelableExtra(AdditionalRequirementsActivity.EXTRA_REQUIREMENTS, AdditionalRequirementsActivity.AdditionalRequirements.class);
        } else {
            additionalRequirements = intent.getParcelableExtra(AdditionalRequirementsActivity.EXTRA_REQUIREMENTS);
        }

        if (selectedYear == null || selectedDepartment == null || selectedTrack == null) {
            Toast.makeText(this, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (additionalRequirements != null) {
            Log.d(TAG, "추가 졸업 요건 수신: " + additionalRequirements);
        } else {
            Log.w(TAG, "추가 졸업 요건 데이터를 받지 못했습니다.");
        }
    }

    /** 저장된 데이터가 있으면 로드 (initViews 이후 호출) */
    private void loadSavedDataIfNeeded() {
        Intent intent = getIntent();
        boolean isLoadingSavedData = intent.getBooleanExtra("isLoadingSavedData", false);
        if (!isLoadingSavedData) {
            return;
        }

        Log.d(TAG, "저장된 데이터 불러오기 모드");

        // 저장된 과목 데이터 로드
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            java.util.ArrayList<CourseInputActivity.Course> savedCourses =
                intent.getParcelableArrayListExtra("savedCourses", CourseInputActivity.Course.class);
            if (savedCourses != null && !savedCourses.isEmpty()) {
                courseList.addAll(savedCourses);
                Log.d(TAG, "저장된 과목 " + savedCourses.size() + "개 로드 완료");
                // 과목 상세 로그
                for (Course c : savedCourses) {
                    Log.d(TAG, "  - " + c.getName() + " (" + c.getCategory() + ", " + c.getCredits() + "학점)");
                }
            }
        } else {
            java.util.ArrayList<CourseInputActivity.Course> savedCourses =
                intent.getParcelableArrayListExtra("savedCourses");
            if (savedCourses != null && !savedCourses.isEmpty()) {
                courseList.addAll(savedCourses);
                Log.d(TAG, "저장된 과목 " + savedCourses.size() + "개 로드 완료");
                // 과목 상세 로그
                for (Course c : savedCourses) {
                    Log.d(TAG, "  - " + c.getName() + " (" + c.getCategory() + ", " + c.getCredits() + "학점)");
                }
            }
        }

        // 저장된 추가 요건이 있으면 덮어쓰기
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            AdditionalRequirementsActivity.AdditionalRequirements savedReqs =
                intent.getParcelableExtra("savedAdditionalRequirements", AdditionalRequirementsActivity.AdditionalRequirements.class);
            if (savedReqs != null) {
                additionalRequirements = savedReqs;
                Log.d(TAG, "저장된 추가 요건 로드 완료");
            }
        } else {
            AdditionalRequirementsActivity.AdditionalRequirements savedReqs =
                intent.getParcelableExtra("savedAdditionalRequirements");
            if (savedReqs != null) {
                additionalRequirements = savedReqs;
                Log.d(TAG, "저장된 추가 요건 로드 완료");
            }
        }

        // UI 업데이트: 불러온 과목들을 화면에 표시
        Log.d(TAG, "courseList.size() = " + courseList.size() + ", isEmpty() = " + courseList.isEmpty());
        if (!courseList.isEmpty()) {
            Log.d(TAG, "UI 업데이트 시작 - 현재 탭: " + currentSelectedTab);
            updateCourseDisplay();  // 현재 탭의 과목들 표시
            updateAnalyzeButtonState();  // 분석 버튼 상태 업데이트
            Toast.makeText(this, "저장된 데이터를 불러왔습니다 (" + courseList.size() + "개 과목)", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "courseList가 비어있어 UI를 업데이트하지 않습니다");
        }
    }

    /** 뷰 바인딩 및 매니저 초기화 */
    private void initViews() {
        textViewStudentInfo = findViewById(R.id.text_view_student_info);
        btnAddCourse = findViewById(R.id.btn_add_course);
        layoutSelectedCategoryCourses = findViewById(R.id.layout_selected_category_courses);
        textEmptyCourses = findViewById(R.id.text_empty_courses);
        btnAnalyzeGraduation = findViewById(R.id.btn_analyze_graduation);
        toolbar = findViewById(R.id.toolbar_course_input);

        btnMajorGroup = findViewById(R.id.btn_major_group);
        btnGeneralGroup = findViewById(R.id.btn_general_group);
        majorTabsContainer = findViewById(R.id.major_tabs_container);
        generalTabsContainer = findViewById(R.id.general_tabs_container);

        tabMajorRequired = findViewById(R.id.tab_major_required);
        tabMajorElective = findViewById(R.id.tab_major_elective);
        tabMajorAdvanced = findViewById(R.id.tab_major_advanced);
        tabGeneralRequired = findViewById(R.id.tab_general_required);
        tabGeneralElective = findViewById(R.id.tab_general_elective);

        // 대체과목 리스트 보기 버튼
        com.google.android.material.button.MaterialButton btnViewReplacementCourses = findViewById(R.id.btn_view_replacement_courses);
        btnViewReplacementCourses.setOnClickListener(v -> showReplacementCoursesDialog());

        dataManager = FirebaseDataManager.getInstance();

        courseList = new ArrayList<>();
    }

    /** 툴바/뒤로가기/타이틀 설정 */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("수강 강의 입력");
        }
    }

    /** 뒤로가기 동작 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { getOnBackPressedDispatcher().onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }

    /** 버튼/탭 리스너 연결 */
    private void setupListeners() {
        btnAddCourse.setOnClickListener(v -> showAddCourseDialog());
        btnAnalyzeGraduation.setOnClickListener(v -> analyzeGraduation());

        // 그룹 전환
        btnMajorGroup.setOnClickListener(v -> switchToMajorGroup());
        btnGeneralGroup.setOnClickListener(v -> switchToGeneralGroup());

        // 탭 전환
        tabMajorRequired.setOnClickListener(v -> switchTab("전공필수", tabMajorRequired));
        tabMajorElective.setOnClickListener(v -> switchTab("전공선택", tabMajorElective));
        tabMajorAdvanced.setOnClickListener(v -> {
            String tabName = DepartmentConfig.getDepartmentCommonCategoryName(selectedDepartment, selectedYear);
            switchTab(tabName, tabMajorAdvanced);
        });
        tabGeneralRequired.setOnClickListener(v -> switchTab("교양필수", tabGeneralRequired));
        tabGeneralElective.setOnClickListener(v -> switchTab("교양선택", tabGeneralElective));
    }

    /** 상단 학생 정보와 탭 텍스트(학년/학부별) */
    private void displayStudentInfo() {
        String studentInfo = String.format("%s학번 %s %s", selectedYear, selectedDepartment, selectedTrack);
        textViewStudentInfo.setText(studentInfo);
        Log.d(TAG, "학생 정보 표시: " + studentInfo);

        updateTabTexts();
    }

    /** 전공 공통 탭 텍스트(학부/연도별 라벨) 갱신 */
    private void updateTabTexts() {
        String tabText = DepartmentConfig.getTabText(selectedDepartment, selectedYear);
        tabMajorAdvanced.setText(tabText);

        if (!tabText.equals(currentSelectedTab) &&
                ("전공심화".equals(currentSelectedTab) || "학부공통".equals(currentSelectedTab))) {
            currentSelectedTab = tabText; // 현재가 공통/심화 중 하나면 새 라벨로 동기화
        }
        updateTabDisplay();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 강의 추가 다이얼로그
    // ────────────────────────ㄷ─────────────────────────────────────────────────
    private void showAddCourseDialog() {
        // 다이어로그 시작 시 중복 로딩 방지 상태 초기화
        resetLoadingState();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_course, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // 다이얼로그 UI
        RadioGroup radioGroupCourseType = dialogView.findViewById(R.id.radio_group_course_type);
        RadioButton radioMajor = dialogView.findViewById(R.id.radio_major);
        RadioButton radioGeneral = dialogView.findViewById(R.id.radio_general);
        RadioButton radioCustom = dialogView.findViewById(R.id.radio_custom);
        LinearLayout layoutMajorCourses = dialogView.findViewById(R.id.layout_major_courses);
        LinearLayout layoutGeneralManualInput = dialogView.findViewById(R.id.layout_general_manual_input);
        LinearLayout layoutManualInput = dialogView.findViewById(R.id.layout_manual_input);
        LinearLayout layoutCustomInput = dialogView.findViewById(R.id.layout_custom_input);
        LinearLayout layoutCustomCompetency = dialogView.findViewById(R.id.layout_custom_competency);
        // 세부 분류 LinearLayout을 직접 찾음 (spinner_course_category의 부모)
        LinearLayout layoutCategorySection = (LinearLayout) dialogView.findViewById(R.id.spinner_course_category).getParent();
        Spinner spinnerMajorCourses = dialogView.findViewById(R.id.spinner_major_courses);
        Spinner spinnerCourseCategory = dialogView.findViewById(R.id.spinner_course_category);
        Spinner spinnerCustomCategory = dialogView.findViewById(R.id.spinner_custom_category);
        Spinner spinnerCustomCompetency = dialogView.findViewById(R.id.spinner_custom_competency);
        EditText editGeneralCourseName = dialogView.findViewById(R.id.edit_general_course_name);
        EditText editGeneralCourseCredits = dialogView.findViewById(R.id.edit_general_course_credits);
        Spinner spinnerGeneralCompetency = dialogView.findViewById(R.id.spinner_general_competency);
        EditText editCourseName = dialogView.findViewById(R.id.edit_course_name);
        EditText editCourseCredits = dialogView.findViewById(R.id.edit_course_credits);
        EditText editCustomCourseName = dialogView.findViewById(R.id.edit_custom_course_name);
        EditText editCustomCourseCredits = dialogView.findViewById(R.id.edit_custom_course_credits);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);

        // 어댑터들
        CleanArrayAdapter<FirebaseDataManager.CourseInfo> majorCoursesAdapter =
                new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        majorCoursesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMajorCourses.setAdapter(majorCoursesAdapter);

        CleanArrayAdapter<String> categoryAdapter =
                new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourseCategory.setAdapter(categoryAdapter);

        CleanArrayAdapter<String> generalCompetencyAdapter =
                new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        generalCompetencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGeneralCompetency.setAdapter(generalCompetencyAdapter);

        // 직접 입력용 카테고리 어댑터 (모든 카테고리 포함)
        CleanArrayAdapter<String> customCategoryAdapter =
                new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        customCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCustomCategory.setAdapter(customCategoryAdapter);
        updateCustomCategorySpinner(customCategoryAdapter);

        // 직접 입력용 역량 어댑터
        CleanArrayAdapter<String> customCompetencyAdapter =
                new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        customCompetencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCustomCompetency.setAdapter(customCompetencyAdapter);
        updateCompetencySpinner(customCompetencyAdapter);

        // 저장된 다이얼로그 상태 복원
        restoreDialogState(
                radioMajor, radioGeneral, categoryAdapter, generalCompetencyAdapter, majorCoursesAdapter,
                layoutMajorCourses, layoutGeneralManualInput, layoutManualInput, spinnerCourseCategory, spinnerGeneralCompetency
        );

        // 세부 분류 영역 참조 (final 처리)
        final LinearLayout finalLayoutCategorySection = layoutCategorySection;

        // 직접입력용 카테고리 선택 시 역량 표시 여부 결정
        spinnerCustomCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = customCategoryAdapter.getItem(position);
                if ("교양선택".equals(selectedCategory)) {
                    layoutCustomCompetency.setVisibility(View.VISIBLE);
                } else {
                    layoutCustomCompetency.setVisibility(View.GONE);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 전공/교양/직접입력 전환
        radioGroupCourseType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMajor = (checkedId == R.id.radio_major);
            boolean isGeneral = (checkedId == R.id.radio_general);
            boolean isCustom = (checkedId == R.id.radio_custom);
            lastSelectedIsMajor = isMajor; // 상태 저장

            if (isCustom) {
                // 직접 입력 모드
                layoutMajorCourses.setVisibility(View.GONE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
                layoutCustomInput.setVisibility(View.VISIBLE);
                if (finalLayoutCategorySection != null) {
                    finalLayoutCategorySection.setVisibility(View.GONE);
                }
                // 입력 필드 초기화
                editCustomCourseName.setText("");
                editCustomCourseCredits.setText("");
                if (customCategoryAdapter.getCount() > 0) spinnerCustomCategory.setSelection(0);
            } else if (isMajor) {
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
                layoutCustomInput.setVisibility(View.GONE);
                if (finalLayoutCategorySection != null) {
                    finalLayoutCategorySection.setVisibility(View.VISIBLE);
                }
            } else {
                // 교양 선택 시: 기본은 교양필수 방식(UI 동일)
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
                layoutCustomInput.setVisibility(View.GONE);
                if (finalLayoutCategorySection != null) {
                    finalLayoutCategorySection.setVisibility(View.VISIBLE);
                }
            }

            if (!isCustom) {
                updateCategorySpinner(categoryAdapter, isMajor);
                clearCourseSpinner(majorCoursesAdapter);

                if (categoryAdapter.getCount() > 0) spinnerCourseCategory.setSelection(0);
                if (majorCoursesAdapter.getCount() > 0) spinnerMajorCourses.setSelection(0);

                if (!isMajor) {
                    editGeneralCourseName.setText("");
                    editGeneralCourseCredits.setText("");
                    if (generalCompetencyAdapter.getCount() > 0) spinnerGeneralCompetency.setSelection(0);
                }
                editCourseName.setText("");
                editCourseCredits.setText("");

                // 기본 카테고리 자동 로드(교양선택 제외) - 지연 제거
                if (categoryAdapter.getCount() > 0) {
                    String selectedCategory = categoryAdapter.getItem(0);
                    Log.d(TAG, "라디오 전환 후 자동 로드: " + selectedCategory + " (전공? " + isMajor + ")");
                    if (!"교양선택".equals(selectedCategory)) {
                        loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                    }
                }
            }
        });

        // 카테고리 선택
        spinnerCourseCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean isMajor = radioGroupCourseType.getCheckedRadioButtonId() == R.id.radio_major;
                if (position < 0) { clearCourseSpinner(majorCoursesAdapter); return; }

                String selectedCategory = categoryAdapter.getItem(position);
                lastSelectedCategoryPosition = position; // 상태 저장

                if (isMajor) {
                    layoutMajorCourses.setVisibility(View.VISIBLE);
                    layoutGeneralManualInput.setVisibility(View.GONE);
                    layoutManualInput.setVisibility(View.GONE);
                    loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                } else {
                    if ("교양선택".equals(selectedCategory)) {
                        layoutMajorCourses.setVisibility(View.GONE);
                        layoutGeneralManualInput.setVisibility(View.VISIBLE);
                        layoutManualInput.setVisibility(View.GONE);
                    } else {
                        layoutMajorCourses.setVisibility(View.VISIBLE);
                        layoutGeneralManualInput.setVisibility(View.GONE);
                        layoutManualInput.setVisibility(View.GONE);
                        loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                    }
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 취소
        btnCancel.setOnClickListener(v -> {
            resetLoadingState(); // 다이어로그 닫을 때 상태 초기화
            dialog.dismiss();
        });

        // 추가
        btnAdd.setOnClickListener(v -> {
            int checkedId = radioGroupCourseType.getCheckedRadioButtonId();
            boolean isMajor = (checkedId == R.id.radio_major);
            boolean isCustom = (checkedId == R.id.radio_custom);

            if (isCustom) {
                // 직접 입력 모드
                if (addCourseFromCustomInput(spinnerCustomCategory, editCustomCourseName, editCustomCourseCredits,
                        spinnerCustomCompetency, customCategoryAdapter, customCompetencyAdapter, layoutCustomCompetency)) {
                    resetLoadingState();
                    dialog.dismiss();
                }
            } else if (isMajor) {
                // 전공: 스피너 선택 우선, 없으면 수동
                if (spinnerMajorCourses.getSelectedItemPosition() >= 0 && majorCoursesAdapter.getCount() > 0) {
                    if (addCourseFromDialog(spinnerCourseCategory, spinnerMajorCourses, categoryAdapter, majorCoursesAdapter)) {
                        resetLoadingState(); // 다이어로그 닫을 때 상태 초기화
                        dialog.dismiss();
                    }
                } else {
                    if (addCourseFromManualInput(spinnerCourseCategory, editCourseName, editCourseCredits, categoryAdapter)) {
                        resetLoadingState(); // 다이어로그 닫을 때 상태 초기화
                        dialog.dismiss();
                    }
                }
            } else {
                // 교양
                String selectedCategory = spinnerCourseCategory.getSelectedItemPosition() >= 0 ?
                        categoryAdapter.getItem(spinnerCourseCategory.getSelectedItemPosition()) : "";
                if ("교양선택".equals(selectedCategory)) {
                    if (addGeneralCourseFromManualInput(
                            spinnerCourseCategory, editGeneralCourseName, editGeneralCourseCredits,
                            spinnerGeneralCompetency, categoryAdapter, generalCompetencyAdapter)) {
                        resetLoadingState(); // 다이어로그 닫을 때 상태 초기화
                        dialog.dismiss();
                    }
                } else {
                    if (spinnerMajorCourses.getSelectedItemPosition() >= 0 && majorCoursesAdapter.getCount() > 0) {
                        if (addCourseFromDialog(spinnerCourseCategory, spinnerMajorCourses, categoryAdapter, majorCoursesAdapter)) {
                            resetLoadingState(); // 다이어로그 닫을 때 상태 초기화
                            dialog.dismiss();
                        }
                    } else {
                        if (addCourseFromManualInput(spinnerCourseCategory, editCourseName, editCourseCredits, categoryAdapter)) {
                            resetLoadingState(); // 다이어로그 닫을 때 상태 초기화
                            dialog.dismiss();
                        }
                    }
                }
            }
        });

        dialog.show();
    }

    /**
     * 다이어로그용 로딩 상태 초기화 (중복 로딩 방지 해제)
     */
    private void resetLoadingState() {
        isLoadingCourses = false;
        lastLoadedCategory = null;
        lastLoadTime = 0;
        pendingRequests.clear(); // 대기열도 초기화
        Log.d(TAG, "다이어로그 로딩 상태 초기화");
    }

    /**
     * 대기 중인 모든 어댑터들에 데이터 업데이트
     */
    private void updatePendingAdapters(String category, List<FirebaseDataManager.CourseInfo> courses) {
        List<CleanArrayAdapter<FirebaseDataManager.CourseInfo>> adapters = pendingRequests.remove(category);
        if (adapters != null) {
            for (CleanArrayAdapter<FirebaseDataManager.CourseInfo> adapter : adapters) {
                adapter.clear();
                adapter.addAll(courses);
                adapter.notifyDataSetChanged();
            }
            Log.d(TAG, "In-Flight 합치기: " + adapters.size() + "개 어댑터 동시 업데이트");
        }
    }

    /** 카테고리 스피너 데이터 구성(전공/교양) */
    private void updateCategorySpinner(CleanArrayAdapter<String> categoryAdapter, boolean isMajor) {
        categoryAdapter.clear();
        if (isMajor) {
            String departmentCommonCategory = DepartmentConfig.getDepartmentCommonCategoryName(selectedDepartment, selectedYear);
            Log.d(TAG, "===== 카테고리 스피너 설정 =====");
            Log.d(TAG, "학부: " + selectedDepartment + ", 학번: " + selectedYear + ", 공통카테고리: " + departmentCommonCategory);
            Log.d(TAG, "usesMajorAdvancedForAllYears: " + DepartmentConfig.usesMajorAdvancedForAllYears(selectedDepartment));
            Log.d(TAG, "DepartmentConfig 전체 설정: " + DepartmentConfig.getAllDepartmentConfigs());

            categoryAdapter.add(departmentCommonCategory);
            categoryAdapter.add("전공필수");
            categoryAdapter.add("전공선택");
        } else {
            categoryAdapter.add("교양필수");
            categoryAdapter.add("교양선택");
        }
        categoryAdapter.notifyDataSetChanged();
    }

    private void clearCourseSpinner(CleanArrayAdapter<FirebaseDataManager.CourseInfo> courseAdapter) {
        courseAdapter.clear();
        courseAdapter.notifyDataSetChanged();
    }

    /** 교양선택 역량 스피너 구성 */
    private void setupCompetencySpinner(CleanArrayAdapter<String> competencyAdapter) {
        competencyAdapter.clear();
        competencyAdapter.add("1역량");
        competencyAdapter.add("2역량");
        competencyAdapter.add("3역량");
        competencyAdapter.add("4역량");
        competencyAdapter.add("5역량");
        competencyAdapter.add("소양");
        competencyAdapter.notifyDataSetChanged();
    }

    /** 직접 입력용 모든 카테고리 스피너 구성 */
    private void updateCustomCategorySpinner(CleanArrayAdapter<String> categoryAdapter) {
        categoryAdapter.clear();
        String departmentCommonCategory = DepartmentConfig.getDepartmentCommonCategoryName(selectedDepartment, selectedYear);

        // 전공 카테고리
        categoryAdapter.add("전공필수");
        categoryAdapter.add("전공선택");
        categoryAdapter.add(departmentCommonCategory); // 학부공통 또는 전공심화

        // 교양 카테고리
        categoryAdapter.add("교양필수");
        categoryAdapter.add("교양선택");
        categoryAdapter.add("소양");

        // 기타 카테고리
        if (DepartmentConfig.isOldCurriculum(selectedDepartment, selectedYear)) {
            categoryAdapter.add("일반선택");
        } else {
            categoryAdapter.add("잔여학점");
        }

        categoryAdapter.notifyDataSetChanged();
    }

    /** 직접 입력용 역량 스피너 구성 */
    private void updateCompetencySpinner(CleanArrayAdapter<String> competencyAdapter) {
        competencyAdapter.clear();
        competencyAdapter.add("없음");
        competencyAdapter.add("1역량");
        competencyAdapter.add("2역량");
        competencyAdapter.add("3역량");
        competencyAdapter.add("4역량");
        competencyAdapter.add("5역량");
        competencyAdapter.notifyDataSetChanged();
    }

    /** 직접 입력 모드에서 과목 추가 */
    private boolean addCourseFromCustomInput(Spinner spinnerCategory, EditText editName, EditText editCredits,
                                              Spinner spinnerCompetency, CleanArrayAdapter<String> categoryAdapter,
                                              CleanArrayAdapter<String> competencyAdapter, LinearLayout layoutCompetency) {
        // 카테고리 선택 확인
        if (spinnerCategory.getSelectedItemPosition() < 0 || categoryAdapter.getCount() == 0) {
            Toast.makeText(this, "분류를 선택해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 강의명 확인
        String courseName = editName.getText().toString().trim();
        if (courseName.isEmpty()) {
            Toast.makeText(this, "강의명을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 학점 확인
        String creditsStr = editCredits.getText().toString().trim();
        if (creditsStr.isEmpty()) {
            Toast.makeText(this, "학점을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        int credits;
        try {
            credits = Integer.parseInt(creditsStr);
            if (credits <= 0 || credits > 10) {
                Toast.makeText(this, "올바른 학점을 입력해주세요 (1-10)", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 학점을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        String category = categoryAdapter.getItem(spinnerCategory.getSelectedItemPosition());

        // 중복 체크
        for (Course c : courseList) {
            if (c.getName().equals(courseName)) {
                Toast.makeText(this, "이미 추가된 강의입니다", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // 역량 처리 (교양선택인 경우)
        String competency = null;
        if ("교양선택".equals(category) && layoutCompetency.getVisibility() == View.VISIBLE) {
            if (spinnerCompetency.getSelectedItemPosition() >= 0 && competencyAdapter.getCount() > 0) {
                String selectedCompetency = competencyAdapter.getItem(spinnerCompetency.getSelectedItemPosition());
                if (!"없음".equals(selectedCompetency)) {
                    competency = selectedCompetency;
                    // 소양인 경우 카테고리 변경
                    if ("소양".equals(competency)) {
                        category = "소양";
                    }
                }
            }
        }

        // 과목 추가
        Course newCourse = new Course(category, courseName, credits, null, competency);
        courseList.add(newCourse);

        Log.d(TAG, "직접 입력 과목 추가: " + courseName + " (" + credits + "학점, " + category +
                (competency != null ? ", " + competency : "") + ")");

        Toast.makeText(this, courseName + " 추가됨", Toast.LENGTH_SHORT).show();
        updateCourseDisplay();
        updateAnalyzeButtonState();

        return true;
    }

    /** 카테고리별 강의 로드(중복/과한 요청 방지 포함) */
    private void loadCoursesForCategory(String category, CleanArrayAdapter<FirebaseDataManager.CourseInfo> courseAdapter) {
        Log.d(TAG, "loadCoursesForCategory: " + category);
        long now = System.currentTimeMillis();

        // In-Flight 합치기: 동일 카테고리 로딩 중이면 어댓터 추가 후 대기
        if (isLoadingCourses && category.equals(lastLoadedCategory)) {
            Log.d(TAG, "In-Flight 합치기: 대기열에 추가 => " + category);
            List<CleanArrayAdapter<FirebaseDataManager.CourseInfo>> adapters = pendingRequests.get(category);
            if (adapters == null) {
                adapters = new ArrayList<>();
                pendingRequests.put(category, adapters);
            }
            adapters.add(courseAdapter);
            return;
        }
        // 최소 간격 체크 (어부징 방지)
        if (category.equals(lastLoadedCategory) && (now - lastLoadTime) < MIN_LOAD_INTERVAL) {
            Log.d(TAG, "너무 빠른 재요청 차단: " + (now - lastLoadTime) + "ms < " + MIN_LOAD_INTERVAL + "ms");
            return;
        }

        isLoadingCourses = true;
        lastLoadedCategory = category;
        lastLoadTime = now;

        // 지연 개선: clear 후 즉시 로딩 메시지 표시
        courseAdapter.clear();
        FirebaseDataManager.CourseInfo loadingItem = new FirebaseDataManager.CourseInfo("로딩 중...", 0);
        courseAdapter.add(loadingItem);
        courseAdapter.notifyDataSetChanged();

        // 로딩 메시지는 리스트 영역에 표시
        showLoadingMessage("강의 목록을 불러오는 중...");

        if ("학부공통".equals(category) || "전공심화".equals(category)) {
            dataManager.loadDepartmentCommonCourses(
                    selectedDepartment, selectedTrack, selectedYear,
                    new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                        @Override
                        public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                            List<FirebaseDataManager.CourseInfo> filtered = filterRegisteredCourses(courses);
                            // 메인 어댑터 업데이트
                            courseAdapter.clear();
                            courseAdapter.addAll(filtered);
                            courseAdapter.notifyDataSetChanged();

                            // 대기 중인 모든 어댑터들도 동시 업데이트
                            updatePendingAdapters(category, filtered);

                            hideLoadingMessage();
                            Log.d(TAG, category + " 강의 로드 성공: " + filtered.size() + "개 (대기열 포함)");
                            isLoadingCourses = false;
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, category + " 강의 로드 실패", e);
                            Toast.makeText(CourseInputActivity.this, category + " 강의를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                            isLoadingCourses = false;
                        }
                    });

        } else if ("전공필수".equals(category) || "전공선택".equals(category)) {
            dataManager.loadMajorCourses(
                    selectedDepartment, selectedTrack, selectedYear, category,
                    new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                        @Override
                        public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                            List<FirebaseDataManager.CourseInfo> filtered = filterRegisteredCourses(courses);
                            courseAdapter.clear(); // 로딩 아이템 제거
                            courseAdapter.addAll(filtered);
                            courseAdapter.notifyDataSetChanged();
                            hideLoadingMessage();
                            Log.d(TAG, "전공 강의 로드 성공: " + filtered.size());
                            isLoadingCourses = false;
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "전공 강의 로드 실패", e);
                            Toast.makeText(CourseInputActivity.this, "전공 강의를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                            isLoadingCourses = false;
                        }
                    });

        } else if ("교양필수".equals(category)) {
            // 교양(필수)만 DataManager 호출 — 학과별 → 없으면 공통으로 폴백은 DataManager가 처리함
            Log.d(TAG, "교양필수 강의 로드 요청 (문서 선택은 DataManager 폴백)");
            dataManager.loadGeneralEducationCourses(
                    selectedDepartment, selectedTrack, selectedYear, category,
                    new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                        @Override
                        public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                            List<FirebaseDataManager.CourseInfo> filtered = filterRegisteredCourses(courses);
                            courseAdapter.clear(); // 로딩 아이템 제거
                            courseAdapter.addAll(filtered);
                            courseAdapter.notifyDataSetChanged();
                            hideLoadingMessage();
                            Log.d(TAG, "교양필수 강의 로드 성공: " + filtered.size());
                            isLoadingCourses = false;
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "교양필수 강의 로드 실패", e);
                            Toast.makeText(CourseInputActivity.this, "교양필수 강의를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                            isLoadingCourses = false;
                        }
                    });

        } else {
            // 교양선택/일반선택은 수동 입력으로 처리하므로 로딩 생략
            Log.d(TAG, "교양선택/일반선택은 수동 입력으로 처리 — 로딩 생략");
            hideLoadingMessage();
            isLoadingCourses = false;
        }
    }


    /** 이미 등록된 과목/oneOf 그룹 충돌 과목 필터링 */
    private List<FirebaseDataManager.CourseInfo> filterRegisteredCourses(List<FirebaseDataManager.CourseInfo> courses) {
        List<FirebaseDataManager.CourseInfo> filtered = new ArrayList<>();
        for (FirebaseDataManager.CourseInfo c : courses) {
            boolean exclude = false;
            for (Course reg : courseList) {
                if (reg.getName().equals(c.getName())) { exclude = true; break; }
                if (c.getGroupId() != null && reg.getGroupId() != null && c.getGroupId().equals(reg.getGroupId())) {
                    exclude = true;
                    Log.d(TAG, "같은 oneOf 그룹 제외: " + c.getName() + " / " + c.getGroupId());
                    break;
                }
            }
            if (!exclude) filtered.add(c);
        }
        return filtered;
    }

    /** 스피너 선택으로 강의 추가(전공/교양필수 공통) */
    private boolean addCourseFromDialog(Spinner spinnerCourseCategory, Spinner spinnerMajorCourses,
                                        CleanArrayAdapter<String> categoryAdapter,
                                        CleanArrayAdapter<FirebaseDataManager.CourseInfo> courseAdapter) {
        int catPos = spinnerCourseCategory.getSelectedItemPosition();
        int coursePos = spinnerMajorCourses.getSelectedItemPosition();
        if (catPos == -1) { Toast.makeText(this, "강의 분류를 선택해주세요.", Toast.LENGTH_SHORT).show(); return false; }
        if (coursePos == -1) { Toast.makeText(this, "강의를 선택해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        String category = categoryAdapter.getItem(catPos);
        FirebaseDataManager.CourseInfo selected = courseAdapter.getItem(coursePos);
        String courseName = selected.getName();
        int credits = selected.getCredits();

        for (Course ex : courseList) {
            if (ex.getName().equals(courseName)) {
                Toast.makeText(this, "이미 등록된 강의입니다: " + courseName, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        Course course = new Course(category, courseName, credits, selected.getGroupId());
        courseList.add(course);
        updateCourseDisplay();
        updateAnalyzeButtonState();

        Log.d(TAG, "강의 추가: " + course);
        Toast.makeText(this, "강의가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        return true;
    }

    /** 교양선택 수동 입력(역량 포함) */
    private boolean addGeneralCourseFromManualInput(Spinner spinnerCourseCategory, EditText editCourseName,
                                                    EditText editCourseCredits, Spinner spinnerCompetency,
                                                    CleanArrayAdapter<String> categoryAdapter,
                                                    CleanArrayAdapter<String> competencyAdapter) {
        int catPos = spinnerCourseCategory.getSelectedItemPosition();
        if (catPos == -1) { Toast.makeText(this, "강의 분류를 선택해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        String name = editCourseName.getText().toString().trim();
        if (name.isEmpty()) { Toast.makeText(this, "강의명을 입력해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        String creditsStr = editCourseCredits.getText().toString().trim();
        if (creditsStr.isEmpty()) { Toast.makeText(this, "학점을 입력해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        int compPos = spinnerCompetency.getSelectedItemPosition();
        if (compPos == -1) { Toast.makeText(this, "역량을 선택해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        int credits;
        try {
            credits = Integer.parseInt(creditsStr);
            if (credits <= 0) { Toast.makeText(this, "유효한 학점을 입력해주세요.", Toast.LENGTH_SHORT).show(); return false; }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "학점은 숫자로 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (Course ex : courseList) {
            if (ex.getName().equals(name)) {
                Toast.makeText(this, "이미 등록된 강의입니다: " + name, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        String category = categoryAdapter.getItem(catPos);
        String competency = competencyAdapter.getItem(compPos);
        if ("소양".equals(competency)) category = "소양"; // 소양 처리

        lastSelectedCompetencyPosition = compPos; // 역량 상태 저장

        Course course = new Course(category, name, credits, null, competency);
        courseList.add(course);
        updateCourseDisplay();
        updateAnalyzeButtonState();

        Log.d(TAG, ("소양".equals(competency) ? "소양" : "교양선택") + " 강의 추가: " + course + ", 역량: " + competency);
        Toast.makeText(this, ("소양".equals(competency) ? "소양" : "교양선택") + " 강의가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        return true;
    }

    /** 수동 입력(전공/교양필수 공통) */
    private boolean addCourseFromManualInput(Spinner spinnerCourseCategory, EditText editCourseName,
                                             EditText editCourseCredits, ArrayAdapter<String> categoryAdapter) {
        int catPos = spinnerCourseCategory.getSelectedItemPosition();
        if (catPos == -1) { Toast.makeText(this, "강의 분류를 선택해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        String name = editCourseName.getText().toString().trim();
        if (name.isEmpty()) { Toast.makeText(this, "강의명을 입력해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        String creditsStr = editCourseCredits.getText().toString().trim();
        if (creditsStr.isEmpty()) { Toast.makeText(this, "학점을 입력해주세요.", Toast.LENGTH_SHORT).show(); return false; }

        int credits;
        try {
            credits = Integer.parseInt(creditsStr);
            if (credits <= 0 || credits > 10) {
                Toast.makeText(this, "학점은 1-10 사이의 숫자여야 합니다.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 학점을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        for (Course ex : courseList) {
            if (ex.getName().equals(name)) {
                Toast.makeText(this, "이미 등록된 강의입니다: " + name, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        String category = categoryAdapter.getItem(catPos);
        Course course = new Course(category, name, credits);
        courseList.add(course);
        updateCourseDisplay();
        updateAnalyzeButtonState();

        Log.d(TAG, "수동 입력 강의 추가: " + course);
        Toast.makeText(this, "강의가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 탭/그룹 UI
    // ─────────────────────────────────────────────────────────────────────────
    private void switchTab(String tabName, com.google.android.material.chip.Chip ignored) {
        currentSelectedTab = tabName;
        updateTabDisplay();
        updateCourseDisplay();
    }

    private void updateTabDisplay() {
        // Chip의 체크 상태로 활성화 관리
        tabMajorRequired.setChecked(false);
        tabMajorElective.setChecked(false);
        tabMajorAdvanced.setChecked(false);
        tabGeneralRequired.setChecked(false);
        tabGeneralElective.setChecked(false);

        com.google.android.material.chip.Chip active = getTabChip(currentSelectedTab);
        if (active != null) active.setChecked(true);
    }

    private com.google.android.material.chip.Chip getTabChip(String tabName) {
        switch (tabName) {
            case "전공필수": return tabMajorRequired;
            case "전공선택": return tabMajorElective;
            case "전공심화":
            case "학부공통": return tabMajorAdvanced;
            case "교양필수": return tabGeneralRequired;
            case "교양선택": return tabGeneralElective;
            default: return null;
        }
    }

    /** 전공 그룹으로 전환(탭 컨테이너/버튼 스타일) */
    private void switchToMajorGroup() {
        isMajorGroupSelected = true;

        // MaterialButton 스타일 변경
        btnMajorGroup.setText("전공");
        btnMajorGroup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary)));
        btnMajorGroup.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        btnGeneralGroup.setText("교양");
        btnGeneralGroup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, android.R.color.transparent)));
        btnGeneralGroup.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        majorTabsContainer.setVisibility(View.VISIBLE);
        generalTabsContainer.setVisibility(View.GONE);

        switchTab("전공필수", tabMajorRequired);
    }

    /** 교양 그룹으로 전환(탭 컨테이너/버튼 스타일) */
    private void switchToGeneralGroup() {
        isMajorGroupSelected = false;

        // MaterialButton 스타일 변경
        btnGeneralGroup.setText("교양");
        btnGeneralGroup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary)));
        btnGeneralGroup.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        btnMajorGroup.setText("전공");
        btnMajorGroup.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, android.R.color.transparent)));
        btnMajorGroup.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        majorTabsContainer.setVisibility(View.GONE);
        generalTabsContainer.setVisibility(View.VISIBLE);

        switchTab("교양필수", tabGeneralRequired);
    }

    /** 선택된 탭 카테고리만 표시 */
    private void updateCourseDisplay() {
        Log.d(TAG, "updateCourseDisplay() 호출 - 현재 탭: " + currentSelectedTab + ", 전체 과목 수: " + courseList.size());
        layoutSelectedCategoryCourses.removeAllViews();

        List<Course> filtered = new ArrayList<>();
        for (Course c : courseList) {
            Log.d(TAG, "  과목 확인: " + c.getName() + " (" + c.getCategory() + ") vs 현재탭(" + currentSelectedTab + ")");
            if (c.getCategory().equals(currentSelectedTab)) {
                filtered.add(c);
                Log.d(TAG, "    -> 매칭됨!");
            }
        }

        Log.d(TAG, "필터링된 과목 수: " + filtered.size());

        if (filtered.isEmpty()) {
            // 빈 상태 메시지를 안전하게 부착
            if (textEmptyCourses.getParent() != layoutSelectedCategoryCourses) {
                if (textEmptyCourses.getParent() instanceof LinearLayout) {
                    ((LinearLayout) textEmptyCourses.getParent()).removeView(textEmptyCourses);
                }
                layoutSelectedCategoryCourses.addView(textEmptyCourses);
            }
            textEmptyCourses.setText("선택된 카테고리에 표시할 강의가 없습니다.");
            textEmptyCourses.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            Log.d(TAG, "필터링된 과목이 없어 빈 메시지 표시");
        } else {
            for (Course c : filtered) {
                Log.d(TAG, "과목 카드 생성: " + c.getName());
                createCourseItemView(c);
            }
            Log.d(TAG, "총 " + filtered.size() + "개 과목 카드 생성 완료");
        }
    }

    /** 개별 강의 카드 뷰 생성 */
    private void createCourseItemView(Course course) {
        // Material Card 스타일 적용
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        card.setLayoutParams(cardParams);
        card.setRadius((int) (12 * getResources().getDisplayMetrics().density));
        card.setCardElevation(0);
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        card.setStrokeColor(android.graphics.Color.parseColor("#E0E0E0"));
        card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        cardContent.setPadding(padding, (int) (12 * getResources().getDisplayMetrics().density), padding, (int) (12 * getResources().getDisplayMetrics().density));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoParams);

        TextView name = new TextView(this);
        name.setText(course.getName());
        name.setTextSize(15);
        name.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        name.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView credits = new TextView(this);
        credits.setText(String.format("%d학점", course.getCredits()));
        credits.setTextSize(13);
        credits.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        credits.setPadding(0, (int) (2 * getResources().getDisplayMetrics().density), 0, 0);

        info.addView(name);
        info.addView(credits);

        // Material IconButton 스타일 삭제 버튼
        android.widget.ImageButton delete = new android.widget.ImageButton(this);
        int btnSize = (int) (36 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(btnSize, btnSize);
        delParams.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
        delete.setLayoutParams(delParams);
        delete.setImageResource(android.R.drawable.ic_delete);
        delete.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light));
        delete.setBackgroundResource(android.R.color.transparent);
        delete.setContentDescription("삭제");

        delete.setOnClickListener(v -> {
            courseList.remove(course);
            updateCourseDisplay();
            updateAnalyzeButtonState();
            Toast.makeText(this, course.getName() + " 삭제됨", Toast.LENGTH_SHORT).show();
        });

        cardContent.addView(info);
        cardContent.addView(delete);
        card.addView(cardContent);
        layoutSelectedCategoryCourses.addView(card);
    }

    /** [분석] 버튼 상태(문구/활성화) */
    private void updateAnalyzeButtonState() {
        boolean enabled = !courseList.isEmpty();
        btnAnalyzeGraduation.setEnabled(enabled);
        btnAnalyzeGraduation.setText(enabled ?
                String.format("졸업요건 분석 (%d개 강의)", courseList.size()) :
                "강의를 입력해주세요");
    }

    /** 졸업요건 분석 화면으로 이동(중복 클릭 방지) */
    private void analyzeGraduation() {
        if (courseList.isEmpty()) {
            Toast.makeText(this, "최소 1개 이상의 강의를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnAnalyzeGraduation.setEnabled(false); // 중복 클릭 방지

        Intent intent = new Intent(this, GraduationAnalysisResultActivity.class);
        intent.putExtra(AdditionalRequirementsActivity.EXTRA_YEAR, selectedYear);
        intent.putExtra(AdditionalRequirementsActivity.EXTRA_DEPARTMENT, selectedDepartment);
        intent.putExtra(AdditionalRequirementsActivity.EXTRA_TRACK, selectedTrack);
        intent.putParcelableArrayListExtra("courses", new ArrayList<>(courseList));
        intent.putExtra(AdditionalRequirementsActivity.EXTRA_REQUIREMENTS, additionalRequirements);

        startActivity(intent);
        Log.d(TAG, "졸업요건 분석 시작 - 강의 수: " + courseList.size());
        if (additionalRequirements != null) {
            Log.d(TAG, "추가 졸업 요건 전달: " + additionalRequirements);
        } else {
            Log.w(TAG, "추가 졸업 요건이 null입니다.");

        }
        // 다음 화면으로 넘어간 뒤 다시 활성화(뒤로올 가능성 고려)
        // 2초 가드: 반드시 재활성화 보장
        btnAnalyzeGraduation.postDelayed(() -> btnAnalyzeGraduation.setEnabled(true), 2000);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 다이얼로그 상태 복원/유틸
    // ─────────────────────────────────────────────────────────────────────────
    private void restoreDialogState(RadioButton radioMajor, RadioButton radioGeneral,
                                    CleanArrayAdapter<String> categoryAdapter,
                                    CleanArrayAdapter<String> generalCompetencyAdapter,
                                    CleanArrayAdapter<FirebaseDataManager.CourseInfo> majorCoursesAdapter,
                                    LinearLayout layoutMajorCourses, LinearLayout layoutGeneralManualInput,
                                    LinearLayout layoutManualInput, Spinner spinnerCourseCategory,
                                    Spinner spinnerGeneralCompetency) {
        // 라디오 상태 복원
        if (lastSelectedIsMajor) radioMajor.setChecked(true); else radioGeneral.setChecked(true);

        updateCategorySpinner(categoryAdapter, lastSelectedIsMajor);
        clearCourseSpinner(majorCoursesAdapter);
        setupCompetencySpinner(generalCompetencyAdapter);

        // 지연 제거: 즉시 실행
        if (lastSelectedCategoryPosition >= 0 && lastSelectedCategoryPosition < categoryAdapter.getCount()) {
            spinnerCourseCategory.setSelection(lastSelectedCategoryPosition, false);
            updateUIForCategorySelection(lastSelectedCategoryPosition, categoryAdapter, majorCoursesAdapter,
                    layoutMajorCourses, layoutGeneralManualInput, layoutManualInput);
        }

        // 지연 제거: 즉시 실행
        if (lastSelectedCompetencyPosition >= 0 && lastSelectedCompetencyPosition < generalCompetencyAdapter.getCount()) {
            spinnerGeneralCompetency.setSelection(lastSelectedCompetencyPosition, false);
        }

        if (lastSelectedIsMajor) {
            layoutMajorCourses.setVisibility(View.VISIBLE);
            layoutGeneralManualInput.setVisibility(View.GONE);
            layoutManualInput.setVisibility(View.GONE);
        } else {
            layoutMajorCourses.setVisibility(View.VISIBLE);
            layoutGeneralManualInput.setVisibility(View.GONE);
            layoutManualInput.setVisibility(View.GONE);
        }

        // 초기 카테고리 로딩 - 지연 제거
        if (categoryAdapter.getCount() > 0) {
            int pos = Math.max(0, Math.min(lastSelectedCategoryPosition, categoryAdapter.getCount() - 1));
            String selectedCategory = categoryAdapter.getItem(pos);
            Log.d(TAG, "초기 카테고리 로딩: " + selectedCategory + " (pos=" + pos + ")");
            if (!"교양선택".equals(selectedCategory)) {
                loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
            }
        }
    }

    private void updateUIForCategorySelection(int position, CleanArrayAdapter<String> categoryAdapter,
                                              CleanArrayAdapter<FirebaseDataManager.CourseInfo> majorCoursesAdapter,
                                              LinearLayout layoutMajorCourses, LinearLayout layoutGeneralManualInput,
                                              LinearLayout layoutManualInput) {
        if (position < 0) return;
        String selectedCategory = categoryAdapter.getItem(position);

        if (lastSelectedIsMajor) {
            layoutMajorCourses.setVisibility(View.VISIBLE);
            layoutGeneralManualInput.setVisibility(View.GONE);
            layoutManualInput.setVisibility(View.GONE);
            loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
        } else {
            if ("교양선택".equals(selectedCategory)) {
                layoutMajorCourses.setVisibility(View.GONE);
                layoutGeneralManualInput.setVisibility(View.VISIBLE);
                layoutManualInput.setVisibility(View.GONE);
            } else {
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
                loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
            }
        }
    }

    /** 간단한 스피너 어댑터(기본 동작) */
    private static class CleanArrayAdapter<T> extends ArrayAdapter<T> {
        public CleanArrayAdapter(android.content.Context context, int resource) { super(context, resource); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 보조(이전 버전 유산 메서드) — 현재 탭 방식에서 직접 사용하진 않지만 유지
    // ─────────────────────────────────────────────────────────────────────────
    private void refreshCourseDisplay() {
        // 탭 방식으로 전환되어 이 메서드는 현재 사용하지 않습니다.
        // 필요 시 카테고리별 섹션 렌더링 로직을 여기에 추가하세요.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 모델/로딩 메시지/수명주기
    // ─────────────────────────────────────────────────────────────────────────
    public static class Course implements android.os.Parcelable {
        private String category;
        private String name;
        private int credits;
        private String groupId;
        private String competency; // 교양선택 역량

        public Course(String category, String name, int credits) {
            this(category, name, credits, null, null);
        }
        public Course(String category, String name, int credits, String groupId) {
            this(category, name, credits, groupId, null);
        }
        public Course(String category, String name, int credits, String groupId, String competency) {
            this.category = category;
            this.name = name;
            this.credits = credits;
            this.groupId = groupId;
            this.competency = competency;
        }

        protected Course(android.os.Parcel in) {
            category = in.readString();
            name = in.readString();
            credits = in.readInt();
            groupId = in.readString();
            competency = in.readString();
        }

        public static final Creator<Course> CREATOR = new Creator<Course>() {
            @Override public Course createFromParcel(android.os.Parcel in) { return new Course(in); }
            @Override public Course[] newArray(int size) { return new Course[size]; }
        };

        @Override public int describeContents() { return 0; }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
            dest.writeString(category);
            dest.writeString(name);
            dest.writeInt(credits);
            dest.writeString(groupId);
            dest.writeString(competency);
        }

        @Override public String toString() { return String.format("[%s] %s (%d학점)", category, name, credits); }

        public String getCategory() { return category; }
        public String getName() { return name; }
        public int getCredits() { return credits; }
        public String getGroupId() { return groupId; }
        public String getCompetency() { return competency; }
    }

    /** 로딩 메시지: 리스트 컨테이너에 안전하게 표시 */
    private void showLoadingMessage(String message) {
        runOnUiThread(() -> {
            layoutSelectedCategoryCourses.removeAllViews();
            if (textEmptyCourses.getParent() != layoutSelectedCategoryCourses) {
                if (textEmptyCourses.getParent() instanceof LinearLayout) {
                    ((LinearLayout) textEmptyCourses.getParent()).removeView(textEmptyCourses);
                }
                layoutSelectedCategoryCourses.addView(textEmptyCourses);
            }
            textEmptyCourses.setText(message);
            textEmptyCourses.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        });
    }

    /** 로딩 종료: 기본 빈 메시지로 되돌림(실제 렌더는 updateCourseDisplay가 담당) */
    private void hideLoadingMessage() {
        runOnUiThread(() -> {
            textEmptyCourses.setText("선택된 카테고리에 표시할 강의가 없습니다.");
            textEmptyCourses.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 필요한 경우 Firebase 리스너/리소스 해제
        if (layoutSelectedCategoryCourses != null) layoutSelectedCategoryCourses.removeAllViews();
        isLoadingCourses = false;
        lastLoadedCategory = null;
        Log.d(TAG, "CourseInputActivity destroyed - 리소스 정리 완료");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 상태 저장/복원
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(S_IS_MAJOR_GROUP, isMajorGroupSelected);
        out.putString(S_CURRENT_TAB, currentSelectedTab);
        out.putParcelableArrayList(S_COURSE_LIST, new ArrayList<>(courseList));
        out.putBoolean(S_LAST_IS_MAJOR, lastSelectedIsMajor);
        out.putInt(S_LAST_CAT_POS, lastSelectedCategoryPosition);
        out.putInt(S_LAST_COMP_POS, lastSelectedCompetencyPosition);
    }

    private void restoreActivityState(Bundle in) {
        isMajorGroupSelected = in.getBoolean(S_IS_MAJOR_GROUP, true);
        currentSelectedTab = in.getString(S_CURRENT_TAB, "전공필수");
        ArrayList<Course> saved;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            saved = in.getParcelableArrayList(S_COURSE_LIST, CourseInputActivity.Course.class);
        } else {
            saved = in.getParcelableArrayList(S_COURSE_LIST);
        }
        if (saved != null) courseList.addAll(saved);

        lastSelectedIsMajor = in.getBoolean(S_LAST_IS_MAJOR, true);
        lastSelectedCategoryPosition = in.getInt(S_LAST_CAT_POS, 0);
        lastSelectedCompetencyPosition = in.getInt(S_LAST_COMP_POS, 0);

        // 그룹 버튼/컨테이너 상태 복원
        if (isMajorGroupSelected) {
            switchToMajorGroup();
        } else {
            switchToGeneralGroup();
        }
        // 저장된 탭으로 재전환(그룹 전환에서 덮어쓴 기본 탭을 다시 설정)
        switchTab(currentSelectedTab, getTabChip(currentSelectedTab));

        // 목록/버튼 상태 갱신
        updateCourseDisplay();
        updateAnalyzeButtonState();
    }

    /**
     * 대체과목 리스트 보기 다이얼로그 표시
     */
    private void showReplacementCoursesDialog() {
        // 현재 학번/학부/트랙 정보로 대체 규칙 로드
        loadReplacementRules();
    }

    /**
     * Firestore에서 대체 규칙 로드
     */
    private void loadReplacementRules() {
        dataManager.loadGraduationRules(selectedYear, selectedDepartment, selectedTrack,
            new FirebaseDataManager.OnGraduationRulesLoadedListener() {
                @Override
                public void onSuccess(sprout.app.sakmvp1.models.GraduationRules rules) {
                    List<sprout.app.sakmvp1.models.ReplacementRule> replacementRules =
                        rules.getReplacementRules();

                    if (replacementRules == null || replacementRules.isEmpty()) {
                        Toast.makeText(CourseInputActivity.this,
                            "설정된 대체과목 규칙이 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showReplacementSelectionDialog(replacementRules);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(CourseInputActivity.this,
                        "대체과목 규칙 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * 대체과목 선택 다이얼로그 표시
     */
    private void showReplacementSelectionDialog(List<sprout.app.sakmvp1.models.ReplacementRule> rules) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_replacement_course_selection, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // UI 요소 참조
        com.google.android.material.textfield.TextInputEditText etSearch =
            dialogView.findViewById(R.id.et_search_replacement);
        androidx.recyclerview.widget.RecyclerView rvReplacements =
            dialogView.findViewById(R.id.rv_replacement_courses);
        com.google.android.material.button.MaterialButton btnCancel =
            dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnAddSelected =
            dialogView.findViewById(R.id.btn_add_selected);

        // Adapter 설정
        ReplacementCourseSelectionAdapter adapter = new ReplacementCourseSelectionAdapter(rules);
        rvReplacements.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvReplacements.setAdapter(adapter);

        // 검색 기능
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 취소 버튼
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 선택한 과목 추가 버튼
        btnAddSelected.setOnClickListener(v -> {
            List<sprout.app.sakmvp1.models.ReplacementRule.CourseInfo> selectedCourses =
                adapter.getSelectedCourses();

            if (selectedCourses.isEmpty()) {
                Toast.makeText(this, "과목을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 선택한 과목들을 courseList에 추가
            for (sprout.app.sakmvp1.models.ReplacementRule.CourseInfo courseInfo : selectedCourses) {
                Course newCourse = new Course(
                    courseInfo.getCategory(),
                    courseInfo.getName(),
                    courseInfo.getCredits()
                );
                courseList.add(newCourse);
            }

            updateCourseDisplay();
            updateAnalyzeButtonState();

            Toast.makeText(this, selectedCourses.size() + "개 과목이 추가되었습니다",
                Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 대체과목 선택 Adapter
     */
    class ReplacementCourseSelectionAdapter extends
        androidx.recyclerview.widget.RecyclerView.Adapter<ReplacementCourseSelectionAdapter.ViewHolder> {

        private List<sprout.app.sakmvp1.models.ReplacementRule> allRules;
        private List<sprout.app.sakmvp1.models.ReplacementRule> filteredRules;
        private List<sprout.app.sakmvp1.models.ReplacementRule.CourseInfo> selectedCourses = new ArrayList<>();

        ReplacementCourseSelectionAdapter(List<sprout.app.sakmvp1.models.ReplacementRule> rules) {
            this.allRules = new ArrayList<>(rules);
            this.filteredRules = new ArrayList<>(rules);
        }

        public void filter(String query) {
            if (query.isEmpty()) {
                filteredRules = new ArrayList<>(allRules);
            } else {
                filteredRules = new ArrayList<>();
                String lowerQuery = query.toLowerCase();
                for (sprout.app.sakmvp1.models.ReplacementRule rule : allRules) {
                    if (rule.getDiscontinuedCourse().getName().toLowerCase().contains(lowerQuery)) {
                        filteredRules.add(rule);
                        continue;
                    }
                    for (sprout.app.sakmvp1.models.ReplacementRule.CourseInfo course :
                        rule.getReplacementCourses()) {
                        if (course.getName().toLowerCase().contains(lowerQuery)) {
                            filteredRules.add(rule);
                            break;
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        public List<sprout.app.sakmvp1.models.ReplacementRule.CourseInfo> getSelectedCourses() {
            return selectedCourses;
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_replacement_course_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            sprout.app.sakmvp1.models.ReplacementRule rule = filteredRules.get(position);
            holder.bind(rule);
        }

        @Override
        public int getItemCount() {
            return filteredRules.size();
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView tvDiscontinuedName, tvDiscontinuedCategory, tvDiscontinuedCredits;
            LinearLayout containerReplacementOptions;

            ViewHolder(View itemView) {
                super(itemView);
                tvDiscontinuedName = itemView.findViewById(R.id.tv_discontinued_name);
                tvDiscontinuedCategory = itemView.findViewById(R.id.tv_discontinued_category);
                tvDiscontinuedCredits = itemView.findViewById(R.id.tv_discontinued_credits);
                containerReplacementOptions = itemView.findViewById(R.id.container_replacement_options);
            }

            void bind(sprout.app.sakmvp1.models.ReplacementRule rule) {
                // 폐강된 과목 정보 표시
                sprout.app.sakmvp1.models.ReplacementRule.CourseInfo discontinued =
                    rule.getDiscontinuedCourse();
                tvDiscontinuedName.setText(discontinued.getName());
                tvDiscontinuedCategory.setText(discontinued.getCategory());
                tvDiscontinuedCredits.setText(discontinued.getCredits() + "학점");

                // 대체 가능한 과목들 표시
                containerReplacementOptions.removeAllViews();
                for (sprout.app.sakmvp1.models.ReplacementRule.CourseInfo replacement :
                    rule.getReplacementCourses()) {

                    View optionView = LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.item_replacement_option, containerReplacementOptions, false);

                    com.google.android.material.checkbox.MaterialCheckBox cbSelect =
                        optionView.findViewById(R.id.cb_select);
                    TextView tvName = optionView.findViewById(R.id.tv_replacement_name);
                    TextView tvCategory = optionView.findViewById(R.id.tv_replacement_category);
                    TextView tvCredits = optionView.findViewById(R.id.tv_replacement_credits);

                    tvName.setText(replacement.getName());
                    tvCategory.setText(replacement.getCategory());
                    tvCredits.setText(replacement.getCredits() + "학점");

                    cbSelect.setChecked(selectedCourses.contains(replacement));

                    cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            if (!selectedCourses.contains(replacement)) {
                                selectedCourses.add(replacement);
                            }
                        } else {
                            selectedCourses.remove(replacement);
                        }
                    });

                    optionView.setOnClickListener(v -> cbSelect.setChecked(!cbSelect.isChecked()));

                    containerReplacementOptions.addView(optionView);
                }
            }
        }
    }
}
