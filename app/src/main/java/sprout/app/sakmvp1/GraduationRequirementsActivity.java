package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerStudentId = findViewById(R.id.spinner_student_id);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerTrack = findViewById(R.id.spinner_track);
        btnSearch = findViewById(R.id.btn_search);
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
            // 상세보기 Activity로 이동
            Intent intent = new Intent(this, GraduationRequirementDetailActivity.class);
            intent.putExtra(GraduationRequirementDetailActivity.EXTRA_DOCUMENT_ID, requirement.getId());
            intent.putExtra(GraduationRequirementDetailActivity.EXTRA_YEAR, requirement.getYear());
            intent.putExtra(GraduationRequirementDetailActivity.EXTRA_DEPARTMENT, requirement.getDepartment());
            intent.putExtra(GraduationRequirementDetailActivity.EXTRA_TRACK, requirement.getTrack());
            startActivity(intent);
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

        // 추가 버튼
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, GraduationRequirementAddActivity.class);
            startActivity(intent);
        });
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
                    Set<String> years = new HashSet<>();
                    Set<String> departments = new HashSet<>();
                    Set<String> tracks = new HashSet<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // 교양 문서는 제외
                        if (document.getId().startsWith("교양_")) {
                            continue;
                        }

                        // Firestore 문서에서 직접 필드 읽어서 객체 생성
                        GraduationRequirement requirement = new GraduationRequirement();
                        requirement.setId(document.getId()); // 문서 ID 설정하면 자동으로 year, department, track 파싱됨

                        // 학점 정보 가져오기 (Firestore는 한글 필드명 사용)
                        int totalCredits = GraduationRequirementUtils.getIntValue(document, "totalCredits", 130);
                        int majorRequired = GraduationRequirementUtils.getIntValue(document, "전공필수", 0);
                        int majorElective = GraduationRequirementUtils.getIntValue(document, "전공선택", 0);
                        int majorAdvanced = GraduationRequirementUtils.getIntValue(document, "전공심화", 0);
                        int generalRequired = GraduationRequirementUtils.getIntValue(document, "교양필수", 0);
                        int generalElective = GraduationRequirementUtils.getIntValue(document, "교양선택", 0);
                        int liberalArts = GraduationRequirementUtils.getIntValue(document, "소양", 0);
                        int freeElective = GraduationRequirementUtils.getIntValue(document, "자율선택", 0);
                        int departmentCommon = GraduationRequirementUtils.getIntValue(document, "학부공통", 0);
                        int remainingCredits = GraduationRequirementUtils.getIntValue(document, "잔여학점", 0);

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

                        Log.d(TAG, "문서 " + document.getId() + " 로드: 총=" + totalCredits
                            + ", 전필=" + majorRequired + ", 전선=" + majorElective
                            + ", 교필=" + generalRequired + ", 교선=" + generalElective
                            + ", 소양=" + liberalArts + ", 자율=" + freeElective
                            + ", 전심=" + majorAdvanced + ", 학공=" + departmentCommon
                            + ", 잔여=" + remainingCredits);

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

                    // 초기에는 결과 표시 안 함
                    showLoading(false);
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
        trackAdapter.clear();
        trackAdapter.add("전체");

        if (!"전체".equals(selectedDepartment) && departmentTrackMap.containsKey(selectedDepartment)) {
            Set<String> tracks = departmentTrackMap.get(selectedDepartment);
            if (tracks != null) {
                trackAdapter.addAll(new ArrayList<>(tracks));
            }
        } else if ("전체".equals(selectedDepartment)) {
            // "전체" 선택 시 모든 트랙 표시
            Set<String> allTracks = new HashSet<>();
            for (Set<String> tracks : departmentTrackMap.values()) {
                allTracks.addAll(tracks);
            }
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

}
