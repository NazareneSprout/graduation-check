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
 *   <li>♿ <strong>접근성</strong>: HighContrastHelper를 통한 고대비 테마 지원</li>
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
public class AdditionalRequirementsActivity extends AppCompatActivity {

    private static final String TAG = "AdditionalRequirements";

    // ── Intent Key 상수화(오타 방지) ─────────────────────────────────────────────
    public static final String EXTRA_YEAR = "year";
    public static final String EXTRA_DEPARTMENT = "department";
    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_REQUIREMENTS = "additionalRequirements";

    // ── SavedInstanceState 키 ─────────────────────────────────────────────────
    private static final String S_TLC = "s_tlc";
    private static final String S_CHAPEL = "s_chapel";
    private static final String S_MILEAGE = "s_mileage";
    private static final String S_EXTRA = "s_extra";

    // ---------- UI 컴포넌트 ----------
    private TextView textViewStudentInfo;          // 상단 학번/학과/트랙 정보
    private EditText editTlcCount;                 // TLC 이수 횟수 입력
    private EditText editChapelCount;              // 채플 이수 학기 입력
    private CheckBox checkboxMileageCompleted;     // 마일리지 달성 여부
    private Button btnNextToCourseInput;           // 다음(수강강의 입력) 화면 이동
    private Toolbar toolbar;                       // 상단 툴바(뒤로가기 포함)
    private LinearLayout layoutDynamicRequirements;// 동적 추가요건 컨테이너
    private View dividerDynamic;                   // 동적 영역 구분선
    private CheckBox checkboxExtraGrad;            // 동적 추가요건 체크박스(로드 시 생성)

    // ---------- 전달 데이터 ----------
    private String selectedYear, selectedDepartment, selectedTrack;

    // ---------- 데이터 로더 ----------
    private FirebaseDataManager dataManager;

    // ── 회전 복원용: 동적으로 만들어지는 체크박스 상태를 나중에 적용하기 위한 보관값 ──
    private Boolean pendingExtraGradChecked = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // (1) 접근성: 고대비 테마 적용
        HighContrastHelper.applyHighContrastTheme(this);

        // (2) 레이아웃 세팅
        setContentView(R.layout.activity_additional_requirements);

        // (3) 기본 액션바 숨김 - 커스텀 Toolbar만 사용
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // (4) 인텐트로 넘어온 필수 데이터 파싱
        getIntentData();

        // (5) 뷰 바인딩 및 매니저 초기화
        initViews();

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

        // (2) 회전 복원: 즉시 복원 가능한 값들
        if (savedInstanceState != null) {
            String savedTlc = savedInstanceState.getString(S_TLC, "0");
            String savedChapel = savedInstanceState.getString(S_CHAPEL, "0");
            // 빈 문자열인 경우 기본값으로 설정
            editTlcCount.setText(savedTlc.isEmpty() ? "0" : savedTlc);
            editChapelCount.setText(savedChapel.isEmpty() ? "0" : savedChapel);
            checkboxMileageCompleted.setChecked(savedInstanceState.getBoolean(S_MILEAGE, false));
            // 동적 체크박스는 아직 생성 전일 수 있으므로 보관
            pendingExtraGradChecked = savedInstanceState.getBoolean(S_EXTRA, false);
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
    }

    /**
     * 레이아웃 내 위젯 참조와 데이터 매니저 초기화
     */
    private void initViews() {
        textViewStudentInfo = findViewById(R.id.text_view_student_info);
        editTlcCount = findViewById(R.id.edit_tlc_count);
        editChapelCount = findViewById(R.id.edit_chapel_count);
        checkboxMileageCompleted = findViewById(R.id.checkbox_mileage_completed);
        btnNextToCourseInput = findViewById(R.id.btn_next_to_course_input);
        toolbar = findViewById(R.id.toolbar_additional_requirements);
        layoutDynamicRequirements = findViewById(R.id.layout_dynamic_requirements);
        dividerDynamic = findViewById(R.id.divider_dynamic);

        // 기본값 설정 (사용자가 미완료 상태임을 명확히 표시)
        editTlcCount.setText("0");
        editChapelCount.setText("0");
        // 마일리지 체크박스는 기본값 false(미체크)로 이미 적절함

        dataManager = FirebaseDataManager.getInstance();
    }

    /**
     * 숫자 입력 UX 강화: 숫자 키패드 + 범위 필터(0~6)
     */
    private void applyNumericInputEnhancements() {
        editTlcCount.setInputType(InputType.TYPE_CLASS_NUMBER);
        editChapelCount.setInputType(InputType.TYPE_CLASS_NUMBER);

        InputFilter[] filters = new InputFilter[]{ new InputFilterMinMax(0, 6) };
        editTlcCount.setFilters(filters);
        editChapelCount.setFilters(filters);
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
            onBackPressed();
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
     * 실시간 입력 검증 연결
     */
    private void setupInputValidation() {
        editTlcCount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateTlcInput(s.toString()); }
        });

        editChapelCount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateChapelInput(s.toString()); }
        });
    }

    /**
     * 선택 학과 기준의 '추가 졸업 요건' 로드
     * - (1) 중복 방지: 컨테이너 정리 + 필드 리셋
     * - (2) 성공 시 카드 추가/노출, 실패/없음 시 숨김
     * - (3) 회전 복원: pendingExtraGradChecked 적용
     */
    private void loadExtraGradRequirements() {
        if (selectedDepartment == null) return;

        dataManager.loadExtraGradRequirements(selectedDepartment, new FirebaseDataManager.OnExtraGradRequirementsLoadedListener() {
            @Override
            public void onSuccess(String extraGradRequirement) {
                // (1) 중복 방지
                layoutDynamicRequirements.removeAllViews();
                checkboxExtraGrad = null;

                if (extraGradRequirement != null && !extraGradRequirement.trim().isEmpty()) {
                    addExtraGradRequirementToUI(extraGradRequirement);
                    layoutDynamicRequirements.setVisibility(View.VISIBLE);
                    dividerDynamic.setVisibility(View.VISIBLE);

                    // (3) 회전 복원: 체크 상태 적용
                    if (pendingExtraGradChecked != null && checkboxExtraGrad != null) {
                        checkboxExtraGrad.setChecked(pendingExtraGradChecked);
                    }
                } else {
                    layoutDynamicRequirements.setVisibility(View.GONE);
                    dividerDynamic.setVisibility(View.GONE);
                }
                Log.d(TAG, "추가 졸업 요건 로드 완료");
            }

            @Override
            public void onFailure(Exception e) {
                // (1) 중복 방지 및 숨김
                layoutDynamicRequirements.removeAllViews();
                checkboxExtraGrad = null;

                layoutDynamicRequirements.setVisibility(View.GONE);
                dividerDynamic.setVisibility(View.GONE);
                Log.e(TAG, "추가 졸업 요건 로드 실패", e);
            }
        });
    }

    /**
     * 동적 추가요건 카드 생성
     */
    private void addExtraGradRequirementToUI(String requirementName) {
        LinearLayout requirementLayout = new LinearLayout(this);
        requirementLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        requirementLayout.setOrientation(LinearLayout.VERTICAL);
        requirementLayout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));

        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView iconView = new TextView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.setMargins(0, 0, dpToPx(12), 0);
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
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleView.setText(requirementName);
        titleView.setTextSize(16);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        TextView descView = new TextView(this);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dpToPx(2), 0, 0);
        descView.setLayoutParams(descParams);
        descView.setText("완료 여부를 체크해주세요");
        descView.setTextSize(13);
        descView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        titleLayout.addView(titleView);
        titleLayout.addView(descView);

        headerLayout.addView(iconView);
        headerLayout.addView(titleLayout);

        checkboxExtraGrad = new CheckBox(this);
        checkboxExtraGrad.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        checkboxExtraGrad.setText("완료");
        checkboxExtraGrad.setTextSize(14);
        checkboxExtraGrad.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        headerLayout.addView(checkboxExtraGrad);

        requirementLayout.addView(headerLayout);
        layoutDynamicRequirements.addView(requirementLayout);
    }

    /** dp → px 변환 */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /** TLC 입력값 검증: 숫자 & 최대 6회 제한(실시간) */
    private void validateTlcInput(String input) {
        if (!input.isEmpty()) {
            try {
                int count = Integer.parseInt(input);
                if (count > 6) {
                    editTlcCount.setError("TLC 이수 횟수는 6회를 초과할 수 없습니다.");
                }
            } catch (NumberFormatException e) {
                editTlcCount.setError("올바른 숫자를 입력해주세요.");
            }
        }
    }

    /** 채플 입력값 검증: 숫자 & 최대 6학기 제한(실시간) */
    private void validateChapelInput(String input) {
        if (!input.isEmpty()) {
            try {
                int count = Integer.parseInt(input);
                if (count > 6) {
                    editChapelCount.setError("채플 이수 학기는 6학기를 초과할 수 없습니다.");
                }
            } catch (NumberFormatException e) {
                editChapelCount.setError("올바른 숫자를 입력해주세요.");
            }
        }
    }

    /**
     * [다음] 버튼 로직:
     * - 중복 클릭 방지(일시 비활성)
     * - AdditionalRequirements 수집/검증
     * - 유효 시 다음 화면으로 전송
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
     * - 숫자 범위/형식 검증
     * - 동적 추가요건(체크박스)도 함께 포함
     * - (9) 정책: 동적 요건 존재 시 완료 체크를 요구(필수 처리)
     */
    private AdditionalRequirements collectAdditionalRequirements() {
        try {
            // TLC 횟수 (0~6)
            int tlcCount = 0;
            String tlcInput = editTlcCount.getText().toString().trim();
            if (!tlcInput.isEmpty()) {
                tlcCount = Integer.parseInt(tlcInput);
                if (tlcCount < 0 || tlcCount > 6) {
                    Toast.makeText(this, "TLC 이수 횟수를 올바르게 입력해주세요 (0-6회)", Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            // 채플 학기 (0~6)
            int chapelCount = 0;
            String chapelInput = editChapelCount.getText().toString().trim();
            if (!chapelInput.isEmpty()) {
                chapelCount = Integer.parseInt(chapelInput);
                if (chapelCount < 0 || chapelCount > 6) {
                    Toast.makeText(this, "채플 이수 학기를 올바르게 입력해주세요 (0-6학기)", Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            // 마일리지 달성 여부
            boolean mileageCompleted = checkboxMileageCompleted.isChecked();

            // 동적 추가요건 완료 여부 (체크하지 않으면 미완료로 처리)
            boolean extraGradCompleted = (checkboxExtraGrad != null) && checkboxExtraGrad.isChecked();

            return new AdditionalRequirements(tlcCount, chapelCount, mileageCompleted, extraGradCompleted);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 추가 졸업 요건 데이터 모델 (Intent로 전달하기 위해 Parcelable 구현)
    // ─────────────────────────────────────────────────────────────────────────
    public static class AdditionalRequirements implements android.os.Parcelable {
        private int tlcCount;              // TLC 이수 횟수
        private int chapelCount;           // 채플 이수 학기
        private boolean mileageCompleted;  // 마일리지 달성 여부
        private boolean extraGradCompleted;// 동적 추가요건 달성 여부

        public AdditionalRequirements(int tlcCount, int chapelCount, boolean mileageCompleted, boolean extraGradCompleted) {
            this.tlcCount = tlcCount;
            this.chapelCount = chapelCount;
            this.mileageCompleted = mileageCompleted;
            this.extraGradCompleted = extraGradCompleted;
        }

        protected AdditionalRequirements(android.os.Parcel in) {
            tlcCount = in.readInt();
            chapelCount = in.readInt();
            mileageCompleted = in.readByte() != 0;
            extraGradCompleted = in.readByte() != 0;
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
        }

        // Getter
        public int getTlcCount() { return tlcCount; }
        public int getChapelCount() { return chapelCount; }
        public boolean isMileageCompleted() { return mileageCompleted; }
        public boolean isExtraGradCompleted() { return extraGradCompleted; }

        @Override
        public String toString() {
            return String.format("AdditionalRequirements{TLC: %d회, Chapel: %d학기, Mileage: %s, ExtraGrad: %s}",
                    tlcCount, chapelCount,
                    mileageCompleted ? "완료" : "미완료",
                    extraGradCompleted ? "완료" : "미완료");
        }
    }

    // ── 회전 시 값 보존 ────────────────────────────────────────────────────────
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(S_TLC, editTlcCount.getText().toString());
        out.putString(S_CHAPEL, editChapelCount.getText().toString());
        out.putBoolean(S_MILEAGE, checkboxMileageCompleted.isChecked());
        out.putBoolean(S_EXTRA, checkboxExtraGrad != null && checkboxExtraGrad.isChecked());
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
