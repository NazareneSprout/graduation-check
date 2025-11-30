package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 학적 정보 입력/수정 화면
 *
 * 학번, 학부, 트랙 정보를 입력받아 Firebase에 저장합니다.
 * 기능1(졸업 요건 분석)과 기능3(수강과목 추천)에서 이 정보를 사용합니다.
 */
public class UserInfoActivity extends BaseActivity {

    private static final String TAG = "UserInfoActivity";

    // UI 컴포넌트
    private MaterialToolbar toolbar;
    private Spinner spinnerStudentId;
    private Spinner spinnerDepartment;
    private Spinner spinnerTrack;
    private Button btnSaveInfo;
    private FrameLayout loadingContainer;
    private ProgressBar progressBar;
    private TextView loadingText;

    // Data
    private FirebaseDataManager dataManager;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // 데이터 로딩 상태
    private boolean studentYearsLoaded = false;
    private boolean departmentsLoaded = false;
    private boolean allTracksLoaded = false;
    private List<String> loadedStudentYears = null;
    private List<String> loadedDepartments = null;
    private Map<String, List<String>> allTracksData = null;

    // 스피너 어댑터
    private ArrayAdapter<String> studentIdAdapter;
    private ArrayAdapter<String> departmentAdapter;
    private ArrayAdapter<String> trackAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        initViews();
        initFirebase();
        loadExistingUserInfo();
        loadInitialData();
        setupListeners();
    }

    private void initViews() {
        // Toolbar 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Views
        spinnerStudentId = findViewById(R.id.spinnerStudentId);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerTrack = findViewById(R.id.spinnerTrack);
        btnSaveInfo = findViewById(R.id.btnSaveInfo);
        loadingContainer = findViewById(R.id.loadingContainer);
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);

        // 어댑터 초기화
        studentIdAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        studentIdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStudentId.setAdapter(studentIdAdapter);

        departmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        trackAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTrack.setAdapter(trackAdapter);
    }

    private void initFirebase() {
        dataManager = FirebaseDataManager.getInstance();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * 기존 저장된 사용자 정보 불러오기
     */
    private void loadExistingUserInfo() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.w(TAG, "사용자가 로그인하지 않음");
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String savedYear = documentSnapshot.getString("studentYear");
                        String savedDepartment = documentSnapshot.getString("department");
                        String savedTrack = documentSnapshot.getString("track");

                        Log.d(TAG, "저장된 정보 불러옴: " + savedYear + "/" + savedDepartment + "/" + savedTrack);

                        // TODO: 불러온 정보를 스피너에 설정 (데이터 로딩 완료 후)
                    } else {
                        Log.d(TAG, "저장된 사용자 정보 없음");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "사용자 정보 불러오기 실패", e);
                });
    }

    /**
     * 초기 데이터 로딩 (학번/학부/트랙)
     */
    private void loadInitialData() {
        showLoading("데이터를 불러오는 중입니다...");

        // 병렬로 데이터 로딩
        loadStudentYears();
        loadDepartments();
        loadAllTracks();
    }

    private void loadStudentYears() {
        dataManager.loadStudentYears(new FirebaseDataManager.OnStudentYearsLoadedListener() {
            @Override
            public void onSuccess(List<String> years) {
                loadedStudentYears = years;
                studentYearsLoaded = true;
                updateStudentYearSpinner(years);
                checkAllDataLoaded();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "학번 로딩 실패", e);
                Toast.makeText(UserInfoActivity.this, "학번 데이터 로딩 실패", Toast.LENGTH_SHORT).show();
                checkAllDataLoaded();
            }
        });
    }

    private void loadDepartments() {
        dataManager.loadDepartments(new FirebaseDataManager.OnDepartmentsLoadedListener() {
            @Override
            public void onSuccess(List<String> departments) {
                loadedDepartments = departments;
                departmentsLoaded = true;
                updateDepartmentSpinner(departments);
                checkAllDataLoaded();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "학부 로딩 실패", e);
                Toast.makeText(UserInfoActivity.this, "학부 데이터 로딩 실패", Toast.LENGTH_SHORT).show();
                checkAllDataLoaded();
            }
        });
    }

    private void loadAllTracks() {
        dataManager.loadAllTracks(new FirebaseDataManager.OnAllTracksLoadedListener() {
            @Override
            public void onSuccess(Map<String, List<String>> tracksMap) {
                allTracksData = tracksMap;
                allTracksLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "트랙 로딩 실패", e);
                Toast.makeText(UserInfoActivity.this, "트랙 데이터 로딩 실패", Toast.LENGTH_SHORT).show();
                checkAllDataLoaded();
            }
        });
    }

    private void checkAllDataLoaded() {
        if (studentYearsLoaded && departmentsLoaded && allTracksLoaded) {
            hideLoading();
            Log.d(TAG, "모든 초기 데이터 로딩 완료");
        }
    }

    private void updateStudentYearSpinner(List<String> years) {
        studentIdAdapter.clear();
        for (String year : years) {
            // 4자리 연도를 2자리로 변환 (예: "2025" -> "25")
            String displayYear = year.substring(2);
            studentIdAdapter.add(displayYear);
        }
        studentIdAdapter.notifyDataSetChanged();
    }

    private void updateDepartmentSpinner(List<String> departments) {
        departmentAdapter.clear();
        departmentAdapter.addAll(departments);
        departmentAdapter.notifyDataSetChanged();
    }

    private void updateTrackSpinner(String department) {
        trackAdapter.clear();
        if (allTracksData != null && allTracksData.containsKey(department)) {
            List<String> tracks = allTracksData.get(department);
            if (tracks != null && !tracks.isEmpty()) {
                trackAdapter.addAll(tracks);
            }
        }
        trackAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        // 학부 선택 시 트랙 업데이트
        spinnerDepartment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedDepartment = departmentAdapter.getItem(position);
                if (selectedDepartment != null) {
                    updateTrackSpinner(selectedDepartment);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // 저장 버튼
        btnSaveInfo.setOnClickListener(v -> saveUserInfo());
    }

    /**
     * 사용자 정보를 Firebase에 저장
     */
    private void saveUserInfo() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String userEmail = auth.getCurrentUser().getEmail(); // Firebase Auth에서 email 가져오기

        // 선택된 값 가져오기
        String selectedYearDisplay = (String) spinnerStudentId.getSelectedItem();
        String selectedDepartment = (String) spinnerDepartment.getSelectedItem();
        String selectedTrack = (String) spinnerTrack.getSelectedItem();

        if (selectedYearDisplay == null || selectedDepartment == null || selectedTrack == null) {
            Toast.makeText(this, "모든 정보를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2자리 연도를 4자리로 변환 (예: "25" -> "2025")
        String selectedYear = "20" + selectedYearDisplay;

        showLoading("저장 중...");

        // Firebase에 저장 (기존 필드를 유지하면서 학적 정보 + email 업데이트)
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("studentYear", selectedYear);
        userInfo.put("department", selectedDepartment);
        userInfo.put("track", selectedTrack);
        userInfo.put("updatedAt", System.currentTimeMillis());

        // email 복구 (Firebase Auth의 email을 Firestore에 저장)
        if (userEmail != null && !userEmail.isEmpty()) {
            userInfo.put("email", userEmail);
        }

        db.collection("users").document(userId)
                .set(userInfo, com.google.firebase.firestore.SetOptions.merge())  // merge 옵션으로 기존 필드 유지
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "사용자 정보 저장 완료: " + selectedYear + "/" + selectedDepartment + "/" + selectedTrack);

                    // 로그인 직후 UserInfoActivity로 온 경우 메인 화면으로 이동
                    boolean fromLogin = getIntent().getBooleanExtra("from_login", false);
                    if (fromLogin) {
                        Intent intent = new Intent(this, MainActivityNew.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }

                    finish();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "저장에 실패했습니다", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "사용자 정보 저장 실패", e);
                });
    }

    private void showLoading(String message) {
        loadingContainer.setVisibility(View.VISIBLE);
        loadingText.setText(message);
        btnSaveInfo.setEnabled(false);
    }

    private void hideLoading() {
        loadingContainer.setVisibility(View.GONE);
        btnSaveInfo.setEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
