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
import sprout.app.sakmvp1.MainActivityNew;
import sprout.app.sakmvp1.AdminActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

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
 */
public class LoginActivity extends AppCompatActivity {

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

        // 일반 접속
        btnNormalAccess.setOnClickListener(v -> {
            // 이미 인증된 사용자가 있으면 재사용, 없으면 익명 로그인
            if (mAuth.getCurrentUser() != null) {
                // 이미 인증됨
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putBoolean("is_admin", false)
                        .apply();

                Toast.makeText(LoginActivity.this, "일반 사용자로 테스트 접속합니다", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivityNew.class));
                dialog.dismiss();
            } else {
                // Firebase 익명 로그인
                mAuth.signInAnonymously()
                        .addOnSuccessListener(authResult -> {
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            prefs.edit()
                                    .putBoolean("is_admin", false)
                                    .apply();

                            Toast.makeText(LoginActivity.this, "일반 사용자로 테스트 접속합니다", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivityNew.class));
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("LoginActivity", "Firebase 익명 인증 실패", e);
                            Toast.makeText(LoginActivity.this, "Firebase 인증 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        // 관리자 접속
        btnAdminAccess.setOnClickListener(v -> {
            // 이미 인증된 사용자가 있으면 재사용, 없으면 익명 로그인
            if (mAuth.getCurrentUser() != null) {
                // 이미 인증됨
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putBoolean("is_admin", true)
                        .commit();

                Toast.makeText(LoginActivity.this, "관리자로 테스트 접속합니다", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                dialog.dismiss();
            } else {
                // Firebase 익명 로그인
                mAuth.signInAnonymously()
                        .addOnSuccessListener(authResult -> {
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            prefs.edit()
                                    .putBoolean("is_admin", true)
                                    .commit();

                            Toast.makeText(LoginActivity.this, "관리자로 테스트 접속합니다", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("LoginActivity", "Firebase 익명 인증 실패", e);
                            Toast.makeText(LoginActivity.this, "Firebase 인증 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
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

                        // 메인 화면으로 이동 후 로그인 화면 종료
                        startActivity(new Intent(LoginActivity.this, MainActivityNew.class));
                        finish();
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
}
