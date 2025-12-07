package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 추가 졸업 요건 입력 화면
 *
 * <p>이 Activity는 졸업 요건 분석 과정의 두 번째 단계로, 기본 졸업 요건 외에
 * 필요한 추가 요건들을 입력받는 화면입니다. 사용자는 TLC, 채플, 마일리지 등의
 * 기본 추가 요건과 학과별 특별 요구사항을 입력할 수 있습니다.</p>
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>📋 <strong>기본 추가 요건 입력</strong>: TLC 이수 횟수(0-6회), 채플 이수 학기(0-6학기)</li>
 *   <li>🏆 <strong>마일리지 달성 여부</strong>: 체크박스로 달성 상태 입력</li>
 *   <li>🎯 <strong>동적 추가 요건</strong>: 학과별 특별 요구사항 (졸업작품 등) 동적 로드</li>
 *   <li>✅ <strong>실시간 입력 검증</strong>: 숫자 범위 체크 및 형식 검증</li>
 *   <li>💾 <strong>상태 보존</strong>: 화면 회전 시 입력 데이터 복원</li>
 * </ul>
 *
 * <h3>데이터 플로우:</h3>
 * <ul>
 *   <li>📥 <strong>입력</strong>: GraduationAnalysisActivity에서 학번/학과/트랙 정보 수신</li>
 *   <li>📤 <strong>출력</strong>: CourseInputActivity로 추가 요건 데이터 전송</li>
 * </ul>
 *
 * <h3>UI 특징:</h3>
 * <ul>
 *   <li>🛡️ <strong>중복 클릭 방지</strong>: 2초 가드를 통한 다중 제출 방지</li>
 *   <li>📱 <strong>키패드 최적화</strong>: 숫자 입력 시 숫자 키패드 자동 표시</li>
 *   <li>🔄 <strong>동적 UI</strong>: 학과별 추가 요건 존재 시에만 관련 영역 표시</li>
 * </ul>
 *
 * <h3>성능 최적화:</h3>
 * <ul>
 *   <li>⚡ <strong>중복 요청 방지</strong>: 추가 요건 로드 시 기존 뷰 정리</li>
 *   <li>💾 <strong>상태 관리</strong>: 회전 복원을 위한 pending 상태 관리</li>
 *   <li>🚀 <strong>즉시 피드백</strong>: 실시간 입력 검증으로 사용자 경험 향상</li>
 * </ul>
 *
 * @see GraduationAnalysisActivity 이전 단계 (학번/학과/트랙 선택)
 * @see CourseInputActivity 다음 단계 (수강 강의 입력)
 * @see AdditionalRequirements 추가 요건 데이터 모델
 * @see FirebaseDataManager#loadExtraGradRequirements 동적 요건 로드
 */
public class AdditionalRequirementsActivity extends BaseActivity {

    private static final String TAG = "AdditionalRequirements";

    // ── Intent Key 상수화(오타 방지) ─────────────────────────────────────────────
    public static final String EXTRA_YEAR = "year";
    public static final String EXTRA_DEPARTMENT = "department";
    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_REQUIREMENTS = "additionalRequirements";

    // ── SavedInstanceState 키 ─────────────────────────────────────────────────
    private static final String S_OTHER_REQS = "s_other_reqs";

    // ---------- UI 컴포넌트 ----------
    private TextView textViewStudentInfo;          // 상단 학번/학과/트랙 정보
    private Button btnNextToCourseInput;           // 다음(수강강의 입력) 화면 이동
    private Toolbar toolbar;                       // 상단 툴바(뒤로가기 포함)
    private LinearLayout layoutDynamicRequirements;// 동적 추가요건 컨테이너

    // ---------- 전달 데이터 ----------
    private String selectedYear, selectedDepartment, selectedTrack;

    // ---------- 데이터 로더 ----------
    private FirebaseDataManager dataManager;

    // ── 동적 요건 입력 필드 저장 ──
    private java.util.Map<String, View> otherRequirementInputs = new java.util.HashMap<>();

    // ── 회전 복원용: 동적 요건 데이터 ──
    private java.util.Map<String, Object> pendingOtherRequirements = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // (1) 레이아웃 세팅
        setContentView(R.layout.activity_additional_requirements);

        // (3) 기본 액션바 숨김 - 커스텀 Toolbar만 사용
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // (4) 인텐트로 넘어온 필수 데이터 파싱
        getIntentData();

        // (5) 뷰 바인딩 및 매니저 초기화
        initViews();

        // (5-1) 저장된 데이터가 있으면 UI에 적용
        loadSavedRequirementsIfNeeded();

        // (4) 입력 UX 개선: 숫자 패드/범위 필터 적용
        applyNumericInputEnhancements();

        // (6) 시스템 UI 인셋 처리
        setupSystemUI();

        // (7) Toolbar 설정
        setupToolbar();

        // (8) 리스너
        setupListeners();

        // (9) 학생 정보 표시
        displayStudentInfo();

        // (10) 실시간 검증
        setupInputValidation();

        // (11) 동적 추가 요건 로드(+ 중복 방지 및 회전 복원 지원)
        loadExtraGradRequirements();

        // (2) 회전 복원: 동적 요건 데이터 보관
        if (savedInstanceState != null) {
            pendingOtherRequirements = (java.util.Map<String, Object>) savedInstanceState.getSerializable(S_OTHER_REQS);
        }
    }

    /**
     * 시스템 바와 겹치지 않도록 루트에 패딩 부여 (WindowInsetsCompat)
     */
    private void setupSystemUI() {
        View root = findViewById(R.id.main);
        if (root == null) return; // NPE 방지

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * 이전 화면에서 전달된 학번/학과/트랙값 수신
     */
    private void getIntentData() {
        Intent intent = getIntent();
        selectedYear = intent.getStringExtra(EXTRA_YEAR);
        selectedDepartment = intent.getStringExtra(EXTRA_DEPARTMENT);
        selectedTrack = intent.getStringExtra(EXTRA_TRACK);

        if (selectedYear == null || selectedDepartment == null || selectedTrack == null) {
            Toast.makeText(this, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 저장된 추가 요건 데이터가 있는지 확인
        boolean hasLoadedData = intent.getBooleanExtra("hasLoadedData", false);
        if (hasLoadedData) {
            Log.d(TAG, "저장된 추가 요건 데이터 불러오기 모드");
            // initViews 이후에 적용하기 위해 Intent에서 가져옴 (나중에 loadSavedRequirements에서 사용)
        }
    }

    /**
     * 레이아웃 내 위젯 참조와 데이터 매니저 초기화
     */
    private void initViews() {
        textViewStudentInfo = findViewById(R.id.text_view_student_info);
        btnNextToCourseInput = findViewById(R.id.btn_next_to_course_input);
        toolbar = findViewById(R.id.toolbar_additional_requirements);
        layoutDynamicRequirements = findViewById(R.id.layout_dynamic_requirements);

        dataManager = FirebaseDataManager.getInstance();
    }

    /**
     * 숫자 입력 UX 강화: (동적 요건에서 처리하므로 메서드는 비움)
     */
    private void applyNumericInputEnhancements() {
        // 동적 요건에서 각 입력 필드에 대해 필터 적용
    }

    /**
     * 저장된 추가 요건 데이터를 UI에 적용
     */
    private void loadSavedRequirementsIfNeeded() {
        Intent intent = getIntent();
        boolean hasLoadedData = intent.getBooleanExtra("hasLoadedData", false);
        if (!hasLoadedData) {
            return;
        }

        Log.d(TAG, "저장된 추가 요건 UI에 적용");

        // 저장된 AdditionalRequirements 객체 가져오기
        AdditionalRequirements savedReqs = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            savedReqs = intent.getParcelableExtra(EXTRA_REQUIREMENTS, AdditionalRequirements.class);
        } else {
            savedReqs = intent.getParcelableExtra(EXTRA_REQUIREMENTS);
        }

        if (savedReqs != null && savedReqs.getOtherRequirements() != null) {
            // 동적 요건 데이터 보관 (UI 생성 후 적용)
            pendingOtherRequirements = savedReqs.getOtherRequirements();

            Toast.makeText(this, "저장된 추가 요건을 불러왔습니다", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "저장된 요건 적용 완료: " + savedReqs.toString());
        }
    }

    /**
     * 커스텀 Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("추가 졸업 요건");
        }
    }

    /**
     * Toolbar 좌측 화살표 동작(뒤로가기)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 버튼 리스너
     */
    private void setupListeners() {
        btnNextToCourseInput.setOnClickListener(v -> proceedToCourseInput());
    }

    /**
     * 상단에 "2020학번 IT융합학부 AI트랙" 형식으로 학생 정보 표시
     */
    private void displayStudentInfo() {
        String studentInfo = String.format("%s학번 %s %s", selectedYear, selectedDepartment, selectedTrack);
        textViewStudentInfo.setText(studentInfo);
    }

    /**
     * 실시간 입력 검증 연결 (동적 요건에서 처리)
     */
    private void setupInputValidation() {
        // 동적 요건 입력 필드에서 검증 처리
    }

    /**
     * 선택 학과 기준의 '추가 졸업 요건' 로드
     * - (1) 중복 방지: 컨테이너 정리 + 필드 리셋
     * - (2) 성공 시 카드 추가/노출, 실패/없음 시 숨김
     * - (3) 회전 복원: pendingExtraGradChecked 적용
     */
    private void loadExtraGradRequirements() {
        if (selectedYear == null || selectedDepartment == null || selectedTrack == null) return;

        // 새로운 other_requirements_groups 컬렉션에서 로드
        dataManager.loadOtherRequirements(selectedYear, selectedDepartment, selectedTrack,
                new FirebaseDataManager.OnOtherRequirementsLoadedListener() {
            @Override
            public void onSuccess(sprout.app.sakmvp1.models.OtherRequirementGroup group) {
                // (1) 중복 방지
                layoutDynamicRequirements.removeAllViews();
                otherRequirementInputs.clear();

                if (group != null && group.getRequirements() != null && !group.getRequirements().isEmpty()) {
                    // 각 요건을 UI에 추가
                    for (sprout.app.sakmvp1.models.OtherRequirementGroup.RequirementItem item : group.getRequirements()) {
                        addOtherRequirementToUI(item);
                    }
                    layoutDynamicRequirements.setVisibility(View.VISIBLE);

                    // 회전 복원 또는 저장된 데이터 적용
                    applyPendingRequirements();

                    Log.d(TAG, "기타 졸업요건 로드 완료: " + group.getRequirements().size() + "개");
                } else {
                    layoutDynamicRequirements.setVisibility(View.GONE);
                    Log.d(TAG, "기타 졸업요건 없음");
                }
            }

            @Override
            public void onFailure(Exception e) {
                // (1) 중복 방지 및 숨김
                layoutDynamicRequirements.removeAllViews();
                otherRequirementInputs.clear();

                layoutDynamicRequirements.setVisibility(View.GONE);
                Log.e(TAG, "기타 졸업요건 로드 실패", e);
            }
        });
    }

    /**
     * 기타 졸업요건 항목을 UI에 추가 (기존 TLC/채플 UI와 동일한 스타일)
     */
    private void addOtherRequirementToUI(sprout.app.sakmvp1.models.OtherRequirementGroup.RequirementItem item) {
        // 메인 컨테이너
        LinearLayout requirementLayout = new LinearLayout(this);
        requirementLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        requirementLayout.setOrientation(LinearLayout.VERTICAL);
        requirementLayout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        LinearLayout.LayoutParams containerParams = (LinearLayout.LayoutParams) requirementLayout.getLayoutParams();
        containerParams.bottomMargin = dpToPx(16);
        requirementLayout.setLayoutParams(containerParams);

        // 헤더 레이아웃 (아이콘 + 제목 + 설명)
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 형식에 따라 입력 UI 추가
        if ("횟수".equals(item.getFormat())) {
            // 헤더에 아이콘과 제목만 (입력은 아래에)
            LinearLayout.LayoutParams headerParams = (LinearLayout.LayoutParams) headerLayout.getLayoutParams();
            headerParams.bottomMargin = dpToPx(16);
            headerLayout.setLayoutParams(headerParams);

            TextView iconView = new TextView(this);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            iconParams.setMarginEnd(dpToPx(12));
            iconView.setLayoutParams(iconParams);
            iconView.setText("🎓");
            iconView.setTextSize(24);

            LinearLayout titleLayout = new LinearLayout(this);
            LinearLayout.LayoutParams titleLayoutParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            titleLayout.setLayoutParams(titleLayoutParams);
            titleLayout.setOrientation(LinearLayout.VERTICAL);

            TextView titleView = new TextView(this);
            titleView.setText(item.getName());
            titleView.setTextSize(16);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            int colorOnSurface = getResources().getColor(android.R.color.black);
            titleView.setTextColor(colorOnSurface);

            TextView descView = new TextView(this);
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            descParams.topMargin = dpToPx(2);
            descView.setLayoutParams(descParams);
            descView.setText("이수 필요 횟수: " + item.getDescription());
            descView.setTextSize(13);
            descView.setTextColor(getResources().getColor(android.R.color.darker_gray));

            titleLayout.addView(titleView);
            titleLayout.addView(descView);

            headerLayout.addView(iconView);
            headerLayout.addView(titleLayout);

            requirementLayout.addView(headerLayout);

            // 입력 레이아웃 (TLC/채플과 동일한 형식)
            LinearLayout inputLayout = new LinearLayout(this);
            inputLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            inputLayout.setOrientation(LinearLayout.HORIZONTAL);
            inputLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView labelView = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelParams.setMarginEnd(dpToPx(12));
            labelView.setLayoutParams(labelParams);
            labelView.setText("이수 횟수:");
            labelView.setTextSize(14);
            labelView.setTextColor(colorOnSurface);

            EditText countInput = new EditText(this);
            countInput.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(60), dpToPx(48)));
            countInput.setBackground(ContextCompat.getDrawable(this, R.drawable.spinner_background));
            countInput.setHint("0");
            countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            countInput.setTextSize(16);
            countInput.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams countParams = (LinearLayout.LayoutParams) countInput.getLayoutParams();
            countParams.setMarginEnd(dpToPx(8));
            countInput.setLayoutParams(countParams);

            // 최대값 필터 적용
            int maxCount = Math.max(item.getCount() * 2, 99);
            countInput.setFilters(new InputFilter[]{new InputFilterMinMax(0, maxCount)});

            TextView unitView = new TextView(this);
            unitView.setText("/ " + item.getDescription());
            unitView.setTextSize(14);
            unitView.setTextColor(getResources().getColor(android.R.color.darker_gray));

            inputLayout.addView(labelView);
            inputLayout.addView(countInput);
            inputLayout.addView(unitView);

            requirementLayout.addView(inputLayout);
            otherRequirementInputs.put(item.getName(), countInput);

        } else if ("통과".equals(item.getFormat())) {
            // 통과 형식 (마일리지와 동일한 형식)
            TextView iconView = new TextView(this);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            iconParams.setMarginEnd(dpToPx(12));
            iconView.setLayoutParams(iconParams);
            iconView.setText("🎓");
            iconView.setTextSize(24);

            LinearLayout titleLayout = new LinearLayout(this);
            LinearLayout.LayoutParams titleLayoutParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            titleLayout.setLayoutParams(titleLayoutParams);
            titleLayout.setOrientation(LinearLayout.VERTICAL);

            TextView titleView = new TextView(this);
            titleView.setText(item.getName());
            titleView.setTextSize(16);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            int colorOnSurface = getResources().getColor(android.R.color.black);
            titleView.setTextColor(colorOnSurface);

            TextView descView = new TextView(this);
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            descParams.topMargin = dpToPx(2);
            descView.setLayoutParams(descParams);
            descView.setText("필요: " + item.getDescription());
            descView.setTextSize(13);
            descView.setTextColor(getResources().getColor(android.R.color.darker_gray));

            titleLayout.addView(titleView);
            titleLayout.addView(descView);

            CheckBox passCheckbox = new CheckBox(this);
            passCheckbox.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            passCheckbox.setText("완료");
            passCheckbox.setTextSize(14);
            passCheckbox.setTextColor(colorOnSurface);

            headerLayout.addView(iconView);
            headerLayout.addView(titleLayout);
            headerLayout.addView(passCheckbox);

            requirementLayout.addView(headerLayout);
            otherRequirementInputs.put(item.getName(), passCheckbox);
        }

        layoutDynamicRequirements.addView(requirementLayout);

        // 구분선 추가 (마지막 항목이 아니면)
        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        dividerParams.bottomMargin = dpToPx(16);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        layoutDynamicRequirements.addView(divider);
    }

    /** dp → px 변환 */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 회전 복원 또는 저장된 데이터를 동적 요건 UI에 적용
     */
    private void applyPendingRequirements() {
        if (pendingOtherRequirements == null || pendingOtherRequirements.isEmpty()) {
            return;
        }

        for (java.util.Map.Entry<String, Object> entry : pendingOtherRequirements.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            View inputView = otherRequirementInputs.get(name);

            if (inputView != null) {
                if (inputView instanceof EditText && value instanceof Number) {
                    ((EditText) inputView).setText(String.valueOf(((Number) value).intValue()));
                } else if (inputView instanceof CheckBox && value instanceof Boolean) {
                    ((CheckBox) inputView).setChecked((Boolean) value);
                }
            }
        }

        // 적용 완료 후 초기화
        pendingOtherRequirements = null;
    }

    /**
     * [다음] 버튼 로직:
     * - 중복 클릭 방지(일시 비활성)
     * - AdditionalRequirements 수집/검증
     * - 유효 시 다음 화면으로 전송
     * - 저장된 과목 데이터가 있으면 함께 전달
     */
    private void proceedToCourseInput() {
        btnNextToCourseInput.setEnabled(false); // 중복 클릭 방지

        // 2초 가드: 반드시 재활성화 보장
        btnNextToCourseInput.postDelayed(() -> btnNextToCourseInput.setEnabled(true), 2000);

        AdditionalRequirements requirements = collectAdditionalRequirements();
        if (requirements != null) {
            Intent intent = new Intent(this, CourseInputActivity.class);
            intent.putExtra(EXTRA_YEAR, selectedYear);
            intent.putExtra(EXTRA_DEPARTMENT, selectedDepartment);
            intent.putExtra(EXTRA_TRACK, selectedTrack);
            intent.putExtra(EXTRA_REQUIREMENTS, requirements); // Parcelable

            // 저장된 과목 데이터가 있으면 CourseInputActivity로 전달
            Intent currentIntent = getIntent();
            boolean hasLoadedData = currentIntent.getBooleanExtra("hasLoadedData", false);
            if (hasLoadedData) {
                // 저장된 과목 데이터 전달
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    java.util.ArrayList<CourseInputActivity.Course> savedCourses =
                        currentIntent.getParcelableArrayListExtra("savedCourses", CourseInputActivity.Course.class);
                    if (savedCourses != null) {
                        intent.putParcelableArrayListExtra("savedCourses", savedCourses);
                        intent.putExtra("isLoadingSavedData", true);
                        Log.d(TAG, "저장된 과목 " + savedCourses.size() + "개를 CourseInputActivity로 전달");
                    }
                } else {
                    java.util.ArrayList<CourseInputActivity.Course> savedCourses =
                        currentIntent.getParcelableArrayListExtra("savedCourses");
                    if (savedCourses != null) {
                        intent.putParcelableArrayListExtra("savedCourses", savedCourses);
                        intent.putExtra("isLoadingSavedData", true);
                        Log.d(TAG, "저장된 과목 " + savedCourses.size() + "개를 CourseInputActivity로 전달");
                    }
                }
            }

            startActivity(intent);
            Toast.makeText(this, "추가 요건이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // 검증 실패 시 가드 제거 후 즉시 재활성화
            btnNextToCourseInput.removeCallbacks(null);
            btnNextToCourseInput.setEnabled(true);
        }
    }

    /**
     * 현재 화면의 입력값을 읽어 AdditionalRequirements(Parcelable)로 구성
     * - 동적 요건 데이터 수집 및 검증
     */
    private AdditionalRequirements collectAdditionalRequirements() {
        try {
            // 기타 졸업요건 데이터 수집
            java.util.Map<String, Object> otherReqs = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, View> entry : otherRequirementInputs.entrySet()) {
                String name = entry.getKey();
                View inputView = entry.getValue();

                if (inputView instanceof EditText) {
                    // 횟수 형식
                    EditText editText = (EditText) inputView;
                    String countStr = editText.getText().toString().trim();
                    if (!countStr.isEmpty()) {
                        try {
                            int count = Integer.parseInt(countStr);
                            otherReqs.put(name, count);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, name + "의 횟수를 올바르게 입력해주세요", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                    } else {
                        otherReqs.put(name, 0); // 입력하지 않으면 0으로 처리
                    }
                } else if (inputView instanceof CheckBox) {
                    // 통과 형식
                    CheckBox checkBox = (CheckBox) inputView;
                    otherReqs.put(name, checkBox.isChecked());
                }
            }

            AdditionalRequirements requirements = new AdditionalRequirements();
            requirements.setOtherRequirements(otherReqs);
            return requirements;

        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 추가 졸업 요건 데이터 모델 (Intent로 전달하기 위해 Parcelable 구현)
    // ─────────────────────────────────────────────────────────────────────────
    public static class AdditionalRequirements implements android.os.Parcelable {
        @Deprecated private int tlcCount;
        @Deprecated private int chapelCount;
        @Deprecated private boolean mileageCompleted;
        @Deprecated private boolean extraGradCompleted;
        private java.util.Map<String, Object> otherRequirements; // 기타 졸업요건 (name -> count/isCompleted)

        public AdditionalRequirements() {
            this.otherRequirements = new java.util.HashMap<>();
        }

        @Deprecated
        public AdditionalRequirements(int tlcCount, int chapelCount, boolean mileageCompleted, boolean extraGradCompleted) {
            this.tlcCount = tlcCount;
            this.chapelCount = chapelCount;
            this.mileageCompleted = mileageCompleted;
            this.extraGradCompleted = extraGradCompleted;
            this.otherRequirements = new java.util.HashMap<>();

            // 기존 데이터를 otherRequirements로 변환
            if (tlcCount > 0) otherRequirements.put("TLC", tlcCount);
            if (chapelCount > 0) otherRequirements.put("채플", chapelCount);
            if (mileageCompleted) otherRequirements.put("1004 마일리지", true);
        }

        protected AdditionalRequirements(android.os.Parcel in) {
            tlcCount = in.readInt();
            chapelCount = in.readInt();
            mileageCompleted = in.readByte() != 0;
            extraGradCompleted = in.readByte() != 0;
            otherRequirements = in.readHashMap(Object.class.getClassLoader());
        }

        public static final Creator<AdditionalRequirements> CREATOR = new Creator<AdditionalRequirements>() {
            @Override
            public AdditionalRequirements createFromParcel(android.os.Parcel in) {
                return new AdditionalRequirements(in);
            }
            @Override
            public AdditionalRequirements[] newArray(int size) {
                return new AdditionalRequirements[size];
            }
        };

        @Override public int describeContents() { return 0; }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
            dest.writeInt(tlcCount);
            dest.writeInt(chapelCount);
            dest.writeByte((byte) (mileageCompleted ? 1 : 0));
            dest.writeByte((byte) (extraGradCompleted ? 1 : 0));
            dest.writeMap(otherRequirements);
        }

        // Getter / Setter
        @Deprecated public int getTlcCount() { return tlcCount; }
        @Deprecated public int getChapelCount() { return chapelCount; }
        @Deprecated public boolean isMileageCompleted() { return mileageCompleted; }
        @Deprecated public boolean isExtraGradCompleted() { return extraGradCompleted; }
        public java.util.Map<String, Object> getOtherRequirements() { return otherRequirements; }
        public void setOtherRequirements(java.util.Map<String, Object> otherRequirements) {
            this.otherRequirements = otherRequirements;
        }

        @Override
        public String toString() {
            return "AdditionalRequirements{otherRequirements: " + otherRequirements + "}";
        }
    }

    // ── 회전 시 값 보존 ────────────────────────────────────────────────────────
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        // 현재 동적 요건 데이터 수집
        java.util.Map<String, Object> currentData = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, View> entry : otherRequirementInputs.entrySet()) {
            String name = entry.getKey();
            View inputView = entry.getValue();

            if (inputView instanceof EditText) {
                String text = ((EditText) inputView).getText().toString().trim();
                if (!text.isEmpty()) {
                    try {
                        currentData.put(name, Integer.parseInt(text));
                    } catch (NumberFormatException e) {
                        currentData.put(name, 0);
                    }
                } else {
                    currentData.put(name, 0);
                }
            } else if (inputView instanceof CheckBox) {
                currentData.put(name, ((CheckBox) inputView).isChecked());
            }
        }

        out.putSerializable(S_OTHER_REQS, (java.io.Serializable) currentData);
    }

    // ── 숫자 범위 필터(0~6) ───────────────────────────────────────────────────
    public static class InputFilterMinMax implements InputFilter {
        private final int min, max;
        public InputFilterMinMax(int min, int max) { this.min = min; this.max = max; }

        @Override
        public CharSequence filter(CharSequence src, int start, int end,
                                   Spanned dst, int dstart, int dend) {
            try {
                String out = dst.subSequence(0, dstart) + src.toString() + dst.subSequence(dend, dst.length());
                if (out.isEmpty()) return null;
                int val = Integer.parseInt(out);
                return (val >= min && val <= max) ? null : "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
    }
}
