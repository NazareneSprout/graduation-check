package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 전공 문서 관리 Activity
 * Firestore의 graduation_requirements 컬렉션에서 전공 문서(교양_ 제외)만 조회하여 관리
 * 검색, 정렬, 복제 기능 포함
 */
public class MajorDocumentManagementActivity extends AppCompatActivity {

    private static final String TAG = "MajorDocMgmt";

    private MaterialToolbar toolbar;
    private Spinner spinnerDepartment, spinnerTrack, spinnerYear;
    private MaterialButton btnSearch;
    private ProgressBar progressBar;
    private RecyclerView rvDocuments;
    private FloatingActionButton fabAdd;
    private ChipGroup chipGroupSort;
    private Chip chipSortId, chipSortYear;
    private TextView tvResultCount;

    private FirebaseFirestore db;
    private MajorDocumentAdapter adapter;
    private List<GraduationRequirement> allDocuments = new ArrayList<>();
    private List<GraduationRequirement> filteredDocuments = new ArrayList<>();

    private String selectedDepartment = "전체";
    private String selectedTrack = "전체";
    private String selectedYear = "전체";
    private SortType currentSortType = SortType.ID;

    private enum SortType {
        ID,     // ID순 (알파벳 순서)
        YEAR    // 학번순 (ID에서 학번 추출)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_major_document_management);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        // 전공 문서 로드
        loadMajorDocuments();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        rvDocuments = findViewById(R.id.rv_documents);
        fabAdd = findViewById(R.id.fab_add);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        spinnerYear = findViewById(R.id.spinner_year);
        btnSearch = findViewById(R.id.btn_search);
        chipGroupSort = findViewById(R.id.chip_group_sort);
        chipSortId = findViewById(R.id.chip_sort_id);
        chipSortYear = findViewById(R.id.chip_sort_year);
        tvResultCount = findViewById(R.id.tv_result_count);

        // 처음에는 결과를 숨김
        rvDocuments.setVisibility(View.GONE);
        tvResultCount.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new MajorDocumentAdapter();
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvDocuments.setAdapter(adapter);

        adapter.setOnItemClickListener(requirement -> {
            // 상세보기/편집 Activity로 이동
            Intent intent = new Intent(this, MajorDocumentEditActivity.class);
            intent.putExtra("DOCUMENT_ID", requirement.getId());
            startActivity(intent);
        });

        // 길게 누르면 삭제
        adapter.setOnItemLongClickListener(requirement -> {
            showDeleteDialog(requirement);
            return true;
        });
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            // 새로 만들기 vs 복제 선택 대화상자
            showAddOptionsDialog();
        });

        // 조회 버튼 클릭 시 필터 적용
        btnSearch.setOnClickListener(v -> applyFilters());

        // 스피너 리스너 설정 (학부 변경 시 트랙 업데이트만)
        spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartment = parent.getItemAtPosition(position).toString();
                updateTrackSpinner();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTrack.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTrack = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 정렬 옵션 변경 시 자동 재정렬 (결과가 표시된 상태에서만)
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_sort_id)) {
                currentSortType = SortType.ID;
            } else if (checkedIds.contains(R.id.chip_sort_year)) {
                currentSortType = SortType.YEAR;
            }
            // 결과가 이미 표시된 경우에만 재정렬
            if (rvDocuments.getVisibility() == View.VISIBLE) {
                applyFilters();
            }
        });
    }

    /**
     * 추가 옵션 선택 대화상자 (새로 만들기 vs 복제)
     */
    private void showAddOptionsDialog() {
        String[] options = {"새로 만들기", "기존 문서 복제"};

        new AlertDialog.Builder(this)
                .setTitle("전공 문서 추가")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 새로 만들기
                        Intent intent = new Intent(this, MajorDocumentEditActivity.class);
                        intent.putExtra("IS_NEW", true);
                        startActivity(intent);
                    } else {
                        // 복제할 문서 선택
                        showDocumentSelectionDialog();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 복제할 문서 선택 대화상자
     */
    private void showDocumentSelectionDialog() {
        if (allDocuments.isEmpty()) {
            Toast.makeText(this, "복제할 문서가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 문서 ID 목록 생성
        String[] docIds = new String[allDocuments.size()];
        for (int i = 0; i < allDocuments.size(); i++) {
            docIds[i] = allDocuments.get(i).getId();
        }

        new AlertDialog.Builder(this)
                .setTitle("복제할 문서 선택")
                .setItems(docIds, (dialog, which) -> {
                    GraduationRequirement selected = allDocuments.get(which);
                    showDuplicateDialog(selected);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDuplicateDialog(GraduationRequirement requirement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("문서 복제");
        builder.setMessage("'" + requirement.getId() + "' 문서를 복제하여 새 문서를 만듭니다.\n새 문서 ID를 입력하세요.");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        com.google.android.material.textfield.TextInputLayout inputLayout =
            new com.google.android.material.textfield.TextInputLayout(this);
        inputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setHint("새 문서 ID (예: IT학부_멀티미디어_2024)");

        com.google.android.material.textfield.TextInputEditText input =
            new com.google.android.material.textfield.TextInputEditText(this);

        // 기존 ID를 기본값으로 설정 (수정 가능)
        String originalId = requirement.getId();
        String suggestedId = generateSuggestedId(originalId);
        input.setText(suggestedId);
        input.setSelection(suggestedId.length()); // 커서를 끝으로

        inputLayout.addView(input);
        layout.addView(inputLayout);
        builder.setView(layout);

        builder.setPositiveButton("복제", (dialog, which) -> {
            String newId = input.getText() != null ? input.getText().toString().trim() : "";
            if (newId.isEmpty()) {
                Toast.makeText(this, "문서 ID를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            // 전공 문서는 "교양_"로 시작하면 안됨
            if (newId.startsWith("교양_")) {
                Toast.makeText(this, "전공 문서 ID는 '교양_'로 시작할 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            duplicateDocument(requirement.getId(), newId);
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    /**
     * 새 ID 제안 생성 (학번을 +1)
     */
    private String generateSuggestedId(String originalId) {
        try {
            String[] parts = originalId.split("_");
            if (parts.length >= 3) {
                int year = Integer.parseInt(parts[2]);
                return parts[0] + "_" + parts[1] + "_" + (year + 1);
            }
        } catch (Exception e) {
            // 파싱 실패 시 원본 반환
        }
        return originalId + "_복사본";
    }

    /**
     * 문서 복제
     */
    private void duplicateDocument(String sourceId, String newId) {
        showLoading(true);

        // 1. 원본 문서 읽기
        db.collection("graduation_requirements")
                .document(sourceId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showLoading(false);
                        Toast.makeText(this, "원본 문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. 모든 데이터 복사
                    Map<String, Object> data = document.getData();
                    if (data == null) {
                        showLoading(false);
                        Toast.makeText(this, "문서 데이터가 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 3. 새 문서로 저장
                    db.collection("graduation_requirements")
                            .document(newId)
                            .set(data)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(this, "문서가 복제되었습니다: " + newId, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "문서 복제 성공: " + sourceId + " → " + newId);

                                // 목록 새로고침
                                loadMajorDocuments();

                                // 복제된 문서 편집 화면으로 이동
                                Intent intent = new Intent(this, MajorDocumentEditActivity.class);
                                intent.putExtra("DOCUMENT_ID", newId);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Log.e(TAG, "문서 복제 실패", e);
                                Toast.makeText(this, "복제 실패: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "원본 문서 로드 실패", e);
                    Toast.makeText(this, "원본 문서 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 전공 문서 로드 (교양_ 제외)
     */
    private void loadMajorDocuments() {
        showLoading(true);

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allDocuments.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // 교양 문서는 제외
                        if (document.getId().startsWith("교양_")) {
                            continue;
                        }

                        GraduationRequirement requirement = new GraduationRequirement();
                        requirement.setId(document.getId());

                        // 학점 정보 로드
                        requirement.setMajorRequired(getIntValue(document, "전공필수", 0));
                        requirement.setMajorElective(getIntValue(document, "전공선택", 0));
                        requirement.setMajorAdvanced(getIntValue(document, "전공심화", 0));
                        requirement.setDepartmentCommon(getIntValue(document, "학부공통", 0));

                        allDocuments.add(requirement);
                        Log.d(TAG, "문서 추가: " + document.getId());
                    }

                    Log.d(TAG, "전공 문서 로드: " + allDocuments.size() + "개");

                    showLoading(false);

                    // 스피너 초기화
                    initializeSpinners();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "데이터 로드 실패", e);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 스피너 초기화
     */
    private void initializeSpinners() {
        // 학부 목록 추출
        List<String> departments = new ArrayList<>();
        departments.add("전체");
        for (GraduationRequirement doc : allDocuments) {
            String[] parts = doc.getId().split("_");
            if (parts.length >= 1) {
                String dept = parts[0];
                if (!departments.contains(dept)) {
                    departments.add(dept);
                }
            }
        }
        Collections.sort(departments.subList(1, departments.size())); // "전체" 제외하고 정렬

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, departments);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        // 학번 목록 추출 (20 제거)
        List<String> years = new ArrayList<>();
        years.add("전체");
        for (GraduationRequirement doc : allDocuments) {
            String[] parts = doc.getId().split("_");
            if (parts.length >= 3) {
                String year = parts[2];
                // "20"으로 시작하면 제거 (예: 2020 -> 20)
                String displayYear = year.startsWith("20") ? year.substring(2) : year;
                if (!years.contains(displayYear)) {
                    years.add(displayYear);
                }
            }
        }
        Collections.sort(years.subList(1, years.size()), Collections.reverseOrder()); // "전체" 제외하고 내림차순 정렬

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // 트랙 스피너 초기화
        updateTrackSpinner();
    }

    /**
     * 트랙 스피너 업데이트 (선택된 학부에 따라)
     */
    private void updateTrackSpinner() {
        List<String> tracks = new ArrayList<>();
        tracks.add("전체");

        if (selectedDepartment.equals("전체")) {
            // 전체 학부의 모든 트랙
            for (GraduationRequirement doc : allDocuments) {
                String[] parts = doc.getId().split("_");
                if (parts.length >= 2) {
                    String track = parts[1];
                    if (!tracks.contains(track)) {
                        tracks.add(track);
                    }
                }
            }
        } else {
            // 선택된 학부의 트랙만
            for (GraduationRequirement doc : allDocuments) {
                String[] parts = doc.getId().split("_");
                if (parts.length >= 2 && parts[0].equals(selectedDepartment)) {
                    String track = parts[1];
                    if (!tracks.contains(track)) {
                        tracks.add(track);
                    }
                }
            }
        }

        Collections.sort(tracks.subList(1, tracks.size())); // "전체" 제외하고 정렬

        ArrayAdapter<String> trackAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tracks);
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);
        spinnerTrack.setSelection(0); // "전체"로 초기화
        selectedTrack = "전체";
    }

    /**
     * 검색 및 정렬 필터 적용
     */
    private void applyFilters() {
        filteredDocuments.clear();

        // 1. 스피너 필터 적용
        for (GraduationRequirement doc : allDocuments) {
            String[] parts = doc.getId().split("_");

            // 학부 필터
            if (!selectedDepartment.equals("전체")) {
                if (parts.length < 1 || !parts[0].equals(selectedDepartment)) {
                    continue;
                }
            }

            // 트랙 필터
            if (!selectedTrack.equals("전체")) {
                if (parts.length < 2 || !parts[1].equals(selectedTrack)) {
                    continue;
                }
            }

            // 학번 필터
            if (!selectedYear.equals("전체")) {
                if (parts.length < 3) {
                    continue;
                }
                // 스피너 값(20, 21 등)을 전체 연도(2020, 2021 등)로 변환하여 비교
                String fullYear = "20" + selectedYear;
                if (!parts[2].equals(fullYear)) {
                    continue;
                }
            }

            filteredDocuments.add(doc);
        }

        // 2. 정렬 적용
        sortDocuments();

        // 3. 어댑터 업데이트
        adapter.setRequirements(filteredDocuments);

        // 4. 결과 카운트 업데이트
        updateResultCount();
    }

    /**
     * 문서 정렬
     */
    private void sortDocuments() {
        switch (currentSortType) {
            case ID:
                // ID순 정렬 (알파벳순)
                Collections.sort(filteredDocuments, new Comparator<GraduationRequirement>() {
                    @Override
                    public int compare(GraduationRequirement o1, GraduationRequirement o2) {
                        return o1.getId().compareTo(o2.getId());
                    }
                });
                break;

            case YEAR:
                // 학번순 정렬 (ID에서 마지막 부분 추출)
                Collections.sort(filteredDocuments, new Comparator<GraduationRequirement>() {
                    @Override
                    public int compare(GraduationRequirement o1, GraduationRequirement o2) {
                        int year1 = extractYear(o1.getId());
                        int year2 = extractYear(o2.getId());

                        // 학번이 같으면 ID 순으로 정렬
                        if (year1 == year2) {
                            return o1.getId().compareTo(o2.getId());
                        }

                        // 학번 내림차순 (최신순)
                        return Integer.compare(year2, year1);
                    }
                });
                break;
        }
    }

    /**
     * ID에서 학번(연도) 추출
     * 예: "IT학부_멀티미디어_2020" → 2020
     */
    private int extractYear(String docId) {
        try {
            String[] parts = docId.split("_");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException e) {
            // 파싱 실패 시 0 반환
        }
        return 0;
    }

    /**
     * 결과 카운트 업데이트
     */
    private void updateResultCount() {
        String countText;
        if (selectedDepartment.equals("전체") && selectedTrack.equals("전체") && selectedYear.equals("전체")) {
            countText = String.format("총 %d개 문서", filteredDocuments.size());
        } else {
            countText = String.format("필터 결과 %d개 / 전체 %d개",
                filteredDocuments.size(), allDocuments.size());
        }
        tvResultCount.setText(countText);
        tvResultCount.setVisibility(View.VISIBLE);
        rvDocuments.setVisibility(View.VISIBLE);
    }

    private int getIntValue(QueryDocumentSnapshot document, String field, int defaultValue) {
        Long value = document.getLong(field);
        return value != null ? value.intValue() : defaultValue;
    }

    private void showDeleteDialog(GraduationRequirement requirement) {
        new AlertDialog.Builder(this)
                .setTitle("문서 삭제")
                .setMessage("'" + requirement.getId() + "' 문서를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteDocument(requirement))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteDocument(GraduationRequirement requirement) {
        showLoading(true);

        db.collection("graduation_requirements")
                .document(requirement.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "문서가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    loadMajorDocuments(); // 목록 새로고침
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "삭제 실패", e);
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvDocuments.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            rvDocuments.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면 복귀 시 목록 새로고침
        loadMajorDocuments();
    }
}
