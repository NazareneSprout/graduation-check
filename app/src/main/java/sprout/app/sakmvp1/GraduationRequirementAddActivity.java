package sprout.app.sakmvp1;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraduationRequirementAddActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etDepartment, etTrack, etCohort;
    private TextView tvMajorDoc, tvGeneralDoc;
    private MaterialButton btnSelectMajorDoc, btnSelectGeneralDoc, btnSave;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String selectedMajorDocId; // 선택된 전공 문서 ID
    private String selectedGeneralDocId; // 선택된 교양 문서 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graduation_requirement_add);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etDepartment = findViewById(R.id.et_department);
        etTrack = findViewById(R.id.et_track);
        etCohort = findViewById(R.id.et_cohort);
        tvMajorDoc = findViewById(R.id.tv_major_doc);
        tvGeneralDoc = findViewById(R.id.tv_general_doc);
        btnSelectMajorDoc = findViewById(R.id.btn_select_major_doc);
        btnSelectGeneralDoc = findViewById(R.id.btn_select_general_doc);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSelectMajorDoc.setOnClickListener(v -> showMajorDocumentSelector());
        btnSelectGeneralDoc.setOnClickListener(v -> showGeneralDocumentSelector());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void showMajorDocumentSelector() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    List<String> documentNames = new ArrayList<>();
                    documentNames.add("+ 새로 생성"); // 첫 번째 옵션: 새로 생성

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // 교양 문서는 제외
                        if (!document.getId().startsWith("교양_")) {
                            documentNames.add(document.getId());
                        }
                    }

                    String[] docArray = documentNames.toArray(new String[0]);

                    new AlertDialog.Builder(this)
                            .setTitle("전공 문서 선택")
                            .setItems(docArray, (dialog, which) -> {
                                if (which == 0) {
                                    // "새로 생성" 선택
                                    selectedMajorDocId = null;
                                    tvMajorDoc.setText("현재: 새로 생성");
                                } else {
                                    selectedMajorDocId = docArray[which];
                                    tvMajorDoc.setText("현재: " + selectedMajorDocId);
                                }
                            })
                            .setNegativeButton("취소", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "문서 조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showGeneralDocumentSelector() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    List<String> documentNames = new ArrayList<>();
                    documentNames.add("+ 새로 생성"); // 첫 번째 옵션: 새로 생성

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // 교양 문서만 포함
                        if (document.getId().startsWith("교양_")) {
                            documentNames.add(document.getId());
                        }
                    }

                    String[] docArray = documentNames.toArray(new String[0]);

                    new AlertDialog.Builder(this)
                            .setTitle("교양 문서 선택")
                            .setItems(docArray, (dialog, which) -> {
                                if (which == 0) {
                                    // "새로 생성" 선택
                                    selectedGeneralDocId = null;
                                    tvGeneralDoc.setText("현재: 새로 생성");
                                } else {
                                    selectedGeneralDocId = docArray[which];
                                    tvGeneralDoc.setText("현재: " + selectedGeneralDocId);
                                }
                            })
                            .setNegativeButton("취소", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "문서 조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void validateAndSave() {
        String department = etDepartment.getText() != null ? etDepartment.getText().toString().trim() : "";
        String track = etTrack.getText() != null ? etTrack.getText().toString().trim() : "";
        String cohortStr = etCohort.getText() != null ? etCohort.getText().toString().trim() : "";

        // 유효성 검사
        if (department.isEmpty()) {
            etDepartment.setError("학부/학과를 입력하세요");
            etDepartment.requestFocus();
            return;
        }

        if (track.isEmpty()) {
            etTrack.setError("트랙을 입력하세요");
            etTrack.requestFocus();
            return;
        }

        if (cohortStr.isEmpty()) {
            etCohort.setError("학번을 입력하세요");
            etCohort.requestFocus();
            return;
        }

        int cohort;
        try {
            cohort = Integer.parseInt(cohortStr);
        } catch (NumberFormatException e) {
            etCohort.setError("올바른 학번을 입력하세요");
            etCohort.requestFocus();
            return;
        }

        // 문서 ID 생성
        String newDocId = department + "_" + track + "_" + cohort;

        // 중복 확인 후 저장
        checkDuplicateAndSave(newDocId, department, track, cohort);
    }

    private void checkDuplicateAndSave(String newDocId, String department, String track, int cohort) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("graduation_requirements")
                .document(newDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        progressBar.setVisibility(View.GONE);
                        new AlertDialog.Builder(this)
                                .setTitle("중복 확인")
                                .setMessage("이미 동일한 졸업요건이 존재합니다.\n덮어쓰시겠습니까?")
                                .setPositiveButton("덮어쓰기", (dialog, which) -> {
                                    saveNewRequirement(newDocId, department, track, cohort);
                                })
                                .setNegativeButton("취소", null)
                                .show();
                    } else {
                        saveNewRequirement(newDocId, department, track, cohort);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "중복 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNewRequirement(String newDocId, String department, String track, int cohort) {
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> newRequirement = new HashMap<>();

        // 기본 정보 설정
        newRequirement.put("department", department);
        newRequirement.put("track", track);
        newRequirement.put("cohort", cohort);

        // 전공 문서 참조 설정
        if (selectedMajorDocId != null && !selectedMajorDocId.isEmpty()) {
            newRequirement.put("majorDocId", selectedMajorDocId);
        }

        // 교양 문서 참조 설정
        if (selectedGeneralDocId != null && !selectedGeneralDocId.isEmpty()) {
            newRequirement.put("generalEducationDocId", selectedGeneralDocId);
        }

        // 선택된 문서가 있는 경우 해당 문서들에서 데이터 복사
        if (selectedMajorDocId != null || selectedGeneralDocId != null) {
            loadAndMergeDocuments(newDocId, newRequirement, department, track, cohort);
        } else {
            // 새로 생성하는 경우 기본 구조 설정
            newRequirement.put("전공필수", new HashMap<String, Object>());
            newRequirement.put("전공선택", new HashMap<String, Object>());
            newRequirement.put("교양필수", new HashMap<String, Object>());
            newRequirement.put("교양선택", new HashMap<String, Object>());
            newRequirement.put("총학점", 0);
            newRequirement.put("전공필수학점", 0);
            newRequirement.put("전공선택학점", 0);
            newRequirement.put("교양필수학점", 0);
            newRequirement.put("교양선택학점", 0);

            saveToFirestore(newDocId, newRequirement);
        }
    }

    private void loadAndMergeDocuments(String newDocId, Map<String, Object> newRequirement, String department, String track, int cohort) {
        // 전공 문서와 교양 문서를 각각 로드하여 병합
        Map<String, Object> majorData = new HashMap<>();
        Map<String, Object> generalData = new HashMap<>();

        // 전공 문서 로드
        if (selectedMajorDocId != null) {
            db.collection("graduation_requirements").document(selectedMajorDocId)
                    .get()
                    .addOnSuccessListener(majorDoc -> {
                        if (majorDoc.exists()) {
                            Map<String, Object> data = majorDoc.getData();
                            if (data != null) {
                                // 전공 관련 필드만 복사
                                if (data.containsKey("전공필수")) majorData.put("전공필수", data.get("전공필수"));
                                if (data.containsKey("전공선택")) majorData.put("전공선택", data.get("전공선택"));
                                if (data.containsKey("전공심화")) majorData.put("전공심화", data.get("전공심화"));
                                if (data.containsKey("학부공통")) majorData.put("학부공통", data.get("학부공통"));
                                if (data.containsKey("전공필수학점")) majorData.put("전공필수학점", data.get("전공필수학점"));
                                if (data.containsKey("전공선택학점")) majorData.put("전공선택학점", data.get("전공선택학점"));
                                if (data.containsKey("전공심화학점")) majorData.put("전공심화학점", data.get("전공심화학점"));
                                if (data.containsKey("학부공통학점")) majorData.put("학부공통학점", data.get("학부공통학점"));
                            }
                        }

                        // 교양 문서 로드
                        loadGeneralDocumentAndSave(newDocId, newRequirement, majorData, generalData);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "전공 문서 로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            // 전공 문서가 선택되지 않은 경우 기본값 설정
            majorData.put("전공필수", new HashMap<String, Object>());
            majorData.put("전공선택", new HashMap<String, Object>());
            majorData.put("전공필수학점", 0);
            majorData.put("전공선택학점", 0);

            // 교양 문서 로드
            loadGeneralDocumentAndSave(newDocId, newRequirement, majorData, generalData);
        }
    }

    private void loadGeneralDocumentAndSave(String newDocId, Map<String, Object> newRequirement,
                                            Map<String, Object> majorData, Map<String, Object> generalData) {
        if (selectedGeneralDocId != null) {
            db.collection("graduation_requirements").document(selectedGeneralDocId)
                    .get()
                    .addOnSuccessListener(generalDoc -> {
                        if (generalDoc.exists()) {
                            Map<String, Object> data = generalDoc.getData();
                            if (data != null) {
                                // 교양 관련 필드만 복사
                                if (data.containsKey("교양필수")) generalData.put("교양필수", data.get("교양필수"));
                                if (data.containsKey("교양선택")) generalData.put("교양선택", data.get("교양선택"));
                                if (data.containsKey("소양")) generalData.put("소양", data.get("소양"));
                                if (data.containsKey("교양필수학점")) generalData.put("교양필수학점", data.get("교양필수학점"));
                                if (data.containsKey("교양선택학점")) generalData.put("교양선택학점", data.get("교양선택학점"));
                                if (data.containsKey("소양학점")) generalData.put("소양학점", data.get("소양학점"));
                            }
                        }

                        // 병합하여 저장
                        mergeAndSave(newDocId, newRequirement, majorData, generalData);
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "교양 문서 로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            // 교양 문서가 선택되지 않은 경우 기본값 설정
            generalData.put("교양필수", new HashMap<String, Object>());
            generalData.put("교양선택", new HashMap<String, Object>());
            generalData.put("교양필수학점", 0);
            generalData.put("교양선택학점", 0);

            // 병합하여 저장
            mergeAndSave(newDocId, newRequirement, majorData, generalData);
        }
    }

    private void mergeAndSave(String newDocId, Map<String, Object> newRequirement,
                              Map<String, Object> majorData, Map<String, Object> generalData) {
        // 전공 데이터 병합
        newRequirement.putAll(majorData);

        // 교양 데이터 병합
        newRequirement.putAll(generalData);

        // 기본 학점 필드가 없으면 추가
        if (!newRequirement.containsKey("총학점")) newRequirement.put("총학점", 0);
        if (!newRequirement.containsKey("자율선택학점")) newRequirement.put("자율선택학점", 0);
        if (!newRequirement.containsKey("잔여학점")) newRequirement.put("잔여학점", 0);

        saveToFirestore(newDocId, newRequirement);
    }

    private void saveToFirestore(String newDocId, Map<String, Object> newRequirement) {
        // Firestore에 저장
        db.collection("graduation_requirements")
                .document(newDocId)
                .set(newRequirement)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "졸업요건이 생성되었습니다", Toast.LENGTH_SHORT).show();
                    finish(); // 액티비티 종료하고 목록으로 돌아가기
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
