package sprout.app.sakmvp1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sprout.app.sakmvp1.utils.UiUtils;

/**
 * 전공 문서 편집 Activity
 * 졸업요건 편집과 유사한 카테고리별 과목 관리
 */
public class MajorDocumentEditActivity extends AppCompatActivity {

    private static final String TAG = "MajorDocEdit";

    private MaterialToolbar toolbar;
    private TextInputEditText etDocId;
    private TextInputEditText etMajorRequired, etMajorElective, etMajorAdvanced, etDepartmentCommon;
    private LinearLayout coursesContainer;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String documentId;
    private boolean isNew;

    // 카테고리별 과목 데이터
    private Map<String, List<Map<String, Object>>> categoryCoursesMap = new HashMap<>();

    // 카테고리 정의
    private static final String[] CATEGORIES = {"학부공통필수", "전공심화", "전공필수", "전공선택"};
    private static final String[] CATEGORY_COLORS = {"#7B1FA2", "#1976D2", "#1976D2", "#1976D2"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_major_document_edit);

        db = FirebaseFirestore.getInstance();

        // Intent에서 파라미터 가져오기
        isNew = getIntent().getBooleanExtra("IS_NEW", false);
        documentId = getIntent().getStringExtra("DOCUMENT_ID");

        initViews();
        setupToolbar();
        setupListeners();

        if (!isNew && documentId != null) {
            // 기존 문서 편집 모드
            loadDocument();
        } else {
            // 새 문서 생성 모드
            etDocId.setEnabled(true);
            initializeEmptyCategories();
            displayCategories();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etDocId = findViewById(R.id.et_doc_id);
        etMajorRequired = findViewById(R.id.et_major_required);
        etMajorElective = findViewById(R.id.et_major_elective);
        etMajorAdvanced = findViewById(R.id.et_major_advanced);
        etDepartmentCommon = findViewById(R.id.et_department_common);
        coursesContainer = findViewById(R.id.requirements_container);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isNew ? "전공 문서 추가" : "전공 문서 편집");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void initializeEmptyCategories() {
        for (String category : CATEGORIES) {
            categoryCoursesMap.put(category, new ArrayList<>());
        }
    }

    private void loadDocument() {
        showLoading(true);

        db.collection("graduation_requirements")
                .document(documentId)
                .get()
                .addOnSuccessListener(document -> {
                    showLoading(false);

                    if (document.exists()) {
                        displayDocument(document);
                    } else {
                        Toast.makeText(this, "문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "문서 로드 실패", e);
                    Toast.makeText(this, "로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayDocument(DocumentSnapshot document) {
        etDocId.setText(document.getId());
        etDocId.setEnabled(false); // 기존 문서는 ID 변경 불가

        // 학점 정보 로드
        Long majorRequired = document.getLong("전공필수");
        Long majorElective = document.getLong("전공선택");
        Long majorAdvanced = document.getLong("전공심화");
        Long deptCommon = document.getLong("학부공통");

        etMajorRequired.setText(majorRequired != null ? String.valueOf(majorRequired) : "0");
        etMajorElective.setText(majorElective != null ? String.valueOf(majorElective) : "0");
        etMajorAdvanced.setText(majorAdvanced != null ? String.valueOf(majorAdvanced) : "0");
        etDepartmentCommon.setText(deptCommon != null ? String.valueOf(deptCommon) : "0");

        // 카테고리별 과목 데이터 초기화
        initializeEmptyCategories();

        // rules.requirements 로드 (교양 문서 스타일)
        Object rulesObj = document.get("rules");
        if (rulesObj instanceof Map) {
            Map<String, Object> rules = (Map<String, Object>) rulesObj;
            Object requirementsObj = rules.get("requirements");

            if (requirementsObj instanceof List) {
                // requirements 구조로 저장된 경우 (교양 스타일)
                List<Object> requirements = (List<Object>) requirementsObj;
                loadFromRequirementsStructure(requirements);
            } else {
                // 학기별 구조로 저장된 경우 (졸업요건 스타일)
                loadFromSemesterStructure(rules);
            }
        }

        Log.d(TAG, "과목 로드 완료");
        displayCategories();
    }

    /**
     * requirements 구조에서 과목 로드 (교양 문서 스타일)
     */
    private void loadFromRequirementsStructure(List<Object> requirements) {
        for (Object reqObj : requirements) {
            if (reqObj instanceof Map) {
                Map<String, Object> req = (Map<String, Object>) reqObj;
                String type = (String) req.get("type");

                if ("single".equals(type)) {
                    // 단일 필수 과목
                    String name = (String) req.get("name");
                    Object creditObj = req.get("credit");
                    int credit = creditObj instanceof Long ? ((Long) creditObj).intValue() :
                                (creditObj instanceof Integer ? (Integer) creditObj : 3);

                    Map<String, Object> course = new HashMap<>();
                    course.put("name", name);
                    course.put("credit", credit);
                    course.put("semester", "미지정");

                    // 기본적으로 전공필수에 추가
                    categoryCoursesMap.get("전공필수").add(course);

                } else if ("oneOf".equals(type)) {
                    // 선택 과목 그룹의 옵션들을 전공선택에 추가
                    Object optionsObj = req.get("options");
                    Object creditObj = req.get("credit");
                    int credit = creditObj instanceof Long ? ((Long) creditObj).intValue() :
                                (creditObj instanceof Integer ? (Integer) creditObj : 3);

                    if (optionsObj instanceof List) {
                        for (Object optObj : (List<Object>) optionsObj) {
                            if (optObj instanceof Map) {
                                String name = (String) ((Map<String, Object>) optObj).get("name");
                                if (name != null) {
                                    Map<String, Object> course = new HashMap<>();
                                    course.put("name", name);
                                    course.put("credit", credit);
                                    course.put("semester", "미지정");
                                    categoryCoursesMap.get("전공선택").add(course);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 학기별 구조에서 과목 로드 (졸업요건 스타일)
     */
    private void loadFromSemesterStructure(Map<String, Object> rules) {
        // rules의 모든 학기를 순회하면서 카테고리별 과목 수집
        for (Map.Entry<String, Object> semesterEntry : rules.entrySet()) {
            String semester = semesterEntry.getKey();
            Object semesterDataObj = semesterEntry.getValue();

            if (semesterDataObj instanceof Map) {
                Map<String, Object> semesterData = (Map<String, Object>) semesterDataObj;

                // 각 카테고리의 과목 목록 가져오기
                for (String category : CATEGORIES) {
                    Object categoryCoursesObj = semesterData.get(category);

                    if (categoryCoursesObj instanceof List) {
                        List<Object> categoryCourses = (List<Object>) categoryCoursesObj;

                        for (Object courseObj : categoryCourses) {
                            if (courseObj instanceof Map) {
                                Map<String, Object> course = (Map<String, Object>) courseObj;
                                Map<String, Object> courseMap = new HashMap<>();
                                courseMap.put("name", course.get("과목명"));
                                courseMap.put("credit", course.get("학점"));
                                courseMap.put("semester", semester);
                                categoryCoursesMap.get(category).add(courseMap);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 카테고리별 카드 표시
     */
    private void displayCategories() {
        coursesContainer.removeAllViews();

        int majorAdvanced = getIntFromEditText(etMajorAdvanced);
        int deptCommon = getIntFromEditText(etDepartmentCommon);

        for (int i = 0; i < CATEGORIES.length; i++) {
            String category = CATEGORIES[i];
            String color = CATEGORY_COLORS[i];

            // 학부공통필수는 학부공통이 0보다 클 때만 표시
            if (category.equals("학부공통필수") && deptCommon == 0) {
                continue;
            }

            // 전공심화는 전공심화가 0보다 클 때만 표시
            if (category.equals("전공심화") && majorAdvanced == 0) {
                continue;
            }

            addCategoryCard(category, color);
        }
    }

    private int getIntFromEditText(TextInputEditText editText) {
        try {
            String text = editText.getText() != null ? editText.getText().toString().trim() : "0";
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 카테고리 카드 추가
     */
    private void addCategoryCard(String categoryName, String colorHex) {
        List<Map<String, Object>> courses = categoryCoursesMap.get(categoryName);
        if (courses == null) {
            courses = new ArrayList<>();
            categoryCoursesMap.put(categoryName, courses);
        }

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, UiUtils.dpToPx(this, 16));
        card.setLayoutParams(cardParams);
        card.setRadius(UiUtils.dpToPx(this, 12));
        card.setCardElevation(UiUtils.dpToPx(this, 2));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(UiUtils.dpToPx(this, 20), UiUtils.dpToPx(this, 20),
                              UiUtils.dpToPx(this, 20), UiUtils.dpToPx(this, 20));

        // 카테고리 제목 + 추가 버튼
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(categoryName + " (" + courses.size() + "개)");
        titleView.setTextSize(16);
        titleView.setTextColor(android.graphics.Color.parseColor(colorHex));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        titleView.setLayoutParams(titleParams);
        headerLayout.addView(titleView);

        // 추가 버튼
        MaterialButton btnAdd = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialIconButtonStyle);
        btnAdd.setText("+");
        btnAdd.setOnClickListener(v -> showAddCourseDialog(categoryName));
        headerLayout.addView(btnAdd);

        cardContent.addView(headerLayout);

        // 과목 목록 컨테이너
        LinearLayout coursesListLayout = new LinearLayout(this);
        coursesListLayout.setOrientation(LinearLayout.VERTICAL);
        coursesListLayout.setPadding(0, UiUtils.dpToPx(this, 12), 0, 0);

        // 과목 목록
        for (int i = 0; i < courses.size(); i++) {
            Map<String, Object> course = courses.get(i);
            addCourseItem(coursesListLayout, course, categoryName, i);
        }

        cardContent.addView(coursesListLayout);
        card.addView(cardContent);
        coursesContainer.addView(card);
    }

    /**
     * 과목 아이템 추가
     */
    private void addCourseItem(LinearLayout parent, Map<String, Object> course,
                              String categoryName, int index) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, UiUtils.dpToPx(this, 8));
        itemLayout.setLayoutParams(itemParams);

        // 과목명 + 학점 + 학기
        TextView nameView = new TextView(this);
        String name = course.get("name") != null ? course.get("name").toString() : "과목명 없음";
        Object creditObj = course.get("credit");
        String creditStr = creditObj != null ? creditObj.toString() + "학점" : "";
        String semester = course.get("semester") != null ? course.get("semester").toString() : "";
        nameView.setText(name + " (" + creditStr + " / " + semester + ")");
        nameView.setTextSize(14);
        nameView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        nameView.setLayoutParams(nameParams);
        itemLayout.addView(nameView);

        // 수정 버튼
        MaterialButton btnEdit = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialIconButtonStyle);
        btnEdit.setText("수정");
        btnEdit.setTextSize(11);
        btnEdit.setOnClickListener(v -> showEditCourseDialog(categoryName, index));
        itemLayout.addView(btnEdit);

        // 삭제 버튼
        MaterialButton btnDelete = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialIconButtonStyle);
        btnDelete.setText("삭제");
        btnDelete.setTextSize(11);
        btnDelete.setOnClickListener(v -> deleteCourse(categoryName, index));
        itemLayout.addView(btnDelete);

        parent.addView(itemLayout);
    }

    /**
     * 과목 추가 대화상자
     */
    private void showAddCourseDialog(String categoryName) {
        // 먼저 학기 선택
        final String[] semesters = {"1학년 1학기", "1학년 2학기", "2학년 1학기", "2학년 2학기",
                                     "3학년 1학기", "3학년 2학기", "4학년 1학기", "4학년 2학기", "미지정"};

        new AlertDialog.Builder(this)
                .setTitle("학기 선택")
                .setItems(semesters, (dialog, which) -> {
                    String selectedSemester = semesters[which];
                    showCourseInputDialog(categoryName, selectedSemester, null, -1);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 과목 입력 대화상자
     */
    private void showCourseInputDialog(String categoryName, String semester,
                                      Map<String, Object> existingCourse, int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(categoryName + " 과목 " + (existingCourse == null ? "추가" : "수정"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText etCourseName = new EditText(this);
        etCourseName.setHint("과목명");
        layout.addView(etCourseName);

        EditText etCourseCredit = new EditText(this);
        etCourseCredit.setHint("학점");
        etCourseCredit.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams creditParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        creditParams.setMargins(0, 16, 0, 0);
        etCourseCredit.setLayoutParams(creditParams);
        layout.addView(etCourseCredit);

        if (existingCourse != null) {
            etCourseName.setText(existingCourse.get("name").toString());
            Object creditObj = existingCourse.get("credit");
            if (creditObj != null) {
                etCourseCredit.setText(creditObj.toString());
            }
        }

        builder.setView(layout);
        builder.setPositiveButton(existingCourse == null ? "추가" : "수정", (dialog, which) -> {
            String name = etCourseName.getText().toString().trim();
            String creditStr = etCourseCredit.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            int credit = 3; // 기본값
            if (!creditStr.isEmpty()) {
                try {
                    credit = Integer.parseInt(creditStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "학점은 숫자로 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            List<Map<String, Object>> courses = categoryCoursesMap.get(categoryName);
            if (courses != null) {
                if (existingCourse == null) {
                    // 추가
                    Map<String, Object> newCourse = new HashMap<>();
                    newCourse.put("name", name);
                    newCourse.put("credit", credit);
                    newCourse.put("semester", semester);
                    courses.add(newCourse);
                } else {
                    // 수정
                    existingCourse.put("name", name);
                    existingCourse.put("credit", credit);
                }
                // 화면 새로고침
                displayCategories();
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    /**
     * 과목 수정 대화상자
     */
    private void showEditCourseDialog(String categoryName, int index) {
        List<Map<String, Object>> courses = categoryCoursesMap.get(categoryName);
        if (courses == null || index >= courses.size()) return;

        Map<String, Object> course = courses.get(index);
        String semester = (String) course.get("semester");

        showCourseInputDialog(categoryName, semester, course, index);
    }

    /**
     * 과목 삭제
     */
    private void deleteCourse(String categoryName, int index) {
        new AlertDialog.Builder(this)
                .setTitle("과목 삭제")
                .setMessage("이 과목을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    List<Map<String, Object>> courses = categoryCoursesMap.get(categoryName);
                    if (courses != null && index < courses.size()) {
                        courses.remove(index);
                        // 화면 새로고침
                        displayCategories();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void validateAndSave() {
        String docId = etDocId.getText() != null ? etDocId.getText().toString().trim() : "";
        String majReq = etMajorRequired.getText() != null ? etMajorRequired.getText().toString().trim() : "0";
        String majElec = etMajorElective.getText() != null ? etMajorElective.getText().toString().trim() : "0";
        String majAdv = etMajorAdvanced.getText() != null ? etMajorAdvanced.getText().toString().trim() : "0";
        String deptComm = etDepartmentCommon.getText() != null ? etDepartmentCommon.getText().toString().trim() : "0";

        // 유효성 검사
        if (docId.isEmpty()) {
            etDocId.setError("문서 ID를 입력하세요 (예: IT학부_멀티미디어_2020)");
            etDocId.requestFocus();
            return;
        }

        int majorRequired, majorElective, majorAdvanced, deptCommon;
        try {
            majorRequired = Integer.parseInt(majReq);
            majorElective = Integer.parseInt(majElec);
            majorAdvanced = Integer.parseInt(majAdv);
            deptCommon = Integer.parseInt(deptComm);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "학점은 숫자로 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        saveDocument(docId, majorRequired, majorElective, majorAdvanced, deptCommon);
    }

    private void saveDocument(String docId, int majorRequired, int majorElective,
                              int majorAdvanced, int deptCommon) {
        showLoading(true);

        // 데이터 구조 생성
        Map<String, Object> data = new HashMap<>();
        data.put("전공필수", majorRequired);
        data.put("전공선택", majorElective);
        data.put("전공심화", majorAdvanced);
        data.put("학부공통", deptCommon);

        // rules 구조 생성 (학기별 구조)
        Map<String, Object> rules = new HashMap<>();

        // 카테고리별 과목을 학기별로 재분류
        for (Map.Entry<String, List<Map<String, Object>>> entry : categoryCoursesMap.entrySet()) {
            String categoryName = entry.getKey();
            List<Map<String, Object>> courses = entry.getValue();

            // 학기별로 그룹화
            Map<String, List<Map<String, Object>>> coursesBySemester = new HashMap<>();

            for (Map<String, Object> course : courses) {
                String semester = (String) course.get("semester");
                if (semester == null || semester.equals("미지정")) {
                    semester = "1학년 1학기"; // 기본값
                }

                if (!coursesBySemester.containsKey(semester)) {
                    coursesBySemester.put(semester, new ArrayList<>());
                }

                Map<String, Object> firestoreCourse = new HashMap<>();
                firestoreCourse.put("과목명", course.get("name"));
                firestoreCourse.put("학점", course.get("credit"));
                coursesBySemester.get(semester).add(firestoreCourse);
            }

            // rules에 학기별로 추가
            for (Map.Entry<String, List<Map<String, Object>>> semesterEntry : coursesBySemester.entrySet()) {
                String semester = semesterEntry.getKey();
                List<Map<String, Object>> semesterCourses = semesterEntry.getValue();

                Object semesterDataObj = rules.get(semester);
                Map<String, Object> semesterData;
                if (semesterDataObj instanceof Map) {
                    semesterData = (Map<String, Object>) semesterDataObj;
                } else {
                    semesterData = new HashMap<>();
                    rules.put(semester, semesterData);
                }
                semesterData.put(categoryName, semesterCourses);
            }
        }

        data.put("rules", rules);

        // 문서 ID 파싱하여 메타데이터 추가
        String[] parts = docId.split("_");
        if (parts.length == 3) {
            data.put("department", parts[0]);
            data.put("track", parts[1]);
            try {
                data.put("cohort", Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                // cohort 파싱 실패 시 무시
            }
        }

        Log.d(TAG, "========== Firestore 저장 시작 ==========");
        Log.d(TAG, "문서 ID: " + docId);
        Log.d(TAG, "전공필수: " + majorRequired);
        Log.d(TAG, "전공선택: " + majorElective);
        Log.d(TAG, "전공심화: " + majorAdvanced);
        Log.d(TAG, "학부공통: " + deptCommon);

        db.collection("graduation_requirements")
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Log.d(TAG, "========== Firestore 저장 성공 ==========");
                    Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "========== Firestore 저장 실패 ==========", e);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }
}
