package sprout.app.sakmvp1.Login;

import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import sprout.app.sakmvp1.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 회원가입 화면
 * - Firebase Authentication으로 계정 생성
 * - Firestore에 사용자 프로필 저장
 * - UI 입력: 이메일, 비밀번호, 비밀번호 확인, 이름
 */
public class SignUpActivity extends AppCompatActivity {

    // UI 요소
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText etConfirmPassword;
    private EditText editTextName;
    private Button buttonRegister;
    private Toolbar toolbar;

    // Firebase 인스턴스
    private FirebaseAuth mAuth;           // 계정 생성/로그인 담당
    private FirebaseFirestore db;         // 사용자 정보 저장용 DB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 시스템 UI와 맞닿도록 레이아웃 확장
        setContentView(R.layout.activity_sign_up);

        // 시스템 바(상태바/내비게이션바) 영역 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase 초기화
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 툴바 설정 (뒤로가기 화살표 추가)
        toolbar = findViewById(R.id.toolbar_sign_up);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // UI 위젯 초기화
        editTextEmail = findViewById(R.id.etEmail);
        editTextPassword = findViewById(R.id.etSignUpPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        editTextName = findViewById(R.id.etName);
        buttonRegister = findViewById(R.id.btnSubmitSignUp);

        // 회원가입 버튼 클릭 이벤트 → registerUser() 실행
        buttonRegister.setOnClickListener(v -> registerUser());

        // 비밀번호 확인 입력 시 실시간 검증
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = editTextPassword.getText().toString();
                String confirm = s.toString();
                if (confirm.isEmpty()) {
                    // 입력이 없을 때 → 에러 없음 + 검정색 글씨
                    etConfirmPassword.setError(null);
                    etConfirmPassword.setTextColor(ContextCompat.getColor(SignUpActivity.this, android.R.color.black));
                } else if (password.equals(confirm)) {
                    // 비밀번호 일치 → 초록색
                    etConfirmPassword.setError(null);
                    etConfirmPassword.setTextColor(ContextCompat.getColor(SignUpActivity.this, android.R.color.holo_green_dark));
                } else {
                    // 불일치 → 빨간색 + 에러 메시지
                    etConfirmPassword.setError("비밀번호가 일치하지 않습니다");
                    etConfirmPassword.setTextColor(ContextCompat.getColor(SignUpActivity.this, android.R.color.holo_red_dark));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 회원가입 처리
     * 1. 입력값 검증
     * 2. Firebase Authentication에 계정 생성
     * 3. Firestore에 사용자 정보(User 객체) 저장
     */
    private void registerUser() {
        // 입력값 읽기
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String signUpDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 입력값 검증
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        // Firebase Authentication 계정 생성
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Firestore에 저장할 사용자 객체 생성
                        User user = new User(name, email, signUpDate);

                        // Firestore에 UID 기준으로 저장
                        db.collection("users")
                                .document(mAuth.getCurrentUser().getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignUpActivity.this, "회원가입 성공", Toast.LENGTH_LONG).show();
                                    finish(); // 가입 완료 후 로그인 화면으로 돌아감
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "회원 정보 등록 실패", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // 계정 생성 실패 (이메일 중복, 네트워크 오류 등)
                        Toast.makeText(SignUpActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Firestore에 저장할 사용자 데이터 모델
     * - public 필드여야 Firestore에서 직렬화 가능
     */
    public static class User {
        public String name, email, signUpDate;

        public User() {} // Firestore 역직렬화용 빈 생성자

        public User(String name, String email, String signUpDate) {
            this.name = name;
            this.email = email;
            this.signUpDate = signUpDate;
        }
    }
}
