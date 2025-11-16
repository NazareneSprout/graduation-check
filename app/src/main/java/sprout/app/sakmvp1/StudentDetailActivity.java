package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import sprout.app.sakmvp1.models.Student;

/**
 * 학생 상세 정보 화면 (관리자용)
 * 학생의 기본 정보와 수강 과목을 카테고리별로 표시
 */
public class StudentDetailActivity extends BaseActivity {

    private static final String TAG = "StudentDetailActivity";
    public static final String EXTRA_STUDENT = "extra_student";

    private MaterialToolbar toolbar;
    private TextView tvStudentName, tvStudentEmail, tvStudentYear, tvDepartment, tvTrack;
    private LinearLayout layoutTrack;
    private ProgressBar progressBar;
    private RecyclerView rvCourses;
    private TextView tvNoData;

    // 추가 졸업요건 뷰
    private com.google.android.material.card.MaterialCardView cardAdditionalRequirements;
    private LinearLayout layoutChapel, layoutTlc, layoutMileage, layoutExtraGrad;
    private TextView tvChapelCount, tvTlcCount, tvMileageStatus, tvExtraGradStatus;

    private FirebaseFirestore db;
    private Student student;
    private CourseCategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_detail);

        db = FirebaseFirestore.getInstance();

        // Intent에서 학생 정보 가져오기
        student = getIntent().getParcelableExtra(EXTRA_STUDENT);
        if (student == null) {
            Toast.makeText(this, "학생 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        displayStudentInfo();
        loadStudentCourses();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvStudentName = findViewById(R.id.tv_student_name);
        tvStudentEmail = findViewById(R.id.tv_student_email);
        tvStudentYear = findViewById(R.id.tv_student_year);
        tvDepartment = findViewById(R.id.tv_department);
        tvTrack = findViewById(R.id.tv_track);
        layoutTrack = findViewById(R.id.layout_track);
        progressBar = findViewById(R.id.progress_bar);
        rvCourses = findViewById(R.id.rv_courses);
        tvNoData = findViewById(R.id.tv_no_data);

        // 추가 졸업요건 뷰
        cardAdditionalRequirements = findViewById(R.id.card_additional_requirements);
        layoutChapel = findViewById(R.id.layout_chapel);
        layoutTlc = findViewById(R.id.layout_tlc);
        layoutMileage = findViewById(R.id.layout_mileage);
        layoutExtraGrad = findViewById(R.id.layout_extra_grad);
        tvChapelCount = findViewById(R.id.tv_chapel_count);
        tvTlcCount = findViewById(R.id.tv_tlc_count);
        tvMileageStatus = findViewById(R.id.tv_mileage_status);
        tvExtraGradStatus = findViewById(R.id.tv_extra_grad_status);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("학생 상세 정보");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new CourseCategoryAdapter();
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        rvCourses.setAdapter(adapter);
    }

    private void displayStudentInfo() {
        Log.d(TAG, "========== 학생 정보 표시 ==========");
        Log.d(TAG, "이름: " + student.getName());
        Log.d(TAG, "이메일: " + student.getEmail());
        Log.d(TAG, "학번: " + student.getStudentYear());
        Log.d(TAG, "학과: " + student.getDepartment());
        Log.d(TAG, "트랙: " + student.getTrack());

        tvStudentName.setText(student.getName());
        tvStudentEmail.setText(student.getEmail() != null ? student.getEmail() : "이메일 없음");
        tvStudentYear.setText(student.getDisplayYear() + "학번");
        tvDepartment.setText(student.getDepartment() != null ? student.getDepartment() : "-");

        // 트랙이 없으면 트랙 레이아웃 전체를 숨김
        if (student.getTrack() != null && !student.getTrack().isEmpty() && !student.getTrack().equals("-")) {
            tvTrack.setText(student.getTrack());
            layoutTrack.setVisibility(View.VISIBLE);
        } else {
            layoutTrack.setVisibility(View.GONE);
        }
    }

    /**
     * 학생의 수강 과목 로드
     */
    private void loadStudentCourses() {
        showLoading(true);
        Log.d(TAG, "========== 수강 과목 로딩 시작 ==========");
        Log.d(TAG, "학생 ID: " + student.getUserId());
        Log.d(TAG, "학생 이름: " + student.getName());

        // users/{userId}/current_graduation_analysis/latest 문서에서 과목 데이터 가져오기
        db.collection("users")
                .document(student.getUserId())
                .collection("current_graduation_analysis")
                .document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<CourseItem> allCourses = new ArrayList<>();

                    if (!documentSnapshot.exists()) {
                        Log.d(TAG, "current_graduation_analysis 문서가 없음. users 문서에서 시도");
                        loadCoursesFromUserDocument();
                        return;
                    }

                    Log.d(TAG, "✅ current_graduation_analysis/latest 문서 발견");

                    // courses 배열에서 과목 데이터 추출
                    List<Map<String, Object>> coursesList = (List<Map<String, Object>>) documentSnapshot.get("courses");
                    if (coursesList != null) {
                        Log.d(TAG, "courses 배열 크기: " + coursesList.size());

                        for (Map<String, Object> courseMap : coursesList) {
                            String category = (String) courseMap.get("category");
                            String name = (String) courseMap.get("name");
                            Object creditsObj = courseMap.get("credits");
                            int credits = 0;
                            if (creditsObj instanceof Long) {
                                credits = ((Long) creditsObj).intValue();
                            } else if (creditsObj instanceof Integer) {
                                credits = (Integer) creditsObj;
                            }

                            Log.d(TAG, "  과목: " + name + " / 카테고리: " + category + " / 학점: " + credits);

                            if (category != null && name != null) {
                                allCourses.add(new CourseItem(category, name, credits));
                            }
                        }
                    }

                    Log.d(TAG, "✅ current_graduation_analysis에서 로드 성공: " + allCourses.size() + "개");

                    if (allCourses.isEmpty()) {
                        // current_graduation_analysis가 비어있으면 users/{userId} 문서 시도
                        loadCoursesFromUserDocument();
                    } else {
                        displayCourses(allCourses);
                        showLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "수강 과목 로드 실패", e);
                    // 실패하면 users 문서에서 시도
                    loadCoursesFromUserDocument();
                });
    }

    /**
     * users/{userId} 문서에서 직접 과목 데이터 가져오기 (대체 방법)
     */
    private void loadCoursesFromUserDocument() {
        Log.d(TAG, "========== 대체 방법: users 문서에서 로드 시도 ==========");
        db.collection("users")
                .document(student.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<CourseItem> allCourses = new ArrayList<>();

                    Log.d(TAG, "users 문서 존재 여부: " + documentSnapshot.exists());

                    // savedGraduationAnalysis 필드에서 과목 데이터 가져오기
                    Map<String, Object> savedAnalysis = (Map<String, Object>) documentSnapshot.get("savedGraduationAnalysis");
                    if (savedAnalysis != null) {
                        Log.d(TAG, "✅ savedGraduationAnalysis 필드 발견");
                        List<Map<String, Object>> coursesList = (List<Map<String, Object>>) savedAnalysis.get("courses");
                        if (coursesList != null) {
                            Log.d(TAG, "✅ savedGraduationAnalysis.courses 배열 발견: " + coursesList.size() + "개");

                            for (Map<String, Object> courseMap : coursesList) {
                                String category = (String) courseMap.get("category");
                                String name = (String) courseMap.get("name");
                                Object creditsObj = courseMap.get("credits");
                                int credits = 0;
                                if (creditsObj instanceof Long) {
                                    credits = ((Long) creditsObj).intValue();
                                } else if (creditsObj instanceof Integer) {
                                    credits = (Integer) creditsObj;
                                }

                                Log.d(TAG, "  과목: " + name + " / 카테고리: " + category + " / 학점: " + credits);

                                if (category != null && name != null) {
                                    allCourses.add(new CourseItem(category, name, credits));
                                }
                            }
                        } else {
                            Log.e(TAG, "❌ savedGraduationAnalysis.courses 배열이 null입니다");
                        }
                    } else {
                        Log.e(TAG, "❌ savedGraduationAnalysis 필드가 존재하지 않습니다");
                    }

                    Log.d(TAG, "✅ users 문서에서 최종 로드: " + allCourses.size() + "개");
                    displayCourses(allCourses);

                    // 추가 졸업요건 데이터 로드 및 표시
                    if (savedAnalysis != null) {
                        Map<String, Object> additionalReqs = (Map<String, Object>) savedAnalysis.get("additionalRequirements");
                        displayAdditionalRequirements(additionalReqs);
                    }

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 사용자 문서 로드 실패", e);
                    showLoading(false);
                    tvNoData.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "과목 데이터를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 추가 졸업요건 표시
     */
    private void displayAdditionalRequirements(Map<String, Object> additionalReqs) {
        if (additionalReqs == null || additionalReqs.isEmpty()) {
            Log.d(TAG, "추가 졸업요건 데이터 없음");
            cardAdditionalRequirements.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "========== 추가 졸업요건 표시 ==========");
        boolean hasAnyRequirement = false;

        // 채플 횟수
        if (additionalReqs.containsKey("chapelCount")) {
            Object chapelObj = additionalReqs.get("chapelCount");
            int chapelCount = 0;
            if (chapelObj instanceof Long) {
                chapelCount = ((Long) chapelObj).intValue();
            } else if (chapelObj instanceof Integer) {
                chapelCount = (Integer) chapelObj;
            }

            if (chapelCount > 0) {
                tvChapelCount.setText(chapelCount + "회");
                layoutChapel.setVisibility(View.VISIBLE);
                hasAnyRequirement = true;
                Log.d(TAG, "채플: " + chapelCount + "회");
            }
        }

        // TLC 횟수
        if (additionalReqs.containsKey("tlcCount")) {
            Object tlcObj = additionalReqs.get("tlcCount");
            int tlcCount = 0;
            if (tlcObj instanceof Long) {
                tlcCount = ((Long) tlcObj).intValue();
            } else if (tlcObj instanceof Integer) {
                tlcCount = (Integer) tlcObj;
            }

            if (tlcCount > 0) {
                tvTlcCount.setText(tlcCount + "회");
                layoutTlc.setVisibility(View.VISIBLE);
                hasAnyRequirement = true;
                Log.d(TAG, "TLC: " + tlcCount + "회");
            }
        }

        // 마일리지
        if (additionalReqs.containsKey("mileageCompleted")) {
            Object mileageObj = additionalReqs.get("mileageCompleted");
            boolean mileageCompleted = false;
            if (mileageObj instanceof Boolean) {
                mileageCompleted = (Boolean) mileageObj;
            }

            tvMileageStatus.setText(mileageCompleted ? "충족" : "미충족");
            tvMileageStatus.setTextColor(getResources().getColor(
                    mileageCompleted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
            layoutMileage.setVisibility(View.VISIBLE);
            hasAnyRequirement = true;
            Log.d(TAG, "마일리지: " + (mileageCompleted ? "충족" : "미충족"));
        }

        // 추가 졸업요건
        if (additionalReqs.containsKey("extraGradCompleted")) {
            Object extraGradObj = additionalReqs.get("extraGradCompleted");
            boolean extraGradCompleted = false;
            if (extraGradObj instanceof Boolean) {
                extraGradCompleted = (Boolean) extraGradObj;
            }

            tvExtraGradStatus.setText(extraGradCompleted ? "충족" : "미충족");
            tvExtraGradStatus.setTextColor(getResources().getColor(
                    extraGradCompleted ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
            layoutExtraGrad.setVisibility(View.VISIBLE);
            hasAnyRequirement = true;
            Log.d(TAG, "추가 요건: " + (extraGradCompleted ? "충족" : "미충족"));
        }

        // 하나라도 표시할 요건이 있으면 카드 표시
        if (hasAnyRequirement) {
            cardAdditionalRequirements.setVisibility(View.VISIBLE);
        } else {
            cardAdditionalRequirements.setVisibility(View.GONE);
        }
    }

    /**
     * 과목 목록을 카테고리별로 그룹화하여 표시
     */
    private void displayCourses(List<CourseItem> courses) {
        if (courses.isEmpty()) {
            tvNoData.setVisibility(View.VISIBLE);
            rvCourses.setVisibility(View.GONE);
            return;
        }

        // 카테고리별로 그룹화
        Map<String, List<CourseItem>> coursesByCategory = new LinkedHashMap<>();

        // 카테고리 순서 정의
        String[] categoryOrder = {
            "전공필수", "전공선택", "학부공통", "전공심화",
            "교양필수", "교양선택", "소양",
            "일반선택", "잔여학점"
        };

        for (String category : categoryOrder) {
            coursesByCategory.put(category, new ArrayList<>());
        }

        // 과목을 카테고리별로 분류
        for (CourseItem course : courses) {
            String category = course.getCategory();
            if (!coursesByCategory.containsKey(category)) {
                coursesByCategory.put(category, new ArrayList<>());
            }
            coursesByCategory.get(category).add(course);
        }

        // 빈 카테고리 제거
        List<CourseCategory> categories = new ArrayList<>();
        for (Map.Entry<String, List<CourseItem>> entry : coursesByCategory.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                categories.add(new CourseCategory(entry.getKey(), entry.getValue()));
            }
        }

        adapter.setCategories(categories);
        tvNoData.setVisibility(View.GONE);
        rvCourses.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            rvCourses.setVisibility(View.VISIBLE);
        }
    }

    // ========== 데이터 모델 ==========

    /**
     * 과목 정보
     */
    static class CourseItem {
        private String category;
        private String name;
        private int credits;

        public CourseItem(String category, String name, int credits) {
            this.category = category;
            this.name = name;
            this.credits = credits;
        }

        public String getCategory() { return category; }
        public String getName() { return name; }
        public int getCredits() { return credits; }
    }

    /**
     * 카테고리와 해당 과목 목록
     */
    static class CourseCategory {
        private String categoryName;
        private List<CourseItem> courses;

        public CourseCategory(String categoryName, List<CourseItem> courses) {
            this.categoryName = categoryName;
            this.courses = courses;
        }

        public String getCategoryName() { return categoryName; }
        public List<CourseItem> getCourses() { return courses; }
        public int getTotalCredits() {
            int total = 0;
            for (CourseItem course : courses) {
                total += course.getCredits();
            }
            return total;
        }
    }

    // ========== RecyclerView Adapter ==========

    static class CourseCategoryAdapter extends RecyclerView.Adapter<CourseCategoryAdapter.ViewHolder> {

        private List<CourseCategory> categories = new ArrayList<>();

        public void setCategories(List<CourseCategory> categories) {
            this.categories = categories;
            notifyDataSetChanged();
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_course_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            CourseCategory category = categories.get(position);
            holder.bind(category);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvCategoryName, tvCourseCount;
            private LinearLayout layoutCourses;

            public ViewHolder(@androidx.annotation.NonNull View itemView) {
                super(itemView);
                tvCategoryName = itemView.findViewById(R.id.tv_category_name);
                tvCourseCount = itemView.findViewById(R.id.tv_course_count);
                layoutCourses = itemView.findViewById(R.id.layout_courses);
            }

            public void bind(CourseCategory category) {
                tvCategoryName.setText(category.getCategoryName());
                tvCourseCount.setText(category.getCourses().size() + "과목 · " +
                        category.getTotalCredits() + "학점");

                // 과목 목록 추가
                layoutCourses.removeAllViews();
                for (CourseItem course : category.getCourses()) {
                    TextView courseView = new TextView(itemView.getContext());
                    courseView.setText("• " + course.getName() + " (" + course.getCredits() + "학점)");
                    courseView.setTextSize(14);
                    courseView.setPadding(0, 8, 0, 8);
                    layoutCourses.addView(courseView);
                }
            }
        }
    }
}
