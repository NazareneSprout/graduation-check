package sprout.app.sakmvp1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 대체과목 관리 Activity
 * 관리자가 폐지된 과목과 그 대체 과목을 관리할 수 있는 화면
 */
public class ReplacementCourseManagementActivity extends AppCompatActivity {

    private static final String TAG = "ReplacementCourseMgmt";
    private static final String COLLECTION_NAME = "replacement_courses";

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupSort;
    private Chip chipSortName, chipSortDate;
    private TextView tvResultCount;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private RecyclerView rvReplacementCourses;
    private FloatingActionButton fabAdd;

    // Data
    private FirebaseFirestore db;
    private ReplacementCourseAdapter adapter;
    private List<ReplacementCourse> allCourses = new ArrayList<>();
    private List<ReplacementCourse> filteredCourses = new ArrayList<>();

    private String searchQuery = "";
    private SortType currentSortType = SortType.NAME;

    private enum SortType {
        NAME,   // 과목명순
        DATE    // 등록일순
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replacement_course_management);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        loadReplacementCourses();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.et_search);
        chipGroupSort = findViewById(R.id.chip_group_sort);
        chipSortName = findViewById(R.id.chip_sort_name);
        chipSortDate = findViewById(R.id.chip_sort_date);
        tvResultCount = findViewById(R.id.tv_result_count);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        rvReplacementCourses = findViewById(R.id.rv_replacement_courses);
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
        adapter = new ReplacementCourseAdapter();
        rvReplacementCourses.setLayoutManager(new LinearLayoutManager(this));
        rvReplacementCourses.setAdapter(adapter);

        adapter.setOnItemClickListener(course -> {
            // 클릭 시 상세 정보 표시 또는 편집
            showEditDialog(course);
        });

        adapter.setOnEditClickListener(this::showEditDialog);

        adapter.setOnDeleteClickListener(this::showDeleteDialog);
    }

    private void setupListeners() {
        // FAB: 새 대체과목 추가
        fabAdd.setOnClickListener(v -> showAddDialog());

        // 검색
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 정렬 옵션 변경
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_sort_name)) {
                currentSortType = SortType.NAME;
            } else if (checkedIds.contains(R.id.chip_sort_date)) {
                currentSortType = SortType.DATE;
            }
            applyFilters();
        });
    }

    /**
     * Firestore에서 대체과목 목록 로드
     */
    private void loadReplacementCourses() {
        showLoading(true);

        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allCourses.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ReplacementCourse course = document.toObject(ReplacementCourse.class);
                        course.setId(document.getId());
                        allCourses.add(course);
                    }

                    Log.d(TAG, "대체과목 로드 완료: " + allCourses.size() + "개");
                    showLoading(false);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "데이터 로드 실패", e);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 검색 및 정렬 필터 적용
     */
    private void applyFilters() {
        filteredCourses.clear();

        // 1. 검색 필터 적용
        for (ReplacementCourse course : allCourses) {
            if (searchQuery.isEmpty() ||
                course.getDiscontinuedCourseName().toLowerCase().contains(searchQuery.toLowerCase())) {
                filteredCourses.add(course);
            }
        }

        // 2. 정렬 적용
        sortCourses();

        // 3. 어댑터 업데이트
        adapter.setCourses(filteredCourses);

        // 4. 결과 카운트 및 빈 상태 업데이트
        updateResultCount();
        updateEmptyState();
    }

    /**
     * 대체과목 정렬
     */
    private void sortCourses() {
        switch (currentSortType) {
            case NAME:
                // 과목명순 정렬 (가나다순)
                Collections.sort(filteredCourses, new Comparator<ReplacementCourse>() {
                    @Override
                    public int compare(ReplacementCourse o1, ReplacementCourse o2) {
                        return o1.getDiscontinuedCourseName().compareTo(o2.getDiscontinuedCourseName());
                    }
                });
                break;

            case DATE:
                // 등록일순 정렬 (최신순)
                Collections.sort(filteredCourses, new Comparator<ReplacementCourse>() {
                    @Override
                    public int compare(ReplacementCourse o1, ReplacementCourse o2) {
                        return Long.compare(o2.getCreatedAt(), o1.getCreatedAt());
                    }
                });
                break;
        }
    }

    /**
     * 결과 카운트 업데이트
     */
    private void updateResultCount() {
        String countText;
        if (searchQuery.isEmpty()) {
            countText = String.format("전체 %d개", filteredCourses.size());
        } else {
            countText = String.format("검색 결과 %d개 / 전체 %d개",
                    filteredCourses.size(), allCourses.size());
        }
        tvResultCount.setText(countText);
    }

    /**
     * 빈 상태 업데이트
     */
    private void updateEmptyState() {
        if (filteredCourses.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvReplacementCourses.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvReplacementCourses.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 새 대체과목 추가 다이얼로그 표시
     */
    private void showAddDialog() {
        showEditDialog(null);
    }

    /**
     * 대체과목 추가/편집 다이얼로그 표시 (검색 기반)
     */
    private void showEditDialog(ReplacementCourse existingCourse) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_replacement_course, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // UI 참조
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        MaterialButton btnClose = dialogView.findViewById(R.id.btn_close);
        Spinner spinnerDepartment = dialogView.findViewById(R.id.spinner_department);
        Spinner spinnerTrack = dialogView.findViewById(R.id.spinner_track);
        Spinner spinnerYear = dialogView.findViewById(R.id.spinner_year);
        TextView tvCourseLoadingStatus = dialogView.findViewById(R.id.tv_course_loading_status);

        // 폐지된 과목 관련
        TextInputEditText etDiscontinuedSearch = dialogView.findViewById(R.id.et_discontinued_search);
        MaterialButton btnSearchDiscontinued = dialogView.findViewById(R.id.btn_search_discontinued);
        RecyclerView rvDiscontinuedResults = dialogView.findViewById(R.id.rv_discontinued_results);
        com.google.android.material.card.MaterialCardView cardSelectedDiscontinued =
            dialogView.findViewById(R.id.card_selected_discontinued);
        TextView tvSelectedDiscontinuedName = dialogView.findViewById(R.id.tv_selected_discontinued_name);
        TextView tvSelectedDiscontinuedCredit = dialogView.findViewById(R.id.tv_selected_discontinued_credit);
        MaterialButton btnClearDiscontinued = dialogView.findViewById(R.id.btn_clear_discontinued);
        TextInputEditText etDiscontinuedCredit = dialogView.findViewById(R.id.et_discontinued_course_credit);

        // 대체 과목 관련
        TextInputEditText etReplacementSearch = dialogView.findViewById(R.id.et_replacement_search);
        MaterialButton btnSearchReplacement = dialogView.findViewById(R.id.btn_search_replacement);
        RecyclerView rvReplacementResults = dialogView.findViewById(R.id.rv_replacement_results);
        TextView tvNoReplacements = dialogView.findViewById(R.id.tv_no_replacements);
        ChipGroup chipGroupReplacements = dialogView.findViewById(R.id.chip_group_replacements);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // 데이터 구조
        final String[] selectedDepartment = {null};
        final String[] selectedTrack = {null};
        final String[] selectedYear = {null};
        final String[] selectedDiscontinuedName = {null};
        final int[] selectedDiscontinuedCredit = {0};
        final List<String> replacementCourses = new ArrayList<>();
        final Map<String, List<FirebaseDataManager.CourseInfo>> departmentCoursesMap = new HashMap<>();

        // RecyclerView 설정
        rvDiscontinuedResults.setLayoutManager(new LinearLayoutManager(this));
        rvReplacementResults.setLayoutManager(new LinearLayoutManager(this));

        // 학부/트랙/연도 스피너 로드
        loadDepartmentSpinnerForDialog(spinnerDepartment, spinnerTrack, spinnerYear,
                tvCourseLoadingStatus, departmentCoursesMap, selectedDepartment, selectedTrack, selectedYear,
                etDiscontinuedSearch, etReplacementSearch, rvDiscontinuedResults, rvReplacementResults);

        // 편집 모드
        if (existingCourse != null) {
            tvTitle.setText("대체과목 편집");
            selectedDepartment[0] = existingCourse.getDepartment();
            selectedDiscontinuedName[0] = existingCourse.getDiscontinuedCourseName();
            selectedDiscontinuedCredit[0] = existingCourse.getDiscontinuedCourseCredit();

            cardSelectedDiscontinued.setVisibility(View.VISIBLE);
            tvSelectedDiscontinuedName.setText(existingCourse.getDiscontinuedCourseName());
            tvSelectedDiscontinuedCredit.setText(existingCourse.getDiscontinuedCourseCredit() + "학점");

            if (existingCourse.getReplacementCourseNames() != null) {
                replacementCourses.addAll(existingCourse.getReplacementCourseNames());
                updateReplacementChips(chipGroupReplacements, replacementCourses, tvNoReplacements);
            }
        }

        // 폐지된 과목 검색 버튼
        btnSearchDiscontinued.setOnClickListener(v -> {
            if (selectedDepartment[0] == null || selectedTrack[0] == null || selectedYear[0] == null) {
                Toast.makeText(ReplacementCourseManagementActivity.this,
                    "학부, 트랙, 연도를 모두 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String query = etDiscontinuedSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(ReplacementCourseManagementActivity.this,
                    "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String key = selectedDepartment[0] + "_" + selectedTrack[0] + "_" + selectedYear[0];
            searchCourses(query, departmentCoursesMap.get(key),
                rvDiscontinuedResults, (course) -> {
                    selectedDiscontinuedName[0] = course.getName();
                    selectedDiscontinuedCredit[0] = course.getCredits();
                    cardSelectedDiscontinued.setVisibility(View.VISIBLE);
                    tvSelectedDiscontinuedName.setText(course.getName());
                    tvSelectedDiscontinuedCredit.setText(course.getCredits() + "학점");
                    rvDiscontinuedResults.setVisibility(View.GONE);
                    etDiscontinuedSearch.setText("");
                });
        });

        // 선택된 폐지된 과목 제거
        btnClearDiscontinued.setOnClickListener(v -> {
            selectedDiscontinuedName[0] = null;
            selectedDiscontinuedCredit[0] = 0;
            cardSelectedDiscontinued.setVisibility(View.GONE);
        });

        // 대체 과목 검색 버튼
        btnSearchReplacement.setOnClickListener(v -> {
            if (selectedDepartment[0] == null || selectedTrack[0] == null || selectedYear[0] == null) {
                Toast.makeText(ReplacementCourseManagementActivity.this,
                    "학부, 트랙, 연도를 모두 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String query = etReplacementSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(ReplacementCourseManagementActivity.this,
                    "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String key = selectedDepartment[0] + "_" + selectedTrack[0] + "_" + selectedYear[0];
            searchCourses(query, departmentCoursesMap.get(key),
                rvReplacementResults, (course) -> {
                    if (!replacementCourses.contains(course.getName())) {
                        replacementCourses.add(course.getName());
                        updateReplacementChips(chipGroupReplacements, replacementCourses, tvNoReplacements);
                        etReplacementSearch.setText("");
                        rvReplacementResults.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(ReplacementCourseManagementActivity.this,
                            "이미 추가된 과목입니다", Toast.LENGTH_SHORT).show();
                    }
                });
        });

        // 닫기 버튼
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 저장 버튼
        btnSave.setOnClickListener(v -> {
            // 유효성 검사
            if (selectedDepartment[0] == null) {
                Toast.makeText(this, "학부를 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedDiscontinuedName[0] == null || selectedDiscontinuedName[0].isEmpty()) {
                Toast.makeText(this, "폐지된 과목을 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 대체과목 객체 생성/수정
            ReplacementCourse course;
            if (existingCourse != null) {
                course = existingCourse;
                course.setDepartment(selectedDepartment[0]);
                course.setDiscontinuedCourseName(selectedDiscontinuedName[0]);
                course.setDiscontinuedCourseCredit(selectedDiscontinuedCredit[0]);
                course.setReplacementCourseNames(new ArrayList<>(replacementCourses));
                course.setNote("");
                course.setUpdatedAt(System.currentTimeMillis());
            } else {
                course = new ReplacementCourse(
                        selectedDepartment[0],
                        selectedDiscontinuedName[0],
                        selectedDiscontinuedCredit[0],
                        new ArrayList<>(replacementCourses),
                        ""
                );
            }

            saveCourse(course, existingCourse != null);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 대체 과목 Chip 업데이트
     */
    private void updateReplacementChips(ChipGroup chipGroup, List<String> replacementCourses, TextView tvNoReplacements) {
        chipGroup.removeAllViews();

        if (replacementCourses.isEmpty()) {
            tvNoReplacements.setVisibility(View.VISIBLE);
        } else {
            tvNoReplacements.setVisibility(View.GONE);
            for (String courseName : replacementCourses) {
                Chip chip = new Chip(this);
                chip.setText(courseName);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> {
                    replacementCourses.remove(courseName);
                    updateReplacementChips(chipGroup, replacementCourses, tvNoReplacements);
                });
                chipGroup.addView(chip);
            }
        }
    }

    /**
     * 학부/트랙/연도 스피너 로드 (다이얼로그용) - 계단식 선택 구현
     */
    private void loadDepartmentSpinnerForDialog(Spinner spinnerDepartment,
                                                Spinner spinnerTrack,
                                                Spinner spinnerYear,
                                                TextView tvLoadingStatus,
                                                Map<String, List<FirebaseDataManager.CourseInfo>> coursesMap,
                                                String[] selectedDeptArray,
                                                String[] selectedTrackArray,
                                                String[] selectedYearArray,
                                                TextInputEditText etDiscontinuedSearch,
                                                TextInputEditText etReplacementSearch,
                                                RecyclerView rvDiscontinuedResults,
                                                RecyclerView rvReplacementResults) {
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // 모든 졸업요건 문서를 학부별로 그룹화
                    Map<String, List<GraduationRequirement>> departmentDocsMap = new HashMap<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        if (document.getId().startsWith("교양_")) continue;

                        GraduationRequirement req = new GraduationRequirement();
                        req.setId(document.getId());

                        String dept = req.getDepartment();
                        if (dept != null) {
                            if (!departmentDocsMap.containsKey(dept)) {
                                departmentDocsMap.put(dept, new ArrayList<>());
                            }
                            departmentDocsMap.get(dept).add(req);
                        }
                    }

                    // 1. 학부 스피너 설정
                    List<String> departments = new ArrayList<>();
                    departments.add("학부 선택");
                    departments.addAll(new ArrayList<>(departmentDocsMap.keySet()));

                    ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, departments);
                    deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDepartment.setAdapter(deptAdapter);

                    // 2. 학부 선택 리스너 - 트랙 스피너 업데이트
                    spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selected = (String) parent.getItemAtPosition(position);

                            if (selected != null && !selected.equals("학부 선택")) {
                                selectedDeptArray[0] = selected;
                                selectedTrackArray[0] = null;
                                selectedYearArray[0] = null;

                                // 트랙 목록 추출
                                List<GraduationRequirement> docs = departmentDocsMap.get(selected);
                                Set<String> tracksSet = new HashSet<>();
                                for (GraduationRequirement doc : docs) {
                                    tracksSet.add(doc.getTrack());
                                }

                                List<String> tracks = new ArrayList<>();
                                tracks.add("트랙 선택");
                                tracks.addAll(tracksSet);

                                ArrayAdapter<String> trackAdapter = new ArrayAdapter<>(
                                    ReplacementCourseManagementActivity.this,
                                    android.R.layout.simple_spinner_item, tracks);
                                trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerTrack.setAdapter(trackAdapter);
                                spinnerTrack.setEnabled(true);

                                // 연도 스피너 초기화
                                spinnerYear.setAdapter(null);
                                spinnerYear.setEnabled(false);

                                Log.d(TAG, "학부 선택: " + selected + ", 트랙 수: " + tracksSet.size());
                            } else {
                                selectedDeptArray[0] = null;
                                selectedTrackArray[0] = null;
                                selectedYearArray[0] = null;
                                spinnerTrack.setAdapter(null);
                                spinnerTrack.setEnabled(false);
                                spinnerYear.setAdapter(null);
                                spinnerYear.setEnabled(false);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    // 3. 트랙 선택 리스너 - 연도 스피너 업데이트
                    spinnerTrack.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selectedTrack = (String) parent.getItemAtPosition(position);

                            if (selectedTrack != null && !selectedTrack.equals("트랙 선택") && selectedDeptArray[0] != null) {
                                selectedTrackArray[0] = selectedTrack;
                                selectedYearArray[0] = null;

                                // 연도 목록 추출
                                List<GraduationRequirement> docs = departmentDocsMap.get(selectedDeptArray[0]);
                                Set<String> yearsSet = new HashSet<>();
                                for (GraduationRequirement doc : docs) {
                                    if (doc.getTrack().equals(selectedTrack)) {
                                        yearsSet.add(doc.getYear());
                                    }
                                }

                                List<String> years = new ArrayList<>();
                                years.add("연도 선택");
                                years.addAll(yearsSet);

                                ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                                    ReplacementCourseManagementActivity.this,
                                    android.R.layout.simple_spinner_item, years);
                                yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerYear.setAdapter(yearAdapter);
                                spinnerYear.setEnabled(true);

                                Log.d(TAG, "트랙 선택: " + selectedTrack + ", 연도 수: " + yearsSet.size());
                            } else {
                                selectedTrackArray[0] = null;
                                selectedYearArray[0] = null;
                                spinnerYear.setAdapter(null);
                                spinnerYear.setEnabled(false);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    // 4. 연도 선택 리스너 - 과목 데이터 로드
                    spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selectedYear = (String) parent.getItemAtPosition(position);

                            if (selectedYear != null && !selectedYear.equals("연도 선택") &&
                                selectedDeptArray[0] != null && selectedTrackArray[0] != null) {

                                selectedYearArray[0] = selectedYear;

                                // 선택된 문서로 과목 로드
                                String key = selectedDeptArray[0] + "_" + selectedTrackArray[0] + "_" + selectedYear;
                                coursesMap.put(key, new ArrayList<>());

                                // 로딩 상태 표시
                                tvLoadingStatus.setVisibility(View.VISIBLE);
                                tvLoadingStatus.setText("과목 데이터를 로드 중입니다...");
                                Log.d(TAG, "로딩 상태 표시: 과목 데이터를 로드 중입니다...");

                                // 로딩 완료 추적을 위한 카운터 (3개의 API 호출)
                                final int[] loadingCount = {3};

                                FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

                                // 전공필수
                                dataManager.loadMajorCourses(selectedDeptArray[0], selectedTrackArray[0],
                                        selectedYear, "전공필수",
                                        new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                                            @Override
                                            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                                                List<FirebaseDataManager.CourseInfo> currentList = coursesMap.get(key);
                                                if (currentList != null) {
                                                    currentList.addAll(courses);
                                                }
                                                Log.d(TAG, key + " - 전공필수: " + courses.size() + "개 과목 로드");

                                                // 로딩 완료 체크
                                                loadingCount[0]--;
                                                Log.d(TAG, "로딩 카운터: " + loadingCount[0]);
                                                if (loadingCount[0] == 0) {
                                                    int totalCount = currentList != null ? currentList.size() : 0;
                                                    String message = "총 " + totalCount + "개 과목 로드 완료!";
                                                    tvLoadingStatus.setText(message);
                                                    Log.d(TAG, "로딩 완료: " + message);
                                                    tvLoadingStatus.postDelayed(() -> {
                                                        tvLoadingStatus.setVisibility(View.GONE);
                                                        Log.d(TAG, "로딩 상태 숨김");
                                                    }, 1500);
                                                }
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Log.e(TAG, key + " - 전공필수 로드 실패", e);
                                                loadingCount[0]--;
                                                if (loadingCount[0] == 0) {
                                                    tvLoadingStatus.setText("로드 완료");
                                                    tvLoadingStatus.postDelayed(() -> {
                                                        tvLoadingStatus.setVisibility(View.GONE);
                                                    }, 1500);
                                                }
                                            }
                                        });

                                // 전공선택
                                dataManager.loadMajorCourses(selectedDeptArray[0], selectedTrackArray[0],
                                        selectedYear, "전공선택",
                                        new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                                            @Override
                                            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                                                List<FirebaseDataManager.CourseInfo> currentList = coursesMap.get(key);
                                                if (currentList != null) {
                                                    currentList.addAll(courses);
                                                }
                                                Log.d(TAG, key + " - 전공선택: " + courses.size() + "개 과목 로드");

                                                // 로딩 완료 체크
                                                loadingCount[0]--;
                                                Log.d(TAG, "로딩 카운터: " + loadingCount[0]);
                                                if (loadingCount[0] == 0) {
                                                    int totalCount = currentList != null ? currentList.size() : 0;
                                                    String message = "총 " + totalCount + "개 과목 로드 완료!";
                                                    tvLoadingStatus.setText(message);
                                                    Log.d(TAG, "로딩 완료: " + message);
                                                    tvLoadingStatus.postDelayed(() -> {
                                                        tvLoadingStatus.setVisibility(View.GONE);
                                                        Log.d(TAG, "로딩 상태 숨김");
                                                    }, 1500);
                                                }
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Log.e(TAG, key + " - 전공선택 로드 실패", e);
                                                loadingCount[0]--;
                                                if (loadingCount[0] == 0) {
                                                    tvLoadingStatus.setText("로드 완료");
                                                    tvLoadingStatus.postDelayed(() -> {
                                                        tvLoadingStatus.setVisibility(View.GONE);
                                                    }, 1500);
                                                }
                                            }
                                        });

                                // 학부공통/전공심화
                                dataManager.loadDepartmentCommonCourses(selectedDeptArray[0], selectedTrackArray[0],
                                        selectedYear,
                                        new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                                            @Override
                                            public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                                                List<FirebaseDataManager.CourseInfo> currentList = coursesMap.get(key);
                                                if (currentList != null) {
                                                    currentList.addAll(courses);
                                                }
                                                String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(
                                                    selectedDeptArray[0], selectedYear);
                                                Log.d(TAG, key + " - " + categoryName + ": " + courses.size() + "개 과목 로드");

                                                // 로딩 완료 체크
                                                loadingCount[0]--;
                                                Log.d(TAG, "로딩 카운터: " + loadingCount[0]);
                                                if (loadingCount[0] == 0) {
                                                    int totalCount = currentList != null ? currentList.size() : 0;
                                                    String message = "총 " + totalCount + "개 과목 로드 완료!";
                                                    tvLoadingStatus.setText(message);
                                                    Log.d(TAG, "로딩 완료: " + message);
                                                    tvLoadingStatus.postDelayed(() -> {
                                                        tvLoadingStatus.setVisibility(View.GONE);
                                                        Log.d(TAG, "로딩 상태 숨김");
                                                    }, 1500);
                                                }
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Log.e(TAG, key + " - 학부공통/전공심화 로드 실패", e);
                                                loadingCount[0]--;
                                                if (loadingCount[0] == 0) {
                                                    tvLoadingStatus.setText("로드 완료");
                                                    tvLoadingStatus.postDelayed(() -> {
                                                        tvLoadingStatus.setVisibility(View.GONE);
                                                    }, 1500);
                                                }
                                            }
                                        });

                                Log.d(TAG, "과목 로드 시작: " + key);
                            } else {
                                selectedYearArray[0] = null;
                                tvLoadingStatus.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    /**
     * 과목 검색
     */
    private void searchCourses(String query, List<FirebaseDataManager.CourseInfo> courses,
                              RecyclerView recyclerView, OnCourseSelectListener listener) {
        if (courses == null || courses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            return;
        }

        List<FirebaseDataManager.CourseInfo> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (FirebaseDataManager.CourseInfo course : courses) {
            if (course.getName().toLowerCase().contains(lowerQuery)) {
                results.add(course);
            }
        }

        if (results.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
        } else {
            CourseSearchAdapter adapter = new CourseSearchAdapter(results, listener);
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 과목 선택 리스너 인터페이스
     */
    interface OnCourseSelectListener {
        void onCourseSelected(FirebaseDataManager.CourseInfo course);
    }

    /**
     * 과목 검색 결과 어댑터
     */
    class CourseSearchAdapter extends RecyclerView.Adapter<CourseSearchAdapter.ViewHolder> {
        private List<FirebaseDataManager.CourseInfo> courses;
        private OnCourseSelectListener listener;

        CourseSearchAdapter(List<FirebaseDataManager.CourseInfo> courses, OnCourseSelectListener listener) {
            this.courses = courses;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_course_search_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(courses.get(position));
        }

        @Override
        public int getItemCount() {
            return courses.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCourseName, tvCourseCredit;
            MaterialButton btnSelect;

            ViewHolder(View itemView) {
                super(itemView);
                tvCourseName = itemView.findViewById(R.id.tv_course_name);
                tvCourseCredit = itemView.findViewById(R.id.tv_course_credit);
                btnSelect = itemView.findViewById(R.id.btn_select);
            }

            void bind(FirebaseDataManager.CourseInfo course) {
                tvCourseName.setText(course.getName());
                tvCourseCredit.setText(course.getCredits() + "학점");
                btnSelect.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCourseSelected(course);
                    }
                });
            }
        }
    }

    /**
     * Firestore에 대체과목 저장
     */
    private void saveCourse(ReplacementCourse course, boolean isUpdate) {
        showLoading(true);

        // Firestore에 저장할 데이터 준비
        Map<String, Object> data = new HashMap<>();
        data.put("department", course.getDepartment());
        data.put("discontinuedCourseName", course.getDiscontinuedCourseName());
        data.put("discontinuedCourseCredit", course.getDiscontinuedCourseCredit());
        data.put("replacementCourseNames", course.getReplacementCourseNames());
        data.put("note", course.getNote());
        data.put("createdAt", course.getCreatedAt());
        data.put("updatedAt", course.getUpdatedAt());

        if (isUpdate && course.getId() != null) {
            // 기존 문서 업데이트
            db.collection(COLLECTION_NAME)
                    .document(course.getId())
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "대체과목이 수정되었습니다", Toast.LENGTH_SHORT).show();
                        loadReplacementCourses();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "저장 실패", e);
                        Toast.makeText(this, "저장 실패: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 새 문서 추가
            db.collection(COLLECTION_NAME)
                    .add(data)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "대체과목이 추가되었습니다", Toast.LENGTH_SHORT).show();
                        loadReplacementCourses();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "저장 실패", e);
                        Toast.makeText(this, "저장 실패: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * 삭제 확인 다이얼로그 표시
     */
    private void showDeleteDialog(ReplacementCourse course) {
        new AlertDialog.Builder(this)
                .setTitle("대체과목 삭제")
                .setMessage("'" + course.getDiscontinuedCourseName() + "' 대체과목을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteCourse(course))
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * Firestore에서 대체과목 삭제
     */
    private void deleteCourse(ReplacementCourse course) {
        if (course.getId() == null) {
            Toast.makeText(this, "삭제할 문서 ID가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        db.collection(COLLECTION_NAME)
                .document(course.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "대체과목이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    loadReplacementCourses();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "삭제 실패", e);
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 로딩 상태 표시
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvReplacementCourses.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * 학과 스피너와 자동완성 설정
     */
    private void setupDepartmentSpinnerAndAutoComplete(Spinner spinnerDepartment,
                                                       AutoCompleteTextView etDiscontinuedName,
                                                       AutoCompleteTextView etReplacementCourse,
                                                       Map<String, List<String>> departmentCoursesMap) {
        // Firestore에서 학과별 과목 데이터 로드
        loadDepartmentCoursesFromFirestore(spinnerDepartment, etDiscontinuedName,
                etReplacementCourse, departmentCoursesMap);
    }

    /**
     * Firestore에서 학부별 과목 데이터 로드
     * graduation_requirements 컬렉션에서 직접 과목 데이터 조회
     */
    private void loadDepartmentCoursesFromFirestore(Spinner spinnerDepartment,
                                                    AutoCompleteTextView etDiscontinuedName,
                                                    AutoCompleteTextView etReplacementCourse,
                                                    Map<String, List<String>> departmentCoursesMap) {
        // graduation_requirements에서 학부 목록 및 과목 데이터 가져오기
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 학부별로 그룹화된 데이터 구조
                    // department -> (year, track) -> document
                    Map<String, List<GraduationRequirement>> departmentDocumentsMap = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // 교양 문서는 제외
                        if (document.getId().startsWith("교양_")) {
                            continue;
                        }

                        // 문서 ID 파싱하여 GraduationRequirement 객체 생성
                        GraduationRequirement requirement = new GraduationRequirement();
                        requirement.setId(document.getId());

                        // 학부별로 그룹화
                        String dept = requirement.getDepartment();
                        if (dept != null) {
                            if (!departmentDocumentsMap.containsKey(dept)) {
                                departmentDocumentsMap.put(dept, new ArrayList<>());
                            }
                            departmentDocumentsMap.get(dept).add(requirement);
                        }
                    }

                    // 스피너용 학부 리스트 생성
                    List<String> departments = new ArrayList<>();
                    departments.add("학부 선택");
                    departments.addAll(new ArrayList<>(departmentDocumentsMap.keySet()));

                    // 스피너 설정
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, departments);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDepartment.setAdapter(spinnerAdapter);

                    // 학부별 과목 데이터 로드
                    FirebaseDataManager dataManager = FirebaseDataManager.getInstance();

                    for (String dept : departmentDocumentsMap.keySet()) {
                        List<GraduationRequirement> deptDocs = departmentDocumentsMap.get(dept);
                        if (deptDocs.isEmpty()) continue;

                        // 해당 학부의 첫 번째 문서를 사용하여 과목 데이터 로드
                        GraduationRequirement firstDoc = deptDocs.get(0);

                        // 중복 제거를 위한 Set 사용
                        Set<String> courseNamesSet = new HashSet<>();

                        // 전공필수, 전공선택, 전공심화/학부공통 과목 모두 로드
                        loadCoursesForDepartment(dataManager, dept, firstDoc.getTrack(),
                                firstDoc.getYear(), "전공필수", courseNamesSet);
                        loadCoursesForDepartment(dataManager, dept, firstDoc.getTrack(),
                                firstDoc.getYear(), "전공선택", courseNamesSet);

                        // 전공심화나 학부공통 시도
                        String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(dept, firstDoc.getYear());
                        if ("전공심화".equals(categoryName)) {
                            loadDepartmentCommonCoursesForDepartment(dataManager, dept,
                                    firstDoc.getTrack(), firstDoc.getYear(), courseNamesSet);
                        } else {
                            loadDepartmentCommonCoursesForDepartment(dataManager, dept,
                                    firstDoc.getTrack(), firstDoc.getYear(), courseNamesSet);
                        }

                        // Set을 List로 변환하여 저장
                        departmentCoursesMap.put(dept, new ArrayList<>(courseNamesSet));
                    }

                    // 스피너 선택 리스너 설정
                    spinnerDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selectedDepartment = (String) parent.getItemAtPosition(position);
                            if (selectedDepartment != null && !selectedDepartment.equals("학부 선택")) {
                                // 선택된 학부의 과목으로 자동완성 업데이트
                                List<String> courseNames = departmentCoursesMap.get(selectedDepartment);
                                if (courseNames != null && !courseNames.isEmpty()) {
                                    ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                                            ReplacementCourseManagementActivity.this,
                                            android.R.layout.simple_dropdown_item_1line,
                                            courseNames);
                                    etDiscontinuedName.setAdapter(courseAdapter);
                                    etDiscontinuedName.setThreshold(1);
                                    etReplacementCourse.setAdapter(courseAdapter);
                                    etReplacementCourse.setThreshold(1);

                                    Log.d(TAG, "학부 '" + selectedDepartment + "' 선택 - " +
                                            courseNames.size() + "개 과목 자동완성 설정");
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학부 데이터 로드 실패", e);
                    Toast.makeText(this, "학부 데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 특정 카테고리의 과목을 로드하여 Set에 추가
     */
    private void loadCoursesForDepartment(FirebaseDataManager dataManager, String department,
                                         String track, String year, String category,
                                         Set<String> courseNamesSet) {
        dataManager.loadMajorCourses(department, track, year, category,
                new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                    @Override
                    public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                        for (FirebaseDataManager.CourseInfo course : courses) {
                            courseNamesSet.add(course.getName());
                        }
                        Log.d(TAG, department + " - " + category + ": " + courses.size() + "개 과목 로드");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, department + " - " + category + " 로드 실패", e);
                    }
                });
    }

    /**
     * 학부공통/전공심화 과목을 로드하여 Set에 추가
     */
    private void loadDepartmentCommonCoursesForDepartment(FirebaseDataManager dataManager,
                                                         String department, String track,
                                                         String year, Set<String> courseNamesSet) {
        dataManager.loadDepartmentCommonCourses(department, track, year,
                new FirebaseDataManager.OnMajorCoursesLoadedListener() {
                    @Override
                    public void onSuccess(List<FirebaseDataManager.CourseInfo> courses) {
                        for (FirebaseDataManager.CourseInfo course : courses) {
                            courseNamesSet.add(course.getName());
                        }
                        String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(department, year);
                        Log.d(TAG, department + " - " + categoryName + ": " + courses.size() + "개 과목 로드");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, department + " - 학부공통/전공심화 로드 실패", e);
                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        // 화면 복귀 시 목록 새로고침
        loadReplacementCourses();
    }
}
