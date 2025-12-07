package sprout.app.sakmvp1;

import android.os.Bundle;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sprout.app.sakmvp1.adapters.OtherRequirementGroupAdapter;
import sprout.app.sakmvp1.models.OtherRequirementGroup;

/**
 * 기타 졸업요건 관리 Activity (관리자용)
 * Firestore의 other_requirements_groups 컬렉션에서 학번/학부/트랙별 기타 졸업요건 조회
 */
public class OtherRequirementsManageActivity extends AppCompatActivity {

    private static final String TAG = "OtherRequirementsManage";
    private static final String COLLECTION_PATH = "other_requirements_groups";

    private MaterialToolbar toolbar;
    private Spinner spinnerStudentId, spinnerDepartment, spinnerTrack;
    private MaterialButton btnSearch;
    private ProgressBar progressBar;
    private RecyclerView rvRequirements;
    private TextView tvNoResults;

    private FirebaseFirestore db;
    private OtherRequirementGroupAdapter adapter;

    // 전체 요건 그룹 데이터 (필터링에 사용)
    private List<OtherRequirementGroup> allGroups = new ArrayList<>();

    // 스피너 어댑터
    private ArrayAdapter<String> studentIdAdapter;
    private ArrayAdapter<String> departmentAdapter;
    private ArrayAdapter<String> trackAdapter;

    // 학부별 트랙 매핑
    private Map<String, Set<String>> departmentTrackMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_requirements_manage);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSpinners();
        setupListeners();

        // 초기 전체 데이터 로드
        loadAllRequirementGroups();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerStudentId = findViewById(R.id.spinner_student_id);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        btnSearch = findViewById(R.id.btn_search);
        progressBar = findViewById(R.id.progress_bar);
        rvRequirements = findViewById(R.id.rv_requirements);
        tvNoResults = findViewById(R.id.tv_no_results);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new OtherRequirementGroupAdapter(this::showEditGroupDialog);
        rvRequirements.setLayoutManager(new LinearLayoutManager(this));
        rvRequirements.setAdapter(adapter);
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
        // 학부 선택 시 트랙 업데이트
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
        btnSearch.setOnClickListener(v -> filterRequirementGroups());
    }

    /**
     * 전체 기타 졸업요건 그룹 로드
     */
    private void loadAllRequirementGroups() {
        showLoading(true);

        // 1. graduation_requirements에서 학번/학부/트랙 정보 먼저 로드
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(gradReqDocs -> {
                    departmentTrackMap.clear();
                    Set<String> years = new HashSet<>();
                    Set<String> departments = new HashSet<>();

                    for (QueryDocumentSnapshot document : gradReqDocs) {
                        String docId = document.getId();

                        // 졸업요건 문서만 확인 (문서 ID가 "졸업요건_"로 시작하는 것만)
                        if (docId.startsWith("졸업요건_")) {
                            // 실제 문서 ID 형식: "졸업요건_IT학부_멀티미디어_2020" (학부_학과_학번)
                            // 기대 형식과 다르므로 인덱스 변경
                            String[] parts = docId.split("_");
                            if (parts.length >= 4) {
                                String department = parts[1];  // 학부
                                String track = parts[2];       // 학과/트랙
                                String year = parts[3];        // 학번

                                Log.d(TAG, "파싱 - year: " + year + ", department: " + department + ", track: " + track);

                                years.add(year);
                                departments.add(department);

                                // 학부별 트랙 매핑 구축
                                if (!departmentTrackMap.containsKey(department)) {
                                    departmentTrackMap.put(department, new HashSet<>());
                                }
                                departmentTrackMap.get(department).add(track);
                            }
                        }
                    }

                    Log.d(TAG, "수집 완료 - years: " + years + ", departments: " + departments);

                    // 스피너 데이터 설정
                    updateSpinnerData(years, departments);

                    Log.d(TAG, "학번: " + years.size() + "개, 학부: " + departments.size() + "개");

                    // 2. other_requirements_groups에서 실제 요건 데이터 로드
                    loadOtherRequirementGroupsData();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "필터 데이터 로드 실패", e);
                    Toast.makeText(this, "필터 데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * other_requirements_groups 컬렉션에서 실제 요건 데이터 로드
     */
    private void loadOtherRequirementGroupsData() {
        db.collection(COLLECTION_PATH)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allGroups.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        OtherRequirementGroup group = document.toObject(OtherRequirementGroup.class);
                        group.setId(document.getId());
                        allGroups.add(group);
                    }

                    // 로딩 종료
                    showLoading(false);

                    Log.d(TAG, "전체 그룹 수: " + allGroups.size());

                    // 초기 로드 시 전체 데이터 표시
                    displayResults(allGroups);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "요건 데이터 로드 실패", e);
                    Toast.makeText(this, "요건 데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 스피너 데이터 업데이트
     */
    private void updateSpinnerData(Set<String> years, Set<String> departments) {
        // 학번 스피너 (4자리 -> 2자리 변환 후 정렬)
        List<String> yearList = new ArrayList<>();
        yearList.add("전체");
        List<String> sortedYears = new ArrayList<>();
        for (String year : years) {
            if (year != null && year.length() == 4) {
                sortedYears.add(year.substring(2)); // "2020" -> "20"
            } else if (year != null) {
                sortedYears.add(year);
            }
        }
        java.util.Collections.sort(sortedYears);
        yearList.addAll(sortedYears);

        Log.d(TAG, "학번 원본: " + years);
        Log.d(TAG, "학번 변환 후: " + yearList);

        studentIdAdapter.clear();
        studentIdAdapter.addAll(yearList);
        studentIdAdapter.notifyDataSetChanged();

        // 학부 스피너 (정렬)
        List<String> departmentList = new ArrayList<>();
        departmentList.add("전체");
        List<String> sortedDepartments = new ArrayList<>(departments);
        java.util.Collections.sort(sortedDepartments);
        departmentList.addAll(sortedDepartments);

        Log.d(TAG, "학부 원본: " + departments);
        Log.d(TAG, "학부 정렬 후: " + departmentList);

        departmentAdapter.clear();
        departmentAdapter.addAll(departmentList);
        departmentAdapter.notifyDataSetChanged();

        // 트랙 스피너는 초기에 "전체"만 표시
        trackAdapter.clear();
        trackAdapter.add("전체");
        trackAdapter.notifyDataSetChanged();

        Log.d(TAG, "스피너 데이터 업데이트 - 학번: " + yearList.size() +
                  ", 학부: " + departmentList.size());
    }

    /**
     * 선택한 학부에 따라 트랙 스피너 업데이트
     */
    private void updateTrackSpinner(String selectedDepartment) {
        trackAdapter.clear();
        trackAdapter.add("전체");

        if (!"전체".equals(selectedDepartment) && departmentTrackMap.containsKey(selectedDepartment)) {
            Set<String> tracks = departmentTrackMap.get(selectedDepartment);
            if (tracks != null) {
                List<String> sortedTracks = new ArrayList<>(tracks);
                java.util.Collections.sort(sortedTracks);
                trackAdapter.addAll(sortedTracks);
            }
        } else if ("전체".equals(selectedDepartment)) {
            // "전체" 선택 시 모든 트랙 표시
            Set<String> allTracks = new HashSet<>();
            for (Set<String> tracks : departmentTrackMap.values()) {
                allTracks.addAll(tracks);
            }
            List<String> sortedTracks = new ArrayList<>(allTracks);
            java.util.Collections.sort(sortedTracks);
            trackAdapter.addAll(sortedTracks);
        }

        trackAdapter.notifyDataSetChanged();
    }

    /**
     * 선택된 조건으로 필터링
     */
    private void filterRequirementGroups() {
        String selectedYear = spinnerStudentId.getSelectedItem() != null ?
                spinnerStudentId.getSelectedItem().toString() : "전체";
        String selectedDepartment = spinnerDepartment.getSelectedItem() != null ?
                spinnerDepartment.getSelectedItem().toString() : "전체";
        String selectedTrack = spinnerTrack.getSelectedItem() != null ?
                spinnerTrack.getSelectedItem().toString() : "전체";

        Log.d(TAG, "필터링 조건 - 학번: " + selectedYear +
                  ", 학부: " + selectedDepartment +
                  ", 트랙: " + selectedTrack);

        // 2자리 학번을 4자리로 변환 ("20" -> "2020")
        if (!"전체".equals(selectedYear) && selectedYear.length() == 2) {
            selectedYear = "20" + selectedYear;
        }

        List<OtherRequirementGroup> filtered = new ArrayList<>();

        for (OtherRequirementGroup group : allGroups) {
            boolean matches = true;

            if (!"전체".equals(selectedYear) && !selectedYear.equals(group.getStudentYear())) {
                matches = false;
            }
            if (!"전체".equals(selectedDepartment) && !selectedDepartment.equals(group.getDepartment())) {
                matches = false;
            }
            if (!"전체".equals(selectedTrack) && !selectedTrack.equals(group.getTrack())) {
                matches = false;
            }

            if (matches) {
                filtered.add(group);
            }
        }

        Log.d(TAG, "필터링 결과: " + filtered.size() + "개");
        displayResults(filtered);
    }

    /**
     * 검색 결과 표시
     */
    private void displayResults(List<OtherRequirementGroup> groups) {
        showLoading(false);

        if (groups.isEmpty()) {
            rvRequirements.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.VISIBLE);
        } else {
            rvRequirements.setVisibility(View.VISIBLE);
            tvNoResults.setVisibility(View.GONE);
            adapter.setGroups(groups);
        }
    }

    /**
     * 로딩 표시 제어
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvRequirements.setVisibility(View.GONE);
            tvNoResults.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * 그룹 편집 다이얼로그 표시
     */
    private void showEditGroupDialog(OtherRequirementGroup group, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("기타 졸업요건 편집");
        builder.setMessage(group.getGroupTitle());

        // 메인 컨테이너
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);

        // 요건 목록을 표시할 LinearLayout 생성
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // 각 요건에 대한 입력 필드 생성
        List<OtherRequirementGroup.RequirementItem> requirements = group.getRequirements();
        List<TextInputEditText> nameInputs = new ArrayList<>();
        List<Spinner> formatSpinners = new ArrayList<>();
        List<TextInputEditText> countInputs = new ArrayList<>();
        List<com.google.android.material.checkbox.MaterialCheckBox> passCheckboxes = new ArrayList<>();

        // 기존 요건 표시
        for (int i = 0; i < requirements.size(); i++) {
            OtherRequirementGroup.RequirementItem item = requirements.get(i);

            // 요건 번호 표시
            TextView tvNumber = new TextView(this);
            tvNumber.setText("요건 " + (i + 1));
            tvNumber.setTextSize(14);
            tvNumber.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvNumber.setPadding(0, i > 0 ? 30 : 10, 0, 8);
            layout.addView(tvNumber);

            // 요건 이름 입력
            com.google.android.material.textfield.TextInputLayout nameLayout =
                    new com.google.android.material.textfield.TextInputLayout(this);
            nameLayout.setHint("요건 이름");
            nameLayout.setBoxBackgroundMode(
                    com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

            TextInputEditText nameInput = new TextInputEditText(this);
            nameInput.setText(item.getName());
            nameInput.setSingleLine(true);
            nameLayout.addView(nameInput);
            layout.addView(nameLayout);
            nameInputs.add(nameInput);

            // 형식 선택
            TextView tvFormatLabel = new TextView(this);
            tvFormatLabel.setText("형식");
            tvFormatLabel.setTextSize(12);
            LinearLayout.LayoutParams formatLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            formatLabelParams.topMargin = 16;
            tvFormatLabel.setLayoutParams(formatLabelParams);
            layout.addView(tvFormatLabel);

            Spinner formatSpinner = new Spinner(this);
            String[] formats = {"횟수", "통과"};
            ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, formats);
            formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            formatSpinner.setAdapter(formatAdapter);
            formatSpinner.setSelection("통과".equals(item.getFormat()) ? 1 : 0);
            layout.addView(formatSpinner);
            formatSpinners.add(formatSpinner);

            // 횟수 입력 (조건부)
            com.google.android.material.textfield.TextInputLayout countLayout =
                    new com.google.android.material.textfield.TextInputLayout(this);
            countLayout.setHint("횟수");
            countLayout.setBoxBackgroundMode(
                    com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
            LinearLayout.LayoutParams countLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            countLayoutParams.topMargin = 16;
            countLayout.setLayoutParams(countLayoutParams);

            TextInputEditText countInput = new TextInputEditText(this);
            countInput.setSingleLine(true);
            countInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            if ("횟수".equals(item.getFormat())) {
                countInput.setText(String.valueOf(item.getCount()));
                countLayout.setVisibility(View.VISIBLE);
            } else {
                countLayout.setVisibility(View.GONE);
            }
            countLayout.addView(countInput);
            layout.addView(countLayout);
            countInputs.add(countInput);

            // 통과 여부는 자동으로 true로 설정되므로 체크박스 불필요
            com.google.android.material.checkbox.MaterialCheckBox passCheckbox =
                    new com.google.android.material.checkbox.MaterialCheckBox(this);
            passCheckbox.setChecked(true);
            passCheckbox.setVisibility(View.GONE);
            passCheckboxes.add(passCheckbox);

            // 형식 변경 시 입력 필드 변경
            final int index = i;
            formatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) { // 횟수
                        countLayout.setVisibility(View.VISIBLE);
                    } else { // 통과
                        countLayout.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // 스크롤 가능하도록 ScrollView로 감싸기
        androidx.core.widget.NestedScrollView scrollView =
                new androidx.core.widget.NestedScrollView(this);

        // ScrollView 높이 제한 (화면의 60% 정도)
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0);
        scrollParams.weight = 1;
        scrollView.setLayoutParams(scrollParams);
        scrollView.addView(layout);

        mainContainer.addView(scrollView);

        // 새 요건 추가 버튼 - 크기 증가
        MaterialButton btnAddRequirement = new MaterialButton(this);
        btnAddRequirement.setText("+ 새 요건 추가");
        btnAddRequirement.setIcon(getDrawable(android.R.drawable.ic_menu_add));
        btnAddRequirement.setTextSize(16);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(50, 30, 50, 30);
        btnParams.height = 150; // 버튼 높이 증가
        btnAddRequirement.setLayoutParams(btnParams);
        btnAddRequirement.setOnClickListener(v -> {
            showAddRequirementDialog(nameInputs, formatSpinners, countInputs, passCheckboxes, layout, scrollView);
        });

        mainContainer.addView(btnAddRequirement);

        // 저장 버튼 추가
        MaterialButton btnSave = new MaterialButton(this);
        btnSave.setText("저장");
        btnSave.setTextSize(16);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(50, 10, 50, 30);
        saveParams.height = 120;
        btnSave.setLayoutParams(saveParams);
        mainContainer.addView(btnSave);

        builder.setView(mainContainer);

        AlertDialog alertDialog = builder.create();

        // 저장 버튼 클릭 리스너
        btnSave.setOnClickListener(v -> {
            // 수정된 요건 목록 생성
            List<OtherRequirementGroup.RequirementItem> updatedRequirements = new ArrayList<>();
            for (int i = 0; i < nameInputs.size(); i++) {
                String name = nameInputs.get(i).getText().toString().trim();
                String format = formatSpinners.get(i).getSelectedItem().toString();
                int count = 0;
                boolean isPass = false;

                if ("횟수".equals(format)) {
                    String countStr = countInputs.get(i).getText().toString().trim();
                    count = countStr.isEmpty() ? 0 : Integer.parseInt(countStr);
                } else {
                    // 통과 형식은 자동으로 true
                    isPass = true;
                }

                if (!name.isEmpty()) {
                    updatedRequirements.add(new OtherRequirementGroup.RequirementItem(
                            name, format, count, isPass));
                }
            }

            if (updatedRequirements.isEmpty()) {
                Toast.makeText(this, "최소 1개 이상의 요건이 필요합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firestore 업데이트
            group.setRequirements(updatedRequirements);
            group.setTimestamp(System.currentTimeMillis());

            db.collection(COLLECTION_PATH).document(group.getId())
                    .set(group)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();

                        // allGroups 리스트 업데이트
                        for (int i = 0; i < allGroups.size(); i++) {
                            if (allGroups.get(i).getId().equals(group.getId())) {
                                allGroups.set(i, group);
                                break;
                            }
                        }

                        // RecyclerView 업데이트
                        adapter.notifyItemChanged(position);

                        // 다이얼로그 닫기
                        alertDialog.dismiss();

                        Log.d(TAG, "요건 그룹 수정 성공: " + group.getId());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "요건 그룹 수정 실패", e);
                    });
        });

        // 삭제 버튼
        builder.setNeutralButton("삭제", (dialog, which) -> {
            new AlertDialog.Builder(this)
                    .setTitle("삭제 확인")
                    .setMessage("정말로 이 졸업요건 그룹을 삭제하시겠습니까?\n\n" + group.getGroupTitle())
                    .setPositiveButton("삭제", (d, w) -> deleteGroup(group, position))
                    .setNegativeButton("취소", null)
                    .show();
        });

        alertDialog.show();
    }

    /**
     * 그룹 삭제
     */
    private void deleteGroup(OtherRequirementGroup group, int position) {
        db.collection(COLLECTION_PATH).document(group.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show();
                    // 데이터 다시 로드
                    filterRequirementGroups();
                    Log.d(TAG, "요건 그룹 삭제 성공: " + group.getId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "요건 그룹 삭제 실패", e);
                });
    }

    /**
     * 새 요건 추가 다이얼로그
     */
    private void showAddRequirementDialog(List<TextInputEditText> nameInputs,
                                          List<Spinner> formatSpinners,
                                          List<TextInputEditText> countInputs,
                                          List<com.google.android.material.checkbox.MaterialCheckBox> passCheckboxes,
                                          LinearLayout parentLayout,
                                          androidx.core.widget.NestedScrollView scrollView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 요건 추가");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // 요건 이름 입력
        TextView tvNameLabel = new TextView(this);
        tvNameLabel.setText("요건 이름");
        tvNameLabel.setTextSize(14);
        tvNameLabel.setPadding(0, 0, 0, 8);
        layout.addView(tvNameLabel);

        com.google.android.material.textfield.TextInputLayout nameLayout =
                new com.google.android.material.textfield.TextInputLayout(this);
        nameLayout.setHint("예: TLC, 채플, 봉사활동 등");
        nameLayout.setBoxBackgroundMode(
                com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

        TextInputEditText etName = new TextInputEditText(this);
        etName.setSingleLine(true);
        nameLayout.addView(etName);
        layout.addView(nameLayout);

        // 형식 선택
        TextView tvFormatLabel = new TextView(this);
        tvFormatLabel.setText("형식");
        tvFormatLabel.setTextSize(14);
        tvFormatLabel.setPadding(0, 30, 0, 8);
        layout.addView(tvFormatLabel);

        Spinner spinnerFormat = new Spinner(this);
        String[] formats = {"횟수", "통과"};
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, formats);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFormat.setAdapter(formatAdapter);
        layout.addView(spinnerFormat);

        // 횟수 입력 (조건부)
        TextView tvCountLabel = new TextView(this);
        tvCountLabel.setText("횟수");
        tvCountLabel.setTextSize(14);
        tvCountLabel.setPadding(0, 30, 0, 8);
        layout.addView(tvCountLabel);

        com.google.android.material.textfield.TextInputLayout countLayout =
                new com.google.android.material.textfield.TextInputLayout(this);
        countLayout.setHint("몇 회인지 입력");
        countLayout.setBoxBackgroundMode(
                com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

        TextInputEditText etCount = new TextInputEditText(this);
        etCount.setSingleLine(true);
        etCount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        countLayout.addView(etCount);
        layout.addView(countLayout);

        // 형식 선택에 따라 입력 필드 변경
        spinnerFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // 횟수
                    tvCountLabel.setVisibility(View.VISIBLE);
                    countLayout.setVisibility(View.VISIBLE);
                } else { // 통과 - 아무 입력도 필요 없음
                    tvCountLabel.setVisibility(View.GONE);
                    countLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setView(layout);

        builder.setPositiveButton("추가", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "요건 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            String format = spinnerFormat.getSelectedItem().toString();
            int count = 0;
            boolean isPass = false;

            if ("횟수".equals(format)) {
                String countStr = etCount.getText().toString().trim();
                if (countStr.isEmpty()) {
                    Toast.makeText(this, "횟수를 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                count = Integer.parseInt(countStr);
            } else {
                // 통과 형식은 자동으로 true
                isPass = true;
            }

            // 새 입력 필드 추가
            int index = nameInputs.size();

            // 요건 번호 표시
            TextView tvNumber = new TextView(this);
            tvNumber.setText("요건 " + (index + 1));
            tvNumber.setTextSize(14);
            tvNumber.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvNumber.setPadding(0, 30, 0, 8);
            parentLayout.addView(tvNumber);

            // 요건 이름 입력
            com.google.android.material.textfield.TextInputLayout newNameLayout =
                    new com.google.android.material.textfield.TextInputLayout(this);
            newNameLayout.setHint("요건 이름");
            newNameLayout.setBoxBackgroundMode(
                    com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

            TextInputEditText newNameInput = new TextInputEditText(this);
            newNameInput.setText(name);
            newNameInput.setSingleLine(true);
            newNameLayout.addView(newNameInput);
            parentLayout.addView(newNameLayout);
            nameInputs.add(newNameInput);

            // 형식 선택 Spinner
            TextView tvNewFormatLabel = new TextView(this);
            tvNewFormatLabel.setText("형식");
            tvNewFormatLabel.setTextSize(12);
            LinearLayout.LayoutParams newFormatLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            newFormatLabelParams.topMargin = 16;
            tvNewFormatLabel.setLayoutParams(newFormatLabelParams);
            parentLayout.addView(tvNewFormatLabel);

            Spinner newFormatSpinner = new Spinner(this);
            String[] newFormats = {"횟수", "통과"};
            ArrayAdapter<String> newFormatAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, newFormats);
            newFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            newFormatSpinner.setAdapter(newFormatAdapter);
            newFormatSpinner.setSelection("통과".equals(format) ? 1 : 0);
            parentLayout.addView(newFormatSpinner);
            formatSpinners.add(newFormatSpinner);

            // 횟수 입력 (조건부)
            com.google.android.material.textfield.TextInputLayout newCountLayout =
                    new com.google.android.material.textfield.TextInputLayout(this);
            newCountLayout.setHint("횟수");
            newCountLayout.setBoxBackgroundMode(
                    com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
            LinearLayout.LayoutParams countLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            countLayoutParams.topMargin = 16;
            newCountLayout.setLayoutParams(countLayoutParams);

            TextInputEditText newCountInput = new TextInputEditText(this);
            newCountInput.setSingleLine(true);
            newCountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            if ("횟수".equals(format)) {
                newCountInput.setText(String.valueOf(count));
                newCountLayout.setVisibility(View.VISIBLE);
            } else {
                newCountLayout.setVisibility(View.GONE);
            }
            newCountLayout.addView(newCountInput);
            parentLayout.addView(newCountLayout);
            countInputs.add(newCountInput);

            // 통과 여부는 자동으로 true로 설정되므로 체크박스 불필요
            com.google.android.material.checkbox.MaterialCheckBox newPassCheckbox =
                    new com.google.android.material.checkbox.MaterialCheckBox(this);
            newPassCheckbox.setChecked(true);
            newPassCheckbox.setVisibility(View.GONE);
            passCheckboxes.add(newPassCheckbox);

            // 형식 변경 시 입력 필드 변경
            newFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) { // 횟수
                        newCountLayout.setVisibility(View.VISIBLE);
                    } else { // 통과
                        newCountLayout.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // 스크롤을 맨 아래로 이동
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

            Toast.makeText(this, "요건 추가됨: " + name, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

}
