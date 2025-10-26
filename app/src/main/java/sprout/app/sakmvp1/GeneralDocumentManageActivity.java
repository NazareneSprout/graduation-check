package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import sprout.app.sakmvp1.adapters.CourseEditAdapter;
import sprout.app.sakmvp1.models.CourseRequirement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 교양 문서 관리 Activity (관리자용)
 * - 기존 교양 문서 선택 또는 새로 생성
 * - 단일과목/복수과목 중 택1 과목 추가/수정/삭제
 * - Firestore에 저장
 */
public class GeneralDocumentManageActivity extends AppCompatActivity {

    private static final String TAG = "GeneralDocManage";

    // UI Components
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private View contentLayout;

    private Spinner spinnerDocuments;
    private MaterialButton btnSave;

    // 과목 목록
    private RecyclerView rvSingleCourses;
    private RecyclerView rvOptionGroups;
    private CourseEditAdapter adapterSingleCourses;
    private CourseEditAdapter adapterOptionGroups;

    private MaterialButton btnAddSingleCourse;
    private MaterialButton btnAddOptionGroup;

    // Data
    private FirebaseFirestore db;
    private List<String> documentIds = new ArrayList<>();
    private String currentDocumentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_document_manage);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupListeners();
        loadDocumentList();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);

        spinnerDocuments = findViewById(R.id.spinner_documents);
        btnSave = findViewById(R.id.btn_save);

        rvSingleCourses = findViewById(R.id.rv_single_courses);
        rvOptionGroups = findViewById(R.id.rv_option_groups);

        btnAddSingleCourse = findViewById(R.id.btn_add_single_course);
        btnAddOptionGroup = findViewById(R.id.btn_add_option_group);
    }

    private void setupToolbar() {
        // Edge-to-edge 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("교양 문서 관리");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        adapterSingleCourses = new CourseEditAdapter();
        rvSingleCourses.setLayoutManager(new LinearLayoutManager(this));
        rvSingleCourses.setAdapter(adapterSingleCourses);
        adapterSingleCourses.setOnCourseActionListener((course, position) ->
            showDeleteCourseDialog("단일과목", position, adapterSingleCourses));

        adapterOptionGroups = new CourseEditAdapter();
        rvOptionGroups.setLayoutManager(new LinearLayoutManager(this));
        rvOptionGroups.setAdapter(adapterOptionGroups);
        adapterOptionGroups.setOnCourseActionListener((course, position) ->
            showDeleteCourseDialog("복수과목 중 택1", position, adapterOptionGroups));
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> showSaveDialog());

        btnAddSingleCourse.setOnClickListener(v -> showAddCourseDialog("단일"));
        btnAddOptionGroup.setOnClickListener(v -> showAddCourseDialog("선택"));

        // 스피너 선택 리스너
        spinnerDocuments.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < documentIds.size()) {
                    String selectedDoc = documentIds.get(position);
                    loadDocument(selectedDoc);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    /**
     * 문서 목록 로드
     */
    private void loadDocumentList() {
        showLoading(true);

        db.collection("graduation_requirements")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                documentIds.clear();
                List<String> displayNames = new ArrayList<>();

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String docId = document.getId();

                    // 교양 문서만 포함
                    if (docId.startsWith("교양_")) {
                        documentIds.add(docId);
                        displayNames.add(docId);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, displayNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDocuments.setAdapter(adapter);

                showLoading(false);
                Log.d(TAG, "교양 문서 " + documentIds.size() + "개 로드");

                // 첫 번째 문서 자동 로드
                if (!documentIds.isEmpty()) {
                    loadDocument(documentIds.get(0));
                }
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Log.e(TAG, "문서 목록 로드 실패", e);
                Toast.makeText(this, "문서 목록 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 문서 로드
     */
    private void loadDocument(String documentId) {
        showLoading(true);
        currentDocumentId = documentId;

        db.collection("graduation_requirements")
            .document(documentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    loadCourses(documentSnapshot.getData());
                }
                showLoading(false);
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Log.e(TAG, "문서 로드 실패", e);
                Toast.makeText(this, "문서 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 과목 데이터 로드 - rules.requirements 필드에서 데이터 파싱
     */
    private void loadCourses(Map<String, Object> data) {
        if (data == null) return;

        // rules 필드에서 데이터 읽기
        Object rulesObj = data.get("rules");
        if (!(rulesObj instanceof Map)) {
            Log.w(TAG, "rules 필드가 없거나 Map이 아닙니다");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rulesMap = (Map<String, Object>) rulesObj;

        // requirements 리스트 읽기
        Object requirementsObj = rulesMap.get("requirements");
        if (!(requirementsObj instanceof List)) {
            Log.w(TAG, "requirements 필드가 없거나 List가 아닙니다");
            return;
        }

        @SuppressWarnings("unchecked")
        List<?> requirementsList = (List<?>) requirementsObj;

        // 카테고리별 과목 리스트
        List<CourseRequirement> singleCourses = new ArrayList<>();
        List<CourseRequirement> optionGroups = new ArrayList<>();

        // requirements에서 과목 추출
        for (Object reqObj : requirementsList) {
            if (!(reqObj instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> requirement = (Map<String, Object>) reqObj;

            Object creditObj = requirement.get("credit");
            int credit = (creditObj instanceof Number) ? ((Number) creditObj).intValue() : 3;

            // options가 있으면 (복수과목 중 택1)
            if (requirement.containsKey("options")) {
                Object optionsObj = requirement.get("options");
                if (optionsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<?> options = (List<?>) optionsObj;

                    // 옵션 과목명들을 쉼표로 연결
                    StringBuilder optionNames = new StringBuilder();
                    for (int i = 0; i < options.size(); i++) {
                        if (options.get(i) instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> option = (Map<String, Object>) options.get(i);
                            String courseName = (String) option.get("name");
                            if (courseName != null) {
                                if (i > 0) optionNames.append(", ");
                                optionNames.append(courseName);
                            }
                        }
                    }

                    if (optionNames.length() > 0) {
                        CourseRequirement course = new CourseRequirement(optionNames.toString(), credit);
                        optionGroups.add(course);
                    }
                }
            } else {
                // 단일 과목
                String courseName = (String) requirement.get("name");
                if (courseName != null) {
                    CourseRequirement course = new CourseRequirement(courseName, credit);
                    singleCourses.add(course);
                }
            }
        }

        // 어댑터에 과목 설정
        adapterSingleCourses.setCourses(singleCourses);
        adapterOptionGroups.setCourses(optionGroups);

        Log.d(TAG, "과목 로드 완료: 단일=" + adapterSingleCourses.getItemCount() +
            ", 선택=" + adapterOptionGroups.getItemCount());
    }

    /**
     * 과목 리스트 파싱 헬퍼 메소드
     */
    private void parseCourseList(Object courseListObj, List<CourseRequirement> targetList) {
        if (!(courseListObj instanceof List)) {
            return;
        }

        List<?> courseList = (List<?>) courseListObj;
        for (Object courseObj : courseList) {
            if (courseObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> course = (Map<String, Object>) courseObj;
                String courseName = (String) course.get("과목명");
                Object creditObj = course.get("학점");
                int credit = (creditObj instanceof Number) ? ((Number) creditObj).intValue() : 3;

                if (courseName != null) {
                    targetList.add(new CourseRequirement(courseName, credit));
                }
            }
        }
    }

    /**
     * 저장 다이얼로그 - 새 문서 만들기 or 기존 문서 수정하기 선택
     */
    private void showSaveDialog() {
        // 먼저 "새 문서 만들기" vs "기존 문서 수정하기" 선택
        String[] options = {"새 교양문서 만들기", "기존 교양문서 수정하기"};

        new MaterialAlertDialogBuilder(this)
            .setTitle("저장 방식 선택")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // 새 문서 만들기
                    showCreateNewDocumentDialog();
                } else {
                    // 기존 문서 수정하기
                    showUpdateExistingDocumentDialog();
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 새 교양 문서 만들기 다이얼로그
     */
    private void showCreateNewDocumentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_general_document, null);

        EditText etCohort = dialogView.findViewById(R.id.et_cohort);

        // 현재 문서 정보로 초기값 설정
        if (currentDocumentId != null) {
            String[] parts = currentDocumentId.split("_");
            if (parts.length >= 3) {
                // "교양_공통_2020" 형식
                String cohortStr = parts[2];
                if (cohortStr.length() == 4) {
                    etCohort.setText(cohortStr.substring(2)); // 2026 -> 26
                }
            } else if (parts.length >= 2) {
                // "교양_2020" 형식
                String cohortStr = parts[1];
                if (cohortStr.length() == 4) {
                    etCohort.setText(cohortStr.substring(2)); // 2026 -> 26
                }
            }
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("새 교양문서 만들기")
            .setMessage("현재 과목 목록을 새로운 교양 문서로 저장합니다")
            .setView(dialogView)
            .setPositiveButton("생성", (dialog, which) -> {
                String cohortStr = etCohort.getText().toString().trim();

                if (cohortStr.isEmpty()) {
                    Toast.makeText(this, "학번을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                int cohort;
                try {
                    cohort = Integer.parseInt(cohortStr);
                    if (cohort < 100) {
                        cohort = 2000 + cohort; // 2자리 -> 4자리 변환
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "올바른 학번을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 문서 ID 생성: "교양_공통_2026" 형식
                String newDocId = "교양_공통_" + cohort;

                Log.d(TAG, "새 교양 문서 생성: " + newDocId);
                saveDocumentWithId(newDocId);
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 기존 교양 문서 수정하기
     */
    private void showUpdateExistingDocumentDialog() {
        if (currentDocumentId == null || currentDocumentId.isEmpty()) {
            Toast.makeText(this, "수정할 문서가 선택되지 않았습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("기존 문서 수정")
            .setMessage("'" + currentDocumentId + "' 문서를 수정하시겠습니까?")
            .setPositiveButton("수정", (dialog, which) -> {
                Log.d(TAG, "기존 교양 문서 수정: " + currentDocumentId);
                saveDocumentWithId(currentDocumentId);
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 과목 추가 다이얼로그
     */
    private void showAddCourseDialog(String type) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_general_course_input, null);

        android.widget.RadioGroup rgCourseType = dialogView.findViewById(R.id.rg_course_type);
        android.widget.RadioButton rbSingle = dialogView.findViewById(R.id.rb_single);
        android.widget.RadioButton rbOneOf = dialogView.findViewById(R.id.rb_one_of);
        android.widget.LinearLayout layoutSingleCourse = dialogView.findViewById(R.id.layout_single_course);
        android.widget.LinearLayout layoutMultipleCourses = dialogView.findViewById(R.id.layout_multiple_courses);
        EditText etSingleCourseName = dialogView.findViewById(R.id.et_single_course_name);
        EditText etMultipleCourseNames = dialogView.findViewById(R.id.et_multiple_course_names);
        EditText etCourseCredit = dialogView.findViewById(R.id.et_course_credit);

        // 타입에 따라 라디오 버튼 초기 선택
        if ("단일".equals(type)) {
            rbSingle.setChecked(true);
            layoutSingleCourse.setVisibility(View.VISIBLE);
            layoutMultipleCourses.setVisibility(View.GONE);
        } else {
            rbOneOf.setChecked(true);
            layoutSingleCourse.setVisibility(View.GONE);
            layoutMultipleCourses.setVisibility(View.VISIBLE);
        }

        // 라디오 버튼 변경 리스너
        rgCourseType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_single) {
                layoutSingleCourse.setVisibility(View.VISIBLE);
                layoutMultipleCourses.setVisibility(View.GONE);
            } else {
                layoutSingleCourse.setVisibility(View.GONE);
                layoutMultipleCourses.setVisibility(View.VISIBLE);
            }
        });

        new MaterialAlertDialogBuilder(this)
            .setTitle("과목 추가")
            .setView(dialogView)
            .setPositiveButton("추가", (dialog, which) -> {
                String creditStr = etCourseCredit.getText().toString().trim();

                int credit;
                try {
                    credit = Integer.parseInt(creditStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "학점을 올바르게 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (rbSingle.isChecked()) {
                    // 단일 과목
                    String courseName = etSingleCourseName.getText().toString().trim();
                    if (courseName.isEmpty()) {
                        Toast.makeText(this, "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CourseRequirement newCourse = new CourseRequirement(courseName, credit);
                    adapterSingleCourses.addCourse(newCourse);
                } else {
                    // 복수 과목 중 택1
                    String courseNames = etMultipleCourseNames.getText().toString().trim();
                    if (courseNames.isEmpty()) {
                        Toast.makeText(this, "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CourseRequirement newCourse = new CourseRequirement(courseNames, credit);
                    adapterOptionGroups.addCourse(newCourse);
                }

                Toast.makeText(this, "과목이 추가되었습니다", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 과목 삭제 다이얼로그
     */
    private void showDeleteCourseDialog(String categoryName, int position, CourseEditAdapter adapter) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("과목 삭제")
            .setMessage("이 과목을 삭제하시겠습니까?")
            .setPositiveButton("삭제", (dialog, which) -> {
                adapter.removeCourse(position);
                Toast.makeText(this, "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 문서 저장 - 마이그레이션 후 구조 (v3)
     * 교양 문서는 과목 목록만 포함, 학점 정보는 제거
     */
    private void saveDocumentWithId(String docId) {
        showLoading(true);

        // 문서 ID에서 cohort 추출: "교양_공통_2020" 또는 "교양_2020"
        String[] parts = docId.split("_");
        String department = "공통"; // 교양은 기본적으로 "공통"
        int cohort = 0;

        if (parts.length >= 3) {
            // "교양_공통_2020" 형식
            department = parts[1];
            try {
                cohort = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "학번 파싱 실패: " + parts[2]);
            }
        } else if (parts.length >= 2) {
            // "교양_2020" 형식
            try {
                cohort = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "학번 파싱 실패: " + parts[1]);
            }
        }

        // rules.requirements 데이터 생성
        List<Map<String, Object>> requirements = new ArrayList<>();

        // 단일 과목 추가
        for (CourseRequirement course : adapterSingleCourses.getCourses()) {
            Map<String, Object> requirement = new HashMap<>();
            requirement.put("name", course.getName());
            requirement.put("credit", course.getCredits());
            requirements.add(requirement);
        }

        // 복수 과목 중 택1 추가
        for (CourseRequirement courseGroup : adapterOptionGroups.getCourses()) {
            Map<String, Object> requirement = new HashMap<>();
            requirement.put("credit", courseGroup.getCredits());

            // 쉼표로 구분된 과목명들을 options 리스트로 변환
            String[] courseNames = courseGroup.getName().split(",\\s*");
            List<Map<String, Object>> options = new ArrayList<>();
            for (String name : courseNames) {
                Map<String, Object> option = new HashMap<>();
                option.put("name", name.trim());
                options.add(option);
            }
            requirement.put("options", options);
            requirements.add(requirement);
        }

        // rules 데이터
        Map<String, Object> rules = new HashMap<>();
        rules.put("requirements", requirements);

        // ✅ v3 교양 문서 구조 (과목만 포함, 학점 정보 제거)
        Map<String, Object> documentData = new HashMap<>();
        documentData.put("docType", "general");
        documentData.put("department", department);
        documentData.put("cohort", cohort);
        documentData.put("version", "v3");
        documentData.put("updatedAt", com.google.firebase.Timestamp.now());
        documentData.put("rules", rules);

        Log.d(TAG, "v3 교양 문서 생성: " + docId);
        Log.d(TAG, "  - docType: general");
        Log.d(TAG, "  - department: " + department + ", cohort: " + cohort);
        Log.d(TAG, "  - rules 포함 (requirements)");
        Log.d(TAG, "  - ❌ 학점요건 제거 (졸업요건 문서에만 존재)");

        // Firestore에 저장
        db.collection("graduation_requirements")
            .document(docId)
            .set(documentData)
            .addOnSuccessListener(aVoid -> {
                showLoading(false);
                Log.d(TAG, "교양 문서 저장 성공 (v3): " + docId);
                Toast.makeText(this, "교양 문서가 저장되었습니다: " + docId, Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Log.e(TAG, "교양 문서 저장 실패: " + docId, e);
                Toast.makeText(this, "저장 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * CourseRequirement 리스트를 Map 리스트로 변환
     */
    private List<Map<String, Object>> convertCoursesToMap(List<CourseRequirement> courses) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseRequirement course : courses) {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("name", course.getName());
            courseMap.put("credits", course.getCredits());
            result.add(courseMap);
        }
        return result;
    }

    /**
     * 모든 과목 초기화
     */
    private void clearAllCourses() {
        adapterSingleCourses.clearCourses();
        adapterOptionGroups.clearCourses();
    }

    /**
     * 로딩 상태 표시/숨김
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
