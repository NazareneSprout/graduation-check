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
    private MaterialCardView cardStudentData, cardNotices;
    private MaterialCardView cardAddDocReferences;
    private MaterialCardView cardUnifyTotalCredits;
    private MaterialCardView cardUnifyMajorDocs;
    private MaterialCardView cardUnifyFreeElective;
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
        cardAddDocReferences = findViewById(R.id.card_add_doc_references);
        cardUnifyTotalCredits = findViewById(R.id.card_unify_total_credits);
        cardUnifyMajorDocs = findViewById(R.id.card_unify_major_docs);
        cardUnifyFreeElective = findViewById(R.id.card_unify_free_elective);

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

        // 참조 문서 ID 일괄 추가
        cardAddDocReferences.setOnClickListener(v -> {
            showAddDocReferencesDialog();
        });

        // 총학점 필드 통일
        cardUnifyTotalCredits.setOnClickListener(v -> {
            showUnifyTotalCreditsDialog();
        });

        // 전공문서 구조 통합
        cardUnifyMajorDocs.setOnClickListener(v -> {
            showUnifyMajorDocsDialog();
        });

        // 자율선택 필드 통일
        cardUnifyFreeElective.setOnClickListener(v -> {
            showUnifyFreeElectiveDialog();
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
     * 참조 문서 ID 일괄 추가 다이얼로그 표시
     */
    private void showAddDocReferencesDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("참조 문서 ID 일괄 추가")
                .setMessage("모든 졸업요건 문서에 majorDocId와 generalEducationDocId를 자동으로 추가합니다.\n\n" +
                        "규칙:\n" +
                        "• majorDocId: {학부}_{트랙}_{학번} 형식으로 추가\n" +
                        "• generalEducationDocId: 교양_{학부}_{학번} 형식으로 추가\n\n" +
                        "예시:\n" +
                        "IT학부_멀티미디어_2021 문서에\n" +
                        "→ majorDocId: IT학부_멀티미디어_2021\n" +
                        "→ generalEducationDocId: 교양_IT학부_2021\n\n" +
                        "계속하시겠습니까?")
                .setPositiveButton("실행", (dialog, which) -> {
                    addDocReferencesToAll();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 모든 졸업요건 문서에 참조 문서 ID 추가
     */
    private void addDocReferencesToAll() {
        Toast.makeText(this, "작업을 시작합니다...", Toast.LENGTH_SHORT).show();

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalDocs = querySnapshot.size();
                    final int[] updatedCount = {0};
                    final int[] skippedCount = {0};

                    Log.d(TAG, "총 " + totalDocs + "개 문서 처리 시작");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        String docId = document.getId();

                        // 문서 ID 파싱: IT학부_멀티미디어_2021
                        String[] parts = docId.split("_");
                        if (parts.length < 3) {
                            Log.w(TAG, "문서 ID 형식이 올바르지 않음: " + docId);
                            skippedCount[0]++;
                            continue;
                        }

                        String department = parts[0];  // IT학부
                        String track = parts[1];       // 멀티미디어
                        String year = parts[2];        // 2021

                        // majorDocId와 generalEducationDocId 생성
                        String majorDocId = docId;  // 자기 자신 참조
                        String generalEducationDocId = "교양_" + department + "_" + year;

                        // 업데이트할 데이터
                        Map<String, Object> updates = new HashMap<>();

                        // 기존 값이 없을 때만 추가
                        if (!document.contains("majorDocId") || document.getString("majorDocId") == null ||
                            document.getString("majorDocId").isEmpty()) {
                            updates.put("majorDocId", majorDocId);
                        }

                        if (!document.contains("generalEducationDocId") ||
                            document.getString("generalEducationDocId") == null ||
                            document.getString("generalEducationDocId").isEmpty()) {
                            updates.put("generalEducationDocId", generalEducationDocId);
                        }

                        // 업데이트할 내용이 있으면 실행
                        if (!updates.isEmpty()) {
                            db.collection("graduation_requirements")
                                    .document(docId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        updatedCount[0]++;
                                        Log.d(TAG, "문서 업데이트 성공: " + docId +
                                                " (majorDocId=" + updates.get("majorDocId") +
                                                ", generalDocId=" + updates.get("generalEducationDocId") + ")");

                                        // 모든 작업 완료 확인
                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        skippedCount[0]++;
                                        Log.e(TAG, "문서 업데이트 실패: " + docId, e);

                                        // 모든 작업 완료 확인
                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    });
                        } else {
                            // 업데이트할 필요 없음 (이미 값이 있음)
                            skippedCount[0]++;
                            Log.d(TAG, "문서 건너뜀 (이미 값 존재): " + docId);

                            // 모든 작업 완료 확인
                            if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                showCompletionMessage(updatedCount[0], skippedCount[0]);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패", e);
                    Toast.makeText(this, "실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 작업 완료 메시지 표시
     */
    private void showCompletionMessage(int updated, int skipped) {
        String message = "작업 완료!\n\n" +
                "업데이트: " + updated + "개\n" +
                "건너뜀: " + skipped + "개";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("완료")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();

        Log.d(TAG, "전체 작업 완료 - 업데이트: " + updated + ", 건너뜀: " + skipped);
    }

    /**
     * 총학점 필드 통일 다이얼로그 표시
     */
    private void showUnifyTotalCreditsDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("총학점 필드 통일")
                .setMessage("모든 졸업요건 문서의 총학점 필드를 통일합니다.\n\n" +
                        "처리 내용:\n" +
                        "• '총이수' 필드 → 'totalCredits'로 복사 후 삭제\n" +
                        "• '총학점' 필드 → 'totalCredits'로 복사 후 삭제\n" +
                        "• 'totalCredits'가 없으면 기본값 130 설정\n\n" +
                        "⚠️ 이 작업은 되돌릴 수 없습니다.\n" +
                        "계속하시겠습니까?")
                .setPositiveButton("실행", (dialog, which) -> {
                    unifyTotalCreditsFields();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 모든 졸업요건 문서의 총학점 필드를 totalCredits로 통일
     */
    private void unifyTotalCreditsFields() {
        Toast.makeText(this, "작업을 시작합니다...", Toast.LENGTH_SHORT).show();

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalDocs = querySnapshot.size();
                    final int[] updatedCount = {0};
                    final int[] skippedCount = {0};

                    Log.d(TAG, "총학점 필드 통일: 총 " + totalDocs + "개 문서 처리 시작");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        String docId = document.getId();
                        Map<String, Object> updates = new HashMap<>();
                        boolean needsUpdate = false;

                        // 현재 필드 값 확인
                        Object totalCreditsObj = document.get("totalCredits");
                        Object totalIsuObj = document.get("총이수");
                        Object totalHakjeomObj = document.get("총학점");

                        // 우선순위: totalCredits > 총이수 > 총학점
                        final int finalTotalCredits;
                        if (totalCreditsObj instanceof Number) {
                            finalTotalCredits = ((Number) totalCreditsObj).intValue();
                        } else if (totalIsuObj instanceof Number) {
                            finalTotalCredits = ((Number) totalIsuObj).intValue();
                            needsUpdate = true;
                        } else if (totalHakjeomObj instanceof Number) {
                            finalTotalCredits = ((Number) totalHakjeomObj).intValue();
                            needsUpdate = true;
                        } else {
                            // 모든 필드가 없으면 기본값 설정
                            finalTotalCredits = 130;
                            needsUpdate = true;
                        }

                        // totalCredits 설정
                        updates.put("totalCredits", finalTotalCredits);

                        // 한글 필드 삭제 (FieldValue.delete() 사용)
                        if (document.contains("총이수")) {
                            updates.put("총이수", com.google.firebase.firestore.FieldValue.delete());
                            needsUpdate = true;
                        }
                        if (document.contains("총학점")) {
                            updates.put("총학점", com.google.firebase.firestore.FieldValue.delete());
                            needsUpdate = true;
                        }

                        if (needsUpdate) {
                            final String finalDocId = docId;
                            db.collection("graduation_requirements")
                                    .document(docId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        updatedCount[0]++;
                                        Log.d(TAG, "문서 업데이트 성공: " + finalDocId + " (totalCredits=" + finalTotalCredits + ")");

                                        // 모든 작업 완료 확인
                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showUnifyCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        skippedCount[0]++;
                                        Log.e(TAG, "문서 업데이트 실패: " + finalDocId, e);

                                        // 모든 작업 완료 확인
                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showUnifyCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    });
                        } else {
                            // 업데이트할 필요 없음
                            skippedCount[0]++;
                            Log.d(TAG, "문서 건너뜀 (이미 totalCredits만 있음): " + docId);

                            // 모든 작업 완료 확인
                            if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                showUnifyCompletionMessage(updatedCount[0], skippedCount[0]);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패", e);
                    Toast.makeText(this, "실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 총학점 필드 통일 작업 완료 메시지 표시
     */
    private void showUnifyCompletionMessage(int updated, int skipped) {
        String message = "총학점 필드 통일 완료!\n\n" +
                "업데이트: " + updated + "개\n" +
                "건너뜀: " + skipped + "개\n\n" +
                "모든 문서가 'totalCredits' 필드를 사용합니다.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("완료")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();

        Log.d(TAG, "총학점 필드 통일 완료 - 업데이트: " + updated + ", 건너뜀: " + skipped);
    }

    /**
     * 전공문서 구조 통합 다이얼로그 표시
     */
    private void showUnifyMajorDocsDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("전공문서 구조 통합")
                .setMessage("모든 졸업요건 문서를 Phase 1-2 통합 구조로 변환합니다.\n\n" +
                        "처리 내용:\n" +
                        "• 카테고리 명칭 통일 (학부공통필수 → 학부공통)\n" +
                        "• 메타 필드 추가 (department, track, cohort, version, updatedAt)\n" +
                        "• 문서 참조 ID 자동 설정 (majorDocId, generalEducationDocId)\n" +
                        "• rules 구조를 Phase 1-2 카테고리별 배열로 변환\n" +
                        "• replacementCourses 필드 추가\n\n" +
                        "⚠️ 이 작업은 되돌릴 수 없습니다.\n" +
                        "계속하시겠습니까?")
                .setPositiveButton("실행", (dialog, which) -> {
                    unifyMajorDocStructures();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 모든 졸업요건 문서를 Phase 1-2 통합 구조로 변환
     */
    private void unifyMajorDocStructures() {
        Toast.makeText(this, "작업을 시작합니다...", Toast.LENGTH_SHORT).show();

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalDocs = querySnapshot.size();
                    final int[] updatedCount = {0};
                    final int[] skippedCount = {0};

                    Log.d(TAG, "전공문서 구조 통합: 총 " + totalDocs + "개 문서 처리 시작");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        String docId = document.getId();
                        Map<String, Object> data = document.getData();
                        Map<String, Object> updates = new HashMap<>();
                        boolean needsUpdate = false;

                        Log.d(TAG, "문서 처리 중: " + docId);

                        // 1. 문서 ID 파싱하여 메타 정보 추출
                        String[] parts = docId.split("_");
                        if (parts.length >= 2) {
                            // department, track, cohort 추출
                            String department = null;
                            String track = null;
                            String cohort = null;

                            if (docId.startsWith("교양_")) {
                                // 교양 문서: 교양_IT학부_2026
                                if (parts.length >= 3) {
                                    department = parts[1];  // IT학부
                                    cohort = parts[2];      // 2026
                                }
                            } else {
                                // 졸업요건 문서: IT학부_멀티미디어_2021
                                if (parts.length >= 3) {
                                    department = parts[0];  // IT학부
                                    track = parts[1];       // 멀티미디어
                                    cohort = parts[2];      // 2021
                                }
                            }

                            // 메타 필드 추가
                            if (department != null && !data.containsKey("department")) {
                                updates.put("department", department);
                                needsUpdate = true;
                            }
                            if (track != null && !data.containsKey("track")) {
                                updates.put("track", track);
                                needsUpdate = true;
                            }
                            if (cohort != null && !data.containsKey("cohort")) {
                                updates.put("cohort", cohort);
                                needsUpdate = true;
                            }
                        }

                        // 2. version과 updatedAt 추가
                        if (!data.containsKey("version")) {
                            updates.put("version", "v2");
                            needsUpdate = true;
                        }
                        updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        needsUpdate = true;

                        // 3. majorDocId와 generalEducationDocId 설정
                        if (!docId.startsWith("교양_")) {
                            // 졸업요건 문서인 경우
                            if (!data.containsKey("majorDocId") || data.get("majorDocId") == null ||
                                    ((String) data.get("majorDocId")).isEmpty()) {
                                updates.put("majorDocId", docId);  // 자기 자신 참조
                                needsUpdate = true;
                            }

                            if (parts.length >= 3) {
                                String generalDocId = "교양_" + parts[0] + "_" + parts[2];
                                if (!data.containsKey("generalEducationDocId") ||
                                        data.get("generalEducationDocId") == null ||
                                        ((String) data.get("generalEducationDocId")).isEmpty()) {
                                    updates.put("generalEducationDocId", generalDocId);
                                    needsUpdate = true;
                                }
                            }
                        }

                        // 4. 학부공통필수 → 학부공통 병합
                        Object rulesObj = data.get("rules");
                        if (rulesObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> rules = new HashMap<>((Map<String, Object>) rulesObj);
                            boolean rulesUpdated = false;

                            // 학부공통필수가 있으면 학부공통으로 병합
                            if (rules.containsKey("학부공통필수")) {
                                Object departmentCommonRequired = rules.get("학부공통필수");
                                Object departmentCommon = rules.get("학부공통");

                                List<Map<String, Object>> mergedList = new ArrayList<>();

                                // 학부공통필수 과목 추가
                                if (departmentCommonRequired instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> list = (List<Map<String, Object>>) departmentCommonRequired;
                                    for (Map<String, Object> course : list) {
                                        // category를 학부공통으로 변경
                                        Map<String, Object> updatedCourse = new HashMap<>(course);
                                        updatedCourse.put("category", "학부공통");
                                        mergedList.add(updatedCourse);
                                    }
                                }

                                // 기존 학부공통 과목 추가
                                if (departmentCommon instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> list = (List<Map<String, Object>>) departmentCommon;
                                    mergedList.addAll(list);
                                }

                                // 병합된 리스트로 업데이트
                                rules.put("학부공통", mergedList);
                                rules.remove("학부공통필수");
                                rulesUpdated = true;
                            }

                            if (rulesUpdated) {
                                updates.put("rules", rules);
                                needsUpdate = true;
                            }
                        }

                        // 5. replacementCourses 필드 추가 (없으면)
                        if (!data.containsKey("replacementCourses")) {
                            updates.put("replacementCourses", new HashMap<String, Object>());
                            needsUpdate = true;
                        }

                        // 6. 업데이트 실행
                        if (needsUpdate) {
                            final String finalDocId = docId;
                            db.collection("graduation_requirements")
                                    .document(docId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        updatedCount[0]++;
                                        Log.d(TAG, "문서 구조 통합 성공: " + finalDocId);

                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showUnifyMajorDocsCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        skippedCount[0]++;
                                        Log.e(TAG, "문서 구조 통합 실패: " + finalDocId, e);

                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showUnifyMajorDocsCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    });
                        } else {
                            skippedCount[0]++;
                            Log.d(TAG, "문서 건너뜀 (이미 통합 구조): " + docId);

                            if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                showUnifyMajorDocsCompletionMessage(updatedCount[0], skippedCount[0]);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패", e);
                    Toast.makeText(this, "실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 전공문서 구조 통합 작업 완료 메시지 표시
     */
    private void showUnifyMajorDocsCompletionMessage(int updated, int skipped) {
        String message = "전공문서 구조 통합 완료!\n\n" +
                "업데이트: " + updated + "개\n" +
                "건너뜀: " + skipped + "개\n\n" +
                "모든 문서가 Phase 1-2 통합 구조를 사용합니다.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("완료")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();

        Log.d(TAG, "전공문서 구조 통합 완료 - 업데이트: " + updated + ", 건너뜀: " + skipped);
    }

    /**
     * 자율선택 필드 통일 다이얼로그 표시
     */
    private void showUnifyFreeElectiveDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("자율선택 필드 통일")
                .setMessage("모든 졸업요건 문서의 일반선택, 잔여학점 필드를 자율선택으로 통일합니다.\n\n" +
                        "처리 내용:\n" +
                        "• '일반선택' + '잔여학점' 값을 합산하여 '자율선택'에 저장\n" +
                        "• '일반선택' 필드 삭제\n" +
                        "• '잔여학점' 필드 삭제\n\n" +
                        "⚠️ 이 작업은 되돌릴 수 없습니다.\n" +
                        "계속하시겠습니까?")
                .setPositiveButton("실행", (dialog, which) -> {
                    unifyFreeElectiveFields();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 모든 졸업요건 문서의 자율선택 필드 통일
     */
    private void unifyFreeElectiveFields() {
        Toast.makeText(this, "작업을 시작합니다...", Toast.LENGTH_SHORT).show();

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalDocs = querySnapshot.size();
                    final int[] updatedCount = {0};
                    final int[] skippedCount = {0};

                    Log.d(TAG, "자율선택 필드 통일: 총 " + totalDocs + "개 문서 처리 시작");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        String docId = document.getId();
                        Map<String, Object> updates = new HashMap<>();
                        boolean needsUpdate = false;

                        // 현재 필드 값 확인
                        Object freeElectiveObj = document.get("자율선택");
                        Object generalSelectionObj = document.get("일반선택");
                        Object remainingCreditsObj = document.get("잔여학점");

                        int freeElective = 0;
                        int generalSelection = 0;
                        int remainingCredits = 0;

                        // 각 필드 값 읽기
                        if (freeElectiveObj instanceof Number) {
                            freeElective = ((Number) freeElectiveObj).intValue();
                        }
                        if (generalSelectionObj instanceof Number) {
                            generalSelection = ((Number) generalSelectionObj).intValue();
                        }
                        if (remainingCreditsObj instanceof Number) {
                            remainingCredits = ((Number) remainingCreditsObj).intValue();
                        }

                        // 합산
                        final int totalFreeElective = freeElective + generalSelection + remainingCredits;

                        // 자율선택으로 통일 (합산 값 설정)
                        if (generalSelection > 0 || remainingCredits > 0) {
                            updates.put("자율선택", totalFreeElective);
                            needsUpdate = true;
                            Log.d(TAG, docId + " - 자율선택 합산: " + freeElective + " + " + generalSelection + " + " + remainingCredits + " = " + totalFreeElective);
                        }

                        // 일반선택 필드 삭제
                        if (document.contains("일반선택")) {
                            updates.put("일반선택", com.google.firebase.firestore.FieldValue.delete());
                            needsUpdate = true;
                        }

                        // 잔여학점 필드 삭제
                        if (document.contains("잔여학점")) {
                            updates.put("잔여학점", com.google.firebase.firestore.FieldValue.delete());
                            needsUpdate = true;
                        }

                        if (needsUpdate) {
                            final String finalDocId = docId;
                            db.collection("graduation_requirements")
                                    .document(docId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        updatedCount[0]++;
                                        Log.d(TAG, "문서 업데이트 성공: " + finalDocId + " (자율선택=" + totalFreeElective + ")");

                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showUnifyFreeElectiveCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        skippedCount[0]++;
                                        Log.e(TAG, "문서 업데이트 실패: " + finalDocId, e);

                                        if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                            showUnifyFreeElectiveCompletionMessage(updatedCount[0], skippedCount[0]);
                                        }
                                    });
                        } else {
                            skippedCount[0]++;
                            Log.d(TAG, "문서 건너뜀 (이미 자율선택만 있음): " + docId);

                            if (updatedCount[0] + skippedCount[0] >= totalDocs) {
                                showUnifyFreeElectiveCompletionMessage(updatedCount[0], skippedCount[0]);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패", e);
                    Toast.makeText(this, "실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 자율선택 필드 통일 작업 완료 메시지 표시
     */
    private void showUnifyFreeElectiveCompletionMessage(int updated, int skipped) {
        String message = "자율선택 필드 통일 완료!\n\n" +
                "업데이트: " + updated + "개\n" +
                "건너뜀: " + skipped + "개\n\n" +
                "모든 문서가 '자율선택' 필드를 사용합니다.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("완료")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();

        Log.d(TAG, "자율선택 필드 통일 완료 - 업데이트: " + updated + ", 건너뜀: " + skipped);
    }

}
