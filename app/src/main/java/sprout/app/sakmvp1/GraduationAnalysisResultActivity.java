package sprout.app.sakmvp1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sprout.app.sakmvp1.CourseInputActivity.Course;

/**
 * 졸업 요건 분석 결과 화면
 *
 * <p>이 Activity는 졸업 요건 분석 과정의 마지막 단계로, 사용자가 입력한 모든 정보를
 * 종합하여 졸업 진행 상황을 시각적으로 분석하고 표시하는 화면입니다.
 * 도넛 차트를 통한 전체 진행률과 상세 분석 결과를 탭 형태로 제공합니다.</p>
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>📊 <strong>도넛 차트 시각화</strong>: 전체 졸업 진행률을 시각적으로 표현</li>
 *   <li>📈 <strong>상세 분석</strong>: 전공/교양 카테고리별 이수 현황 분석</li>
 *   <li>📝 <strong>이수 과목 현황</strong>: 카테고리별 이수한 과목 목록 표시</li>
 *   <li>🎯 <strong>추가 요건 분석</strong>: TLC, 채플, 마일리지 등 특별 요구사항 평가</li>
 *   <li>💾 <strong>결과 저장</strong>: SharedPreferences를 통한 분석 결과 저장</li>
 * </ul>
 *
 * <h3>분석 항목:</h3>
 * <ul>
 *   <li>🏫 <strong>전공 과목</strong>: 전공필수, 전공선택, 전공심화 분석</li>
 *   <li>📚 <strong>교양 과목</strong>: 교양필수, 교양선택 분석</li>
 *   <li>🎓 <strong>학부공통</strong>: 학부공통 과목 이수 현황</li>
 *   <li>➕ <strong>추가 요건</strong>: TLC, 채플, 마일리지, 학과별 특별 요구사항</li>
 * </ul>
 *
 * <h3>UI 구성:</h3>
 * <ul>
 *   <li>📊 <strong>상단 도넛 차트</strong>: 전체 졸업 진행률 시각화</li>
 *   <li>📋 <strong>탭 기반 네비게이션</strong>: ViewPager2로 구현된 상세 분석 탭</li>
 *   <li>🧭 <strong>하단 네비게이션</strong>: 다른 화면으로의 이동</li>
 *   <li>📱 <strong>학생 정보</strong>: 상단에 학번/학과/트랙 정보 표시</li>
 * </ul>
 *
 * <h3>데이터 관리:</h3>
 * <ul>
 *   <li>🔄 <strong>정적 데이터 공유</strong>: Fragment 간 데이터 공유를 위한 static 필드 활용</li>
 *   <li>🗂️ <strong>코스 분류</strong>: 입력된 과목을 카테고리별로 자동 분류</li>
 *   <li>💾 <strong>캐싱</strong>: 분석 결과를 메모리에 캐싱하여 탭 전환 시 빠른 접근</li>
 * </ul>
 *
 * <h3>성능 최적화:</h3>
 * <ul>
 *   <li>⚡ <strong>배치 분석</strong>: 모든 분석을 한 번에 수행하여 중복 계산 방지</li>
 *   <li>📊 <strong>Fragment 재사용</strong>: FragmentStateAdapter를 통한 효율적인 탭 관리</li>
 *   <li>🔍 <strong>실시간 계산</strong>: 학점과 진행률을 동적으로 계산</li>
 * </ul>
 *
 * @see CourseInputActivity 이전 단계 (수강 강의 입력)
 * @see DonutChartView 도넛 차트 커스텀 뷰
 * @see FirebaseDataManager 졸업 요건 데이터 관리
 * @see GraduationProgress 졸업 진행 상황 데이터 모델
 */
public class GraduationAnalysisResultActivity extends AppCompatActivity {

    private static final String TAG = "GraduationResult";

    private TextView textViewStudentInfo;
    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private GraduationTabAdapter tabAdapter;

    private String selectedYear, selectedDepartment, selectedTrack;
    private List<CourseInputActivity.Course> courseList;
    private AdditionalRequirementsActivity.AdditionalRequirements additionalRequirements;
    private static List<CourseInputActivity.Course> staticCourseList;
    private static GraduationRequirements graduationRequirements;
    private static GraduationProgress graduationProgress;
    private static List<String> allMajorRequiredCourses;
    private static List<String> allMajorElectiveCourses;
    private static List<String> allMajorAdvancedCourses;
    private static List<String> allDepartmentCommonCourses;
    private static FirebaseDataManager.CreditRequirements creditRequirements;
    private static List<String> takenMajorRequiredCourses;
    private static List<String> takenMajorElectiveCourses;
    private static List<String> takenMajorAdvancedCourses;
    private static List<String> takenDepartmentCommonCourses;
    private static GeneralEducationAnalysis generalEducationAnalysis;
    private static Map<String, Integer> courseCreditsMap = new HashMap<>(); // 모든 강의의 학점 정보 저장

    // 대체과목 관련 필드 (레거시 - 현재는 GraduationRules 모델에서 처리)
    // 통합 졸업요건 시스템으로 마이그레이션 되었으므로 별도 로드 불필요
    private static Map<String, List<String>> replacementCoursesMap = new HashMap<>(); // 폐지된 과목 -> 대체 과목 목록 매핑

    // Fragment에서 접근할 수 있도록 정적 필드 추가
    private static String staticSelectedYear;
    private static String staticSelectedDepartment;
    private static AdditionalRequirementsActivity.AdditionalRequirements staticAdditionalRequirements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_graduation_analysis_result);

        // 저장된 데이터에서 불러오는 경우 체크
        Intent intent = getIntent();
        boolean fromSaved = intent.getBooleanExtra("fromSaved", false);
        String savedDocId = intent.getStringExtra("savedDocId");

        if (fromSaved && savedDocId != null) {
            // 저장된 데이터 불러오기 (비동기)
            loadSavedGraduationResult(savedDocId);
            return; // onCreate 종료 - loadSavedGraduationResult에서 나머지 초기화 수행
        }

        // 일반적인 경우 (Intent로 데이터 전달받음)
        getIntentData();
        initViews();
        setupToolbar();

        performGraduationAnalysis();
    }

    private void getIntentData() {
        Intent intent = getIntent();

        // 일반적인 경우 (Intent에서 직접 데이터 가져오기)
        selectedYear = intent.getStringExtra("year");
        selectedDepartment = intent.getStringExtra("department");
        selectedTrack = intent.getStringExtra("track");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            courseList = intent.getParcelableArrayListExtra("courses", CourseInputActivity.Course.class);
            additionalRequirements = intent.getParcelableExtra("additionalRequirements", AdditionalRequirementsActivity.AdditionalRequirements.class);
        } else {
            courseList = intent.getParcelableArrayListExtra("courses");
            additionalRequirements = intent.getParcelableExtra("additionalRequirements");
        }

        if (selectedYear == null || selectedDepartment == null || selectedTrack == null || courseList == null) {
            Toast.makeText(this, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // additionalRequirements가 없는 경우 기본값 설정 (이전 버전 호환성)
        if (additionalRequirements == null) {
            Log.w(TAG, "AdditionalRequirements 데이터가 없습니다. 기본값으로 설정합니다.");
            additionalRequirements = new AdditionalRequirementsActivity.AdditionalRequirements(0, 0, false, false);
        }

        // Firebase에서 학부 설정 로드 (캐시에 저장)
        if (selectedDepartment != null) {
            DepartmentConfig.loadDepartmentConfigFromFirebase(selectedDepartment, FirebaseDataManager.getInstance());
        }
    }

    /**
     * Firestore에서 저장된 졸업요건 검사 결과 불러오기
     */
    private void loadSavedGraduationResult(String docId) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("graduation_check_history")
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "저장된 결과를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // 저장된 데이터 파싱
                    selectedYear = documentSnapshot.getString("year");
                    selectedDepartment = documentSnapshot.getString("department");
                    selectedTrack = documentSnapshot.getString("track");

                    // 과목 리스트 복원
                    java.util.List<java.util.Map<String, Object>> courseMaps =
                            (java.util.List<java.util.Map<String, Object>>) documentSnapshot.get("courses");
                    if (courseMaps != null) {
                        courseList = new java.util.ArrayList<>();
                        for (java.util.Map<String, Object> courseMap : courseMaps) {
                            String category = (String) courseMap.get("category");
                            String name = (String) courseMap.get("name");
                            int credits = ((Number) courseMap.get("credits")).intValue();
                            String groupId = (String) courseMap.get("groupId");
                            String competency = (String) courseMap.get("competency");

                            CourseInputActivity.Course course = new CourseInputActivity.Course(
                                    category, name, credits, groupId, competency
                            );
                            courseList.add(course);
                        }
                    }

                    // 추가 요구사항 복원
                    java.util.Map<String, Object> additionalReqMap =
                            (java.util.Map<String, Object>) documentSnapshot.get("additionalRequirements");
                    if (additionalReqMap != null) {
                        additionalRequirements = new AdditionalRequirementsActivity.AdditionalRequirements(
                                ((Number) additionalReqMap.getOrDefault("tlcCount", 0)).intValue(),
                                ((Number) additionalReqMap.getOrDefault("chapelCount", 0)).intValue(),
                                (Boolean) additionalReqMap.getOrDefault("mileageCompleted", false),
                                (Boolean) additionalReqMap.getOrDefault("extraGradCompleted", false)
                        );
                    } else {
                        additionalRequirements = new AdditionalRequirementsActivity.AdditionalRequirements(0, 0, false, false);
                    }

                    // 데이터 유효성 검사
                    if (selectedYear == null || selectedDepartment == null || selectedTrack == null || courseList == null) {
                        Toast.makeText(this, "저장된 데이터가 유효하지 않습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Firebase에서 학부 설정 로드
                    if (selectedDepartment != null) {
                        DepartmentConfig.loadDepartmentConfigFromFirebase(selectedDepartment, FirebaseDataManager.getInstance());
                    }

                    // UI 초기화 및 분석 수행
                    initViews();
                    setupToolbar();
                    performGraduationAnalysis();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "저장된 결과 불러오기 실패", e);
                    Toast.makeText(this, "저장된 결과를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void initViews() {
        textViewStudentInfo = findViewById(R.id.text_view_student_info);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        viewPager = findViewById(R.id.view_pager);
        toolbar = findViewById(R.id.toolbar_graduation_result);

        setupBottomNavigation();
    }

    private void setupTabs() {
        tabAdapter = new GraduationTabAdapter(this, selectedYear);
        viewPager.setAdapter(tabAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_overview) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_details) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_others) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        // ViewPager2의 페이지 변경에 따라 하단 네비게이션 업데이트
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigation.setSelectedItemId(R.id.nav_overview);
                        break;
                    case 1:
                        bottomNavigation.setSelectedItemId(R.id.nav_details);
                        break;
                    case 2:
                        bottomNavigation.setSelectedItemId(R.id.nav_others);
                        break;
                }
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("졸업 요건 분석 결과");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_graduation_result, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            // 저장 버튼 클릭 시 수동 저장 후 홈으로 이동
            saveGraduationCheckAndGoHome();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 졸업요건 검사 결과를 저장하고 홈으로 이동
     */
    private void saveGraduationCheckAndGoHome() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        long currentTime = System.currentTimeMillis();

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // 졸업분석 데이터 전체 저장
        Map<String, Object> graduationData = new HashMap<>();
        graduationData.put("checkedAt", currentTime);
        graduationData.put("year", selectedYear);
        graduationData.put("department", selectedDepartment);
        graduationData.put("track", selectedTrack);

        // 과목 리스트를 Map 형태로 변환
        java.util.List<java.util.Map<String, Object>> coursesData = new java.util.ArrayList<>();
        if (courseList != null) {
            for (Course course : courseList) {
                java.util.Map<String, Object> courseMap = new java.util.HashMap<>();
                courseMap.put("name", course.getName());
                courseMap.put("credits", course.getCredits());
                courseMap.put("category", course.getCategory());
                if (course.getGroupId() != null) {
                    courseMap.put("groupId", course.getGroupId());
                }
                if (course.getCompetency() != null) {
                    courseMap.put("competency", course.getCompetency());
                }
                coursesData.add(courseMap);
            }
        }
        graduationData.put("courses", coursesData);

        // 추가 요건 저장
        if (additionalRequirements != null) {
            java.util.Map<String, Object> reqMap = new java.util.HashMap<>();
            reqMap.put("tlcCount", additionalRequirements.getTlcCount());
            reqMap.put("chapelCount", additionalRequirements.getChapelCount());
            reqMap.put("mileageCompleted", additionalRequirements.isMileageCompleted());
            reqMap.put("extraGradCompleted", additionalRequirements.isExtraGradCompleted());
            graduationData.put("additionalRequirements", reqMap);
        }

        // 진행 중 메시지 표시
        Toast.makeText(this, "저장 중...", Toast.LENGTH_SHORT).show();

        // 1. graduation_check_history 컬렉션에 이력 저장
        db.collection("users")
                .document(userId)
                .collection("graduation_check_history")
                .add(graduationData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "졸업분석 이력 저장 성공: " + documentReference.getId());

                    // 2. users/{userId}/courses 서브컬렉션에 각 과목 저장
                    saveCourseToSubcollection(userId, coursesData);

                    // 3. users 문서에 savedGraduationAnalysis 필드와 lastGraduationCheckDate 업데이트
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("lastGraduationCheckDate", currentTime);
                    updateData.put("savedGraduationAnalysis", graduationData);
                    updateData.put("updatedAt", currentTime);

                    // 사용자 기본 정보도 함께 저장
                    if (currentUser.getDisplayName() != null) {
                        updateData.put("name", currentUser.getDisplayName());
                    }
                    if (currentUser.getEmail() != null) {
                        updateData.put("email", currentUser.getEmail());
                    }
                    updateData.put("studentYear", selectedYear);
                    updateData.put("department", selectedDepartment);
                    updateData.put("track", selectedTrack);

                    db.collection("users")
                            .document(userId)
                            .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "users 문서 업데이트 성공 (savedGraduationAnalysis, lastGraduationCheckDate, name, email)");
                            });

                    // 저장 완료 후 홈으로 이동
                    Toast.makeText(this, "졸업요건 검사 결과가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivityNew.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업분석 결과 저장 실패", e);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    private void saveGraduationAnalysisResult() {
        Toast.makeText(this, "분석이 완료되었습니다.", Toast.LENGTH_SHORT).show();

        // 메인화면으로 이동
        Intent intent = new Intent(this, MainActivityNew.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    private void performGraduationAnalysis() {
        // 학생 정보 표시
        String studentInfo = String.format("%s학번 %s %s", selectedYear, selectedDepartment, selectedTrack);
        textViewStudentInfo.setText(studentInfo);

        // Fragment에서 접근할 수 있도록 정적 필드에 설정
        staticSelectedYear = selectedYear;
        staticSelectedDepartment = selectedDepartment;
        staticAdditionalRequirements = additionalRequirements;
        staticCourseList = courseList;

        // 졸업 요건 설정 (하위 호환성 유지)
        graduationRequirements = new GraduationRequirements(selectedYear);

        Log.d(TAG, "========================================");
        Log.d(TAG, "통합 졸업요건 분석 시작");
        Log.d(TAG, "학번: " + selectedYear + ", 학과: " + selectedDepartment + ", 트랙: " + selectedTrack);
        Log.d(TAG, "입력 과목 수: " + courseList.size());
        Log.d(TAG, "========================================");

        // 새로운 통합 모델로 졸업요건 분석 수행
        FirebaseDataManager.getInstance().loadGraduationRules(
                selectedYear, selectedDepartment, selectedTrack,
                new FirebaseDataManager.OnGraduationRulesLoadedListener() {
                    @Override
                    public void onSuccess(sprout.app.sakmvp1.models.GraduationRules rules) {
                        Log.d(TAG, "졸업요건 데이터 로드 성공: " + rules.toString());

                        // 단일 분석 호출로 모든 졸업요건 분석
                        sprout.app.sakmvp1.models.GraduationAnalysisResult analysisResult = rules.analyze(courseList);

                        Log.d(TAG, "========================================");
                        Log.d(TAG, "분석 결과:");
                        Log.d(TAG, "총 학점: " + analysisResult.getTotalEarnedCredits() + "/" + analysisResult.getTotalRequiredCredits());
                        Log.d(TAG, "졸업 가능: " + analysisResult.isGraduationReady());
                        Log.d(TAG, "카테고리 수: " + analysisResult.getAllCategoryResults().size());
                        Log.d(TAG, "========================================");

                        // 분석 결과를 기존 형식으로 변환하여 Fragment에서 사용
                        convertAnalysisResultToLegacyFormat(analysisResult);

                        // UI 업데이트
                        setupTabs();
                        notifyUIUpdate();

                        // Firestore에 졸업요건 검사 이력 저장
                        saveGraduationCheckToFirestore();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "졸업요건 데이터 로드 실패", e);

                        // V2 통합 시스템만 사용 (V1 레거시 폴백 비활성화)
                        // V1 폴백 로직은 하단에 주석으로 보존됨
                        Log.e(TAG, "V2 통합 졸업요건 데이터가 없습니다. 관리자 화면에서 먼저 등록해주세요.");

                        runOnUiThread(() -> {
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(GraduationAnalysisResultActivity.this);
                            builder.setTitle("졸업요건 데이터 없음");
                            builder.setMessage("해당 학번/학과/트랙의 졸업요건 데이터가 없습니다.\n\n" +
                                    "관리자 화면에서 '졸업요건통합관리'를 통해\n" +
                                    "졸업요건 데이터를 먼저 등록해주세요.\n\n" +
                                    "학번: " + selectedYear + "\n" +
                                    "학과: " + selectedDepartment + "\n" +
                                    "트랙: " + selectedTrack);
                            builder.setPositiveButton("확인", (dialog, which) -> finish());
                            builder.setCancelable(false);
                            builder.show();
                        });

                        /* V1 레거시 폴백 (임시 비활성화)
                        Log.w(TAG, "기존 방식으로 폴백하여 분석 시도...");
                        performLegacyGraduationAnalysis();
                        */
                    }
                });
    }

    /**
     * 새로운 통합 분석 결과를 기존 형식으로 변환
     * Fragment들이 기존 정적 필드를 사용하므로 호환성 유지
     */
    private void convertAnalysisResultToLegacyFormat(sprout.app.sakmvp1.models.GraduationAnalysisResult result) {
        // CreditRequirements 변환 (초기값 0으로 생성 후 값 설정)
        creditRequirements = new FirebaseDataManager.CreditRequirements(0, 0, 0, 0, 0, 0, 0, 0, 0);
        creditRequirements.totalCredits = result.getTotalRequiredCredits();

        // 카테고리별 과목 목록 초기화 (null 방지)
        takenMajorRequiredCourses = new ArrayList<>();
        takenMajorElectiveCourses = new ArrayList<>();
        takenMajorAdvancedCourses = new ArrayList<>();
        takenDepartmentCommonCourses = new ArrayList<>();

        // 카테고리별 요구 학점 설정
        for (sprout.app.sakmvp1.models.CategoryAnalysisResult categoryResult : result.getAllCategoryResults()) {
            String categoryName = categoryResult.getCategoryName();
            int required = categoryResult.getRequiredCredits();

            switch (categoryName) {
                case "전공필수":
                    creditRequirements.majorRequired = required;
                    takenMajorRequiredCourses = new ArrayList<>(categoryResult.getCompletedCourses());
                    break;
                case "전공선택":
                    creditRequirements.majorElective = required;
                    takenMajorElectiveCourses = new ArrayList<>(categoryResult.getCompletedCourses());
                    break;
                case "교양필수":
                    creditRequirements.generalRequired = required;
                    break;
                case "교양선택":
                    creditRequirements.generalElective = required;
                    break;
                case "소양":
                    creditRequirements.liberalArts = required;
                    break;
                case "학부공통":
                    creditRequirements.departmentCommon = required;
                    takenDepartmentCommonCourses = new ArrayList<>(categoryResult.getCompletedCourses());
                    break;
                case "전공심화":
                    creditRequirements.majorAdvanced = required;
                    takenMajorAdvancedCourses = new ArrayList<>(categoryResult.getCompletedCourses());
                    break;
                case "일반선택":
                case "잔여학점":
                case "자율선택":
                    creditRequirements.freeElective = required;
                    break;
            }
        }

        // GraduationProgress 생성
        graduationProgress = new GraduationProgress();
        graduationProgress.totalEarned = result.getTotalEarnedCredits();
        graduationProgress.totalRequired = result.getTotalRequiredCredits();

        // 카테고리별 진행도 생성
        for (sprout.app.sakmvp1.models.CategoryAnalysisResult categoryResult : result.getAllCategoryResults()) {
            String categoryName = categoryResult.getCategoryName();
            CategoryProgress progress = new CategoryProgress(
                    categoryResult.getEarnedCredits(),
                    categoryResult.getRequiredCredits()
            );

            switch (categoryName) {
                case "전공필수":
                    graduationProgress.majorRequired = progress;
                    break;
                case "전공선택":
                    graduationProgress.majorElective = progress;
                    break;
                case "교양필수":
                    graduationProgress.generalRequired = progress;
                    break;
                case "교양선택":
                    graduationProgress.generalElective = progress;
                    break;
                case "소양":
                    graduationProgress.liberalArts = progress;
                    break;
                case "학부공통":
                    graduationProgress.departmentCommon = progress;
                    break;
                case "전공심화":
                    graduationProgress.majorAdvanced = progress;
                    break;
                case "일반선택":
                case "자율선택":
                    graduationProgress.generalSelection = progress;
                    break;
                case "잔여학점":
                    graduationProgress.remainingCredits = progress;
                    break;
            }
        }

        // 교양 분석 결과 변환
        generalEducationAnalysis = new GeneralEducationAnalysis();
        generalEducationAnalysis.oneOfGroupStatus = new HashMap<>();
        generalEducationAnalysis.individualRequiredStatus = new HashMap<>();
        generalEducationAnalysis.takenGeneralElective = new ArrayList<>();
        generalEducationAnalysis.takenLiberalArts = new ArrayList<>();

        // 교양필수 상세 분석 (oneOf 그룹 등)
        for (sprout.app.sakmvp1.models.CategoryAnalysisResult categoryResult : result.getAllCategoryResults()) {
            if ("교양필수".equals(categoryResult.getCategoryName())) {
                // oneOf 그룹 상태 변환
                for (sprout.app.sakmvp1.models.CategoryAnalysisResult.SubgroupResult subgroup : categoryResult.getSubgroupResults()) {
                    OneOfGroupStatus groupStatus = new OneOfGroupStatus();
                    groupStatus.groupName = subgroup.getGroupName();
                    // SubgroupResult의 availableCourses를 사용하여 선택 가능한 모든 과목 목록 채우기
                    groupStatus.requiredCourses = new ArrayList<>(subgroup.getAvailableCourses());
                    groupStatus.takenCourse = subgroup.getSelectedCourse();
                    groupStatus.isCompleted = subgroup.isCompleted();

                    generalEducationAnalysis.oneOfGroupStatus.put(subgroup.getGroupName(), groupStatus);
                }

                // 개별 필수 과목 상태
                for (String course : categoryResult.getCompletedCourses()) {
                    generalEducationAnalysis.individualRequiredStatus.put(course, true);
                }
                for (String course : categoryResult.getMissingCourses()) {
                    generalEducationAnalysis.individualRequiredStatus.put(course, false);
                }

                // V2 분석 결과에서 학점 정보를 courseCreditsMap에 추가
                if (categoryResult.getCourseCreditsMap() != null) {
                    for (Map.Entry<String, Integer> entry : categoryResult.getCourseCreditsMap().entrySet()) {
                        courseCreditsMap.put(entry.getKey(), entry.getValue());
                        Log.d(TAG, "교양필수 과목 학점 정보 추가: " + entry.getKey() + " = " + entry.getValue() + "학점");
                    }
                }
                break;
            }
        }

        // 교양선택 과목 수집
        for (CourseInputActivity.Course course : courseList) {
            if ("교양선택".equals(course.getCategory())) {
                generalEducationAnalysis.takenGeneralElective.add(course.getName());
            } else if ("소양".equals(course.getCategory())) {
                generalEducationAnalysis.takenLiberalArts.add(course.getName());
            }
        }

        // 역량 분석
        graduationProgress.competencyProgress = analyzeCompetencies();

        // 전공/학부공통 과목 목록 설정 (Fragment 상세 표시용)
        allMajorRequiredCourses = new ArrayList<>();
        allMajorElectiveCourses = new ArrayList<>();
        allMajorAdvancedCourses = new ArrayList<>();
        allDepartmentCommonCourses = new ArrayList<>();

        // V2 분석 결과에서 모든 과목 목록 가져오기 (완료 + 미이수)
        for (sprout.app.sakmvp1.models.CategoryAnalysisResult categoryResult : result.getAllCategoryResults()) {
            String categoryName = categoryResult.getCategoryName();
            List<String> allCoursesInCategory = new ArrayList<>();

            // 완료된 과목 + 미이수 과목 = 모든 과목
            allCoursesInCategory.addAll(categoryResult.getCompletedCourses());
            allCoursesInCategory.addAll(categoryResult.getMissingCourses());

            // 모든 카테고리의 학점 정보를 courseCreditsMap에 추가
            if (categoryResult.getCourseCreditsMap() != null) {
                for (Map.Entry<String, Integer> entry : categoryResult.getCourseCreditsMap().entrySet()) {
                    courseCreditsMap.put(entry.getKey(), entry.getValue());
                    Log.d(TAG, categoryName + " 과목 학점 정보 추가: " + entry.getKey() + " = " + entry.getValue() + "학점");
                }
            }

            switch (categoryName) {
                case "전공필수":
                    allMajorRequiredCourses.addAll(allCoursesInCategory);
                    break;
                case "전공선택":
                    allMajorElectiveCourses.addAll(allCoursesInCategory);
                    break;
                case "전공심화":
                    allMajorAdvancedCourses.addAll(allCoursesInCategory);
                    break;
                case "학부공통":
                    allDepartmentCommonCourses.addAll(allCoursesInCategory);
                    break;
            }
        }

        Log.d(TAG, "기존 형식으로 변환 완료");
        Log.d(TAG, "  - oneOf 그룹: " + generalEducationAnalysis.oneOfGroupStatus.size() + "개");
        Log.d(TAG, "  - 개별 필수: " + generalEducationAnalysis.individualRequiredStatus.size() + "개");
        Log.d(TAG, "  - 교양선택: " + generalEducationAnalysis.takenGeneralElective.size() + "개");
        Log.d(TAG, "  - 소양: " + generalEducationAnalysis.takenLiberalArts.size() + "개");
        Log.d(TAG, "  - 역량: " + (graduationProgress.competencyProgress != null ?
                     graduationProgress.competencyProgress.completedCompetencies.size() : 0) + "개");
    }

    /**
     * 기존 방식의 졸업요건 분석 (폴백용) - 임시 비활성화
     * 레거시: 별도 replacement_courses 컬렉션 로드 제거 (현재는 GraduationRules에 통합)
     *
     * 현재는 V2 통합 시스템만 사용합니다.
     * 이 메서드와 관련 V1 레거시 메서드들은 향후 완전 제거 예정입니다.
     *
     * @deprecated V2 통합 졸업요건 시스템을 사용하세요
     */
    @Deprecated
    private void performLegacyGraduationAnalysis() {
        Log.w(TAG, "performLegacyGraduationAnalysis() 호출됨 - 하지만 비활성화 상태");
        Log.w(TAG, "V2 통합 졸업요건 시스템을 사용해야 합니다.");

        // V1 레거시 로직 임시 비활성화
        // 아래 주석을 해제하면 기존 방식으로 동작합니다
        /*
        // 1단계: 전공필수, 전공선택, 학부공통 과목 목록 로드
        analyzeMajorRequiredCoursesForReplacementCalculation(() -> {
            // 2단계: 대체과목 로직을 적용하여 학점 계산
            Map<String, Integer> creditsByCategory = calculateCreditsByCategoryWithReplacements();

            // 3단계: Firebase에서 졸업이수학점 요건을 로드하고 진행도 계산
            loadCreditRequirements(creditsByCategory);

            // 4단계: 교양 과목 상세 분석
            analyzeGeneralEducationCourses();

            int totalCredits = 0;
            for (int credits : creditsByCategory.values()) {
                totalCredits += credits;
            }

            Log.d(TAG, "졸업 요건 분석 완료 (기존 방식) - 총 " + courseList.size() + "개 강의, " + totalCredits + "학점");
        });
        */
    }

    /**
     * V1 레거시 메서드
     * @deprecated V2 GraduationRules.analyze() 사용
     */
    @Deprecated
    private Map<String, Integer> calculateCreditsByCategory() {
        Map<String, Integer> creditsByCategory = new HashMap<>();

        for (CourseInputActivity.Course course : courseList) {
            String category = course.getCategory();
            int credits = course.getCredits();

            creditsByCategory.put(category, creditsByCategory.getOrDefault(category, 0) + credits);
        }

        return creditsByCategory;
    }

    /**
     * 대체과목 로직을 적용하여 카테고리별 학점을 계산하는 함수
     * 이 함수는 대체과목 데이터가 로드된 후에만 호출되어야 합니다.
     *
     * V1 레거시 메서드
     * @deprecated V2 GraduationRules.analyze() 사용
     */
    @Deprecated
    private Map<String, Integer> calculateCreditsByCategoryWithReplacements() {
        Map<String, Integer> creditsByCategory = new HashMap<>();

        // 먼저 기본 학점 계산 (사용자가 직접 수강한 과목들)
        for (CourseInputActivity.Course course : courseList) {
            String category = course.getCategory();
            int credits = course.getCredits();
            creditsByCategory.put(category, creditsByCategory.getOrDefault(category, 0) + credits);
        }

        Log.d(TAG, "calculateCreditsByCategoryWithReplacements: 기본 학점 계산 완료");
        Log.d(TAG, "  - 학부공통: " + creditsByCategory.getOrDefault("학부공통", 0) + "학점");
        Log.d(TAG, "  - 전공선택: " + creditsByCategory.getOrDefault("전공선택", 0) + "학점");

        // 수강한 과목 이름 리스트 생성
        List<String> takenCourseNames = new ArrayList<>();
        for (CourseInputActivity.Course course : courseList) {
            takenCourseNames.add(course.getName());
        }

        Log.d(TAG, "  - 수강 과목: " + takenCourseNames.toString());

        // 대체과목 맵을 순회하며 추가 학점 인정
        // replacementCoursesMap: Map<폐강된과목, List<대체가능과목>>
        for (Map.Entry<String, List<String>> entry : replacementCoursesMap.entrySet()) {
            String discontinuedCourse = entry.getKey();
            List<String> replacementCourses = entry.getValue();

            // 폐강된 과목을 직접 수강했는지 확인
            boolean directlyTaken = takenCourseNames.contains(discontinuedCourse);

            if (!directlyTaken) {
                // 직접 수강하지 않았다면, 대체 과목을 수강했는지 확인
                for (String replacementCourse : replacementCourses) {
                    if (takenCourseNames.contains(replacementCourse)) {
                        // 대체 과목을 수강했으면, 폐강된 과목의 학점을 추가
                        // 폐강된 과목의 카테고리와 학점을 Firebase에서 가져와야 함
                        // 현재는 courseCreditsMap에 저장되어 있음
                        Integer discontinuedCourseCredit = courseCreditsMap.get(discontinuedCourse);
                        if (discontinuedCourseCredit != null && discontinuedCourseCredit > 0) {
                            // 폐강된 과목의 카테고리 결정 (학부공통으로 가정)
                            // TODO: 실제로는 Firebase에서 해당 과목의 카테고리를 확인해야 함
                            String discontinuedCourseCategory = determineDiscontinuedCourseCategory(discontinuedCourse);

                            creditsByCategory.put(
                                    discontinuedCourseCategory,
                                    creditsByCategory.getOrDefault(discontinuedCourseCategory, 0) + discontinuedCourseCredit
                            );

                            Log.d(TAG, "✓ 대체과목 학점 추가: '" + discontinuedCourse + "' (" + discontinuedCourseCredit + "학점) ← '" +
                                    replacementCourse + "' 수강으로 인정 (카테고리: " + discontinuedCourseCategory + ")");
                        }
                        break; // 하나의 대체 과목만 인정
                    }
                }
            }
        }

        Log.d(TAG, "calculateCreditsByCategoryWithReplacements: 대체과목 적용 후 최종 학점");
        Log.d(TAG, "  - 학부공통: " + creditsByCategory.getOrDefault("학부공통", 0) + "학점");
        Log.d(TAG, "  - 전공선택: " + creditsByCategory.getOrDefault("전공선택", 0) + "학점");

        return creditsByCategory;
    }

    /**
     * 폐강된 과목의 카테고리를 결정하는 헬퍼 함수
     * allDepartmentCommonCourses, allMajorRequiredCourses 등의 리스트를 참조
     */
    private String determineDiscontinuedCourseCategory(String courseName) {
        // 학부공통 과목인지 확인
        if (allDepartmentCommonCourses != null && allDepartmentCommonCourses.contains(courseName)) {
            return "학부공통";
        }
        // 전공필수 과목인지 확인
        if (allMajorRequiredCourses != null && allMajorRequiredCourses.contains(courseName)) {
            return "전공필수";
        }
        // 전공선택 과목인지 확인
        if (allMajorElectiveCourses != null && allMajorElectiveCourses.contains(courseName)) {
            return "전공선택";
        }
        // 전공심화 과목인지 확인
        if (allMajorAdvancedCourses != null && allMajorAdvancedCourses.contains(courseName)) {
            return "전공심화";
        }

        // 기본값: 학부공통 (IT개론 등 초기 과목은 대부분 학부공통)
        Log.w(TAG, "determineDiscontinuedCourseCategory: '" + courseName + "' 카테고리를 찾을 수 없음, 학부공통으로 가정");
        return "학부공통";
    }


    private static boolean isOldCurriculum(String year) {
        return DepartmentConfig.isOldCurriculum(staticSelectedDepartment, year);
    }

    public static GraduationProgress getGraduationProgress() {
        return graduationProgress;
    }

    public static GraduationRequirements getGraduationRequirements() {
        return graduationRequirements;
    }

    public static List<String> getAllMajorRequiredCourses() {
        return allMajorRequiredCourses;
    }

    public static List<String> getTakenMajorRequiredCourses() {
        return takenMajorRequiredCourses;
    }

    public static List<String> getAllMajorElectiveCourses() {
        return allMajorElectiveCourses;
    }

    public static List<String> getTakenMajorElectiveCourses() {
        return takenMajorElectiveCourses;
    }

    public static List<String> getAllMajorAdvancedCourses() {
        return allMajorAdvancedCourses;
    }

    public static List<String> getTakenMajorAdvancedCourses() {
        return takenMajorAdvancedCourses;
    }

    public static List<String> getAllDepartmentCommonCourses() {
        return allDepartmentCommonCourses;
    }

    public static List<String> getTakenDepartmentCommonCourses() {
        return takenDepartmentCommonCourses;
    }

    public static GeneralEducationAnalysis getGeneralEducationAnalysis() {
        return generalEducationAnalysis;
    }

    public static FirebaseDataManager.CreditRequirements getCreditRequirements() {
        return creditRequirements;
    }

    /**
     * V1 레거시 메서드
     * @deprecated V2 GraduationRules.analyze() 사용
     */
    @Deprecated
    private void loadCreditRequirements(Map<String, Integer> creditsByCategory) {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();
        dataManager.loadCreditRequirements(selectedDepartment, selectedTrack, selectedYear,
                new FirebaseDataManager.OnCreditRequirementsLoadedListener() {
                    @Override
                    public void onSuccess(FirebaseDataManager.CreditRequirements requirements) {
                        // Firebase에서 로드된 학점 요구사항을 그대로 사용
                        creditRequirements = requirements;

                        Log.d(TAG, "졸업이수학점 요건 로드 완료 (" + selectedYear + "학번 교양필수 " + creditRequirements.generalRequired + "학점): " + creditRequirements.toString());

                        // Firebase 데이터 기반으로 진행도 계산
                        graduationProgress = calculateGraduationProgressWithRequirements(creditsByCategory, creditRequirements);

                        Log.d(TAG, "졸업 진행도 계산 완료 (Firebase 데이터 기반)");

                        // UI 업데이트 호출
                        notifyUIUpdate();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "졸업이수학점 요건 로드 실패", e);

                        // Firebase 데이터 로드 실패 시 사용자에게 알림
                        runOnUiThread(() -> {
                            Toast.makeText(GraduationAnalysisResultActivity.this,
                                "졸업 요건 데이터를 불러올 수 없습니다. 네트워크 상태를 확인해주세요.",
                                Toast.LENGTH_LONG).show();
                        });

                        // Firebase에서 데이터를 가져올 수 없으면 분석을 중단
                        Log.w(TAG, "Firebase 데이터 없이는 정확한 졸업 분석을 수행할 수 없습니다.");
                        finish();
                    }
                });
    }

    private void notifyUIUpdate() {
        Log.d(TAG, "notifyUIUpdate: Firebase 데이터 로드 완료 후 UI 업데이트 시작");

        // ViewPager의 어댑터에 데이터 변경 알림
        if (viewPager != null && viewPager.getAdapter() != null) {
            viewPager.getAdapter().notifyDataSetChanged();
            Log.d(TAG, "notifyUIUpdate: ViewPager 어댑터 업데이트 완료");
        }

        // 현재 탭의 UI를 직접 강제 업데이트
        runOnUiThread(() -> {
            Log.d(TAG, "notifyUIUpdate: UI 스레드에서 직접 업데이트 시작");
            updateCurrentTabUI();
        });
    }

    private void updateCurrentTabUI() {
        if (viewPager == null) return;

        Log.d(TAG, "updateCurrentTabUI: ViewPager 어댑터 완전 재생성");
        // 어댑터를 완전히 새로 생성해서 Fragment들이 최신 데이터로 다시 생성되도록 함
        GraduationTabAdapter newAdapter = new GraduationTabAdapter(this, selectedYear);
        viewPager.setAdapter(newAdapter);

        // 하단 네비게이션과 연결
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_overview) {
                    viewPager.setCurrentItem(0);
                    return true;
                } else if (itemId == R.id.nav_details) {
                    viewPager.setCurrentItem(1);
                    return true;
                } else if (itemId == R.id.nav_others) {
                    viewPager.setCurrentItem(2);
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * 졸업요건 검사 결과를 Firestore에 저장
     * users/{userId} 문서에 lastGraduationCheckDate 필드 업데이트
     */
    private void saveGraduationCheckToFirestore() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "saveGraduationCheckToFirestore: 로그인된 사용자가 없습니다.");
            return;
        }

        String userId = currentUser.getUid();
        long currentTime = System.currentTimeMillis();

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastGraduationCheckDate", currentTime);

        db.collection("users")
                .document(userId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "졸업요건 검사 이력 저장 성공: " + userId + " at " + currentTime);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업요건 검사 이력 저장 실패", e);
                    // 문서가 없는 경우 set 시도
                    Map<String, Object> data = new HashMap<>();
                    data.put("lastGraduationCheckDate", currentTime);
                    data.put("updatedAt", currentTime);

                    db.collection("users")
                            .document(userId)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "졸업요건 검사 이력 생성 성공: " + userId);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "졸업요건 검사 이력 생성 실패", e2);
                            });
                });
    }

    /**
     * 졸업요건 검사 결과를 Firestore에 저장하고 사용자에게 피드백 제공
     * users/{userId}/graduation_check_history 컬렉션에 저장
     */
    private void saveGraduationCheckToFirestoreWithFeedback() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "saveGraduationCheckToFirestoreWithFeedback: 로그인된 사용자가 없습니다.");
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        long currentTime = System.currentTimeMillis();

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // 졸업분석 데이터 전체 저장
        Map<String, Object> graduationData = new HashMap<>();
        graduationData.put("checkedAt", currentTime);
        graduationData.put("year", selectedYear);
        graduationData.put("department", selectedDepartment);
        graduationData.put("track", selectedTrack);

        // 과목 리스트를 Map 형태로 변환
        java.util.List<java.util.Map<String, Object>> coursesData = new java.util.ArrayList<>();
        if (courseList != null) {
            for (Course course : courseList) {
                java.util.Map<String, Object> courseMap = new java.util.HashMap<>();
                courseMap.put("name", course.getName());
                courseMap.put("credits", course.getCredits());
                courseMap.put("category", course.getCategory());
                if (course.getGroupId() != null) {
                    courseMap.put("groupId", course.getGroupId());
                }
                if (course.getCompetency() != null) {
                    courseMap.put("competency", course.getCompetency());
                }
                coursesData.add(courseMap);
            }
        }
        graduationData.put("courses", coursesData);

        // 추가 요건 저장
        if (additionalRequirements != null) {
            java.util.Map<String, Object> reqMap = new java.util.HashMap<>();
            reqMap.put("tlcCount", additionalRequirements.getTlcCount());
            reqMap.put("chapelCount", additionalRequirements.getChapelCount());
            reqMap.put("mileageCompleted", additionalRequirements.isMileageCompleted());
            reqMap.put("extraGradCompleted", additionalRequirements.isExtraGradCompleted());
            graduationData.put("additionalRequirements", reqMap);
        }

        // 진행 중 메시지 표시
        Toast.makeText(this, "저장 중...", Toast.LENGTH_SHORT).show();

        db.collection("users")
                .document(userId)
                .collection("graduation_check_history")
                .add(graduationData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "졸업분석 결과 저장 성공: " + documentReference.getId());
                    Toast.makeText(this, "졸업요건 검사 결과가 저장되었습니다.", Toast.LENGTH_SHORT).show();

                    // users 문서에도 lastGraduationCheckDate 업데이트
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("lastGraduationCheckDate", currentTime);
                    db.collection("users")
                            .document(userId)
                            .set(updateData, com.google.firebase.firestore.SetOptions.merge());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업분석 결과 저장 실패", e);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private GraduationProgress calculateGraduationProgressWithRequirements(
            Map<String, Integer> creditsByCategory, FirebaseDataManager.CreditRequirements creditReqs) {

        Log.d(TAG, "========================================");
        Log.d(TAG, "calculateGraduationProgressWithRequirements 시작");
        Log.d(TAG, "creditsByCategory 내용:");
        for (Map.Entry<String, Integer> entry : creditsByCategory.entrySet()) {
            Log.d(TAG, "  " + entry.getKey() + ": " + entry.getValue() + "학점");
        }
        Log.d(TAG, "creditReqs: " + creditReqs);
        Log.d(TAG, "========================================");

        GraduationProgress progress = new GraduationProgress();
        boolean isOldCurriculum = isOldCurriculum(selectedYear);

        // Firebase 데이터 기반으로 모든 카테고리 진행도 생성
        progress.majorRequired = new CategoryProgress(
            creditsByCategory.getOrDefault("전공필수", 0),
            creditReqs.majorRequired
        );
        progress.majorElective = new CategoryProgress(
            creditsByCategory.getOrDefault("전공선택", 0),
            creditReqs.majorElective
        );
        progress.generalRequired = new CategoryProgress(
            creditsByCategory.getOrDefault("교양필수", 0),
            creditReqs.generalRequired
        );
        progress.generalElective = new CategoryProgress(
            creditsByCategory.getOrDefault("교양선택", 0),
            creditReqs.generalElective
        );
        progress.liberalArts = new CategoryProgress(
            creditsByCategory.getOrDefault("소양", 0),
            creditReqs.liberalArts
        );

        // 학번에 따른 추가 카테고리 처리
        if (isOldCurriculum) {
            // 20-22학번: 학부공통, 일반선택 (자율선택과 동일)
            progress.departmentCommon = new CategoryProgress(
                creditsByCategory.getOrDefault("학부공통", 0),
                creditReqs.departmentCommon
            );
            progress.generalSelection = new CategoryProgress(
                creditsByCategory.getOrDefault("일반선택", 0),
                creditReqs.freeElective  // 자율선택과 동일한 값 사용
            );
        } else {
            // 23-25학번: 전공심화, 잔여학점(자율선택)
            progress.majorAdvanced = new CategoryProgress(
                creditsByCategory.getOrDefault("전공심화", 0),
                creditReqs.majorAdvanced
            );
            progress.remainingCredits = new CategoryProgress(
                creditsByCategory.getOrDefault("잔여학점", 0),
                creditReqs.freeElective
            );
        }

        // 넘치는 학점들을 잔여학점으로 이동 (20-22학번은 일반선택으로)
        int overflowCredits = calculateAndRedistributeOverflow(progress, isOldCurriculum);

        // 총 학점 계산
        progress.totalEarned = creditsByCategory.values().stream().mapToInt(Integer::intValue).sum();
        progress.totalRequired = creditReqs.totalCredits;

        // 교양선택 역량 분석
        progress.competencyProgress = analyzeCompetencies();

        String overflowDestination = DepartmentConfig.getOverflowDestination(selectedDepartment, selectedYear);
        Log.d(TAG, "넘침 학점 재분배 완료 - 총 " + overflowCredits + "학점이 " + overflowDestination + "으로 이동");

        return progress;
    }

    /**
     * 대체과목 학점 계산을 위해 과목 목록만 로드하는 버전
     * 콜백을 받아서 모든 과목 로드 완료 후 실행
     *
     * V1 레거시 메서드
     * @deprecated V2 GraduationRules.analyze() 사용
     */
    @Deprecated
    private void analyzeMajorRequiredCoursesForReplacementCalculation(Runnable onComplete) {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        // 초기화
        allMajorRequiredCourses = new ArrayList<>();
        allMajorElectiveCourses = new ArrayList<>();
        allMajorAdvancedCourses = new ArrayList<>();
        allDepartmentCommonCourses = new ArrayList<>();

        // 전공필수 과목 로드
        dataManager.loadMajorCourses(selectedDepartment, selectedTrack, selectedYear, "전공필수", new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "[대체과목계산용] 전공필수 과목 로드 성공: " + courses.size() + "개");
                allMajorRequiredCourses.clear();
                for (FirebaseDataManager.CourseInfo course : courses) {
                    allMajorRequiredCourses.add(course.getName());
                    courseCreditsMap.put(course.getName(), course.getCredits());
                }
                loadMajorElectiveCoursesForReplacement(onComplete);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "전공필수 과목 로드 실패: " + e.getMessage());
                Toast.makeText(GraduationAnalysisResultActivity.this,
                        "전공필수 과목 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadMajorElectiveCoursesForReplacement(Runnable onComplete) {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        dataManager.loadMajorCourses(selectedDepartment, selectedTrack, selectedYear, "전공선택", new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "[대체과목계산용] 전공선택 과목 로드 성공: " + courses.size() + "개");
                allMajorElectiveCourses.clear();
                for (FirebaseDataManager.CourseInfo course : courses) {
                    allMajorElectiveCourses.add(course.getName());
                    courseCreditsMap.put(course.getName(), course.getCredits());
                }
                loadDepartmentCommonCoursesForReplacement(onComplete);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "전공선택 과목 로드 실패: " + e.getMessage());
                Toast.makeText(GraduationAnalysisResultActivity.this,
                        "전공선택 과목 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadDepartmentCommonCoursesForReplacement(Runnable onComplete) {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(selectedDepartment, selectedYear);
        Log.d(TAG, "[대체과목계산용] 카테고리: " + categoryName);

        dataManager.loadDepartmentCommonCourses(selectedDepartment, selectedTrack, selectedYear, new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "[대체과목계산용] " + categoryName + " 과목 로드 성공: " + courses.size() + "개");
                if ("전공심화".equals(categoryName)) {
                    allMajorAdvancedCourses.clear();
                    for (FirebaseDataManager.CourseInfo course : courses) {
                        allMajorAdvancedCourses.add(course.getName());
                        courseCreditsMap.put(course.getName(), course.getCredits());
                    }
                } else {
                    allDepartmentCommonCourses.clear();
                    for (FirebaseDataManager.CourseInfo course : courses) {
                        allDepartmentCommonCourses.add(course.getName());
                        courseCreditsMap.put(course.getName(), course.getCredits());
                    }
                }
                // 모든 과목 로드 완료, 콜백 실행
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, categoryName + " 과목 로드 실패: " + e.getMessage());
                Toast.makeText(GraduationAnalysisResultActivity.this,
                        categoryName + " 과목 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void analyzeMajorRequiredCourses() {
        // Firebase에서 학번, 학과, 트랙에 맞는 과목 데이터 로드
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        // 초기화
        allMajorRequiredCourses = new ArrayList<>();
        allMajorElectiveCourses = new ArrayList<>();
        allMajorAdvancedCourses = new ArrayList<>();
        allDepartmentCommonCourses = new ArrayList<>();

        // 전공필수 과목 로드
        dataManager.loadMajorCourses(selectedDepartment, selectedTrack, selectedYear, "전공필수", new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "전공필수 과목 로드 성공: " + courses.size() + "개");
                allMajorRequiredCourses.clear();
                for (FirebaseDataManager.CourseInfo course : courses) {
                    allMajorRequiredCourses.add(course.getName());
                    // 전공필수 과목의 학점 정보도 저장
                    courseCreditsMap.put(course.getName(), course.getCredits());
                }
                loadMajorElectiveCourses(); // 다음 단계 진행
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "전공필수 과목 로드 실패: " + e.getMessage());
                Toast.makeText(GraduationAnalysisResultActivity.this,
                    "전공필수 과목 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadMajorElectiveCourses() {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        // 전공선택 과목 로드
        dataManager.loadMajorCourses(selectedDepartment, selectedTrack, selectedYear, "전공선택", new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "전공선택 과목 로드 성공: " + courses.size() + "개");
                allMajorElectiveCourses.clear();
                for (FirebaseDataManager.CourseInfo course : courses) {
                    allMajorElectiveCourses.add(course.getName());
                    // 전공선택 과목의 학점 정보도 저장
                    courseCreditsMap.put(course.getName(), course.getCredits());
                }
                loadMajorAdvancedOrDepartmentCommonCourses(); // 다음 단계 진행
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "전공선택 과목 로드 실패: " + e.getMessage());
                Toast.makeText(GraduationAnalysisResultActivity.this,
                    "전공선택 과목 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadMajorAdvancedOrDepartmentCommonCourses() {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        // DepartmentConfig를 사용하여 학부별 카테고리 결정
        String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(selectedDepartment, selectedYear);
        Log.d(TAG, "학부: " + selectedDepartment + ", 학번: " + selectedYear + " -> 카테고리: " + categoryName);

        if ("전공심화".equals(categoryName)) {
            // 전공심화 과목 로드 - loadDepartmentCommonCourses 사용 (강의 입력과 동일한 메서드)
            dataManager.loadDepartmentCommonCourses(selectedDepartment, selectedTrack, selectedYear, new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                @Override
                public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                    Log.d(TAG, "전공심화 과목 로드 성공: " + courses.size() + "개");
                    allMajorAdvancedCourses.clear();
                    for (FirebaseDataManager.CourseInfo course : courses) {
                        allMajorAdvancedCourses.add(course.getName());
                        // 전공심화 과목의 학점 정보도 저장
                        courseCreditsMap.put(course.getName(), course.getCredits());
                    }
                    analyzeTakenCourses(); // 마지막 단계 진행
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "전공심화 과목 로드 실패: " + e.getMessage());
                    Toast.makeText(GraduationAnalysisResultActivity.this,
                        "전공심화 과목 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            // 학부공통 과목 로드
            dataManager.loadDepartmentCommonCourses(selectedDepartment, selectedTrack, selectedYear, new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                @Override
                public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                    Log.d(TAG, "학부공통 과목 로드 성공: " + courses.size() + "개");
                    allDepartmentCommonCourses.clear();
                    for (FirebaseDataManager.CourseInfo course : courses) {
                        allDepartmentCommonCourses.add(course.getName());
                        // 학부공통 과목의 학점 정보도 저장
                        courseCreditsMap.put(course.getName(), course.getCredits());
                    }
                    analyzeTakenCourses(); // 마지막 단계 진행
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "학부공통 과목 로드 실패: " + e.getMessage());
                    Toast.makeText(GraduationAnalysisResultActivity.this,
                        "학부공통 과목 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }





    private void analyzeTakenCourses() {

        // 사용자가 수강한 전공필수 과목 확인
        takenMajorRequiredCourses = new ArrayList<>();
        takenMajorElectiveCourses = new ArrayList<>();
        takenMajorAdvancedCourses = new ArrayList<>();
        takenDepartmentCommonCourses = new ArrayList<>();

        for (CourseInputActivity.Course course : courseList) {
            if ("전공필수".equals(course.getCategory())) {
                takenMajorRequiredCourses.add(course.getName());
            } else if ("전공선택".equals(course.getCategory())) {
                takenMajorElectiveCourses.add(course.getName());
            } else if ("전공심화".equals(course.getCategory())) {
                takenMajorAdvancedCourses.add(course.getName());
            } else if ("학부공통".equals(course.getCategory())) {
                takenDepartmentCommonCourses.add(course.getName());
            }
        }

        Log.d(TAG, "전공필수 분석 - 전체: " + allMajorRequiredCourses.size() + "과목, 수강: " + takenMajorRequiredCourses.size() + "과목");

        // 모든 데이터 로딩이 완료되면 UI 업데이트
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setupTabs();
            }
        });
    }

    private CompetencyProgress analyzeCompetencies() {
        Set<String> completedCompetencies = new HashSet<>();

        // 교양선택 과목들의 역량 정보 수집
        for (CourseInputActivity.Course course : courseList) {
            if ("교양선택".equals(course.getCategory()) && course.getCompetency() != null) {
                completedCompetencies.add(course.getCompetency());
            }
        }

        Log.d(TAG, "역량 분석 완료 - 완료된 역량: " + completedCompetencies.toString());
        return new CompetencyProgress(completedCompetencies);
    }

    private int calculateAndRedistributeOverflow(GraduationProgress progress, boolean isOldCurriculum) {
        int totalOverflow = 0;

        // 각 카테고리에서 넘치는 학점 계산
        int majorRequiredOverflow = Math.max(0, progress.majorRequired.earned - progress.majorRequired.required);
        int majorElectiveOverflow = Math.max(0, progress.majorElective.earned - progress.majorElective.required);
        int generalRequiredOverflow = Math.max(0, progress.generalRequired.earned - progress.generalRequired.required);
        int generalElectiveOverflow = progress.generalElective != null ? Math.max(0, progress.generalElective.earned - progress.generalElective.required) : 0;
        int liberalArtsOverflow = progress.liberalArts != null ? Math.max(0, progress.liberalArts.earned - progress.liberalArts.required) : 0;

        totalOverflow = majorRequiredOverflow + majorElectiveOverflow + generalRequiredOverflow +
                       generalElectiveOverflow + liberalArtsOverflow;

        // DepartmentConfig 기반 추가 카테고리 넘침 계산
        boolean usesOldCurriculum = DepartmentConfig.isOldCurriculum(staticSelectedDepartment, staticSelectedYear);
        if (usesOldCurriculum) {
            // 학부공통 사용하는 경우: 학부공통에서 넘치는 학점도 계산
            if (progress.departmentCommon != null) {
                int departmentCommonOverflow = Math.max(0, progress.departmentCommon.earned - progress.departmentCommon.required);
                totalOverflow += departmentCommonOverflow;
                Log.d(TAG, "학부공통 넘침: " + departmentCommonOverflow + "학점");
            }
        } else {
            // 전공심화 사용하는 경우: 전공심화에서 넘치는 학점도 계산
            if (progress.majorAdvanced != null) {
                int majorAdvancedOverflow = Math.max(0, progress.majorAdvanced.earned - progress.majorAdvanced.required);
                totalOverflow += majorAdvancedOverflow;
                Log.d(TAG, "전공심화 넘침: " + majorAdvancedOverflow + "학점");
            }
        }

        // 넘치는 학점들을 DepartmentConfig 기반 목적지로 이동
        if (totalOverflow > 0) {
            String destination = DepartmentConfig.getOverflowDestination(staticSelectedDepartment, staticSelectedYear);
            if ("일반선택".equals(destination)) {
                // 일반선택으로 이동
                if (progress.generalSelection != null) {
                    int newGeneralSelectionEarned = progress.generalSelection.earned + totalOverflow;
                    progress.generalSelection = new CategoryProgress(newGeneralSelectionEarned, progress.generalSelection.required);
                    Log.d(TAG, "일반선택에 " + totalOverflow + "학점 추가 - 새 진행도: " +
                          progress.generalSelection.earned + "/" + progress.generalSelection.required);
                }
            } else {
                // 잔여학점으로 이동
                if (progress.remainingCredits != null) {
                    int newRemainingCreditsEarned = progress.remainingCredits.earned + totalOverflow;
                    progress.remainingCredits = new CategoryProgress(newRemainingCreditsEarned, progress.remainingCredits.required);
                    Log.d(TAG, "잔여학점에 " + totalOverflow + "학점 추가 - 새 진행도: " +
                          progress.remainingCredits.earned + "/" + progress.remainingCredits.required);
                }
            }

            // 원래 카테고리들의 넘침 부분을 제거 (earned를 required로 제한)
            progress.majorRequired = new CategoryProgress(
                Math.min(progress.majorRequired.earned, progress.majorRequired.required),
                progress.majorRequired.required
            );

            progress.majorElective = new CategoryProgress(
                Math.min(progress.majorElective.earned, progress.majorElective.required),
                progress.majorElective.required
            );

            progress.generalRequired = new CategoryProgress(
                Math.min(progress.generalRequired.earned, progress.generalRequired.required),
                progress.generalRequired.required
            );

            if (progress.generalElective != null) {
                progress.generalElective = new CategoryProgress(
                    Math.min(progress.generalElective.earned, progress.generalElective.required),
                    progress.generalElective.required
                );
            }

            if (progress.liberalArts != null) {
                progress.liberalArts = new CategoryProgress(
                    Math.min(progress.liberalArts.earned, progress.liberalArts.required),
                    progress.liberalArts.required
                );
            }

            if (usesOldCurriculum && progress.departmentCommon != null) {
                progress.departmentCommon = new CategoryProgress(
                    Math.min(progress.departmentCommon.earned, progress.departmentCommon.required),
                    progress.departmentCommon.required
                );
            } else if (!usesOldCurriculum && progress.majorAdvanced != null) {
                progress.majorAdvanced = new CategoryProgress(
                    Math.min(progress.majorAdvanced.earned, progress.majorAdvanced.required),
                    progress.majorAdvanced.required
                );
            }

            Log.d(TAG, "넘침 학점 세부내역 - 전공필수:" + majorRequiredOverflow + ", 전공선택:" + majorElectiveOverflow +
                      ", 교양필수:" + generalRequiredOverflow + ", 교양선택:" + generalElectiveOverflow +
                      ", 소양:" + liberalArtsOverflow);
        }

        return totalOverflow;
    }

    private void analyzeGeneralEducationCourses() {
        generalEducationAnalysis = new GeneralEducationAnalysis();

        // 사용자가 수강한 교양 과목들 분류
        List<String> takenGeneralRequired = new ArrayList<>();
        List<String> takenGeneralElective = new ArrayList<>();
        List<String> takenLiberalArts = new ArrayList<>();

        for (CourseInputActivity.Course course : courseList) {
            String category = course.getCategory();
            String courseName = course.getName();

            if ("교양필수".equals(category)) {
                takenGeneralRequired.add(courseName);
            } else if ("교양선택".equals(category)) {
                takenGeneralElective.add(courseName);
            } else if ("소양".equals(category)) {
                takenLiberalArts.add(courseName);
            }
        }

        generalEducationAnalysis.takenGeneralElective = takenGeneralElective;
        generalEducationAnalysis.takenLiberalArts = takenLiberalArts;

        Log.d(TAG, "교양 분석 완료 - 교양필수: " + takenGeneralRequired.size() + "과목, 교양선택: " + takenGeneralElective.size() + "과목, 소양: " + takenLiberalArts.size() + "과목");

        // 교양 과목 학점 정보 로드 (교양필수가 있을 때만)
        if (!takenGeneralRequired.isEmpty()) {
            loadGeneralEducationCredits(takenGeneralRequired);
        }

        // 교양그룹 분석은 사용자 입력 유무와 관계없이 항상 실행
        analyzeGeneralRequiredGroups(takenGeneralRequired);
    }

    private void loadGeneralEducationCredits(List<String> takenGeneralRequired) {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        // 교양필수 과목 학점 정보 로드
        dataManager.loadGeneralEducationCourses(selectedDepartment, selectedTrack, selectedYear, "교양필수", new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "교양필수 과목 학점 정보 로드 성공: " + courses.size() + "개");
                for (FirebaseDataManager.CourseInfo course : courses) {
                    courseCreditsMap.put(course.getName(), course.getCredits());
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "교양필수 과목 학점 정보 로드 실패: " + e.getMessage());
                // 실패 시에도 계속 진행
            }
        });
    }

    private void loadAllGeneralEducationCredits() {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        // 모든 교양필수 과목들의 학점 정보 로드 (사용자 수강 여부와 관계없이)
        dataManager.loadGeneralEducationCourses(selectedDepartment, selectedTrack, selectedYear, "교양필수", new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "모든 교양필수 과목 학점 정보 로드 성공: " + courses.size() + "개");
                for (FirebaseDataManager.CourseInfo course : courses) {
                    courseCreditsMap.put(course.getName(), course.getCredits());
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "모든 교양필수 과목 학점 정보 로드 실패: " + e.getMessage());
                // 실패 시에도 계속 진행
            }
        });
    }

    private void analyzeGeneralRequiredGroups(List<String> takenCourses) {
        // Firebase에서 교양교육 그룹 정보 로드
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();
        dataManager.loadGeneralEducationGroups(selectedDepartment, selectedYear, new FirebaseDataManager.OnGeneralEducationGroupsLoadedListener() {
            @Override
            public void onSuccess(Map<String, List<String>> oneOfGroups, List<String> individualRequired) {
                Log.d(TAG, "교양교육 그룹 로드 성공: " + oneOfGroups.size() + "개 그룹, " + individualRequired.size() + "개 개별 필수");
                analyzeGroupsWithData(takenCourses, oneOfGroups, individualRequired);

                // 교양필수 과목들의 학점 정보도 로드 (사용자 수강 여부와 관계없이)
                loadAllGeneralEducationCredits();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "교양교육 그룹 로드 실패: " + e.getMessage());
                Toast.makeText(GraduationAnalysisResultActivity.this,
                    "교양교육 그룹 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void analyzeGroupsWithData(List<String> takenCourses, Map<String, List<String>> oneOfGroups, List<String> individualRequired) {
        // 먼저 모든 교양필수 과목들의 학점 정보를 courseCreditsMap에 저장
        storeGeneralEducationCredits(oneOfGroups, individualRequired);

        // 그룹별 이수 상태 분석
        generalEducationAnalysis.oneOfGroupStatus = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : oneOfGroups.entrySet()) {
            String groupName = entry.getKey();
            List<String> groupCourses = entry.getValue();

            String takenCourse = null;
            for (String course : groupCourses) {
                if (isCourseCompleted(course, takenCourses)) {
                    // 대체과목으로 이수한 경우 실제 수강한 과목명 가져오기
                    String replacementTaken = getReplacementCourseTaken(course, takenCourses);
                    takenCourse = replacementTaken != null ? replacementTaken : course;
                    break;
                }
            }

            OneOfGroupStatus status = new OneOfGroupStatus();
            status.groupName = getGroupDisplayName(groupName, groupCourses); // 과목 기반 동적 그룹명 생성
            status.requiredCourses = new ArrayList<>(groupCourses);
            status.takenCourse = takenCourse;
            status.isCompleted = takenCourse != null;

            generalEducationAnalysis.oneOfGroupStatus.put(groupName, status);
        }

        // 개별 필수 과목 상태 분석
        generalEducationAnalysis.individualRequiredStatus = new HashMap<>();
        for (String course : individualRequired) {
            boolean isTaken = isCourseCompleted(course, takenCourses);
            generalEducationAnalysis.individualRequiredStatus.put(course, isTaken);
        }
    }

    /**
     * 교양필수 과목들의 학점 정보를 courseCreditsMap에 저장
     */
    private void storeGeneralEducationCredits(Map<String, List<String>> oneOfGroups, List<String> individualRequired) {
        FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

        dataManager.loadGeneralEducationCourses(selectedDepartment, selectedTrack, selectedYear, "교양필수", new FirebaseDataManager.OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                Log.d(TAG, "교양필수 과목들의 학점 정보 courseCreditsMap에 저장 시작: " + courses.size() + "개");
                for (FirebaseDataManager.CourseInfo course : courses) {
                    courseCreditsMap.put(course.getName(), course.getCredits());
                    Log.d(TAG, "교양필수 과목 학점 저장: " + course.getName() + " = " + course.getCredits() + "학점");
                }
                Log.d(TAG, "교양필수 과목들의 학점 정보 courseCreditsMap에 저장 완료");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "교양필수 과목들의 학점 정보 저장 실패: " + e.getMessage());
            }
        });
    }

    /**
     * oneofgroup 그룹명을 과목 내용 기반으로 동적 생성
     * 과목명을 분석하여 적절한 카테고리명을 자동으로 부여
     */
    private String getGroupDisplayName(String groupName, List<String> courses) {
        if (courses == null || courses.isEmpty()) {
            return groupName; // 과목이 없으면 원본 그룹명 반환
        }

        // 과목명을 분석하여 카테고리 결정
        String firstCourse = courses.get(0);

        // 생애설계/직업진로 관련 과목
        if (containsAnyKeyword(courses, "생애설계", "직업진로", "취업", "창업")) {
            return "학습혁신 그룹";
        }

        // 논리/사고/글쓰기 관련 과목
        if (containsAnyKeyword(courses, "논리", "사고", "글쓰기", "비판적")) {
            return "사고와표현 그룹";
        }

        // 기독교 관련 과목
        if (containsAnyKeyword(courses, "성서", "하나님", "기독교", "인물로")) {
            return "기독교적 공동체 그룹";
        }

        // 장애/다문화 관련 과목
        if (containsAnyKeyword(courses, "장애인", "다문화", "자립생활")) {
            return "장애공감 그룹";
        }

        // 영어 관련 과목
        if (containsAnyKeyword(courses, "English", "Practical")) {
            return "영어교육 그룹";
        }

        // 컴퓨터/정보 관련 과목
        if (containsAnyKeyword(courses, "컴퓨터", "정보사회", "정보")) {
            return "정보교육 그룹";
        }

        // 일반적인 이름 생성 (첫 번째 과목명 기반)
        if (firstCourse.length() > 3) {
            return firstCourse.substring(0, 3) + " 관련 그룹";
        }

        return groupName; // 기본값
    }

    /**
     * 과목 리스트에서 특정 키워드들 중 하나라도 포함하는지 확인
     */
    private boolean containsAnyKeyword(List<String> courses, String... keywords) {
        for (String course : courses) {
            for (String keyword : keywords) {
                if (course.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 졸업 요건 클래스
    public static class GraduationRequirements {
        public final boolean isOldCurriculum;
        public final int totalRequired;

        public GraduationRequirements(String year) {
            this.isOldCurriculum = isOldCurriculum(year);
            this.totalRequired = creditRequirements != null ? creditRequirements.totalCredits : 0;
        }

        private boolean isOldCurriculum(String year) {
            return DepartmentConfig.isOldCurriculum(
                GraduationAnalysisResultActivity.staticSelectedDepartment, year);
        }
    }

    // 카테고리별 진행도 클래스
    public static class CategoryProgress {
        public final int earned;
        public final int required;
        public final float percentage;
        public final int remaining;
        public final boolean isCompleted;

        public CategoryProgress(int earned, int required) {
            this.earned = earned;
            this.required = required;
            this.percentage = required > 0 ? (float) earned / required * 100 : 0;
            this.remaining = Math.max(0, required - earned);
            this.isCompleted = earned >= required;
        }
    }

    // 교양선택 역량 진행도 클래스
    public static class CompetencyProgress {
        public final Set<String> completedCompetencies; // 완료된 역량들 (1역량, 2역량, 3역량, 4역량, 5역량 중)
        public final int requiredCompetencyCount = 3; // 필요한 역량 수 (5개 중 3개)
        public final boolean isCompleted;

        public CompetencyProgress(Set<String> completedCompetencies) {
            this.completedCompetencies = new HashSet<>(completedCompetencies);
            // "소양"은 별도 카테고리이므로 역량 카운트에서 제외
            this.completedCompetencies.remove("소양");
            this.isCompleted = this.completedCompetencies.size() >= requiredCompetencyCount;
        }

        public int getCompletedCount() {
            return completedCompetencies.size();
        }

        public int getRemainingCount() {
            return Math.max(0, requiredCompetencyCount - getCompletedCount());
        }

        public String getCompletedCompetenciesText() {
            if (completedCompetencies.isEmpty()) {
                return "없음";
            }
            return String.join(", ", completedCompetencies);
        }
    }

    // 전체 졸업 진행도 클래스
    public static class GraduationProgress {
        public CategoryProgress departmentCommon;
        public CategoryProgress majorRequired;
        public CategoryProgress majorElective;
        public CategoryProgress majorAdvanced;
        public CategoryProgress generalRequired;
        public CategoryProgress generalElective;
        public CategoryProgress liberalArts;
        public CategoryProgress generalSelection;
        public CategoryProgress remainingCredits;

        // 교양선택 역량 추적 정보
        public CompetencyProgress competencyProgress;

        public int totalEarned;
        public int totalRequired = 130;

        public float getOverallProgress() {
            return totalRequired > 0 ? (float) totalEarned / totalRequired * 100 : 0;
        }

        public int getTotalRemaining() {
            return Math.max(0, totalRequired - totalEarned);
        }
    }

    // 교양교육 분석 클래스
    public static class GeneralEducationAnalysis {
        public Map<String, OneOfGroupStatus> oneOfGroupStatus;
        public Map<String, Boolean> individualRequiredStatus;
        public List<String> takenGeneralElective;
        public List<String> takenLiberalArts;

        public int getCompletedGroupsCount() {
            if (oneOfGroupStatus == null) return 0;
            int count = 0;
            for (OneOfGroupStatus status : oneOfGroupStatus.values()) {
                if (status.isCompleted) count++;
            }
            return count;
        }

        public int getCompletedIndividualCount() {
            if (individualRequiredStatus == null) return 0;
            int count = 0;
            for (Boolean completed : individualRequiredStatus.values()) {
                if (completed) count++;
            }
            return count;
        }
    }

    // oneOf 그룹 상태 클래스
    public static class OneOfGroupStatus {
        public String groupName;
        public List<String> requiredCourses;
        public String takenCourse;
        public boolean isCompleted;
    }

    // 탭 어댑터
    private static class GraduationTabAdapter extends FragmentStateAdapter {
        private String selectedYear;

        public GraduationTabAdapter(@NonNull FragmentActivity fragmentActivity, String year) {
            super(fragmentActivity);
            this.selectedYear = year;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return OverviewTabFragment.newInstance(selectedYear);
                case 1:
                    return new DetailsTabFragment();
                case 2:
                    return new OthersTabFragment();
                default:
                    return OverviewTabFragment.newInstance(selectedYear);
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    // 전체 탭 프래그먼트
    public static class OverviewTabFragment extends Fragment {
        private static String selectedYear;

        public static OverviewTabFragment newInstance(String year) {
            OverviewTabFragment fragment = new OverviewTabFragment();
            selectedYear = year;
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // 학번에 따라 다른 레이아웃 사용
            boolean shouldUseOldLayout = DepartmentConfig.shouldUseOldLayout(
                GraduationAnalysisResultActivity.staticSelectedDepartment,
                selectedYear);
            int layoutResource = shouldUseOldLayout ? R.layout.tab_overview_old : R.layout.tab_overview;

            View view = inflater.inflate(layoutResource, container, false);

            // 실제 데이터로 UI 업데이트
            updateOverviewUI(view);

            return view;
        }

        private void updateOverviewUI(View view) {
            GraduationProgress progress = getGraduationProgress();
            if (progress == null) {
                Log.w(TAG, "updateOverviewUI: GraduationProgress is null");
                return;
            }

            Log.d(TAG, "updateOverviewUI: 학부공통=" + (progress.departmentCommon != null ?
                progress.departmentCommon.earned + "/" + progress.departmentCommon.required : "null"));
            Log.d(TAG, "updateOverviewUI: 전공심화=" + (progress.majorAdvanced != null ?
                progress.majorAdvanced.earned + "/" + progress.majorAdvanced.required : "null"));

            // 도넛 차트 설정
            DonutChartView donutChart = view.findViewById(R.id.donut_chart_overall);
            if (donutChart != null) {
                donutChart.setProgress(progress.getOverallProgress());
                donutChart.setStrokeWidth(16f);
            }

            // 전체 진행률 텍스트 업데이트
            TextView percentageText = view.findViewById(R.id.text_overall_percentage);
            if (percentageText != null) {
                percentageText.setText(String.format("%.0f%%", progress.getOverallProgress()));
            }

            // 총 학점 텍스트 업데이트 (실제 졸업이수학점 반영)
            TextView totalCreditsText = view.findViewById(R.id.text_total_credits);
            if (totalCreditsText != null) {
                FirebaseDataManager.CreditRequirements creditReqs = getCreditRequirements();
                int requiredCredits = (creditReqs != null) ? creditReqs.totalCredits : progress.totalRequired;
                totalCreditsText.setText(String.format("%d / %d 학점", progress.totalEarned, requiredCredits));
            }

            // 졸업이수학점 정보 표시
            displayCreditRequirements(view);

            // 카테고리별 진행도 업데이트
            updateCategoryProgress(view, progress);
        }

        private void displayCreditRequirements(View view) {
            FirebaseDataManager.CreditRequirements creditReqs = getCreditRequirements();
            if (creditReqs == null) return;

            // 졸업이수학점 정보는 각 카테고리별 진행도에 반영되어 표시됨
            Log.d(TAG, "졸업이수학점 정보 적용 완료: " + creditReqs.toString());
        }

        private void updateCategoryProgress(View view, GraduationProgress progress) {
            boolean isOld = isOldCurriculum(selectedYear);

            if (isOld) {
                updateCategoryUI(view, "department_common", progress.departmentCommon);
                updateCategoryUI(view, "general_selection", progress.generalSelection);
            } else {
                updateCategoryUI(view, "major_advanced", progress.majorAdvanced);
                updateCategoryUI(view, "remaining_credits", progress.remainingCredits);
            }

            updateCategoryUI(view, "major_required", progress.majorRequired);
            updateCategoryUI(view, "major_elective", progress.majorElective);
            updateCategoryUI(view, "general_required", progress.generalRequired);
            updateCategoryUI(view, "general_elective", progress.generalElective);
            updateCategoryUI(view, "liberal_arts", progress.liberalArts);
        }

        private void updateCategoryUI(View view, String category, CategoryProgress progress) {
            if (progress == null) {
                Log.w(TAG, "updateCategoryUI: CategoryProgress is null for " + category);
                return;
            }

            Log.d(TAG, "updateCategoryUI: " + category + " = " + progress.earned + "/" + progress.required + " 학점");

            // 요약 텍스트 업데이트
            int summaryId = getResources().getIdentifier("text_" + category + "_summary", "id", requireContext().getPackageName());
            TextView summaryText = view.findViewById(summaryId);
            if (summaryText != null) {
                String newText = String.format("%d/%d 학점", progress.earned, progress.required);
                summaryText.setText(newText);
                Log.d(TAG, "updateCategoryUI: Updated " + category + " summary to: " + newText);
            } else {
                Log.w(TAG, "updateCategoryUI: TextView not found for " + category + "_summary (ID: " + summaryId + ")");
            }

            // 상태 텍스트 업데이트
            int statusId = getResources().getIdentifier("text_" + category + "_status", "id", requireContext().getPackageName());
            TextView statusText = view.findViewById(statusId);
            if (statusText != null) {
                if (progress.isCompleted) {
                    statusText.setText("완료");
                    statusText.setTextColor(0xFF4CAF50); // Green
                } else {
                    statusText.setText(progress.remaining + "학점 부족");
                    statusText.setTextColor(0xFFFF5722); // Red/Orange
                }
            }

            // 진행바 업데이트
            int progressId = getResources().getIdentifier("progress_" + category, "id", requireContext().getPackageName());
            android.widget.ProgressBar progressBar = view.findViewById(progressId);
            if (progressBar != null) {
                progressBar.setProgress((int) progress.percentage);
            }
        }

        private boolean isOldCurriculum(String year) {
            return DepartmentConfig.isOldCurriculum(
                GraduationAnalysisResultActivity.staticSelectedDepartment, year);
        }
    }

    // 세부 탭 프래그먼트
    public static class DetailsTabFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tab_details, container, false);
            setupAccordions(view);
            updateMajorRequiredDetails(view);
            updateMajorElectiveDetails(view);
            updateMajorAdvancedDetails(view);
            updateDepartmentCommonDetails(view);
            updateGeneralEducationDetails(view);
            return view;
        }

        private void setupAccordions(View view) {
            setupAccordion(view, R.id.accordion_major_required_header, R.id.accordion_major_required_content, R.id.accordion_major_required_icon);
            setupAccordion(view, R.id.accordion_major_elective_header, R.id.accordion_major_elective_content, R.id.accordion_major_elective_icon);
            setupAccordion(view, R.id.accordion_major_advanced_header, R.id.accordion_major_advanced_content, R.id.accordion_major_advanced_icon);
            setupAccordion(view, R.id.accordion_department_common_header, R.id.accordion_department_common_content, R.id.accordion_department_common_icon);

            // DepartmentConfig에 따라 조건부 표시
            String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(
                GraduationAnalysisResultActivity.staticSelectedDepartment,
                GraduationAnalysisResultActivity.staticSelectedYear);

            View majorAdvancedContainer = view.findViewById(R.id.accordion_major_advanced_header).getParent() instanceof View ?
                (View) view.findViewById(R.id.accordion_major_advanced_header).getParent() : null;
            View departmentCommonContainer = view.findViewById(R.id.accordion_department_common_container);

            if ("전공심화".equals(categoryName)) {
                // 전공심화를 사용하는 학부/연도: 전공심화 표시, 학부공통 숨김
                if (majorAdvancedContainer != null) {
                    majorAdvancedContainer.setVisibility(View.VISIBLE);
                }
                if (departmentCommonContainer != null) {
                    departmentCommonContainer.setVisibility(View.GONE);
                }
            } else {
                // 학부공통을 사용하는 학부/연도: 학부공통 표시, 전공심화 숨김
                if (majorAdvancedContainer != null) {
                    majorAdvancedContainer.setVisibility(View.GONE);
                }
                if (departmentCommonContainer != null) {
                    departmentCommonContainer.setVisibility(View.VISIBLE);
                }
            }

            // 새로운 교양 카드들 설정
            setupCard(view, R.id.card_general_required_header, R.id.card_general_required_content);
            setupCard(view, R.id.card_general_elective_header, R.id.card_general_elective_content);
            setupCard(view, R.id.card_liberal_arts_header, R.id.card_liberal_arts_content);
        }

        private void setupAccordion(View parent, int headerId, int contentId, int iconId) {
            LinearLayout header = parent.findViewById(headerId);
            LinearLayout content = parent.findViewById(contentId);
            TextView icon = parent.findViewById(iconId);

            header.setOnClickListener(v -> {
                if (content.getVisibility() == View.VISIBLE) {
                    content.setVisibility(View.GONE);
                    icon.setText("▶");
                } else {
                    content.setVisibility(View.VISIBLE);
                    icon.setText("▼");
                }
            });
        }

        private void setupCard(View parent, int headerId, int contentId) {
            LinearLayout header = parent.findViewById(headerId);
            LinearLayout content = parent.findViewById(contentId);

            if (header != null && content != null) {
                header.setOnClickListener(v -> {
                    if (content.getVisibility() == View.VISIBLE) {
                        content.setVisibility(View.GONE);
                    } else {
                        content.setVisibility(View.VISIBLE);
                    }
                });
            }
        }

        private void updateMajorRequiredDetails(View view) {
            GraduationProgress progress = getGraduationProgress();
            List<String> allCourses = getAllMajorRequiredCourses();
            List<String> takenCourses = getTakenMajorRequiredCourses();

            if (progress == null || allCourses == null || takenCourses == null) return;

            // 헤더 텍스트 업데이트
            LinearLayout header = view.findViewById(R.id.accordion_major_required_header);
            TextView headerText = null;
            if (header != null && header.getChildCount() > 0) {
                View firstChild = header.getChildAt(0);
                if (firstChild instanceof TextView) {
                    headerText = (TextView) firstChild;
                }
            }

            if (headerText != null) {
                int remaining = progress.majorRequired.remaining;
                if (remaining > 0) {
                    headerText.setText("📚 전공필수 (" + remaining + "학점 부족)");
                } else {
                    headerText.setText("📚 전공필수 (완료)");
                }
            }

            // 미이수 과목 목록 업데이트
            LinearLayout contentLayout = view.findViewById(R.id.accordion_major_required_content);
            if (contentLayout != null) {
                // 기존 내용 제거 (헤더 텍스트 제외)
                contentLayout.removeAllViews();

                // 미이수 과목 헤더 추가
                TextView missingHeader = new TextView(getContext());
                missingHeader.setText("미이수 과목:");
                missingHeader.setTextSize(14);
                missingHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                missingHeader.setTextColor(0xFF000000); // colorOnSurface
                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                headerParams.setMargins(0, 0, 0, dpToPx(8));
                missingHeader.setLayoutParams(headerParams);
                contentLayout.addView(missingHeader);

                // 미이수 과목들 추가
                for (String course : allCourses) {
                    if (!isCourseCompleted(course, takenCourses)) {
                        int credits = getCourseCreditsFromFirebase(course);
                        addMissingCourseItem(contentLayout, course, credits);
                    }
                }

                // 안내문구 추가
                TextView guidanceText = new TextView(getContext());
                String overflowGuidance = DepartmentConfig.getOverflowGuidanceText(
                    GraduationAnalysisResultActivity.staticSelectedDepartment,
                    GraduationAnalysisResultActivity.staticSelectedYear);
                guidanceText.setText("💡 전공필수는 반드시 필요한 학점입니다. " + overflowGuidance);
                guidanceText.setTextSize(12);
                guidanceText.setTextColor(0xFF666666);
                guidanceText.setTypeface(null, android.graphics.Typeface.ITALIC);
                LinearLayout.LayoutParams guidanceParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                guidanceParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
                guidanceText.setLayoutParams(guidanceParams);
                contentLayout.addView(guidanceText);

                // 미이수 과목이 없을 경우
                if (progress.majorRequired.isCompleted) {
                    TextView completedText = new TextView(getContext());
                    completedText.setText("✅ 모든 전공필수 과목을 이수했습니다!");
                    completedText.setTextSize(14);
                    completedText.setTypeface(null, android.graphics.Typeface.BOLD);
                    completedText.setTextColor(0xFF4CAF50); // Green
                    completedText.setGravity(android.view.Gravity.CENTER);
                    contentLayout.addView(completedText);
                }
            }
        }

        private void addMissingCourseItem(LinearLayout parent, String courseName, int credits) {
            LinearLayout courseLayout = new LinearLayout(getContext());
            courseLayout.setOrientation(LinearLayout.HORIZONTAL);
            courseLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, dpToPx(6));
            courseLayout.setLayoutParams(layoutParams);

            // 책 이모지
            TextView emoji = new TextView(getContext());
            emoji.setText("📖");
            emoji.setTextSize(16);
            LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            emojiParams.setMargins(0, 0, dpToPx(8), 0);
            emoji.setLayoutParams(emojiParams);
            courseLayout.addView(emoji);

            // 과목명
            TextView courseText = new TextView(getContext());
            courseText.setText(courseName);
            courseText.setTextSize(14);
            courseText.setTextColor(0xFF000000); // colorOnSurface
            LinearLayout.LayoutParams courseParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            );
            courseText.setLayoutParams(courseParams);
            courseLayout.addView(courseText);

            // 학점
            TextView creditText = new TextView(getContext());
            creditText.setText(credits + "학점");
            creditText.setTextSize(14);
            creditText.setTextColor(0xFF2196F3); // colorPrimary
            creditText.setTypeface(null, android.graphics.Typeface.BOLD);
            courseLayout.addView(creditText);

            parent.addView(courseLayout);
        }

        private void updateMajorElectiveDetails(View view) {
            GraduationProgress progress = getGraduationProgress();
            List<String> allCourses = getAllMajorElectiveCourses();
            List<String> takenCourses = getTakenMajorElectiveCourses();

            if (progress == null || allCourses == null || takenCourses == null) return;

            // 헤더 텍스트 업데이트 (이미 updateMajorElectiveHeader에서 처리됨)

            // 미이수 과목 목록 업데이트
            LinearLayout contentLayout = view.findViewById(R.id.accordion_major_elective_content);
            if (contentLayout != null) {
                // 기존 내용 제거
                contentLayout.removeAllViews();

                // 미이수 과목 헤더 추가
                TextView missingHeader = new TextView(getContext());
                missingHeader.setText("미이수 과목:");
                missingHeader.setTextSize(14);
                missingHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                missingHeader.setTextColor(0xFF000000);
                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                headerParams.setMargins(0, 0, 0, dpToPx(8));
                missingHeader.setLayoutParams(headerParams);
                contentLayout.addView(missingHeader);

                // 미이수 과목들 모두 추가
                for (String course : allCourses) {
                    if (!isCourseCompleted(course, takenCourses)) {
                        int credits = getCourseCreditsFromFirebase(course);
                        addMissingCourseItem(contentLayout, course, credits);
                    }
                }

                // 안내문구 추가
                TextView guidanceText = new TextView(getContext());
                String overflowGuidance = DepartmentConfig.getOverflowGuidanceText(
                    GraduationAnalysisResultActivity.staticSelectedDepartment,
                    GraduationAnalysisResultActivity.staticSelectedYear);
                guidanceText.setText("💡 전공선택은 필요한 학점만큼 수강하세요. " + overflowGuidance);
                guidanceText.setTextSize(12);
                guidanceText.setTextColor(0xFF666666);
                guidanceText.setTypeface(null, android.graphics.Typeface.ITALIC);
                LinearLayout.LayoutParams guidanceParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                guidanceParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
                guidanceText.setLayoutParams(guidanceParams);
                contentLayout.addView(guidanceText);
            }
        }

        private void updateMajorAdvancedDetails(View view) {
            GraduationProgress progress = getGraduationProgress();
            List<String> allCourses = getAllMajorAdvancedCourses();
            List<String> takenCourses = getTakenMajorAdvancedCourses();

            // 디버깅 로그 추가
            Log.d(TAG, "===== 전공심화 아코디언 업데이트 =====");
            Log.d(TAG, "progress: " + (progress != null ? "존재" : "null"));
            Log.d(TAG, "allCourses: " + (allCourses != null ? allCourses.size() + "개" : "null"));
            Log.d(TAG, "takenCourses: " + (takenCourses != null ? takenCourses.size() + "개" : "null"));
            if (progress != null && progress.majorAdvanced != null) {
                Log.d(TAG, "majorAdvanced progress: " + progress.majorAdvanced.earned + "/" + progress.majorAdvanced.required);
            }

            // 기본값 설정 - null인 경우 빈 리스트로 초기화
            if (allCourses == null) {
                allCourses = new ArrayList<>();
                Log.w(TAG, "allMajorAdvancedCourses가 null입니다. 빈 리스트로 초기화합니다.");
            }
            if (takenCourses == null) {
                takenCourses = new ArrayList<>();
                Log.w(TAG, "takenMajorAdvancedCourses가 null입니다. 빈 리스트로 초기화합니다.");
            }

            // 미이수 과목 목록 업데이트
            LinearLayout contentLayout = view.findViewById(R.id.accordion_major_advanced_content);
            if (contentLayout != null) {
                // 기존 내용 제거
                contentLayout.removeAllViews();

                if (progress != null && progress.majorAdvanced != null && progress.majorAdvanced.isCompleted) {
                    // 완료된 경우 - 교양필수처럼 완료 메시지 표시
                    TextView completedText = new TextView(getContext());
                    completedText.setText("✅ 전공심화 학점을 모두 취득했습니다!");
                    completedText.setTextSize(16);
                    completedText.setTypeface(null, android.graphics.Typeface.BOLD);
                    completedText.setTextColor(0xFF4CAF50);
                    completedText.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, dpToPx(16));
                    completedText.setLayoutParams(params);
                    contentLayout.addView(completedText);
                } else {
                    // 미완료된 경우 - 미이수 과목들 표시
                    addSectionHeader(contentLayout, "🔍 미이수 전공심화 과목");

                    if (!allCourses.isEmpty()) {
                        // 미이수 과목들 추가
                        boolean hasUncompletedCourses = false;
                        for (String course : allCourses) {
                            if (!isCourseCompleted(course, takenCourses)) {
                                int credits = getCourseCreditsFromFirebase(course);
                                addMissingCourseItem(contentLayout, course, credits);
                                hasUncompletedCourses = true;
                            }
                        }

                        if (!hasUncompletedCourses) {
                            // 모든 과목을 이수했지만 학점이 부족한 경우
                            TextView noMissingText = new TextView(getContext());
                            noMissingText.setText("📚 모든 전공심화 과목을 이수했습니다.\n필요 학점까지 추가 과목을 수강하세요.");
                            noMissingText.setTextSize(14);
                            noMissingText.setTextColor(0xFF666666);
                            noMissingText.setGravity(android.view.Gravity.CENTER);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(0, dpToPx(8), 0, dpToPx(8));
                            noMissingText.setLayoutParams(params);
                            contentLayout.addView(noMissingText);
                        }
                    } else {
                        // 전공심화 과목 데이터가 없는 경우
                        TextView noDataText = new TextView(getContext());
                        noDataText.setText("📚 전공심화 과목 정보를 불러오는 중입니다...\n또는 해당 학과에 전공심화 과목이 없습니다.");
                        noDataText.setTextSize(14);
                        noDataText.setTextColor(0xFF666666);
                        noDataText.setGravity(android.view.Gravity.CENTER);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
                        noDataText.setLayoutParams(params);
                        contentLayout.addView(noDataText);
                    }

                    // 안내문구 추가
                    TextView guidanceText = new TextView(getContext());
                    String overflowGuidance = DepartmentConfig.getOverflowGuidanceText(
                        GraduationAnalysisResultActivity.staticSelectedDepartment,
                        GraduationAnalysisResultActivity.staticSelectedYear);
                    guidanceText.setText("💡 전공심화는 필요한 학점만큼 수강하세요. " + overflowGuidance);
                    guidanceText.setTextSize(12);
                    guidanceText.setTextColor(0xFF666666);
                    guidanceText.setTypeface(null, android.graphics.Typeface.ITALIC);
                    LinearLayout.LayoutParams guidanceParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    guidanceParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
                    guidanceText.setLayoutParams(guidanceParams);
                    contentLayout.addView(guidanceText);
                }
            }
        }

        private void updateDepartmentCommonDetails(View view) {
            GraduationProgress progress = getGraduationProgress();
            List<String> allCourses = getAllDepartmentCommonCourses();
            List<String> takenCourses = getTakenDepartmentCommonCourses();

            if (progress == null || allCourses == null || takenCourses == null) return;

            // 20-22학번에만 표시되므로 학번 체크
            if (!isOldCurriculum(GraduationAnalysisResultActivity.staticSelectedYear)) return;

            // 헤더 텍스트 업데이트
            LinearLayout header = view.findViewById(R.id.accordion_department_common_header);
            TextView headerText = null;
            if (header != null && header.getChildCount() > 0) {
                View firstChild = header.getChildAt(0);
                if (firstChild instanceof TextView) {
                    headerText = (TextView) firstChild;
                }
            }

            if (headerText != null && progress.departmentCommon != null) {
                int remaining = progress.departmentCommon.remaining;
                if (remaining > 0) {
                    headerText.setText("🏛️ 학부공통 (" + remaining + "학점 부족)");
                } else {
                    headerText.setText("🏛️ 학부공통 (완료)");
                }
            }

            // 미이수 과목 목록 업데이트
            LinearLayout contentLayout = view.findViewById(R.id.accordion_department_common_content);
            if (contentLayout != null) {
                // 기존 내용 제거
                contentLayout.removeAllViews();

                // 미이수 과목 헤더 추가
                TextView missingHeader = new TextView(getContext());
                missingHeader.setText("미이수 과목:");
                missingHeader.setTextSize(14);
                missingHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                missingHeader.setTextColor(0xFF000000);
                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                headerParams.setMargins(0, 0, 0, dpToPx(8));
                missingHeader.setLayoutParams(headerParams);
                contentLayout.addView(missingHeader);

                // 미이수 과목들 추가
                for (String course : allCourses) {
                    if (!isCourseCompleted(course, takenCourses)) {
                        int credits = getCourseCreditsFromFirebase(course);
                        addMissingCourseItem(contentLayout, course, credits);
                    }
                }

                // 안내문구 추가
                TextView guidanceText = new TextView(getContext());
                String overflowGuidance = DepartmentConfig.getOverflowGuidanceText(
                    GraduationAnalysisResultActivity.staticSelectedDepartment,
                    GraduationAnalysisResultActivity.staticSelectedYear);
                guidanceText.setText("💡 학부공통은 필요한 학점만큼 수강하세요. " + overflowGuidance);
                guidanceText.setTextSize(12);
                guidanceText.setTextColor(0xFF666666);
                guidanceText.setTypeface(null, android.graphics.Typeface.ITALIC);
                LinearLayout.LayoutParams guidanceParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                guidanceParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
                guidanceText.setLayoutParams(guidanceParams);
                contentLayout.addView(guidanceText);
            }
        }

        private void updateGeneralEducationDetails(View view) {
            GraduationProgress progress = getGraduationProgress();
            GeneralEducationAnalysis analysis = getGeneralEducationAnalysis();

            if (progress == null || analysis == null) return;

            updateMajorElectiveHeader(view, progress);
            updateMajorAdvancedHeader(view, progress);
            updateGeneralRequiredCard(view, progress, analysis);
            updateGeneralElectiveCard(view, progress, analysis);
            updateLiberalArtsCard(view, progress, analysis);
        }

        private void updateMajorElectiveHeader(View view, GraduationProgress progress) {
            LinearLayout header = view.findViewById(R.id.accordion_major_elective_header);
            TextView headerText = null;
            if (header != null && header.getChildCount() > 0) {
                View firstChild = header.getChildAt(0);
                if (firstChild instanceof TextView) {
                    headerText = (TextView) firstChild;
                }
            }

            if (headerText != null && progress.majorElective != null) {
                int remaining = progress.majorElective.remaining;
                if (remaining > 0) {
                    headerText.setText("🎯 전공선택 (" + remaining + "학점 부족)");
                } else {
                    headerText.setText("🎯 전공선택 (완료)");
                }
            }
        }

        private void updateMajorAdvancedHeader(View view, GraduationProgress progress) {
            LinearLayout header = view.findViewById(R.id.accordion_major_advanced_header);
            TextView headerText = null;
            if (header != null && header.getChildCount() > 0) {
                View firstChild = header.getChildAt(0);
                if (firstChild instanceof TextView) {
                    headerText = (TextView) firstChild;
                }
            }

            if (headerText != null && progress.majorAdvanced != null) {
                int remaining = progress.majorAdvanced.remaining;
                if (remaining > 0) {
                    headerText.setText("🚀 전공심화 (" + remaining + "학점 부족)");
                } else {
                    headerText.setText("🚀 전공심화 (완료)");
                }
            }
        }

        private void updateGeneralRequiredCard(View view, GraduationProgress progress, GeneralEducationAnalysis analysis) {
            // 헤더 텍스트 업데이트
            LinearLayout header = view.findViewById(R.id.card_general_required_header);
            TextView headerText = null;
            if (header != null && header.getChildCount() > 0) {
                View firstChild = header.getChildAt(0);
                if (firstChild instanceof TextView) {
                    headerText = (TextView) firstChild;
                }
            }

            if (headerText != null) {
                int remaining = progress.generalRequired.remaining;
                if (remaining > 0) {
                    headerText.setText("📚 교양필수 (" + remaining + "학점 부족)");
                } else {
                    headerText.setText("📚 교양필수 (완료)");
                }
            }

            // 상태 텍스트 업데이트
            TextView statusText = view.findViewById(R.id.text_general_required_status);
            if (statusText != null) {
                int earned = progress.generalRequired.earned;
                int required = progress.generalRequired.required;

                if (progress.generalRequired.isCompleted) {
                    statusText.setText(earned + "/" + required + " 학점");
                    statusText.setTextColor(0xFF4CAF50);
                } else {
                    statusText.setText(earned + "/" + required + " 학점");
                    statusText.setTextColor(0xFFFF5722);
                }
            }

            // 콘텐츠 업데이트
            LinearLayout contentLayout = view.findViewById(R.id.card_general_required_content);
            if (contentLayout != null) {
                contentLayout.removeAllViews();

                if (progress.generalRequired.isCompleted) {
                    // 완료된 경우 - 간단한 완료 메시지만 표시
                    TextView completedText = new TextView(getContext());
                    completedText.setText("✅ 교양필수 모든 과목을 이수했습니다!");
                    completedText.setTextSize(16);
                    completedText.setTypeface(null, android.graphics.Typeface.BOLD);
                    completedText.setTextColor(0xFF4CAF50);
                    completedText.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, dpToPx(16));
                    completedText.setLayoutParams(params);
                    contentLayout.addView(completedText);
                } else {
                    // 미완료된 경우 - 부족한 항목들 표시
                    addSectionHeader(contentLayout, "🔍 미이수 교양필수 과목");

                    // oneOf 그룹 중 미완료된 것들
                    for (OneOfGroupStatus groupStatus : analysis.oneOfGroupStatus.values()) {
                        if (!groupStatus.isCompleted) {
                            addOneOfGroupItem(contentLayout, groupStatus);
                        }
                    }

                    // oneOf 그룹에 포함된 모든 과목 이름을 수집
                    java.util.Set<String> coursesInOneOfGroups = new java.util.HashSet<>();
                    for (OneOfGroupStatus groupStatus : analysis.oneOfGroupStatus.values()) {
                        if (groupStatus.requiredCourses != null) {
                            coursesInOneOfGroups.addAll(groupStatus.requiredCourses);
                        }
                    }

                    // 개별 필수 과목 중 미완료된 것들 (oneOf 그룹에 포함되지 않은 것만)
                    for (Map.Entry<String, Boolean> entry : analysis.individualRequiredStatus.entrySet()) {
                        if (!entry.getValue()) {
                            String courseName = entry.getKey();
                            // oneOf 그룹에 포함된 과목은 건너뜁니다
                            if (coursesInOneOfGroups.contains(courseName)) {
                                android.util.Log.d(TAG, "Skipping course in oneOf group: " + courseName);
                                continue;
                            }
                            int credits = getCreditsForCourse(courseName);
                            addMissingCourseItem(contentLayout, courseName, credits);
                        }
                    }
                }
            }
        }

        private void updateGeneralElectiveCard(View view, GraduationProgress progress, GeneralEducationAnalysis analysis) {
            // 헤더 텍스트 업데이트
            LinearLayout header = view.findViewById(R.id.card_general_elective_header);
            TextView headerText = null;
            if (header != null && header.getChildCount() > 0) {
                View firstChild = header.getChildAt(0);
                if (firstChild instanceof TextView) {
                    headerText = (TextView) firstChild;
                }
            }

            if (headerText != null) {
                // generalElective가 null일 수 있으므로 체크
                if (progress.generalElective == null) {
                    headerText.setText("📖 교양선택 (데이터 없음)");
                    return;
                }

                int remaining = progress.generalElective.remaining;
                String headerMessage = "📖 교양선택";

                // 학점 상태
                if (remaining > 0) {
                    headerMessage += " (" + remaining + "학점 부족";
                } else {
                    headerMessage += " (학점 완료";
                }

                // 역량 상태 추가
                if (progress.competencyProgress != null) {
                    int completedCompetencies = progress.competencyProgress.completedCompetencies.size();
                    int requiredCompetencies = progress.competencyProgress.requiredCompetencyCount;

                    if (progress.competencyProgress.isCompleted) {
                        headerMessage += ", 역량 완료)";
                    } else {
                        int needed = requiredCompetencies - completedCompetencies;
                        headerMessage += ", 역량 " + needed + "개 필요)";
                    }
                } else {
                    headerMessage += ")";
                }

                headerText.setText(headerMessage);
            }

            // 상태 텍스트 업데이트
            TextView statusText = view.findViewById(R.id.text_general_elective_status);
            if (statusText != null) {
                int taken = calculateTotalCreditsByCategory(analysis.takenGeneralElective, "교양선택");
                int required = progress.generalElective.required;

                if (taken >= required) {
                    statusText.setText(taken + "/" + required + " 학점");
                    statusText.setTextColor(0xFF4CAF50);
                } else {
                    statusText.setText(taken + "/" + required + " 학점");
                    statusText.setTextColor(0xFFFF9800);
                }
            }

            // 콘텐츠 업데이트
            LinearLayout contentLayout = view.findViewById(R.id.card_general_elective_content);
            if (contentLayout != null) {
                contentLayout.removeAllViews();

                addSectionHeader(contentLayout, "📖 교양선택 현황");

                // 역량 진행 상황 표시
                if (progress.competencyProgress != null) {
                    LinearLayout competencySection = new LinearLayout(getContext());
                    competencySection.setOrientation(LinearLayout.VERTICAL);
                    competencySection.setBackgroundResource(R.drawable.spinner_background);
                    competencySection.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
                    LinearLayout.LayoutParams competencyParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    competencyParams.setMargins(0, dpToPx(8), 0, dpToPx(16));
                    competencySection.setLayoutParams(competencyParams);

                    // 역량 헤더
                    TextView competencyHeader = new TextView(getContext());
                    competencyHeader.setText("🎯 역량 달성 현황");
                    competencyHeader.setTextSize(16);
                    competencyHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    competencyHeader.setTextColor(0xFF333333);
                    competencyHeader.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    competencySection.addView(competencyHeader);

                    // 역량 진행률
                    TextView competencyStatus = new TextView(getContext());
                    int completedCount = progress.competencyProgress.completedCompetencies.size();
                    int requiredCount = progress.competencyProgress.requiredCompetencyCount;
                    String competencyStatusText = completedCount + "/" + requiredCount + " 역량 완료";
                    if (progress.competencyProgress.isCompleted) {
                        competencyStatusText += " ✅";
                        competencyStatus.setTextColor(0xFF4CAF50);
                    } else {
                        competencyStatusText += " (" + (requiredCount - completedCount) + "개 더 필요)";
                        competencyStatus.setTextColor(0xFFFF9800);
                    }
                    competencyStatus.setText(competencyStatusText);
                    competencyStatus.setTextSize(14);
                    competencyStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                    LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    statusParams.setMargins(0, dpToPx(4), 0, dpToPx(8));
                    competencyStatus.setLayoutParams(statusParams);
                    competencySection.addView(competencyStatus);

                    // 완료된 역량 목록
                    if (!progress.competencyProgress.completedCompetencies.isEmpty()) {
                        TextView completedLabel = new TextView(getContext());
                        completedLabel.setText("완료된 역량:");
                        completedLabel.setTextSize(12);
                        completedLabel.setTextColor(0xFF666666);
                        completedLabel.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        competencySection.addView(completedLabel);

                        StringBuilder completedList = new StringBuilder();
                        for (String competency : progress.competencyProgress.completedCompetencies) {
                            if (completedList.length() > 0) completedList.append(", ");
                            completedList.append(competency);
                        }

                        TextView completedText = new TextView(getContext());
                        completedText.setText(completedList.toString());
                        completedText.setTextSize(14);
                        completedText.setTextColor(0xFF4CAF50);
                        LinearLayout.LayoutParams completedParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        completedParams.setMargins(dpToPx(8), dpToPx(2), 0, 0);
                        completedText.setLayoutParams(completedParams);
                        competencySection.addView(completedText);
                    }

                    contentLayout.addView(competencySection);
                }

                // 교양선택 안내 및 설명 섹션 추가
                LinearLayout guideSection = new LinearLayout(getContext());
                guideSection.setOrientation(LinearLayout.VERTICAL);
                guideSection.setBackgroundResource(R.drawable.spinner_background);
                guideSection.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
                LinearLayout.LayoutParams guideParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                guideParams.setMargins(0, dpToPx(8), 0, dpToPx(16));
                guideSection.setLayoutParams(guideParams);

                // 안내 헤더
                TextView guideHeader = new TextView(getContext());
                guideHeader.setText("💡 교양선택 이수 가이드");
                guideHeader.setTextSize(16);
                guideHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                guideHeader.setTextColor(0xFF333333);
                guideHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                guideSection.addView(guideHeader);

                // 교양선택 이수 조건
                TextView requirementsText = new TextView(getContext());
                String requirements = "교양선택 이수 조건:\n" +
                        "• 총 " + progress.generalElective.required + "학점 이수 필요\n" +
                        "• 최소 " + progress.competencyProgress.requiredCompetencyCount + "개 역량 이수 필요";
                requirementsText.setText(requirements);
                requirementsText.setTextSize(14);
                requirementsText.setTextColor(0xFF333333);
                requirementsText.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams reqParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                reqParams.setMargins(0, dpToPx(8), 0, dpToPx(12));
                requirementsText.setLayoutParams(reqParams);
                guideSection.addView(requirementsText);

                // 현재 문서에 따른 오버플로우 처리 방식 결정
                String overflowDestination;
                if (progress.majorAdvanced != null && progress.majorAdvanced.required > 0) {
                    // 전공심화가 있는 경우 (신 교육과정)
                    overflowDestination = "잔여학점";
                } else if (progress.departmentCommon != null && progress.departmentCommon.required > 0) {
                    // 학부공통이 있는 경우 (구 교육과정)
                    overflowDestination = "일반선택";
                } else {
                    // 기본값 (안전장치)
                    overflowDestination = "잔여학점";
                }

                // 오버플로우 처리 안내
                TextView overflowText = new TextView(getContext());
                overflowText.setText("• 초과 이수한 학점은 " + overflowDestination + "으로 인정됩니다");
                overflowText.setTextSize(14);
                overflowText.setTextColor(0xFF333333);
                LinearLayout.LayoutParams overflowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                overflowParams.setMargins(0, dpToPx(4), 0, 0);
                overflowText.setLayoutParams(overflowParams);
                guideSection.addView(overflowText);

                contentLayout.addView(guideSection);

                if (analysis.takenGeneralElective.isEmpty()) {
                    TextView noCoursesText = new TextView(getContext());
                    noCoursesText.setText("아직 이수한 교양선택 과목이 없습니다.");
                    noCoursesText.setTextSize(14);
                    noCoursesText.setTextColor(0xFF666666);
                    noCoursesText.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, dpToPx(16), 0, dpToPx(16));
                    noCoursesText.setLayoutParams(params);
                    contentLayout.addView(noCoursesText);
                } else {
                    for (String course : analysis.takenGeneralElective) {
                        int credits = getCourseCreditsFromFirebase(course);
                        addTakenCourseItem(contentLayout, course, credits);
                    }
                }
            }
        }

        private void updateLiberalArtsCard(View view, GraduationProgress progress, GeneralEducationAnalysis analysis) {
            // 헤더 텍스트 업데이트
            LinearLayout header = view.findViewById(R.id.card_liberal_arts_header);
            TextView headerText = null;
            if (header != null && header.getChildCount() > 0) {
                View firstChild = header.getChildAt(0);
                if (firstChild instanceof TextView) {
                    headerText = (TextView) firstChild;
                }
            }

            if (headerText != null) {
                // liberalArts가 null일 수 있으므로 체크
                if (progress.liberalArts == null) {
                    headerText.setText("🎨 소양 (데이터 없음)");
                    return;
                }
                int remaining = progress.liberalArts.remaining;
                if (remaining > 0) {
                    headerText.setText("🎨 소양 (" + remaining + "학점 부족)");
                } else {
                    headerText.setText("🎨 소양 (완료)");
                }
            }

            // 상태 텍스트 업데이트
            TextView statusText = view.findViewById(R.id.text_liberal_arts_status);
            if (statusText != null) {
                int taken = calculateTotalCreditsByCategory(analysis.takenLiberalArts, "소양");
                int required = progress.liberalArts.required;

                if (taken >= required) {
                    statusText.setText(taken + "/" + required + " 학점");
                    statusText.setTextColor(0xFF4CAF50);
                } else {
                    statusText.setText(taken + "/" + required + " 학점");
                    statusText.setTextColor(0xFFFF9800);
                }
            }

            // 콘텐츠 업데이트
            LinearLayout contentLayout = view.findViewById(R.id.card_liberal_arts_content);
            if (contentLayout != null) {
                contentLayout.removeAllViews();

                addSectionHeader(contentLayout, "🎨 소양 현황");

                if (analysis.takenLiberalArts.isEmpty()) {
                    TextView noCoursesText = new TextView(getContext());
                    noCoursesText.setText("아직 이수한 소양 과목이 없습니다.");
                    noCoursesText.setTextSize(14);
                    noCoursesText.setTextColor(0xFF666666);
                    noCoursesText.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, dpToPx(16), 0, dpToPx(16));
                    noCoursesText.setLayoutParams(params);
                    contentLayout.addView(noCoursesText);
                } else {
                    for (String course : analysis.takenLiberalArts) {
                        int credits = getCourseCreditsFromFirebase(course);
                        addTakenCourseItem(contentLayout, course, credits);
                    }
                }
            }
        }

        private void addOneOfGroupItem(LinearLayout parent, OneOfGroupStatus groupStatus) {
            // requiredCourses가 비어있으면 해당 그룹을 건너뜁니다
            if (groupStatus.requiredCourses == null || groupStatus.requiredCourses.isEmpty()) {
                android.util.Log.w("GradAnalysis", "Skipping oneOf group with empty requiredCourses: " + groupStatus.groupName);
                return;
            }

            LinearLayout groupLayout = new LinearLayout(getContext());
            groupLayout.setOrientation(LinearLayout.VERTICAL);
            groupLayout.setPadding(0, dpToPx(8), 0, dpToPx(8));
            groupLayout.setBackgroundColor(0xFFF5F5F5);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, dpToPx(6));
            groupLayout.setLayoutParams(layoutParams);

            // 그룹 제목 (가로 레이아웃으로 변경)
            LinearLayout titleLayout = new LinearLayout(getContext());
            titleLayout.setOrientation(LinearLayout.HORIZONTAL);
            titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams titleLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleLayoutParams.setMargins(dpToPx(8), 0, dpToPx(8), dpToPx(4));
            titleLayout.setLayoutParams(titleLayoutParams);

            // 그룹 제목 텍스트
            TextView groupTitle = new TextView(getContext());
            groupTitle.setText("📚 " + groupStatus.groupName + " 중 1개 선택:");
            groupTitle.setTextSize(14);
            groupTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            groupTitle.setTextColor(0xFF000000);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            );
            groupTitle.setLayoutParams(titleParams);
            titleLayout.addView(groupTitle);

            // 학점 표시 (그룹의 첫 번째 과목 기준으로 학점 계산)
            int groupCredits = getCreditsForCourse(groupStatus.requiredCourses.get(0));
            TextView creditText = new TextView(getContext());
            creditText.setText(groupCredits + "학점");
            creditText.setTextSize(14);
            creditText.setTextColor(0xFF2196F3);
            creditText.setTypeface(null, android.graphics.Typeface.BOLD);
            titleLayout.addView(creditText);

            groupLayout.addView(titleLayout);

            // 선택 가능한 과목들 (학점 정보와 함께)
            for (String course : groupStatus.requiredCourses) {
                int credits = getCreditsForCourse(course);
                TextView courseText = new TextView(getContext());
                courseText.setText("  • " + course + " (" + credits + "학점)");
                courseText.setTextSize(12);
                courseText.setTextColor(0xFF666666);
                LinearLayout.LayoutParams courseParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                courseParams.setMargins(dpToPx(16), 0, 0, dpToPx(2));
                courseText.setLayoutParams(courseParams);
                groupLayout.addView(courseText);
            }

            parent.addView(groupLayout);
        }


        private void addSectionHeader(LinearLayout parent, String title) {
            TextView header = new TextView(getContext());
            header.setText(title);
            header.setTextSize(14);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setTextColor(0xFF000000);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            headerParams.setMargins(0, dpToPx(12), 0, dpToPx(8));
            header.setLayoutParams(headerParams);
            parent.addView(header);
        }

        private void addTakenCourseItem(LinearLayout parent, String courseName, int credits) {
            LinearLayout courseLayout = new LinearLayout(getContext());
            courseLayout.setOrientation(LinearLayout.HORIZONTAL);
            courseLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            courseLayout.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            courseLayout.setBackgroundColor(0xFFE8F5E8);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, dpToPx(6));
            courseLayout.setLayoutParams(layoutParams);

            // 완료 아이콘
            TextView icon = new TextView(getContext());
            icon.setText("✅");
            icon.setTextSize(16);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            iconParams.setMargins(0, 0, dpToPx(8), 0);
            icon.setLayoutParams(iconParams);
            courseLayout.addView(icon);

            // 과목명
            TextView nameText = new TextView(getContext());
            nameText.setText(courseName);
            nameText.setTextSize(14);
            nameText.setTextColor(0xFF000000);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            );
            nameText.setLayoutParams(nameParams);
            courseLayout.addView(nameText);

            // 학점
            TextView creditText = new TextView(getContext());
            creditText.setText(credits + "학점");
            creditText.setTextSize(14);
            creditText.setTextColor(0xFF4CAF50);
            creditText.setTypeface(null, android.graphics.Typeface.BOLD);
            courseLayout.addView(creditText);

            parent.addView(courseLayout);
        }

        private int getCreditsForCourse(String courseName) {
            // 1순위: 동적으로 로드된 교양 과목 학점 정보
            if (courseCreditsMap.containsKey(courseName)) {
                return courseCreditsMap.get(courseName);
            }

            // Firebase에서도 찾을 수 없는 경우, 사용자 입력 과목에서 학점 정보 확인
            if (staticCourseList != null) {
                for (CourseInputActivity.Course course : staticCourseList) {
                    if (courseName.equals(course.getName())) {
                        return course.getCredits();
                    }
                }
            }

            // 모든 곳에서 찾을 수 없는 경우 로그 남기고 1학점으로 설정 (최소값)
            Log.w(TAG, "과목 '" + courseName + "'의 학점 정보를 찾을 수 없습니다. 1학점으로 설정합니다.");
            return 1;
        }

        private int getCourseCreditsFromFirebase(String courseName) {
            return getCreditsForCourse(courseName);
        }

        private int calculateTotalCreditsByCategory(List<String> courseNames, String category) {
            int totalCredits = 0;
            for (String courseName : courseNames) {
                totalCredits += getCourseCreditsFromFirebase(courseName);
            }
            return totalCredits;
        }

        private int dpToPx(int dp) {
            float density = getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }

    // 요약 탭 프래그먼트
    public static class OthersTabFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tab_others, container, false);

            // 기타 요건 UI 업데이트
            updateOthersUI(view);

            return view;
        }

        private void updateOthersUI(View view) {
            // 아코디언 클릭 리스너 설정
            setupAccordionClickListeners(view);

            // 학부별 추가 졸업 요건 로드
            loadDepartmentExtraRequirements(view);

            // 사용자 입력 데이터를 기반으로 기타 요건 상태 설정
            updateOthersData(view);
        }

        private void setupAccordionClickListeners(View view) {
            // TLC 아코디언
            View tlcHeader = view.findViewById(R.id.accordion_tlc_header);
            View tlcContent = view.findViewById(R.id.accordion_tlc_content);
            TextView tlcIcon = view.findViewById(R.id.accordion_tlc_icon);

            if (tlcHeader != null && tlcContent != null && tlcIcon != null) {
                tlcHeader.setOnClickListener(v -> toggleAccordion(tlcContent, tlcIcon));
            }

            // 채플 아코디언
            View chapelHeader = view.findViewById(R.id.accordion_chapel_header);
            View chapelContent = view.findViewById(R.id.accordion_chapel_content);
            TextView chapelIcon = view.findViewById(R.id.accordion_chapel_icon);

            if (chapelHeader != null && chapelContent != null && chapelIcon != null) {
                chapelHeader.setOnClickListener(v -> toggleAccordion(chapelContent, chapelIcon));
            }

            // 마일리지 아코디언
            View mileageHeader = view.findViewById(R.id.accordion_mileage_header);
            View mileageContent = view.findViewById(R.id.accordion_mileage_content);
            TextView mileageIcon = view.findViewById(R.id.accordion_mileage_icon);

            if (mileageHeader != null && mileageContent != null && mileageIcon != null) {
                mileageHeader.setOnClickListener(v -> toggleAccordion(mileageContent, mileageIcon));
            }
        }

        private void toggleAccordion(View content, TextView icon) {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                icon.setText("▶");
            } else {
                content.setVisibility(View.VISIBLE);
                icon.setText("▼");
            }
        }

        private void updateOthersData(View view) {
            if (staticAdditionalRequirements == null) {
                Log.w(TAG, "AdditionalRequirements 데이터가 없습니다");
                return;
            }

            // TLC 헤더와 상태 업데이트
            updateTLCHeader(view);
            TextView tlcStatus = view.findViewById(R.id.text_tlc_status);
            if (tlcStatus != null) {
                int tlcCount = staticAdditionalRequirements.getTlcCount();
                tlcStatus.setText(tlcCount + "/6회");
                updateStatusColor(tlcStatus, tlcCount, 6);
            }

            // 채플 헤더와 상태 업데이트
            updateChapelHeader(view);
            TextView chapelStatus = view.findViewById(R.id.text_chapel_status);
            if (chapelStatus != null) {
                int chapelCount = staticAdditionalRequirements.getChapelCount();
                chapelStatus.setText(chapelCount + "/6학기");
                updateStatusColor(chapelStatus, chapelCount, 6);
            }

            // 마일리지 헤더와 상태 업데이트
            updateMileageHeader(view);
            TextView mileageStatus = view.findViewById(R.id.text_mileage_status);
            if (mileageStatus != null) {
                boolean isCompleted = staticAdditionalRequirements.isMileageCompleted();
                mileageStatus.setText(isCompleted ? "완료" : "미완료");
                mileageStatus.setTextColor(isCompleted ? 0xFF4CAF50 : 0xFFFF9800);
            }

            // 전체 진행률 계산
            int tlcCount = staticAdditionalRequirements.getTlcCount();
            int chapelCount = staticAdditionalRequirements.getChapelCount();
            boolean mileageCompleted = staticAdditionalRequirements.isMileageCompleted();
            boolean extraGradCompleted = staticAdditionalRequirements.isExtraGradCompleted();

            // 완료된 항목 수 계산
            int completedCount = 0;
            int totalCount = 3; // TLC, 채플, 마일리지

            if (tlcCount >= 6) completedCount++;
            if (chapelCount >= 6) completedCount++;
            if (mileageCompleted) completedCount++;

            // 추가 졸업 요건이 있는 경우 총 개수 증가
            if (staticSelectedDepartment != null) {
                totalCount = 4; // TLC, 채플, 마일리지, 추가요건
                if (extraGradCompleted) completedCount++;
            }

            // 전체 진행률 업데이트
            TextView othersProgress = view.findViewById(R.id.text_others_progress);
            if (othersProgress != null) {
                othersProgress.setText(completedCount + "/" + totalCount + " 완료");
            }

            // 진행바 업데이트
            android.widget.ProgressBar tlcProgressBar = view.findViewById(R.id.progress_tlc);
            if (tlcProgressBar != null) {
                int tlcProgress = Math.min(100, (tlcCount * 100) / 6);
                tlcProgressBar.setProgress(tlcProgress);
            }

            android.widget.ProgressBar chapelProgressBar = view.findViewById(R.id.progress_chapel);
            if (chapelProgressBar != null) {
                int chapelProgress = Math.min(100, (chapelCount * 100) / 6);
                chapelProgressBar.setProgress(chapelProgress);
            }

            android.widget.ProgressBar othersProgressBar = view.findViewById(R.id.progress_others_total);
            if (othersProgressBar != null) {
                int totalProgress = (completedCount * 100) / totalCount;
                othersProgressBar.setProgress(totalProgress);
            }

            // 퍼센티지 텍스트 업데이트
            TextView tlcPercentage = view.findViewById(R.id.text_tlc_percentage);
            if (tlcPercentage != null) {
                int tlcProgress = Math.min(100, (tlcCount * 100) / 6);
                tlcPercentage.setText(tlcProgress + "%");
            }

            TextView chapelPercentage = view.findViewById(R.id.text_chapel_percentage);
            if (chapelPercentage != null) {
                int chapelProgress = Math.min(100, (chapelCount * 100) / 6);
                chapelPercentage.setText(chapelProgress + "%");
            }

            // 요약 메시지 업데이트
            TextView othersSummary = view.findViewById(R.id.text_others_summary);
            if (othersSummary != null) {
                String summaryMessage = generateSummaryMessage(tlcCount, chapelCount, mileageCompleted, extraGradCompleted);
                othersSummary.setText(summaryMessage);
            }
        }

        private void updateTLCHeader(View view) {
            TextView headerText = view.findViewById(R.id.text_tlc_header);

            if (headerText != null && staticAdditionalRequirements != null) {
                int tlcCount = staticAdditionalRequirements.getTlcCount();
                Log.d(TAG, "updateTLCHeader: TLC 횟수 = " + tlcCount);
                if (tlcCount >= 6) {
                    headerText.setText("🎓 TLC (완료)");
                    Log.d(TAG, "updateTLCHeader: TLC 헤더를 '완료'로 설정");
                } else {
                    int remaining = 6 - tlcCount;
                    headerText.setText("🎓 TLC (" + remaining + "회 부족)");
                    Log.d(TAG, "updateTLCHeader: TLC 헤더를 '" + remaining + "회 부족'으로 설정");
                }
            } else {
                Log.w(TAG, "updateTLCHeader: headerText=" + (headerText != null) + ", staticAdditionalRequirements=" + (staticAdditionalRequirements != null));
            }
        }

        private void updateChapelHeader(View view) {
            TextView headerText = view.findViewById(R.id.text_chapel_header);

            if (headerText != null && staticAdditionalRequirements != null) {
                int chapelCount = staticAdditionalRequirements.getChapelCount();
                Log.d(TAG, "updateChapelHeader: 채플 횟수 = " + chapelCount);
                if (chapelCount >= 6) {
                    headerText.setText("⛪ 채플 (완료)");
                    Log.d(TAG, "updateChapelHeader: 채플 헤더를 '완료'로 설정");
                } else {
                    int remaining = 6 - chapelCount;
                    headerText.setText("⛪ 채플 (" + remaining + "학기 부족)");
                    Log.d(TAG, "updateChapelHeader: 채플 헤더를 '" + remaining + "학기 부족'으로 설정");
                }
            } else {
                Log.w(TAG, "updateChapelHeader: headerText=" + (headerText != null) + ", staticAdditionalRequirements=" + (staticAdditionalRequirements != null));
            }
        }

        private void updateMileageHeader(View view) {
            TextView headerText = view.findViewById(R.id.text_mileage_header);

            if (headerText != null && staticAdditionalRequirements != null) {
                boolean isCompleted = staticAdditionalRequirements.isMileageCompleted();
                Log.d(TAG, "updateMileageHeader: 마일리지 완료 = " + isCompleted);
                if (isCompleted) {
                    headerText.setText("🏃 1004 마일리지 (완료)");
                    Log.d(TAG, "updateMileageHeader: 마일리지 헤더를 '완료'로 설정");
                } else {
                    headerText.setText("🏃 1004 마일리지 (미완료)");
                    Log.d(TAG, "updateMileageHeader: 마일리지 헤더를 '미완료'로 설정");
                }
            } else {
                Log.w(TAG, "updateMileageHeader: headerText=" + (headerText != null) + ", staticAdditionalRequirements=" + (staticAdditionalRequirements != null));
            }
        }

        private void updateStatusColor(TextView statusView, int current, int required) {
            if (current >= required) {
                statusView.setTextColor(0xFF4CAF50); // 완료 - 녹색
            } else {
                statusView.setTextColor(0xFFFF9800); // 미완료 - 주황색
            }
        }

        private String generateSummaryMessage(int tlcCount, int chapelCount, boolean mileageCompleted, boolean extraGradCompleted) {
            java.util.List<String> remaining = new java.util.ArrayList<>();

            if (tlcCount < 6) {
                remaining.add("TLC " + (6 - tlcCount) + "회");
            }
            if (chapelCount < 6) {
                remaining.add("채플 " + (6 - chapelCount) + "학기");
            }
            if (!mileageCompleted) {
                remaining.add("1004 마일리지");
            }
            if (staticSelectedDepartment != null && !extraGradCompleted) {
                remaining.add("추가 졸업 요건");
            }

            if (remaining.isEmpty()) {
                return "🎉 모든 기타 졸업 요건을 완료했습니다!";
            } else {
                return String.join(", ", remaining) + " 더 완료하면 모든 기타 요건이 충족됩니다.";
            }
        }

        private void loadDepartmentExtraRequirements(View view) {
            if (staticSelectedDepartment == null) {
                return;
            }

            FirebaseDataManager dataManager = FirebaseDataManager.getInstance();
            dataManager.loadExtraGradRequirements(staticSelectedDepartment, new FirebaseDataManager.OnExtraGradRequirementsLoadedListener() {
                @Override
                public void onSuccess(String extraGradRequirement) {
                    if (extraGradRequirement != null && !extraGradRequirement.trim().isEmpty()) {
                        // 동적 요건이 있으면 UI에 추가
                        addExtraRequirementToOthersTab(view, extraGradRequirement);
                        Log.d(TAG, "기타 탭에 추가 졸업 요건 UI 추가 완료: " + extraGradRequirement);
                    } else {
                        Log.d(TAG, "추가 졸업 요건 없음");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "추가 졸업 요건 로드 실패", e);
                }
            });
        }

        private void addExtraRequirementToOthersTab(View view, String requirementName) {
            LinearLayout dynamicLayout = view.findViewById(R.id.layout_dynamic_extra_requirements);
            if (dynamicLayout == null) {
                return;
            }

            // 동적 요건 아코디언 생성
            LinearLayout requirementCard = new LinearLayout(getContext());
            requirementCard.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            requirementCard.setOrientation(LinearLayout.VERTICAL);
            // 기존 아코디언들과 동일한 테마 적용
            requirementCard.setBackgroundResource(R.drawable.spinner_background);

            LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) requirementCard.getLayoutParams();
            cardParams.setMargins(0, 0, 0, dpToPx(12));

            // 헤더 레이아웃
            LinearLayout headerLayout = new LinearLayout(getContext());
            headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            headerLayout.setClickable(true);
            headerLayout.setFocusable(true);
            // selectableItemBackground을 올바르게 가져오기
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
            headerLayout.setBackgroundResource(typedValue.resourceId);

            // 제목
            TextView titleView = new TextView(getContext());
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            titleView.setLayoutParams(titleParams);
            titleView.setText("🎓 " + requirementName);
            titleView.setTextSize(16);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);

            // 상태 텍스트
            TextView statusView = new TextView(getContext());
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statusParams.setMargins(0, 0, dpToPx(12), 0);
            statusView.setLayoutParams(statusParams);
            // 실제 데이터로 상태 설정
            boolean isExtraGradCompleted = (staticAdditionalRequirements != null) ?
                staticAdditionalRequirements.isExtraGradCompleted() : false;
            statusView.setText(isExtraGradCompleted ? "완료" : "미완료");
            statusView.setTextSize(14);
            statusView.setTypeface(null, android.graphics.Typeface.BOLD);
            statusView.setTextColor(isExtraGradCompleted ? 0xFF4CAF50 : 0xFFFF9800);

            // 아이콘
            TextView iconView = new TextView(getContext());
            iconView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            iconView.setText("▶");
            iconView.setTextSize(14);

            headerLayout.addView(titleView);
            headerLayout.addView(statusView);
            headerLayout.addView(iconView);

            // 콘텐츠 레이아웃
            LinearLayout contentLayout = new LinearLayout(getContext());
            contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            contentLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            contentLayout.setBackgroundColor(0xFFF5F5F5);
            contentLayout.setVisibility(View.GONE);

            // 설명 텍스트
            TextView descView = new TextView(getContext());
            descView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            descView.setText(requirementName + " 완료 여부를 확인해주세요.");
            descView.setTextSize(14);
            descView.setTextColor(0xFF000000);

            contentLayout.addView(descView);

            // 아코디언 토글 리스너 설정
            headerLayout.setOnClickListener(v -> toggleAccordion(contentLayout, iconView));

            requirementCard.addView(headerLayout);
            requirementCard.addView(contentLayout);

            dynamicLayout.addView(requirementCard);
            dynamicLayout.setVisibility(View.VISIBLE);
        }

        private int dpToPx(int dp) {
            float density = getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }

    /**
     * 레거시: Firestore에서 대체과목 데이터 로드 (더 이상 사용 안 함)
     *
     * 현재는 통합 졸업요건 시스템에서 GraduationRules.replacementRules로 처리됩니다.
     * 별도의 replacement_courses 컬렉션을 사용하지 않습니다.
     *
     * @deprecated 이 메서드는 더 이상 사용되지 않습니다. GraduationRules 모델의 replacementRules를 사용하세요.
     */
    @Deprecated
    private void loadReplacementCourses(Runnable onComplete) {
        // 레거시 메서드 - 더 이상 사용하지 않음
        Log.d(TAG, "loadReplacementCourses: 레거시 메서드 (사용 안 함, GraduationRules.replacementRules 사용)");

        // 콜백만 실행
        if (onComplete != null) {
            onComplete.run();
        }

        /* 이전 코드 (주석 처리)
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("replacement_courses")
                .whereEqualTo("department", selectedDepartment)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    replacementCoursesMap.clear();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // ReplacementCourse 클래스는 삭제됨 - 현재는 GraduationRules.replacementRules 사용
                    }

                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "대체과목 로드 실패", e);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
        */
    }

    /**
     * 과목 이수 여부 확인 (대체과목 포함)
     * @param requiredCourse 필수 과목명
     * @param takenCourseNames 수강한 과목명 목록
     * @return 이수 여부
     */
    private static boolean isCourseCompleted(String requiredCourse, List<String> takenCourseNames) {
        // 1. 직접 이수한 경우
        if (takenCourseNames.contains(requiredCourse)) {
            return true;
        }

        // 2. 대체과목으로 이수한 경우 확인
        List<String> replacements = replacementCoursesMap.get(requiredCourse);
        if (replacements != null && !replacements.isEmpty()) {
            Log.d(TAG, "대체과목 체크: '" + requiredCourse + "' → 대체 가능: " + replacements);
            for (String replacementCourse : replacements) {
                if (takenCourseNames.contains(replacementCourse)) {
                    Log.d(TAG, "✓ 대체과목 인정: '" + requiredCourse + "' ← '" + replacementCourse + "' 수강으로 인정");
                    return true;
                }
            }
            Log.d(TAG, "✗ 대체과목 미이수: '" + requiredCourse + "' 및 대체과목 모두 미이수");
        }

        return false;
    }

    /**
     * 대체과목으로 인정된 과목명 찾기
     * @param discontinuedCourse 폐지된 과목명
     * @param takenCourseNames 수강한 과목명 목록
     * @return 대체 인정된 과목명, 없으면 null
     */
    private static String getReplacementCourseTaken(String discontinuedCourse, List<String> takenCourseNames) {
        List<String> replacements = replacementCoursesMap.get(discontinuedCourse);
        if (replacements != null) {
            for (String replacementCourse : replacements) {
                if (takenCourseNames.contains(replacementCourse)) {
                    return replacementCourse;
                }
            }
        }
        return null;
    }

    /**
     * users/{userId}/courses 서브컬렉션에 각 과목을 개별 문서로 저장
     * 관리자가 학생 상세 정보를 조회할 때 사용
     */
    private void saveCourseToSubcollection(String userId, java.util.List<java.util.Map<String, Object>> coursesData) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // 기존 courses 서브컬렉션 삭제 후 새로 저장
        db.collection("users")
                .document(userId)
                .collection("courses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 기존 문서 삭제
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }

                    Log.d(TAG, "기존 courses 서브컬렉션 삭제 완료: " + queryDocumentSnapshots.size() + "개");

                    // 새 과목 저장
                    int savedCount = 0;
                    for (java.util.Map<String, Object> courseData : coursesData) {
                        db.collection("users")
                                .document(userId)
                                .collection("courses")
                                .add(courseData)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "과목 저장 성공: " + courseData.get("name"));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "과목 저장 실패: " + courseData.get("name"), e);
                                });
                        savedCount++;
                    }

                    Log.d(TAG, "courses 서브컬렉션에 " + savedCount + "개 과목 저장 시작");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "기존 courses 서브컬렉션 조회 실패", e);
                });
    }

}