package sprout.app.sakmvp1;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sprout.app.sakmvp1.utils.GraduationRequirementUtils;
import sprout.app.sakmvp1.utils.UiUtils;

/**
 * 졸업요건 편집 Activity (관리자용)
 * 과목 추가/수정/삭제 기능 제공
 */
public class GraduationRequirementEditActivity extends AppCompatActivity {

    private static final String TAG = "GradReqEdit";
    public static final String EXTRA_DOCUMENT_ID = "document_id";

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private NestedScrollView contentLayout;
    private LinearLayout coursesContainer;
    private MaterialButton btnSave;
    private TextView tvCurrentMajorDoc;
    private MaterialButton btnSelectMajorDoc;
    private TextView tvCurrentGeneralDoc;
    private MaterialButton btnSelectGeneralDoc;

    private FirebaseFirestore db;
    private String documentId;
    private DocumentSnapshot currentDocument;
    private String selectedMajorDocId; // 선택된 전공 문서 ID
    private String selectedGeneralDocId; // 선택된 교양 문서 ID

    // 편집 중인 과목 데이터 (카테고리명 -> 과목 리스트)
    private Map<String, List<Map<String, Object>>> editingCourses = new HashMap<>();

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
        setupListeners();
        loadGraduationRequirement();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);
        coursesContainer = findViewById(R.id.courses_container);
        btnSave = findViewById(R.id.btn_save);
        tvCurrentMajorDoc = findViewById(R.id.tv_current_major_doc);
        btnSelectMajorDoc = findViewById(R.id.btn_select_major_doc);
        tvCurrentGeneralDoc = findViewById(R.id.tv_current_general_doc);
        btnSelectGeneralDoc = findViewById(R.id.btn_select_general_doc);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("졸업요건 편집");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveChanges());
        btnSelectMajorDoc.setOnClickListener(v -> showMajorDocumentSelector());
        btnSelectGeneralDoc.setOnClickListener(v -> showGeneralDocumentSelector());
    }

    private void loadGraduationRequirement() {
        showLoading(true);

        db.collection("graduation_requirements")
                .document(documentId)
                .get()
                .addOnSuccessListener(document -> {
                    currentDocument = document;

                    // 저장된 전공 문서 ID 읽기 (없으면 현재 문서 ID 사용)
                    selectedMajorDocId = document.getString("majorDocId");
                    String displayMajorDocId = (selectedMajorDocId != null && !selectedMajorDocId.isEmpty())
                        ? selectedMajorDocId
                        : documentId;
                    tvCurrentMajorDoc.setText("현재: " + displayMajorDocId);
                    Log.d(TAG, "전공 문서 사용: " + displayMajorDocId);

                    // 저장된 교양 문서 ID 읽기 (없으면 기본값 사용)
                    selectedGeneralDocId = document.getString("generalEducationDocId");
                    String displayGeneralDocId;
                    if (selectedGeneralDocId != null && !selectedGeneralDocId.isEmpty()) {
                        displayGeneralDocId = selectedGeneralDocId;
                    } else {
                        // cohort 필드에서 학번 추출하여 기본값 생성
                        Long cohort = document.getLong("cohort");
                        if (cohort != null) {
                            String year = String.valueOf(cohort);
                            displayGeneralDocId = "교양_공통_" + year;
                        } else {
                            displayGeneralDocId = "없음";
                        }
                    }
                    tvCurrentGeneralDoc.setText("현재: " + displayGeneralDocId);
                    Log.d(TAG, "교양 문서 사용: " + displayGeneralDocId);

                    // Firestore 문서의 모든 필드 로그 출력
                    Log.d(TAG, "========== Firestore 문서 전체 필드 ==========");
                    if (document.exists()) {
                        Map<String, Object> data = document.getData();
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                String key = entry.getKey();
                                Object value = entry.getValue();
                                String valueType = value != null ? value.getClass().getSimpleName() : "null";
                                Log.d(TAG, "필드: " + key + " = " + value + " (타입: " + valueType + ")");
                            }
                        }
                    }
                    Log.d(TAG, "==============================================");

                    displayEditableContent(document);
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "데이터 로드 실패", e);
                    showLoading(false);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayEditableContent(DocumentSnapshot document) {
        coursesContainer.removeAllViews();

        // rules 필드에서 학기별 과목 데이터 추출
        Object rulesObj = document.get("rules");
        if (rulesObj instanceof Map) {
            Map<String, Object> rules = (Map<String, Object>) rulesObj;

            // 전공심화/학부공통 중 0이 아닌 것만 표시 (유틸리티 사용)
            int majorAdvanced = GraduationRequirementUtils.getLongAsInt(document.getLong("전공심화"), 0);
            int departmentCommon = GraduationRequirementUtils.getLongAsInt(document.getLong("학부공통"), 0);

            if (departmentCommon > 0) {
                // 학부공통이 있으면 학부공통필수 표시
                addEditableCourseCategory(rules, "학부공통필수", "#7B1FA2");
            } else if (majorAdvanced > 0) {
                // 전공심화가 있으면 전공심화 표시
                addEditableCourseCategory(rules, "전공심화", "#1976D2");
            }

            // 기본 카테고리 표시
            addEditableCourseCategory(rules, "전공필수", "#1976D2");
            addEditableCourseCategory(rules, "전공선택", "#1976D2");

            // 교양필수는 별도 문서에서 조회
            loadGeneralEducationCourses(document);
        } else {
            Log.e(TAG, "rules 필드를 찾을 수 없습니다");
            Toast.makeText(this, "데이터 구조 오류", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 교양필수 과목은 별도 문서에서 조회
     * selectedGeneralDocId가 있으면 그것을 사용, 없으면 기본값(교양_공통_{year}) 사용
     */
    private void loadGeneralEducationCourses(DocumentSnapshot mainDocument) {
        String generalDocId;

        if (selectedGeneralDocId != null && !selectedGeneralDocId.isEmpty()) {
            // 사용자가 선택한 교양 문서 사용
            generalDocId = selectedGeneralDocId;
            Log.d(TAG, "선택된 교양 문서 사용: " + generalDocId);
        } else {
            // cohort 필드에서 학번 추출하여 기본값 생성
            Long cohort = mainDocument.getLong("cohort");
            if (cohort == null) {
                Log.e(TAG, "cohort 필드가 없고 선택된 교양 문서도 없습니다");
                return;
            }
            String year = String.valueOf(cohort);
            generalDocId = "교양_공통_" + year;
            Log.d(TAG, "기본 교양 문서 사용: " + generalDocId);
        }

        Log.d(TAG, "교양 과목 조회 시작: " + generalDocId);

        db.collection("graduation_requirements")
                .document(generalDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "교양 문서 조회 완료 - 존재 여부: " + doc.exists());
                    if (doc.exists()) {
                        Log.d(TAG, "교양 문서 데이터: " + doc.getData());
                        Object rulesObj = doc.get("rules");
                        Log.d(TAG, "교양 rules 필드: " + rulesObj);

                        // 교양 문서는 requirements 구조로 되어 있음
                        if (rulesObj instanceof Map) {
                            Map<String, Object> rules = (Map<String, Object>) rulesObj;
                            Object requirementsObj = rules.get("requirements");

                            if (requirementsObj instanceof List) {
                                Log.d(TAG, "교양 requirements 구조 발견");
                                List<Object> requirements = (List<Object>) requirementsObj;
                                addGeneralEducationCourses(requirements, "교양필수", "#F57C00", generalDocId);
                            } else {
                                Log.w(TAG, "교양 requirements 필드가 List가 아님");
                            }
                        } else {
                            Log.w(TAG, "교양 rules 필드가 Map이 아님");
                        }
                    } else {
                        Log.w(TAG, "교양 문서 없음: " + generalDocId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "교양 과목 조회 실패: " + generalDocId, e);
                });
    }

    /**
     * requirements 구조에서 교양 과목 추출
     */
    private void addGeneralEducationCourses(List<Object> requirements, String categoryName, String colorHex, String documentId) {
        Log.d(TAG, "=== 교양 카테고리: " + categoryName + " 조회 시작 ===");

        int totalCourseCount = 0;

        // 총 과목 수 계산 (저장용)
        List<Map<String, Object>> coursesList = new ArrayList<>();
        for (Object reqObj : requirements) {
            if (reqObj instanceof Map) {
                Map<String, Object> req = (Map<String, Object>) reqObj;
                String type = (String) req.get("type");

                if ("single".equals(type)) {
                    totalCourseCount++;
                    String name = (String) req.get("name");
                    Object creditObj = req.get("credit");
                    if (name != null && creditObj != null) {
                        Map<String, Object> courseMap = new HashMap<>();
                        courseMap.put("name", name);
                        courseMap.put("credit", creditObj instanceof Long ? ((Long) creditObj).intValue() : creditObj);
                        courseMap.put("semester", "교양");
                        coursesList.add(courseMap);
                    }
                } else if ("oneOf".equals(type)) {
                    Object optionsObj = req.get("options");
                    if (optionsObj instanceof List) {
                        List<Object> options = (List<Object>) optionsObj;
                        totalCourseCount += options.size();
                        for (Object optObj : options) {
                            if (optObj instanceof Map) {
                                Map<String, Object> opt = (Map<String, Object>) optObj;
                                String name = (String) opt.get("name");
                                if (name != null) {
                                    Object creditObj = req.get("credit");
                                    Map<String, Object> courseMap = new HashMap<>();
                                    courseMap.put("name", name);
                                    courseMap.put("credit", creditObj instanceof Long ? ((Long) creditObj).intValue() : creditObj);
                                    courseMap.put("semester", "교양");
                                    coursesList.add(courseMap);
                                }
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, categoryName + " - 총 과목 수: " + totalCourseCount);

        // 편집 중인 데이터에 저장
        editingCourses.put(categoryName, coursesList);

        // 교양 과목은 편집 불가로 표시만 (requirements 구조가 다르므로)
        if (totalCourseCount > 0) {
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
            cardContent.setPadding(UiUtils.dpToPx(this, 20), UiUtils.dpToPx(this, 20), UiUtils.dpToPx(this, 20), UiUtils.dpToPx(this, 20));

            // 제목 (문서 ID 포함)
            TextView titleView = new TextView(this);
            titleView.setText(categoryName + " (" + totalCourseCount + "개) - " + documentId);
            titleView.setTextSize(18);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            titleView.setTextColor(android.graphics.Color.parseColor(colorHex));
            cardContent.addView(titleView);

            // requirements 각 항목 표시 (그룹화)
            for (Object reqObj : requirements) {
                if (reqObj instanceof Map) {
                    Map<String, Object> req = (Map<String, Object>) reqObj;
                    String type = (String) req.get("type");

                    if ("single".equals(type)) {
                        // 단일 필수 과목
                        String name = (String) req.get("name");
                        Object creditObj = req.get("credit");
                        if (name != null && creditObj != null) {
                            TextView courseView = new TextView(this);
                            courseView.setText("• " + name + " (" + creditObj + "학점)");
                            courseView.setTextSize(14);
                            courseView.setTextColor(0xFF424242);
                            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            textParams.setMargins(0, UiUtils.dpToPx(this,8), 0, 0);
                            courseView.setLayoutParams(textParams);
                            cardContent.addView(courseView);
                        }
                    } else if ("oneOf".equals(type)) {
                        // 선택 과목군 (그룹 헤더 + 옵션들)
                        Object creditObj = req.get("credit");
                        Object minObj = req.get("min");
                        int min = minObj instanceof Long ? ((Long) minObj).intValue() : 1;

                        // 그룹 헤더
                        TextView groupHeader = new TextView(this);
                        groupHeader.setText("▼ 다음 중 " + min + "개 선택 (" + creditObj + "학점)");
                        groupHeader.setTextSize(15);
                        groupHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                        groupHeader.setTextColor(android.graphics.Color.parseColor(colorHex));
                        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        headerParams.setMargins(0, UiUtils.dpToPx(this,12), 0, UiUtils.dpToPx(this,4));
                        groupHeader.setLayoutParams(headerParams);
                        cardContent.addView(groupHeader);

                        // 옵션 과목들 (들여쓰기)
                        Object optionsObj = req.get("options");
                        if (optionsObj instanceof List) {
                            List<Object> options = (List<Object>) optionsObj;
                            for (Object optObj : options) {
                                if (optObj instanceof Map) {
                                    Map<String, Object> opt = (Map<String, Object>) optObj;
                                    String name = (String) opt.get("name");
                                    if (name != null) {
                                        TextView optionView = new TextView(this);
                                        optionView.setText("    - " + name);
                                        optionView.setTextSize(14);
                                        optionView.setTextColor(0xFF616161);
                                        LinearLayout.LayoutParams optParams = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        optParams.setMargins(UiUtils.dpToPx(this,16), UiUtils.dpToPx(this,4), 0, 0);
                                        optionView.setLayoutParams(optParams);
                                        cardContent.addView(optionView);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            card.addView(cardContent);
            coursesContainer.addView(card);
        }
    }

    /**
     * rules Map에서 학기별로 흩어진 과목들을 카테고리별로 모아서 표시
     */
    private void addEditableCourseCategory(Map<String, Object> rules, String categoryName, String colorHex) {
        Log.d(TAG, "=== 카테고리: " + categoryName + " 조회 시작 ===");

        List<Map<String, Object>> coursesList = new ArrayList<>();

        // rules의 모든 학기를 순회하면서 해당 카테고리의 과목 수집
        for (Map.Entry<String, Object> semesterEntry : rules.entrySet()) {
            String semester = semesterEntry.getKey(); // 예: "1학년 1학기"
            Object semesterDataObj = semesterEntry.getValue();

            if (semesterDataObj instanceof Map) {
                Map<String, Object> semesterData = (Map<String, Object>) semesterDataObj;

                // 해당 카테고리의 과목 목록 가져오기
                Object categoryCoursesObj = semesterData.get(categoryName);

                if (categoryCoursesObj instanceof List) {
                    List<Object> categoryCourses = (List<Object>) categoryCoursesObj;

                    for (Object courseObj : categoryCourses) {
                        if (courseObj instanceof Map) {
                            Map<String, Object> course = (Map<String, Object>) courseObj;
                            // 과목명과 학점을 name, credit으로 매핑
                            Map<String, Object> courseMap = new HashMap<>();
                            courseMap.put("name", course.get("과목명"));
                            courseMap.put("credit", course.get("학점"));
                            courseMap.put("semester", semester); // 학기 정보 추가
                            coursesList.add(courseMap);
                            Log.d(TAG, categoryName + " - " + semester + ": " + course.get("과목명"));
                        }
                    }
                }
            }
        }

        Log.d(TAG, categoryName + " - 총 과목 수: " + coursesList.size());

        // 학기순으로 정렬 (유틸리티 사용)
        coursesList.sort((c1, c2) -> {
            String s1 = (String) c1.get("semester");
            String s2 = (String) c2.get("semester");
            if (s1 == null) s1 = "";
            if (s2 == null) s2 = "";
            return GraduationRequirementUtils.getSemesterOrder(s1) - GraduationRequirementUtils.getSemesterOrder(s2);
        });

        // 편집 중인 데이터에 저장
        editingCourses.put(categoryName, coursesList);

        // 카테고리 카드 생성
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, UiUtils.dpToPx(this,16));
        card.setLayoutParams(cardParams);
        card.setRadius(UiUtils.dpToPx(this,12));
        card.setCardElevation(UiUtils.dpToPx(this,2));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(UiUtils.dpToPx(this,20), UiUtils.dpToPx(this,20), UiUtils.dpToPx(this,20), UiUtils.dpToPx(this,20));

        // 카테고리 제목 + 추가 버튼
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(categoryName + " (" + coursesList.size() + "개)");
        titleView.setTextSize(16);
        titleView.setTextColor(android.graphics.Color.parseColor(colorHex));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        titleView.setLayoutParams(titleParams);
        headerLayout.addView(titleView);

        // 추가 버튼
        MaterialButton btnAdd = new MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle);
        btnAdd.setText("+");
        btnAdd.setOnClickListener(v -> showAddCourseDialog(categoryName, colorHex));
        headerLayout.addView(btnAdd);

        cardContent.addView(headerLayout);

        // 과목 목록 컨테이너
        LinearLayout coursesListLayout = new LinearLayout(this);
        coursesListLayout.setOrientation(LinearLayout.VERTICAL);
        coursesListLayout.setPadding(0, UiUtils.dpToPx(this,12), 0, 0);

        // 과목 목록
        for (int i = 0; i < coursesList.size(); i++) {
            Map<String, Object> course = coursesList.get(i);
            addEditableCourseItem(coursesListLayout, course, categoryName, i);
        }

        cardContent.addView(coursesListLayout);
        card.addView(cardContent);
        coursesContainer.addView(card);
    }

    private void addEditableCourseItem(LinearLayout parent, Map<String, Object> course, String categoryName, int index) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, UiUtils.dpToPx(this,8));
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
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        nameView.setLayoutParams(nameParams);
        itemLayout.addView(nameView);

        // 수정 버튼
        MaterialButton btnEdit = new MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle);
        btnEdit.setText("수정");
        btnEdit.setTextSize(11);
        btnEdit.setOnClickListener(v -> showEditCourseDialog(categoryName, index));
        itemLayout.addView(btnEdit);

        // 삭제 버튼
        MaterialButton btnDelete = new MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle);
        btnDelete.setText("삭제");
        btnDelete.setTextSize(11);
        btnDelete.setOnClickListener(v -> deleteCourse(categoryName, index));
        itemLayout.addView(btnDelete);

        parent.addView(itemLayout);
    }

    private void showAddCourseDialog(String categoryName, String colorHex) {
        // 먼저 학기 선택
        final String[] semesters = {"1학년 1학기", "1학년 2학기", "2학년 1학기", "2학년 2학기",
                                     "3학년 1학기", "3학년 2학기", "4학년 1학기", "4학년 2학기"};

        new AlertDialog.Builder(this)
                .setTitle("학기 선택")
                .setItems(semesters, (dialog, which) -> {
                    String selectedSemester = semesters[which];
                    showCourseInputDialog(categoryName, selectedSemester, null, -1);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showCourseInputDialog(String categoryName, String semester, Map<String, Object> existingCourse, int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(categoryName + " 과목 " + (existingCourse == null ? "추가" : "수정"));

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_course_input, null);
        EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        EditText etCourseCredit = dialogView.findViewById(R.id.et_course_credit);

        if (existingCourse != null) {
            etCourseName.setText(existingCourse.get("name").toString());
            Object creditObj = existingCourse.get("credit");
            if (creditObj != null) {
                etCourseCredit.setText(creditObj.toString());
            }
        }

        builder.setView(dialogView);
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

            List<Map<String, Object>> courses = editingCourses.get(categoryName);
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
                Object rulesObj = currentDocument.get("rules");
                if (rulesObj instanceof Map) {
                    displayEditableContent(currentDocument);
                }
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void showEditCourseDialog(String categoryName, int index) {
        List<Map<String, Object>> courses = editingCourses.get(categoryName);
        if (courses == null || index >= courses.size()) return;

        Map<String, Object> course = courses.get(index);
        String semester = (String) course.get("semester");

        showCourseInputDialog(categoryName, semester, course, index);
    }

    private void deleteCourse(String categoryName, int index) {
        new AlertDialog.Builder(this)
                .setTitle("과목 삭제")
                .setMessage("이 과목을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    List<Map<String, Object>> courses = editingCourses.get(categoryName);
                    if (courses != null && index < courses.size()) {
                        courses.remove(index);
                        // 화면 새로고침
                        Object rulesObj = currentDocument.get("rules");
                        if (rulesObj instanceof Map) {
                            displayEditableContent(currentDocument);
                        }
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveChanges() {
        showLoading(true);

        // 기존 rules 구조 가져오기
        Object rulesObj = currentDocument.get("rules");
        if (!(rulesObj instanceof Map)) {
            Toast.makeText(this, "데이터 구조 오류", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Map<String, Object> rules = (Map<String, Object>) rulesObj;
        final Map<String, Object>[] generalRulesHolder = new Map[]{null}; // 교양 과목용 (final wrapper)

        // 편집된 과목들을 학기별로 다시 분류
        for (Map.Entry<String, List<Map<String, Object>>> entry : editingCourses.entrySet()) {
            String categoryName = entry.getKey();
            List<Map<String, Object>> courses = entry.getValue();

            // 교양필수는 별도 처리
            if (categoryName.equals("교양필수")) {
                generalRulesHolder[0] = new HashMap<>();
                Map<String, List<Map<String, Object>>> coursesBySemester = new HashMap<>();

                for (Map<String, Object> course : courses) {
                    String semester = (String) course.get("semester");
                    if (semester == null) semester = "1학년 1학기";

                    if (!coursesBySemester.containsKey(semester)) {
                        coursesBySemester.put(semester, new ArrayList<>());
                    }

                    Map<String, Object> firestoreCourse = new HashMap<>();
                    firestoreCourse.put("과목명", course.get("name"));
                    firestoreCourse.put("학점", course.get("credit"));
                    coursesBySemester.get(semester).add(firestoreCourse);
                }

                // 교양 rules 구조 생성
                for (Map.Entry<String, List<Map<String, Object>>> semesterEntry : coursesBySemester.entrySet()) {
                    Map<String, Object> semesterData = new HashMap<>();
                    semesterData.put(categoryName, semesterEntry.getValue());
                    generalRulesHolder[0].put(semesterEntry.getKey(), semesterData);
                }
                continue;
            }

            // 일반 과목 (학부공통필수, 전공필수, 전공선택)
            Map<String, List<Map<String, Object>>> coursesBySemester = new HashMap<>();

            for (Map<String, Object> course : courses) {
                String semester = (String) course.get("semester");
                if (semester == null) semester = "1학년 1학기";

                if (!coursesBySemester.containsKey(semester)) {
                    coursesBySemester.put(semester, new ArrayList<>());
                }

                Map<String, Object> firestoreCourse = new HashMap<>();
                firestoreCourse.put("과목명", course.get("name"));
                firestoreCourse.put("학점", course.get("credit"));
                coursesBySemester.get(semester).add(firestoreCourse);
            }

            // rules에 업데이트
            for (Map.Entry<String, List<Map<String, Object>>> semesterEntry : coursesBySemester.entrySet()) {
                String semester = semesterEntry.getKey();
                List<Map<String, Object>> semesterCourses = semesterEntry.getValue();

                Object semesterDataObj = rules.get(semester);
                if (semesterDataObj instanceof Map) {
                    Map<String, Object> semesterData = (Map<String, Object>) semesterDataObj;
                    semesterData.put(categoryName, semesterCourses);
                }
            }
        }

        // 메인 문서 저장
        Map<String, Object> updates = new HashMap<>();
        updates.put("rules", rules);

        // 선택된 전공 문서 ID도 저장
        if (selectedMajorDocId != null && !selectedMajorDocId.isEmpty()) {
            updates.put("majorDocId", selectedMajorDocId);
            Log.d(TAG, "전공 문서 ID 저장: " + selectedMajorDocId);
        }

        // 선택된 교양 문서 ID도 저장
        if (selectedGeneralDocId != null && !selectedGeneralDocId.isEmpty()) {
            updates.put("generalEducationDocId", selectedGeneralDocId);
            Log.d(TAG, "교양 문서 ID 저장: " + selectedGeneralDocId);
        }

        db.collection("graduation_requirements")
                .document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 교양 문서도 저장 (있는 경우)
                    if (generalRulesHolder[0] != null) {
                        saveGeneralEducationCourses(generalRulesHolder[0]);
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "저장 실패", e);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 교양 과목 저장 (별도 문서)
     */
    private void saveGeneralEducationCourses(Map<String, Object> generalRules) {
        Long cohort = currentDocument.getLong("cohort");
        if (cohort == null) {
            showLoading(false);
            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String year = String.valueOf(cohort);
        String generalDocId = "교양_공통_" + year;

        Map<String, Object> updates = new HashMap<>();
        updates.put("rules", generalRules);

        db.collection("graduation_requirements")
                .document(generalDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "교양 과목 저장 실패", e);
                    Toast.makeText(this, "교양 과목 저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * 선택한 전공 문서로 미리보기 로드
     */
    private void loadMajorDocumentPreview(String majorDocId) {
        Log.d(TAG, "전공 문서 미리보기 로드: " + majorDocId);

        db.collection("graduation_requirements").document(majorDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // 전체 화면 새로고침
                        coursesContainer.removeAllViews();
                        displayEditableContent(doc);
                        Log.d(TAG, "전공 문서 미리보기 로드 완료");
                    } else {
                        Toast.makeText(this, "문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 문서 로드 실패", e);
                    Toast.makeText(this, "문서 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 선택한 교양 문서로 미리보기 즉시 반영
     */
    private void refreshGeneralEducationPreview() {
        Log.d(TAG, "교양 문서 미리보기 갱신");
        // 교양 문서만 다시 로드 (전공은 유지)
        coursesContainer.removeAllViews();
        displayEditableContent(currentDocument);
    }

    /**
     * 전공 문서 선택 다이얼로그 표시
     */
    private void showMajorDocumentSelector() {
        Log.d(TAG, "전공 문서 선택 다이얼로그 시작");

        // 현재 문서 ID에서 학부/학과 정보 추출 (유틸리티 사용)
        String department = GraduationRequirementUtils.getDepartmentFromDocId(documentId);
        if (department == null) {
            Toast.makeText(this, "문서 ID 형식 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        // 해당 학부/학과로 시작하는 모든 전공 문서 조회 (교양 제외)
        db.collection("graduation_requirements")
                .whereGreaterThanOrEqualTo("__name__", department + "_")
                .whereLessThan("__name__", department + "_" + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> docIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docId = doc.getId();
                        // 교양 문서는 제외
                        if (!docId.startsWith("교양_")) {
                            docIds.add(docId);
                        }
                    }

                    Log.d(TAG, "전공 문서 목록 조회 성공: " + docIds.size() + "개");

                    if (docIds.isEmpty()) {
                        Toast.makeText(this, "전공 문서가 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 다이얼로그로 선택
                    String[] docArray = docIds.toArray(new String[0]);
                    new AlertDialog.Builder(this)
                            .setTitle("전공 문서 선택")
                            .setItems(docArray, (dialog, which) -> {
                                selectedMajorDocId = docArray[which];
                                tvCurrentMajorDoc.setText("현재: " + selectedMajorDocId);
                                Log.d(TAG, "전공 문서 선택됨: " + selectedMajorDocId);

                                // 선택한 전공 문서로 미리보기 즉시 로드
                                loadMajorDocumentPreview(selectedMajorDocId);

                                Toast.makeText(this, "선택: " + selectedMajorDocId, Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("취소", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 문서 목록 조회 실패", e);
                    Toast.makeText(this, "조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 교양 문서 선택 다이얼로그 표시
     */
    private void showGeneralDocumentSelector() {
        Log.d(TAG, "교양 문서 선택 다이얼로그 시작");

        // "교양_"으로 시작하는 모든 문서 조회
        db.collection("graduation_requirements")
                .whereGreaterThanOrEqualTo("__name__", "교양_")
                .whereLessThan("__name__", "교양_" + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> docIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        docIds.add(doc.getId());
                    }

                    Log.d(TAG, "교양 문서 목록 조회 성공: " + docIds.size() + "개");

                    if (docIds.isEmpty()) {
                        Toast.makeText(this, "교양 문서가 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 다이얼로그로 선택
                    String[] docArray = docIds.toArray(new String[0]);
                    new AlertDialog.Builder(this)
                            .setTitle("교양 문서 선택")
                            .setItems(docArray, (dialog, which) -> {
                                selectedGeneralDocId = docArray[which];
                                tvCurrentGeneralDoc.setText("현재: " + selectedGeneralDocId);
                                Log.d(TAG, "교양 문서 선택됨: " + selectedGeneralDocId);

                                // 선택 후 교양 과목 미리보기 즉시 반영
                                refreshGeneralEducationPreview();

                                Toast.makeText(this, "선택: " + selectedGeneralDocId, Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("취소", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "교양 문서 목록 조회 실패", e);
                    Toast.makeText(this, "조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
