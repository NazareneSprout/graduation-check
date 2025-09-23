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
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CourseInputActivity extends AppCompatActivity {

    private static final String TAG = "CourseInput";

    // UI 컴포넌트
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

    // 탭 버튼들 (폴더 형태)
    private TextView tabMajorRequired;
    private TextView tabMajorElective;
    private TextView tabMajorAdvanced;
    private TextView tabGeneralRequired;
    private TextView tabGeneralElective;

    // 현재 선택된 그룹과 탭
    private boolean isMajorGroupSelected = true;
    private String currentSelectedTab = "전공필수";

    // Firebase 데이터 매니저
    private FirebaseDataManager dataManager;

    // 데이터
    private String selectedYear, selectedDepartment, selectedTrack;
    private List<Course> courseList;
    private AdditionalRequirementsActivity.AdditionalRequirements additionalRequirements;

    // 다이얼로그 상태 저장 변수들
    private boolean lastSelectedIsMajor = true; // 마지막으로 선택한 라디오 버튼 상태
    private int lastSelectedCategoryPosition = 0; // 마지막으로 선택한 카테고리 위치
    private int lastSelectedCompetencyPosition = 0; // 마지막으로 선택한 역량 위치

    // 중복 로딩 방지를 위한 상태 관리
    private boolean isLoadingCourses = false;
    private String lastLoadedCategory = null;
    private long lastLoadTime = 0;
    private static final long MIN_LOAD_INTERVAL = 2000; // 최소 로딩 간격 (밀리초)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_input);

        // 시스템 UI와의 중복 방지
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getIntentData();
        initViews();
        setupSystemUI();
        setupToolbar();
        setupSpinner();
        setupListeners();
        displayStudentInfo();
    }

    private void setupSystemUI() {
        // 시스템 UI 인셋 처리
        findViewById(R.id.main).setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarHeight = insets.getSystemWindowInsetTop();
            int navigationBarHeight = insets.getSystemWindowInsetBottom();

            v.setPadding(
                insets.getSystemWindowInsetLeft(),
                statusBarHeight,
                insets.getSystemWindowInsetRight(),
                navigationBarHeight
            );
            return insets;
        });
    }

    private void getIntentData() {
        Intent intent = getIntent();
        selectedYear = intent.getStringExtra("year");
        selectedDepartment = intent.getStringExtra("department");
        selectedTrack = intent.getStringExtra("track");
        additionalRequirements = intent.getParcelableExtra("additionalRequirements");

        if (selectedYear == null || selectedDepartment == null || selectedTrack == null) {
            Toast.makeText(this, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 추가 요건 데이터 로깅
        if (additionalRequirements != null) {
            Log.d(TAG, "추가 졸업 요건 수신: " + additionalRequirements.toString());
        } else {
            Log.w(TAG, "추가 졸업 요건 데이터를 받지 못했습니다.");
        }
    }

    private void initViews() {
        textViewStudentInfo = findViewById(R.id.text_view_student_info);
        btnAddCourse = findViewById(R.id.btn_add_course);
        layoutSelectedCategoryCourses = findViewById(R.id.layout_selected_category_courses);
        textEmptyCourses = findViewById(R.id.text_empty_courses);
        btnAnalyzeGraduation = findViewById(R.id.btn_analyze_graduation);
        toolbar = findViewById(R.id.toolbar_course_input);

        // 그룹 전환 버튼들 초기화
        btnMajorGroup = findViewById(R.id.btn_major_group);
        btnGeneralGroup = findViewById(R.id.btn_general_group);
        majorTabsContainer = findViewById(R.id.major_tabs_container);
        generalTabsContainer = findViewById(R.id.general_tabs_container);

        // 탭 버튼들 초기화
        tabMajorRequired = findViewById(R.id.tab_major_required);
        tabMajorElective = findViewById(R.id.tab_major_elective);
        tabMajorAdvanced = findViewById(R.id.tab_major_advanced);
        tabGeneralRequired = findViewById(R.id.tab_general_required);
        tabGeneralElective = findViewById(R.id.tab_general_elective);

        // Firebase 데이터 매니저 초기화 (싱글톤)
        dataManager = FirebaseDataManager.getInstance();

        // 강의 목록 초기화
        courseList = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("수강 강의 입력");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSpinner() {
        // 다이얼로그로 이동했으므로 여기서는 제거
    }

    private void setupListeners() {
        btnAddCourse.setOnClickListener(v -> showAddCourseDialog());
        btnAnalyzeGraduation.setOnClickListener(v -> analyzeGraduation());

        // 그룹 전환 버튼 리스너
        btnMajorGroup.setOnClickListener(v -> switchToMajorGroup());
        btnGeneralGroup.setOnClickListener(v -> switchToGeneralGroup());

        // 탭 리스너들
        tabMajorRequired.setOnClickListener(v -> switchTab("전공필수", tabMajorRequired));
        tabMajorElective.setOnClickListener(v -> switchTab("전공선택", tabMajorElective));
        tabMajorAdvanced.setOnClickListener(v -> {
            // 20, 21, 22학번은 "학부공통", 나머지는 "전공심화"
            boolean isOldCurriculum = "2020".equals(selectedYear) || "2021".equals(selectedYear) || "2022".equals(selectedYear);
            String tabName = isOldCurriculum ? "학부공통" : "전공심화";
            switchTab(tabName, tabMajorAdvanced);
        });
        tabGeneralRequired.setOnClickListener(v -> switchTab("교양필수", tabGeneralRequired));
        tabGeneralElective.setOnClickListener(v -> switchTab("교양선택", tabGeneralElective));

        // 초기 설정
        updateTabDisplay();
        switchToMajorGroup(); // 초기에는 전공 그룹 표시
    }

    private void displayStudentInfo() {
        String studentInfo = String.format("%s학번 %s %s", selectedYear, selectedDepartment, selectedTrack);
        textViewStudentInfo.setText(studentInfo);
        Log.d(TAG, "학생 정보 표시: " + studentInfo);

        // 학번에 따라 탭 텍스트 설정
        updateTabTexts();
    }

    private void updateTabTexts() {
        // 20, 21, 22학번은 "전공심화" 대신 "학부공통" 사용
        boolean isOldCurriculum = "2020".equals(selectedYear) || "2021".equals(selectedYear) || "2022".equals(selectedYear);

        if (isOldCurriculum) {
            tabMajorAdvanced.setText("학부공통");
            // 현재 선택된 탭이 "전공심화"였다면 "학부공통"으로 변경
            if ("전공심화".equals(currentSelectedTab)) {
                currentSelectedTab = "학부공통";
            }
        } else {
            tabMajorAdvanced.setText("전공심화");
            // 현재 선택된 탭이 "학부공통"이었다면 "전공심화"로 변경
            if ("학부공통".equals(currentSelectedTab)) {
                currentSelectedTab = "전공심화";
            }
        }
    }

    private void showAddCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_course, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // 다이얼로그 UI 컴포넌트
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

        // 어댑터들 - 깔끔한 커스텀 어댑터 사용
        CleanArrayAdapter<FirebaseDataManager.CourseInfo> majorCoursesAdapter =
            new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        majorCoursesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMajorCourses.setAdapter(majorCoursesAdapter);

        CleanArrayAdapter<String> categoryAdapter =
            new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourseCategory.setAdapter(categoryAdapter);

        // 교양선택 역량 스피너 어댑터
        CleanArrayAdapter<String> generalCompetencyAdapter =
            new CleanArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        generalCompetencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGeneralCompetency.setAdapter(generalCompetencyAdapter);

        // 저장된 상태 복원
        restoreDialogState(radioMajor, radioGeneral, categoryAdapter, generalCompetencyAdapter,
                          majorCoursesAdapter, layoutMajorCourses, layoutGeneralManualInput, layoutManualInput,
                          spinnerCourseCategory, spinnerGeneralCompetency);

        // 라디오 버튼 리스너
        radioGroupCourseType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMajor = (checkedId == R.id.radio_major);

            // 상태 저장
            lastSelectedIsMajor = isMajor;

            // UI 레이아웃 설정
            if (isMajor) {
                // 전공 선택시
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
            } else {
                // 교양 선택시 - 기본적으로는 기존 스피너 표시 (교양필수용)
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
            }

            // 카테고리 스피너 업데이트 및 강의 스피너 초기화
            updateCategorySpinner(categoryAdapter, isMajor);
            clearCourseSpinner(majorCoursesAdapter);

            // 스피너 상태 초기화 - CleanArrayAdapter에서는 첫 번째 항목이 실제 데이터
            if (categoryAdapter.getCount() > 0) {
                spinnerCourseCategory.setSelection(0);
            }
            if (majorCoursesAdapter.getCount() > 0) {
                spinnerMajorCourses.setSelection(0);
            }
            if (!isMajor) {
                // 교양선택 입력 필드들 초기화
                editGeneralCourseName.setText("");
                editGeneralCourseCredits.setText("");
                if (generalCompetencyAdapter.getCount() > 0) {
                    spinnerGeneralCompetency.setSelection(0);
                }
            }

            // 수동 입력 필드 초기화
            editCourseName.setText("");
            editCourseCredits.setText("");

            // 라디오 버튼 전환 후 기본 카테고리의 강의를 자동 로드
            spinnerCourseCategory.postDelayed(() -> {
                if (categoryAdapter.getCount() > 0) {
                    String selectedCategory = categoryAdapter.getItem(0);  // 첫 번째 카테고리
                    Log.d(TAG, "라디오 버튼 전환 후 자동 로딩: " + selectedCategory + " (전공여부: " + isMajor + ")");

                    // 교양선택이 아닌 경우에만 강의 로드
                    if (!"교양선택".equals(selectedCategory)) {
                        loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                    }
                }
            }, 100);
        });

        // 세부분류 스피너 리스너 - 전공/교양 구분 및 교양선택/교양필수 구분 처리
        spinnerCourseCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                boolean isMajor = radioGroupCourseType.getCheckedRadioButtonId() == R.id.radio_major;

                if (position >= 0) {
                    String selectedCategory = categoryAdapter.getItem(position);

                    // 카테고리 선택 상태 저장
                    lastSelectedCategoryPosition = position;

                    if (isMajor) {
                        // 전공인 경우 기존 로직
                        layoutMajorCourses.setVisibility(View.VISIBLE);
                        layoutGeneralManualInput.setVisibility(View.GONE);
                        layoutManualInput.setVisibility(View.GONE);
                        loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                    } else {
                        // 교양인 경우
                        if ("교양선택".equals(selectedCategory)) {
                            // 교양선택일 때 수동 입력 표시
                            layoutMajorCourses.setVisibility(View.GONE);
                            layoutGeneralManualInput.setVisibility(View.VISIBLE);
                            layoutManualInput.setVisibility(View.GONE);
                        } else {
                            // 교양필수일 때는 기존 방식 (majorCourses 스피너 사용)
                            layoutMajorCourses.setVisibility(View.VISIBLE);
                            layoutGeneralManualInput.setVisibility(View.GONE);
                            layoutManualInput.setVisibility(View.GONE);
                            // 교양필수 강의 로드
                            loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                        }
                    }
                } else {
                    // 선택 안됨 - 기본 상태로 복원
                    clearCourseSpinner(majorCoursesAdapter);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });



        // 취소 버튼
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 추가 버튼
        btnAdd.setOnClickListener(v -> {
            boolean isMajor = radioGroupCourseType.getCheckedRadioButtonId() == R.id.radio_major;

            if (isMajor) {
                // 전공 과목 처리
                if (spinnerMajorCourses.getSelectedItemPosition() >= 0 && majorCoursesAdapter.getCount() > 0) {
                    // 스피너에서 선택된 강의 추가
                    if (addCourseFromDialog(spinnerCourseCategory, spinnerMajorCourses, categoryAdapter, majorCoursesAdapter)) {
                        dialog.dismiss();
                    }
                } else {
                    // 스피너에서 선택되지 않았다면 수동 입력으로 처리
                    if (addCourseFromManualInput(spinnerCourseCategory, editCourseName, editCourseCredits, categoryAdapter)) {
                        dialog.dismiss();
                    }
                }
            } else {
                // 교양 과목 처리
                String selectedCategory = spinnerCourseCategory.getSelectedItemPosition() >= 0 ?
                    categoryAdapter.getItem(spinnerCourseCategory.getSelectedItemPosition()) : "";

                if ("교양선택".equals(selectedCategory)) {
                    // 교양선택인 경우 수동 입력 사용
                    if (addGeneralCourseFromManualInput(spinnerCourseCategory, editGeneralCourseName,
                                                      editGeneralCourseCredits, spinnerGeneralCompetency,
                                                      categoryAdapter, generalCompetencyAdapter)) {
                        dialog.dismiss();
                    }
                } else {
                    // 교양필수인 경우 기존 방식 사용 (majorCourses 스피너)
                    if (spinnerMajorCourses.getSelectedItemPosition() >= 0 && majorCoursesAdapter.getCount() > 0) {
                        // 기존 스피너에서 선택된 강의 추가
                        if (addCourseFromDialog(spinnerCourseCategory, spinnerMajorCourses, categoryAdapter, majorCoursesAdapter)) {
                            dialog.dismiss();
                        }
                    } else {
                        // 교양 수동 입력으로 처리
                        if (addCourseFromManualInput(spinnerCourseCategory, editCourseName, editCourseCredits, categoryAdapter)) {
                            dialog.dismiss();
                        }
                    }
                }
            }
        });

        dialog.show();
    }

    private void updateCategorySpinner(CleanArrayAdapter<String> categoryAdapter, boolean isMajor) {
        categoryAdapter.clear();
        if (isMajor) {
            // 학번에 따라 다른 카테고리 제공
            int year = Integer.parseInt(selectedYear);
            if (year >= 2023) {
                // 2023-2025학번: 전공심화
                categoryAdapter.add("전공심화");
            } else {
                // 2020-2022학번: 학부공통
                categoryAdapter.add("학부공통");
            }

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


    private void loadCoursesForCategory(String category, CleanArrayAdapter<FirebaseDataManager.CourseInfo> courseAdapter) {
        Log.d(TAG, "loadCoursesForCategory 호출됨 - " + category);

        long currentTime = System.currentTimeMillis();

        // 강화된 중복 로딩 방지 체크
        if (isLoadingCourses && category.equals(lastLoadedCategory)) {
            Log.d(TAG, "중복 로딩 방지: 이미 " + category + " 로딩 중");
            return;
        }

        // 시간 기반 중복 방지 (같은 카테고리를 짧은 시간 내 재요청 방지)
        if (category.equals(lastLoadedCategory) && (currentTime - lastLoadTime) < MIN_LOAD_INTERVAL) {
            Log.d(TAG, "시간 기반 중복 방지: " + category + " 요청 간격이 너무 짧음");
            return;
        }

        isLoadingCourses = true;
        lastLoadedCategory = category;
        lastLoadTime = currentTime;

        courseAdapter.clear();
        courseAdapter.notifyDataSetChanged();

        // 로딩 상태 표시
        showLoadingMessage("강의 목록을 불러오는 중...");

        if ("학부공통".equals(category) || "전공심화".equals(category)) {
            // 학부공통 또는 전공심화 강의 로드
            dataManager.loadDepartmentCommonCourses(selectedDepartment, selectedTrack, selectedYear, new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                @Override
                public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                    // 이미 등록된 강의 필터링
                    List<FirebaseDataManager.CourseInfo> filteredCourses = filterRegisteredCourses(courses);
                    courseAdapter.addAll(filteredCourses);
                    courseAdapter.notifyDataSetChanged();
                    hideLoadingMessage();
                    Log.d(TAG, category + " 강의 로드 성공: " + filteredCourses.size() + "개 (필터링 후)");
                    isLoadingCourses = false; // 로딩 완료
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, category + " 강의 로드 실패", e);
                    Toast.makeText(CourseInputActivity.this, category + " 강의를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    isLoadingCourses = false; // 로딩 완료
                }
            });
        } else if ("전공필수".equals(category) || "전공선택".equals(category)) {
            // 전공 강의 로드 (카테고리별로 필터링)
            dataManager.loadMajorCourses(selectedDepartment, selectedTrack, selectedYear, category, new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                @Override
                public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                    // 이미 등록된 강의 필터링
                    List<FirebaseDataManager.CourseInfo> filteredCourses = filterRegisteredCourses(courses);
                    courseAdapter.addAll(filteredCourses);
                    courseAdapter.notifyDataSetChanged();
                    Log.d(TAG, "전공 강의 로드 성공: " + filteredCourses.size() + "개 (필터링 후)");
                    isLoadingCourses = false; // 로딩 완료
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "전공 강의 로드 실패", e);
                    Toast.makeText(CourseInputActivity.this, "전공 강의를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    isLoadingCourses = false; // 로딩 완료
                }
            });
        } else if ("교양필수".equals(category) || "교양선택".equals(category) || "일반선택".equals(category)) {
            // 교양 강의 로드 (카테고리별로 필터링)
            Log.d(TAG, "=== 교양 강의 로드 요청 ===");
            Log.d(TAG, "교양 강의 로드 요청 시작");
            Log.d(TAG, "학부: " + selectedDepartment + ", 트랙: " + selectedTrack + ", 년도: " + selectedYear + ", 카테고리: " + category);
            dataManager.loadGeneralEducationCourses(selectedDepartment, selectedTrack, selectedYear, category, new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                @Override
                public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                    Log.d(TAG, "교양 강의 로드 콜백 성공: " + courses.size() + "개 원본");
                    // 이미 등록된 강의 필터링
                    List<FirebaseDataManager.CourseInfo> filteredCourses = filterRegisteredCourses(courses);
                    Log.d(TAG, "필터링 후: " + filteredCourses.size() + "개");
                    courseAdapter.addAll(filteredCourses);
                    courseAdapter.notifyDataSetChanged();
                    Log.d(TAG, "스피너 업데이트 완료");
                    isLoadingCourses = false; // 로딩 완료
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "교양 강의 로드 콜백 실패", e);
                    Toast.makeText(CourseInputActivity.this, "교양 강의를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    isLoadingCourses = false; // 로딩 완료
                }
            });
        }
    }

    private List<FirebaseDataManager.CourseInfo> filterRegisteredCourses(List<FirebaseDataManager.CourseInfo> courses) {
        List<FirebaseDataManager.CourseInfo> filteredCourses = new ArrayList<>();

        for (FirebaseDataManager.CourseInfo course : courses) {
            boolean shouldExclude = false;

            // 이미 등록된 과목인지 확인
            for (Course registeredCourse : courseList) {
                if (registeredCourse.getName().equals(course.getName())) {
                    shouldExclude = true;
                    break;
                }

                // oneOf 그룹 체크: 등록된 과목과 같은 그룹이면 제외
                if (course.getGroupId() != null &&
                    registeredCourse.getGroupId() != null &&
                    course.getGroupId().equals(registeredCourse.getGroupId())) {
                    shouldExclude = true;
                    Log.d(TAG, "같은 oneOf 그룹 과목 제외: " + course.getName() + " (그룹: " + course.getGroupId() + ")");
                    break;
                }
            }

            if (!shouldExclude) {
                filteredCourses.add(course);
            }
        }

        return filteredCourses;
    }

    private boolean addCourseFromDialog(Spinner spinnerCourseCategory, Spinner spinnerMajorCourses,
                                       CleanArrayAdapter<String> categoryAdapter,
                                       CleanArrayAdapter<FirebaseDataManager.CourseInfo> courseAdapter) {
        // 입력값 검증
        int categoryPosition = spinnerCourseCategory.getSelectedItemPosition();
        int coursePosition = spinnerMajorCourses.getSelectedItemPosition();

        if (categoryPosition == -1) {
            Toast.makeText(this, "강의 분류를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (coursePosition == -1) {
            Toast.makeText(this, "강의를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 강의 추가 - 학점은 CourseInfo에서 자동으로 가져옴
        String category = categoryAdapter.getItem(categoryPosition);
        FirebaseDataManager.CourseInfo selectedCourse = courseAdapter.getItem(coursePosition);
        String courseName = selectedCourse.getName();
        int credits = selectedCourse.getCredits();

        // 중복 강의 확인
        for (Course existingCourse : courseList) {
            if (existingCourse.getName().equals(courseName)) {
                Toast.makeText(this, "이미 등록된 강의입니다: " + courseName, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        Course course = new Course(category, courseName, credits, selectedCourse.getGroupId());
        courseList.add(course);
        updateCourseDisplay();

        updateAnalyzeButtonState();

        Log.d(TAG, "강의 추가: " + course.toString());
        Toast.makeText(this, "강의가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        return true;
    }

    private boolean addGeneralCourseFromManualInput(Spinner spinnerCourseCategory, EditText editCourseName,
                                                  EditText editCourseCredits, Spinner spinnerCompetency,
                                                  CleanArrayAdapter<String> categoryAdapter,
                                                  CleanArrayAdapter<String> competencyAdapter) {
        // 입력값 검증
        int categoryPosition = spinnerCourseCategory.getSelectedItemPosition();
        if (categoryPosition == -1) {
            Toast.makeText(this, "강의 분류를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String courseName = editCourseName.getText().toString().trim();
        if (courseName.isEmpty()) {
            Toast.makeText(this, "강의명을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String creditsStr = editCourseCredits.getText().toString().trim();
        if (creditsStr.isEmpty()) {
            Toast.makeText(this, "학점을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int competencyPosition = spinnerCompetency.getSelectedItemPosition();
        if (competencyPosition == -1) {
            Toast.makeText(this, "역량을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int credits;
        try {
            credits = Integer.parseInt(creditsStr);
            if (credits <= 0) {
                Toast.makeText(this, "유효한 학점을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "학점은 숫자로 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 중복 강의 확인
        for (Course existingCourse : courseList) {
            if (existingCourse.getName().equals(courseName)) {
                Toast.makeText(this, "이미 등록된 강의입니다: " + courseName, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        String category = categoryAdapter.getItem(categoryPosition);
        String competency = competencyAdapter.getItem(competencyPosition);

        // 역량이 "소양"인 경우 카테고리를 "소양"으로 변경
        if ("소양".equals(competency)) {
            category = "소양";
        }

        // 역량 선택 상태 저장
        lastSelectedCompetencyPosition = competencyPosition;

        Course course = new Course(category, courseName, credits, null, competency); // 교양선택은 groupId 없고 역량 정보 포함
        courseList.add(course);
        updateCourseDisplay();

        updateAnalyzeButtonState();

        if ("소양".equals(competency)) {
            Log.d(TAG, "소양 강의 추가: " + course.toString() + ", 역량: " + competency);
            Toast.makeText(this, "소양 강의가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "교양선택 강의 추가: " + course.toString() + ", 역량: " + competency);
            Toast.makeText(this, "교양선택 강의가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private boolean addCourseFromManualInput(Spinner spinnerCourseCategory, EditText editCourseName,
                                           EditText editCourseCredits, ArrayAdapter<String> categoryAdapter) {
        // 입력값 검증
        int categoryPosition = spinnerCourseCategory.getSelectedItemPosition();
        if (categoryPosition == -1) {
            Toast.makeText(this, "강의 분류를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String courseName = editCourseName.getText().toString().trim();
        if (courseName.isEmpty()) {
            Toast.makeText(this, "강의명을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String creditsStr = editCourseCredits.getText().toString().trim();
        if (creditsStr.isEmpty()) {
            Toast.makeText(this, "학점을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

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

        // 중복 강의 확인
        for (Course existingCourse : courseList) {
            if (existingCourse.getName().equals(courseName)) {
                Toast.makeText(this, "이미 등록된 강의입니다: " + courseName, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // 강의 추가
        String category = categoryAdapter.getItem(categoryPosition);
        Course course = new Course(category, courseName, credits);
        courseList.add(course);
        updateCourseDisplay();
        updateAnalyzeButtonState();

        Log.d(TAG, "수동 입력 강의 추가: " + course.toString());
        Toast.makeText(this, "강의가 추가되었습니다.", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void refreshCourseDisplay() {
        // 기존 뷰들 제거
        // 기존 코드 제거됨 - 탭 방식으로 변경

        if (courseList.isEmpty()) {
            // 강의가 없으면 빈 메시지 표시
            if (textEmptyCourses.getParent() != null) {
                ((android.view.ViewGroup) textEmptyCourses.getParent()).removeView(textEmptyCourses);
            }
            // 탭 방식에서는 updateCourseDisplay()에서 처리
            return;
        }

        // 카테고리별로 강의들을 그룹화
        Map<String, List<Course>> coursesByCategory = new LinkedHashMap<>();
        for (Course course : courseList) {
            String category = course.getCategory();
            if (!coursesByCategory.containsKey(category)) {
                coursesByCategory.put(category, new ArrayList<>());
            }
            coursesByCategory.get(category).add(course);
        }

        // 카테고리별로 뷰 생성
        for (Map.Entry<String, List<Course>> entry : coursesByCategory.entrySet()) {
            String category = entry.getKey();
            List<Course> courses = entry.getValue();

            // 카테고리 헤더 생성
            createCategoryGroup(category, courses);
        }
    }

    private void createCategoryGroup(String category, List<Course> courses) {
        // 카테고리 총 학점 계산
        int totalCredits = courses.stream().mapToInt(Course::getCredits).sum();

        // 카테고리 헤더
        TextView categoryHeader = new TextView(this);
        categoryHeader.setText(String.format("📚 %s (%d학점)", category, totalCredits));
        categoryHeader.setTextSize(16);
        categoryHeader.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        categoryHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryHeader.setPadding(8, 16, 8, 8);

        // 마진 설정
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 12, 0, 0);
        categoryHeader.setLayoutParams(headerParams);

        // 탭 방식에서는 사용하지 않음

        // 강의 목록
        for (Course course : courses) {
            createCourseItem(course);
        }

        // 구분선
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) divider.getLayoutParams();
        dividerParams.setMargins(0, 8, 0, 8);
        // 탭 방식에서는 사용하지 않음
    }

    private void createCourseItem(Course course) {
        LinearLayout courseLayout = new LinearLayout(this);
        courseLayout.setOrientation(LinearLayout.HORIZONTAL);
        courseLayout.setPadding(16, 8, 8, 8);
        courseLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 마진 설정
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 2, 0, 2);
        courseLayout.setLayoutParams(layoutParams);

        // 강의명과 학점
        TextView courseText = new TextView(this);
        courseText.setText(String.format("• %s (%d학점)", course.getName(), course.getCredits()));
        courseText.setTextSize(14);
        courseText.setTextColor(getResources().getColor(android.R.color.black));
        courseText.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // 삭제 버튼 - 깔끔하고 클릭하기 쉽게
        TextView deleteBtn = new TextView(this);
        deleteBtn.setText("✕");
        deleteBtn.setTextSize(18);
        deleteBtn.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        deleteBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        deleteBtn.setPadding(12, 8, 12, 8);
        deleteBtn.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(40, 40);
        btnParams.setMargins(8, 0, 0, 0);
        deleteBtn.setLayoutParams(btnParams);

        // 클릭 효과를 위한 배경 설정
        deleteBtn.setBackgroundResource(android.R.drawable.btn_default);

        deleteBtn.setOnClickListener(v -> {
            courseList.remove(course);
            updateCourseDisplay();
            updateAnalyzeButtonState();
            Toast.makeText(this, course.getName() + " 삭제됨", Toast.LENGTH_SHORT).show();
        });

        courseLayout.addView(courseText);
        courseLayout.addView(deleteBtn);
        // 탭 방식에서는 사용하지 않음
    }

    private void updateAnalyzeButtonState() {
        btnAnalyzeGraduation.setEnabled(!courseList.isEmpty());
        btnAnalyzeGraduation.setText(courseList.isEmpty() ?
            "강의를 입력해주세요" :
            String.format("졸업요건 분석 (%d개 강의)", courseList.size()));
    }

    private void analyzeGraduation() {
        if (courseList.isEmpty()) {
            Toast.makeText(this, "최소 1개 이상의 강의를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 졸업요건 분석 결과 화면으로 이동
        Intent intent = new Intent(this, GraduationAnalysisResultActivity.class);
        intent.putExtra("year", selectedYear);
        intent.putExtra("department", selectedDepartment);
        intent.putExtra("track", selectedTrack);
        intent.putParcelableArrayListExtra("courses", new ArrayList<>(courseList));
        intent.putExtra("additionalRequirements", additionalRequirements);

        startActivity(intent);
        Log.d(TAG, "졸업요건 분석 시작 - 강의 수: " + courseList.size());
        if (additionalRequirements != null) {
            Log.d(TAG, "추가 졸업 요건 전달: " + additionalRequirements.toString());
        } else {
            Log.w(TAG, "추가 졸업 요건이 null입니다.");
        }
    }

    // 다이얼로그 상태 복원 메서드
    private void restoreDialogState(RadioButton radioMajor, RadioButton radioGeneral,
                                   CleanArrayAdapter<String> categoryAdapter,
                                   CleanArrayAdapter<String> generalCompetencyAdapter,
                                   CleanArrayAdapter<FirebaseDataManager.CourseInfo> majorCoursesAdapter,
                                   LinearLayout layoutMajorCourses, LinearLayout layoutGeneralManualInput,
                                   LinearLayout layoutManualInput, Spinner spinnerCourseCategory,
                                   Spinner spinnerGeneralCompetency) {

        // 라디오 버튼 상태 복원
        if (lastSelectedIsMajor) {
            radioMajor.setChecked(true);
        } else {
            radioGeneral.setChecked(true);
        }

        // 카테고리 스피너 초기화
        updateCategorySpinner(categoryAdapter, lastSelectedIsMajor);
        clearCourseSpinner(majorCoursesAdapter);
        setupCompetencySpinner(generalCompetencyAdapter);

        // 카테고리 선택 상태 복원 (postDelayed 사용하여 확실한 초기화 후 실행)
        spinnerCourseCategory.postDelayed(() -> {
            if (lastSelectedCategoryPosition >= 0 && lastSelectedCategoryPosition < categoryAdapter.getCount()) {
                spinnerCourseCategory.setSelection(lastSelectedCategoryPosition, false);
                // UI 상태 업데이트
                updateUIForCategorySelection(lastSelectedCategoryPosition, categoryAdapter, majorCoursesAdapter,
                                            layoutMajorCourses, layoutGeneralManualInput, layoutManualInput);
            }
        }, 100);

        // 역량 선택 상태 복원
        spinnerGeneralCompetency.postDelayed(() -> {
            if (lastSelectedCompetencyPosition >= 0 && lastSelectedCompetencyPosition < generalCompetencyAdapter.getCount()) {
                spinnerGeneralCompetency.setSelection(lastSelectedCompetencyPosition, false);
            }
        }, 100);

        // 초기 UI 상태 설정
        if (lastSelectedIsMajor) {
            layoutMajorCourses.setVisibility(View.VISIBLE);
            layoutGeneralManualInput.setVisibility(View.GONE);
            layoutManualInput.setVisibility(View.GONE);
        } else {
            layoutMajorCourses.setVisibility(View.VISIBLE);
            layoutGeneralManualInput.setVisibility(View.GONE);
            layoutManualInput.setVisibility(View.GONE);
        }

        // 초기 선택된 카테고리에 대한 강의 로드 (postDelayed로 스피너 초기화 완료 후 실행)
        spinnerCourseCategory.postDelayed(() -> {
            if (categoryAdapter.getCount() > 0) {
                int selectedPosition = lastSelectedCategoryPosition >= 0 ? lastSelectedCategoryPosition : 0;
                if (selectedPosition < categoryAdapter.getCount()) {
                    String selectedCategory = categoryAdapter.getItem(selectedPosition);
                    Log.d(TAG, "초기 카테고리 로딩: " + selectedCategory + " (위치: " + selectedPosition + ")");

                    // 교양선택이 아닌 경우에만 강의 로드
                    if (!"교양선택".equals(selectedCategory)) {
                        loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
                    }
                }
            }
        }, 150);
    }

    // 카테고리 선택에 따른 UI 업데이트 메서드
    private void updateUIForCategorySelection(int position, CleanArrayAdapter<String> categoryAdapter,
                                            CleanArrayAdapter<FirebaseDataManager.CourseInfo> majorCoursesAdapter,
                                            LinearLayout layoutMajorCourses, LinearLayout layoutGeneralManualInput,
                                            LinearLayout layoutManualInput) {
        if (position >= 0) {
            String selectedCategory = categoryAdapter.getItem(position);

            if (lastSelectedIsMajor) {
                // 전공인 경우
                layoutMajorCourses.setVisibility(View.VISIBLE);
                layoutGeneralManualInput.setVisibility(View.GONE);
                layoutManualInput.setVisibility(View.GONE);
                loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
            } else {
                // 교양인 경우
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
    }

    // 커스텀 스피너 어댑터 - 첫 번째 항목을 드롭다운에서 숨김
    private static class CleanArrayAdapter<T> extends ArrayAdapter<T> {
        public CleanArrayAdapter(android.content.Context context, int resource) {
            super(context, resource);
        }
    }

    // 탭 전환 메서드
    private void switchTab(String tabName, TextView selectedTabButton) {
        currentSelectedTab = tabName;
        updateTabDisplay();
        updateCourseDisplay();
    }

    // 탭 버튼 외관 업데이트
    private void updateTabDisplay() {
        // 모든 탭을 비활성화 상태로 설정
        resetTabButton(tabMajorRequired);
        resetTabButton(tabMajorElective);
        resetTabButton(tabMajorAdvanced);
        resetTabButton(tabGeneralRequired);
        resetTabButton(tabGeneralElective);

        // 현재 선택된 탭을 활성화 상태로 설정
        TextView activeTab = getTabButton(currentSelectedTab);
        if (activeTab != null) {
            setActiveTabButton(activeTab);
        }
    }

    // 탭 버튼을 비활성화 상태로 설정
    private void resetTabButton(TextView button) {
        button.setBackgroundResource(R.drawable.folder_tab_inactive);
        button.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
    }

    // 탭 버튼을 활성화 상태로 설정
    private void setActiveTabButton(TextView button) {
        button.setBackgroundResource(R.drawable.folder_tab_active);
        button.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
    }

    // 탭 이름으로 버튼 찾기
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

    // 선택된 카테고리의 강의 목록 업데이트
    private void updateCourseDisplay() {
        layoutSelectedCategoryCourses.removeAllViews();

        // 현재 선택된 카테고리의 강의들만 필터링
        List<Course> filteredCourses = new ArrayList<>();
        for (Course course : courseList) {
            if (course.getCategory().equals(currentSelectedTab)) {
                filteredCourses.add(course);
            }
        }

        if (filteredCourses.isEmpty()) {
            // 강의가 없으면 빈 메시지 표시
            layoutSelectedCategoryCourses.addView(textEmptyCourses);
        } else {
            // 강의 목록 표시
            for (Course course : filteredCourses) {
                createCourseItemView(course);
            }
        }
    }

    // 개별 강의 아이템 뷰 생성 - 깔끔한 카드 디자인
    private void createCourseItemView(Course course) {
        // 카드 컨테이너
        LinearLayout courseCard = new LinearLayout(this);
        courseCard.setOrientation(LinearLayout.HORIZONTAL);
        courseCard.setPadding(20, 16, 20, 16);
        courseCard.setBackground(getResources().getDrawable(R.drawable.spinner_background, getTheme()));
        courseCard.setElevation(4);

        // 카드 마진 및 레이아웃 설정
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        courseCard.setLayoutParams(cardParams);

        // 왼쪽 강의 정보 컨테이너
        LinearLayout courseInfoContainer = new LinearLayout(this);
        courseInfoContainer.setOrientation(LinearLayout.VERTICAL);
        courseInfoContainer.setPadding(0, 2, 0, 2);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        courseInfoContainer.setLayoutParams(infoParams);

        // 강의명
        TextView courseName = new TextView(this);
        courseName.setText(course.getName());
        courseName.setTextSize(16);
        courseName.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
        courseName.setTypeface(null, android.graphics.Typeface.BOLD);

        // 학점 정보
        TextView courseCredits = new TextView(this);
        courseCredits.setText(String.format("%d학점", course.getCredits()));
        courseCredits.setTextSize(13);
        courseCredits.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));

        courseInfoContainer.addView(courseName);
        courseInfoContainer.addView(courseCredits);

        // 오른쪽 삭제 버튼 - 더 깔끔한 원형 버튼
        Button deleteButton = new Button(this);
        deleteButton.setText("✕");
        deleteButton.setTextSize(16);
        deleteButton.setTypeface(null, android.graphics.Typeface.BOLD);
        deleteButton.setTextColor(getResources().getColor(android.R.color.white, getTheme()));

        // 원형 삭제 버튼 스타일
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(80, 80);
        deleteParams.setMargins(12, 0, 0, 0);
        deleteParams.gravity = android.view.Gravity.CENTER_VERTICAL;
        deleteButton.setLayoutParams(deleteParams);

        // 빨간 원형 배경 설정
        android.graphics.drawable.GradientDrawable deleteBackground = new android.graphics.drawable.GradientDrawable();
        deleteBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        deleteBackground.setColor(getResources().getColor(android.R.color.holo_red_light, getTheme()));
        deleteBackground.setStroke(2, getResources().getColor(android.R.color.holo_red_dark, getTheme()));
        deleteButton.setBackground(deleteBackground);

        // 삭제 버튼 클릭 리스너
        deleteButton.setOnClickListener(v -> {
            courseList.remove(course);
            updateCourseDisplay();
            updateAnalyzeButtonState();
            Toast.makeText(this, course.getName() + " 강의가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
        });

        courseCard.addView(courseInfoContainer);
        courseCard.addView(deleteButton);
        layoutSelectedCategoryCourses.addView(courseCard);
    }

    // 그룹 전환 메소드들
    private void switchToMajorGroup() {
        isMajorGroupSelected = true;

        // 버튼 상태 및 화살표 업데이트
        btnMajorGroup.setText("전공 ▼");
        btnMajorGroup.setBackgroundResource(R.drawable.button_primary);
        btnMajorGroup.setTextColor(getResources().getColor(android.R.color.white, getTheme()));

        btnGeneralGroup.setText("교양 ▷");
        btnGeneralGroup.setBackgroundResource(R.drawable.spinner_background);
        btnGeneralGroup.setTextColor(getResources().getColor(android.R.color.black, getTheme()));

        // 탭 컨테이너 표시/숨김
        majorTabsContainer.setVisibility(View.VISIBLE);
        generalTabsContainer.setVisibility(View.GONE);

        // 전공 첫 번째 탭으로 전환
        switchTab("전공필수", tabMajorRequired);
    }

    private void switchToGeneralGroup() {
        isMajorGroupSelected = false;

        // 버튼 상태 및 화살표 업데이트
        btnGeneralGroup.setText("교양 ▼");
        btnGeneralGroup.setBackgroundResource(R.drawable.button_primary);
        btnGeneralGroup.setTextColor(getResources().getColor(android.R.color.white, getTheme()));

        btnMajorGroup.setText("전공 ▷");
        btnMajorGroup.setBackgroundResource(R.drawable.spinner_background);
        btnMajorGroup.setTextColor(getResources().getColor(android.R.color.black, getTheme()));

        // 탭 컨테이너 표시/숨김
        majorTabsContainer.setVisibility(View.GONE);
        generalTabsContainer.setVisibility(View.VISIBLE);

        // 교양 첫 번째 탭으로 전환
        switchTab("교양필수", tabGeneralRequired);
    }

    // 강의 클래스
    public static class Course implements android.os.Parcelable {
        private String category;
        private String name;
        private int credits;
        private String groupId;
        private String competency; // 교양선택 역량 정보

        public Course(String category, String name, int credits) {
            this.category = category;
            this.name = name;
            this.credits = credits;
            this.groupId = null;
            this.competency = null;
        }

        public Course(String category, String name, int credits, String groupId) {
            this.category = category;
            this.name = name;
            this.credits = credits;
            this.groupId = groupId;
            this.competency = null;
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
            @Override
            public Course createFromParcel(android.os.Parcel in) {
                return new Course(in);
            }

            @Override
            public Course[] newArray(int size) {
                return new Course[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
            dest.writeString(category);
            dest.writeString(name);
            dest.writeInt(credits);
            dest.writeString(groupId);
            dest.writeString(competency);
        }

        @Override
        public String toString() {
            return String.format("[%s] %s (%d학점)", category, name, credits);
        }

        // Getters
        public String getCategory() { return category; }
        public String getName() { return name; }
        public int getCredits() { return credits; }
        public String getGroupId() { return groupId; }
        public String getCompetency() { return competency; }
    }

    // 로딩 메시지 표시
    private void showLoadingMessage(String message) {
        runOnUiThread(() -> {
            if (textEmptyCourses != null) {
                textEmptyCourses.setText(message);
                textEmptyCourses.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
            }
        });
    }

    // 성공 후 로딩 메시지 제거
    private void hideLoadingMessage() {
        runOnUiThread(() -> {
            if (textEmptyCourses != null) {
                textEmptyCourses.setText("선택된 카테고리에 표시할 강의가 없습니다.");
                textEmptyCourses.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 메모리 누수 방지를 위한 리소스 정리
        if (dataManager != null) {
            // 필요시 Firebase 리스너 정리
        }

        // 어댑터 정리
        if (layoutSelectedCategoryCourses != null) {
            layoutSelectedCategoryCourses.removeAllViews();
        }

        // 로딩 상태 초기화
        isLoadingCourses = false;
        lastLoadedCategory = null;

        Log.d(TAG, "CourseInputActivity destroyed - 리소스 정리 완료");
    }
}