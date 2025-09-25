package sprout.app.sakmvp1;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 수강 강의 입력 화면
 *
 * 역할
 * - (이전 화면에서 전달된) 학번/학과/트랙 + 추가 졸업 요건 표시
 * - 전공/교양 그룹과 탭(전공필수/선택/학부공통|전공심화, 교양필수/선택) 전환
 * - 강의 추가(전공/교양, 자동/수동 입력), 삭제
 * - 졸업요건 분석 화면으로 입력 데이터 전달
 *
 * 안정성/UX
 * - WindowInsetsCompat 적용(안전영역)
 * - Intent 키 상수 사용(AdditionalRequirementsActivity의 상수 재사용)
 * - 상태 저장/복원(그룹/탭/강의목록/다이얼로그 선택값)
 * - 중복 로딩/중복 클릭 방지
 * - 로딩 메시지 표시 안정화
 */
public class CourseInputActivity extends AppCompatActivity {

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
    private TextView btnMajorGroup;
    private TextView btnGeneralGroup;
    private View majorTabsContainer;
    private View generalTabsContainer;

    // 탭 버튼들
    private TextView tabMajorRequired;
    private TextView tabMajorElective;
    private TextView tabMajorAdvanced;
    private TextView tabGeneralRequired;
    private TextView tabGeneralElective;

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

    // 중복 로딩 방지
    private boolean isLoadingCourses = false;
    private String lastLoadedCategory = null;
    private long lastLoadTime = 0;
    private static final long MIN_LOAD_INTERVAL = 2000; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HighContrastHelper.applyHighContrastTheme(this); // 고대비 테마

        setContentView(R.layout.activity_course_input);

        // 기본 액션바 숨김(커스텀 Toolbar 사용)
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        getIntentData();      // 인텐트 데이터 파싱
        initViews();          // 뷰 바인딩/매니저 초기화
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
        additionalRequirements = intent.getParcelableExtra(AdditionalRequirementsActivity.EXTRA_REQUIREMENTS);

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
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
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
    // ─────────────────────────────────────────────────────────────────────────
    private void showAddCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_course, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // 다이얼로그 UI
        RadioGroup radioGroupCourseType = dialogView.findViewById(R.id.radio_group_course_type);
        RadioButton radioMajor = dialogView.findViewById(R.id.radio_major);
        RadioButton radioGeneral = dialogView.findViewById(R.id.radio_general);
        LinearLayout layoutMajorCourses = dialogView.findViewById(R.id.layout_major_courses);
        LinearLayout layoutGeneralManualInput = dialogView.findViewById(R.id.layout_general_manual_input);
        LinearLayout layoutManualInput = dialogView.findViewById(R.id.layout_manual_input);
        Spinner spinnerMajorCourses = dialogView.findViewById(R.id.spinner_major_courses);
        Spinner spinnerCourseCategory = dialogView.findViewById(R.id.spinner_course_category);
        EditText editGeneralCourseName = dialogView.findViewById(R.id.edit_general_course_name);
        EditText editGeneralCourseCredits = dialogView.findViewById(R.id.edit_general_course_credits);
        Spinner spinnerGeneralCompetency = dialogView.findViewById(R.id.spinner_general_competency);
        EditText editCourseName = dialogView.findViewById(R.id.edit_course_name);
        EditText editCourseCredits = dialogView.findViewById(R.id.edit_course_credits);
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

        // 저장된 다이얼로그 상태 복원
        restoreDialogState(
                radioMajor, radioGeneral, categoryAdapter, generalCompetencyAdapter, majorCoursesAdapter,
                layoutMajorCourses, layoutGeneralManualInput, layoutManualInput, spinnerCourseCategory, spinnerGeneralCompetency
        );

        // 전공/교양 전환
        radioGroupCourseType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMajor = (checkedId == R.id.radio_major);
            lastSelectedIsMajor = isMajor; // 상태 저장

            if (isMajor) {
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
            } else {
                // 교양 선택 시: 기본은 교양필수 방식(UI 동일)
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
            }

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

            // 기본 카테고리 자동 로드(교양선택 제외)
            spinnerCourseCategory.postDelayed(() -> {
                if (categoryAdapter.getCount() > 0) {
                    String selectedCategory = categoryAdapter.getItem(0);
                    Log.d(TAG, "라디오 전환 후 자동 로드: " + selectedCategory + " (전공? " + isMajor + ")");
                    if (!"교양선택".equals(selectedCategory)) {
                        loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                    }
                }
            }, 100);
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
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 추가
        btnAdd.setOnClickListener(v -> {
            boolean isMajor = radioGroupCourseType.getCheckedRadioButtonId() == R.id.radio_major;

            if (isMajor) {
                // 전공: 스피너 선택 우선, 없으면 수동
                if (spinnerMajorCourses.getSelectedItemPosition() >= 0 && majorCoursesAdapter.getCount() > 0) {
                    if (addCourseFromDialog(spinnerCourseCategory, spinnerMajorCourses, categoryAdapter, majorCoursesAdapter)) {
                        dialog.dismiss();
                    }
                } else {
                    if (addCourseFromManualInput(spinnerCourseCategory, editCourseName, editCourseCredits, categoryAdapter)) {
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
                        dialog.dismiss();
                    }
                } else {
                    if (spinnerMajorCourses.getSelectedItemPosition() >= 0 && majorCoursesAdapter.getCount() > 0) {
                        if (addCourseFromDialog(spinnerCourseCategory, spinnerMajorCourses, categoryAdapter, majorCoursesAdapter)) {
                            dialog.dismiss();
                        }
                    } else {
                        if (addCourseFromManualInput(spinnerCourseCategory, editCourseName, editCourseCredits, categoryAdapter)) {
                            dialog.dismiss();
                        }
                    }
                }
            }
        });

        dialog.show();
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

    /** 카테고리별 강의 로드(중복/과한 요청 방지 포함) */
    private void loadCoursesForCategory(String category, CleanArrayAdapter<FirebaseDataManager.CourseInfo> courseAdapter) {
        Log.d(TAG, "loadCoursesForCategory: " + category);
        long now = System.currentTimeMillis();

        if (isLoadingCourses && category.equals(lastLoadedCategory)) {
            Log.d(TAG, "중복 로딩 방지: 이미 로딩 중 => " + category);
            return;
        }
        if (category.equals(lastLoadedCategory) && (now - lastLoadTime) < MIN_LOAD_INTERVAL) {
            Log.d(TAG, "시간 기반 중복 방지: 요청 간격 짧음 => " + category);
            return;
        }

        isLoadingCourses = true;
        lastLoadedCategory = category;
        lastLoadTime = now;

        courseAdapter.clear();
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
                            courseAdapter.addAll(filtered);
                            courseAdapter.notifyDataSetChanged();
                            hideLoadingMessage();
                            Log.d(TAG, category + " 강의 로드 성공: " + filtered.size());
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
    private void switchTab(String tabName, TextView ignored) {
        currentSelectedTab = tabName;
        updateTabDisplay();
        updateCourseDisplay();
    }

    private void updateTabDisplay() {
        resetTabButton(tabMajorRequired);
        resetTabButton(tabMajorElective);
        resetTabButton(tabMajorAdvanced);
        resetTabButton(tabGeneralRequired);
        resetTabButton(tabGeneralElective);

        TextView active = getTabButton(currentSelectedTab);
        if (active != null) setActiveTabButton(active);
    }

    private void resetTabButton(TextView btn) {
        btn.setBackgroundResource(R.drawable.folder_tab_inactive);
        btn.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void setActiveTabButton(TextView btn) {
        btn.setBackgroundResource(R.drawable.folder_tab_active);
        btn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private TextView getTabButton(String tabName) {
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

        btnMajorGroup.setText("전공 ▼");
        btnMajorGroup.setBackgroundResource(R.drawable.button_primary);
        btnMajorGroup.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        btnGeneralGroup.setText("교양 ▷");
        btnGeneralGroup.setBackgroundResource(R.drawable.spinner_background);
        btnGeneralGroup.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        majorTabsContainer.setVisibility(View.VISIBLE);
        generalTabsContainer.setVisibility(View.GONE);

        switchTab("전공필수", tabMajorRequired);
    }

    /** 교양 그룹으로 전환(탭 컨테이너/버튼 스타일) */
    private void switchToGeneralGroup() {
        isMajorGroupSelected = false;

        btnGeneralGroup.setText("교양 ▼");
        btnGeneralGroup.setBackgroundResource(R.drawable.button_primary);
        btnGeneralGroup.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        btnMajorGroup.setText("전공 ▷");
        btnMajorGroup.setBackgroundResource(R.drawable.spinner_background);
        btnMajorGroup.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        majorTabsContainer.setVisibility(View.GONE);
        generalTabsContainer.setVisibility(View.VISIBLE);

        switchTab("교양필수", tabGeneralRequired);
    }

    /** 선택된 탭 카테고리만 표시 */
    private void updateCourseDisplay() {
        layoutSelectedCategoryCourses.removeAllViews();

        List<Course> filtered = new ArrayList<>();
        for (Course c : courseList) {
            if (c.getCategory().equals(currentSelectedTab)) filtered.add(c);
        }

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
        } else {
            for (Course c : filtered) {
                createCourseItemView(c);
            }
        }
    }

    /** 개별 강의 카드 뷰 생성 */
    private void createCourseItemView(Course course) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(20, 16, 20, 16);
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.spinner_background));
        card.setElevation(4);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, 2, 0, 2);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoParams);

        TextView name = new TextView(this);
        name.setText(course.getName());
        name.setTextSize(16);
        name.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        name.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView credits = new TextView(this);
        credits.setText(String.format("%d학점", course.getCredits()));
        credits.setTextSize(13);
        credits.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        info.addView(name);
        info.addView(credits);

        Button delete = new Button(this);
        delete.setText("✕");
        delete.setTextSize(16);
        delete.setTypeface(null, android.graphics.Typeface.BOLD);
        delete.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(80, 80);
        delParams.setMargins(12, 0, 0, 0);
        delParams.gravity = android.view.Gravity.CENTER_VERTICAL;
        delete.setLayoutParams(delParams);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        bg.setStroke(2, ContextCompat.getColor(this, android.R.color.holo_red_dark));
        delete.setBackground(bg);

        delete.setOnClickListener(v -> {
            courseList.remove(course);
            updateCourseDisplay();
            updateAnalyzeButtonState();
            Toast.makeText(this, course.getName() + " 강의가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
        });

        card.addView(info);
        card.addView(delete);
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
        btnAnalyzeGraduation.postDelayed(() -> btnAnalyzeGraduation.setEnabled(true), 500);
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

        spinnerCourseCategory.postDelayed(() -> {
            if (lastSelectedCategoryPosition >= 0 && lastSelectedCategoryPosition < categoryAdapter.getCount()) {
                spinnerCourseCategory.setSelection(lastSelectedCategoryPosition, false);
                updateUIForCategorySelection(lastSelectedCategoryPosition, categoryAdapter, majorCoursesAdapter,
                        layoutMajorCourses, layoutGeneralManualInput, layoutManualInput);
            }
        }, 100);

        spinnerGeneralCompetency.postDelayed(() -> {
            if (lastSelectedCompetencyPosition >= 0 && lastSelectedCompetencyPosition < generalCompetencyAdapter.getCount()) {
                spinnerGeneralCompetency.setSelection(lastSelectedCompetencyPosition, false);
            }
        }, 100);

        if (lastSelectedIsMajor) {
            layoutMajorCourses.setVisibility(View.VISIBLE);
            layoutGeneralManualInput.setVisibility(View.GONE);
            layoutManualInput.setVisibility(View.GONE);
        } else {
            layoutMajorCourses.setVisibility(View.VISIBLE);
            layoutGeneralManualInput.setVisibility(View.GONE);
            layoutManualInput.setVisibility(View.GONE);
        }

        spinnerCourseCategory.postDelayed(() -> {
            if (categoryAdapter.getCount() > 0) {
                int pos = Math.max(0, Math.min(lastSelectedCategoryPosition, categoryAdapter.getCount() - 1));
                String selectedCategory = categoryAdapter.getItem(pos);
                Log.d(TAG, "초기 카테고리 로딩: " + selectedCategory + " (pos=" + pos + ")");
                if (!"교양선택".equals(selectedCategory)) {
                    loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                }
            }
        }, 150);
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
        ArrayList<Course> saved = in.getParcelableArrayList(S_COURSE_LIST);
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
        switchTab(currentSelectedTab, getTabButton(currentSelectedTab));

        // 목록/버튼 상태 갱신
        updateCourseDisplay();
        updateAnalyzeButtonState();
    }
}
