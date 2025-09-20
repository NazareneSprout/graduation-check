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

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText etConfirmPassword;

    private EditText editTextName;
    private EditText editTextUsername;
    private EditText editTextPhone;
    private EditText editTextBirthDate;
    //private EditText editTextAddress;
    //private EditText editTextDetailAddress;
    private Spinner spinnerDepartment;
    private EditText editTextStudentId;
    private RadioButton radioButtonMale;
    private RadioButton radioButtonFemale;
    private Button buttonRegister;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        // 시스템 UI 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 툴바 설정
        toolbar = findViewById(R.id.toolbar_sign_up);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // 뒤로가기 버튼 클릭 리스너
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        editTextEmail = findViewById(R.id.etEmail);
        editTextPassword = findViewById(R.id.etSignUpPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        editTextName = findViewById(R.id.etName);
        editTextUsername = findViewById(R.id.etUsername);
        editTextPhone = findViewById(R.id.etPhoneNumber);
        editTextBirthDate = findViewById(R.id.etBirthDate);
        //editTextAddress = findViewById(R.id.etAddress);
       // editTextDetailAddress = findViewById(R.id.etDetailAddress);
        radioButtonMale = findViewById(R.id.rbMale);
        radioButtonFemale = findViewById(R.id.rbFemale);
        buttonRegister = findViewById(R.id.btnSubmitSignUp);

        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        editTextStudentId = findViewById(R.id.etStudentId);

        // 날짜 선택 다이얼로그
        editTextBirthDate.setOnClickListener(v -> showDatePickerDialog());

        // 회원가입 버튼 클릭
        buttonRegister.setOnClickListener(v -> registerUser());

        // 비밀번호 확인 실시간 체크
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = editTextPassword.getText().toString();
                String confirm = s.toString();
                if (confirm.isEmpty()) {
                    etConfirmPassword.setError(null);
                    etConfirmPassword.setTextColor(ContextCompat.getColor(SignUpActivity.this, android.R.color.black));
                } else if (password.equals(confirm)) {
                    etConfirmPassword.setError(null);
                    etConfirmPassword.setTextColor(ContextCompat.getColor(SignUpActivity.this, android.R.color.holo_green_dark));
                } else {
                    etConfirmPassword.setError("비밀번호가 일치하지 않습니다");
                    etConfirmPassword.setTextColor(ContextCompat.getColor(SignUpActivity.this, android.R.color.holo_red_dark));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        // 학부 Spinner 불러오기
        loadDepartments();

        // 회원가입 버튼 클릭
        buttonRegister.setOnClickListener(v -> registerUser());

    }

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
    private void loadDepartments() {
        db.collection("graduation_meta")
                .document("catalog")
                .collection("departments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<String> departmentList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // doc.getId()를 사용하면 문서 ID가 학부 이름
                            String departmentName = doc.getId();
                            departmentList.add(departmentName);
                        }
                        Collections.sort(departmentList); // 가나다 순 정렬

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


    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String birthDate = editTextBirthDate.getText().toString().trim();
        //String address = editTextAddress.getText().toString().trim();
        //String detailAddress = editTextDetailAddress.getText().toString().trim();
        String gender = radioButtonMale.isChecked() ? "Male" : "Female";
        String signUpDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String studentId = editTextStudentId.getText().toString().trim();
        String department = spinnerDepartment.getSelectedItem() != null ?
                spinnerDepartment.getSelectedItem().toString() : "";



        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || username.isEmpty()
                || phone.isEmpty() || birthDate.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        User user = new User(
                                name, username, email, phone, gender,
                                birthDate, signUpDate, studentId, department
                        );

                        db.collection("users")
                                .document(mAuth.getCurrentUser().getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignUpActivity.this, "회원가입 성공", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "회원 정보 등록 실패", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Toast.makeText(SignUpActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();

                    }
                });
    }

    public static class User {
        public String name, username, email, phone, gender,
                birthDate, signUpDate, studentId, department;

        public User() {}

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
