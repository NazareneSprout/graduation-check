package sprout.app.sakmvp1;

import java.util.HashMap;
import java.util.Map;

/**
 * 학부별 전공심화/학부공통 사용 설정을 관리하는 클래스
 * Firebase에서 동적으로 설정을 로드하며, 캐싱 기능을 제공합니다.
 */
public class DepartmentConfig {

    // Firebase에서 로드한 설정 캐시 (department -> uses_major_advanced)
    private static final Map<String, Boolean> CACHED_DEPARTMENT_CONFIG = new HashMap<>();

    // 기본값: Firebase 데이터가 없을 때 사용할 폴백 설정
    private static final Map<String, Boolean> DEFAULT_DEPARTMENT_USES_MAJOR_ADVANCED = new HashMap<>();

    static {
        // 기본값 설정 (Firebase에서 로드 실패 시 폴백용)
        DEFAULT_DEPARTMENT_USES_MAJOR_ADVANCED.put("태권도학부", true);
        DEFAULT_DEPARTMENT_USES_MAJOR_ADVANCED.put("태권도학과", true);
        DEFAULT_DEPARTMENT_USES_MAJOR_ADVANCED.put("체육학과", true);
        DEFAULT_DEPARTMENT_USES_MAJOR_ADVANCED.put("IT학부", false);
    }

    /**
     * Firebase에서 학부 설정 캐시 업데이트
     * @param department 학부명
     * @param usesMajorAdvanced 전공심화 사용 여부
     */
    public static void updateDepartmentConfig(String department, boolean usesMajorAdvanced) {
        CACHED_DEPARTMENT_CONFIG.put(department, usesMajorAdvanced);
    }

    /**
     * 해당 학부가 모든 연도에서 전공심화를 사용하는지 확인
     * @param department 학부명
     * @return true: 모든 연도 전공심화, false: 연도별 구분
     */
    public static boolean usesMajorAdvancedForAllYears(String department) {
        // 캐시된 값이 있으면 사용
        if (CACHED_DEPARTMENT_CONFIG.containsKey(department)) {
            return CACHED_DEPARTMENT_CONFIG.get(department);
        }

        // 캐시된 값이 없으면 기본값 사용
        return DEFAULT_DEPARTMENT_USES_MAJOR_ADVANCED.getOrDefault(department, false);
    }

    /**
     * 학부와 연도에 따른 카테고리명 반환
     * @param department 학부명
     * @param year 학번
     * @return "전공심화" 또는 "학부공통"
     */
    public static String getDepartmentCommonCategoryName(String department, String year) {
        if (usesMajorAdvancedForAllYears(department)) {
            return "전공심화";
        } else {
            // 연도에 따라 구분
            int yearInt = Integer.parseInt(year);
            return yearInt >= 2023 ? "전공심화" : "학부공통";
        }
    }

    /**
     * 학부와 연도에 따른 탭 텍스트 반환
     * @param department 학부명
     * @param year 학번
     * @return "전공심화" 또는 "학부공통"
     */
    public static String getTabText(String department, String year) {
        return getDepartmentCommonCategoryName(department, year);
    }

    /**
     * 구 교육과정 여부 판단 (모든 isOldCurriculum 로직을 대체)
     * @param department 학부명
     * @param year 학번
     * @return true: 학부공통 사용, false: 전공심화 사용
     */
    public static boolean isOldCurriculum(String department, String year) {
        return "학부공통".equals(getDepartmentCommonCategoryName(department, year));
    }

    /**
     * 학점 오버플로우 시 이동할 카테고리 반환
     * @param department 학부명
     * @param year 학번
     * @return "일반선택" 또는 "잔여학점"
     */
    public static String getOverflowDestination(String department, String year) {
        return isOldCurriculum(department, year) ? "일반선택" : "잔여학점";
    }

    /**
     * 전공심화 사용 여부 판단
     * @param department 학부명
     * @param year 학번
     * @return true: 전공심화 사용, false: 학부공통 사용
     */
    public static boolean shouldUseMajorAdvanced(String department, String year) {
        return "전공심화".equals(getDepartmentCommonCategoryName(department, year));
    }

    /**
     * 레이아웃 리소스 선택
     * @param department 학부명
     * @param year 학번
     * @return 구 교육과정이면 old 레이아웃, 신 교육과정이면 새 레이아웃
     */
    public static boolean shouldUseOldLayout(String department, String year) {
        return isOldCurriculum(department, year);
    }

    /**
     * 오버플로우 안내 텍스트 생성
     * @param department 학부명
     * @param year 학번
     * @return 해당 학부/연도에 맞는 오버플로우 안내 텍스트
     */
    public static String getOverflowGuidanceText(String department, String year) {
        String destination = getOverflowDestination(department, year);
        return "초과 이수한 학점은 " + destination + "으로 인정됩니다.";
    }

    /**
     * 디버깅용: 현재 설정된 모든 학부 목록 반환
     * @return 학부별 설정 맵
     */
    public static Map<String, Boolean> getAllDepartmentConfigs() {
        Map<String, Boolean> allConfigs = new HashMap<>(DEFAULT_DEPARTMENT_USES_MAJOR_ADVANCED);
        allConfigs.putAll(CACHED_DEPARTMENT_CONFIG); // 캐시된 값으로 덮어쓰기
        return allConfigs;
    }

    /**
     * Firebase에서 학부 설정을 로드하고 캐시에 저장
     * @param department 학부명
     * @param firebaseDataManager FirebaseDataManager 인스턴스
     */
    public static void loadDepartmentConfigFromFirebase(String department, FirebaseDataManager firebaseDataManager) {
        firebaseDataManager.loadDepartmentConfig(department, new FirebaseDataManager.OnDepartmentConfigLoadedListener() {
            @Override
            public void onSuccess(boolean usesMajorAdvanced) {
                updateDepartmentConfig(department, usesMajorAdvanced);
            }

            @Override
            public void onFailure(Exception e) {
                // 실패 시 기본값 사용 (이미 캐시에 없다면 기본값이 사용됨)
                android.util.Log.w("DepartmentConfig", "Firebase에서 학부 설정 로드 실패: " + department + " - 기본값 사용");
            }
        });
    }
}