package sprout.app.sakmvp1.utils;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

/**
 * 졸업요건 관련 유틸리티 클래스
 * 공통적으로 사용되는 헬퍼 메서드 모음
 */
public class GraduationRequirementUtils {

    private static final String TAG = "GradReqUtils";

    /**
     * 문서 ID를 파싱하여 학과, 트랙, 학번 정보를 추출
     * @param documentId 형식: "department_track_year" (예: "컴퓨터정보통신_일반_2025")
     * @return Map with keys: "department", "track", "year"
     */
    public static Map<String, String> parseDocumentId(String documentId) {
        Map<String, String> result = new HashMap<>();
        if (documentId != null && documentId.contains("_")) {
            String[] parts = documentId.split("_");
            if (parts.length >= 3) {
                result.put("department", parts[0]);
                result.put("track", parts[1]);
                result.put("year", parts[2]);
            }
        }
        return result;
    }

    /**
     * DocumentSnapshot에서 정수값 안전하게 추출
     * Firestore는 숫자를 Number 또는 String으로 저장할 수 있으므로 안전하게 변환
     *
     * @param document Firestore DocumentSnapshot
     * @param key 필드 이름
     * @param defaultValue 기본값 (필드가 없거나 변환 실패 시)
     * @return 변환된 정수값
     */
    public static int getIntValue(DocumentSnapshot document, String key, int defaultValue) {
        Object value = document.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                Log.w(TAG, "숫자 변환 실패: " + key + " = " + value);
            }
        }
        return defaultValue;
    }

    /**
     * Map에서 정수값 안전하게 추출
     * v2 creditRequirements 객체에서 값 추출 시 사용
     *
     * @param map Map 객체
     * @param key 필드 이름
     * @param defaultValue 기본값 (필드가 없거나 변환 실패 시)
     * @return 변환된 정수값
     */
    public static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }

        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                Log.w(TAG, "숫자 변환 실패: " + key + " = " + value);
            }
        }
        return defaultValue;
    }

    /**
     * Long 값을 안전하게 int로 변환
     * @param value Long 값 (null 가능)
     * @param defaultValue 기본값
     * @return 변환된 int 값
     */
    public static int getLongAsInt(Long value, int defaultValue) {
        return value != null ? value.intValue() : defaultValue;
    }

    /**
     * 문서 ID가 교양 문서인지 확인
     * @param documentId 문서 ID
     * @return 교양 문서이면 true
     */
    public static boolean isGeneralEducationDocument(String documentId) {
        return documentId != null && documentId.startsWith("교양_");
    }

    /**
     * 학기 문자열을 순서 번호로 변환 (정렬용)
     * @param semester 학기 문자열 (예: "1학년 1학기")
     * @return 순서 번호 (1-8, 알 수 없으면 99)
     */
    public static int getSemesterOrder(String semester) {
        if (semester == null) return 99;
        switch (semester) {
            case "1학년 1학기": return 1;
            case "1학년 2학기": return 2;
            case "2학년 1학기": return 3;
            case "2학년 2학기": return 4;
            case "3학년 1학기": return 5;
            case "3학년 2학기": return 6;
            case "4학년 1학기": return 7;
            case "4학년 2학기": return 8;
            default: return 99;
        }
    }

    /**
     * 문서 ID에서 학부/학과 추출
     * @param documentId 문서 ID
     * @return 학부/학과명 (추출 실패 시 null)
     */
    public static String getDepartmentFromDocId(String documentId) {
        Map<String, String> parsed = parseDocumentId(documentId);
        return parsed.get("department");
    }

    /**
     * 문서 ID에서 트랙 추출
     * @param documentId 문서 ID
     * @return 트랙명 (추출 실패 시 null)
     */
    public static String getTrackFromDocId(String documentId) {
        Map<String, String> parsed = parseDocumentId(documentId);
        return parsed.get("track");
    }

    /**
     * 문서 ID에서 학번(연도) 추출
     * @param documentId 문서 ID
     * @return 학번 (추출 실패 시 null)
     */
    public static String getYearFromDocId(String documentId) {
        Map<String, String> parsed = parseDocumentId(documentId);
        return parsed.get("year");
    }

    /**
     * 문서 ID 생성
     * @param department 학부/학과
     * @param track 트랙
     * @param year 학번
     * @return 생성된 문서 ID (예: "컴퓨터정보통신_일반_2025")
     */
    public static String createDocumentId(String department, String track, String year) {
        return department + "_" + track + "_" + year;
    }

    /**
     * 교양 문서 ID 생성
     * @param department 학부/학과 (null이면 "공통")
     * @param year 학번
     * @return 생성된 교양 문서 ID (예: "교양_공통_2025" 또는 "교양_컴퓨터정보통신_2025")
     */
    public static String createGeneralEducationDocId(String department, String year) {
        if (department == null || department.trim().isEmpty()) {
            return "교양_공통_" + year;
        }
        return "교양_" + department + "_" + year;
    }

    /**
     * Firestore 문서에서 creditRequirements 객체를 통해 특정 카테고리의 학점 가져오기
     * 통합 구조(v2)를 지원하며, 이전 구조(v1)로 fallback
     * totalCredits/총학점 필드명 양쪽 지원
     *
     * @param document Firestore DocumentSnapshot
     * @param categoryName 카테고리 이름 (예: "전공심화", "전공필수", "totalCredits" 등)
     * @param defaultValue 기본값
     * @return 해당 카테고리의 학점
     */
    @SuppressWarnings("unchecked")
    public static int getCreditFromRequirements(DocumentSnapshot document, String categoryName, int defaultValue) {
        String docId = document.getId();

        // 0. totalCredits의 경우 한글 필드명 "총학점"도 함께 확인
        if (categoryName.equals("totalCredits")) {
            // 먼저 한글 필드명 "총학점"을 확인 (v2 구조)
            Object creditReqObj = document.get("creditRequirements");
            if (creditReqObj instanceof Map) {
                Map<String, Object> creditRequirements = (Map<String, Object>) creditReqObj;
                if (creditRequirements.containsKey("총학점")) {
                    int value = getIntValue(creditRequirements, "총학점", defaultValue);
                    Log.d(TAG, "[" + docId + "] Found 총학점 in creditRequirements (v2): " + value);
                    return value;
                }
            }

            // v1 구조에서 "총학점" 확인
            Object koreanValue = document.get("총학점");
            if (koreanValue != null) {
                int value = getIntValue(document, "총학점", defaultValue);
                Log.d(TAG, "[" + docId + "] Found 총학점 in document root (v1): " + value);
                return value;
            }
        }

        // 1. 먼저 통합 구조(v2)에서 시도: creditRequirements 객체 확인
        Object creditReqObj = document.get("creditRequirements");
        if (creditReqObj instanceof Map) {
            Map<String, Object> creditRequirements = (Map<String, Object>) creditReqObj;

            // creditRequirements에 직접 카테고리 이름이 있는지 확인
            if (creditRequirements.containsKey(categoryName)) {
                int value = getIntValue(creditRequirements, categoryName, defaultValue);
                Log.d(TAG, "[" + docId + "] Found " + categoryName + " in creditRequirements (v2): " + value);
                return value;
            }
        }

        // 2. 이전 구조(v1)로 fallback: 문서 루트에서 직접 읽기
        Object rootValue = document.get(categoryName);
        if (rootValue != null) {
            int value = getIntValue(document, categoryName, defaultValue);
            Log.d(TAG, "[" + docId + "] Found " + categoryName + " in document root (v1): " + value);
            return value;
        }

        // 3. 둘 다 없으면 기본값 사용
        Log.w(TAG, "[" + docId + "] " + categoryName + " NOT FOUND - using default value: " + defaultValue);
        return defaultValue;
    }
}
