package sprout.app.sakmvp1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 교수/조교 전용 학사 관리 시스템 Activity
 * 졸업요건, 대체과목, 학생 데이터, 공지사항 관리 기능 제공
 */
public class AdminActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private MaterialCardView cardGraduationRequirements, cardMajorDocuments, cardGeneralDocuments;
    private MaterialCardView cardSubstituteCourses, cardStudentData, cardNotices;
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
        cardMajorDocuments = findViewById(R.id.card_major_documents);
        cardGeneralDocuments = findViewById(R.id.card_general_documents);
        cardSubstituteCourses = findViewById(R.id.card_substitute_courses);
        cardStudentData = findViewById(R.id.card_student_data);
        cardNotices = findViewById(R.id.card_notices);

        // 로그아웃 버튼
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

        // 전공 문서 관리
        cardMajorDocuments.setOnClickListener(v -> {
            Intent intent = new Intent(this, MajorDocumentManagementActivity.class);
            startActivity(intent);
        });

        // 교양 문서 관리
        cardGeneralDocuments.setOnClickListener(v -> {
            Intent intent = new Intent(this, EducationDocumentManagementActivity.class);
            startActivity(intent);
        });

        // 대체과목 관리
        cardSubstituteCourses.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReplacementCourseManagementActivity.class);
            startActivity(intent);
        });

        // 학생 데이터 조회
        cardStudentData.setOnClickListener(v -> {
            Toast.makeText(this, "학생 데이터 조회 기능 - 개발 예정", Toast.LENGTH_SHORT).show();
            // TODO: StudentDataActivity 구현
        });

        // 공지사항 관리
        cardNotices.setOnClickListener(v -> {
            Toast.makeText(this, "공지사항 관리 기능 - 개발 예정", Toast.LENGTH_SHORT).show();
            // TODO: NoticesActivity 구현
        });

        // 일반 모드로 돌아가기
        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_admin", false).apply();
            Toast.makeText(this, "일반 모드로 전환되었습니다", Toast.LENGTH_SHORT).show();
            finish();
        });
    }


}
