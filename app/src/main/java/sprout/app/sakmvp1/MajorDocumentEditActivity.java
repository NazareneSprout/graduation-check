package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import sprout.app.sakmvp1.adapters.CourseEditAdapter;
import sprout.app.sakmvp1.managers.CustomizedRequirementsManager;
import sprout.app.sakmvp1.models.*;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;

/**
 * 전공문서 편집 Activity
 * - 기준 전공문서 선택
 * - 전공필수/전공선택/학부공통(또는 전공심화) 학점 수정
 * - 과목 추가/수정/삭제
 */
public class MajorDocumentEditActivity extends AppCompatActivity {

    private static final String TAG = "MajorDocEdit";

    // UI Components
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private View contentLayout;

    private TextView tvCurrentDocument;
    private MaterialButton btnSelectDocument;

    // 학점 입력
    private TextInputEditText etMajorRequired;
    private TextInputEditText etMajorElective;
    private TextInputEditText etDeptCommon;
    private TextView tvDeptCommonLabel;

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
    private MaterialButton btnSave;
    private MaterialButton btnReset;

    // Data
    private FirebaseFirestore db;
    private CustomizedRequirementsManager customizationManager;
    private UserCustomizedRequirements customizations;
    private GraduationRules baseRules;
    private String deptCommonCategoryName = "학부공통";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_major_document_edit);

        db = FirebaseFirestore.getInstance();
        customizationManager = new CustomizedRequirementsManager();

        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupListeners();
        loadUserCustomizations();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);

        tvCurrentDocument = findViewById(R.id.tv_current_document);
        btnSelectDocument = findViewById(R.id.btn_select_document);

        etMajorRequired = findViewById(R.id.et_major_required);
        etMajorElective = findViewById(R.id.et_major_elective);
        etDeptCommon = findViewById(R.id.et_dept_common);
        tvDeptCommonLabel = findViewById(R.id.tv_dept_common_label);

        rvMajorRequired = findViewById(R.id.rv_major_required);
        rvMajorElective = findViewById(R.id.rv_major_elective);
        rvDeptCommon = findViewById(R.id.rv_dept_common);

        btnAddMajorRequired = findViewById(R.id.btn_add_major_required);
        btnAddMajorElective = findViewById(R.id.btn_add_major_elective);
        btnAddDeptCommon = findViewById(R.id.btn_add_dept_common);
        btnSave = findViewById(R.id.btn_save);
        btnReset = findViewById(R.id.btn_reset);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("전공문서 편집");
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
        btnSelectDocument.setOnClickListener(v -> showDocumentSelectionDialog());
        btnAddMajorRequired.setOnClickListener(v -> showAddCourseDialog("전공필수"));
        btnAddMajorElective.setOnClickListener(v -> showAddCourseDialog("전공선택"));
        btnAddDeptCommon.setOnClickListener(v -> showAddCourseDialog(deptCommonCategoryName));
        btnSave.setOnClickListener(v -> saveCustomizations());
        btnReset.setOnClickListener(v -> showResetConfirmDialog());
    }

    /**
     * 사용자 커스터마이징 로드
     */
    private void loadUserCustomizations() {
        showLoading(true);

        customizationManager.loadUserCustomizations(new CustomizedRequirementsManager.OnLoadListener() {
            @Override
            public void onSuccess(UserCustomizedRequirements customizations) {
                MajorDocumentEditActivity.this.customizations = customizations;

                if (customizations.getMajorDocumentId() != null) {
                    loadMajorDocument(customizations.getMajorDocumentId());
                } else {
                    showLoading(false);
                    Toast.makeText(MajorDocumentEditActivity.this,
                        "전공문서를 선택해주세요", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Log.e(TAG, "커스터마이징 로드 실패", e);
                Toast.makeText(MajorDocumentEditActivity.this,
                    "로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 전공문서 로드
     */
    private void loadMajorDocument(String documentId) {
        db.collection("graduation_requirements")
            .document(documentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    baseRules = documentSnapshot.toObject(GraduationRules.class);
                    if (baseRules != null) {
                        // 문서 타입 확인 (학부공통 vs 전공심화)
                        String categoryType = baseRules.getMajorCategoryType();
                        if (categoryType != null) {
                            deptCommonCategoryName = categoryType;
                            updateDeptCommonLabels();
                        }

                        // 커스터마이징 적용
                        GraduationRules customizedRules = customizationManager.applyCustomizations(
                            baseRules, customizations, "major");

                        displayDocument(customizedRules);
                    }
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
     * 문서 선택 다이얼로그
     */
    private void showDocumentSelectionDialog() {
        showLoading(true);

        db.collection("graduation_requirements")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<String> docIds = new ArrayList<>();
                List<String> displayNames = new ArrayList<>();

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String docId = document.getId();
                    docIds.add(docId);

                    String cohort = document.getString("cohort");
                    String department = document.getString("department");
                    String track = document.getString("track");

                    String displayName = cohort + " " + department;
                    if (track != null && !track.isEmpty()) {
                        displayName += " (" + track + ")";
                    }
                    displayNames.add(displayName);
                }

                showLoading(false);

                if (docIds.isEmpty()) {
                    Toast.makeText(this, "불러올 문서가 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                new MaterialAlertDialogBuilder(this)
                    .setTitle("전공문서 선택")
                    .setItems(displayNames.toArray(new String[0]), (dialog, which) -> {
                        customizations.setMajorDocumentId(docIds.get(which));
                        loadMajorDocument(docIds.get(which));
                    })
                    .setNegativeButton("취소", null)
                    .show();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Toast.makeText(this, "문서 목록 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 문서 내용 표시
     */
    private void displayDocument(GraduationRules rules) {
        tvCurrentDocument.setText(rules.getCohort() + " " + rules.getDepartment() +
            (rules.getTrack() != null ? " (" + rules.getTrack() + ")" : ""));

        // 학점 표시
        if (rules.getCreditRequirements() != null) {
            etMajorRequired.setText(String.valueOf(rules.getCreditRequirements().get전공필수()));
            etMajorElective.setText(String.valueOf(rules.getCreditRequirements().get전공선택()));

            if ("학부공통".equals(deptCommonCategoryName)) {
                etDeptCommon.setText(String.valueOf(rules.getCreditRequirements().get학부공통()));
            } else {
                etDeptCommon.setText(String.valueOf(rules.getCreditRequirements().get전공심화()));
            }
        }

        // 과목 표시
        if (rules.getCategories() != null) {
            for (RequirementCategory category : rules.getCategories()) {
                String categoryName = category.getName();
                List<CourseRequirement> courses = category.getCourses();

                if (courses != null) {
                    if ("전공필수".equals(categoryName)) {
                        adapterMajorRequired.setCourses(courses);
                    } else if ("전공선택".equals(categoryName)) {
                        adapterMajorElective.setCourses(courses);
                    } else if (deptCommonCategoryName.equals(categoryName)) {
                        adapterDeptCommon.setCourses(courses);
                    }
                }
            }
        }
    }

    /**
     * 학부공통/전공심화 라벨 업데이트
     */
    private void updateDeptCommonLabels() {
        tvDeptCommonLabel.setText(deptCommonCategoryName);
        btnAddDeptCommon.setText("+ " + deptCommonCategoryName + " 추가");
    }

    /**
     * 과목 추가 다이얼로그
     */
    private void showAddCourseDialog(String categoryName) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_course_input, null);

        EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        EditText etCourseCredit = dialogView.findViewById(R.id.et_course_credit);

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
     * 커스터마이징 저장
     */
    private void saveCustomizations() {
        if (customizations == null || baseRules == null) {
            Toast.makeText(this, "먼저 전공문서를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // 학점 수정사항 저장
        UserCustomizedRequirements.DocumentCustomization majorCustomization =
            customizations.getMajorCustomizations();

        try {
            int majorRequired = Integer.parseInt(etMajorRequired.getText().toString().trim());
            int majorElective = Integer.parseInt(etMajorElective.getText().toString().trim());
            int deptCommon = Integer.parseInt(etDeptCommon.getText().toString().trim());

            majorCustomization.modifyCredit("전공필수", majorRequired);
            majorCustomization.modifyCredit("전공선택", majorElective);
            majorCustomization.modifyCredit(deptCommonCategoryName, deptCommon);

        } catch (NumberFormatException e) {
            showLoading(false);
            Toast.makeText(this, "학점을 올바르게 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 과목 수정사항 저장 (간단하게 전체 교체로 처리)
        majorCustomization.getCourseModifications().clear();

        // 기본 과목과 비교해서 수정사항 파악
        saveCourseDifferences("전공필수", adapterMajorRequired.getCourses(), majorCustomization);
        saveCourseDifferences("전공선택", adapterMajorElective.getCourses(), majorCustomization);
        saveCourseDifferences(deptCommonCategoryName, adapterDeptCommon.getCourses(), majorCustomization);

        customizationManager.saveUserCustomizations(customizations,
            new CustomizedRequirementsManager.OnSaveListener() {
                @Override
                public void onSuccess() {
                    showLoading(false);
                    Toast.makeText(MajorDocumentEditActivity.this,
                        "저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    showLoading(false);
                    Log.e(TAG, "저장 실패", e);
                    Toast.makeText(MajorDocumentEditActivity.this,
                        "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * 과목 차이점 저장 (기본 문서와 비교)
     */
    private void saveCourseDifferences(String categoryName, List<CourseRequirement> currentCourses,
                                      UserCustomizedRequirements.DocumentCustomization customization) {
        // 기본 문서의 해당 카테고리 과목 찾기
        List<CourseRequirement> baseCourses = new ArrayList<>();
        if (baseRules.getCategories() != null) {
            for (RequirementCategory category : baseRules.getCategories()) {
                if (categoryName.equals(category.getName()) && category.getCourses() != null) {
                    baseCourses = category.getCourses();
                    break;
                }
            }
        }

        // 추가된 과목 찾기
        for (CourseRequirement current : currentCourses) {
            boolean found = false;
            for (CourseRequirement base : baseCourses) {
                if (current.getName().equals(base.getName())) {
                    found = true;
                    // 학점이 변경되었는지 확인
                    if (current.getCredits() != base.getCredits()) {
                        customization.modifyCourse(categoryName, base.getName(),
                            current.getName(), current.getCredits());
                    }
                    break;
                }
            }
            if (!found) {
                // 새로 추가된 과목
                customization.addCourse(categoryName, current.getName(), current.getCredits());
            }
        }

        // 삭제된 과목 찾기
        for (CourseRequirement base : baseCourses) {
            boolean found = false;
            for (CourseRequirement current : currentCourses) {
                if (base.getName().equals(current.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // 삭제된 과목
                customization.deleteCourse(categoryName, base.getName());
            }
        }
    }

    /**
     * 초기화 확인 다이얼로그
     */
    private void showResetConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("초기화")
            .setMessage("전공문서 커스터마이징을 초기화하시겠습니까?\n기본 문서 설정으로 되돌아갑니다.")
            .setPositiveButton("초기화", (dialog, which) -> resetCustomizations())
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 커스터마이징 초기화
     */
    private void resetCustomizations() {
        if (customizations != null) {
            customizations.setMajorDocumentId(null);
            customizations.setMajorCustomizations(new UserCustomizedRequirements.DocumentCustomization());

            showLoading(true);
            customizationManager.saveUserCustomizations(customizations,
                new CustomizedRequirementsManager.OnSaveListener() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(MajorDocumentEditActivity.this,
                            "초기화되었습니다", Toast.LENGTH_SHORT).show();
                        loadUserCustomizations();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showLoading(false);
                        Toast.makeText(MajorDocumentEditActivity.this,
                            "초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    /**
     * 로딩 상태 표시/숨김
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
