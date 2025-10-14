package sprout.app.sakmvp1;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 교양 문서 편집 Activity
 * rules.requirements 구조의 과목 목록을 편집
 */
public class EducationDocumentEditActivity extends AppCompatActivity {

    private static final String TAG = "EduDocEdit";

    private MaterialToolbar toolbar;
    private TextInputEditText etDocId, etGeneralRequired, etGeneralElective, etLiberalArts;
    private LinearLayout requirementsContainer;
    private MaterialButton btnSave, btnAddRequirement;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String documentId;
    private boolean isNew;

    // requirements 목록
    private List<Map<String, Object>> requirementsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education_document_edit);

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
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etDocId = findViewById(R.id.et_doc_id);
        etGeneralRequired = findViewById(R.id.et_general_required);
        etGeneralElective = findViewById(R.id.et_general_elective);
        etLiberalArts = findViewById(R.id.et_liberal_arts);
        requirementsContainer = findViewById(R.id.requirements_container);
        btnSave = findViewById(R.id.btn_save);
        btnAddRequirement = findViewById(R.id.btn_add_requirement);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isNew ? "교양 문서 추가" : "교양 문서 편집");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> validateAndSave());
        btnAddRequirement.setOnClickListener(v -> showAddRequirementDialog());
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

        // 학점 필드 로드
        Long genRequired = document.getLong("교양필수");
        Long genElective = document.getLong("교양선택");
        Long liberal = document.getLong("소양");

        if (genRequired != null) etGeneralRequired.setText(String.valueOf(genRequired));
        if (genElective != null) etGeneralElective.setText(String.valueOf(genElective));
        if (liberal != null) etLiberalArts.setText(String.valueOf(liberal));

        // rules.requirements 로드
        Object rulesObj = document.get("rules");
        if (rulesObj instanceof Map) {
            Map<String, Object> rules = (Map<String, Object>) rulesObj;
            Object requirementsObj = rules.get("requirements");

            if (requirementsObj instanceof List) {
                List<Object> requirements = (List<Object>) requirementsObj;
                requirementsList.clear();

                for (Object reqObj : requirements) {
                    if (reqObj instanceof Map) {
                        requirementsList.add((Map<String, Object>) reqObj);
                    }
                }

                Log.d(TAG, "로드된 requirements: " + requirementsList.size() + "개");
                displayRequirements();
            }
        }
    }

    private void displayRequirements() {
        requirementsContainer.removeAllViews();

        for (int i = 0; i < requirementsList.size(); i++) {
            Map<String, Object> req = requirementsList.get(i);
            addRequirementCard(req, i);
        }
    }

    private void addRequirementCard(Map<String, Object> req, int index) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 24);
        card.setLayoutParams(cardParams);
        card.setCardElevation(4);
        card.setRadius(12);
        card.setUseCompatPadding(true);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(32, 32, 32, 32);

        // 타입 표시
        String type = (String) req.get("type");
        String displayText = "";

        if ("single".equals(type)) {
            String name = (String) req.get("name");
            Object creditObj = req.get("credit");
            int credit = creditObj instanceof Long ? ((Long) creditObj).intValue() : (int) creditObj;
            displayText = String.format("📌 필수 과목\n과목명: %s\n학점: %d", name, credit);
        } else if ("oneOf".equals(type)) {
            Object creditObj = req.get("credit");
            int credit = creditObj instanceof Long ? ((Long) creditObj).intValue() : (int) creditObj;
            Object minObj = req.get("min");
            int min = minObj instanceof Long ? ((Long) minObj).intValue() : (int) minObj;

            List<String> optionNames = new ArrayList<>();
            Object optionsObj = req.get("options");
            if (optionsObj instanceof List) {
                for (Object opt : (List<Object>) optionsObj) {
                    if (opt instanceof Map) {
                        String optName = (String) ((Map<String, Object>) opt).get("name");
                        if (optName != null) optionNames.add(optName);
                    }
                }
            }

            displayText = String.format("🔀 선택 과목 (최소 %d개)\n학점: %d\n선택 가능 과목:\n  - %s",
                min, credit, String.join("\n  - ", optionNames));
        }

        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(displayText);
        textView.setTextSize(14);
        textView.setTextColor(0xFF424242);

        cardContent.addView(textView);

        // 버튼 영역
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayoutParams.setMargins(0, 16, 0, 0);
        buttonLayout.setLayoutParams(buttonLayoutParams);

        MaterialButton btnEdit = new MaterialButton(this);
        btnEdit.setText("수정");
        btnEdit.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));
        btnEdit.setOnClickListener(v -> showEditRequirementDialog(index));

        MaterialButton btnDelete = new MaterialButton(this);
        btnDelete.setText("삭제");
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        deleteParams.setMargins(16, 0, 0, 0);
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("과목 삭제")
                .setMessage("이 과목을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    requirementsList.remove(index);
                    displayRequirements();
                })
                .setNegativeButton("취소", null)
                .show();
        });

        buttonLayout.addView(btnEdit);
        buttonLayout.addView(btnDelete);
        cardContent.addView(buttonLayout);

        card.addView(cardContent);
        requirementsContainer.addView(card);
    }

    private void showAddRequirementDialog() {
        String[] types = {"필수 과목 (single)", "선택 과목 (oneOf)"};

        new AlertDialog.Builder(this)
            .setTitle("과목 타입 선택")
            .setItems(types, (dialog, which) -> {
                if (which == 0) {
                    showSingleCourseDialog(-1);
                } else {
                    showOneOfCourseDialog(-1);
                }
            })
            .show();
    }

    private void showEditRequirementDialog(int index) {
        Map<String, Object> req = requirementsList.get(index);
        String type = (String) req.get("type");

        if ("single".equals(type)) {
            showSingleCourseDialog(index);
        } else if ("oneOf".equals(type)) {
            showOneOfCourseDialog(index);
        }
    }

    private void showSingleCourseDialog(int editIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(editIndex >= 0 ? "필수 과목 수정" : "필수 과목 추가");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextInputEditText nameInput = new TextInputEditText(this);
        nameInput.setHint("과목명");

        TextInputEditText creditInput = new TextInputEditText(this);
        creditInput.setHint("학점");
        creditInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (editIndex >= 0) {
            Map<String, Object> req = requirementsList.get(editIndex);
            nameInput.setText((String) req.get("name"));
            Object creditObj = req.get("credit");
            creditInput.setText(String.valueOf(creditObj));
        }

        layout.addView(nameInput);
        layout.addView(creditInput);

        builder.setView(layout);
        builder.setPositiveButton(editIndex >= 0 ? "수정" : "추가", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String creditStr = creditInput.getText().toString().trim();

            if (name.isEmpty() || creditStr.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> req = new HashMap<>();
            req.put("type", "single");
            req.put("name", name);
            req.put("credit", Integer.parseInt(creditStr));

            if (editIndex >= 0) {
                requirementsList.set(editIndex, req);
            } else {
                requirementsList.add(req);
            }
            displayRequirements();
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void showOneOfCourseDialog(int editIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(editIndex >= 0 ? "선택 과목 수정" : "선택 과목 추가");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextInputEditText minInput = new TextInputEditText(this);
        minInput.setHint("최소 선택 개수");
        minInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        TextInputEditText creditInput = new TextInputEditText(this);
        creditInput.setHint("학점");
        creditInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        TextInputEditText optionsInput = new TextInputEditText(this);
        optionsInput.setHint("과목명 목록 (쉼표로 구분)");

        if (editIndex >= 0) {
            Map<String, Object> req = requirementsList.get(editIndex);
            Object minObj = req.get("min");
            minInput.setText(String.valueOf(minObj));
            Object creditObj = req.get("credit");
            creditInput.setText(String.valueOf(creditObj));

            List<String> optionNames = new ArrayList<>();
            Object optionsObj = req.get("options");
            if (optionsObj instanceof List) {
                for (Object opt : (List<Object>) optionsObj) {
                    if (opt instanceof Map) {
                        String optName = (String) ((Map<String, Object>) opt).get("name");
                        if (optName != null) optionNames.add(optName);
                    }
                }
            }
            optionsInput.setText(String.join(", ", optionNames));
        }

        layout.addView(minInput);
        layout.addView(creditInput);
        layout.addView(optionsInput);

        builder.setView(layout);
        builder.setPositiveButton(editIndex >= 0 ? "수정" : "추가", (dialog, which) -> {
            String minStr = minInput.getText().toString().trim();
            String creditStr = creditInput.getText().toString().trim();
            String optionsStr = optionsInput.getText().toString().trim();

            if (minStr.isEmpty() || creditStr.isEmpty() || optionsStr.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Map<String, Object>> options = new ArrayList<>();
            for (String optName : optionsStr.split(",")) {
                String trimmed = optName.trim();
                if (!trimmed.isEmpty()) {
                    Map<String, Object> opt = new HashMap<>();
                    opt.put("name", trimmed);
                    options.add(opt);
                }
            }

            Map<String, Object> req = new HashMap<>();
            req.put("type", "oneOf");
            req.put("min", Integer.parseInt(minStr));
            req.put("credit", Integer.parseInt(creditStr));
            req.put("options", options);

            if (editIndex >= 0) {
                requirementsList.set(editIndex, req);
            } else {
                requirementsList.add(req);
            }
            displayRequirements();
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void validateAndSave() {
        String docId = etDocId.getText() != null ? etDocId.getText().toString().trim() : "";
        String genReqStr = etGeneralRequired.getText() != null ? etGeneralRequired.getText().toString().trim() : "0";
        String genElecStr = etGeneralElective.getText() != null ? etGeneralElective.getText().toString().trim() : "0";
        String liberalStr = etLiberalArts.getText() != null ? etLiberalArts.getText().toString().trim() : "0";

        if (docId.isEmpty()) {
            etDocId.setError("문서 ID를 입력하세요");
            return;
        }

        if (!docId.startsWith("교양_")) {
            etDocId.setError("교양 문서 ID는 '교양_'로 시작해야 합니다");
            return;
        }

        int genRequired, genElective, liberal;
        try {
            genRequired = Integer.parseInt(genReqStr);
            genElective = Integer.parseInt(genElecStr);
            liberal = Integer.parseInt(liberalStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "학점은 숫자로 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 저장
        saveDocument(docId, genRequired, genElective, liberal);
    }

    private void saveDocument(String docId, int genRequired, int genElective, int liberal) {
        showLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("교양필수", genRequired);
        data.put("교양선택", genElective);
        data.put("소양", liberal);

        // rules 구조 생성
        Map<String, Object> rules = new HashMap<>();
        rules.put("requirements", requirementsList);
        rules.put("counts", new HashMap<String, Object>() {{
            put("tlc", 6);
            put("chapel", 6);
        }});
        data.put("rules", rules);

        // 저장 전 데이터 로그 출력
        Log.d(TAG, "========== Firestore 저장 시작 ==========");
        Log.d(TAG, "문서 ID: " + docId);
        Log.d(TAG, "교양필수: " + genRequired);
        Log.d(TAG, "교양선택: " + genElective);
        Log.d(TAG, "소양: " + liberal);
        Log.d(TAG, "requirements 개수: " + requirementsList.size());
        for (int i = 0; i < requirementsList.size(); i++) {
            Map<String, Object> req = requirementsList.get(i);
            Log.d(TAG, "  [" + i + "] type=" + req.get("type") +
                       ", name=" + req.get("name") +
                       ", credit=" + req.get("credit") +
                       ", min=" + req.get("min"));
        }
        Log.d(TAG, "========================================");

        db.collection("graduation_requirements")
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Log.d(TAG, "========== Firestore 저장 성공 ==========");
                    Log.d(TAG, "문서 ID: " + docId + " 저장 완료");
                    Log.d(TAG, "======================================");
                    Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "========== Firestore 저장 실패 ==========");
                    Log.e(TAG, "문서 ID: " + docId);
                    Log.e(TAG, "에러: " + e.getMessage(), e);
                    Log.e(TAG, "======================================");
                    Toast.makeText(this, "저장 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        btnAddRequirement.setEnabled(!show);
    }
}
