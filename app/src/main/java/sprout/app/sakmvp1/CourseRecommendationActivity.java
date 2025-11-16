package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
public class CourseRecommendationActivity extends BaseActivity {

    private static final String TAG = "CourseRecommendation";

    private MaterialToolbar toolbar;
    private Spinner spinnerGrade;
    private Spinner spinnerSemester;
    private Button btnGetRecommendations;

    // 우선순위 선택 UI
    private RadioGroup radioGroupPriority;
    private RadioButton radioPriorityShortage;
    private RadioButton radioPriorityAbundant;
    private RadioButton radioPriorityCustom;
    private Spinner spinnerCategoryPriority;

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

        btnGetRecommendations = findViewById(R.id.btnGetRecommendations);

        // 우선순위 선택 UI 초기화
        radioGroupPriority = findViewById(R.id.radioGroupPriority);
        radioPriorityShortage = findViewById(R.id.radioPriorityShortage);
        radioPriorityAbundant = findViewById(R.id.radioPriorityAbundant);
        radioPriorityCustom = findViewById(R.id.radioPriorityCustom);
        spinnerCategoryPriority = findViewById(R.id.spinnerCategoryPriority);

        // 특정 과목군 선택 스피너 설정
        String[] categories = {
            "교양필수", "전공필수", "학부공통", "전공심화",
            "전공선택", "교양선택", "소양"
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryPriority.setAdapter(categoryAdapter);

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

        // 추천 받기 버튼
        btnGetRecommendations.setOnClickListener(v -> {
            getRecommendations();
        });

        // 라디오 버튼 변경 시 특정 과목군 스피너 표시/숨김
        radioGroupPriority.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPriorityCustom) {
                // 특정 과목군 선택 시 스피너 표시
                spinnerCategoryPriority.setVisibility(View.VISIBLE);
            } else {
                // 다른 옵션 선택 시 스피너 숨김
                spinnerCategoryPriority.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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

        // 선택된 학년/학기 가져오기
        String selectedGrade = spinnerGrade.getSelectedItem().toString();
        String selectedSemester = spinnerSemester.getSelectedItem().toString();
        String currentSemester = selectedGrade + "-" + selectedSemester; // "1-1", "2-2" 형식

        // 선택된 우선순위 모드 확인
        String priorityMode = "shortage"; // 기본값: 부족한 과목군 우선
        String selectedCategory = null;

        int checkedId = radioGroupPriority.getCheckedRadioButtonId();
        if (checkedId == R.id.radioPriorityShortage) {
            priorityMode = "shortage";
        } else if (checkedId == R.id.radioPriorityAbundant) {
            priorityMode = "abundant";
        } else if (checkedId == R.id.radioPriorityCustom) {
            priorityMode = "custom";
            selectedCategory = spinnerCategoryPriority.getSelectedItem().toString();
        }

        // 추천 결과 화면으로 이동
        Intent intent = new Intent(this, RecommendationResultActivity.class);
        intent.putExtra("userYear", userYear);
        intent.putExtra("userDepartment", userDepartment);
        intent.putExtra("userTrack", userTrack);
        intent.putExtra("currentSemester", currentSemester); // 현재 학년/학기 추가
        intent.putExtra("priorityMode", priorityMode); // 우선순위 모드 추가
        if (selectedCategory != null) {
            intent.putExtra("selectedCategory", selectedCategory); // 특정 과목군 추가
        }
        startActivity(intent);
    }

}
