package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 사용자 프로필 화면
 *
 * 사용자의 계정 정보와 학적 정보를 표시합니다.
 */
public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfile";

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvStudentYear;
    private TextView tvDepartment;
    private TextView tvTrack;
    private TextView tvNoUserInfo;
    private LinearLayout btnEditUserInfo;
    private LinearLayout btnLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // 시스템 바 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        initFirebase();
        loadUserData();
        setupListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // 네비게이션 바에 시스템 네비게이션 바 높이만큼 하단 패딩 추가
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navigationBars.bottom);
            return insets;
        });

        // TextViews
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvStudentYear = findViewById(R.id.tvStudentYear);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvTrack = findViewById(R.id.tvTrack);
        tvNoUserInfo = findViewById(R.id.tvNoUserInfo);

        // Buttons
        btnEditUserInfo = findViewById(R.id.btnEditUserInfo);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 계정 정보 표시
        String displayName = user.getDisplayName();
        String email = user.getEmail();

        tvUserName.setText(displayName != null ? displayName : "사용자");
        tvUserEmail.setText(email != null ? email : "");

        // 학적 정보 불러오기
        loadAcademicInfo(user.getUid());
    }

    private void loadAcademicInfo(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() &&
                        documentSnapshot.contains("studentYear") &&
                        documentSnapshot.contains("department") &&
                        documentSnapshot.contains("track")) {

                        String year = documentSnapshot.getString("studentYear");
                        String department = documentSnapshot.getString("department");
                        String track = documentSnapshot.getString("track");

                        // 학적 정보 표시
                        showAcademicInfo(year, department, track);
                    } else {
                        // 학적 정보 없음
                        showNoAcademicInfo();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학적 정보 로딩 실패", e);
                    showNoAcademicInfo();
                });
    }

    private void showAcademicInfo(String year, String department, String track) {
        tvNoUserInfo.setVisibility(View.GONE);
        tvStudentYear.setVisibility(View.VISIBLE);
        tvDepartment.setVisibility(View.VISIBLE);
        tvTrack.setVisibility(View.VISIBLE);

        // 4자리 연도를 2자리로 변환 (예: "2025" -> "25학번")
        String displayYear = year.length() >= 4 ? year.substring(2) + "학번" : year + "학번";
        tvStudentYear.setText(displayYear);
        tvDepartment.setText(department);
        tvTrack.setText(track);
    }

    private void showNoAcademicInfo() {
        tvNoUserInfo.setVisibility(View.VISIBLE);
        tvStudentYear.setVisibility(View.GONE);
        tvDepartment.setVisibility(View.GONE);
        tvTrack.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // 학적정보 수정
        btnEditUserInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserInfoActivity.class);
            startActivity(intent);
        });

        // 로그아웃
        btnLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> {
                    auth.signOut();
                    Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

                    // 로그인 화면으로 이동
                    Intent intent = new Intent(this, sprout.app.sakmvp1.Login.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            // 내 정보 버튼 활성화
            bottomNavigation.setSelectedItemId(R.id.nav_button_4);

            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_button_1) {
                    // 홈 화면으로 이동
                    Intent intent = new Intent(this, MainActivityNew.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_button_2) {
                    // 시간표 화면으로 이동 - MainActivityNew의 시간표 탭
                    Intent intent = new Intent(this, MainActivityNew.class);
                    intent.putExtra("selected_tab", "timetable");
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_button_3) {
                    Toast.makeText(this, "버튼3 - 준비중", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_button_4) {
                    // 이미 내 정보 화면이므로 아무 동작 안 함
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면 재진입 시 학적 정보 새로고침
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            loadAcademicInfo(user.getUid());
        }

        // 내 정보 화면으로 돌아올 때 네비게이션 상태 초기화
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_4);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
