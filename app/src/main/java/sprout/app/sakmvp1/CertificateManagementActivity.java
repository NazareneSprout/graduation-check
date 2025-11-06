package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 자격증 관리 Activity (관리자 전용)
 * 자격증 목록 조회, 추가, 수정, 삭제 기능 제공
 */
public class CertificateManagementActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerCertificates;
    private LinearLayout layoutEmpty;
    private FloatingActionButton fabAddCertificate;

    private FirebaseFirestore db;
    private CertificateAdminAdapter adapter;
    private List<Certificate> certificateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_management);

        db = FirebaseFirestore.getInstance();
        certificateList = new ArrayList<>();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        loadCertificates();
    }

    /**
     * View 초기화
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerCertificates = findViewById(R.id.recycler_certificates);
        layoutEmpty = findViewById(R.id.layout_empty);
        fabAddCertificate = findViewById(R.id.fab_add_certificate);
    }

    /**
     * Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * RecyclerView 설정
     */
    private void setupRecyclerView() {
        recyclerCertificates.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CertificateAdminAdapter(certificateList, new CertificateAdminAdapter.OnCertificateActionListener() {
            @Override
            public void onEdit(Certificate certificate) {
                // 수정 화면으로 이동
                Intent intent = new Intent(CertificateManagementActivity.this, CertificateEditActivity.class);
                intent.putExtra("certificate_id", certificate.getId());
                intent.putExtra("certificate_title", certificate.getTitle());
                intent.putExtra("certificate_issuer", certificate.getIssuer());
                intent.putExtra("certificate_department", certificate.getDepartment());
                intent.putExtra("certificate_url", certificate.getTargetUrl());
                intent.putExtra("is_edit_mode", true);
                startActivity(intent);
            }

            @Override
            public void onDelete(Certificate certificate) {
                // 삭제 확인 다이얼로그
                showDeleteConfirmDialog(certificate);
            }
        });
        recyclerCertificates.setAdapter(adapter);
    }

    /**
     * 클릭 리스너 설정
     */
    private void setupClickListeners() {
        fabAddCertificate.setOnClickListener(v -> {
            // 추가 화면으로 이동
            Intent intent = new Intent(this, CertificateEditActivity.class);
            intent.putExtra("is_edit_mode", false);
            startActivity(intent);
        });
    }

    /**
     * Firestore에서 자격증 목록 로드
     */
    private void loadCertificates() {
        db.collection("certificates")
                .orderBy("title")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    certificateList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Certificate certificate = document.toObject(Certificate.class);
                        certificate.setId(document.getId());
                        certificateList.add(certificate);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "자격증 목록을 불러오는데 실패했습니다: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 자격증 삭제 확인 다이얼로그
     */
    private void showDeleteConfirmDialog(Certificate certificate) {
        new AlertDialog.Builder(this)
                .setTitle("자격증 삭제")
                .setMessage("'" + certificate.getTitle() + "'을(를) 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteCertificate(certificate))
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * Firestore에서 자격증 삭제
     */
    private void deleteCertificate(Certificate certificate) {
        db.collection("certificates")
                .document(certificate.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "자격증이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    loadCertificates(); // 목록 새로고침
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 빈 상태 업데이트
     */
    private void updateEmptyState() {
        if (certificateList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerCertificates.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerCertificates.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 수정/추가 후 돌아왔을 때 목록 새로고침
        loadCertificates();
    }
}
