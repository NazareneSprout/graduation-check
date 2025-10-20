package sprout.app.sakmvp1.managers;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import sprout.app.sakmvp1.models.GraduationRules;
import sprout.app.sakmvp1.models.UserCustomizedRequirements;
import sprout.app.sakmvp1.models.RequirementCategory;
import sprout.app.sakmvp1.models.CourseRequirement;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자별 커스터마이즈된 졸업요건 관리
 */
public class CustomizedRequirementsManager {
    private static final String TAG = "CustomizedReqManager";
    private static final String COLLECTION_NAME = "user_customized_requirements";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public CustomizedRequirementsManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * 사용자의 커스터마이즈된 요건 로드
     */
    public void loadUserCustomizations(OnLoadListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        db.collection(COLLECTION_NAME)
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    UserCustomizedRequirements customizations =
                        documentSnapshot.toObject(UserCustomizedRequirements.class);
                    listener.onSuccess(customizations);
                } else {
                    // 커스터마이징이 없으면 빈 객체 반환
                    UserCustomizedRequirements empty = new UserCustomizedRequirements(currentUser.getUid());
                    listener.onSuccess(empty);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "커스터마이징 로드 실패", e);
                listener.onFailure(e);
            });
    }

    /**
     * 사용자의 커스터마이즈된 요건 저장
     */
    public void saveUserCustomizations(UserCustomizedRequirements customizations,
                                      OnSaveListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        customizations.setUserId(currentUser.getUid());
        customizations.setUpdatedAt(Timestamp.now());

        if (customizations.getCreatedAt() == null) {
            customizations.setCreatedAt(Timestamp.now());
        }

        db.collection(COLLECTION_NAME)
            .document(currentUser.getUid())
            .set(customizations)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "커스터마이징 저장 성공");
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "커스터마이징 저장 실패", e);
                listener.onFailure(e);
            });
    }

    /**
     * 기본 GraduationRules에 사용자 커스터마이징을 적용
     */
    public GraduationRules applyCustomizations(GraduationRules baseRules,
                                              UserCustomizedRequirements customizations,
                                              String documentType) {
        if (baseRules == null || customizations == null) {
            return baseRules;
        }

        // 기본 규칙 복사 (원본 보존)
        GraduationRules customizedRules = copyGraduationRules(baseRules);

        UserCustomizedRequirements.DocumentCustomization docCustomization;
        if ("major".equals(documentType)) {
            docCustomization = customizations.getMajorCustomizations();
        } else if ("general".equals(documentType)) {
            docCustomization = customizations.getGeneralCustomizations();
        } else {
            return customizedRules;
        }

        if (docCustomization == null || !docCustomization.hasCustomizations()) {
            return customizedRules;
        }

        Log.d(TAG, "커스터마이징 적용 중: " + documentType);

        // 1. 학점 수정 사항 적용
        applyCreditModifications(customizedRules, docCustomization);

        // 2. 과목 수정 사항 적용
        applyCourseModifications(customizedRules, docCustomization);

        return customizedRules;
    }

    /**
     * 학점 수정 사항 적용
     */
    private void applyCreditModifications(GraduationRules rules,
                                         UserCustomizedRequirements.DocumentCustomization customization) {
        if (rules.getCreditRequirements() == null || customization.getCreditModifications().isEmpty()) {
            return;
        }

        for (String categoryName : customization.getCreditModifications().keySet()) {
            int modifiedCredits = customization.getCreditModifications().get(categoryName);

            // CreditRequirements의 한글 메서드 사용
            switch (categoryName) {
                case "전공필수":
                    rules.getCreditRequirements().set전공필수(modifiedCredits);
                    break;
                case "전공선택":
                    rules.getCreditRequirements().set전공선택(modifiedCredits);
                    break;
                case "학부공통":
                    rules.getCreditRequirements().set학부공통(modifiedCredits);
                    break;
                case "전공심화":
                    rules.getCreditRequirements().set전공심화(modifiedCredits);
                    break;
                case "교양필수":
                    rules.getCreditRequirements().set교양필수(modifiedCredits);
                    break;
                case "교양선택":
                    rules.getCreditRequirements().set교양선택(modifiedCredits);
                    break;
                case "소양":
                    rules.getCreditRequirements().set소양(modifiedCredits);
                    break;
            }
            Log.d(TAG, "  학점 수정: " + categoryName + " = " + modifiedCredits);
        }
    }

    /**
     * 과목 수정 사항 적용
     */
    private void applyCourseModifications(GraduationRules rules,
                                         UserCustomizedRequirements.DocumentCustomization customization) {
        if (customization.getCourseModifications().isEmpty()) {
            return;
        }

        List<RequirementCategory> categories = rules.getCategories();
        if (categories == null) {
            return;
        }

        for (UserCustomizedRequirements.CourseModification modification : customization.getCourseModifications()) {
            RequirementCategory targetCategory = findCategory(categories, modification.getCategoryName());

            if (targetCategory == null) {
                continue;
            }

            String type = modification.getModificationType();
            if ("ADD".equals(type)) {
                // 과목 추가
                CourseRequirement newCourse = new CourseRequirement(
                    modification.getCourseName(),
                    modification.getCredits()
                );
                targetCategory.getCourses().add(newCourse);
                Log.d(TAG, "  과목 추가: " + modification);

            } else if ("DELETE".equals(type)) {
                // 과목 삭제
                targetCategory.getCourses().removeIf(
                    course -> course.getName().equals(modification.getCourseName())
                );
                Log.d(TAG, "  과목 삭제: " + modification);

            } else if ("MODIFY".equals(type)) {
                // 과목 수정
                for (CourseRequirement course : targetCategory.getCourses()) {
                    if (course.getName().equals(modification.getOldCourseName())) {
                        course.setName(modification.getCourseName());
                        course.setCredits(modification.getCredits());
                        Log.d(TAG, "  과목 수정: " + modification);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 카테고리 찾기
     */
    private RequirementCategory findCategory(List<RequirementCategory> categories, String categoryName) {
        for (RequirementCategory category : categories) {
            if (categoryName.equals(category.getName())) {
                return category;
            }
        }
        return null;
    }

    /**
     * GraduationRules 복사 (Deep Copy)
     */
    private GraduationRules copyGraduationRules(GraduationRules original) {
        GraduationRules copy = new GraduationRules();
        copy.setDocId(original.getDocId());
        copy.setCohort(original.getCohort());
        copy.setDepartment(original.getDepartment());
        copy.setTrack(original.getTrack());
        copy.setVersion(original.getVersion());
        copy.setUpdatedAt(original.getUpdatedAt());
        copy.setOverflowDestination(original.getOverflowDestination());

        // CreditRequirements 복사
        if (original.getCreditRequirements() != null) {
            copy.setCreditRequirements(original.getCreditRequirements());
        }

        // Categories 복사
        if (original.getCategories() != null) {
            List<RequirementCategory> copiedCategories = new ArrayList<>();
            for (RequirementCategory category : original.getCategories()) {
                RequirementCategory copiedCategory = new RequirementCategory();
                copiedCategory.setId(category.getId());
                copiedCategory.setName(category.getName());
                copiedCategory.setDisplayName(category.getDisplayName());
                copiedCategory.setType(category.getType());
                copiedCategory.setRequired(category.getRequired());
                copiedCategory.setRequiredType(category.getRequiredType());

                // Courses 복사
                if (category.getCourses() != null) {
                    List<CourseRequirement> copiedCourses = new ArrayList<>();
                    for (CourseRequirement course : category.getCourses()) {
                        CourseRequirement copiedCourse = new CourseRequirement(
                            course.getName(),
                            course.getCredits()
                        );
                        copiedCourse.setSemester(course.getSemester());
                        copiedCourse.setMandatory(course.isMandatory());
                        copiedCourses.add(copiedCourse);
                    }
                    copiedCategory.setCourses(copiedCourses);
                }

                copiedCategories.add(copiedCategory);
            }
            copy.setCategories(copiedCategories);
        }

        // ReplacementRules 복사
        if (original.getReplacementRules() != null) {
            copy.setReplacementRules(new ArrayList<>(original.getReplacementRules()));
        }

        return copy;
    }

    /**
     * 커스터마이징 초기화
     */
    public void resetCustomizations(OnSaveListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        db.collection(COLLECTION_NAME)
            .document(currentUser.getUid())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "커스터마이징 초기화 성공");
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "커스터마이징 초기화 실패", e);
                listener.onFailure(e);
            });
    }

    // Callback 인터페이스들
    public interface OnLoadListener {
        void onSuccess(UserCustomizedRequirements customizations);
        void onFailure(Exception e);
    }

    public interface OnSaveListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}
