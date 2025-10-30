package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * 수강과목 추천 화면
 *
 * 사용자의 수강 이력과 졸업 요건을 분석하여
 * 맞춤형 과목 추천을 제공하는 Activity입니다.
 *
 * 주요 기능:
 * - 카테고리별 과목 필터링 (전공필수, 전공선택, 교양 등)
 * - AI 기반 맞춤 추천 알고리즘
 * - 수강 가능 과목 목록 표시
 * - 과목 상세 정보 확인
 */
public class CourseRecommendationActivity extends AppCompatActivity {

    private static final String TAG = "CourseRecommendation";

    private MaterialToolbar toolbar;
    private Spinner spinnerGrade;
    private Spinner spinnerSemester;
    private MaterialCheckBox checkboxConsiderTimetable;
    private TextView tvTimetableExplanation;
    private Slider sliderDifficulty;
    private TextView tvDifficultyLevel;
    private TextView tvDifficultyValue;
    private TextView tvDifficultyDescription;
    private Button btnGetRecommendations;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // 저장된 사용자 정보
    private String userYear;
    private String userDepartment;
    private String userTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_recommendation);

        initViews();
        checkUserInfo();
        setupListeners();
    }

    private void initViews() {
        // Toolbar 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 학년/학기 스피너
        spinnerGrade = findViewById(R.id.spinnerGrade);
        spinnerSemester = findViewById(R.id.spinnerSemester);

        // 학년 스피너 설정 (1~4학년)
        String[] grades = {"1", "2", "3", "4"};
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, grades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrade.setAdapter(gradeAdapter);
        spinnerGrade.setSelection(0); // 기본값: 1학년

        // 학기 스피너 설정 (1학기, 2학기)
        String[] semesters = {"1", "2"};
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);
        spinnerSemester.setSelection(0); // 기본값: 1학기

        // 옵션 체크박스 및 설명
        checkboxConsiderTimetable = findViewById(R.id.checkboxConsiderTimetable);
        tvTimetableExplanation = findViewById(R.id.tvTimetableExplanation);

        // 난이도 슬라이더 및 관련 텍스트
        sliderDifficulty = findViewById(R.id.sliderDifficulty);
        tvDifficultyLevel = findViewById(R.id.tvDifficultyLevel);
        tvDifficultyValue = findViewById(R.id.tvDifficultyValue);
        tvDifficultyDescription = findViewById(R.id.tvDifficultyDescription);

        // 커스텀 수직 막대 형태의 thumb drawable 적용
        sliderDifficulty.setCustomThumbDrawable(R.drawable.slider_thumb_large);

        // halo를 투명하게 설정하여 thumb 형태만 표시
        sliderDifficulty.setHaloRadius(0);

        btnGetRecommendations = findViewById(R.id.btnGetRecommendations);

        // Firebase 초기화
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * 저장된 사용자 정보 확인
     */
    private void checkUserInfo() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.w(TAG, "사용자가 로그인하지 않음");
            showUserInfoRequiredDialog();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() &&
                        documentSnapshot.contains("studentYear") &&
                        documentSnapshot.contains("department") &&
                        documentSnapshot.contains("track")) {

                        // 저장된 정보가 있음
                        userYear = documentSnapshot.getString("studentYear");
                        userDepartment = documentSnapshot.getString("department");
                        userTrack = documentSnapshot.getString("track");

                        Log.d(TAG, "저장된 정보 발견: " + userYear + "/" + userDepartment + "/" + userTrack);
                    } else {
                        // 저장된 정보가 없음
                        Log.d(TAG, "저장된 사용자 정보 없음");
                        showUserInfoRequiredDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "사용자 정보 확인 실패", e);
                    showUserInfoRequiredDialog();
                });
    }

    /**
     * 사용자 정보 입력 필요 안내 다이얼로그
     */
    private void showUserInfoRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("학적 정보 입력 필요")
                .setMessage("수강과목 추천을 위해서는 학적 정보(학번, 학부, 트랙)가 필요합니다.\n\n" +
                        "'내 학적정보' 메뉴에서 정보를 입력해주세요.")
                .setPositiveButton("정보 입력하기", (dialog, which) -> {
                    Intent intent = new Intent(CourseRecommendationActivity.this, UserInfoActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void setupListeners() {
        // Toolbar 뒤로가기 버튼
        toolbar.setNavigationOnClickListener(v -> finish());

        // 시간표 고려 체크박스
        checkboxConsiderTimetable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvTimetableExplanation.setVisibility(View.VISIBLE);
                tvTimetableExplanation.setText("✓ 기존 시간표와 겹치지 않는 과목만 추천합니다");
                tvTimetableExplanation.setTextColor(getColor(com.google.android.material.R.color.design_default_color_primary));
            } else {
                tvTimetableExplanation.setVisibility(View.VISIBLE);
                tvTimetableExplanation.setText("✕ 시간표 중복 여부를 고려하지 않습니다");
                tvTimetableExplanation.setTextColor(getColor(android.R.color.darker_gray));
            }
        });

        // 난이도 슬라이더 변경 리스너
        sliderDifficulty.addOnChangeListener((slider, value, fromUser) -> {
            updateDifficultyUI((int) value);
        });

        // 추천 받기 버튼
        btnGetRecommendations.setOnClickListener(v -> {
            getRecommendations();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * 난이도 UI 업데이트
     */
    private void updateDifficultyUI(int value) {
        // 단계 표시
        tvDifficultyValue.setText(value + "/3");

        // 난이도 레벨 및 설명
        String level;
        String description;
        int color;

        if (value == 1) {
            level = "😊 쉬움";
            description = "여유로운 학습 부담으로 편안한 학기를 보낼 수 있습니다. 다른 활동에도 시간을 투자할 수 있어요.";
            color = getColor(android.R.color.holo_green_dark);
        } else if (value == 2) {
            level = "📚 보통";
            description = "균형잡힌 학습 부담으로 안정적인 학기를 보낼 수 있습니다. 적절한 도전과 성취감을 느낄 수 있어요.";
            color = getColor(com.google.android.material.R.color.design_default_color_primary);
        } else {
            level = "🔥 어려움";
            description = "도전적인 학습 부담으로 집중이 필요한 학기입니다. 높은 성취감을 얻을 수 있지만 시간 관리가 중요해요.";
            color = getColor(android.R.color.holo_red_dark);
        }

        tvDifficultyLevel.setText(level);
        tvDifficultyLevel.setTextColor(color);
        tvDifficultyDescription.setText(description);
    }

    /**
     * 추천 과목을 가져옵니다
     */
    private void getRecommendations() {
        // 사용자 정보 확인
        if (userYear == null || userDepartment == null || userTrack == null) {
            Toast.makeText(this, "학적 정보를 먼저 입력해주세요", Toast.LENGTH_SHORT).show();
            showUserInfoRequiredDialog();
            return;
        }

        boolean considerTimetable = checkboxConsiderTimetable.isChecked();
        int difficultyLevel = (int) sliderDifficulty.getValue();

        // 선택된 학년/학기 가져오기
        String selectedGrade = spinnerGrade.getSelectedItem().toString();
        String selectedSemester = spinnerSemester.getSelectedItem().toString();
        String currentSemester = selectedGrade + "-" + selectedSemester; // "1-1", "2-2" 형식

        // 추천 결과 화면으로 이동
        Intent intent = new Intent(this, RecommendationResultActivity.class);
        intent.putExtra("considerTimetable", considerTimetable);
        intent.putExtra("difficultyLevel", difficultyLevel);
        intent.putExtra("userYear", userYear);
        intent.putExtra("userDepartment", userDepartment);
        intent.putExtra("userTrack", userTrack);
        intent.putExtra("currentSemester", currentSemester); // 현재 학년/학기 추가
        startActivity(intent);
    }

}
