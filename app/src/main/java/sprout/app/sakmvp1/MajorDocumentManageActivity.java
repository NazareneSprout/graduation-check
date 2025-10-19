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

                    // 교양 문서는 제외
                    if (docId.startsWith("교양_")) {
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
     * 저장 다이얼로그 - 새 학번으로 저장
     */
    private void showSaveDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_major_document, null);

        Spinner spinnerDepartment = dialogView.findViewById(R.id.spinner_department);
        Spinner spinnerTrack = dialogView.findViewById(R.id.spinner_track);
        EditText etCohort = dialogView.findViewById(R.id.et_cohort);
        Spinner spinnerCategoryType = dialogView.findViewById(R.id.spinner_category_type);
        android.widget.CheckBox cbCopyCreditRequirements = dialogView.findViewById(R.id.cb_copy_credit_requirements);

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
            .setTitle("새 학번으로 저장")
            .setMessage("현재 과목 목록을 새로운 문서로 저장합니다")
            .setView(dialogView)
            .setPositiveButton("저장", (dialog, which) -> {
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

                // 학점요건 복사 여부 확인
                boolean copyCreditRequirements = cbCopyCreditRequirements.isChecked();
                Log.d(TAG, "체크박스 상태: " + copyCreditRequirements + ", 현재 학점요건: " + (currentCreditRequirements != null ? "있음" : "없음"));

                saveDocumentWithId(newDocId, copyCreditRequirements);
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
     * 문서 저장
     */
    private void saveDocumentWithId(String docId, boolean copyCreditRequirements) {
        showLoading(true);

        // categories 데이터 생성
        List<Map<String, Object>> categories = new ArrayList<>();

        // 전공필수
        Map<String, Object> majorRequiredCategory = new HashMap<>();
        majorRequiredCategory.put("name", "전공필수");
        majorRequiredCategory.put("courses", convertCoursesToMap(adapterMajorRequired.getCourses()));
        categories.add(majorRequiredCategory);

        // 전공선택
        Map<String, Object> majorElectiveCategory = new HashMap<>();
        majorElectiveCategory.put("name", "전공선택");
        majorElectiveCategory.put("courses", convertCoursesToMap(adapterMajorElective.getCourses()));
        categories.add(majorElectiveCategory);

        // 학부공통 또는 전공심화
        Map<String, Object> deptCommonCategory = new HashMap<>();
        deptCommonCategory.put("name", deptCommonCategoryName);
        deptCommonCategory.put("courses", convertCoursesToMap(adapterDeptCommon.getCourses()));
        categories.add(deptCommonCategory);

        // Firestore 문서 데이터
        Map<String, Object> documentData = new HashMap<>();
        documentData.put("categories", categories);
        documentData.put("majorCategoryType", deptCommonCategoryName);

        // 학점요건 복사 - 한글 필드명들을 문서 최상위 레벨에 직접 저장
        if (copyCreditRequirements && currentCreditRequirements != null) {
            // 한글 필드명들을 그대로 복사
            documentData.putAll(currentCreditRequirements);

            // 총이수 필드가 없으면 자동으로 계산해서 추가
            if (!currentCreditRequirements.containsKey("총이수")) {
                int total = 0;
                String[] creditFields = {"전공필수", "전공선택", "교양필수", "교양선택", "소양", "학부공통", "일반선택", "전공심화", "잔여학점"};
                for (String field : creditFields) {
                    Object value = currentCreditRequirements.get(field);
                    if (value instanceof Number) {
                        total += ((Number) value).intValue();
                    }
                }
                if (total > 0) {
                    documentData.put("총이수", total);
                    Log.d(TAG, "총이수 필드 자동 계산 추가: " + total);
                }
            }

            Log.d(TAG, "학점요건 복사함: " + currentCreditRequirements);
        } else {
            Log.d(TAG, "학점요건 복사하지 않음");
        }

        // Firestore에 저장
        db.collection("graduation_requirements")
            .document(docId)
            .set(documentData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                showLoading(false);
                Log.d(TAG, "문서 저장 성공: " + docId);
                Toast.makeText(this, "저장되었습니다: " + docId, Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Log.e(TAG, "문서 저장 실패: " + docId, e);
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
