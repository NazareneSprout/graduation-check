package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sprout.app.sakmvp1.models.CategoryAnalysisResult;
import sprout.app.sakmvp1.models.GraduationAnalysisResult;
import sprout.app.sakmvp1.models.GraduationRules;

/**
 * 수강과목 추천 결과 화면 (V2 통합)
 *
 * FirebaseDataManager와 GraduationRules를 활용하여
 * 졸업 요건을 분석하고 맞춤형 과목을 추천합니다.
 */
public class RecommendationResultActivity extends AppCompatActivity {

    private static final String TAG = "RecommendationResult";

    private MaterialToolbar toolbar;
    private TextView tvRecommendationOptions;
    private RecyclerView recyclerViewRecommendations;
    private ProgressBar progressBar;
    private TextView tvEmptyMessage;

    private RecommendedCourseAdapter adapter;
    private List<RecommendedCourse> recommendedCourses;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseDataManager dataManager;

    private String userYear;
    private String userDepartment;
    private String userTrack;
    private boolean considerTimetable;
    private int difficultyLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation_result);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dataManager = FirebaseDataManager.getInstance();
        recommendedCourses = new ArrayList<>();

        initViews();
        loadData();
    }

    private void initViews() {
        // Toolbar 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Views
        tvRecommendationOptions = findViewById(R.id.tvRecommendationOptions);
        recyclerViewRecommendations = findViewById(R.id.recyclerViewRecommendations);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // RecyclerView 설정
        adapter = new RecommendedCourseAdapter(recommendedCourses);
        recyclerViewRecommendations.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecommendations.setAdapter(adapter);

        // 초기에는 로딩 표시
        showLoading(true);
    }

    private void loadData() {
        // Intent에서 정보 가져오기
        considerTimetable = getIntent().getBooleanExtra("considerTimetable", false);
        difficultyLevel = getIntent().getIntExtra("difficultyLevel", 2);
        userYear = getIntent().getStringExtra("userYear");
        userDepartment = getIntent().getStringExtra("userDepartment");
        userTrack = getIntent().getStringExtra("userTrack");

        // 추천 옵션 표시
        String timetableText = considerTimetable ? "고려함" : "안함";
        String difficultyText = difficultyLevel == 1 ? "😊 쉬움" :
                               difficultyLevel == 2 ? "📚 보통" : "🔥 어려움";
        tvRecommendationOptions.setText("시간표 고려: " + timetableText + " | 학기 난이도: " + difficultyText);

        // 추천 과목 생성
        generateRecommendations();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewRecommendations != null) {
            recyclerViewRecommendations.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (tvEmptyMessage != null) {
            tvEmptyMessage.setVisibility(View.GONE);
        }
    }

    /**
     * 추천 과목 생성 (V2 통합 방식)
     */
    private void generateRecommendations() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "========================================");
        Log.d(TAG, "수강과목 추천 시작");
        Log.d(TAG, "학번: " + userYear + ", 학부: " + userDepartment + ", 트랙: " + userTrack);
        Log.d(TAG, "========================================");

        // 1. 사용자 수강 이력 가져오기
        db.collection("users").document(userId)
                .collection("courses")
                .get()
                .addOnSuccessListener(coursesSnapshot -> {
                    // 수강한 과목 리스트 생성
                    List<CourseInputActivity.Course> takenCourses = new ArrayList<>();

                    for (DocumentSnapshot doc : coursesSnapshot.getDocuments()) {
                        String courseName = doc.getString("courseName");
                        String category = doc.getString("category");
                        Object creditsObj = doc.get("credits");

                        if (courseName != null && category != null) {
                            int credits = 0;
                            if (creditsObj instanceof Long) {
                                credits = ((Long) creditsObj).intValue();
                            } else if (creditsObj instanceof Integer) {
                                credits = (Integer) creditsObj;
                            }

                            CourseInputActivity.Course course = new CourseInputActivity.Course(
                                category, courseName, credits
                            );
                            takenCourses.add(course);
                        }
                    }

                    Log.d(TAG, "수강한 과목 수: " + takenCourses.size());

                    // 2. GraduationRules 로드 및 분석
                    loadGraduationRulesAndAnalyze(takenCourses);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "사용자 수강 이력 로드 실패", e);
                    showLoading(false);
                    showError("수강 이력을 불러올 수 없습니다");
                });
    }

    /**
     * GraduationRules 로드 및 분석
     */
    private void loadGraduationRulesAndAnalyze(List<CourseInputActivity.Course> takenCourses) {
        // 학번을 숫자로 변환 (예: "2020" -> 2020)
        long cohort = 0;
        try {
            cohort = Long.parseLong(userYear);
        } catch (NumberFormatException e) {
            Log.e(TAG, "학번 파싱 실패: " + userYear, e);
            showLoading(false);
            showError("학번 형식이 올바르지 않습니다");
            return;
        }

        final long finalCohort = cohort;

        // FirebaseDataManager를 사용하여 GraduationRules 로드
        dataManager.loadGraduationRules(
            String.valueOf(cohort),
            userDepartment,
            userTrack,
            new FirebaseDataManager.OnGraduationRulesLoadedListener() {
                @Override
                public void onSuccess(GraduationRules rules) {
                    Log.d(TAG, "✓ GraduationRules 로드 성공");

                    // 졸업요건 분석 실행
                    GraduationAnalysisResult analysisResult = rules.analyze(takenCourses);

                    // 추천 과목 생성
                    generateRecommendationsFromAnalysis(analysisResult, rules);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "GraduationRules 로드 실패", e);
                    showLoading(false);
                    showError("졸업요건 정보를 불러올 수 없습니다");
                }
            }
        );
    }

    /**
     * 분석 결과에서 추천 과목 생성
     */
    private void generateRecommendationsFromAnalysis(GraduationAnalysisResult analysisResult, GraduationRules rules) {
        List<RecommendedCourse> allRecommendations = new ArrayList<>();

        Log.d(TAG, "========================================");
        Log.d(TAG, "분석 결과에서 추천 과목 생성");

        // 각 카테고리별로 미이수 과목 추출
        for (CategoryAnalysisResult categoryResult : analysisResult.getAllCategoryResults()) {
            String categoryName = categoryResult.getCategoryName();
            int earnedCredits = categoryResult.getEarnedCredits();
            int requiredCredits = categoryResult.getRequiredCredits();
            int remainingCredits = Math.max(0, requiredCredits - earnedCredits);

            // 완료된 카테고리는 건너뛰기
            if (categoryResult.isCompleted() || remainingCredits == 0) {
                continue;
            }

            Log.d(TAG, "카테고리: " + categoryName +
                  " (" + earnedCredits + "/" + requiredCredits + ", 부족: " + remainingCredits + ")");

            // 미이수 과목 리스트
            List<String> missingCourses = categoryResult.getMissingCourses();
            Map<String, Integer> courseCreditsMap = categoryResult.getCourseCreditsMap();

            if (missingCourses != null && !missingCourses.isEmpty()) {
                Log.d(TAG, "  미이수 과목 " + missingCourses.size() + "개 발견");

                for (String courseName : missingCourses) {
                    // 학점 정보 가져오기
                    int credits = 3; // 기본값
                    if (courseCreditsMap != null && courseCreditsMap.containsKey(courseName)) {
                        credits = courseCreditsMap.get(courseName);
                    }

                    // 우선순위 계산 (부족한 학점이 많을수록 높은 우선순위)
                    int priority = calculatePriority(categoryName, remainingCredits, categoryResult.isCompleted());
                    String reason = getRecommendationReason(categoryName, remainingCredits);

                    RecommendedCourse course = new RecommendedCourse(
                        courseName, categoryName, credits, priority, reason
                    );
                    allRecommendations.add(course);

                    Log.d(TAG, "    - " + courseName + " (우선순위: " + priority + ")");
                }
            }
        }

        Log.d(TAG, "========================================");
        Log.d(TAG, "총 추천 과목 수: " + allRecommendations.size());

        // 우선순위로 정렬
        Collections.sort(allRecommendations, new Comparator<RecommendedCourse>() {
            @Override
            public int compare(RecommendedCourse c1, RecommendedCourse c2) {
                // 우선순위 낮은 숫자가 먼저 (1 > 2 > 3...)
                int priorityCompare = Integer.compare(c1.getPriority(), c2.getPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                // 우선순위가 같으면 카테고리 순서
                int categoryCompare = c1.getCategory().compareTo(c2.getCategory());
                if (categoryCompare != 0) {
                    return categoryCompare;
                }
                // 카테고리도 같으면 과목명 순서
                return c1.getCourseName().compareTo(c2.getCourseName());
            }
        });

        // UI 업데이트
        finalizeRecommendations(allRecommendations);
    }

    /**
     * 우선순위 계산
     * 숫자가 낮을수록 높은 우선순위
     */
    private int calculatePriority(String category, int remainingCredits, boolean isCompleted) {
        // 기본 카테고리 우선순위
        int basePriority = RecommendedCourse.getCategoryPriority(category);

        // 이미 완료된 카테고리는 낮은 우선순위
        if (isCompleted) {
            return basePriority + 100;
        }

        // 부족한 학점이 많을수록 높은 우선순위
        if (remainingCredits > 0) {
            // 12학점 이상 부족: 우선순위 +0
            // 6-11학점 부족: 우선순위 +10
            // 1-5학점 부족: 우선순위 +20
            if (remainingCredits >= 12) {
                return basePriority;
            } else if (remainingCredits >= 6) {
                return basePriority + 10;
            } else {
                return basePriority + 20;
            }
        }

        return basePriority;
    }

    /**
     * 추천 이유 생성
     */
    private String getRecommendationReason(String category, int remainingCredits) {
        String baseReason;

        switch (category) {
            case "교양필수":
                baseReason = "필수 교양과목";
                break;
            case "전공필수":
                baseReason = "필수 전공과목";
                break;
            case "학부공통":
                baseReason = "학부공통 이수과목";
                break;
            case "전공심화":
                baseReason = "전공심화 이수과목";
                break;
            case "전공선택":
                baseReason = "전공선택 이수과목";
                break;
            case "소양":
                baseReason = "소양 이수과목";
                break;
            case "교양선택":
                baseReason = "교양선택 이수과목";
                break;
            default:
                baseReason = "이수 권장 과목";
        }

        if (remainingCredits > 0) {
            return baseReason + " · 부족 " + remainingCredits + "학점";
        } else {
            return baseReason;
        }
    }

    /**
     * 추천 과목 최종 표시
     */
    private void finalizeRecommendations(List<RecommendedCourse> allCourses) {
        showLoading(false);

        // Adapter 업데이트
        recommendedCourses.clear();
        recommendedCourses.addAll(allCourses);
        adapter.notifyDataSetChanged();

        // 결과 확인
        if (allCourses.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("🎉 축하합니다!\n\n모든 필수 과목을 이수했습니다.\n선택 과목을 자유롭게 수강하세요.");
            recyclerViewRecommendations.setVisibility(View.GONE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerViewRecommendations.setVisibility(View.VISIBLE);

            String message = "📚 " + allCourses.size() + "개의 과목을 추천합니다";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            Log.d(TAG, "최종 추천 과목:");
            for (int i = 0; i < allCourses.size(); i++) {
                RecommendedCourse course = allCourses.get(i);
                Log.d(TAG, (i+1) + ". " + course.getCourseName() +
                      " [" + course.getCategory() + "] " +
                      "(" + course.getCredits() + "학점, 우선순위: " + course.getPriority() + ")");
            }
        }
    }

    private void showError(String message) {
        tvEmptyMessage.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText("❌ 오류\n\n" + message);
        recyclerViewRecommendations.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
