package sprout.app.sakmvp1;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 교수/조교 전용 학사 관리 시스템 Activity
 * 졸업요건, 대체과목, 학생 데이터, 필요서류 관리 기능 제공
 */
public class AdminActivity extends AppCompatActivity {

    private static final String TAG = "AdminActivity";

    private MaterialToolbar toolbar;
    private MaterialCardView cardGraduationRequirements;
    private MaterialCardView cardStudentData, cardDocuments, cardBannerManagement;
    private MaterialCardView cardMajorDocument, cardGeneralDocument;
    private MaterialCardView cardMigrateTimetables;
    private MaterialButton btnLogout;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // 관리자 권한 확인
        if (!isAdmin()) {
            Toast.makeText(this, "관리자 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupClickListeners();
    }

    /**
     * 관리자 권한 확인
     */
    private boolean isAdmin() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("is_admin", false);
    }

    /**
     * View 초기화
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // 관리 카드
        cardGraduationRequirements = findViewById(R.id.card_graduation_requirements);
        cardStudentData = findViewById(R.id.card_student_data);
        cardDocuments = findViewById(R.id.card_documents);
        cardMajorDocument = findViewById(R.id.card_major_document);
        cardGeneralDocument = findViewById(R.id.card_general_document);
        cardBannerManagement = findViewById(R.id.card_banner_management);
        cardMigrateTimetables = findViewById(R.id.card_migrate_timetables);

        // 버튼들
        btnLogout = findViewById(R.id.btn_logout);
    }

    /**
     * Toolbar 설정
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    /**
     * 클릭 리스너 설정
     */
    private void setupClickListeners() {
        // 졸업요건 관리
        cardGraduationRequirements.setOnClickListener(v -> {
            Intent intent = new Intent(this, GraduationRequirementsActivity.class);
            startActivity(intent);
        });

        // 학생 데이터 조회
        cardStudentData.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentDataActivity.class);
            startActivity(intent);
        });

        // 필요서류 관리
        cardDocuments.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageDocumentFoldersActivity.class);
            startActivity(intent);
        });

        // 전공문서 관리
        cardMajorDocument.setOnClickListener(v -> {
            Intent intent = new Intent(this, MajorDocumentManageActivity.class);
            startActivity(intent);
        });

        // 교양문서 관리
        cardGeneralDocument.setOnClickListener(v -> {
            Intent intent = new Intent(this, GeneralDocumentManageActivity.class);
            startActivity(intent);
        });

        // 배너 관리
        cardBannerManagement.setOnClickListener(v -> {
            Intent intent = new Intent(this, BannerManagementActivity.class);
            startActivity(intent);
        });

        // 시간표 데이터 마이그레이션
        cardMigrateTimetables.setOnClickListener(v -> {
            showMigrationConfirmDialog();
        });

        // 일반 모드로 돌아가기
        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_admin", false).apply();
            Toast.makeText(this, "일반 모드로 전환되었습니다", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    /**
     * 마이그레이션 확인 다이얼로그 표시
     */
    private void showMigrationConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("데이터 마이그레이션")
                .setMessage("다음 작업을 수행합니다:\n\n" +
                        "1. 시간표 데이터를 users 컬렉션으로 이동\n" +
                        "2. 졸업 분석 데이터를 단일 문서로 통합\n\n" +
                        "이 작업은 일회성이며, 실행하시겠습니까?")
                .setPositiveButton("실행", (dialog, which) -> {
                    startMigration();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 마이그레이션 시작
     */
    private void startMigration() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("마이그레이션 진행 중...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1단계: 모든 사용자 가져오기
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        userIds.add(doc.getId());
                    }

                    if (userIds.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "마이그레이션할 사용자가 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2단계: 각 사용자별로 마이그레이션 수행
                    performMigrationForUsers(userIds, progressDialog);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "사용자 목록 가져오기 실패", e);
                    Toast.makeText(this, "마이그레이션 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 사용자별 마이그레이션 수행
     */
    private void performMigrationForUsers(List<String> userIds, ProgressDialog progressDialog) {
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        int totalUsers = userIds.size();

        for (String userId : userIds) {
            // 시간표 마이그레이션
            migrateTimetablesForUser(userId)
                    .addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            // 졸업 분석 데이터 마이그레이션
                            migrateGraduationAnalysisForUser(userId)
                                    .addOnCompleteListener(task2 -> {
                                        if (task2.isSuccessful()) {
                                            successCount.incrementAndGet();
                                        } else {
                                            errorCount.incrementAndGet();
                                            Log.e(TAG, "졸업 분석 마이그레이션 실패: " + userId, task2.getException());
                                        }

                                        // 완료 카운트 증가
                                        int completed = completedCount.incrementAndGet();
                                        progressDialog.setMessage("마이그레이션 진행 중... (" + completed + "/" + totalUsers + ")");

                                        // 모든 사용자 완료 시
                                        if (completed == totalUsers) {
                                            progressDialog.dismiss();
                                            showMigrationResult(successCount.get(), errorCount.get());
                                        }
                                    });
                        } else {
                            errorCount.incrementAndGet();
                            Log.e(TAG, "시간표 마이그레이션 실패: " + userId, task1.getException());

                            // 완료 카운트 증가
                            int completed = completedCount.incrementAndGet();
                            progressDialog.setMessage("마이그레이션 진행 중... (" + completed + "/" + totalUsers + ")");

                            // 모든 사용자 완료 시
                            if (completed == totalUsers) {
                                progressDialog.dismiss();
                                showMigrationResult(successCount.get(), errorCount.get());
                            }
                        }
                    });
        }
    }

    /**
     * 사용자별 시간표 마이그레이션
     * 옛날 경로: timetables/{userId}/user_timetables/*
     * 새 경로: users/{userId}/timetables/*
     */
    private com.google.android.gms.tasks.Task<Void> migrateTimetablesForUser(String userId) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();

        // 옛날 경로에서 데이터 읽기
        db.collection("timetables").document(userId)
                .collection("user_timetables")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // 마이그레이션할 데이터 없음
                        Log.d(TAG, "시간표 없음: " + userId);
                        taskSource.setResult(null);
                        return;
                    }

                    // 새 경로로 데이터 복사
                    AtomicInteger timetableCount = new AtomicInteger(queryDocumentSnapshots.size());
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();

                        // 새 경로에 쓰기
                        db.collection("users").document(userId)
                                .collection("timetables")
                                .document(doc.getId())
                                .set(data)
                                .addOnCompleteListener(task -> {
                                    if (timetableCount.decrementAndGet() == 0) {
                                        Log.d(TAG, "시간표 마이그레이션 완료: " + userId);
                                        taskSource.setResult(null);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "시간표 읽기 실패: " + userId, e);
                    taskSource.setException(e);
                });

        return taskSource.getTask();
    }

    /**
     * 사용자별 졸업 분석 데이터 마이그레이션
     * 옛날 경로: users/{userId}/graduation_check_history (최신 1개)
     * 새 경로: users/{userId}/current_graduation_analysis/latest
     */
    private com.google.android.gms.tasks.Task<Void> migrateGraduationAnalysisForUser(String userId) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource = new com.google.android.gms.tasks.TaskCompletionSource<>();

        // 옛날 경로에서 최신 데이터 1개 읽기
        db.collection("users").document(userId)
                .collection("graduation_check_history")
                .orderBy("checkedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // 마이그레이션할 데이터 없음
                        Log.d(TAG, "졸업 분석 없음: " + userId);
                        taskSource.setResult(null);
                        return;
                    }

                    // 최신 데이터 가져오기
                    QueryDocumentSnapshot latestDoc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                    Map<String, Object> data = latestDoc.getData();

                    // 새 경로에 쓰기
                    db.collection("users").document(userId)
                            .collection("current_graduation_analysis")
                            .document("latest")
                            .set(data)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "졸업 분석 마이그레이션 완료: " + userId);
                                taskSource.setResult(null);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "졸업 분석 쓰기 실패: " + userId, e);
                                taskSource.setException(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "졸업 분석 읽기 실패: " + userId, e);
                    taskSource.setException(e);
                });

        return taskSource.getTask();
    }

    /**
     * 마이그레이션 결과 표시
     */
    private void showMigrationResult(int successCount, int errorCount) {
        String message = "마이그레이션 완료\n\n" +
                "성공: " + successCount + "명\n" +
                "실패: " + errorCount + "명";

        new AlertDialog.Builder(this)
                .setTitle("마이그레이션 결과")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();

        Log.i(TAG, message);
    }

}
