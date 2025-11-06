package sprout.app.sakmvp1;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * 자격증 추가/수정 Activity
 */
public class CertificateEditActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilTitle, tilIssuer, tilDepartment, tilTargetUrl;
    private TextInputEditText etTitle, etIssuer, etDepartment, etTargetUrl;
    private MaterialButton btnSave, btnDelete;

    private FirebaseFirestore db;
    private boolean isEditMode;
    private String certificateId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_edit);

        db = FirebaseFirestore.getInstance();

        // Intent에서 데이터 받기
        isEditMode = getIntent().getBooleanExtra("is_edit_mode", false);
        if (isEditMode) {
            certificateId = getIntent().getStringExtra("certificate_id");
        }

        initViews();
        setupToolbar();
        loadExistingData();
        setupClickListeners();
    }

    /**
     * View 초기화
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tilTitle = findViewById(R.id.til_title);
        tilIssuer = findViewById(R.id.til_issuer);
        tilDepartment = findViewById(R.id.til_department);
        tilTargetUrl = findViewById(R.id.til_target_url);
        etTitle = findViewById(R.id.et_title);
        etIssuer = findViewById(R.id.et_issuer);
        etDepartment = findViewById(R.id.et_department);
        etTargetUrl = findViewById(R.id.et_target_url);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
    }

    /**
     * Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (isEditMode) {
                toolbar.setTitle("자격증 수정");
            } else {
                toolbar.setTitle("자격증 추가");
            }
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * 기존 데이터 로드 (수정 모드일 경우)
     */
    private void loadExistingData() {
        if (isEditMode) {
            etTitle.setText(getIntent().getStringExtra("certificate_title"));
            etIssuer.setText(getIntent().getStringExtra("certificate_issuer"));
            etDepartment.setText(getIntent().getStringExtra("certificate_department"));
            etTargetUrl.setText(getIntent().getStringExtra("certificate_url"));
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }
    }

    /**
     * 클릭 리스너 설정
     */
    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> validateAndSave());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    /**
     * 입력 값 검증 및 저장
     */
    private void validateAndSave() {
        // 에러 초기화
        tilTitle.setError(null);
        tilIssuer.setError(null);

        String title = etTitle.getText().toString().trim();
        String issuer = etIssuer.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String targetUrl = etTargetUrl.getText().toString().trim();

        // 필수 필드 검증
        boolean hasError = false;

        if (TextUtils.isEmpty(title)) {
            tilTitle.setError("자격증 이름을 입력해주세요");
            hasError = true;
        }

        if (TextUtils.isEmpty(issuer)) {
            tilIssuer.setError("발급 기관을 입력해주세요");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        // Firestore에 저장할 데이터
        Map<String, Object> certificateData = new HashMap<>();
        certificateData.put("title", title);
        certificateData.put("issuer", issuer);
        certificateData.put("department", department);
        certificateData.put("targetUrl", targetUrl);

        if (isEditMode) {
            // 수정 모드: 기존 문서 업데이트
            updateCertificate(certificateData);
        } else {
            // 추가 모드: 새 문서 생성
            certificateData.put("bookmarkCount", 0);
            certificateData.put("bookmarks", new HashMap<String, Boolean>());
            addCertificate(certificateData);
        }
    }

    /**
     * 자격증 추가
     */
    private void addCertificate(Map<String, Object> certificateData) {
        db.collection("certificates")
                .add(certificateData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "자격증이 추가되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 자격증 수정
     */
    private void updateCertificate(Map<String, Object> certificateData) {
        db.collection("certificates")
                .document(certificateId)
                .update(certificateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "자격증이 수정되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 삭제 확인 다이얼로그
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("자격증 삭제")
                .setMessage("이 자격증을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteCertificate())
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 자격증 삭제
     */
    private void deleteCertificate() {
        db.collection("certificates")
                .document(certificateId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "자격증이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
