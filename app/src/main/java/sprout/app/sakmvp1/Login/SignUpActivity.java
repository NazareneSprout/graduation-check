package sprout.app.sakmvp1.Login;

import android.app.DatePickerDialog;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 회원가입 화면
 * - Firebase Authentication으로 계정 생성
 * - Firestore에 사용자 프로필 저장
 * - UI 입력: 이메일, 비밀번호, 비밀번호 확인, 이름, 닉네임, 전화번호, 생년월일, 학부, 학번, 성별
 */
public class SignUpActivity extends AppCompatActivity {

    // UI 요소
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText etConfirmPassword;
    private EditText editTextName;
    private EditText editTextUsername;
    private EditText editTextPhone;
    private EditText editTextBirthDate;
    private Spinner spinnerDepartment;
    private EditText editTextStudentId;
    private RadioButton radioButtonMale;
    private RadioButton radioButtonFemale;
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
        editTextUsername = findViewById(R.id.etUsername);
        editTextPhone = findViewById(R.id.etPhoneNumber);
        editTextBirthDate = findViewById(R.id.etBirthDate);
        radioButtonMale = findViewById(R.id.rbMale);
        radioButtonFemale = findViewById(R.id.rbFemale);
        buttonRegister = findViewById(R.id.btnSubmitSignUp);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        editTextStudentId = findViewById(R.id.etStudentId);

        // 날짜 입력 필드 클릭 시 DatePickerDialog 표시
        editTextBirthDate.setOnClickListener(v -> showDatePickerDialog());

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

        // Firestore에서 학부 목록 불러와 Spinner 채우기
        loadDepartments();
    }

    /**
     * 생년월일 입력 → 달력 다이얼로그 표시
     */
    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    editTextBirthDate.setText(date);
                }, year, month, day);
        dialog.show();
    }

    /**
     * Firestore에서 학부 목록을 불러와 Spinner에 설정
     * - 경로: graduation_meta/catalog/departments
     * - 문서 ID가 학부 이름
     */
    private void loadDepartments() {
        db.collection("graduation_meta")
                .document("catalog")
                .collection("departments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<String> departmentList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String departmentName = doc.getId(); // 문서 ID = 학부명
                            departmentList.add(departmentName);
                        }
                        Collections.sort(departmentList); // 가나다순 정렬

                        // Spinner 어댑터에 연결
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                SignUpActivity.this,
                                android.R.layout.simple_spinner_item,
                                departmentList
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerDepartment.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "학부 로딩 실패", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "학부 불러오기 실패", e);
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
        String username = editTextUsername.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String birthDate = editTextBirthDate.getText().toString().trim();
        String gender = radioButtonMale.isChecked() ? "Male" : "Female";
        String signUpDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String studentId = editTextStudentId.getText().toString().trim();
        String department = spinnerDepartment.getSelectedItem() != null ?
                spinnerDepartment.getSelectedItem().toString() : "";

        // 입력값 검증
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || username.isEmpty()
                || phone.isEmpty() || birthDate.isEmpty() || gender.isEmpty()) {
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
                        User user = new User(
                                name, username, email, phone, gender,
                                birthDate, signUpDate, studentId, department
                        );

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
        public String name, username, email, phone, gender,
                birthDate, signUpDate, studentId, department;

        public User() {} // Firestore 역직렬화용 빈 생성자

        public User(String name, String username, String email, String phone, String gender,
                    String birthDate, String signUpDate, String studentId, String department) {
            this.name = name;
            this.username = username;
            this.email = email;
            this.phone = phone;
            this.gender = gender;
            this.birthDate = birthDate;
            this.signUpDate = signUpDate;
            this.studentId = studentId;
            this.department = department;
        }
    }
}
