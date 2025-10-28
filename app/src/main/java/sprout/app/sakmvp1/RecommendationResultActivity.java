package sprout.app.sakmvp1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
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
import sprout.app.sakmvp1.models.CourseRequirement;
import sprout.app.sakmvp1.models.GraduationAnalysisResult;
import sprout.app.sakmvp1.models.GraduationRules;
import sprout.app.sakmvp1.models.RequirementCategory;

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
    private MaterialCardView cardPrioritySummary;
    private LinearLayout layoutPrioritySummary;
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
    private String currentSemester;

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
        cardPrioritySummary = findViewById(R.id.cardPrioritySummary);
        layoutPrioritySummary = findViewById(R.id.layoutPrioritySummary);
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
        currentSemester = getIntent().getStringExtra("currentSemester");

        // 추천 옵션 표시
        String timetableText = considerTimetable ? "고려함" : "안함";
        String difficultyText = difficultyLevel == 1 ? "😊 쉬움" :
                               difficultyLevel == 2 ? "📚 보통" : "🔥 어려움";
        String semesterText = currentSemester != null ? currentSemester + " 이하" : "전체";
        tvRecommendationOptions.setText("시간표 고려: " + timetableText + " | 학기 난이도: " + difficultyText + " | 학기: " + semesterText);

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

            // oneOf 그룹 과목 추출 (이 중 하나만 선택하는 그룹)
            Set<String> oneOfGroupCourses = new HashSet<>();
            Map<String, String> oneOfGroupRepresentative = new HashMap<>(); // 그룹 -> 대표 과목
            Map<String, List<String>> oneOfGroupMembers = new HashMap<>(); // 그룹 -> 모든 과목 리스트

            // SubgroupResults에서 oneOf 그룹 확인
            Log.d(TAG, "  >>> SubgroupResults 확인 시작");
            if (categoryResult.getSubgroupResults() != null) {
                Log.d(TAG, "      SubgroupResults 개수: " + categoryResult.getSubgroupResults().size());
                int subgroupIndex = 0;
                for (CategoryAnalysisResult.SubgroupResult subgroup : categoryResult.getSubgroupResults()) {
                    subgroupIndex++;
                    Log.d(TAG, "      [" + subgroupIndex + "] groupId: " + subgroup.getGroupId() + ", name: " + subgroup.getGroupName());
                    List<String> availableCourses = subgroup.getAvailableCourses();
                    if (availableCourses != null) {
                        Log.d(TAG, "          availableCourses: " + availableCourses.size() + "개");
                        for (String course : availableCourses) {
                            Log.d(TAG, "            - " + course);
                        }
                        if (availableCourses.size() > 1) {
                            // oneOf 그룹으로 추정 (여러 선택지가 있는 경우)
                            String groupId = subgroup.getGroupId();

                            // 이 그룹의 모든 과목을 Set에 추가
                            oneOfGroupCourses.addAll(availableCourses);

                            // 그룹의 모든 과목 리스트 저장
                            oneOfGroupMembers.put(groupId, new ArrayList<>(availableCourses));

                            // 첫 번째 과목을 대표로 선정
                            if (!oneOfGroupRepresentative.containsKey(groupId)) {
                                oneOfGroupRepresentative.put(groupId, availableCourses.get(0));
                                Log.d(TAG, "          ✓ oneOf 그룹 발견: " + groupId + ", 대표 과목: " + availableCourses.get(0) +
                                      " (총 " + availableCourses.size() + "개 선택지)");
                            }
                        }
                    } else {
                        Log.d(TAG, "          availableCourses: NULL");
                    }
                }
            } else {
                Log.d(TAG, "      SubgroupResults가 NULL입니다");
            }

            // 미이수 과목 리스트
            List<String> missingCourses = categoryResult.getMissingCourses();
            Map<String, Integer> courseCreditsMap = categoryResult.getCourseCreditsMap();

            if (missingCourses != null && !missingCourses.isEmpty()) {
                Log.d(TAG, "  미이수 과목 " + missingCourses.size() + "개 발견");

                for (String courseName : missingCourses) {
                    // oneOf 그룹 처리: 대표 과목이 아니면 건너뜀
                    if (oneOfGroupCourses.contains(courseName)) {
                        boolean isRepresentative = false;
                        for (String representative : oneOfGroupRepresentative.values()) {
                            if (representative.equals(courseName)) {
                                isRepresentative = true;
                                break;
                            }
                        }
                        if (!isRepresentative) {
                            Log.d(TAG, "    ⊘ " + courseName + " (oneOf 그룹 중복 제외)");
                            continue;
                        }
                    }

                    // 학기 정보 확인 및 필터링
                    String courseSemester = getCourseSemester(courseName, rules);
                    if (!isSemesterEligible(courseSemester, currentSemester, categoryName)) {
                        Log.d(TAG, "    ✗ " + courseName + " (학기 필터링: " + courseSemester + " | 현재: " + currentSemester + " | 카테고리: " + categoryName + ")");
                        continue; // 학기 조건에 맞지 않는 과목은 건너뜀
                    }

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

                    // oneOf 그룹의 대표 과목인 경우, 대체 가능한 과목 리스트 설정
                    if (oneOfGroupCourses.contains(courseName)) {
                        // 이 과목이 속한 그룹 찾기
                        for (Map.Entry<String, String> entry : oneOfGroupRepresentative.entrySet()) {
                            if (entry.getValue().equals(courseName)) {
                                String groupId = entry.getKey();
                                List<String> groupMembers = oneOfGroupMembers.get(groupId);
                                if (groupMembers != null && groupMembers.size() > 1) {
                                    // 자신을 제외한 나머지 과목들을 대체 과목으로 설정
                                    List<String> alternatives = new ArrayList<>();
                                    for (String member : groupMembers) {
                                        if (!member.equals(courseName)) {
                                            alternatives.add(member);
                                        }
                                    }
                                    course.setAlternativeCourses(alternatives);
                                    Log.d(TAG, "    ✓ " + courseName + " (oneOf 대체 과목 " + alternatives.size() + "개 설정)");
                                }
                                break;
                            }
                        }
                    }

                    allRecommendations.add(course);

                    Log.d(TAG, "    ✓ " + courseName + " (우선순위: " + priority + ", 학기: " + (courseSemester != null ? courseSemester : "미지정") + ")");
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
            cardPrioritySummary.setVisibility(View.GONE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerViewRecommendations.setVisibility(View.VISIBLE);

            // 우선순위 요약 생성 및 표시
            displayPrioritySummary(allCourses);

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

    /**
     * 카테고리별 과목군 요약 표시
     */
    private void displayPrioritySummary(List<RecommendedCourse> courses) {
        layoutPrioritySummary.removeAllViews();

        // 카테고리별 그룹화
        Map<String, List<RecommendedCourse>> categoryGroups = new HashMap<>();
        for (RecommendedCourse course : courses) {
            String category = course.getCategory();
            if (!categoryGroups.containsKey(category)) {
                categoryGroups.put(category, new ArrayList<>());
            }
            categoryGroups.get(category).add(course);
        }

        // 카테고리 표시 순서 정의 (우선순위 기반)
        String[] categoryOrder = {
            "교양필수", "전공필수", "학부공통", "전공심화",
            "전공선택", "교양선택", "소양", "자율선택", "일반선택", "잔여학점"
        };

        boolean hasAnyCategory = false;
        for (String category : categoryOrder) {
            List<RecommendedCourse> categoryCourses = categoryGroups.get(category);
            if (categoryCourses != null && !categoryCourses.isEmpty()) {
                hasAnyCategory = true;
                addCategoryView(category, categoryCourses);
            }
        }

        // 정의되지 않은 카테고리도 추가
        for (Map.Entry<String, List<RecommendedCourse>> entry : categoryGroups.entrySet()) {
            String category = entry.getKey();
            boolean isInOrder = false;
            for (String orderedCategory : categoryOrder) {
                if (orderedCategory.equals(category)) {
                    isInOrder = true;
                    break;
                }
            }
            if (!isInOrder && !entry.getValue().isEmpty()) {
                hasAnyCategory = true;
                addCategoryView(category, entry.getValue());
            }
        }

        cardPrioritySummary.setVisibility(hasAnyCategory ? View.VISIBLE : View.GONE);
    }

    /**
     * 카테고리별 뷰 추가
     */
    private void addCategoryView(String category, List<RecommendedCourse> courses) {
        View categoryView = getLayoutInflater().inflate(R.layout.item_priority_level, layoutPrioritySummary, false);

        Chip chipCategoryName = categoryView.findViewById(R.id.chipCategoryName);
        TextView tvCourseCount = categoryView.findViewById(R.id.tvCourseCount);
        TextView tvCategoryDescription = categoryView.findViewById(R.id.tvCategoryDescription);

        // 카테고리명과 과목 수 표시
        chipCategoryName.setText(category);
        tvCourseCount.setText(" · " + courses.size() + "과목");

        // 카테고리별 색상 적용
        int color = getCategoryColor(category);
        chipCategoryName.setChipBackgroundColor(ColorStateList.valueOf(color));
        // 텍스트 색상은 배경색의 밝기에 따라 자동 결정
        chipCategoryName.setTextColor(getContrastColor(color));

        // 카테고리별 설명 설정
        String description = getCategoryDescription(category);
        tvCategoryDescription.setText(description);

        layoutPrioritySummary.addView(categoryView);
    }

    /**
     * 카테고리별 추천 설명 반환
     */
    private String getCategoryDescription(String category) {
        switch (category) {
            case "교양필수":
                return "💡 최대한 저학년일 때 듣는 것이 좋습니다";
            case "학부공통":
            case "전공심화":
                return "📅 학년에 맞춰서 다 듣는 것을 추천합니다";
            case "전공필수":
                return "✅ 웬만하면 듣는 것을 추천합니다";
            case "전공선택":
                return "🎯 학점 여유에 맞춰 듣는 것을 추천합니다";
            case "교양선택":
            case "소양":
                return "⭐ 듣고 싶은 과목의 자리를 얻었다면 그때 들으세요";
            case "자율선택":
            case "일반선택":
            case "잔여학점":
                return "📝 자유롭게 수강할 수 있는 학점입니다";
            default:
                return "📚 추천되는 과목입니다";
        }
    }

    /**
     * 카테고리별 색상 반환
     */
    public static int getCategoryColor(String category) {
        switch (category) {
            case "교양필수":
                return Color.parseColor("#FFB74D"); // 주황색
            case "전공필수":
                return Color.parseColor("#EF5350"); // 빨간색
            case "학부공통":
                return Color.parseColor("#42A5F5"); // 파란색
            case "전공심화":
                return Color.parseColor("#AB47BC"); // 보라색
            case "전공선택":
                return Color.parseColor("#66BB6A"); // 초록색
            case "교양선택":
                return Color.parseColor("#FFA726"); // 밝은 주황색
            case "소양":
                return Color.parseColor("#26C6DA"); // 청록색
            case "자율선택":
            case "일반선택":
            case "잔여학점":
                return Color.parseColor("#9E9E9E"); // 회색
            default:
                return Color.parseColor("#78909C"); // 청회색
        }
    }

    /**
     * 배경색에 대비되는 텍스트 색상 반환 (밝기 기반)
     */
    private int getContrastColor(int color) {
        // 색상의 밝기 계산
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.5 ? Color.BLACK : Color.WHITE;
    }

    private void showError(String message) {
        tvEmptyMessage.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText("❌ 오류\n\n" + message);
        recyclerViewRecommendations.setVisibility(View.GONE);
        cardPrioritySummary.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * 학기 비교 헬퍼 메서드 (전공 과목용 - 홀짝 학기 매칭)
     * @param courseSemester 과목의 학기 (예: "1-1", "2-2")
     * @param currentSemester 현재 학기 (예: "2-1")
     * @param categoryName 카테고리 이름 (전공/교양 구분용)
     * @return 과목이 추천 가능하면 true, 아니면 false
     */
    private boolean isSemesterEligible(String courseSemester, String currentSemester, String categoryName) {
        if (courseSemester == null || courseSemester.isEmpty()) {
            // 학기 정보가 없는 과목은 항상 추천 (선택과목 등)
            return true;
        }
        if (currentSemester == null || currentSemester.isEmpty()) {
            // 현재 학기 필터가 없으면 모든 과목 추천
            return true;
        }

        try {
            // "X-Y" 형식 파싱
            String[] courseParts = courseSemester.split("-");
            String[] currentParts = currentSemester.split("-");

            if (courseParts.length != 2 || currentParts.length != 2) {
                // 파싱 실패 시 추천 (안전한 기본값)
                return true;
            }

            int courseGrade = Integer.parseInt(courseParts[0]);
            int courseSem = Integer.parseInt(courseParts[1]);
            int currentGrade = Integer.parseInt(currentParts[0]);
            int currentSem = Integer.parseInt(currentParts[1]);

            // 교양 과목은 학기 상관없이 현재 학년 이하만 체크
            boolean isGeneralEducation = categoryName != null &&
                (categoryName.contains("교양") || categoryName.contains("소양"));

            if (isGeneralEducation) {
                // 교양: 학년만 체크 (학기 무관)
                return courseGrade <= currentGrade;
            }

            // 전공 과목: 학년 체크 + 같은 홀짝 학기만
            // 미래 학년 과목은 제외
            if (courseGrade > currentGrade) {
                return false;
            }

            // 현재 학년 이하이고, 같은 홀짝 학기인지 확인
            // 예: 현재 3학년 2학기(짝수) → 1-2, 2-2, 3-2만 추천
            if (courseSem == currentSem) {
                // 같은 학기 번호 (1학기끼리 또는 2학기끼리)
                return true;
            }

            // 다른 학기 번호는 제외
            return false;

        } catch (NumberFormatException e) {
            Log.w(TAG, "학기 파싱 실패: " + courseSemester + ", " + currentSemester, e);
            return true; // 파싱 실패 시 추천 (안전한 기본값)
        }
    }

    /**
     * GraduationRules에서 특정 과목의 학기 정보 찾기
     * @param courseName 과목명
     * @param rules 졸업요건 규칙
     * @return 학기 정보 (없으면 null)
     */
    private String getCourseSemester(String courseName, GraduationRules rules) {
        if (courseName == null || rules == null) {
            return null;
        }

        // 모든 카테고리를 순회하며 과목 찾기
        if (rules.getCategories() != null) {
            for (RequirementCategory category : rules.getCategories()) {
                String semester = searchCourseSemester(courseName, category);
                if (semester != null) {
                    return semester;
                }
            }
        }
        return null;
    }

    /**
     * 카테고리 내에서 과목의 학기 정보 재귀 검색
     * @param courseName 과목명
     * @param category 카테고리
     * @return 학기 정보 (없으면 null)
     */
    private String searchCourseSemester(String courseName, RequirementCategory category) {
        if (category == null) {
            return null;
        }

        // 현재 카테고리의 과목 목록 검색
        if (category.getCourses() != null) {
            for (CourseRequirement courseReq : category.getCourses()) {
                if (courseName.equals(courseReq.getName())) {
                    return courseReq.getSemester();
                }
            }
        }

        // 하위 그룹 재귀 검색
        if (category.getSubgroups() != null) {
            for (RequirementCategory subgroup : category.getSubgroups()) {
                String semester = searchCourseSemester(courseName, subgroup);
                if (semester != null) {
                    return semester;
                }
            }
        }

        return null;
    }
}
