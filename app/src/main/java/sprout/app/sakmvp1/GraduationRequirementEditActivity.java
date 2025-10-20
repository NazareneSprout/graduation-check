package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import sprout.app.sakmvp1.adapters.GraduationRequirementPagerAdapter;
import sprout.app.sakmvp1.fragments.CreditRequirementsFragment;
import sprout.app.sakmvp1.fragments.GeneralCoursesFragment;
import sprout.app.sakmvp1.fragments.MajorCoursesFragment;
import sprout.app.sakmvp1.fragments.ReplacementRulesFragment;
import sprout.app.sakmvp1.models.GraduationRules;

/**
 * 졸업요건 편집 Activity (관리자용) - 리팩토링 버전
 *
 * 구조:
 * - TabLayout + ViewPager2로 4개 탭 제공
 * - 각 탭은 Fragment로 구현 (CreditRequirements, MajorCourses, GeneralCourses, ReplacementRules)
 * - 통합 데이터 구조 사용 (graduation_requirements collection)
 * - 각 Fragment에서 "Firestore에서 불러오기" 기능 제공 (기존 데이터 로드 후 부분 수정 가능)
 */
public class GraduationRequirementEditActivity extends AppCompatActivity {

    private static final String TAG = "GradReqEdit";
    public static final String EXTRA_DOCUMENT_ID = "document_id";

    // UI Components
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private View contentLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MaterialButton btnSave;
    private MaterialButton btnDelete;

    // Adapter (public for Fragment access)
    public GraduationRequirementPagerAdapter pagerAdapter;

    // Data
    private FirebaseFirestore db;
    private String documentId;
    private GraduationRules graduationRules;
    private String generalEducationDocId;  // 참조된 교양 문서 ID
    private boolean hasUnsavedChanges = false;  // 저장하지 않은 변경사항이 있는지 추적

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graduation_requirement_edit);

        db = FirebaseFirestore.getInstance();
        documentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);

        if (documentId == null) {
            Toast.makeText(this, "문서 ID가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupViewPager();
        setupListeners();
        loadGraduationRequirement();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("졸업요건 편집");
        }
        toolbar.setNavigationOnClickListener(v -> handleBackPress());
    }

    private void setupViewPager() {
        pagerAdapter = new GraduationRequirementPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // 모든 Fragment를 미리 로드하여 탭 전환 시 지연 제거
        // 4개 탭이 있으므로 3으로 설정 (현재 페이지 + 양쪽 3페이지씩 = 모든 페이지)
        viewPager.setOffscreenPageLimit(3);

        // TabLayout과 ViewPager2 연결
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("학점요건");
                    break;
                case 1:
                    tab.setText("전공과목");
                    break;
                case 2:
                    tab.setText("교양과목");
                    break;
                case 3:
                    tab.setText("대체과목");
                    break;
            }
        }).attach();
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    /**
     * Firestore에서 졸업요건 데이터 로드
     */
    private void loadGraduationRequirement() {
        showLoading(true);

        db.collection("graduation_requirements")
                .document(documentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // v1 구조에서 데이터 읽기
                        String displayName = documentId;

                        // 교양 문서 참조 ID 읽기
                        generalEducationDocId = document.getString("generalEducationDocId");
                        Log.d(TAG, "교양 문서 참조: " + (generalEducationDocId != null ? generalEducationDocId : "없음"));

                        // v1 데이터를 GraduationRules로 변환
                        graduationRules = convertV1ToGraduationRules(document);
                        graduationRules.setSourceDocumentName(displayName);

                        Log.d(TAG, "데이터 로드 성공: " + documentId);
                        bindDataToFragments();
                        showLoading(false);
                    } else {
                        Log.e(TAG, "문서 없음: " + documentId);
                        showLoading(false);
                        Toast.makeText(this, "문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "데이터 로드 실패", e);
                    showLoading(false);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * v1 데이터를 GraduationRules 객체로 변환
     */
    private GraduationRules convertV1ToGraduationRules(com.google.firebase.firestore.DocumentSnapshot document) {
        GraduationRules rules = new GraduationRules();
        rules.setDocId(documentId);

        // documentId에서 정보 추출: IT학부_멀티미디어_2025 형식
        String[] parts = documentId.split("_");
        if (parts.length >= 3) {
            rules.setDepartment(parts[0]);      // IT학부
            rules.setTrack(parts[1]);           // 멀티미디어
            try {
                rules.setCohort(Long.parseLong(parts[2]));  // 2025 (String → long)
            } catch (NumberFormatException e) {
                Log.e(TAG, "학번 파싱 실패: " + parts[2], e);
                rules.setCohort(0);
            }
        } else if (parts.length >= 2) {
            rules.setDepartment(parts[0]);
            rules.setTrack(parts[1]);
        }

        // CreditRequirements 설정
        // v2 구조(creditRequirements 객체) 또는 v1 구조(루트 필드)에서 읽기
        sprout.app.sakmvp1.models.CreditRequirements creditReqs = new sprout.app.sakmvp1.models.CreditRequirements();

        int 전공필수 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "전공필수", 0);
        int 전공선택 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "전공선택", 0);
        int 학부공통 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "학부공통", 0);
        int 전공심화 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "전공심화", 0);
        int 교양필수 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "교양필수", 0);
        int 교양선택 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "교양선택", 0);
        int 소양 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "소양", 0);
        int 일반선택 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "자율선택", 0);
        int 잔여학점 = sprout.app.sakmvp1.utils.GraduationRequirementUtils.getCreditFromRequirements(document, "잔여학점", 0);

        // 총 이수학점 = 모든 카테고리 학점의 합
        int totalCredits = 전공필수 + 전공선택 + 학부공통 + 전공심화 + 교양필수 + 교양선택 + 소양 + 일반선택 + 잔여학점;

        creditReqs.setTotal(totalCredits);
        creditReqs.set전공필수(전공필수);
        creditReqs.set전공선택(전공선택);
        creditReqs.set학부공통(학부공통);
        creditReqs.set전공심화(전공심화);
        creditReqs.set교양필수(교양필수);
        creditReqs.set교양선택(교양선택);
        creditReqs.set소양(소양);
        creditReqs.set일반선택(일반선택);
        creditReqs.set잔여학점(잔여학점);

        rules.setCreditRequirements(creditReqs);

        // rules 데이터 파싱 (전공 과목)
        Object rulesObj = document.get("rules");
        if (rulesObj instanceof java.util.Map) {
            java.util.Map<String, Object> rulesMap = (java.util.Map<String, Object>) rulesObj;
            parseV1RulesData(rulesMap, rules);
        }

        // replacementRules 데이터 파싱 (대체과목)
        Object replacementRulesObj = document.get("replacementRules");
        if (replacementRulesObj instanceof java.util.List) {
            java.util.List<sprout.app.sakmvp1.models.ReplacementRule> replacementRules =
                parseReplacementRules((java.util.List<?>) replacementRulesObj);
            rules.setReplacementRules(replacementRules);
        }

        return rules;
    }

    /**
     * v1 rules 데이터 파싱 (학기별 전공 과목)
     */
    private void parseV1RulesData(java.util.Map<String, Object> rulesMap, GraduationRules rules) {
        java.util.List<sprout.app.sakmvp1.models.RequirementCategory> categories = new java.util.ArrayList<>();

        // 전공필수, 전공선택, 학부공통/전공심화를 저장할 맵
        java.util.Map<String, java.util.List<sprout.app.sakmvp1.models.CourseRequirement>> categoryCoursesMap =
            new java.util.HashMap<>();
        categoryCoursesMap.put("전공필수", new java.util.ArrayList<>());
        categoryCoursesMap.put("전공선택", new java.util.ArrayList<>());
        categoryCoursesMap.put("학부공통", new java.util.ArrayList<>());
        categoryCoursesMap.put("전공심화", new java.util.ArrayList<>());

        // 학기별로 과목 추출
        for (java.util.Map.Entry<String, Object> entry : rulesMap.entrySet()) {
            String key = entry.getKey();

            // 학년학기 키만 처리
            if (key.contains("학년") && entry.getValue() instanceof java.util.Map) {
                java.util.Map<String, Object> semester = (java.util.Map<String, Object>) entry.getValue();

                // 전공필수
                parseV1CourseList(semester.get("전공필수"), key, "전공필수", categoryCoursesMap);

                // 전공선택
                parseV1CourseList(semester.get("전공선택"), key, "전공선택", categoryCoursesMap);

                // 학부공통 (2024-10-19: 학부공통필수 → 학부공통 병합 완료)
                parseV1CourseList(semester.get("학부공통"), key, "학부공통", categoryCoursesMap);

                // 전공심화
                parseV1CourseList(semester.get("전공심화"), key, "전공심화", categoryCoursesMap);
            }
        }

        // 카테고리별로 GraduationRules에 추가
        for (java.util.Map.Entry<String, java.util.List<sprout.app.sakmvp1.models.CourseRequirement>> entry :
                categoryCoursesMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                sprout.app.sakmvp1.models.RequirementCategory category =
                    new sprout.app.sakmvp1.models.RequirementCategory();
                category.setName(entry.getKey());
                category.setCourses(entry.getValue());
                categories.add(category);
                Log.d(TAG, "카테고리 추가: " + entry.getKey() + " - " + entry.getValue().size() + "개 과목");
            }
        }

        rules.setCategories(categories);
    }

    /**
     * v1 과목 리스트 파싱
     */
    private void parseV1CourseList(Object courseListObj, String semester, String categoryName,
                                   java.util.Map<String, java.util.List<sprout.app.sakmvp1.models.CourseRequirement>> categoryCoursesMap) {
        if (!(courseListObj instanceof java.util.List)) {
            return;
        }

        java.util.List<?> courseList = (java.util.List<?>) courseListObj;
        java.util.List<sprout.app.sakmvp1.models.CourseRequirement> targetList = categoryCoursesMap.get(categoryName);

        if (targetList == null) {
            return;
        }

        for (Object courseObj : courseList) {
            if (courseObj instanceof java.util.Map) {
                java.util.Map<String, Object> course = (java.util.Map<String, Object>) courseObj;
                String courseName = (String) course.get("과목명");
                Object creditObj = course.get("학점");
                int credit = (creditObj instanceof Number) ? ((Number) creditObj).intValue() : 3;

                if (courseName != null) {
                    sprout.app.sakmvp1.models.CourseRequirement courseReq =
                        new sprout.app.sakmvp1.models.CourseRequirement(courseName, credit);
                    courseReq.setSemester(semester);
                    targetList.add(courseReq);
                }
            }
        }
    }

    /**
     * replacementRules 데이터 파싱
     */
    private java.util.List<sprout.app.sakmvp1.models.ReplacementRule> parseReplacementRules(java.util.List<?> rulesList) {
        java.util.List<sprout.app.sakmvp1.models.ReplacementRule> replacementRules = new java.util.ArrayList<>();

        for (Object ruleObj : rulesList) {
            if (!(ruleObj instanceof java.util.Map)) {
                continue;
            }

            java.util.Map<String, Object> ruleMap = (java.util.Map<String, Object>) ruleObj;
            sprout.app.sakmvp1.models.ReplacementRule rule = new sprout.app.sakmvp1.models.ReplacementRule();

            // discontinuedCourse 파싱
            Object discontinuedObj = ruleMap.get("discontinuedCourse");
            if (discontinuedObj instanceof java.util.Map) {
                java.util.Map<String, Object> discontinuedMap = (java.util.Map<String, Object>) discontinuedObj;
                sprout.app.sakmvp1.models.ReplacementRule.CourseInfo discontinuedCourse =
                    new sprout.app.sakmvp1.models.ReplacementRule.CourseInfo();
                discontinuedCourse.setName((String) discontinuedMap.get("name"));
                discontinuedCourse.setCategory((String) discontinuedMap.get("category"));
                Object creditsObj = discontinuedMap.get("credits");
                if (creditsObj instanceof Number) {
                    discontinuedCourse.setCredits(((Number) creditsObj).intValue());
                }
                rule.setDiscontinuedCourse(discontinuedCourse);
            }

            // replacementCourses 파싱
            Object replacementCoursesObj = ruleMap.get("replacementCourses");
            if (replacementCoursesObj instanceof java.util.List) {
                java.util.List<?> replacementCoursesList = (java.util.List<?>) replacementCoursesObj;
                java.util.List<sprout.app.sakmvp1.models.ReplacementRule.CourseInfo> replacementCourses =
                    new java.util.ArrayList<>();

                for (Object courseObj : replacementCoursesList) {
                    if (courseObj instanceof java.util.Map) {
                        java.util.Map<String, Object> courseMap = (java.util.Map<String, Object>) courseObj;
                        sprout.app.sakmvp1.models.ReplacementRule.CourseInfo courseInfo =
                            new sprout.app.sakmvp1.models.ReplacementRule.CourseInfo();
                        courseInfo.setName((String) courseMap.get("name"));
                        courseInfo.setCategory((String) courseMap.get("category"));
                        Object creditsObj = courseMap.get("credits");
                        if (creditsObj instanceof Number) {
                            courseInfo.setCredits(((Number) creditsObj).intValue());
                        }
                        replacementCourses.add(courseInfo);
                    }
                }
                rule.setReplacementCourses(replacementCourses);
            }

            // scope 필드 파싱 (없으면 기본값 "document")
            String scope = (String) ruleMap.get("scope");
            rule.setScope(scope != null ? scope : "document");

            replacementRules.add(rule);
        }

        return replacementRules;
    }

    /**
     * 로드된 데이터를 각 Fragment에 바인딩
     */
    private void bindDataToFragments() {
        // Fragment가 아직 생성되지 않았을 수 있으므로 지연 실행
        viewPager.post(() -> {
            CreditRequirementsFragment creditFragment = pagerAdapter.getCreditFragment();
            MajorCoursesFragment majorFragment = pagerAdapter.getMajorFragment();
            GeneralCoursesFragment generalFragment = pagerAdapter.getGeneralFragment();
            ReplacementRulesFragment replacementFragment = pagerAdapter.getReplacementFragment();

            if (creditFragment != null) {
                creditFragment.bindData(graduationRules);
            }
            if (majorFragment != null) {
                majorFragment.bindData(graduationRules);
                Log.d(TAG, "전공 과목 데이터 바인딩 완료");
            }
            if (generalFragment != null) {
                generalFragment.bindData(graduationRules);

                // 교양 문서 ID가 있으면 자동으로 로드
                if (generalEducationDocId != null && !generalEducationDocId.isEmpty()) {
                    Log.d(TAG, "교양 문서 자동 로드 시작: " + generalEducationDocId);
                    generalFragment.loadGeneralCoursesFromDocument(generalEducationDocId);
                }
            } else {
                // Fragment가 아직 생성되지 않았을 경우
                Log.d(TAG, "교양 Fragment 아직 생성 안됨. generalEducationDocId 저장: " + generalEducationDocId);
            }
            if (replacementFragment != null) {
                replacementFragment.bindData(graduationRules);
            }

            // 데이터 로딩이 완료되면 편집 가능 상태로 간주 (변경 가능성이 있음)
            hasUnsavedChanges = true;
        });
    }

    /**
     * 각 Fragment에서 데이터 수집
     */
    private void collectDataFromFragments() {
        if (graduationRules == null) {
            graduationRules = new GraduationRules();
            graduationRules.setDocId(documentId);
        }

        CreditRequirementsFragment creditFragment = pagerAdapter.getCreditFragment();
        MajorCoursesFragment majorFragment = pagerAdapter.getMajorFragment();
        GeneralCoursesFragment generalFragment = pagerAdapter.getGeneralFragment();
        ReplacementRulesFragment replacementFragment = pagerAdapter.getReplacementFragment();

        if (creditFragment != null) {
            creditFragment.updateGraduationRules(graduationRules);
        }
        if (majorFragment != null) {
            majorFragment.updateGraduationRules(graduationRules);
        }
        if (generalFragment != null) {
            generalFragment.updateGraduationRules(graduationRules);
        }
        if (replacementFragment != null) {
            replacementFragment.updateGraduationRules(graduationRules);
        }
    }

    /**
     * 변경사항 저장 - 전공 문서와 교양 문서를 별도로 저장
     */
    private void saveChanges() {
        showLoading(true);

        // 각 Fragment에서 데이터 수집
        collectDataFromFragments();

        if (graduationRules == null) {
            showLoading(false);
            Toast.makeText(this, "데이터 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        // 전공 문서용 업데이트 데이터 준비
        java.util.Map<String, Object> majorUpdateData = new java.util.HashMap<>();

        // 학점 요건 업데이트
        if (graduationRules.getCreditRequirements() != null) {
            sprout.app.sakmvp1.models.CreditRequirements creditReqs = graduationRules.getCreditRequirements();
            majorUpdateData.put("totalCredits", creditReqs.getTotal());
            majorUpdateData.put("전공필수", creditReqs.get전공필수());
            majorUpdateData.put("전공선택", creditReqs.get전공선택());
            majorUpdateData.put("학부공통", creditReqs.get학부공통());
            majorUpdateData.put("전공심화", creditReqs.get전공심화());
            majorUpdateData.put("교양필수", creditReqs.get교양필수());
            majorUpdateData.put("교양선택", creditReqs.get교양선택());
            majorUpdateData.put("소양", creditReqs.get소양());
            majorUpdateData.put("자율선택", creditReqs.get일반선택());
            majorUpdateData.put("잔여학점", creditReqs.get잔여학점());
        }

        // 전공 과목 요건 업데이트 (v1 학기 구조로 변환)
        if (graduationRules.getCategories() != null && !graduationRules.getCategories().isEmpty()) {
            java.util.Map<String, Object> rulesMap = convertCategoriesToV1SemesterStructure(
                graduationRules.getCategories());
            majorUpdateData.put("rules", rulesMap);
        }

        // 대체과목 규칙 업데이트
        if (graduationRules.getReplacementRules() != null && !graduationRules.getReplacementRules().isEmpty()) {
            java.util.List<java.util.Map<String, Object>> rulesList = new java.util.ArrayList<>();
            for (sprout.app.sakmvp1.models.ReplacementRule rule : graduationRules.getReplacementRules()) {
                java.util.Map<String, Object> ruleMap = new java.util.HashMap<>();

                // discontinuedCourse 변환
                if (rule.getDiscontinuedCourse() != null) {
                    java.util.Map<String, Object> discontinuedCourseMap = new java.util.HashMap<>();
                    discontinuedCourseMap.put("name", rule.getDiscontinuedCourse().getName());
                    discontinuedCourseMap.put("category", rule.getDiscontinuedCourse().getCategory());
                    discontinuedCourseMap.put("credits", rule.getDiscontinuedCourse().getCredits());
                    ruleMap.put("discontinuedCourse", discontinuedCourseMap);
                }

                // replacementCourses 변환
                if (rule.getReplacementCourses() != null && !rule.getReplacementCourses().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> replacementCoursesList = new java.util.ArrayList<>();
                    for (sprout.app.sakmvp1.models.ReplacementRule.CourseInfo course : rule.getReplacementCourses()) {
                        java.util.Map<String, Object> courseMap = new java.util.HashMap<>();
                        courseMap.put("name", course.getName());
                        courseMap.put("category", course.getCategory());
                        courseMap.put("credits", course.getCredits());
                        replacementCoursesList.add(courseMap);
                    }
                    ruleMap.put("replacementCourses", replacementCoursesList);
                }

                // scope 필드 저장
                ruleMap.put("scope", rule.getScope() != null ? rule.getScope() : "document");

                rulesList.add(ruleMap);
            }
            majorUpdateData.put("replacementRules", rulesList);
            Log.d(TAG, "대체과목 규칙 저장: " + rulesList.size() + "개");
        }

        // 교양 문서 참조 정보 저장
        GeneralCoursesFragment generalFragment = pagerAdapter.getGeneralFragment();
        String generalDocId = null;
        if (generalFragment != null) {
            generalDocId = generalFragment.getLoadedDocumentName();
            if (generalDocId != null && !generalDocId.isEmpty()) {
                majorUpdateData.put("generalEducationDocId", generalDocId);
                Log.d(TAG, "교양 문서 참조 저장: " + generalDocId);
            }
        }

        majorUpdateData.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        Log.d(TAG, "전공 문서 저장할 데이터: " + majorUpdateData.keySet());

        // 전공 문서 먼저 저장
        final String finalGeneralDocId = generalDocId;
        db.collection("graduation_requirements")
                .document(documentId)
                .update(majorUpdateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "전공 문서 저장 성공: " + documentId);
                    hasUnsavedChanges = false;  // 저장 성공 시 플래그 초기화

                    // 교양 문서 저장
                    saveGeneralEducationDocument(finalGeneralDocId);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "전공 문서 저장 실패", e);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 교양 문서 별도 저장
     */
    private void saveGeneralEducationDocument(String generalDocId) {
        GeneralCoursesFragment generalFragment = pagerAdapter.getGeneralFragment();

        if (generalFragment == null || generalDocId == null || generalDocId.isEmpty()) {
            Log.d(TAG, "교양 문서 저장 조건 불충족 - 완료");
            showLoading(false);
            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        java.util.List<sprout.app.sakmvp1.models.GeneralCourseGroup> generalGroups =
            generalFragment.getGeneralCourseGroups();

        if (generalGroups == null || generalGroups.isEmpty()) {
            Log.d(TAG, "교양과목 데이터 없음 - 완료");
            showLoading(false);
            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 교양과목을 v1 requirements 구조로 변환
        java.util.List<java.util.Map<String, Object>> requirementsList =
            convertGeneralCoursesToV1Requirements(generalGroups);

        Log.d(TAG, "교양과목 변환 완료: " + requirementsList.size() + "개");

        // 교양 문서용 업데이트 데이터
        java.util.Map<String, Object> generalUpdateData = new java.util.HashMap<>();
        java.util.Map<String, Object> rulesMap = new java.util.HashMap<>();
        rulesMap.put("requirements", requirementsList);
        generalUpdateData.put("rules", rulesMap);
        generalUpdateData.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        Log.d(TAG, "교양 문서 저장 시작: " + generalDocId);

        // 교양 문서 저장
        db.collection("graduation_requirements")
                .document(generalDocId)
                .update(generalUpdateData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Log.d(TAG, "교양 문서 저장 성공: " + generalDocId);
                    Toast.makeText(this, "저장되었습니다 (전공 + 교양)", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "교양 문서 저장 실패: " + generalDocId, e);
                    Toast.makeText(this, "교양 문서 저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * GeneralCourseGroup 리스트를 v1 requirements 구조로 변환
     * 단일 과목: {name: "과목명", credit: 3}
     * 선택 과목: {type: "oneOf", options: [{name: "과목1"}, {name: "과목2"}], credit: 3}
     */
    private java.util.List<java.util.Map<String, Object>> convertGeneralCoursesToV1Requirements(
            java.util.List<sprout.app.sakmvp1.models.GeneralCourseGroup> groups) {

        java.util.List<java.util.Map<String, Object>> requirementsList = new java.util.ArrayList<>();

        for (sprout.app.sakmvp1.models.GeneralCourseGroup group : groups) {
            java.util.Map<String, Object> requirementMap = new java.util.HashMap<>();

            if (group.isSingle()) {
                // 단일 과목: {name: "과목명", credit: 3}
                requirementMap.put("name", group.getCourseNames().get(0));
                requirementMap.put("credit", group.getCredit());
            } else if (group.getType() == sprout.app.sakmvp1.models.GeneralCourseGroup.Type.ONE_OF) {
                // 선택 과목 (택1): {type: "oneOf", options: [{name: "과목1"}, ...], credit: 3}
                requirementMap.put("type", "oneOf");
                requirementMap.put("credit", group.getCredit());

                java.util.List<java.util.Map<String, Object>> optionsList = new java.util.ArrayList<>();
                for (String courseName : group.getCourseNames()) {
                    java.util.Map<String, Object> optionMap = new java.util.HashMap<>();
                    optionMap.put("name", courseName);
                    optionsList.add(optionMap);
                }
                requirementMap.put("options", optionsList);
            }

            requirementsList.add(requirementMap);
            Log.d(TAG, "교양과목 추가: " + (group.isSingle() ? "단일 - " + group.getCourseNames().get(0) :
                "택1 - " + String.join(", ", group.getCourseNames())));
        }

        return requirementsList;
    }

    /**
     * Categories 데이터를 v1 학기 구조로 변환
     * 예: rules.{학기}.{카테고리} = [{과목명, 학점}, ...]
     */
    private java.util.Map<String, Object> convertCategoriesToV1SemesterStructure(
            java.util.List<sprout.app.sakmvp1.models.RequirementCategory> categories) {

        java.util.Map<String, Object> rulesMap = new java.util.HashMap<>();

        // 학기별로 그룹화 (1학년 1학기 ~ 4학년 2학기)
        String[] semesters = {
            "1학년 1학기", "1학년 2학기",
            "2학년 1학기", "2학년 2학기",
            "3학년 1학기", "3학년 2학기",
            "4학년 1학기", "4학년 2학기"
        };

        // 각 학기별 데이터 초기화
        for (String semester : semesters) {
            java.util.Map<String, Object> semesterMap = new java.util.HashMap<>();
            semesterMap.put("전공필수", new java.util.ArrayList<>());
            semesterMap.put("전공선택", new java.util.ArrayList<>());
            semesterMap.put("학부공통", new java.util.ArrayList<>()); // 2024-10-19: 학부공통필수 → 학부공통 병합 완료
            semesterMap.put("전공심화", new java.util.ArrayList<>());
            rulesMap.put(semester, semesterMap);
        }

        // 카테고리별로 과목 분류하여 학기에 배치
        for (sprout.app.sakmvp1.models.RequirementCategory category : categories) {
            String categoryName = category.getName();
            java.util.List<sprout.app.sakmvp1.models.CourseRequirement> courses = category.getCourses();

            if (courses == null || courses.isEmpty()) {
                continue;
            }

            // v1 카테고리명으로 매핑 (2024-10-19: 학부공통필수 → 학부공통 병합 완료)
            String v1CategoryName;
            if ("전공필수".equals(categoryName)) {
                v1CategoryName = "전공필수";
            } else if ("전공선택".equals(categoryName)) {
                v1CategoryName = "전공선택";
            } else if ("학부공통".equals(categoryName)) {
                v1CategoryName = "학부공통";
            } else if ("전공심화".equals(categoryName)) {
                v1CategoryName = "전공심화";
            } else {
                continue; // 교양 과목은 rules에 저장하지 않음
            }

            for (sprout.app.sakmvp1.models.CourseRequirement course : courses) {
                String semester = course.getSemester();
                if (semester == null || semester.isEmpty()) {
                    Log.w(TAG, "과목 " + course.getName() + "의 학기 정보가 없습니다. 건너뜁니다.");
                    continue;
                }

                // 해당 학기의 카테고리 리스트 가져오기
                java.util.Map<String, Object> semesterMap =
                    (java.util.Map<String, Object>) rulesMap.get(semester);

                if (semesterMap != null) {
                    java.util.List<java.util.Map<String, Object>> courseList =
                        (java.util.List<java.util.Map<String, Object>>) semesterMap.get(v1CategoryName);

                    if (courseList != null) {
                        java.util.Map<String, Object> courseMap = new java.util.HashMap<>();
                        courseMap.put("과목명", course.getName());
                        courseMap.put("학점", course.getCredits());
                        courseList.add(courseMap);

                        Log.d(TAG, "과목 추가: " + semester + " / " + v1CategoryName + " / " + course.getName());
                    }
                }
            }
        }

        return rulesMap;
    }

    /**
     * GraduationRules를 Map으로 변환 (저장용)
     */
    private java.util.Map<String, Object> convertGraduationRulesToMap(GraduationRules rules) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();

        // 기본 정보
        data.put("docId", rules.getDocId());
        data.put("sourceDocumentName", rules.getSourceDocumentName());
        data.put("cohort", rules.getCohort());
        data.put("department", rules.getDepartment());
        data.put("track", rules.getTrack());

        // 학점 요건
        if (rules.getCreditRequirements() != null) {
            sprout.app.sakmvp1.models.CreditRequirements creditReqs = rules.getCreditRequirements();
            java.util.Map<String, Object> creditData = new java.util.HashMap<>();
            creditData.put("total", creditReqs.getTotal());
            creditData.put("전공필수", creditReqs.get전공필수());
            creditData.put("전공선택", creditReqs.get전공선택());
            creditData.put("학부공통", creditReqs.get학부공통());
            creditData.put("전공심화", creditReqs.get전공심화());
            creditData.put("교양필수", creditReqs.get교양필수());
            creditData.put("교양선택", creditReqs.get교양선택());
            creditData.put("소양", creditReqs.get소양());
            creditData.put("일반선택", creditReqs.get일반선택());
            creditData.put("잔여학점", creditReqs.get잔여학점());
            data.put("creditRequirements", creditData);
        }

        // 과목 요건 (categories)
        if (rules.getCategories() != null && !rules.getCategories().isEmpty()) {
            java.util.List<java.util.Map<String, Object>> categoriesList = new java.util.ArrayList<>();
            for (sprout.app.sakmvp1.models.RequirementCategory category : rules.getCategories()) {
                java.util.Map<String, Object> categoryMap = new java.util.HashMap<>();
                categoryMap.put("name", category.getName());

                if (category.getCourses() != null && !category.getCourses().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> coursesList = new java.util.ArrayList<>();
                    for (sprout.app.sakmvp1.models.CourseRequirement course : category.getCourses()) {
                        java.util.Map<String, Object> courseMap = new java.util.HashMap<>();
                        courseMap.put("name", course.getName());
                        courseMap.put("credit", course.getCredits());
                        coursesList.add(courseMap);
                    }
                    categoryMap.put("courses", coursesList);
                }

                categoriesList.add(categoryMap);
            }
            data.put("categories", categoriesList);
        }

        // 대체 규칙 (replacementRules)
        if (rules.getReplacementRules() != null && !rules.getReplacementRules().isEmpty()) {
            java.util.List<java.util.Map<String, Object>> rulesList = new java.util.ArrayList<>();
            for (sprout.app.sakmvp1.models.ReplacementRule rule : rules.getReplacementRules()) {
                java.util.Map<String, Object> ruleMap = new java.util.HashMap<>();

                // discontinuedCourse 변환
                if (rule.getDiscontinuedCourse() != null) {
                    java.util.Map<String, Object> discontinuedCourseMap = new java.util.HashMap<>();
                    discontinuedCourseMap.put("name", rule.getDiscontinuedCourse().getName());
                    discontinuedCourseMap.put("category", rule.getDiscontinuedCourse().getCategory());
                    discontinuedCourseMap.put("credits", rule.getDiscontinuedCourse().getCredits());
                    ruleMap.put("discontinuedCourse", discontinuedCourseMap);
                }

                // replacementCourses 변환
                if (rule.getReplacementCourses() != null && !rule.getReplacementCourses().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> replacementCoursesList = new java.util.ArrayList<>();
                    for (sprout.app.sakmvp1.models.ReplacementRule.CourseInfo course : rule.getReplacementCourses()) {
                        java.util.Map<String, Object> courseMap = new java.util.HashMap<>();
                        courseMap.put("name", course.getName());
                        courseMap.put("category", course.getCategory());
                        courseMap.put("credits", course.getCredits());
                        replacementCoursesList.add(courseMap);
                    }
                    ruleMap.put("replacementCourses", replacementCoursesList);
                }

                rulesList.add(ruleMap);
            }
            data.put("replacementRules", rulesList);
        }

        Log.d(TAG, "GraduationRules를 Map으로 변환 완료");

        return data;
    }

    /**
     * 로딩 상태 표시/숨김
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (viewPager != null) {
            viewPager.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (btnSave != null) {
            btnSave.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (btnDelete != null) {
            btnDelete.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Fragment에서 데이터를 가져올 수 있도록 public getter 제공
     */
    public GraduationRules getGraduationRules() {
        return graduationRules;
    }

    /**
     * 교양 문서 ID 반환 (Fragment가 나중에 생성될 때 사용)
     */
    public String getGeneralEducationDocId() {
        return generalEducationDocId;
    }

    /**
     * Fragment에서 데이터 변경을 알릴 수 있도록 public 메서드 제공
     */
    public void markAsModified() {
        hasUnsavedChanges = true;
    }

    /**
     * Back 버튼 처리 (저장하지 않은 변경사항이 있으면 경고 다이얼로그 표시)
     */
    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    /**
     * 뒤로가기 처리 로직 (저장 안 된 변경사항 확인)
     */
    private void handleBackPress() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    /**
     * 저장하지 않은 변경사항 경고 다이얼로그
     */
    private void showUnsavedChangesDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("변경사항이 저장되지 않았습니다")
                .setMessage("저장하지 않고 나가면 변경사항이 반영되지 않습니다.\n정말 나가시겠습니까?")
                .setNegativeButton("나가기", (dialog, which) -> {
                    hasUnsavedChanges = false;  // 플래그 초기화
                    finish();
                })
                .setPositiveButton("취소", null)
                .show();
    }

    /**
     * 삭제 확인 다이얼로그
     */
    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("졸업요건 삭제")
                .setMessage("정말로 이 졸업요건 문서를 삭제하시겠습니까?\n\n문서 ID: " + documentId + "\n\n이 작업은 되돌릴 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> deleteDocument())
                .show();
    }

    /**
     * Firestore에서 문서 삭제
     */
    private void deleteDocument() {
        showLoading(true);

        db.collection("graduation_requirements")
                .document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "문서 삭제 성공: " + documentId);
                    showLoading(false);
                    Toast.makeText(this, "졸업요건이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    hasUnsavedChanges = false;  // 플래그 초기화
                    finish();  // 액티비티 종료
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "문서 삭제 실패: " + documentId, e);
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
