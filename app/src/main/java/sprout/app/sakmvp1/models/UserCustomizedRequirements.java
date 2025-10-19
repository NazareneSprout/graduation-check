package sprout.app.sakmvp1.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자별 커스터마이즈된 졸업요건
 * 사용자가 기본 졸업요건을 기반으로 개인화한 설정을 저장
 * Firestore user_customized_requirements 컬렉션과 매핑
 */
public class UserCustomizedRequirements {

    private String userId;

    // 기준 문서 ID
    private String majorDocumentId;        // 전공 기준 문서 (예: "2024학번_컴퓨터공학과_일반")
    private String generalDocumentId;      // 교양 기준 문서 (예: "2024학번_공통교양")

    // 전공 커스터마이징
    private DocumentCustomization majorCustomizations;

    // 교양 커스터마이징
    private DocumentCustomization generalCustomizations;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Firestore 역직렬화를 위한 빈 생성자
    public UserCustomizedRequirements() {
        this.majorCustomizations = new DocumentCustomization();
        this.generalCustomizations = new DocumentCustomization();
    }

    public UserCustomizedRequirements(String userId) {
        this.userId = userId;
        this.majorCustomizations = new DocumentCustomization();
        this.generalCustomizations = new DocumentCustomization();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMajorDocumentId() {
        return majorDocumentId;
    }

    public void setMajorDocumentId(String majorDocumentId) {
        this.majorDocumentId = majorDocumentId;
    }

    public String getGeneralDocumentId() {
        return generalDocumentId;
    }

    public void setGeneralDocumentId(String generalDocumentId) {
        this.generalDocumentId = generalDocumentId;
    }

    public DocumentCustomization getMajorCustomizations() {
        return majorCustomizations;
    }

    public void setMajorCustomizations(DocumentCustomization majorCustomizations) {
        this.majorCustomizations = majorCustomizations;
    }

    public DocumentCustomization getGeneralCustomizations() {
        return generalCustomizations;
    }

    public void setGeneralCustomizations(DocumentCustomization generalCustomizations) {
        this.generalCustomizations = generalCustomizations;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 문서별 커스터마이징 정보
     */
    public static class DocumentCustomization {
        // 영역별 학점 수정 사항 (예: {"전공필수": 42, "전공선택": 18})
        private Map<String, Integer> creditModifications;

        // 과목 수정 사항
        private List<CourseModification> courseModifications;

        public DocumentCustomization() {
            this.creditModifications = new HashMap<>();
            this.courseModifications = new ArrayList<>();
        }

        public Map<String, Integer> getCreditModifications() {
            return creditModifications;
        }

        public void setCreditModifications(Map<String, Integer> creditModifications) {
            this.creditModifications = creditModifications;
        }

        public List<CourseModification> getCourseModifications() {
            return courseModifications;
        }

        public void setCourseModifications(List<CourseModification> courseModifications) {
            this.courseModifications = courseModifications;
        }

        /**
         * 특정 영역의 학점 수정
         */
        public void modifyCredit(String categoryName, int credits) {
            creditModifications.put(categoryName, credits);
        }

        /**
         * 과목 추가
         */
        public void addCourse(String categoryName, String courseName, int credits) {
            CourseModification modification = new CourseModification();
            modification.setModificationType("ADD");
            modification.setCategoryName(categoryName);
            modification.setCourseName(courseName);
            modification.setCredits(credits);
            courseModifications.add(modification);
        }

        /**
         * 과목 삭제
         */
        public void deleteCourse(String categoryName, String courseName) {
            CourseModification modification = new CourseModification();
            modification.setModificationType("DELETE");
            modification.setCategoryName(categoryName);
            modification.setCourseName(courseName);
            courseModifications.add(modification);
        }

        /**
         * 과목 수정
         */
        public void modifyCourse(String categoryName, String oldCourseName,
                                String newCourseName, int newCredits) {
            CourseModification modification = new CourseModification();
            modification.setModificationType("MODIFY");
            modification.setCategoryName(categoryName);
            modification.setOldCourseName(oldCourseName);
            modification.setCourseName(newCourseName);
            modification.setCredits(newCredits);
            courseModifications.add(modification);
        }

        /**
         * 커스터마이징이 있는지 확인
         */
        public boolean hasCustomizations() {
            return !creditModifications.isEmpty() || !courseModifications.isEmpty();
        }
    }

    /**
     * 과목 수정 정보
     */
    public static class CourseModification {
        private String modificationType;  // ADD, DELETE, MODIFY
        private String categoryName;      // 전공필수, 전공선택, 교양필수 등
        private String courseName;        // 새 과목명 (또는 대상 과목명)
        private String oldCourseName;     // 기존 과목명 (MODIFY인 경우)
        private int credits;              // 학점

        public CourseModification() {
        }

        public String getModificationType() {
            return modificationType;
        }

        public void setModificationType(String modificationType) {
            this.modificationType = modificationType;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getCourseName() {
            return courseName;
        }

        public void setCourseName(String courseName) {
            this.courseName = courseName;
        }

        public String getOldCourseName() {
            return oldCourseName;
        }

        public void setOldCourseName(String oldCourseName) {
            this.oldCourseName = oldCourseName;
        }

        public int getCredits() {
            return credits;
        }

        public void setCredits(int credits) {
            this.credits = credits;
        }

        @Override
        public String toString() {
            if ("ADD".equals(modificationType)) {
                return "[추가] " + categoryName + " - " + courseName + " (" + credits + "학점)";
            } else if ("DELETE".equals(modificationType)) {
                return "[삭제] " + categoryName + " - " + courseName;
            } else if ("MODIFY".equals(modificationType)) {
                return "[수정] " + categoryName + " - " + oldCourseName + " → " +
                       courseName + " (" + credits + "학점)";
            }
            return "Unknown modification";
        }
    }
}
