package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
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

public class AdditionalRequirementsActivity extends AppCompatActivity {

    private static final String TAG = "AdditionalRequirements";

    // UI 컴포넌트
    private TextView textViewStudentInfo;
    private EditText editTlcCount;
    private EditText editChapelCount;
    private CheckBox checkboxMileageCompleted;
    private Button btnNextToCourseInput;
    private Toolbar toolbar;
    private LinearLayout layoutDynamicRequirements;
    private View dividerDynamic;
    private CheckBox checkboxExtraGrad;

    // 데이터
    private String selectedYear, selectedDepartment, selectedTrack;
    private FirebaseDataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional_requirements);

        // 시스템 UI와의 중복 방지
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getIntentData();
        initViews();
        setupSystemUI();
        setupToolbar();
        setupListeners();
        displayStudentInfo();
        setupInputValidation();
        loadExtraGradRequirements();
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

        if (selectedYear == null || selectedDepartment == null || selectedTrack == null) {
            Toast.makeText(this, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        textViewStudentInfo = findViewById(R.id.text_view_student_info);
        editTlcCount = findViewById(R.id.edit_tlc_count);
        editChapelCount = findViewById(R.id.edit_chapel_count);
        checkboxMileageCompleted = findViewById(R.id.checkbox_mileage_completed);
        btnNextToCourseInput = findViewById(R.id.btn_next_to_course_input);
        toolbar = findViewById(R.id.toolbar_additional_requirements);
        layoutDynamicRequirements = findViewById(R.id.layout_dynamic_requirements);
        dividerDynamic = findViewById(R.id.divider_dynamic);

        // Firebase 데이터 매니저 초기화
        dataManager = FirebaseDataManager.getInstance();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("추가 졸업 요건");
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

    private void setupListeners() {
        btnNextToCourseInput.setOnClickListener(v -> proceedToCourseInput());
    }

    private void displayStudentInfo() {
        String studentInfo = String.format("%s학번 %s %s", selectedYear, selectedDepartment, selectedTrack);
        textViewStudentInfo.setText(studentInfo);
    }

    private void setupInputValidation() {
        // TLC 입력 검증
        editTlcCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateTlcInput(s.toString());
            }
        });

        // 채플 입력 검증
        editChapelCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateChapelInput(s.toString());
            }
        });

        // 마일리지 체크박스는 별도 로직 없이 단순하게 사용
    }

    private void loadExtraGradRequirements() {
        if (selectedDepartment == null) {
            return;
        }

        dataManager.loadExtraGradRequirements(selectedDepartment, new FirebaseDataManager.OnExtraGradRequirementsLoadedListener() {
            @Override
            public void onSuccess(String extraGradRequirement) {
                if (extraGradRequirement != null && !extraGradRequirement.trim().isEmpty()) {
                    // 동적 요건이 있으면 UI에 추가
                    addExtraGradRequirementToUI(extraGradRequirement);
                    layoutDynamicRequirements.setVisibility(View.VISIBLE);
                    dividerDynamic.setVisibility(View.VISIBLE);
                    Log.d(TAG, "추가 졸업 요건 UI 추가 완료: " + extraGradRequirement);
                } else {
                    // 동적 요건이 없으면 UI 숨김
                    layoutDynamicRequirements.setVisibility(View.GONE);
                    dividerDynamic.setVisibility(View.GONE);
                    Log.d(TAG, "추가 졸업 요건 없음");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "추가 졸업 요건 로드 실패", e);
                // 실패 시 UI 숨김
                layoutDynamicRequirements.setVisibility(View.GONE);
                dividerDynamic.setVisibility(View.GONE);
            }
        });
    }

    private void addExtraGradRequirementToUI(String requirementName) {
        // 동적 요건 레이아웃 생성 (TLC, 채플과 동일한 스타일)
        LinearLayout requirementLayout = new LinearLayout(this);
        requirementLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        requirementLayout.setOrientation(LinearLayout.VERTICAL);
        requirementLayout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(16));

        // 헤더 레이아웃
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 아이콘
        TextView iconView = new TextView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.setMargins(0, 0, dpToPx(12), 0);
        iconView.setLayoutParams(iconParams);
        iconView.setText("🎓");
        iconView.setTextSize(24);

        // 제목 및 설명 레이아웃
        LinearLayout titleLayout = new LinearLayout(this);
        LinearLayout.LayoutParams titleLayoutParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        titleLayout.setLayoutParams(titleLayoutParams);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        // 제목
        TextView titleView = new TextView(this);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleView.setText(requirementName);
        titleView.setTextSize(16);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(getResources().getColor(android.R.color.black, null));

        // 설명
        TextView descView = new TextView(this);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dpToPx(2), 0, 0);
        descView.setLayoutParams(descParams);
        descView.setText("완료 여부를 체크해주세요");
        descView.setTextSize(13);
        descView.setTextColor(getResources().getColor(android.R.color.darker_gray, null));

        titleLayout.addView(titleView);
        titleLayout.addView(descView);

        headerLayout.addView(iconView);
        headerLayout.addView(titleLayout);

        // 체크박스
        checkboxExtraGrad = new CheckBox(this);
        checkboxExtraGrad.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        checkboxExtraGrad.setText("완료");
        checkboxExtraGrad.setTextSize(14);
        checkboxExtraGrad.setTextColor(getResources().getColor(android.R.color.black, null));

        headerLayout.addView(checkboxExtraGrad);

        requirementLayout.addView(headerLayout);
        layoutDynamicRequirements.addView(requirementLayout);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

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


    private void proceedToCourseInput() {
        // 입력값 수집 및 검증
        AdditionalRequirements requirements = collectAdditionalRequirements();

        if (requirements != null) {
            // 수강 강의 입력 화면으로 이동
            Intent intent = new Intent(this, CourseInputActivity.class);
            intent.putExtra("year", selectedYear);
            intent.putExtra("department", selectedDepartment);
            intent.putExtra("track", selectedTrack);
            intent.putExtra("additionalRequirements", requirements);

            startActivity(intent);
            Toast.makeText(this, "추가 요건이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }


    private AdditionalRequirements collectAdditionalRequirements() {
        try {
            // TLC 횟수
            int tlcCount = 0;
            String tlcInput = editTlcCount.getText().toString().trim();
            if (!tlcInput.isEmpty()) {
                tlcCount = Integer.parseInt(tlcInput);
                if (tlcCount < 0 || tlcCount > 6) {
                    Toast.makeText(this, "TLC 이수 횟수를 올바르게 입력해주세요 (0-6회)", Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            // 채플 학기
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

            // 추가 졸업 요건 달성 여부
            boolean extraGradCompleted = (checkboxExtraGrad != null) ? checkboxExtraGrad.isChecked() : false;

            return new AdditionalRequirements(tlcCount, chapelCount, mileageCompleted, extraGradCompleted);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // 추가 졸업 요건 데이터 클래스
    public static class AdditionalRequirements implements android.os.Parcelable {
        private int tlcCount;
        private int chapelCount;
        private boolean mileageCompleted;
        private boolean extraGradCompleted;

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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) {
            dest.writeInt(tlcCount);
            dest.writeInt(chapelCount);
            dest.writeByte((byte) (mileageCompleted ? 1 : 0));
            dest.writeByte((byte) (extraGradCompleted ? 1 : 0));
        }

        // Getters
        public int getTlcCount() { return tlcCount; }
        public int getChapelCount() { return chapelCount; }
        public boolean isMileageCompleted() { return mileageCompleted; }
        public boolean isExtraGradCompleted() { return extraGradCompleted; }

        @Override
        public String toString() {
            return String.format("AdditionalRequirements{TLC: %d회, Chapel: %d학기, Mileage: %s, ExtraGrad: %s}",
                tlcCount, chapelCount, mileageCompleted ? "완료" : "미완료", extraGradCompleted ? "완료" : "미완료");
        }
    }
}