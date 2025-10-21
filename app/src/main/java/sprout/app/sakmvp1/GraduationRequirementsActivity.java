package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sprout.app.sakmvp1.utils.GraduationRequirementUtils;

/**
 * 졸업요건 관리 Activity (관리자용)
 * Firestore의 graduation_requirements 컬렉션에서 학번/학과/트랙별 졸업요건 조회
 */
public class GraduationRequirementsActivity extends AppCompatActivity {

    private static final String TAG = "GradReqActivity";

    private MaterialToolbar toolbar;
    private Spinner spinnerStudentId, spinnerDepartment, spinnerTrack;
    private MaterialButton btnSearch;
    private MaterialButton btnDeleteSelected;
    private ProgressBar progressBar;
    private RecyclerView rvRequirements;
    private FloatingActionButton fabAdd;

    private FirebaseFirestore db;
    private GraduationRequirementAdapter adapter;

    // 전체 졸업요건 데이터 (필터링에 사용)
    private List<GraduationRequirement> allRequirements = new ArrayList<>();

    // 스피너 어댑터
    private ArrayAdapter<String> studentIdAdapter;
    private ArrayAdapter<String> departmentAdapter;
    private ArrayAdapter<String> trackAdapter;

    // 학과별 트랙 매핑
    private Map<String, Set<String>> departmentTrackMap = new HashMap<>();

    // 마지막 필터 조건 저장 (편집 후 복원용)
    private String lastSelectedYear = "전체";
    private String lastSelectedDepartment = "전체";
    private String lastSelectedTrack = "전체";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graduation_requirements);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSpinners();
        setupListeners();

        // 초기 전체 데이터 로드
        loadAllRequirements();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 편집 후 돌아왔을 때 데이터 새로고침하고 마지막 필터 재적용
        loadAllRequirements();
    }

    /**
     * 데이터 로드 후 마지막 필터 조건 재적용
     */
    private void reapplyLastFilter() {
        // 저장된 마지막 조건이 "전체"가 아니면 스피너 위치 복원 후 필터 재적용
        if (!"전체".equals(lastSelectedYear) || !"전체".equals(lastSelectedDepartment) || !"전체".equals(lastSelectedTrack)) {
            // 스피너 위치 복원 후 필터링 (UI 스레드에서 실행)
            spinnerStudentId.post(() -> {
                restoreSpinnerSelections();
                // 필터링은 저장된 값으로 직접 수행
                applyFilterWithSavedConditions();
            });
        }
    }

    /**
     * 저장된 조건으로 직접 필터링 (스피너 getSelectedItem 사용 안 함)
     */
    private void applyFilterWithSavedConditions() {
        String selectedYear = lastSelectedYear;
        String selectedDepartment = lastSelectedDepartment;
        String selectedTrack = lastSelectedTrack;

        // 2자리 학번을 4자리로 변환 ("20" -> "2020")
        if (!"전체".equals(selectedYear) && selectedYear.length() == 2) {
            selectedYear = "20" + selectedYear;
        }

        Log.d(TAG, "필터링 (저장된 조건): " + selectedYear + "/" + selectedDepartment + "/" + selectedTrack);

        List<GraduationRequirement> filtered = new ArrayList<>();

        for (GraduationRequirement req : allRequirements) {
            boolean matches = true;

            if (!"전체".equals(selectedYear) && !selectedYear.equals(req.getYear())) {
                matches = false;
            }
            if (!"전체".equals(selectedDepartment) && !selectedDepartment.equals(req.getDepartment())) {
                matches = false;
            }
            if (!"전체".equals(selectedTrack) && !selectedTrack.equals(req.getTrack())) {
                matches = false;
            }

            if (matches) {
                filtered.add(req);
            }
        }

        Log.d(TAG, "필터링 결과: " + filtered.size() + "개");
        displayResults(filtered);
    }

    /**
     * 저장된 조건으로 스피너 위치 복원
     */
    private void restoreSpinnerSelections() {
        Log.d(TAG, "restoreSpinnerSelections 호출 - 저장된 조건: " + lastSelectedYear + "/" + lastSelectedDepartment + "/" + lastSelectedTrack);

        // 학번 스피너 복원
        if (!"전체".equals(lastSelectedYear)) {
            int yearPosition = findSpinnerPosition(studentIdAdapter, lastSelectedYear);
            Log.d(TAG, "학번 위치 찾기: " + lastSelectedYear + " → " + yearPosition);
            if (yearPosition >= 0) {
                spinnerStudentId.setSelection(yearPosition);
            }
        }

        // 학과 스피너 복원
        if (!"전체".equals(lastSelectedDepartment)) {
            int deptPosition = findSpinnerPosition(departmentAdapter, lastSelectedDepartment);
            Log.d(TAG, "학과 위치 찾기: " + lastSelectedDepartment + " → " + deptPosition);
            if (deptPosition >= 0) {
                spinnerDepartment.setSelection(deptPosition);
                // 학과 선택 시 트랙 업데이트 (리스너가 자동으로 호출되지 않을 수 있으므로 명시적 호출)
                updateTrackSpinner(lastSelectedDepartment);

                spinnerDepartment.post(() -> {
                    // 트랙 스피너 복원
                    if (!"전체".equals(lastSelectedTrack)) {
                        int trackPosition = findSpinnerPosition(trackAdapter, lastSelectedTrack);
                        Log.d(TAG, "트랙 위치 찾기 (학과 선택 후): " + lastSelectedTrack + " → " + trackPosition + ", adapter 크기: " + trackAdapter.getCount());
                        if (trackPosition >= 0) {
                            spinnerTrack.setSelection(trackPosition);
                        }
                    }
                });
            }
        } else {
            // 학과가 "전체"인 경우 트랙 스피너 업데이트 후 복원
            updateTrackSpinner("전체");

            if (!"전체".equals(lastSelectedTrack)) {
                spinnerTrack.post(() -> {
                    int trackPosition = findSpinnerPosition(trackAdapter, lastSelectedTrack);
                    Log.d(TAG, "트랙 위치 찾기 (학과=전체): " + lastSelectedTrack + " → " + trackPosition);
                    if (trackPosition >= 0) {
                        spinnerTrack.setSelection(trackPosition);
                    }
                });
            }
        }
    }

    /**
     * ArrayAdapter에서 특정 아이템의 위치 찾기
     */
    private int findSpinnerPosition(ArrayAdapter<String> adapter, String value) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerStudentId = findViewById(R.id.spinner_student_id);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        btnSearch = findViewById(R.id.btn_search);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        progressBar = findViewById(R.id.progress_bar);
        rvRequirements = findViewById(R.id.rv_requirements);
        fabAdd = findViewById(R.id.fab_add);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new GraduationRequirementAdapter();
        rvRequirements.setLayoutManager(new LinearLayoutManager(this));
        rvRequirements.setAdapter(adapter);

        adapter.setOnItemClickListener(requirement -> {
            // 편집 Activity로 이동
            Intent intent = new Intent(this, GraduationRequirementEditActivity.class);
            intent.putExtra(GraduationRequirementEditActivity.EXTRA_DOCUMENT_ID, requirement.getId());
            startActivity(intent);
        });

        // 선택 변경 리스너
        adapter.setOnSelectionChangedListener(selectedCount -> {
            // 선택된 항목이 있으면 삭제 버튼 활성화
            btnDeleteSelected.setEnabled(selectedCount > 0);
            if (selectedCount > 0) {
                btnDeleteSelected.setText("선택 항목 삭제 (" + selectedCount + ")");
            } else {
                btnDeleteSelected.setText("선택 항목 삭제");
            }
        });
    }

    private void setupSpinners() {
        // 초기 어댑터 설정 (빈 리스트)
        studentIdAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        studentIdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStudentId.setAdapter(studentIdAdapter);

        departmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        trackAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);
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

        // 검색 버튼
        btnSearch.setOnClickListener(v -> filterRequirements());

        // 추가 버튼 - 다이얼로그로 옵션 선택
        fabAdd.setOnClickListener(v -> showDocumentTypeDialog());

        // 삭제 버튼
        btnDeleteSelected.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    /**
     * 문서 타입 선택 다이얼로그 표시
     */
    private void showDocumentTypeDialog() {
        String[] options = {
            "졸업요건 문서 생성",
            "전공 문서 생성",
            "교양 문서 생성"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("생성할 문서 선택")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 졸업요건 문서 생성
                            openGraduationRequirementAdd();
                            break;
                        case 1:
                            // 전공 문서 생성
                            openMajorDocumentAdd();
                            break;
                        case 2:
                            // 교양 문서 생성
                            openGeneralDocumentAdd();
                            break;
                    }
                })
                .show();
    }

    /**
     * 졸업요건 문서 추가 Activity 열기
     */
    private void openGraduationRequirementAdd() {
        Intent intent = new Intent(this, GraduationRequirementAddActivity.class);
        startActivity(intent);
    }

    /**
     * 전공 문서 추가 Activity 열기
     */
    private void openMajorDocumentAdd() {
        Intent intent = new Intent(this, MajorDocumentManageActivity.class);
        startActivity(intent);
    }

    /**
     * 교양 문서 추가 Activity 열기
     */
    private void openGeneralDocumentAdd() {
        Intent intent = new Intent(this, GeneralDocumentManageActivity.class);
        startActivity(intent);
    }

    /**
     * 전체 졸업요건 로드
     */
    private void loadAllRequirements() {
        showLoading(true);

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRequirements.clear();
                    departmentTrackMap.clear(); // 학과별 트랙 매핑도 초기화
                    Set<String> years = new HashSet<>();
                    Set<String> departments = new HashSet<>();
                    Set<String> tracks = new HashSet<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();

                        // 졸업요건 문서만 표시 (문서 ID가 "졸업요건_"로 시작하는 것만)
                        if (!docId.startsWith("졸업요건_")) {
                            Log.d(TAG, "졸업요건 문서 아님 제외: " + docId);
                            continue;
                        }

                        // 참조 문서 ID 먼저 확인
                        String majorDocRef = document.getString("majorDocRef");
                        String generalDocRef = document.getString("generalDocRef");

                        // Firestore 문서에서 직접 필드 읽어서 객체 생성
                        GraduationRequirement requirement = new GraduationRequirement();
                        requirement.setId(document.getId()); // 문서 ID 설정하면 자동으로 year, department, track 파싱됨

                        // 학점 정보 가져오기
                        // 통합 구조(v2: creditRequirements 객체) 또는 이전 구조(v1: 루트 필드)에서 읽기
                        int totalCredits = GraduationRequirementUtils.getCreditFromRequirements(document, "totalCredits", 130);
                        int majorRequired = GraduationRequirementUtils.getCreditFromRequirements(document, "전공필수", 0);
                        int majorElective = GraduationRequirementUtils.getCreditFromRequirements(document, "전공선택", 0);
                        int majorAdvanced = GraduationRequirementUtils.getCreditFromRequirements(document, "전공심화", 0);
                        int generalRequired = GraduationRequirementUtils.getCreditFromRequirements(document, "교양필수", 0);
                        int generalElective = GraduationRequirementUtils.getCreditFromRequirements(document, "교양선택", 0);
                        int liberalArts = GraduationRequirementUtils.getCreditFromRequirements(document, "소양", 0);
                        int freeElective = GraduationRequirementUtils.getCreditFromRequirements(document, "자율선택", 0);
                        int departmentCommon = GraduationRequirementUtils.getCreditFromRequirements(document, "학부공통", 0);
                        int remainingCredits = GraduationRequirementUtils.getCreditFromRequirements(document, "잔여학점", 0);

                        requirement.setTotalCredits(totalCredits);
                        requirement.setMajorRequired(majorRequired);
                        requirement.setMajorElective(majorElective);
                        requirement.setMajorAdvanced(majorAdvanced);
                        requirement.setGeneralRequired(generalRequired);
                        requirement.setGeneralElective(generalElective);
                        requirement.setLiberalArts(liberalArts);
                        requirement.setFreeElective(freeElective);
                        requirement.setDepartmentCommon(departmentCommon);
                        requirement.setRemainingCredits(remainingCredits);

                        // 참조 문서 ID 설정
                        requirement.setMajorDocRef(majorDocRef);
                        requirement.setGeneralDocRef(generalDocRef);

                        // 디버깅: 특정 문서는 상세 로그 출력
                        if (document.getId().startsWith("졸업요건_")) {
                            Log.d(TAG, "========== " + document.getId() + " 상세 정보 ==========");
                            Log.d(TAG, "문서 ID: " + document.getId());
                            Log.d(TAG, "majorDocRef 필드: " + (majorDocRef != null ? "'" + majorDocRef + "'" : "null"));
                            Log.d(TAG, "generalDocRef 필드: " + (generalDocRef != null ? "'" + generalDocRef + "'" : "null"));
                            Log.d(TAG, "totalCredits 필드: " + document.get("totalCredits"));
                            Log.d(TAG, "creditRequirements 필드: " + document.get("creditRequirements"));
                            Log.d(TAG, "총학점 (로드된 값): " + totalCredits);
                            Log.d(TAG, "문서의 모든 필드: " + document.getData());
                            Log.d(TAG, "=======================================================");
                        }

                        Log.d(TAG, "문서 " + document.getId() + " 로드: 총=" + totalCredits
                            + ", 전필=" + majorRequired + ", 전선=" + majorElective
                            + ", 교필=" + generalRequired + ", 교선=" + generalElective
                            + ", 소양=" + liberalArts + ", 자율=" + freeElective
                            + ", 전심=" + majorAdvanced + ", 학공=" + departmentCommon
                            + ", 잔여=" + remainingCredits
                            + ", 전공문서=" + (majorDocRef != null ? majorDocRef : "없음")
                            + ", 교양문서=" + (generalDocRef != null ? generalDocRef : "없음"));

                        allRequirements.add(requirement);

                        // 스피너 옵션 수집
                        if (requirement.getYear() != null) {
                            years.add(requirement.getYear());
                        }
                        if (requirement.getDepartment() != null) {
                            departments.add(requirement.getDepartment());

                            // 학과별 트랙 매핑 구축
                            if (requirement.getTrack() != null) {
                                if (!departmentTrackMap.containsKey(requirement.getDepartment())) {
                                    departmentTrackMap.put(requirement.getDepartment(), new HashSet<>());
                                }
                                departmentTrackMap.get(requirement.getDepartment()).add(requirement.getTrack());
                            }
                        }
                        if (requirement.getTrack() != null) {
                            tracks.add(requirement.getTrack());
                        }
                    }

                    Log.d(TAG, "전체 졸업요건 로드: " + allRequirements.size() + "개");

                    // 스피너 데이터 설정
                    updateSpinnerData(years, departments, tracks);

                    // 로딩 종료
                    showLoading(false);

                    // 마지막 필터 조건 재적용 (onCreate가 아닐 때만 - onResume에서 호출된 경우)
                    reapplyLastFilter();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "데이터 로드 실패", e);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 스피너 데이터 업데이트
     */
    private void updateSpinnerData(Set<String> years, Set<String> departments, Set<String> tracks) {
        // 학번 스피너 (4자리 -> 2자리 변환)
        List<String> yearList = new ArrayList<>();
        yearList.add("전체");
        for (String year : years) {
            if (year != null && year.length() == 4) {
                yearList.add(year.substring(2)); // "2020" -> "20"
            } else {
                yearList.add(year);
            }
        }
        studentIdAdapter.clear();
        studentIdAdapter.addAll(yearList);
        studentIdAdapter.notifyDataSetChanged();

        // 학과 스피너
        List<String> departmentList = new ArrayList<>();
        departmentList.add("전체");
        departmentList.addAll(new ArrayList<>(departments));
        departmentAdapter.clear();
        departmentAdapter.addAll(departmentList);
        departmentAdapter.notifyDataSetChanged();

        // 트랙 스피너는 초기에 "전체"만 표시
        trackAdapter.clear();
        trackAdapter.add("전체");
        trackAdapter.notifyDataSetChanged();
    }

    /**
     * 선택한 학과에 따라 트랙 스피너 업데이트
     */
    private void updateTrackSpinner(String selectedDepartment) {
        Log.d(TAG, "updateTrackSpinner 호출됨: " + selectedDepartment);
        Log.d(TAG, "departmentTrackMap 크기: " + departmentTrackMap.size());
        Log.d(TAG, "departmentTrackMap 내용: " + departmentTrackMap);

        trackAdapter.clear();
        trackAdapter.add("전체");

        if (!"전체".equals(selectedDepartment) && departmentTrackMap.containsKey(selectedDepartment)) {
            Set<String> tracks = departmentTrackMap.get(selectedDepartment);
            if (tracks != null) {
                Log.d(TAG, "트랙 개수: " + tracks.size() + ", 내용: " + tracks);
                trackAdapter.addAll(new ArrayList<>(tracks));
            }
        } else if ("전체".equals(selectedDepartment)) {
            // "전체" 선택 시 모든 트랙 표시
            Set<String> allTracks = new HashSet<>();
            for (Set<String> tracks : departmentTrackMap.values()) {
                allTracks.addAll(tracks);
            }
            Log.d(TAG, "전체 트랙 개수: " + allTracks.size() + ", 내용: " + allTracks);
            trackAdapter.addAll(new ArrayList<>(allTracks));
        }

        trackAdapter.notifyDataSetChanged();
    }

    /**
     * 선택된 조건으로 필터링
     */
    private void filterRequirements() {
        String selectedYear = spinnerStudentId.getSelectedItem() != null ?
            spinnerStudentId.getSelectedItem().toString() : "전체";
        String selectedDepartment = spinnerDepartment.getSelectedItem() != null ?
            spinnerDepartment.getSelectedItem().toString() : "전체";
        String selectedTrack = spinnerTrack.getSelectedItem() != null ?
            spinnerTrack.getSelectedItem().toString() : "전체";

        // 마지막 선택 조건 저장 (편집 후 복원용)
        lastSelectedYear = selectedYear;
        lastSelectedDepartment = selectedDepartment;
        lastSelectedTrack = selectedTrack;

        // 2자리 학번을 4자리로 변환 ("20" -> "2020")
        if (!"전체".equals(selectedYear) && selectedYear.length() == 2) {
            selectedYear = "20" + selectedYear;
        }

        Log.d(TAG, "필터링: " + selectedYear + "/" + selectedDepartment + "/" + selectedTrack);

        List<GraduationRequirement> filtered = new ArrayList<>();

        for (GraduationRequirement req : allRequirements) {
            boolean matches = true;

            if (!"전체".equals(selectedYear) && !selectedYear.equals(req.getYear())) {
                matches = false;
            }
            if (!"전체".equals(selectedDepartment) && !selectedDepartment.equals(req.getDepartment())) {
                matches = false;
            }
            if (!"전체".equals(selectedTrack) && !selectedTrack.equals(req.getTrack())) {
                matches = false;
            }

            if (matches) {
                filtered.add(req);
            }
        }

        Log.d(TAG, "필터링 결과: " + filtered.size() + "개");
        displayResults(filtered);
    }

    /**
     * 검색 결과 표시
     */
    private void displayResults(List<GraduationRequirement> requirements) {
        showLoading(false);

        if (requirements.isEmpty()) {
            rvRequirements.setVisibility(View.GONE);
            Toast.makeText(this, "조회된 졸업요건이 없습니다", Toast.LENGTH_SHORT).show();
        } else {
            rvRequirements.setVisibility(View.VISIBLE);
            adapter.setRequirements(requirements);
        }
    }

    /**
     * 로딩 표시 제어
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvRequirements.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * 옵션 메뉴 생성
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_graduation_requirements, menu);
        return true;
    }

    /**
     * 옵션 메뉴 아이템 선택
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            toggleDeleteMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 삭제 모드 토글
     */
    private void toggleDeleteMode() {
        boolean newDeleteMode = !adapter.isDeleteMode();
        adapter.setDeleteMode(newDeleteMode);

        if (newDeleteMode) {
            // 삭제 모드 활성화
            Toast.makeText(this, "삭제할 항목을 선택하세요", Toast.LENGTH_SHORT).show();
            // Toolbar 제목 변경
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("삭제 모드");
            }
            // FAB 숨기기
            fabAdd.hide();
            // 삭제 버튼 표시
            btnDeleteSelected.setVisibility(View.VISIBLE);
            btnDeleteSelected.setEnabled(false);
            btnDeleteSelected.setText("선택 항목 삭제");
        } else {
            // 삭제 모드 비활성화 - 그냥 종료
            exitDeleteMode();
        }
    }

    /**
     * 삭제 확인 다이얼로그 표시
     */
    private void showDeleteConfirmDialog() {
        int selectedCount = adapter.getSelectedCount();
        if (selectedCount == 0) {
            Toast.makeText(this, "삭제할 항목을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("졸업요건 삭제")
                .setMessage(selectedCount + "개의 졸업요건을 삭제하시겠습니까?")
                .setNegativeButton("예", (dialog, which) -> deleteSelectedRequirements())
                .setPositiveButton("아니오", null)
                .show();
    }

    /**
     * 선택된 졸업요건 삭제
     */
    private void deleteSelectedRequirements() {
        Set<String> selectedIds = adapter.getSelectedIds();
        if (selectedIds.isEmpty()) {
            exitDeleteMode();
            return;
        }

        showLoading(true);

        // 삭제할 개수 추적
        final int[] deletedCount = {0};
        final int totalCount = selectedIds.size();

        for (String docId : selectedIds) {
            db.collection("graduation_requirements")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        deletedCount[0]++;
                        Log.d(TAG, "문서 삭제 성공: " + docId);

                        // 모든 삭제가 완료되었는지 확인
                        if (deletedCount[0] == totalCount) {
                            showLoading(false);
                            Toast.makeText(this, totalCount + "개의 졸업요건이 삭제되었습니다",
                                    Toast.LENGTH_SHORT).show();
                            exitDeleteMode();
                            // 데이터 새로고침
                            loadAllRequirements();
                        }
                    })
                    .addOnFailureListener(e -> {
                        deletedCount[0]++;
                        Log.e(TAG, "문서 삭제 실패: " + docId, e);

                        // 모든 작업 완료 확인 (실패 포함)
                        if (deletedCount[0] == totalCount) {
                            showLoading(false);
                            Toast.makeText(this, "일부 졸업요건 삭제 실패: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            exitDeleteMode();
                            loadAllRequirements();
                        }
                    });
        }
    }

    /**
     * 삭제 모드 종료
     */
    private void exitDeleteMode() {
        adapter.setDeleteMode(false);
        // Toolbar 제목 복원
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("졸업요건 관리");
        }
        // FAB 표시
        fabAdd.show();
        // 삭제 버튼 숨기기
        btnDeleteSelected.setVisibility(View.GONE);
    }

    /**
     * 뒤로 가기 버튼 처리 (삭제 모드에서는 모드 종료)
     */
    @Override
    public void onBackPressed() {
        if (adapter.isDeleteMode()) {
            exitDeleteMode();
        } else {
            super.onBackPressed();
        }
    }

}
