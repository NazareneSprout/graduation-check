package sprout.app.sakmvp1.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 대체과목 규칙
 * 폐강된 과목과 그것을 대체할 수 있는 과목들의 관계를 정의
 */
public class ReplacementRule {
    private CourseInfo discontinuedCourse;
    private List<CourseInfo> replacementCourses;
    private String note;
    private Timestamp createdAt;
    private String scope;  // "document" 또는 "department"

    // Firestore 역직렬화를 위한 빈 생성자
    public ReplacementRule() {
        this.replacementCourses = new ArrayList<>();
        this.scope = "document";  // 기본값: 해당 문서에만 적용
    }

    public ReplacementRule(CourseInfo discontinuedCourse, List<CourseInfo> replacementCourses) {
        this.discontinuedCourse = discontinuedCourse;
        this.replacementCourses = replacementCourses != null ? replacementCourses : new ArrayList<>();
    }

    /**
     * 수강 과목 목록에서 이 대체과목 규칙을 적용할 수 있는지 확인
     * @param takenCourseNames 사용자가 수강한 과목 이름 목록
     * @return 적용 가능하면 true
     */
    public boolean canApply(List<String> takenCourseNames) {
        if (discontinuedCourse == null || takenCourseNames == null) {
            return false;
        }

        // 폐강된 과목을 직접 수강했으면 대체 불필요
        if (takenCourseNames.contains(discontinuedCourse.getName())) {
            return false;
        }

        // 대체 과목 중 하나라도 수강했으면 적용 가능
        for (CourseInfo replacement : replacementCourses) {
            if (takenCourseNames.contains(replacement.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 실제로 수강한 대체 과목 이름 반환
     * 여러 개의 대체과목을 수강한 경우, 첫 번째로 발견된 것만 반환 (중복 인정 방지)
     *
     * @param takenCourseNames 수강 과목 목록
     * @return 수강한 대체 과목 이름, 없으면 null
     */
    public String getTakenReplacementCourse(List<String> takenCourseNames) {
        if (takenCourseNames == null) {
            return null;
        }

        // 첫 번째로 매칭되는 대체과목만 반환 (중복 인정 방지)
        for (CourseInfo replacement : replacementCourses) {
            if (takenCourseNames.contains(replacement.getName())) {
                return replacement.getName();
            }
        }

        return null;
    }

    /**
     * 실제로 수강한 모든 대체 과목 목록 반환 (디버깅/로깅용)
     * @param takenCourseNames 수강 과목 목록
     * @return 수강한 모든 대체 과목 이름 목록
     */
    public List<String> getAllTakenReplacementCourses(List<String> takenCourseNames) {
        List<String> taken = new ArrayList<>();
        if (takenCourseNames == null) {
            return taken;
        }

        for (CourseInfo replacement : replacementCourses) {
            if (takenCourseNames.contains(replacement.getName())) {
                taken.add(replacement.getName());
            }
        }

        return taken;
    }

    // Getters and Setters
    public CourseInfo getDiscontinuedCourse() {
        return discontinuedCourse;
    }

    public void setDiscontinuedCourse(CourseInfo discontinuedCourse) {
        this.discontinuedCourse = discontinuedCourse;
    }

    public List<CourseInfo> getReplacementCourses() {
        return replacementCourses;
    }

    public void setReplacementCourses(List<CourseInfo> replacementCourses) {
        this.replacementCourses = replacementCourses;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return discontinuedCourse != null ?
            discontinuedCourse.getName() + " → " + replacementCourses.size() + "개 대체과목" :
            "ReplacementRule (empty)";
    }

    /**
     * 과목 정보 (대체과목 규칙에서 사용)
     */
    public static class CourseInfo {
        private String name;
        private String category;
        private int credits;

        // Firestore 역직렬화를 위한 빈 생성자
        public CourseInfo() {
        }

        public CourseInfo(String name, String category, int credits) {
            this.name = name;
            this.category = category;
            this.credits = credits;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public int getCredits() {
            return credits;
        }

        public void setCredits(int credits) {
            this.credits = credits;
        }

        @Override
        public String toString() {
            return name + " (" + credits + "학점, " + category + ")";
        }
    }
}
