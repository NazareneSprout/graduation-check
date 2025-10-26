package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import sprout.app.sakmvp1.models.RequirementCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 전공 문서 관리 Activity (관리자용)
 * - 기존 전공 문서 선택 또는 새로 생성
 * - 전공필수/전공선택/학부공통(또는 전공심화) 과목 추가/수정/삭제
 * - Firestore에 저장
 */
public class MajorDocumentManageActivity extends AppCompatActivity {

    private static final String TAG = "MajorDocManage";

    // UI Components
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private View contentLayout;

    private Spinner spinnerDocuments;
    private MaterialButton btnSave;

    // 과목 목록
    private RecyclerView rvMajorRequired;
    private RecyclerView rvMajorElective;
    private RecyclerView rvDeptCommon;
    private CourseEditAdapter adapterMajorRequired;
    private CourseEditAdapter adapterMajorElective;
    private CourseEditAdapter adapterDeptCommon;

    private MaterialButton btnAddMajorRequired;
    private MaterialButton btnAddMajorElective;
    private MaterialButton btnAddDeptCommon;

    // Data
    private FirebaseFirestore db;
    private List<String> documentIds = new ArrayList<>();
    private String currentDocumentId;
    private String deptCommonCategoryName = "학부공통"; // 또는 "전공심화"
    private Map<String, Object> currentCreditRequirements; // 현재 문서의 학점요건

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_major_document_manage);

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

        rvMajorRequired = findViewById(R.id.rv_major_required);
        rvMajorElective = findViewById(R.id.rv_major_elective);
        rvDeptCommon = findViewById(R.id.rv_dept_common);

        btnAddMajorRequired = findViewById(R.id.btn_add_major_required);
        btnAddMajorElective = findViewById(R.id.btn_add_major_elective);
        btnAddDeptCommon = findViewById(R.id.btn_add_dept_common);
    }

    private void setupToolbar() {
        // Edge-to-edge 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("전공 문서 관리");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        adapterMajorRequired = new CourseEditAdapter();
        rvMajorRequired.setLayoutManager(new LinearLayoutManager(this));
        rvMajorRequired.setAdapter(adapterMajorRequired);
        adapterMajorRequired.setOnCourseActionListener((course, position) ->
            showDeleteCourseDialog("전공필수", position, adapterMajorRequired));

        adapterMajorElective = new CourseEditAdapter();
        rvMajorElective.setLayoutManager(new LinearLayoutManager(this));
        rvMajorElective.setAdapter(adapterMajorElective);
        adapterMajorElective.setOnCourseActionListener((course, position) ->
            showDeleteCourseDialog("전공선택", position, adapterMajorElective));

        adapterDeptCommon = new CourseEditAdapter();
        rvDeptCommon.setLayoutManager(new LinearLayoutManager(this));
        rvDeptCommon.setAdapter(adapterDeptCommon);
        adapterDeptCommon.setOnCourseActionListener((course, position) ->
            showDeleteCourseDialog(deptCommonCategoryName, position, adapterDeptCommon));
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> showSaveDialog());

        btnAddMajorRequired.setOnClickListener(v -> showAddCourseDialog("전공필수"));
        btnAddMajorElective.setOnClickListener(v -> showAddCourseDialog("전공선택"));
        btnAddDeptCommon.setOnClickListener(v -> showAddCourseDialog(deptCommonCategoryName));

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

                    // 교양 문서와 졸업요건 문서는 제외 (전공 문서만 표시)
                    if (docId.startsWith("교양_") || docId.startsWith("졸업요건_")) {
                        continue;
                    }

                    documentIds.add(docId);
                    displayNames.add(docId);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, displayNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDocuments.setAdapter(adapter);

                showLoading(false);
                Log.d(TAG, "전공 문서 " + documentIds.size() + "개 로드");

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
                    // 카테고리 타입 확인
                    String categoryType = documentSnapshot.getString("majorCategoryType");
                    if (categoryType != null) {
                        deptCommonCategoryName = categoryType;
                    }

                    // 학점요건 데이터 저장 - 문서의 한글 필드명들을 직접 읽음
                    currentCreditRequirements = new HashMap<>();
                    String[] creditFields = {"전공필수", "전공선택", "교양필수", "교양선택", "소양", "학부공통", "일반선택", "전공심화", "잔여학점", "총이수"};

                    for (String field : creditFields) {
                        Object value = documentSnapshot.get(field);
                        if (value != null) {
                            currentCreditRequirements.put(field, value);
                        }
                    }

                    if (currentCreditRequirements.isEmpty()) {
                        currentCreditRequirements = null;
                        Log.d(TAG, "학점요건 데이터 없음");
                    } else {
                        Log.d(TAG, "학점요건 데이터 로드됨: " + currentCreditRequirements);
                    }

                    // 과목 로드
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
     * 과목 데이터 로드 - rules 필드에서 학기별 과목 파싱
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

        // 카테고리별 과목을 모을 리스트
        List<CourseRequirement> majorRequiredCourses = new ArrayList<>();
        List<CourseRequirement> majorElectiveCourses = new ArrayList<>();
        List<CourseRequirement> deptCommonCourses = new ArrayList<>();

        // 학기별로 과목 추출
        for (Map.Entry<String, Object> entry : rulesMap.entrySet()) {
            String key = entry.getKey();

            // 학년학기 키만 처리 (예: "1학년1학기", "2학년2학기")
            if (key.contains("학년") && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> semester = (Map<String, Object>) entry.getValue();

                // 전공필수
                parseCourseList(semester.get("전공필수"), majorRequiredCourses);

                // 전공선택
                parseCourseList(semester.get("전공선택"), majorElectiveCourses);

                // 학부공통 (2024-10-19: 학부공통필수 → 학부공통 병합 완료)
                parseCourseList(semester.get("학부공통"), deptCommonCourses);

                // 전공심화
                parseCourseList(semester.get("전공심화"), deptCommonCourses);
            }
        }

        // 어댑터에 과목 설정
        adapterMajorRequired.setCourses(majorRequiredCourses);
        adapterMajorElective.setCourses(majorElectiveCourses);
        adapterDeptCommon.setCourses(deptCommonCourses);

        Log.d(TAG, "과목 로드 완료: 전필=" + adapterMajorRequired.getItemCount() +
            ", 전선=" + adapterMajorElective.getItemCount() +
            ", " + deptCommonCategoryName + "=" + adapterDeptCommon.getItemCount());
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
        String[] options = {"새 전공문서 만들기", "기존 전공문서 수정하기"};

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
     * 새 전공 문서 만들기 다이얼로그
     */
    private void showCreateNewDocumentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_major_document, null);

        Spinner spinnerDepartment = dialogView.findViewById(R.id.spinner_department);
        Spinner spinnerTrack = dialogView.findViewById(R.id.spinner_track);
        EditText etCohort = dialogView.findViewById(R.id.et_cohort);
        Spinner spinnerCategoryType = dialogView.findViewById(R.id.spinner_category_type);

        Log.d(TAG, "showSaveDialog - documentIds 크기: " + documentIds.size());

        // 학부/트랙 목록 추출
        Set<String> departments = new java.util.HashSet<>();
        Map<String, Set<String>> tracksByDepartment = new HashMap<>();

        for (String docId : documentIds) {
            Log.d(TAG, "문서 ID 파싱: " + docId);
            String[] parts = docId.split("_");
            // 형식: "IT학부_멀티미디어_2020" (3부분) 또는 "IT학부_멀티미디어_2020_major" (4부분)
            if (parts.length >= 3) {
                String dept = parts[0];
                String track = parts[1];
                departments.add(dept);
                tracksByDepartment.computeIfAbsent(dept, k -> new java.util.HashSet<>()).add(track);
                Log.d(TAG, "추출됨 - 학부: " + dept + ", 트랙: " + track);
            } else {
                Log.w(TAG, "잘못된 문서 ID 형식: " + docId + " (parts=" + parts.length + ")");
            }
        }

        List<String> departmentList = new ArrayList<>(departments);
        java.util.Collections.sort(departmentList);
        Log.d(TAG, "학부 목록 크기: " + departmentList.size());

        // 학부 스피너 설정
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, departmentList);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        // 트랙 스피너는 학부 선택에 따라 업데이트
        ArrayAdapter<String> trackAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, new ArrayList<>());
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);

        // 학부 선택 리스너
        spinnerDepartment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedDept = departmentList.get(position);
                Set<String> tracks = tracksByDepartment.get(selectedDept);
                if (tracks != null) {
                    List<String> trackList = new ArrayList<>(tracks);
                    java.util.Collections.sort(trackList);
                    trackAdapter.clear();
                    trackAdapter.addAll(trackList);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // 카테고리 타입 스피너 설정
        String[] categoryTypes = {"학부공통", "전공심화"};
        ArrayAdapter<String> catTypeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, categoryTypes);
        catTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryType.setAdapter(catTypeAdapter);

        // 현재 문서 정보로 초기값 설정
        if (currentDocumentId != null) {
            String[] parts = currentDocumentId.split("_");
            // 형식: "IT학부_멀티미디어_2020" (3부분) 또는 "IT학부_멀티미디어_2020_major" (4부분)
            if (parts.length >= 3) {
                String currentDept = parts[0];
                String currentTrack = parts[1];
                String cohortStr = parts[2];

                // 학부 선택
                int deptPos = departmentList.indexOf(currentDept);
                if (deptPos >= 0) {
                    spinnerDepartment.setSelection(deptPos);
                }

                // 트랙 선택 (학부 선택 후)
                spinnerDepartment.post(() -> {
                    for (int i = 0; i < trackAdapter.getCount(); i++) {
                        if (currentTrack.equals(trackAdapter.getItem(i))) {
                            spinnerTrack.setSelection(i);
                            break;
                        }
                    }
                });

                // 학번 설정
                if (cohortStr.length() == 4) {
                    etCohort.setText(cohortStr.substring(2)); // 2026 -> 26
                }
            }

            // 현재 카테고리 타입 선택
            if ("학부공통".equals(deptCommonCategoryName)) {
                spinnerCategoryType.setSelection(0);
            } else {
                spinnerCategoryType.setSelection(1);
            }
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("새 전공문서 만들기")
            .setMessage("현재 과목 목록을 새로운 전공 문서로 저장합니다")
            .setView(dialogView)
            .setPositiveButton("생성", (dialog, which) -> {
                String department = (String) spinnerDepartment.getSelectedItem();
                String track = (String) spinnerTrack.getSelectedItem();
                String cohortStr = etCohort.getText().toString().trim();
                String categoryType = (String) spinnerCategoryType.getSelectedItem();

                if (department == null || track == null || cohortStr.isEmpty()) {
                    Toast.makeText(this, "모든 항목을 선택/입력하세요", Toast.LENGTH_SHORT).show();
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

                // 문서 ID 생성: "IT학부_멀티미디어_2026" 형식
                String newDocId = department + "_" + track + "_" + cohort;
                deptCommonCategoryName = categoryType;

                // 학점요건 복사는 전공 문서에서는 사용하지 않음 (v3 구조)
                Log.d(TAG, "새 전공 문서 생성: " + newDocId);

                saveDocumentWithId(newDocId, false); // v3 구조에서는 학점요건 복사 안함
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 기존 전공 문서 수정하기
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
                Log.d(TAG, "기존 전공 문서 수정: " + currentDocumentId);
                saveDocumentWithId(currentDocumentId, false);
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 과목 추가 다이얼로그
     */
    private void showAddCourseDialog(String categoryName) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_course_input, null);

        EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        EditText etCourseCredit = dialogView.findViewById(R.id.et_course_credit);
        Spinner spinnerSemester = dialogView.findViewById(R.id.spinner_semester);

        // 학기 목록 설정
        String[] semesters = {
            "1학년1학기", "1학년2학기",
            "2학년1학기", "2학년2학기",
            "3학년1학기", "3학년2학기",
            "4학년1학기", "4학년2학기"
        };
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        new MaterialAlertDialogBuilder(this)
            .setTitle(categoryName + " 과목 추가")
            .setView(dialogView)
            .setPositiveButton("추가", (dialog, which) -> {
                String courseName = etCourseName.getText().toString().trim();
                String creditStr = etCourseCredit.getText().toString().trim();

                if (courseName.isEmpty()) {
                    Toast.makeText(this, "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                int credit;
                try {
                    credit = Integer.parseInt(creditStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "학점을 올바르게 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                CourseRequirement newCourse = new CourseRequirement(courseName, credit);

                if ("전공필수".equals(categoryName)) {
                    adapterMajorRequired.addCourse(newCourse);
                } else if ("전공선택".equals(categoryName)) {
                    adapterMajorElective.addCourse(newCourse);
                } else {
                    adapterDeptCommon.addCourse(newCourse);
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
     * 전공 문서는 과목 목록만 포함, 학점 정보는 제거
     */
    private void saveDocumentWithId(String docId, boolean copyCreditRequirements) {
        showLoading(true);

        // 문서 ID에서 department, track, cohort 추출
        String[] parts = docId.split("_");
        String department = parts.length >= 1 ? parts[0] : "";
        String track = parts.length >= 2 ? parts[1] : "";
        int cohort = 0;
        if (parts.length >= 3) {
            try {
                cohort = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "학번 파싱 실패: " + parts[2]);
            }
        }

        // v3 구조: rules 필드에 학기별 과목 저장
        Map<String, Object> rules = new HashMap<>();

        // 학기별로 과목을 분류할 맵 (빈 학기 포함)
        String[] semesters = {
            "1학년1학기", "1학년2학기",
            "2학년1학기", "2학년2학기",
            "3학년1학기", "3학년2학기",
            "4학년1학기", "4학년2학기"
        };

        for (String semester : semesters) {
            Map<String, Object> semesterData = new HashMap<>();
            semesterData.put("전공필수", new ArrayList<Map<String, Object>>());
            semesterData.put("전공선택", new ArrayList<Map<String, Object>>());
            semesterData.put(deptCommonCategoryName, new ArrayList<Map<String, Object>>());
            rules.put(semester, semesterData);
        }

        // 현재는 학기 정보가 없으므로 모든 과목을 1학년1학기에 임시 배치
        // (실제로는 과목에 학기 정보가 있어야 함)
        @SuppressWarnings("unchecked")
        Map<String, Object> firstSemester = (Map<String, Object>) rules.get("1학년1학기");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> majorRequiredList = (List<Map<String, Object>>) firstSemester.get("전공필수");
        for (CourseRequirement course : adapterMajorRequired.getCourses()) {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("과목명", course.getName());
            courseMap.put("학점", course.getCredits());
            majorRequiredList.add(courseMap);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> majorElectiveList = (List<Map<String, Object>>) firstSemester.get("전공선택");
        for (CourseRequirement course : adapterMajorElective.getCourses()) {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("과목명", course.getName());
            courseMap.put("학점", course.getCredits());
            majorElectiveList.add(courseMap);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> deptCommonList = (List<Map<String, Object>>) firstSemester.get(deptCommonCategoryName);
        for (CourseRequirement course : adapterDeptCommon.getCourses()) {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("과목명", course.getName());
            courseMap.put("학점", course.getCredits());
            deptCommonList.add(courseMap);
        }

        // ✅ v3 전공 문서 구조 (과목만 포함, 학점 정보 제거)
        Map<String, Object> documentData = new HashMap<>();
        documentData.put("docType", "major");
        documentData.put("department", department);
        documentData.put("track", track);
        documentData.put("cohort", cohort);
        documentData.put("version", "v3");
        documentData.put("updatedAt", com.google.firebase.Timestamp.now());
        documentData.put("rules", rules);

        Log.d(TAG, "v3 전공 문서 생성: " + docId);
        Log.d(TAG, "  - docType: major");
        Log.d(TAG, "  - department: " + department + ", track: " + track + ", cohort: " + cohort);
        Log.d(TAG, "  - rules 포함 (학기별 과목)");
        Log.d(TAG, "  - ❌ 학점요건 제거 (졸업요건 문서에만 존재)");

        // Firestore에 저장
        db.collection("graduation_requirements")
            .document(docId)
            .set(documentData)
            .addOnSuccessListener(aVoid -> {
                showLoading(false);
                Log.d(TAG, "전공 문서 저장 성공 (v3): " + docId);
                Toast.makeText(this, "전공 문서가 저장되었습니다: " + docId, Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Log.e(TAG, "전공 문서 저장 실패: " + docId, e);
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
        adapterMajorRequired.clearCourses();
        adapterMajorElective.clearCourses();
        adapterDeptCommon.clearCourses();
    }

    /**
     * 로딩 상태 표시/숨김
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
