package sprout.app.sakmvp1.Login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import sprout.app.sakmvp1.R; // <- applicationId 기준으로 생성된 R 클래스(리소스 접근용)
import sprout.app.sakmvp1.BaseActivity; // <- 색약 모드 지원을 위해 추가
import sprout.app.sakmvp1.MainActivityNew;
import sprout.app.sakmvp1.AdminActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 로그인 화면 액티비티
 *
 * 기능 요약
 * - 이메일/비밀번호 입력 → Firebase Authentication으로 로그인 시도
 * - "자동 로그인" 체크 시 SharedPreferences에 사용자 선택 저장
 * - 앱 실행 시 자동 로그인 설정 + 이미 인증된 사용자(FirebaseAuth.getCurrentUser()!=null)면
 *   로그인 화면을 건너뛰고 메인으로 이동
 * - 회원가입 화면으로 이동 버튼
 * - 테스트용(비로그인)으로 메인 진입 버튼(개발/시연 용도)
 *
 * 주의
 * - 실제 배포 시 테스트용 진입 버튼은 제거하거나 디버그 빌드에서만 노출하는 것을 권장
 * - 이메일 인증 여부, 비밀번호 최소 길이/강도 등의 추가 검증은 별도 정책에 맞게 확장 가능
 *
 * BaseActivity 상속:
 * - BaseActivity를 상속받아 색약 모드(흑백 필터)를 자동으로 지원합니다
 * - 사용자가 설정한 접근성 모드가 로그인 화면에서도 적용됩니다
 */
public class LoginActivity extends BaseActivity {

    // UI 참조
    private EditText etEmail;
    private EditText etPassword;
    private CheckBox cbAutoLogin;
    private Button btnLogin;
    private Button btnSignUp;
    private Button btnTestAccess;

    // Firebase 인증 진입점
    private FirebaseAuth mAuth;

    // 자동 로그인 설정 저장용 SharedPreferences 키
    private static final String PREFS_NAME = "user_prefs";     // 프리퍼런스 파일명
    private static final String KEY_AUTO_LOGIN = "auto_login"; // 자동 로그인 여부 저장 키

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 시스템 바(상태바/내비게이션 바) 영역까지 컨텐츠가 확장되도록 설정
        // (AndroidX EdgeToEdge API 사용: 투명 시스템 바 등 최신 디자인 가이드 대응)
        EdgeToEdge.enable(this);

        // 레이아웃 바인딩
        setContentView(R.layout.activity_login);

        // 시스템 UI 인셋을 고려해 루트 뷰 패딩을 동적으로 적용
        // (상단 노치/상태바, 하단 제스처 영역 등에 컨텐츠가 가려지지 않게 처리)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase 인증 인스턴스 획득
        mAuth = FirebaseAuth.getInstance();

        // 레이아웃 내 위젯 참조
        etEmail = findViewById(R.id.etUserid);     // 이메일 입력란
        etPassword = findViewById(R.id.etPassword); // 비밀번호 입력란
        cbAutoLogin = findViewById(R.id.cbLoginauto); // 자동 로그인 체크박스
        btnLogin = findViewById(R.id.btnLogin);       // 로그인 버튼
        btnSignUp = findViewById(R.id.btnSignUp);     // 회원가입 이동 버튼
        btnTestAccess = findViewById(R.id.btnTestAccess); // (개발용) 로그인 없이 메인 이동 버튼

        // 자동 로그인 설정 확인
        // - 사용자가 과거에 자동 로그인을 켰는지 여부를 불러옴
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean autoLogin = prefs.getBoolean(KEY_AUTO_LOGIN, false);

        // 자동 로그인 ON 이고, Firebase에 이미 인증된 사용자가 남아있으면(앱 재실행 등)
        // 로그인 화면을 건너뛰고 바로 메인으로 진입
        // - getCurrentUser()!=null 은 토큰이 유효하고 세션이 남아있음을 의미
        if (autoLogin && mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivityNew.class));
            finish(); // 로그인 화면을 백스택에서 제거
        }

        // [로그인] 버튼 클릭 → 입력값 검증 후 Firebase 로그인 시도
        btnLogin.setOnClickListener(v -> loginUser());

        // [회원가입] 버튼 클릭 → 같은 패키지 내 SignUpActivity로 전환
        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        // [테스트 접속] 버튼 클릭 → 일반/관리자 선택 다이얼로그 표시
        // - 시연/디자인 점검 용도
        // - finish()를 호출하지 않아 뒤로가기 시 로그인 화면으로 복귀 가능
        btnTestAccess.setOnClickListener(v -> showTestAccessDialog());
    }

    /**
     * 테스트 접속 다이얼로그 표시
     * 일반 접속 또는 관리자 접속을 선택할 수 있음
     */
    private void showTestAccessDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_test_access, null);
        builder.setView(dialogView);

        com.google.android.material.button.MaterialButton btnNormalAccess = dialogView.findViewById(R.id.btn_normal_access);
        com.google.android.material.button.MaterialButton btnAdminAccess = dialogView.findViewById(R.id.btn_admin_access);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // 일반 접속 (테스트용 - Firebase 인증 없이 바로 접속)
        btnNormalAccess.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("is_admin", false)
                    .apply();

            Toast.makeText(LoginActivity.this, "일반 사용자로 테스트 접속합니다", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivityNew.class));
            dialog.dismiss();
        });

        // 관리자 접속 (테스트용 - Firebase 인증 없이 바로 최고 권한 부여)
        btnAdminAccess.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("is_admin", true)
                    .apply();

            Toast.makeText(LoginActivity.this, "관리자로 테스트 접속합니다 (최고 권한)", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
            dialog.dismiss();
        });

        // 취소
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * 이메일/비밀번호로 Firebase 인증 시도
     * 1) 공란 검증
     * 2) 이메일로 관리자 여부 자동 판단
     * 3) FirebaseAuth.signInWithEmailAndPassword 호출
     * 4) 성공 시 자동 로그인 설정 저장 + 메인으로 이동
     * 5) 실패 시 예외 타입에 따라 사용자 친화적 메세지 노출
     */
    private void loginUser() {
        // 양끝 공백 제거(사용자 입력 실수 방지)
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 최소한의 유효성 검사(공란 체크)
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 이메일로 관리자 여부 자동 판단
        boolean isAdmin = isAdminEmail(email);

        // Firebase 이메일/비밀번호 로그인
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 인증 성공
                        String loginType = isAdmin ? "관리자 로그인 성공" : "로그인 성공";
                        Toast.makeText(LoginActivity.this, loginType, Toast.LENGTH_SHORT).show();

                        // 자동 로그인 설정을 사용자 선택(체크박스)대로 저장
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean(KEY_AUTO_LOGIN, cbAutoLogin.isChecked())
                                .putBoolean("is_admin", isAdmin)
                                .apply();

                        // Firestore에서 접근성 설정 로드 후 메인 화면으로 이동
                        loadAccessibilitySettingsAndNavigate();
                    } else {
                        // 인증 실패: 예외 유형에 따라 구체 메시지 제공
                        Exception exception = task.getException();

                        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(LoginActivity.this, "잘못된 비밀번호입니다.", Toast.LENGTH_SHORT).show();
                        } else if (exception instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(LoginActivity.this, "존재하지 않는 이메일입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(
                                    LoginActivity.this,
                                    "로그인 실패: " + (exception != null ? exception.getMessage() : "알 수 없는 오류"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });
    }

    /**
     * 이메일로 관리자 여부 판단
     * 특정 도메인(@admin.nu.ac.kr) 또는 화이트리스트에 등록된 이메일인지 확인
     * 실제 배포 시에는 Firebase Firestore나 Realtime Database에서 관리자 목록을 관리하는 것을 권장
     */
    private boolean isAdminEmail(String email) {
        // 관리자 도메인 체크
        if (email.endsWith("@admin.nu.ac.kr")) {
            return true;
        }

        // 관리자 이메일 화이트리스트
        String[] adminEmails = {
            "admin@nu.ac.kr",
            "manager@nu.ac.kr"
        };

        for (String adminEmail : adminEmails) {
            if (email.equals(adminEmail)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Firestore에서 접근성 설정을 로드한 후 메인 화면으로 이동
     *
     * 로그인 성공 후 호출되어 사용자의 색약 모드 설정을 불러옵니다.
     * 학적정보가 없는 경우 입력을 권유하는 Dialog를 표시합니다.
     * 로컬 SharedPreferences에 저장하여 앱 전체에서 사용할 수 있도록 합니다.
     */
    private void loadAccessibilitySettingsAndNavigate() {
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Firestore에서 사용자 문서 조회
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // color_blind_mode 필드 읽기
                        Boolean colorBlindMode = documentSnapshot.getBoolean("color_blind_mode");
                        if (colorBlindMode == null) {
                            colorBlindMode = false;
                        }

                        // 로컬 SharedPreferences에 저장
                        SharedPreferences prefs = getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("color_blind_mode", colorBlindMode)
                                .apply();

                        Log.d("LoginActivity", "접근성 설정 로드 완료: color_blind_mode = " + colorBlindMode);

                        // 학적정보 확인
                        String name = documentSnapshot.getString("name");
                        String studentYear = documentSnapshot.getString("studentYear");
                        String department = documentSnapshot.getString("department");
                        String track = documentSnapshot.getString("track");

                        boolean hasAcademicInfo = (name != null && !name.isEmpty()) &&
                                                  (studentYear != null && !studentYear.isEmpty()) &&
                                                  (department != null && !department.isEmpty()) &&
                                                  (track != null && !track.isEmpty());

                        if (!hasAcademicInfo) {
                            // 학적정보가 없으면 입력 권유 Dialog 표시
                            showAcademicInfoDialog();
                        } else {
                            // 학적정보가 있으면 바로 메인 화면으로 이동
                            navigateToMain();
                        }
                    } else {
                        // 문서가 없으면 학적정보 입력 권유
                        showAcademicInfoDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    // 로드 실패 시에도 메인 화면으로 이동 (기본값 사용)
                    Log.w("LoginActivity", "접근성 설정 로드 실패 - 기본값 사용", e);
                    navigateToMain();
                });
    }

    /**
     * 학적정보 입력을 권유하는 Dialog 표시
     */
    private void showAcademicInfoDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("학적정보 입력")
                .setMessage("졸업 요건 분석과 수강과목 추천 기능을 사용하려면\n학적정보(이름, 학번, 학부, 트랙)를 입력해야 합니다.\n\n지금 입력하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("입력하기", (dialog, which) -> {
                    // UserInfoActivity로 이동 (로그인 직후 플래그 전달)
                    Intent intent = new Intent(LoginActivity.this, sprout.app.sakmvp1.UserInfoActivity.class);
                    intent.putExtra("from_login", true);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("나중에", (dialog, which) -> {
                    // 메인 화면으로 이동
                    navigateToMain();
                })
                .show();
    }

    /**
     * 메인 화면으로 이동
     */
    private void navigateToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivityNew.class));
        finish();
    }
}
