package sprout.app.sakmvp1;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirebaseDataManager {
    private static final String TAG = "FirebaseDataManager";
    private static FirebaseDataManager instance;
    private FirebaseFirestore db;

    // 캐싱을 위한 맵들
    private Map<String, List<String>> studentYearsCache = new HashMap<>();
    private Map<String, List<String>> departmentsCache = new HashMap<>();
    private Map<String, List<String>> tracksCache = new HashMap<>();
    private Map<String, List<CourseInfo>> coursesCache = new HashMap<>();
    private Map<String, Object> graduationCache = new HashMap<>();

    private FirebaseDataManager() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseFirestore 인스턴스 초기화 성공");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseFirestore 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }

    public static synchronized FirebaseDataManager getInstance() {
        if (instance == null) {
            instance = new FirebaseDataManager();
        }
        return instance;
    }


    // 학번 데이터 조회
    public interface OnStudentYearsLoadedListener {
        void onSuccess(List<String> years);
        void onFailure(Exception e);
    }

    public void loadStudentYears(OnStudentYearsLoadedListener listener) {
        // 캐시에서 확인
        String cacheKey = "student_years";
        if (studentYearsCache.containsKey(cacheKey)) {
            Log.d(TAG, "캐시에서 학번 데이터 로드: " + studentYearsCache.get(cacheKey).size() + "개");
            listener.onSuccess(studentYearsCache.get(cacheKey));
            return;
        }

        // graduation_requirements 컬렉션에서 년도 정보 추출
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> yearsSet = new HashSet<>(); // 중복 제거를 위해 Set 사용

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();
                        // 문서 ID 형식: "IT학부_멀티미디어_2020"에서 년도 추출
                        String[] parts = docId.split("_");
                        if (parts.length >= 3) {
                            String year = parts[parts.length - 1]; // 마지막 부분이 년도
                            yearsSet.add(year);
                        }
                    }

                    // 2024, 2025학번 추가 (2023 데이터와 동일하게 처리) - Set에 추가하여 중복 방지
                    if (yearsSet.contains("2023")) {
                        yearsSet.add("2024");
                        yearsSet.add("2025");
                    }

                    List<String> years = new ArrayList<>(yearsSet);

                    // 연도순으로 정렬 (최신년도가 먼저 오도록)
                    years.sort((a, b) -> b.compareTo(a));

                    // 캐시에 저장
                    studentYearsCache.put(cacheKey, years);

                    Log.d(TAG, "학번 데이터 로드 성공: " + years.size() + "개 - " + years);
                    listener.onSuccess(years);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학번 데이터 로드 실패", e);
                    // 기본값 제공
                    List<String> defaultYears = new ArrayList<>();
                    defaultYears.add("2025");
                    defaultYears.add("2024");
                    defaultYears.add("2023");
                    listener.onSuccess(defaultYears);
                });
    }

    // 학부 데이터 조회
    public interface OnDepartmentsLoadedListener {
        void onSuccess(List<String> departments);
        void onFailure(Exception e);
    }

    public void loadDepartments(OnDepartmentsLoadedListener listener) {
        // graduation_requirements 컬렉션에서 학부 정보 추출
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> departmentsSet = new HashSet<>(); // 중복 제거를 위해 Set 사용

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();
                        // 문서 ID 형식: "IT학부_멀티미디어_2020"에서 학부 추출
                        String[] parts = docId.split("_");
                        if (parts.length >= 3) {
                            String department = parts[0]; // 첫 번째 부분이 학부
                            // 교양은 특정 학부가 아니므로 제외
                            if (!"교양".equals(department)) {
                                departmentsSet.add(department);
                            }
                        }
                    }

                    List<String> departments = new ArrayList<>(departmentsSet);
                    // 알파벳 순으로 정렬
                    departments.sort(String::compareTo);

                    Log.d(TAG, "학부 데이터 로드 성공: " + departments.size() + "개 - " + departments);
                    listener.onSuccess(departments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학부 데이터 로드 실패", e);
                    listener.onFailure(e);
                });
    }

    // 트랙 데이터 조회
    public interface OnTracksLoadedListener {
        void onSuccess(List<String> tracks);
        void onFailure(Exception e);
    }

    public void loadTracksByDepartment(String departmentName, OnTracksLoadedListener listener) {
        // graduation_requirements 컬렉션에서 해당 학부의 트랙 정보 추출
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> tracksSet = new HashSet<>(); // 중복 제거를 위해 Set 사용

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();
                        // 문서 ID 형식: "IT학부_멀티미디어_2020"
                        String[] parts = docId.split("_");
                        if (parts.length >= 3) {
                            String department = parts[0]; // 첫 번째 부분이 학부
                            String track = parts[1]; // 두 번째 부분이 트랙

                            if (department.equals(departmentName)) {
                                tracksSet.add(track);
                            }
                        }
                    }

                    List<String> tracks = new ArrayList<>(tracksSet);
                    // 알파벳 순으로 정렬
                    tracks.sort(String::compareTo);

                    Log.d(TAG, departmentName + " 트랙 데이터 로드 성공: " + tracks.size() + "개 - " + tracks);
                    listener.onSuccess(tracks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, departmentName + " 트랙 데이터 로드 실패", e);
                    listener.onFailure(e);
                });
    }

    // 졸업 요건 조회
    public interface OnGraduationRequirementsLoadedListener {
        void onSuccess(Map<String, Object> requirements);
        void onFailure(Exception e);
    }

    // 졸업이수학점 요건 조회
    public interface OnCreditRequirementsLoadedListener {
        void onSuccess(CreditRequirements creditRequirements);
        void onFailure(Exception e);
    }

    // 졸업이수학점 데이터 클래스
    public static class CreditRequirements {
        public int totalCredits;
        public int majorRequired;
        public int majorElective;
        public int generalRequired;
        public int generalElective;
        public int liberalArts;
        public int freeElective;
        public int departmentCommon;
        public int majorAdvanced;

        public CreditRequirements(int totalCredits, int majorRequired, int majorElective,
                                int generalRequired, int generalElective, int liberalArts, int freeElective,
                                int departmentCommon, int majorAdvanced) {
            this.totalCredits = totalCredits;
            this.majorRequired = majorRequired;
            this.majorElective = majorElective;
            this.generalRequired = generalRequired;
            this.generalElective = generalElective;
            this.liberalArts = liberalArts;
            this.freeElective = freeElective;
            this.departmentCommon = departmentCommon;
            this.majorAdvanced = majorAdvanced;
        }

        @Override
        public String toString() {
            return String.format("CreditRequirements{총이수: %d, 전공필수: %d, 전공선택: %d, 교양필수: %d, 교양선택: %d, 소양: %d, 자율선택: %d, 학부공통: %d, 전공심화: %d}",
                totalCredits, majorRequired, majorElective, generalRequired, generalElective, liberalArts, freeElective, departmentCommon, majorAdvanced);
        }
    }

    public void loadGraduationRequirements(String department, String track, String year,
                                         OnGraduationRequirementsLoadedListener listener) {
        // 2023, 2024, 2025학번은 2023 데이터를 사용 (2020, 2021, 2022는 그대로 사용)
        String actualYear = year;
        if ("2023".equals(year) || "2024".equals(year) || "2025".equals(year)) {
            actualYear = "2023";
            Log.d(TAG, year + "학번은 2023 데이터를 사용합니다");
        }

        // 실제 문서 ID 형식: "IT학부_멀티미디어_2023"
        String documentId = department + "_" + track + "_" + actualYear;

        Log.d(TAG, "졸업 요건 조회 시작: " + documentId + " (원래 학번: " + year + ")");

        db.collection("graduation_requirements").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Log.d(TAG, "졸업 요건 조회 성공: " + documentId);
                        listener.onSuccess(data);
                    } else {
                        Log.w(TAG, "졸업 요건 문서 없음: " + documentId);
                        listener.onFailure(new Exception("해당 조건의 졸업요건을 찾을 수 없습니다: " + documentId));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업 요건 조회 실패: " + documentId, e);
                    listener.onFailure(e);
                });
    }

    // 졸업이수학점 요건 로드
    public void loadCreditRequirements(String department, String track, String year,
                                     OnCreditRequirementsLoadedListener listener) {
        // 2023, 2024, 2025학번은 2023 데이터를 사용
        String actualYear = year;
        if ("2023".equals(year) || "2024".equals(year) || "2025".equals(year)) {
            actualYear = "2023";
        }

        String documentId = department + "_" + track + "_" + actualYear;
        Log.d(TAG, "졸업이수학점 요건 조회 시작: " + documentId);

        // 먼저 총 학점을 조회
        loadTotalCredits(department, new OnTotalCreditsLoadedListener() {
            @Override
            public void onSuccess(Integer totalCredits) {
                // 총 학점 조회 성공 후 상세 요건 조회
                loadDetailedCreditRequirements(documentId, totalCredits != null ? totalCredits : 130, listener);
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "총 학점 조회 실패, 기본값 130 사용: " + e.getMessage());
                // 총 학점 조회 실패시 기본값으로 진행
                loadDetailedCreditRequirements(documentId, 130, listener);
            }
        });
    }

    // 상세 졸업이수학점 요건 조회 (총 학점이 이미 확보된 상태)
    private void loadDetailedCreditRequirements(String documentId, int totalCredits,
                                              OnCreditRequirementsLoadedListener listener) {
        db.collection("graduation_requirements").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();

                        // 문서의 전체 구조 로그 출력
                        Log.d(TAG, "=== " + documentId + " 문서 전체 구조 ===");
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                Log.d(TAG, "최상위 키: " + entry.getKey() + " -> 타입: " + entry.getValue().getClass().getSimpleName());
                                if (entry.getValue() instanceof Map) {
                                    Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                                    for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
                                        Log.d(TAG, "  하위키: " + nestedEntry.getKey() + " -> 값: " + nestedEntry.getValue());
                                    }
                                } else {
                                    Log.d(TAG, "  값: " + entry.getValue());
                                }
                            }
                        }
                        Log.d(TAG, "=== 문서 구조 끝 ===");

                        // 실제 Firestore 구조에 맞게 직접 필드에서 읽기
                        if (data != null) {
                            // 총 학점을 사용하여 졸업이수학점 정보 추출
                            CreditRequirements creditReqs = extractCreditRequirementsFromDirectFields(data, totalCredits);
                            Log.d(TAG, "졸업이수학점 요건 로드 성공: " + creditReqs.toString());
                            listener.onSuccess(creditReqs);
                        } else {
                            Log.w(TAG, "문서 데이터가 null: " + documentId);
                            listener.onFailure(new Exception("졸업이수학점 정보를 찾을 수 없습니다"));
                        }
                    } else {
                        Log.w(TAG, "졸업이수학점 문서 없음: " + documentId);
                        listener.onFailure(new Exception("해당 조건의 졸업이수학점 정보를 찾을 수 없습니다"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업이수학점 요건 조회 실패: " + documentId, e);
                    listener.onFailure(e);
                });
    }

    // 특정 문서 직접 조회해서 구조 확인 (디버그용)
    public void inspectSpecificDocument(String documentId) {
        Log.d(TAG, "=== 특정 문서 조회 시작: " + documentId + " ===");

        db.collection("graduation_requirements").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Log.d(TAG, "문서 존재함: " + documentId);

                        if (data != null) {
                            Log.d(TAG, "전체 필드 목록: " + data.keySet().toString());

                            // 각 필드의 값 출력
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                Object value = entry.getValue();
                                String valueStr = value != null ? value.toString() : "null";
                                String type = value != null ? value.getClass().getSimpleName() : "null";
                                Log.d(TAG, "필드 [" + entry.getKey() + "] = " + valueStr + " (타입: " + type + ")");
                            }

                            // 졸업이수학점 관련 필드 확인
                            Log.d(TAG, "=== 졸업이수학점 관련 필드 확인 ===");
                            String[] creditFields = {"전공필수", "전공선택", "교양필수", "교양선택", "소양", "자율선택", "학부공통"};
                            for (String field : creditFields) {
                                Object value = data.get(field);
                                if (value != null) {
                                    Log.d(TAG, "✓ " + field + ": " + value);
                                } else {
                                    Log.d(TAG, "✗ " + field + ": 없음");
                                }
                            }
                        } else {
                            Log.w(TAG, "문서 데이터가 null: " + documentId);
                        }
                    } else {
                        Log.w(TAG, "문서가 존재하지 않음: " + documentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패: " + documentId, e);
                });
    }

    // 실제 Firestore 필드에서 직접 CreditRequirements 객체 생성
    private CreditRequirements extractCreditRequirementsFromDirectFields(Map<String, Object> data, int totalCredits) {
        Log.d(TAG, "직접 필드에서 졸업이수학점 정보 추출 시작 (총 학점: " + totalCredits + ")");

        // Firebase에서 개별 학점들 추출
        int majorRequired = getIntValue(data, "전공필수", 27);
        int majorElective = getIntValue(data, "전공선택", 18);
        int generalRequired = getIntValue(data, "교양필수", 16);
        int generalElective = getIntValue(data, "교양선택", 8);
        int liberalArts = getIntValue(data, "소양", 6);
        int departmentCommon = getIntValue(data, "학부공통", 0);
        int majorAdvanced = getIntValue(data, "전공심화", 0);

        // 자율선택 학점은 총 학점에서 다른 모든 학점들을 빼서 계산
        int specifiedCredits = majorRequired + majorElective + generalRequired + generalElective +
                              liberalArts + departmentCommon + majorAdvanced;
        int freeElective = totalCredits - specifiedCredits;

        // 자율선택이 음수가 되지 않도록 보정
        if (freeElective < 0) {
            Log.w(TAG, "자율선택 학점이 음수입니다: " + freeElective + ", 0으로 설정합니다.");
            freeElective = 0;
        }

        Log.d(TAG, "추출된 졸업이수학점:");
        Log.d(TAG, "총 학점: " + totalCredits + " (Firebase에서 조회)");
        Log.d(TAG, "전공필수: " + majorRequired);
        Log.d(TAG, "전공선택: " + majorElective);
        Log.d(TAG, "교양필수: " + generalRequired);
        Log.d(TAG, "교양선택: " + generalElective);
        Log.d(TAG, "소양: " + liberalArts);
        Log.d(TAG, "학부공통: " + departmentCommon);
        Log.d(TAG, "전공심화: " + majorAdvanced);
        Log.d(TAG, "자율선택: " + freeElective + " (계산됨: " + totalCredits + " - " + specifiedCredits + ")");

        return new CreditRequirements(totalCredits, majorRequired, majorElective,
                                    generalRequired, generalElective, liberalArts, freeElective,
                                    departmentCommon, majorAdvanced);
    }

    // Firestore 데이터에서 CreditRequirements 객체 생성 (기존 방식)
    private CreditRequirements extractCreditRequirements(Map<String, Object> requirements) {
        int totalCredits = getIntValue(requirements, "totalCredits", 130); // 기본값 130
        int majorRequired = getIntValue(requirements, "majorRequired", 45);
        int majorElective = getIntValue(requirements, "majorElective", 30);
        int generalRequired = getIntValue(requirements, "generalRequired", 20);
        int generalElective = getIntValue(requirements, "generalElective", 15);
        int liberalArts = getIntValue(requirements, "liberalArts", 10);
        int freeElective = getIntValue(requirements, "freeElective", 10);
        int departmentCommon = getIntValue(requirements, "departmentCommon", 0);
        int majorAdvanced = getIntValue(requirements, "majorAdvanced", 0);

        return new CreditRequirements(totalCredits, majorRequired, majorElective,
                generalRequired, generalElective, liberalArts, freeElective,
                departmentCommon, majorAdvanced);
    }

    // Map에서 정수값 안전하게 추출
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
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

    // 모든 컬렉션 데이터 조회
    public interface OnCollectionDataLoadedListener {
        void onSuccess(List<Map<String, Object>> documents);
        void onFailure(Exception e);
    }

    public void loadAllDocumentsFromCollection(String collectionName, OnCollectionDataLoadedListener listener) {
        Log.d(TAG, "컬렉션 조회 시작: " + collectionName);

        if (db == null) {
            Log.e(TAG, "Firestore 인스턴스가 null입니다");
            listener.onFailure(new Exception("Firestore가 초기화되지 않았습니다"));
            return;
        }

        db.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "쿼리 성공 - 문서 개수: " + queryDocumentSnapshots.size());
                    List<Map<String, Object>> documents = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d(TAG, "문서 처리 중: " + document.getId());
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", document.getId());
                        Map<String, Object> docData = document.getData();
                        if (docData != null) {
                            data.putAll(docData);
                        }
                        documents.add(data);
                    }
                    Log.d(TAG, "최종 문서 리스트 크기: " + documents.size());
                    listener.onSuccess(documents);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "컬렉션 조회 실패: " + collectionName, e);
                    listener.onFailure(e);
                });
    }

    // 특정 문서 조회
    public interface OnDocumentLoadedListener {
        void onSuccess(Map<String, Object> document);
        void onFailure(Exception e);
    }

    public void loadDocument(String collectionName, String documentId, OnDocumentLoadedListener listener) {
        db.collection(collectionName).document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", documentSnapshot.getId());
                        data.putAll(documentSnapshot.getData());
                        listener.onSuccess(data);
                    } else {
                        listener.onFailure(new Exception("문서를 찾을 수 없습니다."));
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // 조건부 쿼리 조회
    public void loadDocumentsWithCondition(String collectionName, String field, Object value, OnCollectionDataLoadedListener listener) {
        db.collection(collectionName)
                .whereEqualTo(field, value)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> documents = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id", document.getId());
                        data.putAll(document.getData());
                        documents.add(data);
                    }
                    listener.onSuccess(documents);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // 모든 컬렉션 이름 조회
    public interface OnCollectionNamesLoadedListener {
        void onSuccess(List<String> collectionNames);
        void onFailure(Exception e);
    }

    public void loadAvailableCollections(OnCollectionNamesLoadedListener listener) {
        // Firestore에서 직접 컬렉션 목록을 가져올 수 없으므로
        // 알려진 컬렉션들을 반환
        List<String> knownCollections = new ArrayList<>();
        knownCollections.add("test"); // 테스트용 컬렉션 추가
        knownCollections.add("departments");
        knownCollections.add("graduation_requirements");
        knownCollections.add("courses");
        knownCollections.add("users");
        knownCollections.add("student_records");

        listener.onSuccess(knownCollections);
    }


    // 테스트 데이터 생성 메서드
    public void createTestData(OnDocumentLoadedListener listener) {
        Map<String, Object> testData = new HashMap<>();
        testData.put("name", "테스트 데이터");
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("description", "Firestore 연결 테스트용 데이터");

        db.collection("test")
                .add(testData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "테스트 데이터 생성 성공: " + documentReference.getId());
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", documentReference.getId());
                    result.putAll(testData);
                    listener.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "테스트 데이터 생성 실패", e);
                    listener.onFailure(e);
                });
    }

    // Firebase 연결 테스트
    public interface OnConnectionTestListener {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    public void testFirebaseConnection(OnConnectionTestListener listener) {
        Log.d(TAG, "Firebase 연결 테스트 시작");

        // 가장 간단한 연결 테스트: Firebase settings 확인
        try {
            if (db == null) {
                listener.onFailure(new Exception("Firestore 인스턴스가 null입니다"));
                return;
            }

            // FirebaseFirestore의 설정을 가져와서 연결 확인
            db.getFirestoreSettings();
            Log.d(TAG, "Firestore 설정 확인 완료");

            // 간단한 읽기 테스트 (존재하지 않는 컬렉션이라도 권한 확인 가능)
            db.collection("connection_test").limit(1).get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d(TAG, "Firebase 연결 테스트 성공");
                        listener.onSuccess("Firebase 연결 성공! (쿼리 실행됨)");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase 연결 테스트 실패", e);
                        listener.onFailure(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Firebase 설정 확인 실패", e);
            listener.onFailure(e);
        }
    }

    // 데이터 개수 조회
    public interface OnCountLoadedListener {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    public void getDocumentCount(String collectionName, OnCountLoadedListener listener) {
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onSuccess(querySnapshot.size());
                })
                .addOnFailureListener(listener::onFailure);
    }

    // 강의 정보를 담는 클래스
    public static class CourseInfo {
        private String name;
        private int credits;
        private String groupId; // oneOf 그룹을 구분하기 위한 ID

        public CourseInfo(String name, int credits) {
            this.name = name;
            this.credits = credits;
            this.groupId = null;
        }

        public CourseInfo(String name, int credits, String groupId) {
            this.name = name;
            this.credits = credits;
            this.groupId = groupId;
        }

        public String getName() { return name; }
        public int getCredits() { return credits; }
        public String getGroupId() { return groupId; }

        @Override
        public String toString() {
            return name + " (" + credits + "학점)";
        }
    }

    // 전공 강의 목록 조회
    public interface OnMajorCoursesLoadedListener {
        void onSuccess(List<CourseInfo> courses);
        void onFailure(Exception e);
    }

    public void loadMajorCourses(String department, String track, String year, String category, OnMajorCoursesLoadedListener listener) {
        Log.d(TAG, "전공 강의 조회 시작: " + department + "_" + track + "_" + year);

        // 2023, 2024, 2025학번은 2023 데이터를 사용 (2020, 2021, 2022는 그대로 사용)
        String actualYear = year;
        if ("2023".equals(year) || "2024".equals(year) || "2025".equals(year)) {
            actualYear = "2023";
            Log.d(TAG, year + "학번은 2023 데이터를 사용합니다");
        }

        // graduation_requirements 컬렉션에서 해당 학과/트랙의 전공 강의 조회
        String documentId = department + "_" + track + "_" + actualYear;

        db.collection("graduation_requirements").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // 중복 제거를 위해 Set 사용 (과목명을 키로)
                    Set<String> addedCourseNames = new HashSet<>();
                    List<CourseInfo> majorCourses = new ArrayList<>();

                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            // rules 객체에서 학기별 데이터 추출
                            Object rulesObj = data.get("rules");
                            if (rulesObj instanceof Map) {
                                Map<String, Object> rules = (Map<String, Object>) rulesObj;

                                // 학기 키들을 정렬 (1학년 1학기, 1학년 2학기, 2학년 1학기, ...)
                                List<String> sortedSemesters = new ArrayList<>(rules.keySet());
                                sortedSemesters.sort((s1, s2) -> {
                                    // 학년과 학기 추출
                                    int year1 = extractYear(s1);
                                    int semester1 = extractSemester(s1);
                                    int year2 = extractYear(s2);
                                    int semester2 = extractSemester(s2);

                                    // 학년으로 먼저 정렬, 같으면 학기로 정렬
                                    if (year1 != year2) {
                                        return Integer.compare(year1, year2);
                                    }
                                    return Integer.compare(semester1, semester2);
                                });

                                // 정렬된 순서로 각 학기에서 전공필수와 전공선택 강의들을 수집
                                for (String semesterKey : sortedSemesters) {
                                    Object value = rules.get(semesterKey);

                                    // 학기 데이터인지 확인
                                    if (semesterKey.contains("학년") && value instanceof Map) {
                                        Map<String, Object> semester = (Map<String, Object>) value;

                                        // 선택한 카테고리에 해당하는 과목들만 수집
                                        if ("전공필수".equals(category)) {
                                            // 전공필수 과목들만 수집
                                            Object majorRequired = semester.get("전공필수");
                                            if (majorRequired instanceof List) {
                                                List<?> requiredList = (List<?>) majorRequired;
                                                for (Object courseObj : requiredList) {
                                                    if (courseObj instanceof Map) {
                                                        Map<String, Object> course = (Map<String, Object>) courseObj;
                                                        Object courseName = course.get("과목명");
                                                        Object credits = course.get("학점");
                                                        if (courseName instanceof String && credits instanceof Number) {
                                                            String courseNameStr = (String) courseName;
                                                            if (!addedCourseNames.contains(courseNameStr)) {
                                                                addedCourseNames.add(courseNameStr);
                                                                majorCourses.add(new CourseInfo(courseNameStr, ((Number) credits).intValue()));
                                                                Log.d(TAG, "전공필수 강의 추가: " + courseNameStr + "(" + credits + "학점)");
                                                            } else {
                                                                Log.d(TAG, "전공필수 중복 강의 제외: " + courseNameStr);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else if ("전공선택".equals(category)) {
                                            // 전공선택 과목들만 수집
                                            Object majorElective = semester.get("전공선택");
                                            if (majorElective instanceof List) {
                                                List<?> electiveList = (List<?>) majorElective;
                                                for (Object courseObj : electiveList) {
                                                    if (courseObj instanceof Map) {
                                                        Map<String, Object> course = (Map<String, Object>) courseObj;
                                                        Object courseName = course.get("과목명");
                                                        Object credits = course.get("학점");
                                                        if (courseName instanceof String && credits instanceof Number) {
                                                            String courseNameStr = (String) courseName;
                                                            if (!addedCourseNames.contains(courseNameStr)) {
                                                                addedCourseNames.add(courseNameStr);
                                                                majorCourses.add(new CourseInfo(courseNameStr, ((Number) credits).intValue()));
                                                                Log.d(TAG, "전공선택 강의 추가: " + courseNameStr + "(" + credits + "학점)");
                                                            } else {
                                                                Log.d(TAG, "전공선택 중복 강의 제외: " + courseNameStr);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Log.d(TAG, "전공 강의 로드 성공: " + majorCourses.size() + "개 - " + majorCourses);
                        listener.onSuccess(majorCourses);
                    } else {
                        Log.w(TAG, "전공 강의 문서 없음: " + documentId);
                        // 빈 리스트 반환
                        listener.onSuccess(majorCourses);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 강의 로드 실패: " + documentId, e);
                    listener.onFailure(e);
                });
    }

    // 학기 문자열에서 학년 추출 (예: "1학년 1학기" -> 1)
    private int extractYear(String semester) {
        try {
            if (semester.contains("학년")) {
                String yearStr = semester.substring(0, semester.indexOf("학년"));
                return Integer.parseInt(yearStr);
            }
        } catch (Exception e) {
            Log.w(TAG, "학년 추출 실패: " + semester, e);
        }
        return 0;
    }

    // 학기 문자열에서 학기 추출 (예: "1학년 1학기" -> 1)
    private int extractSemester(String semester) {
        try {
            if (semester.contains("학기")) {
                String semesterPart = semester.substring(semester.lastIndexOf(" ") + 1);
                String semesterStr = semesterPart.substring(0, semesterPart.indexOf("학기"));
                return Integer.parseInt(semesterStr);
            }
        } catch (Exception e) {
            Log.w(TAG, "학기 추출 실패: " + semester, e);
        }
        return 0;
    }

    // 교양 강의 목록 조회
    public void loadGeneralEducationCourses(String department, String track, String year, String category, OnMajorCoursesLoadedListener listener) {
        Log.d(TAG, "=== 교양 강의 조회 시작 ===");
        Log.d(TAG, "FirebaseDataManager 교양 조회 시작");
        Log.d(TAG, "입력값 - 학부: " + department + ", 트랙: " + track + ", 년도: " + year + ", 카테고리: " + category);

        // 2022, 2024, 2025학번은 2023 데이터를 사용 (2020, 2021은 그대로 사용)
        final String actualYear = ("2024".equals(year) || "2025".equals(year)) ? "2023" : year;
        if (!actualYear.equals(year)) {
            Log.d(TAG, year + "학번은 2023 데이터를 사용합니다");
        }

        // 교양 문서 형태: 교양_학부명_연도 (예: 교양_공통_2023, 교양_IT학부_2023)
        // 먼저 해당 학부의 교양 문서를 시도하고, 없으면 공통 문서를 시도
        String departmentSpecificDocId = "교양_" + department + "_" + actualYear;
        String commonDocId = "교양_공통_" + actualYear;

        Log.d(TAG, "실제 사용 년도: " + actualYear + " (입력 년도: " + year + ")");

        Log.d(TAG, "교양 문서 조회 시도: " + departmentSpecificDocId + " -> " + commonDocId);

        // 먼저 모든 문서를 조회해서 교양 관련 문서가 있는지 확인
        Log.d(TAG, "디버깅: 모든 graduation_requirements 문서 조회");
        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "전체 문서 수: " + querySnapshot.size());
                    for (DocumentSnapshot doc : querySnapshot) {
                        String docId = doc.getId();
                        Log.d(TAG, "전체 문서: " + docId);
                        if (docId.contains("교양")) {
                            Log.d(TAG, "교양 관련 문서 발견: " + docId);
                        }
                        if (docId.contains("2023") && docId.contains("멀티미디어")) {
                            Log.d(TAG, "2023 멀티미디어 관련 문서 발견: " + docId);
                        }
                    }
                });

        // 교양 문서 조회 순서
        List<String> documentsToTry = new ArrayList<>();
        String originalYear = year;

        // 20, 21, 22학번은 해당 연도의 교양_공통 문서 직접 조회
        if ("2020".equals(originalYear) || "2021".equals(originalYear) || "2022".equals(originalYear)) {
            Log.d(TAG, originalYear + "학번은 교양_공통_" + originalYear + " 문서를 직접 조회");
            documentsToTry.add("교양_공통_" + originalYear);

            // 폴백으로 다른 연도 공통 문서들도 추가
            if (!"2020".equals(originalYear)) documentsToTry.add("교양_공통_2020");
            if (!"2021".equals(originalYear)) documentsToTry.add("교양_공통_2021");
            if (!"2022".equals(originalYear)) documentsToTry.add("교양_공통_2022");
        } else {
            // 23학번 이후는 기존 로직: 학부별 우선, 연도별 우선

            // 1순위: 같은 학부, 원래 연도 (교양_IT학부_2025)
            documentsToTry.add("교양_" + department + "_" + originalYear);

            // 2순위: 같은 학부, 변환된 연도 (교양_IT학부_2023)
            if (!originalYear.equals(actualYear)) {
                documentsToTry.add("교양_" + department + "_" + actualYear);
            }

            // 3순위: 공통, 원래 연도 (교양_공통_2025)
            documentsToTry.add("교양_공통_" + originalYear);

            // 4순위: 공통, 변환된 연도 (교양_공통_2023)
            if (!originalYear.equals(actualYear)) {
                documentsToTry.add("교양_공통_" + actualYear);
            }

            // 5순위: 폴백 - 학부별 2020
            documentsToTry.add("교양_" + department + "_2020");

            // 6순위: 폴백 - 공통 2020
            documentsToTry.add("교양_공통_2020");
        }

        Log.d(TAG, "교양 문서 조회 순서: " + documentsToTry);

        tryDocumentsSequentially(documentsToTry, 0, category, listener);
    }

    // 교양 문서를 순차적으로 시도하는 헬퍼 메서드
    private void tryDocumentsSequentially(List<String> documentsToTry, int index, String category, OnMajorCoursesLoadedListener listener) {
        if (index >= documentsToTry.size()) {
            Log.w(TAG, "모든 교양 문서 조회 실패, 빈 리스트 반환");
            listener.onSuccess(new ArrayList<>());
            return;
        }

        String docId = documentsToTry.get(index);
        Log.d(TAG, "문서 조회 시도 [" + (index + 1) + "/" + documentsToTry.size() + "]: " + docId);

        db.collection("graduation_requirements").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "교양 문서 발견: " + docId);
                        loadGeneralEducationFromDocument(documentSnapshot, category, listener);
                    } else {
                        Log.d(TAG, "문서 없음: " + docId + ", 다음 문서 시도");
                        tryDocumentsSequentially(documentsToTry, index + 1, category, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패: " + docId, e);
                    tryDocumentsSequentially(documentsToTry, index + 1, category, listener);
                });
    }

    // 교양 문서에서 데이터를 추출하는 헬퍼 메서드
    private void loadGeneralEducationFromDocument(DocumentSnapshot documentSnapshot, String category, OnMajorCoursesLoadedListener listener) {
        Log.d(TAG, "=== 교양 문서 데이터 추출 시작 ===");
        Log.d(TAG, "문서 ID: " + documentSnapshot.getId() + ", 카테고리: " + category);

        // 중복 제거를 위해 Set 사용 (과목명을 키로)
        Set<String> addedCourseNames = new HashSet<>();
        List<CourseInfo> resultCourses = new ArrayList<>();

        Map<String, Object> data = documentSnapshot.getData();
        if (data != null) {
            Log.d(TAG, "교양 문서 데이터 구조: " + data.keySet().toString());

            Object rulesObj = data.get("rules");
            if (rulesObj instanceof Map) {
                Map<String, Object> rules = (Map<String, Object>) rulesObj;
                Log.d(TAG, "rules 내부 키 목록: " + rules.keySet().toString());

                // 교양 문서는 requirements가 List 구조
                Object requirementsObj = rules.get("requirements");
                if (requirementsObj instanceof List) {
                    List<?> requirementsList = (List<?>) requirementsObj;
                    Log.d(TAG, "requirements는 List 형태, 항목 수: " + requirementsList.size());

                    // requirements 리스트에서 각 과목 정보 추출
                    int groupCounter = 0;
                    for (Object reqObj : requirementsList) {
                        if (reqObj instanceof Map) {
                            Map<String, Object> requirement = (Map<String, Object>) reqObj;
                            Log.d(TAG, "requirement 항목: " + requirement.keySet().toString());

                            // name 필드가 있으면 단일 과목, options 필드가 있으면 선택 과목
                            if (requirement.containsKey("name")) {
                                // 단일 과목
                                Object nameObj = requirement.get("name");
                                Object creditObj = requirement.get("credit");
                                if (nameObj instanceof String && creditObj instanceof Number) {
                                    String courseName = (String) nameObj;
                                    int credits = ((Number) creditObj).intValue();

                                    // 중복 체크
                                    if (!addedCourseNames.contains(courseName)) {
                                        addedCourseNames.add(courseName);
                                        resultCourses.add(new CourseInfo(courseName, credits));
                                        Log.d(TAG, "교양필수 단일 과목 발견: " + courseName + "(" + credits + "학점)");
                                    } else {
                                        Log.d(TAG, "중복 과목 제외: " + courseName);
                                    }
                                }
                            } else if (requirement.containsKey("options")) {
                                // 선택 과목 리스트 - 모든 개별 과목을 표시하되 그룹 ID 부여
                                Object optionsObj = requirement.get("options");
                                Object creditObj = requirement.get("credit");
                                Object typeObj = requirement.get("type");

                                if (optionsObj instanceof List && creditObj instanceof Number) {
                                    List<?> optionsList = (List<?>) optionsObj;
                                    int credits = ((Number) creditObj).intValue();
                                    String type = typeObj instanceof String ? (String) typeObj : "";

                                    if ("oneOf".equals(type)) {
                                        // oneOf 타입: 모든 옵션을 표시하되 같은 그룹 ID 부여
                                        String groupId = "oneOf_group_" + groupCounter++;
                                        for (Object optionObj : optionsList) {
                                            if (optionObj instanceof Map) {
                                                Map<String, Object> option = (Map<String, Object>) optionObj;
                                                Object nameObj = option.get("name");
                                                if (nameObj instanceof String) {
                                                    String courseName = (String) nameObj;

                                                    // 중복 체크
                                                    if (!addedCourseNames.contains(courseName)) {
                                                        addedCourseNames.add(courseName);
                                                        resultCourses.add(new CourseInfo(courseName, credits, groupId));
                                                        Log.d(TAG, "교양필수 oneOf 과목 발견: " + courseName + "(" + credits + "학점) 그룹:" + groupId);
                                                    } else {
                                                        Log.d(TAG, "중복 과목 제외: " + courseName);
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // 일반 선택 과목: 모든 옵션 표시 (그룹 ID 없음)
                                        for (Object optionObj : optionsList) {
                                            if (optionObj instanceof Map) {
                                                Map<String, Object> option = (Map<String, Object>) optionObj;
                                                Object nameObj = option.get("name");
                                                if (nameObj instanceof String) {
                                                    String courseName = (String) nameObj;

                                                    // 중복 체크
                                                    if (!addedCourseNames.contains(courseName)) {
                                                        addedCourseNames.add(courseName);
                                                        resultCourses.add(new CourseInfo(courseName, credits));
                                                        Log.d(TAG, "교양필수 선택 과목 발견: " + courseName + "(" + credits + "학점)");
                                                    } else {
                                                        Log.d(TAG, "중복 과목 제외: " + courseName);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "requirements 키가 없거나 List가 아님. rules 전체 구조: " + rules);
                }
            } else {
                Log.d(TAG, "rules 키가 없거나 Map이 아님");
            }
        }

        Log.d(TAG, "교양 강의 로드 성공: " + resultCourses.size() + "개 (중복 제거 후) - " + resultCourses);
        listener.onSuccess(resultCourses);
    }

    // 학부공통 강의 목록 조회
    public void loadDepartmentCommonCourses(String department, String track, String year, OnMajorCoursesLoadedListener listener) {
        Log.d(TAG, "학부공통/전공심화 강의 조회 시작: " + department + "_" + track + "_" + year);

        // 2023, 2024, 2025학번은 2023 데이터를 사용 (2020, 2021, 2022는 그대로 사용)
        final String actualYear = ("2023".equals(year) || "2024".equals(year) || "2025".equals(year)) ? "2023" : year;
        if (!actualYear.equals(year)) {
            Log.d(TAG, year + "학번은 2023 데이터를 사용합니다");
        }

        // graduation_requirements 컬렉션에서 해당 학부_트랙_학번 문서 조회
        String documentId = department + "_" + track + "_" + actualYear;
        Log.d(TAG, "학부공통 강의 문서 ID: " + documentId);

        db.collection("graduation_requirements").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Set<String> addedCourseNames = new HashSet<>();
                    List<CourseInfo> commonCourses = new ArrayList<>();

                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        // Log.d(TAG, "학부공통 문서 데이터 구조: " + data); // 성능 최적화를 위해 주석 처리
                        if (data != null) {
                            // Log.d(TAG, "문서 전체 키 목록: " + data.keySet().toString()); // 성능 최적화

                            // rules 객체에서 학기별 데이터 추출
                            Object rulesObj = data.get("rules");
                            if (rulesObj instanceof Map) {
                                Map<String, Object> rules = (Map<String, Object>) rulesObj;
                                Log.d(TAG, "rules 내부 키 목록: " + rules.keySet().toString());

                                // 학기 키들을 정렬 (1학년 1학기, 1학년 2학기, 2학년 1학기, ...)
                                List<String> sortedSemesters = new ArrayList<>(rules.keySet());
                                sortedSemesters.sort((s1, s2) -> {
                                    // 학년과 학기 추출
                                    int year1 = extractYear(s1);
                                    int semester1 = extractSemester(s1);
                                    int year2 = extractYear(s2);
                                    int semester2 = extractSemester(s2);

                                    // 학년으로 먼저 정렬, 같으면 학기로 정렬
                                    if (year1 != year2) {
                                        return Integer.compare(year1, year2);
                                    }
                                    return Integer.compare(semester1, semester2);
                                });

                                // 정렬된 순서로 각 학기에서 학부공통필수 강의들을 수집
                                for (String semesterKey : sortedSemesters) {
                                    Object value = rules.get(semesterKey);
                                    Log.d(TAG, "학기 키: " + semesterKey + ", 값 타입: " + (value != null ? value.getClass().getSimpleName() : "null"));

                                    // 학기 데이터인지 확인
                                    if (semesterKey.contains("학년") && value instanceof Map) {
                                        Map<String, Object> semester = (Map<String, Object>) value;
                                        Log.d(TAG, "학기 " + semesterKey + " 내부 키 목록: " + semester.keySet().toString());

                                        // 학번에 따라 다른 카테고리 조회 (23-25학번: 전공심화, 20-22학번: 학부공통필수)
                                        String categoryKey = Integer.parseInt(actualYear) >= 2023 ? "전공심화" : "학부공통필수";
                                        Object departmentCommon = semester.get(categoryKey);
                                        Log.d(TAG, categoryKey + " 데이터: " + departmentCommon);
                                        if (departmentCommon instanceof List) {
                                            List<?> commonList = (List<?>) departmentCommon;
                                            Log.d(TAG, categoryKey + " 과목 수: " + commonList.size());
                                            for (Object courseObj : commonList) {
                                                if (courseObj instanceof Map) {
                                                    Map<String, Object> course = (Map<String, Object>) courseObj;
                                                    Object courseName = course.get("과목명");
                                                    Object credits = course.get("학점");
                                                    Log.d(TAG, "과목: " + courseName + ", 학점: " + credits);
                                                    if (courseName instanceof String && credits instanceof Number) {
                                                        String courseNameStr = (String) courseName;
                                                        if (!addedCourseNames.contains(courseNameStr)) {
                                                            addedCourseNames.add(courseNameStr);
                                                            commonCourses.add(new CourseInfo(courseNameStr, ((Number) credits).intValue()));
                                                            Log.d(TAG, categoryKey + " 강의 발견: " + courseName + "(" + credits + "학점)");
                                                        } else {
                                                            Log.d(TAG, categoryKey + " 중복 강의 제외: " + courseName);
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Log.d(TAG, "학부공통필수 데이터가 List가 아님: " + (departmentCommon != null ? departmentCommon.getClass().getSimpleName() : "null"));
                                        }
                                    } else {
                                        Log.d(TAG, "학기 데이터가 아님 또는 Map이 아님: " + semesterKey);
                                    }
                                }
                            } else {
                                Log.d(TAG, "rules 키가 없거나 Map이 아님");
                            }
                        }

                        Log.d(TAG, "학부공통 강의 로드 성공: " + commonCourses.size() + "개 - " + commonCourses);
                        listener.onSuccess(commonCourses);
                    } else {
                        Log.w(TAG, "학부공통 강의 문서 없음: " + documentId);
                        // 빈 리스트 반환
                        listener.onSuccess(commonCourses);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학부공통 강의 로드 실패: " + documentId, e);
                    listener.onFailure(e);
                });
    }

    // 학부별 추가 졸업 요건 조회
    public interface OnExtraGradRequirementsLoadedListener {
        void onSuccess(String extraGradRequirement);
        void onFailure(Exception e);
    }

    // 총 학점 조회 인터페이스
    public interface OnTotalCreditsLoadedListener {
        void onSuccess(Integer totalCredits);
        void onFailure(Exception e);
    }

    public void loadExtraGradRequirements(String department, OnExtraGradRequirementsLoadedListener listener) {
        Log.d(TAG, "학부별 추가 졸업 요건 조회 시작: " + department);

        // graduation_meta/catalog/departments/{department} 경로에서 extra_grad 필드 조회
        db.collection("graduation_meta")
                .document("catalog")
                .collection("departments")
                .document(department)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null && data.containsKey("extra_grad")) {
                            String extraGrad = (String) data.get("extra_grad");
                            if (extraGrad != null && !extraGrad.trim().isEmpty()) {
                                Log.d(TAG, "추가 졸업 요건 로드 성공: " + extraGrad);
                                listener.onSuccess(extraGrad);
                            } else {
                                Log.d(TAG, "추가 졸업 요건이 비어있음");
                                listener.onSuccess(null);
                            }
                        } else {
                            Log.d(TAG, "extra_grad 필드가 없음");
                            listener.onSuccess(null);
                        }
                    } else {
                        Log.w(TAG, "학부 문서 없음: " + department);
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "추가 졸업 요건 로드 실패: " + department, e);
                    listener.onFailure(e);
                });
    }

    // 총 학점 조회 (graduation_meta/catalog/departments/{department}의 '총 학점' 필드)
    public void loadTotalCredits(String department, OnTotalCreditsLoadedListener listener) {
        Log.d(TAG, "=== 총 학점 조회 시작: " + department + " ===");

        // graduation_meta/catalog/departments/{department} 경로에서 '총 학점' 필드 조회
        db.collection("graduation_meta")
                .document("catalog")
                .collection("departments")
                .document(department)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            // '총 학점' 필드 조회
                            Object totalCreditsObj = data.get("총 학점");
                            Log.d(TAG, department + " 문서 데이터: " + data.toString());

                            if (totalCreditsObj != null) {
                                Integer totalCredits = null;
                                if (totalCreditsObj instanceof Long) {
                                    totalCredits = ((Long) totalCreditsObj).intValue();
                                } else if (totalCreditsObj instanceof Integer) {
                                    totalCredits = (Integer) totalCreditsObj;
                                } else if (totalCreditsObj instanceof String) {
                                    try {
                                        totalCredits = Integer.parseInt((String) totalCreditsObj);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "총 학점 값을 숫자로 변환할 수 없습니다: " + totalCreditsObj);
                                        listener.onFailure(new Exception("총 학점 값 변환 실패: " + totalCreditsObj));
                                        return;
                                    }
                                }

                                if (totalCredits != null) {
                                    Log.d(TAG, department + " 총 학점 조회 성공: " + totalCredits);
                                    listener.onSuccess(totalCredits);
                                } else {
                                    Log.w(TAG, department + "의 '총 학점' 필드가 올바르지 않습니다: " + totalCreditsObj);
                                    listener.onFailure(new Exception("총 학점 필드 형식 오류"));
                                }
                            } else {
                                Log.w(TAG, department + "에 '총 학점' 필드가 없습니다");
                                listener.onFailure(new Exception("총 학점 필드 없음"));
                            }
                        } else {
                            Log.w(TAG, department + " 문서가 비어있습니다");
                            listener.onFailure(new Exception("문서 데이터 없음"));
                        }
                    } else {
                        Log.w(TAG, department + " 문서가 존재하지 않습니다");
                        listener.onFailure(new Exception("문서 존재하지 않음"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, department + " 총 학점 조회 실패", e);
                    listener.onFailure(e);
                });
    }

    // 졸업이수학점 데이터 추가 메서드
    public interface OnCreditRequirementsAddedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void addCreditRequirements(String documentId, CreditRequirements requirements, OnCreditRequirementsAddedListener listener) {
        Log.d(TAG, "졸업이수학점 데이터 추가 시작: " + documentId);

        Map<String, Object> requirementsData = new HashMap<>();
        requirementsData.put("총이수", requirements.totalCredits);
        requirementsData.put("전공필수", requirements.majorRequired);
        requirementsData.put("전공선택", requirements.majorElective);
        requirementsData.put("교양필수", requirements.generalRequired);
        requirementsData.put("교양선택", requirements.generalElective);
        requirementsData.put("소양", requirements.liberalArts);
        requirementsData.put("자율선택", requirements.freeElective);

        db.collection("graduation_requirements")
                .document(documentId)
                .update("requirements", requirementsData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "졸업이수학점 데이터 추가 성공: " + documentId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업이수학점 데이터 추가 실패: " + documentId, e);
                    listener.onFailure(e);
                });
    }

    // Firestore 문서 조회 및 출력
    public void inspectDocument(String documentId) {
        Log.d(TAG, "=== 문서 상세 조회 시작: " + documentId + " ===");

        db.collection("graduation_requirements")
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Log.d(TAG, "문서 " + documentId + " 전체 구조:");
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                Log.d(TAG, "  키: " + entry.getKey() + " -> 값: " + entry.getValue());
                                if (entry.getValue() instanceof Map) {
                                    Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                                    for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
                                        Log.d(TAG, "    하위키: " + nestedEntry.getKey() + " -> 값: " + nestedEntry.getValue());
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "문서가 존재하지 않음: " + documentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패: " + documentId, e);
                });
    }

    // IT학부_멀티미디어_20 졸업이수학점 요건 추가
    public void addIT멀티미디어20CreditRequirements() {
        // 사용자가 준비한 데이터를 기반으로 설정
        CreditRequirements requirements = new CreditRequirements(
            130, // 총졸업이수학점
            27,  // 전공필수
            18,  // 전공선택
            16,  // 교양필수
            8,   // 교양선택
            6,   // 소양
            20,  // 자율선택 (잔여학점)
            36,  // 학부공통
            12   // 전공심화
        );

        addCreditRequirements("IT학부_멀티미디어_2020", requirements, new OnCreditRequirementsAddedListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "IT학부_멀티미디어_2020 졸업이수학점 요건 추가 완료");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "IT학부_멀티미디어_2020 졸업이수학점 요건 추가 실패", e);
            }
        });

        // 2023학번용 데이터도 추가 (23-25학번은 2023 데이터 사용)
        CreditRequirements requirements2023 = new CreditRequirements(
            130, // 총졸업이수학점
            27,  // 전공필수
            18,  // 전공선택
            16,  // 교양필수
            8,   // 교양선택
            6,   // 소양
            20,  // 자율선택
            36,  // 학부공통
            12   // 전공심화
        );

        addCreditRequirements("IT학부_멀티미디어_2023", requirements2023, new OnCreditRequirementsAddedListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "IT학부_멀티미디어_2023 졸업이수학점 요건 추가 완료");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "IT학부_멀티미디어_2023 졸업이수학점 요건 추가 실패", e);
            }
        });
    }
}