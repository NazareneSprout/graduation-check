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
    private MaterialButton btnLogout, btnFixFirestore;

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

        // 버튼들
        btnFixFirestore = findViewById(R.id.btn_fix_firestore);
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

        // 임시: Firestore 데이터 수정
        btnFixFirestore.setOnClickListener(v -> {
            fixDepartmentCommonCategory();
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
     * "학부공통필수" 카테고리를 "학부공통"으로 병합
     * rules 안의 각 학기별 과목 리스트에서 카테고리가 "학부공통필수"인 것을 "학부공통"으로 변경
     */
    @SuppressWarnings("unchecked")
    private void fixDepartmentCommonCategory() {
        String docId = "IT학부_인공지능_2020";
        Toast.makeText(this, "🔧 학부공통필수 → 학부공통 병합 시작...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "========================================");
        Log.d(TAG, "학부공통필수 → 학부공통 병합 시작");
        Log.d(TAG, "문서: " + docId);
        Log.d(TAG, "========================================");

        db.collection("graduation_requirements").document(docId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    String msg = "❌ 문서가 존재하지 않습니다: " + docId;
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, msg);
                    return;
                }

                Map<String, Object> data = documentSnapshot.getData();
                if (data == null) {
                    String msg = "❌ 문서 데이터가 null입니다";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, msg);
                    return;
                }

                // rules 필드 확인
                Map<String, Object> rules = (Map<String, Object>) data.get("rules");
                if (rules == null) {
                    String msg = "❌ rules 필드가 없습니다";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, msg);
                    return;
                }

                Log.d(TAG, "rules 하위 학기들: " + rules.keySet().toString());

                // 업데이트할 필드들을 담을 맵
                Map<String, Object> updates = new HashMap<>();
                final int[] totalChanges = {0};  // lambda에서 사용하기 위해 배열로 선언

                // 각 학기를 순회하면서 과목 리스트의 카테고리 수정
                for (String semester : rules.keySet()) {
                    Object semesterObj = rules.get(semester);

                    // 각 학기는 Map<카테고리, List<과목>> 구조
                    if (!(semesterObj instanceof Map)) {
                        Log.w(TAG, "⚠️ " + semester + "는 Map 타입이 아닙니다: " + semesterObj.getClass().getSimpleName());
                        continue;
                    }

                    Map<String, Object> semesterMap = (Map<String, Object>) semesterObj;
                    Log.d(TAG, "📚 " + semester + " 카테고리들: " + semesterMap.keySet());

                    // "학부공통필수" 카테고리가 있는지 확인
                    if (semesterMap.containsKey("학부공통필수")) {
                        Object categoryCoursesObj = semesterMap.get("학부공통필수");
                        if (categoryCoursesObj instanceof List) {
                            List<Map<String, Object>> courses = (List<Map<String, Object>>) categoryCoursesObj;
                            Log.d(TAG, "✓ " + semester + "에서 '학부공통필수' 카테고리 발견, 과목 수: " + courses.size());

                            // "학부공통" 카테고리에 병합
                            List<Map<String, Object>> commonCourses = (List<Map<String, Object>>) semesterMap.get("학부공통");
                            if (commonCourses == null) {
                                commonCourses = new ArrayList<>();
                            }

                            // 학부공통필수 과목들을 학부공통에 추가
                            for (Map<String, Object> course : courses) {
                                Log.d(TAG, "  └─ " + course.get("name") + " 병합");
                                commonCourses.add(course);
                                totalChanges[0]++;
                            }

                            // 업데이트: 학부공통필수 삭제, 학부공통에 추가
                            updates.put("rules." + semester + ".학부공통필수", com.google.firebase.firestore.FieldValue.delete());
                            updates.put("rules." + semester + ".학부공통", commonCourses);
                        }
                    }
                }

                if (totalChanges[0] == 0) {
                    String msg = "⚠️ 변경할 '학부공통필수' 과목이 없습니다";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    Log.w(TAG, msg);
                    return;
                }

                Log.d(TAG, "총 " + totalChanges[0] + "개 과목의 카테고리 변경 예정");

                // 최상위 "학부공통필수" 필드도 삭제 (있다면)
                if (data.containsKey("학부공통필수")) {
                    updates.put("학부공통필수", com.google.firebase.firestore.FieldValue.delete());
                    Log.d(TAG, "✓ 최상위 '학부공통필수' 필드도 삭제 예정");
                }

                // Firestore에 업데이트
                db.collection("graduation_requirements").document(docId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        String msg = "✅ Firestore 업데이트 성공!\n\n" + totalChanges[0] + "개 과목의 카테고리를 '학부공통필수' → '학부공통'으로 변경했습니다.\n\n이제 졸업요건 검사를 다시 실행하세요.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "✅ Firestore 업데이트 성공");
                        Log.d(TAG, "- 총 " + totalChanges[0] + "개 과목 카테고리 변경");
                        Log.d(TAG, "========================================");
                    })
                    .addOnFailureListener(e -> {
                        String msg = "❌ Firestore 업데이트 실패: " + e.getMessage();
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Firestore 업데이트 실패", e);
                    });
            })
            .addOnFailureListener(e -> {
                String msg = "❌ 문서 로드 실패: " + e.getMessage();
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "문서 로드 실패", e);
            });
    }

}
