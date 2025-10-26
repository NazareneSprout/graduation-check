package sprout.app.sakmvp1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 교수/조교 전용 학사 관리 시스템 Activity
 * 졸업요건, 대체과목, 학생 데이터, 공지사항 관리 기능 제공
 */
public class AdminActivity extends AppCompatActivity {

    private static final String TAG = "AdminActivity";

    private MaterialToolbar toolbar;
    private MaterialCardView cardGraduationRequirements;
    private MaterialCardView cardStudentData, cardNotices, cardDocuments, cardDbRestructure;
    private MaterialButton btnLogout;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Firebase 초기화
        db = FirebaseFirestore.getInstance();

        // 관리자 권한 확인
        if (!isAdmin()) {
            Toast.makeText(this, "관리자 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
        cardNotices = findViewById(R.id.card_notices);
        cardDocuments = findViewById(R.id.card_documents);
        cardDbRestructure = findViewById(R.id.card_db_restructure);

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

        // 공지사항 관리
        cardNotices.setOnClickListener(v -> {
            Toast.makeText(this, "공지사항 관리 기능 - 개발 예정", Toast.LENGTH_SHORT).show();
            // TODO: NoticesActivity 구현
        });

        // 필요서류 관리
        cardDocuments.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageDocumentFoldersActivity.class);
            startActivity(intent);
        });

        // DB 구조 정리
        cardDbRestructure.setOnClickListener(v -> {
            showDbRestructureDialog();
        });

        // 임시: DB 구조 정리 카드를 길게 누르면 현재 상태 확인
        cardDbRestructure.setOnLongClickListener(v -> {
            checkCurrentDbStructure();
            return true;
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
     * DB 구조 정리 확인 다이얼로그
     */
    private void showDbRestructureDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ DB 구조 정리")
                .setMessage("전공/교양 문서의 책임을 명확히 분리합니다.\n\n" +
                        "✅ 전공 문서: 전공 관련 정보만 포함\n" +
                        "✅ 교양 문서: 교양 관련 정보만 포함\n" +
                        "✅ 졸업요건 문서: 참조 정보만 포함\n\n" +
                        "⚠️ 이 작업은 모든 graduation_requirements 문서에 영향을 줍니다.\n\n" +
                        "계속하시겠습니까?")
                .setPositiveButton("실행", (dialog, which) -> {
                    executeDbRestructure();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * DB 구조 정리 실행
     */
    private void executeDbRestructure() {
        Log.d(TAG, "DB 구조 정리 시작");
        Toast.makeText(this, "DB 구조 정리를 시작합니다...", Toast.LENGTH_SHORT).show();

        // ProgressDialog 대신 간단한 Toast로 진행상황 표시
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalDocs = querySnapshot.size();
                    final int[] processedDocs = {0};
                    final int[] majorDocs = {0};
                    final int[] generalDocs = {0};
                    final int[] gradDocs = {0};

                    Log.d(TAG, "총 " + totalDocs + "개 문서 처리 시작");

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docId = doc.getId();
                        Log.d(TAG, "문서 처리 중: " + docId);

                        // 문서 타입 판별
                        String docType = determineDocType(docId, doc);
                        Log.d(TAG, docId + " -> docType: " + docType);

                        // 타입에 따라 정리
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("docType", docType);

                        switch (docType) {
                            case "major":
                                cleanupMajorDocument(doc, updates);
                                majorDocs[0]++;
                                break;
                            case "general":
                                cleanupGeneralDocument(doc, updates);
                                generalDocs[0]++;
                                break;
                            case "graduation":
                                cleanupGraduationDocument(doc, updates);
                                gradDocs[0]++;
                                break;
                        }

                        // 문서 업데이트
                        db.collection("graduation_requirements")
                                .document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    processedDocs[0]++;
                                    Log.d(TAG, "문서 업데이트 완료: " + docId + " (" + processedDocs[0] + "/" + totalDocs + ")");

                                    if (processedDocs[0] == totalDocs) {
                                        // 모든 문서 처리 완료
                                        String result = "DB 구조 정리 완료!\n\n" +
                                                "전공 문서: " + majorDocs[0] + "개\n" +
                                                "교양 문서: " + generalDocs[0] + "개\n" +
                                                "졸업요건 문서: " + gradDocs[0] + "개";
                                        Log.d(TAG, result);
                                        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "문서 업데이트 실패: " + docId, e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패", e);
                    Toast.makeText(this, "문서 조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 문서 타입 판별
     */
    private String determineDocType(String docId, com.google.firebase.firestore.DocumentSnapshot doc) {
        // 이미 docType이 설정되어 있으면 그대로 사용
        String existingDocType = doc.getString("docType");
        if (existingDocType != null && !existingDocType.isEmpty()) {
            return existingDocType;
        }

        // 문서 ID로 판별
        if (docId.startsWith("졸업요건_")) {
            return "graduation";
        } else if (docId.startsWith("교양_")) {
            return "general";
        } else {
            // 학부_트랙_년도 형식 (전공 문서)
            return "major";
        }
    }

    /**
     * 전공 문서 정리
     */
    private void cleanupMajorDocument(com.google.firebase.firestore.DocumentSnapshot doc, Map<String, Object> updates) {
        Log.d(TAG, "전공 문서 정리: " + doc.getId());

        // 학점 요구사항 제거 (과목 목록만 남김)
        updates.put("전공필수", com.google.firebase.firestore.FieldValue.delete());
        updates.put("전공선택", com.google.firebase.firestore.FieldValue.delete());
        updates.put("학부공통", com.google.firebase.firestore.FieldValue.delete());
        updates.put("전공심화", com.google.firebase.firestore.FieldValue.delete());

        // 교양 관련 필드 제거
        updates.put("교양필수", com.google.firebase.firestore.FieldValue.delete());
        updates.put("교양선택", com.google.firebase.firestore.FieldValue.delete());
        updates.put("소양", com.google.firebase.firestore.FieldValue.delete());

        // 기타 필드 제거
        updates.put("자율선택", com.google.firebase.firestore.FieldValue.delete());
        updates.put("잔여학점", com.google.firebase.firestore.FieldValue.delete());
        updates.put("totalCredits", com.google.firebase.firestore.FieldValue.delete());

        // 대체과목 규칙 제거 (졸업요건 문서로 이동)
        updates.put("replacementRules", com.google.firebase.firestore.FieldValue.delete());
        updates.put("replacementCourses", com.google.firebase.firestore.FieldValue.delete());

        // 참조 필드 제거 (전공 문서는 참조하지 않음)
        updates.put("majorDocId", com.google.firebase.firestore.FieldValue.delete());
        updates.put("generalEducationDocId", com.google.firebase.firestore.FieldValue.delete());
        updates.put("majorDocRef", com.google.firebase.firestore.FieldValue.delete());
        updates.put("generalDocRef", com.google.firebase.firestore.FieldValue.delete());

        Log.d(TAG, "전공 문서 정리 완료: 과목 목록만 유지");
    }

    /**
     * 교양 문서 정리
     */
    private void cleanupGeneralDocument(com.google.firebase.firestore.DocumentSnapshot doc, Map<String, Object> updates) {
        Log.d(TAG, "교양 문서 정리: " + doc.getId());

        // 학점 요구사항 제거 (과목 목록만 남김)
        updates.put("교양필수", com.google.firebase.firestore.FieldValue.delete());
        updates.put("교양선택", com.google.firebase.firestore.FieldValue.delete());
        updates.put("소양", com.google.firebase.firestore.FieldValue.delete());

        // 전공 관련 필드 제거
        updates.put("전공필수", com.google.firebase.firestore.FieldValue.delete());
        updates.put("전공선택", com.google.firebase.firestore.FieldValue.delete());
        updates.put("학부공통", com.google.firebase.firestore.FieldValue.delete());
        updates.put("전공심화", com.google.firebase.firestore.FieldValue.delete());

        // 기타 필드 제거
        updates.put("자율선택", com.google.firebase.firestore.FieldValue.delete());
        updates.put("잔여학점", com.google.firebase.firestore.FieldValue.delete());
        updates.put("totalCredits", com.google.firebase.firestore.FieldValue.delete());

        // 대체과목 규칙 제거 (졸업요건 문서로 이동)
        updates.put("replacementRules", com.google.firebase.firestore.FieldValue.delete());
        updates.put("replacementCourses", com.google.firebase.firestore.FieldValue.delete());

        // 참조 필드 제거
        updates.put("majorDocId", com.google.firebase.firestore.FieldValue.delete());
        updates.put("generalEducationDocId", com.google.firebase.firestore.FieldValue.delete());
        updates.put("majorDocRef", com.google.firebase.firestore.FieldValue.delete());
        updates.put("generalDocRef", com.google.firebase.firestore.FieldValue.delete());

        Log.d(TAG, "교양 문서 정리 완료: 과목 목록만 유지");
    }

    /**
     * 졸업요건 문서 정리
     */
    private void cleanupGraduationDocument(com.google.firebase.firestore.DocumentSnapshot doc, Map<String, Object> updates) {
        Log.d(TAG, "졸업요건 문서 정리: " + doc.getId());

        // rules 필드 제거 (과목 목록은 참조 문서에서 가져옴)
        updates.put("rules", com.google.firebase.firestore.FieldValue.delete());

        // ✅ 학점 요구사항은 유지 (졸업요건 문서가 가져야 할 핵심 정보)
        // 전공필수, 전공선택, 학부공통, 전공심화
        // 교양필수, 교양선택, 소양
        // 자율선택, 잔여학점, totalCredits

        // ✅ replacementRules 유지 (대체과목 규칙은 졸업요건 문서에 보관)
        // replacementCourses는 제거 (구버전)
        updates.put("replacementCourses", com.google.firebase.firestore.FieldValue.delete());

        // ✅ majorDocRef, generalDocRef 유지 (참조 정보)
        // ✅ additionalRequirements 유지 (TLC, 채플 등)

        Log.d(TAG, "졸업요건 문서 정리 완료: 과목 목록 제거, 학점/대체과목/참조 정보 유지");
    }

    /**
     * 현재 DB 구조 확인 (대체과목 규칙 확인)
     */
    private void checkCurrentDbStructure() {
        Log.d(TAG, "현재 DB 구조 확인 시작");
        Toast.makeText(this, "DB 구조를 확인하는 중...", Toast.LENGTH_SHORT).show();

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    StringBuilder result = new StringBuilder();
                    result.append("=== 대체과목 규칙 보유 문서 ===\n\n");

                    int totalDocs = querySnapshot.size();
                    int docsWithReplacement = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docId = doc.getId();
                        String docType = determineDocType(docId, doc);

                        // replacementRules 확인
                        Object replacementRulesObj = doc.get("replacementRules");
                        if (replacementRulesObj instanceof java.util.List) {
                            java.util.List<?> rules = (java.util.List<?>) replacementRulesObj;
                            if (!rules.isEmpty()) {
                                docsWithReplacement++;
                                result.append("📄 ").append(docId).append("\n");
                                result.append("   타입: ").append(docType).append("\n");
                                result.append("   규칙: ").append(rules.size()).append("개\n\n");

                                Log.d(TAG, "✅ " + docId + " (" + docType + ") - " + rules.size() + "개 대체과목 규칙");
                            }
                        }
                    }

                    result.append("총 ").append(totalDocs).append("개 문서 중 ")
                            .append(docsWithReplacement).append("개 문서에 대체과목 규칙 존재");

                    Log.d(TAG, "DB 구조 확인 완료: " + docsWithReplacement + "/" + totalDocs);

                    // 다이얼로그로 결과 표시
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("현재 DB 구조")
                            .setMessage(result.toString())
                            .setPositiveButton("확인", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "DB 구조 확인 실패", e);
                    Toast.makeText(this, "확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
