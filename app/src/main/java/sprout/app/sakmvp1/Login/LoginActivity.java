package sprout.app.sakmvp1.Login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import sprout.app.sakmvp1.MainActivity;
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
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // 로그인 화면을 백스택에서 제거
        }

        // [로그인] 버튼 클릭 → 입력값 검증 후 Firebase 로그인 시도
        btnLogin.setOnClickListener(v -> loginUser());

        // [회원가입] 버튼 클릭 → 같은 패키지 내 SignUpActivity로 전환
        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        // [테스트 접속] 버튼 클릭 → 실제 인증 절차 없이 메인 화면으로 이동
        // - 시연/디자인 점검 용도
        // - finish()를 호출하지 않아 뒤로가기 시 로그인 화면으로 복귀 가능
        btnTestAccess.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "테스트 모드로 접속합니다", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        });
    }

    /**
     * 이메일/비밀번호로 Firebase 인증 시도
     * 1) 공란 검증
     * 2) FirebaseAuth.signInWithEmailAndPassword 호출
     * 3) 성공 시 자동 로그인 설정 저장 + 메인으로 이동
     * 4) 실패 시 예외 타입에 따라 사용자 친화적 메세지 노출
     */
    private void loginUser() {
        // 양끝 공백 제거(사용자 입력 실수 방지)
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 최소한의 유효성 검사(공란 체크)
        // - 추가적으로 이메일 형식 검증(android.util.Patterns.EMAIL_ADDRESS) 등을 확장 가능
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase 이메일/비밀번호 로그인
        // - 네트워크 비동기 작업: addOnCompleteListener 콜백에서 결과 처리
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 인증 성공
                        Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                        // 자동 로그인 설정을 사용자 선택(체크박스)대로 저장
                        // - true: 다음 앱 실행 시 자동 진입 시도
                        // - false: 다음 실행 시 로그인 화면 유지
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean(KEY_AUTO_LOGIN, cbAutoLogin.isChecked())
                                .apply();

                        // 메인 화면으로 이동 후 로그인 화면 종료
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        // 인증 실패: 예외 유형에 따라 구체 메시지 제공
                        Exception exception = task.getException();

                        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            // 비밀번호 오류(혹은 형식 불일치)
                            Toast.makeText(LoginActivity.this, "잘못된 비밀번호입니다.", Toast.LENGTH_SHORT).show();
                        } else if (exception instanceof FirebaseAuthInvalidUserException) {
                            // 가입된 이메일이 아님 / 비활성 사용자 등
                            Toast.makeText(LoginActivity.this, "존재하지 않는 이메일입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            // 그 외: 네트워크 오류, 과도한 시도, 서버 에러 등
                            // - 사용자에게는 일반화된 메시지 + 디버깅을 위해 상세 메시지도 덧붙임
                            // - 실제 배포환경에선 상세 메시지 노출을 최소화하는 것이 보안/UX 측면에서 바람직
                            Toast.makeText(
                                    LoginActivity.this,
                                    "로그인 실패: " + (exception != null ? exception.getMessage() : "알 수 없는 오류"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });
    }
}
