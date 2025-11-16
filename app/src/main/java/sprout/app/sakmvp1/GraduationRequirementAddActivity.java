package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraduationRequirementAddActivity extends BaseActivity {

    private static final String TAG = "GradReqAdd";

    private MaterialToolbar toolbar;
    private Spinner spinnerDepartment, spinnerTrack, spinnerGraduationRequirement;
    private TextInputEditText etCohort;
    private TextView tvMajorDoc, tvGeneralDoc;
    private MaterialButton btnSelectMajorDoc, btnSelectGeneralDoc, btnSave;
    private ProgressBar progressBar;
    private android.view.View cardMajorDoc, cardGeneralDoc, cardCopyFromPrevious;
    private android.widget.RadioGroup radioGroupCreationMode;
    private android.widget.RadioButton radioCopyFromPrevious, radioCreateNew;
    private android.widget.CheckBox cbCopyCredits;

    private FirebaseFirestore db;
    private String selectedMajorDocId; // 선택된 전공 문서 ID
    private String selectedGeneralDocId; // 선택된 교양 문서 ID

    // 스피너 어댑터
    private ArrayAdapter<String> departmentAdapter;
    private ArrayAdapter<String> trackAdapter;
    private ArrayAdapter<String> graduationRequirementAdapter;

    // 졸업요건 문서 목록
    private List<String> graduationRequirementList = new ArrayList<>();

    // 학과별 트랙 매핑
    private Map<String, Set<String>> departmentTrackMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graduation_requirement_add);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupSpinners();
        setupListeners();

        // Firestore에서 데이터 로드하여 스피너 채우기
        loadDataForSpinners();
        loadGraduationRequirements();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        spinnerGraduationRequirement = findViewById(R.id.spinner_graduation_requirement);
        cardMajorDoc = findViewById(R.id.card_major_doc);
        cardGeneralDoc = findViewById(R.id.card_general_doc);
        cardCopyFromPrevious = findViewById(R.id.card_copy_from_previous);
        etCohort = findViewById(R.id.et_cohort);
        tvMajorDoc = findViewById(R.id.tv_major_doc);
        tvGeneralDoc = findViewById(R.id.tv_general_doc);
        btnSelectMajorDoc = findViewById(R.id.btn_select_major_doc);
        btnSelectGeneralDoc = findViewById(R.id.btn_select_general_doc);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);
        radioGroupCreationMode = findViewById(R.id.radio_group_creation_mode);
        radioCopyFromPrevious = findViewById(R.id.radio_copy_from_previous);
        radioCreateNew = findViewById(R.id.radio_create_new);
        cbCopyCredits = findViewById(R.id.cb_copy_credits);
    }

    private void setupSpinners() {
        // 초기 어댑터 설정 (빈 리스트)
        departmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        trackAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);

        graduationRequirementAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, graduationRequirementList);
        graduationRequirementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGraduationRequirement.setAdapter(graduationRequirementAdapter);
    }

    private void setupToolbar() {
        // Edge-to-edge 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // 학과 선택 시 트랙 업데이트
        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDepartment = parent.getItemAtPosition(position).toString();
                updateTrackSpinner(selectedDepartment);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        btnSelectMajorDoc.setOnClickListener(v -> showMajorDocumentSelector());
        btnSelectGeneralDoc.setOnClickListener(v -> showGeneralDocumentSelector());
        btnSave.setOnClickListener(v -> handleSave());

        // 라디오버튼 리스너
        radioGroupCreationMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_copy_from_previous) {
                // 이전 졸업요건에서 가져오기 모드
                cardCopyFromPrevious.setVisibility(View.VISIBLE);
                cardMajorDoc.setVisibility(View.GONE);
                cardGeneralDoc.setVisibility(View.GONE);
            } else if (checkedId == R.id.radio_create_new) {
                // 새롭게 만들기 모드 - 경고 다이얼로그 표시
                showCreateNewWarningDialog();
            }
        });
    }

    /**
     * 선택한 학과에 따라 트랙 스피너 업데이트
     */
    private void updateTrackSpinner(String selectedDepartment) {
        Log.d(TAG, "updateTrackSpinner 호출됨: " + selectedDepartment);
        Log.d(TAG, "departmentTrackMap 크기: " + departmentTrackMap.size());

        trackAdapter.clear();

        if (departmentTrackMap.containsKey(selectedDepartment)) {
            Set<String> tracks = departmentTrackMap.get(selectedDepartment);
            if (tracks != null) {
                Log.d(TAG, "트랙 개수: " + tracks.size() + ", 내용: " + tracks);
                trackAdapter.addAll(new ArrayList<>(tracks));
            }
        } else {
            Log.d(TAG, "departmentTrackMap에 학과 없음: " + selectedDepartment);
        }

        trackAdapter.notifyDataSetChanged();
    }

    /**
     * Firestore에서 데이터 로드하여 스피너 채우기
     */
    private void loadDataForSpinners() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> departments = new HashSet<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();

                        // 교양 문서와 졸업요건 문서는 제외 (전공 문서만 사용)
                        if (docId.startsWith("교양_") || docId.startsWith("졸업요건_")) {
                            continue;
                        }

                        // 문서 ID에서 정보 추출: IT학부_멀티미디어_2025 형식
                        String[] parts = docId.split("_");
                        if (parts.length >= 3) {
                            String department = parts[0];
                            String track = parts[1];

                            departments.add(department);

                            // 학과별 트랙 매핑 구축
                            if (!departmentTrackMap.containsKey(department)) {
                                departmentTrackMap.put(department, new HashSet<>());
                            }
                            departmentTrackMap.get(department).add(track);
                        }
                    }

                    Log.d(TAG, "학과 " + departments.size() + "개 로드");

                    // 스피너 데이터 설정
                    updateSpinnerData(departments);

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "데이터 로드 실패", e);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 스피너 데이터 업데이트
     */
    private void updateSpinnerData(Set<String> departments) {
        // 학과 스피너
        departmentAdapter.clear();
        departmentAdapter.addAll(new ArrayList<>(departments));
        departmentAdapter.notifyDataSetChanged();

        // 트랙 스피너는 학과 선택 시 업데이트됨
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
                        // 교양 문서와 졸업요건 문서는 제외 (전공 문서만 포함)
                        String docId = document.getId();
                        if (!docId.startsWith("교양_") && !docId.startsWith("졸업요건_")) {
                            documentNames.add(docId);
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
                        // 교양 문서만 포함 (졸업요건 문서는 제외)
                        String docId = document.getId();
                        if (docId.startsWith("교양_") && !docId.startsWith("졸업요건_")) {
                            documentNames.add(docId);
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

    /**
     * 새로 만들기 모드 경고 다이얼로그
     */
    private void showCreateNewWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ 주의")
                .setMessage("새로 만들기는 학점 요건을 직접 입력해야 합니다.\n\n" +
                        "졸업요건 생성 후, 졸업요건 편집 화면의 '학점 요건' 카드에서:\n" +
                        "• 전공필수\n" +
                        "• 전공선택\n" +
                        "• 교양필수\n" +
                        "• 교양선택\n" +
                        "• 소양\n" +
                        "• 학부공통/전공심화\n" +
                        "• 자율선택\n" +
                        "모든 학점을 하나씩 직접 입력해야 합니다.\n\n" +
                        "학점까지 복사하려면 '이전 졸업요건에서 가져오기'를 사용하세요.")
                .setPositiveButton("이해했습니다", (dialog, which) -> {
                    // 확인 버튼을 누르면 카드 표시
                    cardCopyFromPrevious.setVisibility(View.GONE);
                    cardMajorDoc.setVisibility(View.VISIBLE);
                    cardGeneralDoc.setVisibility(View.VISIBLE);
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    // 취소하면 이전 졸업요건에서 가져오기로 되돌림
                    radioCopyFromPrevious.setChecked(true);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Firestore에서 졸업요건 목록 로드
     */
    private void loadGraduationRequirements() {
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    graduationRequirementList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docId = doc.getId();
                        if (docId.startsWith("졸업요건_")) {
                            graduationRequirementList.add(docId);
                        }
                    }
                    graduationRequirementAdapter.notifyDataSetChanged();
                    Log.d(TAG, "졸업요건 목록 로드 완료: " + graduationRequirementList.size() + "개");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업요건 목록 로드 실패", e);
                    Toast.makeText(this, "졸업요건 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 저장 버튼 클릭 처리
     */
    private void handleSave() {
        int checkedId = radioGroupCreationMode.getCheckedRadioButtonId();

        if (checkedId == R.id.radio_copy_from_previous) {
            // 이전 졸업요건에서 가져오기
            if (spinnerGraduationRequirement.getSelectedItem() == null) {
                Toast.makeText(this, "복사할 졸업요건을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedGradReqDocId = spinnerGraduationRequirement.getSelectedItem().toString();
            boolean copyCredits = cbCopyCredits.isChecked();
            copyFromGraduationRequirement(selectedGradReqDocId, copyCredits);
        } else if (checkedId == R.id.radio_create_new) {
            // 새롭게 만들기
            validateAndSave();
        }
    }

    /**
     * 선택한 졸업요건 문서에서 데이터를 복사하여 새 문서 생성
     * @param sourceDocId 복사할 원본 졸업요건 문서 ID
     * @param copyCredits 학점 정보도 복사할지 여부
     */
    private void copyFromGraduationRequirement(String sourceDocId, boolean copyCredits) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("graduation_requirements")
                .document(sourceDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "선택한 졸업요건 문서를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 학점 정보 복사
                    Map<String, Object> sourceData = documentSnapshot.getData();
                    if (sourceData == null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "졸업요건 데이터를 읽을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d(TAG, "학점 복사 원본 문서: " + sourceDocId);

                    // 현재 입력된 정보로 새 문서 생성
                    String department = spinnerDepartment.getSelectedItem() != null ?
                            spinnerDepartment.getSelectedItem().toString().trim() : "";
                    String track = spinnerTrack.getSelectedItem() != null ?
                            spinnerTrack.getSelectedItem().toString().trim() : "";
                    String cohortStr = etCohort.getText() != null ? etCohort.getText().toString().trim() : "";

                    // 유효성 검사
                    if (department.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "학부/학과를 선택하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (track.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "트랙을 선택하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (cohortStr.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "학번을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int cohortInput;
                    try {
                        cohortInput = Integer.parseInt(cohortStr);
                    } catch (NumberFormatException e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "올바른 학번을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2자리 학번을 4자리로 변환
                    int cohort;
                    if (cohortInput < 100) {
                        cohort = 2000 + cohortInput;
                    } else if (cohortInput >= 2000 && cohortInput <= 2099) {
                        cohort = cohortInput;
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "올바른 학번을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newDocId = "졸업요건_" + department + "_" + track + "_" + cohort;

                    // 새 문서 데이터 생성
                    Map<String, Object> newRequirement = new HashMap<>();
                    newRequirement.put("docType", "graduation");
                    newRequirement.put("department", department);
                    newRequirement.put("track", track);
                    newRequirement.put("cohort", cohort);
                    newRequirement.put("version", "v3");
                    newRequirement.put("updatedAt", com.google.firebase.Timestamp.now());

                    // 학점 정보 복사 (선택적)
                    if (copyCredits) {
                        Log.d(TAG, "학점 복사 시작");

                        // 모든 학점 관련 필드 복사
                        String[] creditFields = {"전공필수", "전공선택", "교양필수", "교양선택", "소양",
                                "학부공통", "전공심화", "자율선택", "totalCredits"};

                        for (String field : creditFields) {
                            Object value = sourceData.get(field);
                            if (value instanceof Number) {
                                newRequirement.put(field, value);
                                Log.d(TAG, "✅ " + field + " 복사: " + value);
                            }
                        }

                        // totalCredits가 없으면 대체 필드 확인
                        if (!newRequirement.containsKey("totalCredits")) {
                            Object totalIsuObj = sourceData.get("총이수");
                            Object totalHakjeomObj = sourceData.get("총학점");
                            if (totalIsuObj instanceof Number) {
                                newRequirement.put("totalCredits", totalIsuObj);
                                Log.d(TAG, "✅ totalCredits 복사 (총이수): " + totalIsuObj);
                            } else if (totalHakjeomObj instanceof Number) {
                                newRequirement.put("totalCredits", totalHakjeomObj);
                                Log.d(TAG, "✅ totalCredits 복사 (총학점): " + totalHakjeomObj);
                            }
                        }
                    } else {
                        Log.d(TAG, "학점 복사 건너뜀");
                    }

                    // ✅ 전공/교양 문서 참조는 원본 졸업요건이 참조하는 문서를 그대로 복사
                    String sourceMajorDocRef = (String) sourceData.get("majorDocRef");
                    String sourceGeneralDocRef = (String) sourceData.get("generalDocRef");

                    if (sourceMajorDocRef != null && !sourceMajorDocRef.isEmpty()) {
                        newRequirement.put("majorDocRef", sourceMajorDocRef);
                        Log.d(TAG, "✅ 전공 문서 참조 복사: " + sourceMajorDocRef);
                    } else {
                        // 원본에 참조가 없으면 기본값 생성
                        String defaultMajorDocId = department + "_" + track + "_" + cohort;
                        newRequirement.put("majorDocRef", defaultMajorDocId);
                        Log.d(TAG, "✅ 전공 문서 참조 (기본값): " + defaultMajorDocId);
                    }

                    if (sourceGeneralDocRef != null && !sourceGeneralDocRef.isEmpty()) {
                        newRequirement.put("generalDocRef", sourceGeneralDocRef);
                        Log.d(TAG, "✅ 교양 문서 참조 복사: " + sourceGeneralDocRef);
                    } else {
                        // 원본에 참조가 없으면 기본값 생성
                        String defaultGeneralDocId = "교양_" + department + "_" + cohort;
                        newRequirement.put("generalDocRef", defaultGeneralDocId);
                        Log.d(TAG, "✅ 교양 문서 참조 (기본값): " + defaultGeneralDocId);
                    }

                    // ✅ 대체과목 규칙도 원본에서 복사
                    Object replacementRulesObj = sourceData.get("replacementRules");
                    if (replacementRulesObj instanceof List) {
                        newRequirement.put("replacementRules", replacementRulesObj);
                        List<?> rules = (List<?>) replacementRulesObj;
                        Log.d(TAG, "✅ 대체과목 규칙 복사: " + rules.size() + "개");
                    } else {
                        newRequirement.put("replacementRules", new ArrayList<>());
                        Log.d(TAG, "대체과목 규칙 없음 (빈 리스트 생성)");
                    }

                    // Firestore에 저장
                    db.collection("graduation_requirements")
                            .document(newDocId)
                            .set(newRequirement)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "졸업요건이 생성되었습니다 (학점 복사 완료)", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "✅ 졸업요건 생성 완료: " + newDocId);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "❌ 저장 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "원본 문서 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void validateAndSave() {
        // 스피너와 입력 필드에서 값 가져오기
        String department = spinnerDepartment.getSelectedItem() != null ?
                spinnerDepartment.getSelectedItem().toString().trim() : "";
        String track = spinnerTrack.getSelectedItem() != null ?
                spinnerTrack.getSelectedItem().toString().trim() : "";
        String cohortStr = etCohort.getText() != null ? etCohort.getText().toString().trim() : "";

        // 유효성 검사
        if (department.isEmpty()) {
            Toast.makeText(this, "학부/학과를 선택하세요", Toast.LENGTH_SHORT).show();
            spinnerDepartment.requestFocus();
            return;
        }

        if (track.isEmpty()) {
            Toast.makeText(this, "트랙을 선택하세요", Toast.LENGTH_SHORT).show();
            spinnerTrack.requestFocus();
            return;
        }

        if (cohortStr.isEmpty()) {
            etCohort.setError("학번을 입력하세요 (예: 25, 26)");
            etCohort.requestFocus();
            return;
        }

        int cohortInput;
        try {
            cohortInput = Integer.parseInt(cohortStr);
        } catch (NumberFormatException e) {
            etCohort.setError("올바른 학번을 입력하세요 (숫자만)");
            etCohort.requestFocus();
            return;
        }

        // 2자리 학번을 4자리로 변환 (25 -> 2025, 26 -> 2026)
        int cohort;
        if (cohortInput < 100) {
            // 2자리 입력: 20XX 형식으로 변환
            cohort = 2000 + cohortInput;
            Log.d(TAG, "학번 변환: " + cohortInput + " -> " + cohort);
        } else if (cohortInput >= 2000 && cohortInput <= 2099) {
            // 4자리 입력: 그대로 사용
            cohort = cohortInput;
        } else {
            etCohort.setError("올바른 학번을 입력하세요 (예: 25, 26 또는 2025, 2026)");
            etCohort.requestFocus();
            return;
        }

        // 문서 ID 생성: 졸업요건_IT학부_멀티미디어_2026
        String newDocId = "졸업요건_" + department + "_" + track + "_" + cohort;

        Log.d(TAG, "새 졸업요건 문서 생성: " + newDocId);

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

        // ✅ v3 졸업요건 문서 기본 정보 설정
        newRequirement.put("docType", "graduation");
        newRequirement.put("department", department);
        newRequirement.put("track", track);
        newRequirement.put("cohort", cohort);
        newRequirement.put("version", "v3");
        newRequirement.put("updatedAt", com.google.firebase.Timestamp.now());

        // ✅ 전공 문서 참조 설정
        if (selectedMajorDocId != null && !selectedMajorDocId.isEmpty()) {
            newRequirement.put("majorDocRef", selectedMajorDocId);
            Log.d(TAG, "전공 문서 참조: " + selectedMajorDocId);
        } else {
            // 참조가 없으면 기본 문서 ID 생성
            String defaultMajorDocId = department + "_" + track + "_" + cohort;
            newRequirement.put("majorDocRef", defaultMajorDocId);
            Log.d(TAG, "전공 문서 참조 (기본값): " + defaultMajorDocId);
        }

        // ✅ 교양 문서 참조 설정
        if (selectedGeneralDocId != null && !selectedGeneralDocId.isEmpty()) {
            newRequirement.put("generalDocRef", selectedGeneralDocId);
            Log.d(TAG, "교양 문서 참조: " + selectedGeneralDocId);
        } else {
            // 참조가 없으면 기본 문서 ID 생성
            String defaultGeneralDocId = "교양_공통_" + cohort;
            newRequirement.put("generalDocRef", defaultGeneralDocId);
            Log.d(TAG, "교양 문서 참조 (기본값): " + defaultGeneralDocId);
        }

        // 선택된 문서가 있는 경우 해당 문서들에서 학점 정보만 복사
        if (selectedMajorDocId != null || selectedGeneralDocId != null) {
            loadAndCopyCreditRequirements(newDocId, newRequirement, department, track, cohort);
        } else {
            // 새로 생성하는 경우 기본 학점 설정
            boolean usesMajorAdvanced = DepartmentConfig.usesMajorAdvancedForAllYears(department);

            newRequirement.put("전공필수", 0);
            newRequirement.put("전공선택", 0);
            newRequirement.put("교양필수", 0);
            newRequirement.put("교양선택", 0);
            newRequirement.put("소양", 0);

            if (usesMajorAdvanced) {
                newRequirement.put("전공심화", 0);
            } else {
                newRequirement.put("학부공통", 0);
            }

            newRequirement.put("자율선택", 0);
            newRequirement.put("totalCredits", 130);

            // ✅ 빈 대체과목 규칙 초기화
            newRequirement.put("replacementRules", new ArrayList<>());

            Log.d(TAG, "v3 졸업요건 문서 기본 구조 생성 완료");
            Log.d(TAG, "  - docType: graduation");
            Log.d(TAG, "  - majorDocRef: " + newRequirement.get("majorDocRef"));
            Log.d(TAG, "  - generalDocRef: " + newRequirement.get("generalDocRef"));
            Log.d(TAG, "  - 학점 정보 포함");
            Log.d(TAG, "  - replacementRules: []");

            saveToFirestore(newDocId, newRequirement);
        }
    }


    // 졸업요건 문서: 참조 문서들에서 학점 정보만 복사
    // v3 구조: 전공/교양 문서에는 학점 정보가 없음 (하위 호환성 유지)
    private void loadAndCopyCreditRequirements(String newDocId, Map<String, Object> newRequirement, String department, String track, int cohort) {
        Log.d(TAG, "학점 복사 시도 - v3 구조에서는 전공/교양 문서에 학점 없음");
        Map<String, Object> creditRequirements = new HashMap<>();
        final int[] documentsToLoad = {0};
        final int[] documentsLoaded = {0};

        // 로드할 문서 개수 계산
        if (selectedMajorDocId != null) documentsToLoad[0]++;
        if (selectedGeneralDocId != null) documentsToLoad[0]++;

        if (documentsToLoad[0] == 0) {
            Log.d(TAG, "선택된 참조 문서 없음 - 기본값 사용");
            saveToFirestore(newDocId, newRequirement);
            return;
        }

        // 전공 문서에서 학점 복사
        if (selectedMajorDocId != null) {
            db.collection("graduation_requirements").document(selectedMajorDocId)
                    .get()
                    .addOnSuccessListener(majorDoc -> {
                        if (majorDoc.exists()) {
                            Map<String, Object> data = majorDoc.getData();
                            if (data != null) {
                                // 학점요건 데이터 복사
                                String[] creditFields = {"전공필수", "전공선택", "교양필수", "교양선택", "소양", "학부공통", "일반선택", "전공심화", "잔여학점", "자율선택"};
                                for (String field : creditFields) {
                                    Object value = data.get(field);
                                    if (value instanceof Number) {
                                        creditRequirements.put(field, value);
                                        Log.d(TAG, "전공 문서 [" + selectedMajorDocId + "] " + field + " = " + value);
                                    }
                                }

                                // totalCredits 복사
                                Object totalCreditsObj = data.get("totalCredits");
                                Object totalIsuObj = data.get("총이수");
                                Object totalHakjeomObj = data.get("총학점");

                                if (totalCreditsObj instanceof Number) {
                                    creditRequirements.put("totalCredits", totalCreditsObj);
                                } else if (totalIsuObj instanceof Number) {
                                    creditRequirements.put("totalCredits", totalIsuObj);
                                } else if (totalHakjeomObj instanceof Number) {
                                    creditRequirements.put("totalCredits", totalHakjeomObj);
                                }

                                Log.d(TAG, "전공 문서에서 학점요건 복사 완료");
                            }
                        }

                        documentsLoaded[0]++;
                        if (documentsLoaded[0] == documentsToLoad[0]) {
                            processCreditRequirementsAndSave(newDocId, newRequirement, creditRequirements);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "전공 문서 로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }

        // 교양 문서에서 학점 복사 (전공 문서에 없는 필드만)
        if (selectedGeneralDocId != null) {
            db.collection("graduation_requirements").document(selectedGeneralDocId)
                    .get()
                    .addOnSuccessListener(generalDoc -> {
                        if (generalDoc.exists()) {
                            Map<String, Object> data = generalDoc.getData();
                            if (data != null) {
                                String[] creditFields = {"전공필수", "전공선택", "교양필수", "교양선택", "소양", "학부공통", "일반선택", "전공심화", "잔여학점", "자율선택"};
                                for (String field : creditFields) {
                                    Object value = data.get(field);
                                    if (value instanceof Number && !creditRequirements.containsKey(field)) {
                                        creditRequirements.put(field, value);
                                        Log.d(TAG, "교양 문서 [" + selectedGeneralDocId + "] " + field + " = " + value);
                                    }
                                }

                                // totalCredits 처리 (전공 문서가 우선)
                                if (!creditRequirements.containsKey("totalCredits")) {
                                    Object totalCreditsObj = data.get("totalCredits");
                                    Object totalIsuObj = data.get("총이수");
                                    Object totalHakjeomObj = data.get("총학점");

                                    if (totalCreditsObj instanceof Number) {
                                        creditRequirements.put("totalCredits", totalCreditsObj);
                                    } else if (totalIsuObj instanceof Number) {
                                        creditRequirements.put("totalCredits", totalIsuObj);
                                    } else if (totalHakjeomObj instanceof Number) {
                                        creditRequirements.put("totalCredits", totalHakjeomObj);
                                    }
                                }

                                Log.d(TAG, "교양 문서에서 학점요건 복사 완료");
                            }
                        }

                        documentsLoaded[0]++;
                        if (documentsLoaded[0] == documentsToLoad[0]) {
                            processCreditRequirementsAndSave(newDocId, newRequirement, creditRequirements);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "교양 문서 로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void processCreditRequirementsAndSave(String newDocId, Map<String, Object> newRequirement, Map<String, Object> creditRequirements) {
        // 일반선택, 잔여학점 필드는 제거하고 자율선택으로 변환
        int freeElective = 0;
        if (creditRequirements.containsKey("자율선택")) {
            freeElective += ((Number) creditRequirements.get("자율선택")).intValue();
        }
        if (creditRequirements.containsKey("일반선택")) {
            freeElective += ((Number) creditRequirements.get("일반선택")).intValue();
            creditRequirements.remove("일반선택");
            Log.d(TAG, "일반선택 필드 제거 및 자율선택으로 병합");
        }
        if (creditRequirements.containsKey("잔여학점")) {
            freeElective += ((Number) creditRequirements.get("잔여학점")).intValue();
            creditRequirements.remove("잔여학점");
            Log.d(TAG, "잔여학점 필드 제거 및 자율선택으로 병합");
        }
        if (freeElective > 0) {
            creditRequirements.put("자율선택", freeElective);
        }

        // 총이수, 총학점 필드 제거 (totalCredits만 사용)
        creditRequirements.remove("총이수");
        creditRequirements.remove("총학점");

        // 학점요건을 newRequirement에 병합
        newRequirement.putAll(creditRequirements);

        // 기본 학점 필드가 없으면 추가
        if (!newRequirement.containsKey("totalCredits")) {
            newRequirement.put("totalCredits", 130);
        }

        Log.d(TAG, "최종 졸업요건 문서 생성: " + newRequirement.keySet());

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

    /**
     * 이전 연도 졸업요건에서 복사
     */
    private void copyFromPreviousYear(String newDocId, Map<String, Object> newRequirement,
                                      String department, String track, int cohort) {
        Log.d(TAG, "이전 연도에서 복사 시작: " + department + "/" + track + "/" + cohort);

        // 이전 연도들을 역순으로 검색 (cohort-1부터 시작)
        findPreviousYearRequirement(newDocId, newRequirement, department, track, cohort, cohort - 1);
    }

    /**
     * 재귀적으로 이전 연도 졸업요건 찾기
     */
    private void findPreviousYearRequirement(String newDocId, Map<String, Object> newRequirement,
                                             String department, String track, int targetCohort, int searchCohort) {
        // 너무 오래된 연도는 검색하지 않음 (최대 5년 전까지)
        if (targetCohort - searchCohort > 5) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이전 연도 졸업요건을 찾을 수 없습니다.\n수동으로 생성해주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        String previousDocId = "졸업요건_" + department + "_" + track + "_" + searchCohort;
        Log.d(TAG, "이전 연도 문서 검색 중: " + previousDocId);

        db.collection("graduation_requirements").document(previousDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 이전 연도 문서를 찾은 경우
                        Log.d(TAG, "이전 연도 문서 발견: " + previousDocId);
                        copyDataFromDocument(newDocId, newRequirement, documentSnapshot, targetCohort);
                    } else {
                        // 찾지 못한 경우 더 이전 연도 검색
                        Log.d(TAG, "문서 없음, 더 이전 연도 검색: " + (searchCohort - 1));
                        findPreviousYearRequirement(newDocId, newRequirement, department, track, targetCohort, searchCohort - 1);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "검색 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 이전 연도 문서에서 데이터 복사 - v3 구조
     */
    private void copyDataFromDocument(String newDocId, Map<String, Object> newRequirement,
                                      com.google.firebase.firestore.DocumentSnapshot sourceDoc, int targetCohort) {
        // 복사한 원본 문서 ID 표시
        String sourceDocId = sourceDoc.getId();
        Log.d(TAG, "이전 연도 졸업요건에서 데이터 복사 (v3): " + sourceDocId);

        // 학점 요건 복사
        String[] creditFields = {"전공필수", "전공선택", "교양필수", "교양선택", "소양",
                                 "학부공통", "전공심화", "자율선택", "잔여학점", "totalCredits"};

        for (String field : creditFields) {
            Object value = sourceDoc.get(field);
            if (value instanceof Number) {
                newRequirement.put(field, value);
                Log.d(TAG, "복사: " + field + " = " + value);
            }
        }

        // ✅ 참조 문서 ID도 복사 (학번만 변경)
        String sourceMajorDocRef = sourceDoc.getString("majorDocRef");
        String sourceGeneralDocRef = sourceDoc.getString("generalDocRef");

        if (sourceMajorDocRef != null && !sourceMajorDocRef.isEmpty()) {
            // 참조 문서의 학번을 현재 학번으로 변경
            // 예: IT학부_멀티미디어_2024 → IT학부_멀티미디어_2025
            String[] parts = sourceMajorDocRef.split("_");
            if (parts.length >= 3) {
                String newMajorDocRef = parts[0] + "_" + parts[1] + "_" + targetCohort;
                newRequirement.put("majorDocRef", newMajorDocRef);
                Log.d(TAG, "전공 문서 참조 복사 및 학번 변경: " + sourceMajorDocRef + " → " + newMajorDocRef);
            } else {
                newRequirement.put("majorDocRef", sourceMajorDocRef);
                Log.d(TAG, "전공 문서 참조 복사 (변경 없음): " + sourceMajorDocRef);
            }
        } else {
            // 참조가 없으면 기본 생성
            String department = newRequirement.get("department").toString();
            String track = newRequirement.get("track").toString();
            String defaultMajorDocRef = department + "_" + track + "_" + targetCohort;
            newRequirement.put("majorDocRef", defaultMajorDocRef);
            Log.d(TAG, "전공 문서 참조 기본 생성: " + defaultMajorDocRef);
        }

        if (sourceGeneralDocRef != null && !sourceGeneralDocRef.isEmpty()) {
            // 참조 문서의 학번을 현재 학번으로 변경
            String[] parts = sourceGeneralDocRef.split("_");
            if (parts.length >= 2) {
                // "교양_공통_2024" → "교양_공통_2025" 또는 "교양_2024" → "교양_2025"
                if (parts.length >= 3) {
                    String newGeneralDocRef = parts[0] + "_" + parts[1] + "_" + targetCohort;
                    newRequirement.put("generalDocRef", newGeneralDocRef);
                    Log.d(TAG, "교양 문서 참조 복사 및 학번 변경: " + sourceGeneralDocRef + " → " + newGeneralDocRef);
                } else {
                    String newGeneralDocRef = parts[0] + "_" + targetCohort;
                    newRequirement.put("generalDocRef", newGeneralDocRef);
                    Log.d(TAG, "교양 문서 참조 복사 및 학번 변경: " + sourceGeneralDocRef + " → " + newGeneralDocRef);
                }
            } else {
                newRequirement.put("generalDocRef", sourceGeneralDocRef);
                Log.d(TAG, "교양 문서 참조 복사 (변경 없음): " + sourceGeneralDocRef);
            }
        } else {
            // 참조가 없으면 기본 생성
            String defaultGeneralDocRef = "교양_공통_" + targetCohort;
            newRequirement.put("generalDocRef", defaultGeneralDocRef);
            Log.d(TAG, "교양 문서 참조 기본 생성: " + defaultGeneralDocRef);
        }

        // ✅ 대체과목 규칙 복사
        Object replacementRulesObj = sourceDoc.get("replacementRules");
        if (replacementRulesObj instanceof List) {
            newRequirement.put("replacementRules", replacementRulesObj);
            List<?> rules = (List<?>) replacementRulesObj;
            Log.d(TAG, "대체과목 규칙 복사: " + rules.size() + "개");
        } else {
            newRequirement.put("replacementRules", new ArrayList<>());
            Log.d(TAG, "대체과목 규칙 없음 (빈 리스트 생성)");
        }

        Log.d(TAG, "v3 졸업요건 문서 복사 완료");
        Log.d(TAG, "  - majorDocRef: " + newRequirement.get("majorDocRef"));
        Log.d(TAG, "  - generalDocRef: " + newRequirement.get("generalDocRef"));

        // Firestore에 저장
        saveToFirestore(newDocId, newRequirement);
    }
}

