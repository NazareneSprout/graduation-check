package sprout.app.sakmvp1;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDataManager {
    private static final String TAG = "UserDataManager";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public UserDataManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // 사용자별 학적 정보 저장
    public void saveUserAcademicInfo(String studentId, String department, String track) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "로그인된 사용자가 없습니다.");
            return;
        }

        Map<String, Object> userAcademicInfo = new HashMap<>();
        userAcademicInfo.put("userId", currentUser.getUid());
        userAcademicInfo.put("email", currentUser.getEmail());
        userAcademicInfo.put("studentId", studentId);
        userAcademicInfo.put("department", department);
        userAcademicInfo.put("track", track);
        userAcademicInfo.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        userAcademicInfo.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        db.collection("user_academic_info")
                .document(currentUser.getUid())
                .set(userAcademicInfo)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "사용자 학적 정보 저장 완료"))
                .addOnFailureListener(e -> Log.e(TAG, "사용자 학적 정보 저장 실패", e));
    }

    // 사용자별 수강 이력 저장
    public void saveUserCourseHistory(List<CourseInfo> courseHistory) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "로그인된 사용자가 없습니다.");
            return;
        }

        Map<String, Object> userCourseData = new HashMap<>();
        userCourseData.put("userId", currentUser.getUid());
        userCourseData.put("courseHistory", courseHistory);
        userCourseData.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        db.collection("user_course_history")
                .document(currentUser.getUid())
                .set(userCourseData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "사용자 수강 이력 저장 완료"))
                .addOnFailureListener(e -> Log.e(TAG, "사용자 수강 이력 저장 실패", e));
    }

    // 사용자별 졸업 요건 분석 결과 저장
    public void saveGraduationAnalysisResult(String studentId, String department, String track,
                                           Map<String, Object> analysisResult) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "로그인된 사용자가 없습니다.");
            return;
        }

        Map<String, Object> userAnalysisData = new HashMap<>();
        userAnalysisData.put("userId", currentUser.getUid());
        userAnalysisData.put("studentId", studentId);
        userAnalysisData.put("department", department);
        userAnalysisData.put("track", track);
        userAnalysisData.put("analysisResult", analysisResult);
        userAnalysisData.put("analyzedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        db.collection("user_graduation_analysis")
                .document(currentUser.getUid())
                .set(userAnalysisData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "졸업 요건 분석 결과 저장 완료"))
                .addOnFailureListener(e -> Log.e(TAG, "졸업 요건 분석 결과 저장 실패", e));
    }

    // 사용자 학적 정보 조회
    public interface OnUserAcademicInfoLoadedListener {
        void onSuccess(Map<String, Object> academicInfo);
        void onFailure(Exception e);
        void onNotFound();
    }

    public void loadUserAcademicInfo(OnUserAcademicInfoLoadedListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("로그인된 사용자가 없습니다."));
            return;
        }

        db.collection("user_academic_info")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onSuccess(documentSnapshot.getData());
                    } else {
                        listener.onNotFound();
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // 수강 정보 데이터 클래스
    public static class CourseInfo {
        public String courseCode;
        public String courseName;
        public String courseType; // 전공필수, 전공선택, 교양필수, 교양선택
        public int credits;
        public String grade;
        public String semester; // 2023-1, 2023-2 형식
        public boolean isCompleted;

        public CourseInfo() {} // Firestore용 빈 생성자

        public CourseInfo(String courseCode, String courseName, String courseType,
                         int credits, String grade, String semester, boolean isCompleted) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.courseType = courseType;
            this.credits = credits;
            this.grade = grade;
            this.semester = semester;
            this.isCompleted = isCompleted;
        }
    }
}