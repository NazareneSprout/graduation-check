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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Firebase Firestore 데이터 관리 싱글톤 클래스
 *
 * <p>이 클래스는 애플리케이션의 모든 Firestore 데이터 접근을 중앙 집중식으로 관리합니다.
 * 주요 기능으로는 졸업 요건 데이터 조회, 강의 정보 관리, 학부/트랙 정보 제공 등이 있습니다.</p>
 *
 * <h3>주요 컬렉션 구조:</h3>
 * <ul>
 *   <li><code>graduation_requirements</code> - 학부별/트랙별/연도별 졸업 요건 및 강의 정보</li>
 *   <li><code>graduation_meta</code> - 학부 메타데이터 (총 학점, 추가 졸업 조건 등)</li>
 *   <li><code>student_progress</code> - 개별 학생의 학습 진행 상황</li>
 * </ul>
 *
 * <h3>성능 최적화:</h3>
 * <ul>
 *   <li><strong>캐싱 시스템:</strong> 5분 유효성 메모리 캐시로 네트워크 요청 최소화</li>
 *   <li><strong>N+1 쿼리 해결:</strong> DocumentSnapshot 캐싱으로 중복 쿼리 방지</li>
 *   <li><strong>동시 로딩:</strong> 학번/학부/트랙 데이터 병렬 처리</li>
 *   <li><strong>Single-flight 패턴:</strong> 중복 요청 자동 병합</li>
 * </ul>
 *
 * @version 1.0
 * @since 2025-09-26
 * @author SakMvp1 Development Team
 */
@SuppressWarnings("unchecked")
public class FirebaseDataManager {
    /** 로깅용 태그 */
    private static final String TAG = "FirebaseDataManager";

    /** 싱글톤 인스턴스 */
    private static FirebaseDataManager instance;

    /** Firestore 데이터베이스 인스턴스 */
    private FirebaseFirestore db;

    // ========== 캐싱 시스템 ==========

    /** 학번(연도) 목록 캐시 - Key: "years", Value: ["2025", "2024", ...] */
    private Map<String, List<String>> studentYearsCache = new HashMap<>();

    /** 학부 목록 캐시 - Key: "departments", Value: ["IT학부", "태권도학부", ...] */
    private Map<String, List<String>> departmentsCache = new HashMap<>();

    /** 학부별 트랙 캐시 - Key: 학부명, Value: ["인공지능", "멀티미디어", ...] */
    private Map<String, List<String>> tracksCache = new HashMap<>();

    /** 강의 정보 캐시 - Key: "학부_트랙_연도_카테고리", Value: CourseInfo 리스트 */
    private Map<String, List<CourseInfo>> coursesCache = new HashMap<>();

    /** 졸업 요건 원시 데이터 캐시 - Key: "학부_트랙_연도", Value: Firestore 문서 데이터 */
    private Map<String, Object> graduationCache = new HashMap<>();

    // ========== N+1 쿼리 최적화 및 고급 캐싱 ==========

    /**
     * 교양 문서 선택 캐시
     * Key: "학부|연도" (예: "IT학부|2025")
     * Value: 최종 사용할 문서ID ("교양_IT학부_2025" 또는 "교양_공통_2025")
     *
     * 교양 강의는 학부별 특화 문서를 우선 사용하고, 없으면 공통 문서를 fallback으로 사용
     */
    private final Map<String, String> generalDocCache = new ConcurrentHashMap<>();

    /**
     * DocumentSnapshot 캐시 (N+1 쿼리 해결용)
     * Key: Firestore 문서 ID
     * Value: 캐시된 DocumentSnapshot 객체
     *
     * 동일한 문서에 대한 반복적인 Firestore 조회를 방지하여 성능을 크게 개선
     */
    private final Map<String, DocumentSnapshot> docSnapshotCache = new ConcurrentHashMap<>();

    /**
     * 전공 문서 캐시
     * Key: "학부|트랙|년도" (예: "IT학부|멀티미디어|2025")
     * Value: 최종 사용할 전공 문서 DocumentSnapshot
     */
    private final Map<String, DocumentSnapshot> majorDocCache = new ConcurrentHashMap<>();

    /**
     * 학부공통 문서 캐시
     * Key: "학부|트랙|년도" (예: "IT학부|멀티미디어|2025")
     * Value: 학부공통 문서 DocumentSnapshot
     */
    private final Map<String, DocumentSnapshot> deptCommonDocCache = new ConcurrentHashMap<>();

    /**
     * 캐시 타임스탬프 관리
     * Key: 캐시 키
     * Value: 캐시 생성 시간 (밀리초)
     */
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    /** 캐시 유효 시간: 5분 (300,000ms) */
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000;

    /**
     * private 생성자 (싱글톤 패턴)
     *
     * <p>Firestore 인스턴스를 초기화하고, 초기화 실패 시 RuntimeException을 발생시킵니다.
     * 이를 통해 애플리케이션 전체에서 데이터 접근 불가 상황을 조기에 감지할 수 있습니다.</p>
     *
     * @throws RuntimeException Firebase 초기화 실패 시
     */
    private FirebaseDataManager() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseFirestore 인스턴스 초기화 성공");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseFirestore 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패 - 네트워크 연결 또는 Firebase 설정을 확인하세요", e);
        }
    }

    /**
     * 싱글톤 인스턴스 획득 (Thread-Safe)
     *
     * <p>애플리케이션 전체에서 단일 FirebaseDataManager 인스턴스를 공유합니다.
     * synchronized 키워드로 멀티스레드 환경에서의 안전성을 보장합니다.</p>
     *
     * @return FirebaseDataManager 싱글톤 인스턴스
     * @throws RuntimeException Firebase 초기화 실패 시 (최초 호출 시에만)
     */
    public static synchronized FirebaseDataManager getInstance() {
        if (instance == null) {
            instance = new FirebaseDataManager();
        }
        return instance;
    }

    // ========== 콜백 인터페이스 정의 ==========

    /**
     * 교양 문서 해결 콜백 인터페이스
     *
     * <p>교양 강의 문서는 학부별 특화 문서("교양_IT학부_2025")를 우선 사용하고,
     * 없을 경우 공통 문서("교양_공통_2025")를 fallback으로 사용합니다.
     * 이러한 복잡한 로직의 결과를 비동기적으로 전달하기 위한 콜백입니다.</p>
     */
    public interface OnGeneralDocResolvedListener {
        /**
         * 교양 문서가 성공적으로 해결되었을 때 호출
         *
         * @param docId 최종 결정된 문서 ID (예: "교양_IT학부_2025" 또는 "교양_공통_2025")
         * @param snapshot 문서의 전체 내용 (N+1 쿼리 최적화를 위해 제공)
         */
        void onResolved(String docId, DocumentSnapshot snapshot);

        /**
         * 학부별 문서와 공통 문서 모두 존재하지 않을 때 호출
         * 해당 연도의 교양 강의 데이터가 아직 Firestore에 업로드되지 않은 경우
         */
        void onNotFound();

        /**
         * Firestore 조회 중 네트워크 오류 등의 문제가 발생했을 때 호출
         *
         * @param e 발생한 예외 객체
         */
        void onError(Exception e);
    }

    /**
     * 교양 문서 ID 결정 (우선순위 기반 해결)
     *
     * <p>교양 강의 문서는 다음 우선순위로 결정됩니다:</p>
     * <ol>
     *   <li><strong>관리자 설정 문서:</strong> 졸업요건 문서의 generalEducationDocId 필드 (최우선)</li>
     *   <li><strong>학부별 특화 문서:</strong> "교양_{학부}_{연도}" (예: "교양_IT학부_2025")</li>
     *   <li><strong>공통 문서 (Fallback):</strong> "교양_공통_{연도}" (예: "교양_공통_2025")</li>
     *   <li><strong>문서 없음:</strong> 모두 존재하지 않으면 onNotFound() 호출</li>
     * </ol>
     *
     * <p><strong>성능 최적화:</strong> 한 번 해결된 결과는 generalDocCache에 캐싱되어
     * 동일한 학부-연도 조합에 대한 반복 요청 시 즉시 반환됩니다.</p>
     *
     * @param department 학부명 (null 또는 공백 허용)
     * @param track 트랙명 (null 허용, generalEducationDocId 조회에 필요)
     * @param year 연도 (4자리, 예: "2025")
     * @param cb 결과를 받을 콜백 리스너
     */
    private void resolveGeneralDocId(String department, String track, String year, OnGeneralDocResolvedListener cb) {
        // 입력 매개변수 정규화 (null 안전성 확보)
        String dept = (department == null ? "" : department.trim());
        String tr = (track == null ? "" : track.trim());
        String yr = (year == null ? "" : year.trim());

        // Firestore 문서 ID 생성 (명명 규칙에 따라)
        final String graduationReqDocId = dept + "_" + tr + "_" + yr;  // 졸업요건 문서
        final String deptDocId = "교양_" + dept + "_" + yr;              // 학부별 특화 문서
        final String commonDocId = "교양_공통_" + yr;                     // 공통 Fallback 문서
        final String cacheKey = dept + "|" + tr + "|" + yr;             // 캐시 키 (track 포함)

        // 캐시 히트 - 캐시된 문서 ID로 다시 조회 (DocumentSnapshot 필요)
        if (generalDocCache.containsKey(cacheKey)) {
            String cachedDocId = generalDocCache.get(cacheKey);
            db.collection("graduation_requirements").document(cachedDocId).get()
                    .addOnSuccessListener(ds -> {
                        if (ds.exists()) {
                            cb.onResolved(cachedDocId, ds);
                        } else {
                            // 캐시 무효화 후 재시도
                            generalDocCache.remove(cacheKey);
                            resolveGeneralDocId(department, track, year, cb);
                        }
                    })
                    .addOnFailureListener(cb::onError);
            return;
        }

        // 0순위: 졸업요건 문서에서 관리자가 설정한 generalEducationDocId 확인
        db.collection("graduation_requirements").document(graduationReqDocId).get()
                .addOnSuccessListener(gradDoc -> {
                    String customDocId = gradDoc.getString("generalEducationDocId");

                    if (customDocId != null && !customDocId.trim().isEmpty()) {
                        // 관리자가 설정한 교양 문서 ID가 있으면 최우선 사용
                        Log.d(TAG, "관리자 설정 교양 문서 사용: " + customDocId);
                        db.collection("graduation_requirements").document(customDocId).get()
                                .addOnSuccessListener(customDs -> {
                                    if (customDs.exists()) {
                                        generalDocCache.put(cacheKey, customDocId);
                                        docSnapshotCache.put(cacheKey, customDs);
                                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                                        cb.onResolved(customDocId, customDs);
                                    } else {
                                        // 설정된 문서가 없으면 기본 로직으로 fallback
                                        Log.w(TAG, "설정된 교양 문서 없음, 기본 로직 사용: " + customDocId);
                                        resolveWithDefaultLogic(dept, yr, deptDocId, commonDocId, cacheKey, cb);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "설정된 교양 문서 조회 실패, 기본 로직 사용", e);
                                    resolveWithDefaultLogic(dept, yr, deptDocId, commonDocId, cacheKey, cb);
                                });
                    } else {
                        // generalEducationDocId 필드가 없으면 기본 로직 사용
                        resolveWithDefaultLogic(dept, yr, deptDocId, commonDocId, cacheKey, cb);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업요건 문서 조회 실패, 기본 로직 사용", e);
                    resolveWithDefaultLogic(dept, yr, deptDocId, commonDocId, cacheKey, cb);
                });
    }

    /**
     * 기본 교양 문서 해결 로직 (학부별 → 공통 순서)
     */
    private void resolveWithDefaultLogic(String dept, String yr, String deptDocId, String commonDocId, String cacheKey, OnGeneralDocResolvedListener cb) {
        // 1순위: 학부 전용
        db.collection("graduation_requirements").document(deptDocId).get()
                .addOnSuccessListener(ds -> {
                    if (ds.exists()) {
                        // 캐시 저장 (문서 ID + DocumentSnapshot + 타임스탬프)
                        generalDocCache.put(cacheKey, deptDocId);
                        docSnapshotCache.put(cacheKey, ds);
                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                        cb.onResolved(deptDocId, ds);
                    } else {
                        // 2순위: 공통
                        db.collection("graduation_requirements").document(commonDocId).get()
                                .addOnSuccessListener(ds2 -> {
                                    if (ds2.exists()) {
                                        // 캐시 저장 (공통 문서)
                                        generalDocCache.put(cacheKey, commonDocId);
                                        docSnapshotCache.put(cacheKey, ds2);
                                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                                        cb.onResolved(commonDocId, ds2);
                                    } else {
                                        cb.onNotFound();
                                    }
                                })
                                .addOnFailureListener(cb::onError);
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * 백그라운드 캐시 새로고침 (비동기)
     */
    private void refreshCacheInBackground(String department, String year, String cacheKey) {
        String dept = (department == null ? "" : department.trim());
        String yr = (year == null ? "" : year.trim());
        final String deptDocId = "교양_" + dept + "_" + yr;
        final String commonDocId = "교양_공통_" + yr;

        // 백그라운드로 업데이트 (사용자에게 영향 없음)
        db.collection("graduation_requirements").document(deptDocId).get()
                .addOnSuccessListener(ds -> {
                    if (ds.exists()) {
                        generalDocCache.put(cacheKey, deptDocId);
                        docSnapshotCache.put(cacheKey, ds);
                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                        Log.d(TAG, "Background refresh success: " + cacheKey);
                    } else {
                        // 공통 문서 시도
                        db.collection("graduation_requirements").document(commonDocId).get()
                                .addOnSuccessListener(ds2 -> {
                                    if (ds2.exists()) {
                                        generalDocCache.put(cacheKey, commonDocId);
                                        docSnapshotCache.put(cacheKey, ds2);
                                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                                        Log.d(TAG, "Background refresh success (common): " + cacheKey);
                                    }
                                })
                                .addOnFailureListener(e -> Log.w(TAG, "Background refresh failed (common): " + cacheKey, e));
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Background refresh failed: " + cacheKey, e));
    }

    // ---------- 학번/학부/트랙 조회 ----------

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

    // 모든 트랙 데이터 조회 (학부별 맵으로 반환)
    public interface OnAllTracksLoadedListener {
        void onSuccess(Map<String, List<String>> allTracks); // 학부명 -> 트랙 리스트
        void onFailure(Exception e);
    }

    public void loadTracksByDepartment(String departmentName, OnTracksLoadedListener listener) {
        // 캐시 확인 - 즉시 반환 가능
        if (tracksCache.containsKey(departmentName)) {
            List<String> cachedTracks = tracksCache.get(departmentName);
            Log.d(TAG, departmentName + " 트랙 데이터 캐시 히트: " + cachedTracks.size() + "개");
            listener.onSuccess(new ArrayList<>(cachedTracks)); // 방어적 복사
            return;
        }

        // 캐시 미스 - Firestore에서 로드
        Log.d(TAG, departmentName + " 트랙 데이터 캐시 미스, Firestore 조회 중...");
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

                    // 캐시에 저장
                    tracksCache.put(departmentName, new ArrayList<>(tracks));

                    Log.d(TAG, departmentName + " 트랙 데이터 로드 및 캐시 성공: " + tracks.size() + "개 - " + tracks);
                    listener.onSuccess(tracks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, departmentName + " 트랙 데이터 로드 실패", e);
                    listener.onFailure(e);
                });
    }

    /**
     * 모든 학부의 트랙 데이터를 한번에 로드하여 캐시에 저장
     * - graduation_requirements 컬렉션을 한번만 조회
     * - 모든 학부별 트랙을 추출하여 캐시에 저장
     * - 결과를 Map<학부명, 트랙리스트>로 반환
     */
    public void loadAllTracks(OnAllTracksLoadedListener listener) {
        Log.d(TAG, "모든 트랙 데이터 로드 시작 (한번의 Firestore 조회)");

        db.collection("graduation_requirements")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Set<String>> departmentTracksMap = new HashMap<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();
                        // 문서 ID 형식: "IT학부_멀티미디어_2020"
                        String[] parts = docId.split("_");
                        if (parts.length >= 3) {
                            String department = parts[0]; // 첫 번째 부분이 학부
                            String track = parts[1]; // 두 번째 부분이 트랙

                            // 교양 문서는 제외
                            if (!department.equals("교양")) {
                                departmentTracksMap.computeIfAbsent(department, k -> new HashSet<>()).add(track);
                            }
                        }
                    }

                    // Set을 List로 변환하고 정렬
                    Map<String, List<String>> result = new HashMap<>();
                    for (Map.Entry<String, Set<String>> entry : departmentTracksMap.entrySet()) {
                        String department = entry.getKey();
                        List<String> tracks = new ArrayList<>(entry.getValue());
                        tracks.sort(String::compareTo); // 알파벳 순 정렬

                        // 캐시에 저장
                        tracksCache.put(department, new ArrayList<>(tracks));
                        result.put(department, tracks);

                        Log.d(TAG, department + " 트랙 캐시 저장 완료: " + tracks.size() + "개 - " + tracks);
                    }

                    Log.d(TAG, "모든 트랙 데이터 로드 완료: " + result.size() + "개 학부");
                    listener.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "모든 트랙 데이터 로드 실패", e);
                    listener.onFailure(e);
                });
    }

    // ---------- 졸업 요건/학점 ----------

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
        // 졸업요건 문서 ID 형식: "졸업요건_IT학부_멀티미디어_2023"
        String gradDocId = "졸업요건_" + department + "_" + track + "_" + year;

        Log.d(TAG, "졸업 요건 조회 시작: " + gradDocId);

        db.collection("graduation_requirements").document(gradDocId)
                .get()
                .addOnSuccessListener(gradDoc -> {
                    if (gradDoc.exists()) {
                        Log.d(TAG, "졸업요건 문서 조회 성공: " + gradDocId);

                        // 새로운 구조: majorDocRef와 generalDocRef 확인
                        String majorDocRef = gradDoc.getString("majorDocRef");
                        String generalDocRef = gradDoc.getString("generalDocRef");

                        if (majorDocRef != null && !majorDocRef.isEmpty()) {
                            Log.d(TAG, "전공 문서 참조: " + majorDocRef + ", 교양 문서 참조: " + generalDocRef);
                            // 새 구조: 전공 + 교양 문서를 병합
                            loadAndMergeMajorAndGeneralDocs(gradDoc.getData(), majorDocRef, generalDocRef, listener);
                        } else {
                            // 구 구조 호환: 참조 없으면 현재 문서가 전공 문서 (legacy)
                            Log.d(TAG, "구 구조 호환 모드: 졸업요건 문서가 전공 데이터 포함");
                            String generalEducationDocId = gradDoc.getString("generalEducationDocId");
                            if (generalEducationDocId != null && !generalEducationDocId.isEmpty()) {
                                loadAndMergeGeneralEducationDocument(gradDoc.getData(), generalEducationDocId, listener);
                            } else {
                                listener.onSuccess(gradDoc.getData());
                            }
                        }
                    } else {
                        Log.w(TAG, "졸업 요건 문서 없음: " + gradDocId);
                        listener.onFailure(new Exception("해당 조건의 졸업요건을 찾을 수 없습니다: " + gradDocId));
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "졸업 요건 문서 조회 실패: " + gradDocId;
                    Log.e(TAG, errorMsg, e);
                    listener.onFailure(new Exception(errorMsg, e));
                });
    }

    /**
     * 전공 문서와 교양 문서를 로드하여 졸업요건 데이터와 병합 (새 구조)
     * 졸업요건 문서: 모든 학점 정보 + 문서 참조
     * 전공 문서: rules만 (학기별 과목 목록)
     * 교양 문서: rules만 (requirements 배열)
     */
    private void loadAndMergeMajorAndGeneralDocs(Map<String, Object> gradData,
                                                   String majorDocRef,
                                                   String generalDocRef,
                                                   OnGraduationRequirementsLoadedListener listener) {
        Log.d(TAG, "전공/교양 문서 병합 시작");
        Log.d(TAG, "학점 정보는 졸업요건 문서에서 이미 로드됨");

        // 1. 전공 문서 로드 (과목 목록만)
        db.collection("graduation_requirements").document(majorDocRef)
                .get()
                .addOnSuccessListener(majorDoc -> {
                    if (majorDoc.exists() && majorDoc.getData() != null) {
                        Map<String, Object> majorData = majorDoc.getData();
                        Log.d(TAG, "전공 문서 로드 성공: " + majorDocRef);

                        // 전공 과목 목록만 복사 (학점 정보는 이미 gradData에 있음)
                        if (majorData.containsKey("rules")) {
                            gradData.put("rules", majorData.get("rules"));
                            Log.d(TAG, "전공 과목 목록(rules) 복사 완료");
                        }

                        // 전공 대체과목 규칙 복사
                        if (majorData.containsKey("replacementRules")) {
                            gradData.put("replacementRules", majorData.get("replacementRules"));
                            Log.d(TAG, "전공 대체과목 규칙 복사 완료");
                        }

                        // 2. 교양 문서 로드 (과목 목록만)
                        if (generalDocRef != null && !generalDocRef.isEmpty()) {
                            db.collection("graduation_requirements").document(generalDocRef)
                                    .get()
                                    .addOnSuccessListener(generalDoc -> {
                                        if (generalDoc.exists() && generalDoc.getData() != null) {
                                            Map<String, Object> generalData = generalDoc.getData();
                                            Log.d(TAG, "교양 문서 로드 성공: " + generalDocRef);

                                            // 교양 과목 목록 병합 (학점 정보는 이미 gradData에 있음)
                                            Object generalRulesObj = generalData.get("rules");
                                            if (generalRulesObj instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> generalRules = (Map<String, Object>) generalRulesObj;

                                                // 전공 rules 가져오기 (없으면 생성)
                                                Object rulesObj = gradData.get("rules");
                                                Map<String, Object> rules;
                                                if (rulesObj instanceof Map) {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> temp = (Map<String, Object>) rulesObj;
                                                    rules = new HashMap<>(temp);
                                                } else {
                                                    rules = new HashMap<>();
                                                }

                                                // 교양 requirements를 rules에 추가
                                                if (generalRules.containsKey("requirements")) {
                                                    rules.put("generalRequirements", generalRules.get("requirements"));
                                                    Log.d(TAG, "교양 과목 목록(requirements) 병합 완료");
                                                }

                                                gradData.put("rules", rules);
                                            }

                                            // 교양 대체과목 규칙 병합 (있는 경우)
                                            if (generalData.containsKey("replacementRules")) {
                                                // 기존 replacementRules와 병합
                                                Object existingRulesObj = gradData.get("replacementRules");
                                                if (existingRulesObj instanceof List) {
                                                    @SuppressWarnings("unchecked")
                                                    List<Object> existingRules = new ArrayList<>((List<Object>) existingRulesObj);
                                                    Object generalReplacementRulesObj = generalData.get("replacementRules");
                                                    if (generalReplacementRulesObj instanceof List) {
                                                        @SuppressWarnings("unchecked")
                                                        List<Object> generalReplacementRules = (List<Object>) generalReplacementRulesObj;
                                                        existingRules.addAll(generalReplacementRules);
                                                    }
                                                    gradData.put("replacementRules", existingRules);
                                                    Log.d(TAG, "교양 대체과목 규칙 병합 완료");
                                                } else {
                                                    gradData.put("replacementRules", generalData.get("replacementRules"));
                                                }
                                            }

                                            Log.d(TAG, "전공/교양 문서 병합 완료");
                                            listener.onSuccess(gradData);
                                        } else {
                                            Log.w(TAG, "교양 문서를 찾을 수 없음: " + generalDocRef + " - 전공 데이터만 반환");
                                            listener.onSuccess(gradData);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "교양 문서 로드 실패: " + generalDocRef + " - 전공 데이터만 반환", e);
                                        listener.onSuccess(gradData);
                                    });
                        } else {
                            Log.d(TAG, "교양 문서 참조 없음 - 전공 데이터만 반환");
                            listener.onSuccess(gradData);
                        }
                    } else {
                        Log.w(TAG, "전공 문서를 찾을 수 없음: " + majorDocRef);
                        listener.onFailure(new Exception("전공 문서를 찾을 수 없습니다: " + majorDocRef));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 문서 로드 실패: " + majorDocRef, e);
                    listener.onFailure(new Exception("전공 문서 로드 실패: " + majorDocRef, e));
                });
    }

    /**
     * 교양 문서를 로드하여 전공 문서 데이터에 병합 (구 구조 호환)
     */
    private void loadAndMergeGeneralEducationDocument(Map<String, Object> majorData,
                                                     String generalDocId,
                                                     OnGraduationRequirementsLoadedListener listener) {
        db.collection("graduation_requirements").document(generalDocId)
                .get()
                .addOnSuccessListener(generalDoc -> {
                    if (generalDoc.exists() && generalDoc.getData() != null) {
                        Map<String, Object> generalData = generalDoc.getData();
                        Log.d(TAG, "교양 문서 로드 성공: " + generalDocId);

                        // 교양 문서의 rules를 전공 문서에 병합
                        // 교양 문서 구조: { rules: { requirements: [...] } }
                        Object generalRulesObj = generalData.get("rules");
                        if (generalRulesObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> generalRules = (Map<String, Object>) generalRulesObj;

                            // 전공 문서의 rules 가져오기 (없으면 생성)
                            Object majorRulesObj = majorData.get("rules");
                            Map<String, Object> majorRules;
                            if (majorRulesObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> temp = (Map<String, Object>) majorRulesObj;
                                majorRules = new java.util.HashMap<>(temp);
                            } else {
                                majorRules = new java.util.HashMap<>();
                            }

                            // 교양 requirements를 전공 rules에 추가
                            if (generalRules.containsKey("requirements")) {
                                majorRules.put("generalRequirements", generalRules.get("requirements"));
                                Log.d(TAG, "교양 과목 데이터 병합 완료");
                            }

                            // 병합된 rules를 전공 데이터에 다시 저장
                            majorData.put("rules", majorRules);
                        }

                        listener.onSuccess(majorData);
                    } else {
                        Log.w(TAG, "교양 문서를 찾을 수 없음: " + generalDocId + " - 전공 문서만 반환");
                        listener.onSuccess(majorData);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "교양 문서 로드 실패: " + generalDocId + " - 전공 문서만 반환", e);
                    // 교양 문서 로드 실패해도 전공 문서는 반환
                    listener.onSuccess(majorData);
                });
    }

    // 졸업이수학점 요건 로드
    public void loadCreditRequirements(String department, String track, String year,
                                       OnCreditRequirementsLoadedListener listener) {
        // 졸업요건 문서 ID 형식: "졸업요건_IT학부_멀티미디어_2023"
        String gradDocId = "졸업요건_" + department + "_" + track + "_" + year;
        Log.d(TAG, "졸업이수학점 요건 조회 시작: " + gradDocId);

        db.collection("graduation_requirements").document(gradDocId)
                .get()
                .addOnSuccessListener(gradDoc -> {
                    if (gradDoc.exists()) {
                        Map<String, Object> gradData = gradDoc.getData();

                        // 총 이수학점 가져오기
                        int totalCredits = getIntValue(gradData, "totalCredits", 130);

                        // 새로운 구조: majorDocRef와 generalDocRef 확인
                        String majorDocRef = gradDoc.getString("majorDocRef");
                        String generalDocRef = gradDoc.getString("generalDocRef");

                        if (majorDocRef != null && !majorDocRef.isEmpty()) {
                            Log.d(TAG, "새 구조: 전공/교양 문서 참조 발견");
                            loadCreditRequirementsFromSeparateDocs(majorDocRef, generalDocRef, gradData, totalCredits, listener);
                        } else {
                            // 구 구조 호환
                            Log.d(TAG, "구 구조 호환 모드: 졸업요건 문서에서 직접 학점 읽기");
                            String legacyDocId = department + "_" + track + "_" + year;
                            loadDetailedCreditRequirements(legacyDocId, totalCredits, listener);
                        }
                    } else {
                        Log.w(TAG, "졸업요건 문서 없음: " + gradDocId);
                        listener.onFailure(new Exception("졸업요건 문서를 찾을 수 없습니다: " + gradDocId));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업요건 문서 조회 실패: " + gradDocId, e);
                    listener.onFailure(new Exception("졸업요건 문서 조회 실패: " + gradDocId, e));
                });
    }

    /**
     * 졸업요건 문서에서 학점 요구사항 로드 (새 구조)
     * 새 구조에서는 모든 학점 정보가 졸업요건 문서에 있음
     * 전공/교양 문서는 과목 목록만 포함
     */
    private void loadCreditRequirementsFromSeparateDocs(String majorDocRef,
                                                         String generalDocRef,
                                                         Map<String, Object> gradData,
                                                         int totalCredits,
                                                         OnCreditRequirementsLoadedListener listener) {
        Log.d(TAG, "졸업요건 문서에서 학점 요구사항 추출 시작");
        Log.d(TAG, "새 구조: 모든 학점 정보는 졸업요건 문서에 있음");

        // 졸업요건 문서에서 모든 학점 정보 추출
        int majorRequired = getIntValue(gradData, "전공필수", 0);
        int majorElective = getIntValue(gradData, "전공선택", 0);
        int departmentCommon = getIntValue(gradData, "학부공통", 0);
        int majorAdvanced = getIntValue(gradData, "전공심화", 0);
        int generalRequired = getIntValue(gradData, "교양필수", 0);
        int generalElective = getIntValue(gradData, "교양선택", 0);
        int liberalArts = getIntValue(gradData, "소양", 0);

        // 자율선택/잔여학점
        int freeElective = getIntValue(gradData, "자율선택", 0);
        if (freeElective == 0) {
            freeElective = getIntValue(gradData, "잔여학점", 0);
        }

        // 자율선택이 명시되지 않은 경우 계산
        if (freeElective == 0) {
            int specifiedCredits = majorRequired + majorElective + departmentCommon + majorAdvanced +
                    generalRequired + generalElective + liberalArts;
            freeElective = totalCredits - specifiedCredits;
            if (freeElective < 0) freeElective = 0;
        }

        // CreditRequirements 객체 생성
        CreditRequirements creditReqs = new CreditRequirements(
                totalCredits, majorRequired, majorElective,
                generalRequired, generalElective, liberalArts, freeElective,
                departmentCommon, majorAdvanced
        );

        Log.d(TAG, "학점 요구사항 추출 완료: " + creditReqs.toString());
        listener.onSuccess(creditReqs);
    }

    // 상세 졸업이수학점 요건 조회 (총 학점이 이미 확보된 상태) - 구 구조 호환
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
                                    @SuppressWarnings("unchecked")
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
                        String errorMsg = "졸업이수학점 문서를 찾을 수 없습니다: " + documentId;
                        Log.e(TAG, errorMsg);
                        listener.onFailure(new Exception(errorMsg));
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "졸업이수학점 문서 조회 실패: " + documentId;
                    Log.e(TAG, errorMsg, e);
                    listener.onFailure(new Exception(errorMsg, e));
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

    // ---------- 일반 Firestore 읽기 ----------

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

        try {
            if (db == null) {
                listener.onFailure(new Exception("Firestore 인스턴스가 null입니다"));
                return;
            }

            db.getFirestoreSettings();
            Log.d(TAG, "Firestore 설정 확인 완료");

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

    // ---------- 강의 로딩 ----------

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

        // 모든 학번은 해당 연도 그대로 사용
        String actualYear = year;

        // graduation_requirements 컬렉션에서 해당 학과/트랙의 전공 강의 조회
        String documentId = department + "_" + track + "_" + actualYear;
        String cacheKey = department + "|" + track + "|" + actualYear;

        // 캐시 확인 (유효 시간 체크 포함)
        if (majorDocCache.containsKey(cacheKey)) {
            Long cachedTime = cacheTimestamps.get(cacheKey);
            if (cachedTime != null && (System.currentTimeMillis() - cachedTime) < CACHE_VALIDITY_MS) {
                Log.d(TAG, "전공 문서 캐시 히트: " + cacheKey);
                DocumentSnapshot cachedDoc = majorDocCache.get(cacheKey);
                loadMajorCoursesFromSnapshot(cachedDoc, category, listener);
                return;
            } else {
                // 캐시 만료
                Log.d(TAG, "전공 문서 캐시 만료, 재조회: " + cacheKey);
                majorDocCache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey);
            }
        }

        // 먼저 현재 졸업요건 문서에서 majorDocId가 설정되어 있는지 확인
        db.collection("graduation_requirements").document(documentId)
                .get()
                .addOnSuccessListener(mainDoc -> {
                    String customMajorDocId = mainDoc.getString("majorDocId");

                    if (customMajorDocId != null && !customMajorDocId.trim().isEmpty()) {
                        // 관리자가 설정한 전공 문서 사용
                        Log.d(TAG, "관리자 설정 전공 문서 사용: " + customMajorDocId);
                        db.collection("graduation_requirements").document(customMajorDocId)
                                .get()
                                .addOnSuccessListener(customDoc -> {
                                    if (customDoc.exists()) {
                                        // 캐시 저장
                                        majorDocCache.put(cacheKey, customDoc);
                                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                                        loadMajorCoursesFromSnapshot(customDoc, category, listener);
                                    } else {
                                        Log.w(TAG, "지정된 전공 문서 없음, 기본 문서 사용: " + documentId);
                                        // 기본 문서로 폴백
                                        majorDocCache.put(cacheKey, mainDoc);
                                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                                        loadMajorCoursesFromSnapshot(mainDoc, category, listener);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "지정된 전공 문서 조회 실패, 기본 문서 사용", e);
                                    // 기본 문서로 폴백
                                    majorDocCache.put(cacheKey, mainDoc);
                                    cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                                    loadMajorCoursesFromSnapshot(mainDoc, category, listener);
                                });
                    } else {
                        // 기본 문서 사용
                        Log.d(TAG, "기본 전공 문서 사용: " + documentId);
                        majorDocCache.put(cacheKey, mainDoc);
                        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                        loadMajorCoursesFromSnapshot(mainDoc, category, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 문서 조회 실패", e);
                    listener.onFailure(e);
                });
    }

    private void loadMajorCoursesFromSnapshot(DocumentSnapshot documentSnapshot, String category, OnMajorCoursesLoadedListener listener) {
        String docId = documentSnapshot.getId();

        // 중복 제거를 위해 Set 사용 (과목명을 키로)
        Set<String> addedCourseNames = new HashSet<>();
        List<CourseInfo> majorCourses = new ArrayList<>();

        if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Log.d(TAG, "[" + docId + "] 문서 존재, 전체 데이터 키: " + (data != null ? data.keySet() : "null"));
                        if (data != null) {
                            // rules 객체에서 학기별 데이터 추출
                            Object rulesObj = data.get("rules");
                            Log.d(TAG, "[" + docId + "] rules 타입: " + (rulesObj != null ? rulesObj.getClass().getSimpleName() : "null"));
                            if (rulesObj instanceof Map) {
                                Map<String, Object> rules = (Map<String, Object>) rulesObj;
                                Log.d(TAG, "[" + docId + "] rules 키 개수: " + rules.size() + ", 키 목록: " + rules.keySet());

                                // rules의 첫 번째 키가 무엇인지 확인
                                if (!rules.isEmpty()) {
                                    String firstKey = rules.keySet().iterator().next();
                                    Object firstValue = rules.get(firstKey);
                                    Log.d(TAG, "[" + docId + "] 첫 번째 키: '" + firstKey + "', 값 타입: " + (firstValue != null ? firstValue.getClass().getSimpleName() : "null"));
                                    if (firstValue instanceof Map) {
                                        Map<String, Object> firstValueMap = (Map<String, Object>) firstValue;
                                        Log.d(TAG, "[" + docId + "] 첫 번째 값의 키들: " + firstValueMap.keySet());
                                    } else if (firstValue instanceof List) {
                                        Log.d(TAG, "[" + docId + "] 첫 번째 값은 List, 크기: " + ((List<?>) firstValue).size());
                                    }
                                }

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
            String errorMsg = "전공 강의 문서를 찾을 수 없습니다: " + docId;
            Log.e(TAG, errorMsg);
            listener.onFailure(new Exception(errorMsg));
        }
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

    // 교양 강의 목록 조회 (폴백: 관리자 설정 → 교양_학부_연도 → 교양_공통_연도)
    public void loadGeneralEducationCourses(String department, String track, String year, String category, OnMajorCoursesLoadedListener listener) {
        Log.d(TAG, "=== 교양 강의 조회 시작 ===");
        Log.d(TAG, "입력값 - 학부: " + department + ", 트랙: " + track + ", 년도: " + year + ", 카테고리: " + category);

        resolveGeneralDocId(department, track, year, new OnGeneralDocResolvedListener() {
            @Override
            public void onResolved(String docId, DocumentSnapshot snapshot) {
                Log.d(TAG, "교양 문서 확정: " + docId);
                // N+1 해결: 이미 받은 DocumentSnapshot 사용
                if (snapshot.exists()) {
                    loadGeneralEducationFromDocument(snapshot, category, listener);
                } else {
                    listener.onFailure(new Exception("교양 문서가 존재하지 않습니다: " + docId));
                }
            }

            @Override
            public void onNotFound() {
                String msg = "교양 문서를 찾을 수 없습니다. 학부 전용/공통 문서가 모두 없음";
                Log.e(TAG, msg);
                listener.onFailure(new Exception(msg));
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "교양 문서 선택 중 오류", e);
                listener.onFailure(e);
            }
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

    // 학부공통/전공심화 강의 목록 조회
    public void loadDepartmentCommonCourses(String department, String track, String year, OnMajorCoursesLoadedListener listener) {
        String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(department, year);
        Log.d(TAG, categoryName + " 강의 조회 시작: " + department + "_" + track + "_" + year);

        // 모든 학번은 해당 연도 그대로 사용
        final String actualYear = year;

        // graduation_requirements 컬렉션에서 해당 학부_트랙_학번 문서 조회
        String documentId = department + "_" + track + "_" + actualYear;
        String cacheKey = department + "|" + track + "|" + actualYear;
        Log.d(TAG, categoryName + " 강의 문서 ID: " + documentId);

        // 캐시 확인 (유효 시간 체크 포함)
        if (deptCommonDocCache.containsKey(cacheKey)) {
            Long cachedTime = cacheTimestamps.get(cacheKey + "_deptCommon");
            if (cachedTime != null && (System.currentTimeMillis() - cachedTime) < CACHE_VALIDITY_MS) {
                Log.d(TAG, "학부공통 문서 캐시 히트: " + cacheKey);
                DocumentSnapshot cachedDoc = deptCommonDocCache.get(cacheKey);
                loadDepartmentCommonFromSnapshot(cachedDoc, department, actualYear, listener);
                return;
            } else {
                // 캐시 만료
                Log.d(TAG, "학부공통 문서 캐시 만료, 재조회: " + cacheKey);
                deptCommonDocCache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey + "_deptCommon");
            }
        }

        db.collection("graduation_requirements").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 캐시 저장
                        deptCommonDocCache.put(cacheKey, documentSnapshot);
                        cacheTimestamps.put(cacheKey + "_deptCommon", System.currentTimeMillis());
                        loadDepartmentCommonFromSnapshot(documentSnapshot, department, actualYear, listener);
                    } else {
                        String errorMsg = categoryName + " 강의 문서를 찾을 수 없습니다: " + documentId;
                        Log.e(TAG, errorMsg);
                        listener.onFailure(new Exception(errorMsg));
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = categoryName + " 강의 문서 조회 실패: " + documentId;
                    Log.e(TAG, errorMsg, e);
                    listener.onFailure(new Exception(errorMsg, e));
                });
    }

    private void loadDepartmentCommonFromSnapshot(DocumentSnapshot documentSnapshot, String department, String year, OnMajorCoursesLoadedListener listener) {
        String categoryName = DepartmentConfig.getDepartmentCommonCategoryName(department, year);
        String docId = documentSnapshot.getId();

        Set<String> addedCourseNames = new HashSet<>();
        List<CourseInfo> commonCourses = new ArrayList<>();

        if (documentSnapshot.exists()) {
            Map<String, Object> data = documentSnapshot.getData();
            if (data != null) {
                Object rulesObj = data.get("rules");
                if (rulesObj instanceof Map) {
                    Map<String, Object> rules = (Map<String, Object>) rulesObj;
                    Log.d(TAG, "rules 내부 키 목록: " + rules.keySet().toString());

                    List<String> sortedSemesters = new ArrayList<>(rules.keySet());
                    sortedSemesters.sort((s1, s2) -> {
                        int year1 = extractYear(s1);
                        int semester1 = extractSemester(s1);
                        int year2 = extractYear(s2);
                        int semester2 = extractSemester(s2);

                        if (year1 != year2) {
                            return Integer.compare(year1, year2);
                        }
                        return Integer.compare(semester1, semester2);
                    });

                    // 정렬된 순서로 각 학기에서 학부공통/전공심화 강의들을 수집
                    for (String semesterKey : sortedSemesters) {
                        Object value = rules.get(semesterKey);
                        if (semesterKey.contains("학년") && value instanceof Map) {
                            Map<String, Object> semester = (Map<String, Object>) value;

                            // 설정 기반 카테고리 조회 로직
                            String categoryKey = DepartmentConfig.getDepartmentCommonCategoryName(department, year);

                            // Firestore에서 "학부공통" 카테고리 직접 조회 (2024-10-19: 학부공통필수 → 학부공통 병합 완료)
                            Object departmentCommon = semester.get(categoryKey);
                            if (departmentCommon instanceof List) {
                                List<?> commonList = (List<?>) departmentCommon;
                                for (Object courseObj : commonList) {
                                    if (courseObj instanceof Map) {
                                        Map<String, Object> course = (Map<String, Object>) courseObj;
                                        Object courseName = course.get("과목명");
                                        Object credits = course.get("학점");
                                        if (courseName instanceof String && credits instanceof Number) {
                                            String courseNameStr = (String) courseName;
                                            if (!addedCourseNames.contains(courseNameStr)) {
                                                addedCourseNames.add(courseNameStr);
                                                commonCourses.add(new CourseInfo(courseNameStr, ((Number) credits).intValue()));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Log.d(TAG, categoryName + " 강의 로드 성공: " + commonCourses.size() + "개 - " + commonCourses);
            listener.onSuccess(commonCourses);
        } else {
            String errorMsg = categoryName + " 강의 문서를 찾을 수 없습니다: " + docId;
            Log.e(TAG, errorMsg);
            listener.onFailure(new Exception(errorMsg));
        }
    }

    // ---------- 교양 그룹(oneOf/required) ----------

    public interface OnGeneralEducationGroupsLoadedListener {
        void onSuccess(Map<String, List<String>> oneOfGroups, List<String> individualRequired);
        void onFailure(Exception e);
    }

    public interface OnDepartmentConfigLoadedListener {
        void onSuccess(boolean usesMajorAdvanced);
        void onFailure(Exception e);
    }

    public void loadGeneralEducationGroups(String department, String year, OnGeneralEducationGroupsLoadedListener listener) {
        Log.d(TAG, "교양교육 그룹 로드 시작: " + department + ", " + year);

        resolveGeneralDocId(department, null, year, new OnGeneralDocResolvedListener() {
            @Override
            public void onResolved(String docId, DocumentSnapshot snapshot) {
                // N+1 해결: 이미 받은 DocumentSnapshot 사용
                if (snapshot.exists()) {
                    extractGeneralEducationGroupsData(snapshot, listener);
                } else {
                    listener.onFailure(new Exception("교양교육 문서가 존재하지 않습니다: " + docId));
                }
            }

            @Override
            public void onNotFound() {
                listener.onFailure(new Exception("교양교육 문서를 찾을 수 없습니다 (학부/공통 모두 없음)"));
            }

            @Override
            public void onError(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void extractGeneralEducationGroupsData(DocumentSnapshot documentSnapshot, OnGeneralEducationGroupsLoadedListener listener) {
        try {
            Map<String, List<String>> oneOfGroups = new HashMap<>();
            List<String> individualRequired = new ArrayList<>();

            // rules.requirements에서 데이터 추출
            Map<String, Object> data = documentSnapshot.getData();
            if (data != null && data.containsKey("rules")) {
                Map<String, Object> rules = (Map<String, Object>) data.get("rules");
                if (rules != null && rules.containsKey("requirements")) {
                    List<Map<String, Object>> requirements = (List<Map<String, Object>>) rules.get("requirements");

                    int groupIndex = 0;
                    for (Map<String, Object> requirement : requirements) {
                        String type = (String) requirement.get("type");
                        Log.d(TAG, "requirement type 확인: " + type + ", 전체 데이터: " + requirement);

                        if ("oneOf".equals(type)) {
                            // oneOf 그룹 처리
                            List<Map<String, Object>> options = (List<Map<String, Object>>) requirement.get("options");
                            if (options != null && !options.isEmpty()) {
                                List<String> groupCourses = new ArrayList<>();
                                for (Map<String, Object> option : options) {
                                    String courseName = (String) option.get("name");
                                    if (courseName != null) {
                                        groupCourses.add(courseName);
                                    }
                                }
                                if (!groupCourses.isEmpty()) {
                                    oneOfGroups.put("oneOf_group_" + groupIndex, groupCourses);
                                    groupIndex++;
                                }
                            }
                        } else {
                            // 개별 필수 과목 처리 (oneOf가 아니고 name 필드가 있는 경우)
                            String courseName = (String) requirement.get("name");
                            if (courseName != null) {
                                individualRequired.add(courseName);
                                Log.d(TAG, "개별 필수 과목 추가: " + courseName + " (type: " + type + ")");
                            }
                        }
                    }

                    Log.d(TAG, "교양교육 그룹 로드 성공: " + oneOfGroups.size() + "개 그룹, " + individualRequired.size() + "개 개별 필수");
                    listener.onSuccess(oneOfGroups, individualRequired);
                } else {
                    Log.e(TAG, "교양교육 문서에서 requirements 필드를 찾을 수 없습니다.");
                    listener.onFailure(new Exception("교양교육 문서에서 requirements 필드를 찾을 수 없습니다."));
                }
            } else {
                Log.e(TAG, "교양교육 문서에서 rules 필드를 찾을 수 없습니다.");
                listener.onFailure(new Exception("교양교육 문서에서 rules 필드를 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            Log.e(TAG, "교양교육 그룹 데이터 파싱 중 오류: " + e.getMessage(), e);
            listener.onFailure(e);
        }
    }

    // ---------- 학부 설정 / 총학점 / 기타 ----------

    /**
     * 학부 설정 정보를 Firebase에서 로드
     * @param department 학부명
     * @param listener 콜백
     */
    public void loadDepartmentConfig(String department, OnDepartmentConfigLoadedListener listener) {
        Log.d(TAG, "학부 설정 로드 시작: " + department);

        db.collection("학부").document(department)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data != null) {
                                // uses_major_advanced 필드 확인 (기본값: false)
                                Boolean usesMajorAdvanced = (Boolean) data.get("uses_major_advanced");
                                if (usesMajorAdvanced == null) {
                                    usesMajorAdvanced = false; // 기본값
                                }

                                Log.d(TAG, "학부 설정 로드 성공: " + department + " -> uses_major_advanced: " + usesMajorAdvanced);
                                listener.onSuccess(usesMajorAdvanced);
                            } else {
                                Log.w(TAG, "학부 설정 데이터가 비어있습니다: " + department);
                                listener.onSuccess(false); // 기본값 반환
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "학부 설정 데이터 파싱 중 오류: " + e.getMessage(), e);
                            listener.onFailure(e);
                        }
                    } else {
                        Log.w(TAG, "학부 설정 문서가 존재하지 않습니다: " + department + " (기본값 사용)");
                        listener.onSuccess(false); // 기본값 반환
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학부 설정 Firebase 조회 실패: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

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

    // ---------- 학생 진도 데이터 (저장/조회) ----------

    public static class StudentProgress {
        public String studentId;
        public String department;
        public String track;
        public String year;
        public List<String> completedCourses;
        public Map<String, Integer> categoryCredits;
        public Map<String, Object> additionalRequirements;
        public long lastUpdated;

        public StudentProgress() {}

        public StudentProgress(String studentId, String department, String track, String year) {
            this.studentId = studentId;
            this.department = department;
            this.track = track;
            this.year = year;
            this.completedCourses = new ArrayList<>();
            this.categoryCredits = new HashMap<>();
            this.additionalRequirements = new HashMap<>();
            this.lastUpdated = System.currentTimeMillis();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("studentId", studentId);
            data.put("department", department);
            data.put("track", track);
            data.put("year", year);
            data.put("completedCourses", completedCourses);
            data.put("categoryCredits", categoryCredits);
            data.put("additionalRequirements", additionalRequirements);
            data.put("lastUpdated", lastUpdated);
            return data;
        }

        public static StudentProgress fromMap(Map<String, Object> data) {
            StudentProgress progress = new StudentProgress();
            progress.studentId = (String) data.get("studentId");
            progress.department = (String) data.get("department");
            progress.track = (String) data.get("track");
            progress.year = (String) data.get("year");
            progress.completedCourses = (List<String>) data.get("completedCourses");
            progress.categoryCredits = (Map<String, Integer>) data.get("categoryCredits");
            progress.additionalRequirements = (Map<String, Object>) data.get("additionalRequirements");
            Object lastUpdatedObj = data.get("lastUpdated");
            if (lastUpdatedObj instanceof Long) {
                progress.lastUpdated = (Long) lastUpdatedObj;
            } else {
                progress.lastUpdated = System.currentTimeMillis();
            }
            return progress;
        }
    }

    public interface OnStudentProgressSavedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnStudentProgressLoadedListener {
        void onSuccess(StudentProgress progress);
        void onFailure(Exception e);
    }

    public void saveStudentProgress(StudentProgress progress, OnStudentProgressSavedListener listener) {
        Log.d(TAG, "학생 진도 데이터 저장 시작: " + progress.studentId);

        progress.lastUpdated = System.currentTimeMillis();

        db.collection("student_progress")
                .document(progress.studentId)
                .set(progress.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "학생 진도 데이터 저장 성공: " + progress.studentId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학생 진도 데이터 저장 실패: " + progress.studentId, e);
                    listener.onFailure(e);
                });
    }

    public void loadStudentProgress(String studentId, OnStudentProgressLoadedListener listener) {
        Log.d(TAG, "학생 진도 데이터 로드 시작: " + studentId);

        db.collection("student_progress")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            StudentProgress progress = StudentProgress.fromMap(documentSnapshot.getData());
                            Log.d(TAG, "학생 진도 데이터 로드 성공: " + studentId);
                            listener.onSuccess(progress);
                        } catch (Exception e) {
                            Log.e(TAG, "학생 진도 데이터 파싱 실패: " + studentId, e);
                            listener.onFailure(e);
                        }
                    } else {
                        Log.d(TAG, "학생 진도 데이터 없음: " + studentId);
                        listener.onFailure(new Exception("학생 진도 데이터를 찾을 수 없습니다"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학생 진도 데이터 로드 실패: " + studentId, e);
                    listener.onFailure(e);
                });
    }

    public void deleteStudentProgress(String studentId, OnStudentProgressSavedListener listener) {
        Log.d(TAG, "학생 진도 데이터 삭제 시작: " + studentId);

        db.collection("student_progress")
                .document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "학생 진도 데이터 삭제 성공: " + studentId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학생 진도 데이터 삭제 실패: " + studentId, e);
                    listener.onFailure(e);
                });
    }

    public String generateStudentId(String department, String track, String year) {
        return department + "_" + track + "_" + year + "_" + System.currentTimeMillis();
    }

    public interface OnStudentProgressListLoadedListener {
        void onSuccess(List<StudentProgress> progressList);
        void onFailure(Exception e);
    }

    public void loadStudentProgressByDepartment(String department, String track, String year, OnStudentProgressListLoadedListener listener) {
        Log.d(TAG, "학부별 학생 진도 데이터 목록 로드 시작: " + department + "_" + track + "_" + year);

        db.collection("student_progress")
                .whereEqualTo("department", department)
                .whereEqualTo("track", track)
                .whereEqualTo("year", year)
                .orderBy("lastUpdated", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudentProgress> progressList = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot.getDocuments()) {
                        try {
                            StudentProgress progress = StudentProgress.fromMap(document.getData());
                            progressList.add(progress);
                        } catch (Exception e) {
                            Log.w(TAG, "학생 진도 데이터 파싱 실패: " + document.getId(), e);
                        }
                    }
                    Log.d(TAG, "학부별 학생 진도 데이터 로드 성공: " + progressList.size() + "개");
                    listener.onSuccess(progressList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학부별 학생 진도 데이터 로드 실패: " + department + "_" + track + "_" + year, e);
                    listener.onFailure(e);
                });
    }

    public void loadLatestStudentProgress(String department, String track, String year, OnStudentProgressLoadedListener listener) {
        Log.d(TAG, "최신 학생 진도 데이터 로드 시작: " + department + "_" + track + "_" + year);

        loadStudentProgressByDepartment(department, track, year, new OnStudentProgressListLoadedListener() {
            @Override
            public void onSuccess(List<StudentProgress> progressList) {
                if (!progressList.isEmpty()) {
                    // 가장 최신 데이터 (첫 번째, 이미 lastUpdated DESC로 정렬됨)
                    StudentProgress latestProgress = progressList.get(0);
                    Log.d(TAG, "최신 학생 진도 데이터 로드 성공: " + latestProgress.studentId);
                    listener.onSuccess(latestProgress);
                } else {
                    Log.d(TAG, "해당 조건의 학생 진도 데이터가 없습니다: " + department + "_" + track + "_" + year);
                    listener.onFailure(new Exception("저장된 졸업 분석 데이터가 없습니다"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    // ========== 통합 졸업요건 시스템 ==========

    /**
     * 통합 졸업요건 규칙 로드 (graduation_requirements 컬렉션)
     *
     * @param cohort 학번 (예: "2020")
     * @param department 학부 (예: "IT학부")
     * @param track 트랙 (예: "인공지능")
     * @param listener 로드 완료 리스너
     */
    public void loadGraduationRules(String cohort, String department, String track,
                                     OnGraduationRulesLoadedListener listener) {
        // 새로운 3-tier 구조: 졸업요건 문서에서 학점 정보 로드
        String gradDocId = "졸업요건_" + department + "_" + track + "_" + cohort;
        Log.d(TAG, "========================================");
        Log.d(TAG, "Loading unified graduation rules: " + gradDocId);
        Log.d(TAG, "========================================");

        // 캐시 확인
        String cacheKey = "graduation_rules_" + gradDocId;
        Long cacheTime = cacheTimestamps.get(cacheKey);
        if (cacheTime != null && (System.currentTimeMillis() - cacheTime) < CACHE_VALIDITY_MS) {
            sprout.app.sakmvp1.models.GraduationRules cached =
                (sprout.app.sakmvp1.models.GraduationRules) graduationCache.get(cacheKey);
            if (cached != null) {
                Log.d(TAG, "✓ Cache hit for graduation rules: " + gradDocId);
                listener.onSuccess(cached);
                return;
            }
        }

        // 1. 졸업요건 문서 로드 (학점 정보 + 문서 참조)
        db.collection("graduation_requirements")
            .document(gradDocId)
            .get()
            .addOnSuccessListener(gradDoc -> {
                if (!gradDoc.exists()) {
                    Log.e(TAG, "졸업요건 문서를 찾을 수 없습니다: " + gradDocId);
                    listener.onFailure(new Exception("졸업요건 문서를 찾을 수 없습니다: " + gradDocId));
                    return;
                }

                Log.d(TAG, "✓ 졸업요건 문서 로드 성공: " + gradDocId);

                // 졸업요건 문서에서 학점 정보 읽기
                Map<String, Object> gradData = gradDoc.getData();
                if (gradData == null) {
                    Log.e(TAG, "졸업요건 문서 데이터가 null입니다");
                    listener.onFailure(new Exception("졸업요건 문서 데이터가 null"));
                    return;
                }

                // 문서 참조 읽기
                String majorDocRef = gradDoc.getString("majorDocRef");
                String generalDocRef = gradDoc.getString("generalDocRef");

                Log.d(TAG, "  - majorDocRef: " + majorDocRef);
                Log.d(TAG, "  - generalDocRef: " + generalDocRef);
                Log.d(TAG, "  - 총학점: " + gradDoc.getLong("총학점"));
                Log.d(TAG, "  - 전공필수: " + gradDoc.getLong("전공필수"));
                Log.d(TAG, "  - 전공선택: " + gradDoc.getLong("전공선택"));

                // 2. 전공 문서와 교양 문서 로드 및 병합
                loadAndMergeDocumentsForGraduationRules(
                    gradData, majorDocRef, generalDocRef, cohort, department, track,
                    cacheKey, listener);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "졸업요건 문서 로드 실패: " + gradDocId, e);
                listener.onFailure(e);
            });
    }

    /**
     * 졸업요건 문서에서 전공/교양 문서를 로드하고 병합하여 GraduationRules 생성
     */
    private void loadAndMergeDocumentsForGraduationRules(
            Map<String, Object> gradData,
            String majorDocRef,
            String generalDocRef,
            String cohort,
            String department,
            String track,
            String cacheKey,
            OnGraduationRulesLoadedListener listener) {

        Log.d(TAG, "전공/교양 문서 병합 시작");

        // 전공 문서 로드
        if (majorDocRef != null && !majorDocRef.isEmpty()) {
            db.collection("graduation_requirements").document(majorDocRef)
                .get()
                .addOnSuccessListener(majorDoc -> {
                    if (majorDoc.exists() && majorDoc.getData() != null) {
                        Map<String, Object> majorData = majorDoc.getData();
                        Log.d(TAG, "✓ 전공 문서 로드 성공: " + majorDocRef);

                        // 전공 rules 복사
                        if (majorData.containsKey("rules")) {
                            gradData.put("majorRules", majorData.get("rules"));
                        }

                        // 전공 replacementRules 복사
                        if (majorData.containsKey("replacementRules")) {
                            gradData.put("replacementRules", majorData.get("replacementRules"));
                        }

                        // 교양 문서 로드
                        if (generalDocRef != null && !generalDocRef.isEmpty()) {
                            db.collection("graduation_requirements").document(generalDocRef)
                                .get()
                                .addOnSuccessListener(generalDoc -> {
                                    if (generalDoc.exists() && generalDoc.getData() != null) {
                                        Map<String, Object> generalData = generalDoc.getData();
                                        Log.d(TAG, "✓ 교양 문서 로드 성공: " + generalDocRef);

                                        // 교양 requirements 복사
                                        if (generalData.containsKey("rules")) {
                                            Map<String, Object> generalRules = (Map<String, Object>) generalData.get("rules");
                                            if (generalRules != null && generalRules.containsKey("requirements")) {
                                                gradData.put("generalRequirements", generalRules.get("requirements"));
                                            }
                                        }
                                    }

                                    // 병합된 데이터로 GraduationRules 생성
                                    createGraduationRulesFromMergedData(
                                        gradData, cohort, department, track, cacheKey, listener);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "교양 문서 로드 실패, 전공만 사용: " + e.getMessage());
                                    createGraduationRulesFromMergedData(
                                        gradData, cohort, department, track, cacheKey, listener);
                                });
                        } else {
                            Log.d(TAG, "교양 문서 참조 없음, 전공만 사용");
                            createGraduationRulesFromMergedData(
                                gradData, cohort, department, track, cacheKey, listener);
                        }
                    } else {
                        Log.e(TAG, "전공 문서가 존재하지 않음: " + majorDocRef);
                        listener.onFailure(new Exception("전공 문서를 찾을 수 없습니다: " + majorDocRef));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 문서 로드 실패: " + majorDocRef, e);
                    listener.onFailure(e);
                });
        } else {
            Log.e(TAG, "전공 문서 참조가 없습니다");
            listener.onFailure(new Exception("전공 문서 참조가 없습니다"));
        }
    }

    /**
     * 병합된 데이터로 GraduationRules 객체 생성
     */
    private void createGraduationRulesFromMergedData(
            Map<String, Object> gradData,
            String cohort,
            String department,
            String track,
            String cacheKey,
            OnGraduationRulesLoadedListener listener) {

        Log.d(TAG, "병합된 데이터로 GraduationRules 생성 시작");

        // 1. GraduationRules 객체 생성
        sprout.app.sakmvp1.models.GraduationRules rules =
            new sprout.app.sakmvp1.models.GraduationRules(
                Long.parseLong(cohort), department, track);

        // 2. 총 학점 설정 (졸업요건 문서의 총학점 또는 totalCredits 필드)
        int totalCreditsValue = getIntValue(gradData, "총학점", 0);
        if (totalCreditsValue == 0) {
            totalCreditsValue = getIntValue(gradData, "totalCredits", 130);
        }
        rules.setTotalCredits(totalCreditsValue);
        Log.d(TAG, "  - 총학점: " + totalCreditsValue);

        // 3. CreditRequirements 생성 (졸업요건 문서에서 모든 학점 읽기)
        sprout.app.sakmvp1.models.CreditRequirements creditReqs =
            new sprout.app.sakmvp1.models.CreditRequirements();

        creditReqs.setTotal(totalCreditsValue);
        creditReqs.setRequiredCredits("전공필수", getIntValue(gradData, "전공필수", 0));
        creditReqs.setRequiredCredits("전공선택", getIntValue(gradData, "전공선택", 0));
        creditReqs.setRequiredCredits("교양필수", getIntValue(gradData, "교양필수", 0));
        creditReqs.setRequiredCredits("교양선택", getIntValue(gradData, "교양선택", 0));
        creditReqs.setRequiredCredits("소양", getIntValue(gradData, "소양", 0));
        creditReqs.setRequiredCredits("학부공통", getIntValue(gradData, "학부공통", 0));
        creditReqs.setRequiredCredits("전공심화", getIntValue(gradData, "전공심화", 0));

        // 잔여학점 또는 자율선택 (둘 중 하나만 사용, 자율선택 우선)
        int freeElective = getIntValue(gradData, "자율선택", 0);
        int remainingCredits = getIntValue(gradData, "잔여학점", 0);

        if (freeElective > 0) {
            // 자율선택이 있으면 자율선택만 사용
            creditReqs.setRequiredCredits("자율선택", freeElective);
            creditReqs.setRequiredCredits("잔여학점", 0);
            Log.d(TAG, "  - 자율선택: " + freeElective);
        } else if (remainingCredits > 0) {
            // 자율선택이 없고 잔여학점만 있으면 잔여학점 사용
            creditReqs.setRequiredCredits("잔여학점", remainingCredits);
            creditReqs.setRequiredCredits("자율선택", 0);
            Log.d(TAG, "  - 잔여학점: " + remainingCredits);
        }

        rules.setCreditRequirements(creditReqs);

        Log.d(TAG, "✓ 학점 요구사항 설정 완료:");
        Log.d(TAG, "  - 전공필수: " + creditReqs.getRequiredCredits("전공필수"));
        Log.d(TAG, "  - 전공선택: " + creditReqs.getRequiredCredits("전공선택"));
        Log.d(TAG, "  - 교양필수: " + creditReqs.getRequiredCredits("교양필수"));
        Log.d(TAG, "  - 교양선택: " + creditReqs.getRequiredCredits("교양선택"));
        Log.d(TAG, "  - 소양: " + creditReqs.getRequiredCredits("소양"));
        Log.d(TAG, "  - 학부공통: " + creditReqs.getRequiredCredits("학부공통"));
        Log.d(TAG, "  - 전공심화: " + creditReqs.getRequiredCredits("전공심화"));

        // 4. Categories 생성 (전공 rules + 교양 requirements)
        List<sprout.app.sakmvp1.models.RequirementCategory> categories =
            createCategoriesFromMergedData(gradData, creditReqs);
        rules.setCategories(categories);
        Log.d(TAG, "✓ Categories 생성 완료: " + categories.size() + "개");

        // 5. Overflow destination 설정
        String overflowDest = remainingCredits > 0 ? "잔여학점" : "일반선택";
        rules.setOverflowDestination(overflowDest);

        // 6. ReplacementRules 설정
        Object replacementRulesObj = gradData.get("replacementRules");
        if (replacementRulesObj instanceof List) {
            // TODO: List를 ReplacementRule 객체로 변환
            rules.setReplacementRules(new ArrayList<>());
        } else {
            rules.setReplacementRules(new ArrayList<>());
        }

        // 7. 메타데이터
        rules.setVersion("V2");
        rules.setUpdatedAt(com.google.firebase.Timestamp.now());

        // 캐시 저장
        graduationCache.put(cacheKey, rules);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());

        Log.d(TAG, "========================================");
        Log.d(TAG, "GraduationRules 생성 완료");
        Log.d(TAG, "========================================");

        listener.onSuccess(rules);
    }

    /**
     * 병합된 데이터에서 RequirementCategory 목록 생성
     */
    private List<sprout.app.sakmvp1.models.RequirementCategory> createCategoriesFromMergedData(
            Map<String, Object> gradData,
            sprout.app.sakmvp1.models.CreditRequirements creditReqs) {

        List<sprout.app.sakmvp1.models.RequirementCategory> categories = new ArrayList<>();

        // 전공 rules 처리: rules -> 학기 -> 카테고리 -> [과목들]
        Object majorRulesObj = gradData.get("majorRules");
        if (majorRulesObj instanceof Map) {
            Map<String, Object> majorRules = (Map<String, Object>) majorRulesObj;

            // 전공 카테고리들 (학기별로 통합)
            Map<String, List<sprout.app.sakmvp1.models.CourseRequirement>> majorCoursesByCategory = new HashMap<>();
            String[] majorCategories = {"전공필수", "전공선택", "학부공통", "전공심화"};

            // 각 학기를 순회
            for (Map.Entry<String, Object> semesterEntry : majorRules.entrySet()) {
                String semester = semesterEntry.getKey();
                Object semesterDataObj = semesterEntry.getValue();

                if (semesterDataObj instanceof Map) {
                    Map<String, Object> semesterData = (Map<String, Object>) semesterDataObj;

                    // 각 카테고리 처리
                    for (String categoryName : majorCategories) {
                        Object categoryObj = semesterData.get(categoryName);
                        if (categoryObj instanceof List) {
                            List<Map<String, Object>> courseList = (List<Map<String, Object>>) categoryObj;

                            if (!majorCoursesByCategory.containsKey(categoryName)) {
                                majorCoursesByCategory.put(categoryName, new ArrayList<>());
                            }

                            for (Map<String, Object> courseData : courseList) {
                                String courseName = (String) courseData.get("과목명");
                                if (courseName == null) courseName = (String) courseData.get("name");

                                int credits = getIntValue(courseData, "학점", 3);
                                if (credits == 0) credits = getIntValue(courseData, "credits", 3);

                                // 중복 체크: 같은 과목명이 이미 있으면 추가하지 않음
                                List<sprout.app.sakmvp1.models.CourseRequirement> existingCourses =
                                    majorCoursesByCategory.get(categoryName);
                                boolean isDuplicate = false;
                                for (sprout.app.sakmvp1.models.CourseRequirement existing : existingCourses) {
                                    if (existing.getName().equals(courseName)) {
                                        isDuplicate = true;
                                        Log.d(TAG, "    중복 과목 제외: " + categoryName + " - " + courseName);
                                        break;
                                    }
                                }

                                if (!isDuplicate) {
                                    sprout.app.sakmvp1.models.CourseRequirement courseReq =
                                        new sprout.app.sakmvp1.models.CourseRequirement(courseName, credits);
                                    existingCourses.add(courseReq);
                                }
                            }
                        }
                    }
                }
            }

            // 각 전공 카테고리를 RequirementCategory로 변환
            for (String categoryName : majorCategories) {
                List<sprout.app.sakmvp1.models.CourseRequirement> courses =
                    majorCoursesByCategory.get(categoryName);
                if (courses != null && !courses.isEmpty()) {
                    int requiredCredits = creditReqs.getRequiredCredits(categoryName);
                    sprout.app.sakmvp1.models.RequirementCategory category =
                        new sprout.app.sakmvp1.models.RequirementCategory(
                            categoryName, categoryName, "list");
                    category.setRequired(requiredCredits);
                    category.setCourses(courses);
                    categories.add(category);
                }
            }
        }

        // 교양 requirements 처리
        Object generalReqObj = gradData.get("generalRequirements");
        if (generalReqObj instanceof List) {
            List<Map<String, Object>> requirements = (List<Map<String, Object>>) generalReqObj;

            // 교양필수, 교양선택, 소양 카테고리 생성
            List<sprout.app.sakmvp1.models.CourseRequirement> generalRequiredCourses = new ArrayList<>();

            for (Map<String, Object> req : requirements) {
                String type = (String) req.get("type");

                if ("oneOf".equals(type)) {
                    // oneOf: 여러 과목 중 하나 선택
                    Object optionsObj = req.get("options");
                    if (optionsObj instanceof List) {
                        List<Map<String, Object>> options = (List<Map<String, Object>>) optionsObj;
                        for (Map<String, Object> option : options) {
                            String courseName = (String) option.get("name");
                            int credits = getIntValue(req, "credit", 3);
                            if (credits == 0) credits = getIntValue(req, "credits", 3);

                            sprout.app.sakmvp1.models.CourseRequirement courseReq =
                                new sprout.app.sakmvp1.models.CourseRequirement(courseName, credits);
                            generalRequiredCourses.add(courseReq);
                        }
                    }
                } else {
                    // 단일 필수 과목
                    String courseName = (String) req.get("name");
                    if (courseName != null) {
                        int credits = getIntValue(req, "credit", 3);
                        if (credits == 0) credits = getIntValue(req, "credits", 3);

                        sprout.app.sakmvp1.models.CourseRequirement courseReq =
                            new sprout.app.sakmvp1.models.CourseRequirement(courseName, credits);
                        generalRequiredCourses.add(courseReq);
                    }
                }
            }

            if (!generalRequiredCourses.isEmpty()) {
                int requiredCredits = creditReqs.getRequiredCredits("교양필수");
                sprout.app.sakmvp1.models.RequirementCategory category =
                    new sprout.app.sakmvp1.models.RequirementCategory(
                        "교양필수", "교양필수", "list");
                category.setRequired(requiredCredits);
                category.setCourses(generalRequiredCourses);
                categories.add(category);
            }
        }

        // 교양선택, 소양, 잔여학점, 자율선택 카테고리 추가 (과목 목록 없음)
        String[] otherCategories = {"교양선택", "소양", "잔여학점", "자율선택"};
        for (String categoryName : otherCategories) {
            int requiredCredits = creditReqs.getRequiredCredits(categoryName);
            if (requiredCredits > 0) {
                sprout.app.sakmvp1.models.RequirementCategory category =
                    new sprout.app.sakmvp1.models.RequirementCategory(
                        categoryName, categoryName, "elective");
                category.setRequired(requiredCredits);
                category.setCourses(new ArrayList<>());
                categories.add(category);
                Log.d(TAG, "  ✓ " + categoryName + " 카테고리 추가: " + requiredCredits + "학점");
            }
        }

        return categories;
    }

    /**
     * V1 Firestore 문서를 V2 GraduationRules 객체로 비동기 변환 (교양 문서 포함)
     *
     * V1 구조:
     * - rules (Map): 학기별 전공 강의 목록
     * - generalEducationDocId (String): 교양 문서 참조
     * - 전공필수, 전공선택, 교양필수, 교양선택, 소양, 학부공통, 전공심화 (Number): 학점
     *
     * V2 구조:
     * - categories (List<RequirementCategory>): 통합 카테고리 (전공 + 교양)
     * - creditRequirements (CreditRequirements): 학점 요건
     * - replacementRules (List<ReplacementRule>): 대체 과목 규칙
     *
     * @param documentSnapshot V1 전공 문서 스냅샷
     * @param cohort 학번
     * @param department 학부
     * @param track 트랙
     * @param listener 변환 완료 리스너
     */
    private void convertV1ToV2Async(
            com.google.firebase.firestore.DocumentSnapshot documentSnapshot,
            String cohort, String department, String track,
            OnV1ToV2ConversionListener listener) {

        Log.d(TAG, "========================================");
        Log.d(TAG, "V1 → V2 비동기 변환 시작");
        Log.d(TAG, "문서 ID: " + documentSnapshot.getId());
        Log.d(TAG, "========================================");

        Map<String, Object> majorData = documentSnapshot.getData();
        if (majorData == null) {
            Log.e(TAG, "문서 데이터가 null입니다");
            listener.onFailure(new Exception("문서 데이터가 null"));
            return;
        }

        // 교양 문서 참조 확인
        String generalDocId = documentSnapshot.getString("generalEducationDocId");

        if (generalDocId != null && !generalDocId.isEmpty()) {
            Log.d(TAG, "교양 문서 참조 발견: " + generalDocId + ", 로드 중...");

            // 교양 문서 로드 후 병합
            db.collection("graduation_requirements").document(generalDocId)
                .get()
                .addOnSuccessListener(generalDoc -> {
                    if (generalDoc.exists() && generalDoc.getData() != null) {
                        Map<String, Object> generalData = generalDoc.getData();
                        Log.d(TAG, "✓ 교양 문서 로드 성공: " + generalDocId);

                        // 전공 + 교양 데이터 병합 후 변환
                        Map<String, Object> mergedData = mergeV1Data(majorData, generalData);
                        sprout.app.sakmvp1.models.GraduationRules rules =
                            convertV1DataToV2Rules(mergedData, cohort, department, track, documentSnapshot.getId());

                        listener.onSuccess(rules);
                    } else {
                        Log.w(TAG, "교양 문서가 없음, 전공 데이터만 변환: " + generalDocId);
                        // 전공 데이터만으로 변환
                        sprout.app.sakmvp1.models.GraduationRules rules =
                            convertV1DataToV2Rules(majorData, cohort, department, track, documentSnapshot.getId());
                        listener.onSuccess(rules);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "교양 문서 로드 실패, 전공 데이터만 변환", e);
                    // 전공 데이터만으로 변환
                    sprout.app.sakmvp1.models.GraduationRules rules =
                        convertV1DataToV2Rules(majorData, cohort, department, track, documentSnapshot.getId());
                    listener.onSuccess(rules);
                });
        } else {
            Log.d(TAG, "교양 문서 참조 없음, 전공 데이터만 변환");
            // 전공 데이터만으로 변환
            sprout.app.sakmvp1.models.GraduationRules rules =
                convertV1DataToV2Rules(majorData, cohort, department, track, documentSnapshot.getId());
            listener.onSuccess(rules);
        }
    }

    /**
     * V1 전공 데이터와 교양 데이터 병합
     */
    private Map<String, Object> mergeV1Data(Map<String, Object> majorData, Map<String, Object> generalData) {
        Map<String, Object> merged = new HashMap<>(majorData);

        // 교양 문서의 rules 병합
        Object generalRulesObj = generalData.get("rules");
        if (generalRulesObj instanceof Map) {
            Map<String, Object> generalRules = (Map<String, Object>) generalRulesObj;

            // 전공 rules 가져오기
            Object majorRulesObj = merged.get("rules");
            Map<String, Object> majorRules;
            if (majorRulesObj instanceof Map) {
                majorRules = new HashMap<>((Map<String, Object>) majorRulesObj);
            } else {
                majorRules = new HashMap<>();
            }

            // 교양 requirements를 전공 rules에 추가
            if (generalRules.containsKey("requirements")) {
                majorRules.put("generalRequirements", generalRules.get("requirements"));
                Log.d(TAG, "  교양 requirements 병합 완료");
            }

            merged.put("rules", majorRules);
        }

        return merged;
    }

    /**
     * 병합된 V1 데이터를 V2 GraduationRules로 변환
     */
    private sprout.app.sakmvp1.models.GraduationRules convertV1DataToV2Rules(
            Map<String, Object> data, String cohort, String department, String track, String sourceDocId) {

        // GraduationRules 객체 생성
        sprout.app.sakmvp1.models.GraduationRules rules =
            new sprout.app.sakmvp1.models.GraduationRules(
                Long.parseLong(cohort), department, track);

        // 1. CreditRequirements 변환
        sprout.app.sakmvp1.models.CreditRequirements creditReqs = convertV1CreditRequirements(data);
        rules.setCreditRequirements(creditReqs);

        // totalCredits 필드 설정 (V2 필드 우선, 없으면 V1의 "총이수" 필드 사용)
        int totalCreditsValue;
        if (data.containsKey("totalCredits")) {
            totalCreditsValue = getIntValue(data, "totalCredits", 130);
            Log.d(TAG, "  totalCredits 필드 발견 (V2): " + totalCreditsValue);
        } else {
            totalCreditsValue = getIntValue(data, "총이수", 130);
            Log.d(TAG, "  총이수 필드 사용 (V1): " + totalCreditsValue);
        }
        rules.setTotalCredits(totalCreditsValue);
        Log.d(TAG, "✓ CreditRequirements 변환 완료 (totalCredits: " + totalCreditsValue + ")");

        // 2. Categories 변환 (전공 + 교양)
        List<sprout.app.sakmvp1.models.RequirementCategory> categories = convertV1CategoriesWithGeneral(data, creditReqs);
        rules.setCategories(categories);
        Log.d(TAG, "✓ Categories 변환 완료: " + categories.size() + "개");

        // 3. 넘치는 학점 처리 설정 - 잔여학점 또는 일반선택 중 사용하는 것으로 설정
        String overflowDest = creditReqs.getRequiredCredits("잔여학점") > 0 ? "잔여학점" : "일반선택";
        rules.setOverflowDestination(overflowDest);
        Log.d(TAG, "✓ Overflow destination 설정: " + overflowDest);

        // 4. ReplacementRules는 빈 리스트
        rules.setReplacementRules(new ArrayList<>());
        Log.d(TAG, "✓ ReplacementRules 초기화 (빈 리스트)");

        // 5. 메타데이터
        rules.setVersion("V1_CONVERTED");
        rules.setUpdatedAt(com.google.firebase.Timestamp.now());
        rules.setSourceDocumentName(sourceDocId);

        Log.d(TAG, "========================================");
        Log.d(TAG, "V1 → V2 변환 완료");
        Log.d(TAG, "========================================");

        return rules;
    }

    /**
     * V1 학점 데이터를 V2 CreditRequirements로 변환
     */
    private sprout.app.sakmvp1.models.CreditRequirements convertV1CreditRequirements(Map<String, Object> data) {
        sprout.app.sakmvp1.models.CreditRequirements creditReqs =
            new sprout.app.sakmvp1.models.CreditRequirements();

        // V1 필드에서 학점 추출
        int totalCredits = getIntValue(data, "총이수", 130);
        int majorRequired = getIntValue(data, "전공필수", 0);
        int majorElective = getIntValue(data, "전공선택", 0);
        int generalRequired = getIntValue(data, "교양필수", 0);
        int generalElective = getIntValue(data, "교양선택", 0);
        int liberalArts = getIntValue(data, "소양", 0);
        int departmentCommon = getIntValue(data, "학부공통", 0);
        int majorAdvanced = getIntValue(data, "전공심화", 0);

        // 자율선택/일반선택과 잔여학점 필드 읽기
        int freeElective = getIntValue(data, "자율선택", 0);
        int remainingCredits = getIntValue(data, "잔여학점", 0);

        // 둘 다 0이면 계산해서 잔여학점에 넣기
        if (freeElective == 0 && remainingCredits == 0) {
            int specifiedCredits = majorRequired + majorElective + generalRequired + generalElective +
                    liberalArts + departmentCommon + majorAdvanced;
            remainingCredits = Math.max(0, totalCredits - specifiedCredits);
            Log.d(TAG, "  자율선택/잔여학점 계산됨 → 잔여학점: " + remainingCredits);
        } else if (remainingCredits > 0) {
            Log.d(TAG, "  잔여학점 필드 사용: " + remainingCredits);
        } else if (freeElective > 0) {
            Log.d(TAG, "  자율선택 필드 사용: " + freeElective);
        }

        creditReqs.setTotal(totalCredits);
        creditReqs.setRequiredCredits("전공필수", majorRequired);
        creditReqs.setRequiredCredits("전공선택", majorElective);
        creditReqs.setRequiredCredits("교양필수", generalRequired);
        creditReqs.setRequiredCredits("교양선택", generalElective);
        creditReqs.setRequiredCredits("소양", liberalArts);
        creditReqs.setRequiredCredits("일반선택", freeElective);
        creditReqs.setRequiredCredits("잔여학점", remainingCredits);
        creditReqs.setRequiredCredits("학부공통", departmentCommon);
        creditReqs.setRequiredCredits("전공심화", majorAdvanced);

        Log.d(TAG, "  총이수: " + totalCredits + "학점");
        Log.d(TAG, "  전공필수: " + majorRequired + ", 전공선택: " + majorElective);
        Log.d(TAG, "  교양필수: " + generalRequired + ", 교양선택: " + generalElective);
        Log.d(TAG, "  소양: " + liberalArts + ", 일반선택: " + freeElective + ", 잔여학점: " + remainingCredits);
        Log.d(TAG, "  학부공통: " + departmentCommon + ", 전공심화: " + majorAdvanced);

        return creditReqs;
    }

    /**
     * V1 rules 데이터를 V2 Categories로 변환 (전공 + 교양)
     */
    private List<sprout.app.sakmvp1.models.RequirementCategory> convertV1CategoriesWithGeneral(
            Map<String, Object> data, sprout.app.sakmvp1.models.CreditRequirements creditReqs) {

        List<sprout.app.sakmvp1.models.RequirementCategory> categories = new ArrayList<>();

        // V1의 rules 필드에서 강의 목록 추출
        Object rulesObj = data.get("rules");
        Map<String, List<sprout.app.sakmvp1.models.CourseRequirement>> coursesByCategory = new HashMap<>();

        // 교양 requirements 처리를 위한 변수들 (메서드 스코프로 선언)
        List<sprout.app.sakmvp1.models.RequirementCategory> generalSubgroups = new ArrayList<>();
        List<sprout.app.sakmvp1.models.CourseRequirement> individualGeneralCourses = new ArrayList<>();
        int oneOfGroupCounter = 0;

        if (rulesObj instanceof Map) {
            Map<String, Object> rules = (Map<String, Object>) rulesObj;

            // 학기별 강의를 카테고리별로 그룹화 (전공)
            for (Map.Entry<String, Object> semesterEntry : rules.entrySet()) {
                String semester = semesterEntry.getKey();

                // generalRequirements는 별도 처리
                if ("generalRequirements".equals(semester)) {
                    continue;
                }

                // 학기 형식("X학년 X학기")만 처리, 다른 키는 무시
                if (!semester.matches(".*학년.*학기")) {
                    Log.d(TAG, "  Skipping non-semester key: " + semester);
                    continue;
                }

                if (semesterEntry.getValue() instanceof Map) {
                    Map<String, Object> semesterData = (Map<String, Object>) semesterEntry.getValue();

                    for (Map.Entry<String, Object> categoryEntry : semesterData.entrySet()) {
                        String category = categoryEntry.getKey();

                        if (categoryEntry.getValue() instanceof List) {
                            List<Object> courses = (List<Object>) categoryEntry.getValue();

                            if (!coursesByCategory.containsKey(category)) {
                                coursesByCategory.put(category, new ArrayList<>());
                            }

                            for (Object courseObj : courses) {
                                if (courseObj instanceof Map) {
                                    Map<String, Object> courseMap = (Map<String, Object>) courseObj;
                                    // V1 데이터는 한글 필드명 사용: "과목명", "학점"
                                    String courseName = (String) courseMap.get("과목명");
                                    int credits = getIntValue(courseMap, "학점", 3);
                                    // V1 데이터: 전공필수, 교양필수, 학부공통, 전공선택, 전공심화 등 모든 과목 추적 필요
                                    boolean mandatory = courseMap.containsKey("mandatory") ?
                                        (boolean) courseMap.get("mandatory") :
                                        (category.contains("필수") || category.equals("학부공통") ||
                                         category.equals("전공선택") || category.equals("전공심화"));

                                    sprout.app.sakmvp1.models.CourseRequirement courseReq =
                                        new sprout.app.sakmvp1.models.CourseRequirement();
                                    courseReq.setName(courseName);
                                    courseReq.setCredits(credits);
                                    courseReq.setMandatory(mandatory);

                                    coursesByCategory.get(category).add(courseReq);
                                }
                            }
                        }
                    }
                }
            }

            // 교양 requirements 처리
            // generalRequirements는 List<Requirement>로, 각 requirement는 단일 과목 또는 oneOf 그룹
            // 교양 카테고리는 "교양필수", "교양선택", "소양" 등으로 고정
            // V2 통합 시스템: oneOf 그룹은 별도의 서브그룹으로 처리

            if (rules.containsKey("generalRequirements")) {
                Object generalReqObj = rules.get("generalRequirements");
                if (generalReqObj instanceof List) {
                    List<Object> generalReqs = (List<Object>) generalReqObj;

                    for (Object reqObj : generalReqs) {
                        if (reqObj instanceof Map) {
                            Map<String, Object> reqMap = (Map<String, Object>) reqObj;

                            // Case 1: 단일 과목 { name: "...", credit: N }
                            if (reqMap.containsKey("name") && !reqMap.containsKey("options")) {
                                String courseName = (String) reqMap.get("name");
                                int credits = getIntValue(reqMap, "credit", 2);

                                sprout.app.sakmvp1.models.CourseRequirement courseReq =
                                    new sprout.app.sakmvp1.models.CourseRequirement();
                                courseReq.setName(courseName);
                                courseReq.setCredits(credits);
                                courseReq.setMandatory(true);  // 교양필수 개별 과목은 모두 필수

                                individualGeneralCourses.add(courseReq);
                            }
                            // Case 2: oneOf 그룹 { type: "oneOf", options: [...] }
                            else if (reqMap.containsKey("options")) {
                                oneOfGroupCounter++;
                                String groupId = "교양필수_oneOf_" + oneOfGroupCounter;
                                List<sprout.app.sakmvp1.models.CourseRequirement> oneOfCourses = new ArrayList<>();
                                int totalCredits = 0;

                                Log.d(TAG, "  oneOf 그룹 #" + oneOfGroupCounter + " 파싱 시작");
                                Log.d(TAG, "    reqMap keys: " + reqMap.keySet());

                                List<Object> options = (List<Object>) reqMap.get("options");
                                Log.d(TAG, "    options 개수: " + (options != null ? options.size() : "null"));

                                if (options != null) {
                                    for (int i = 0; i < options.size(); i++) {
                                        Object optionObj = options.get(i);
                                        Log.d(TAG, "      option #" + (i+1) + " type: " + optionObj.getClass().getSimpleName());

                                        if (optionObj instanceof Map) {
                                            Map<String, Object> optionMap = (Map<String, Object>) optionObj;
                                            Log.d(TAG, "        optionMap keys: " + optionMap.keySet());

                                            // Firestore 구조: options: [{name: "과목명"}] 형태
                                            // 즉, option 자체가 과목 정보
                                            if (optionMap.containsKey("name")) {
                                                String courseName = (String) optionMap.get("name");
                                                // credit은 option 레벨이 아닌 reqMap 레벨에 있음
                                                int credits = getIntValue(reqMap, "credit", 2);

                                                sprout.app.sakmvp1.models.CourseRequirement courseReq =
                                                    new sprout.app.sakmvp1.models.CourseRequirement();
                                                courseReq.setName(courseName);
                                                courseReq.setCredits(credits);
                                                courseReq.setMandatory(false);  // oneOf 내 과목은 선택사항

                                                oneOfCourses.add(courseReq);
                                                if (totalCredits == 0) {
                                                    totalCredits = credits;
                                                }

                                                Log.d(TAG, "          과목 추가: " + courseName + " (" + credits + "학점)");
                                            }
                                        }
                                    }
                                }

                                // oneOf 서브그룹 생성
                                if (!oneOfCourses.isEmpty()) {
                                    sprout.app.sakmvp1.models.RequirementCategory oneOfGroup =
                                        new sprout.app.sakmvp1.models.RequirementCategory(groupId, groupId, "oneOf");
                                    oneOfGroup.setCourses(oneOfCourses);
                                    oneOfGroup.setRequired(totalCredits);
                                    oneOfGroup.setRequiredType("credits");
                                    generalSubgroups.add(oneOfGroup);

                                    Log.d(TAG, "  교양필수 oneOf 그룹 생성: " + groupId + " (" + oneOfCourses.size() + "개 과목 중 1개 선택, " + totalCredits + "학점)");
                                }
                            }
                        }
                    }

                    Log.d(TAG, "  교양 카테고리 추출 완료: 개별 " + individualGeneralCourses.size() + "개 과목, oneOf 그룹 " + oneOfGroupCounter + "개");
                }
            }
        }

        Log.d(TAG, "  카테고리별 과목 그룹화 완료: " + coursesByCategory.size() + "개 카테고리");

        // 카테고리별로 RequirementCategory 생성
        for (Map.Entry<String, List<sprout.app.sakmvp1.models.CourseRequirement>> entry : coursesByCategory.entrySet()) {
            String categoryName = entry.getKey();
            List<sprout.app.sakmvp1.models.CourseRequirement> courses = entry.getValue();

            sprout.app.sakmvp1.models.RequirementCategory category =
                new sprout.app.sakmvp1.models.RequirementCategory(
                    categoryName, categoryName, "list");

            category.setCourses(courses);
            category.setRequired(creditReqs.getRequiredCredits(categoryName));
            category.setRequiredType("credits");

            categories.add(category);

            Log.d(TAG, "    - " + categoryName + ": " + courses.size() + "개 과목, " +
                  category.getRequired() + "학점 필요");
        }

        // 교양필수 카테고리 특별 처리: oneOf 그룹이 있으면 group 타입으로 재구성
        if (!generalSubgroups.isEmpty() || !individualGeneralCourses.isEmpty()) {
            // 기존 list 타입 교양필수 카테고리 제거
            categories.removeIf(cat -> "교양필수".equals(cat.getName()));

            // group 타입 교양필수 카테고리 생성
            sprout.app.sakmvp1.models.RequirementCategory generalCategory =
                new sprout.app.sakmvp1.models.RequirementCategory(
                    "교양필수", "교양필수", "group");

            generalCategory.setRequired(creditReqs.getRequiredCredits("교양필수"));
            generalCategory.setRequiredType("credits");

            // 개별 과목이 있으면 list 서브그룹으로 추가
            if (!individualGeneralCourses.isEmpty()) {
                sprout.app.sakmvp1.models.RequirementCategory individualGroup =
                    new sprout.app.sakmvp1.models.RequirementCategory(
                        "교양필수_개별", "개별 필수 과목", "list");
                individualGroup.setCourses(individualGeneralCourses);
                individualGroup.setRequired(0);  // 개별 서브그룹은 학점 요구 없음 (상위에서 관리)
                individualGroup.setRequiredType("credits");
                generalSubgroups.add(0, individualGroup);  // 맨 앞에 추가
            }

            generalCategory.setSubgroups(generalSubgroups);
            categories.add(generalCategory);

            Log.d(TAG, "  ✓ 교양필수 group 카테고리 생성: " + generalSubgroups.size() + "개 서브그룹, " +
                  creditReqs.getRequiredCredits("교양필수") + "학점 필요");
        }

        // CreditRequirements에는 있지만 과목 목록이 없는 카테고리들 추가
        // (교양선택, 소양, 잔여학점/일반선택 등)
        Log.d(TAG, "  빈 카테고리 생성 로직 시작...");
        String[] allCategories = {"전공필수", "전공선택", "교양필수", "교양선택", "소양", "학부공통", "전공심화", "일반선택", "잔여학점"};
        for (String categoryName : allCategories) {
            int required = creditReqs.getRequiredCredits(categoryName);

            // categories 리스트에서 이미 존재하는지 확인 (group 타입 카테고리도 감지)
            boolean alreadyExists = false;
            for (sprout.app.sakmvp1.models.RequirementCategory cat : categories) {
                if (categoryName.equals(cat.getName())) {
                    alreadyExists = true;
                    break;
                }
            }

            Log.d(TAG, "    체크 중: " + categoryName + " (required=" + required +
                  ", 이미 존재=" + alreadyExists + ")");

            if (required > 0 && !alreadyExists) {
                // 빈 카테고리 생성
                sprout.app.sakmvp1.models.RequirementCategory category =
                    new sprout.app.sakmvp1.models.RequirementCategory(
                        categoryName, categoryName, "list");

                category.setCourses(new ArrayList<>()); // 빈 리스트
                category.setRequired(required);
                category.setRequiredType("credits");

                categories.add(category);

                Log.d(TAG, "    ✓ 빈 카테고리 생성: " + categoryName + " (0개 과목, " +
                      required + "학점 필요)");
            }
        }
        Log.d(TAG, "  빈 카테고리 생성 완료. 총 카테고리 수: " + categories.size());

        return categories;
    }

    /**
     * V1 rules 데이터를 V2 Categories로 변환 (전공만, 구버전 - 사용 안 함)
     * @deprecated Use convertV1CategoriesWithGeneral instead
     */
    @Deprecated
    private List<sprout.app.sakmvp1.models.RequirementCategory> convertV1Categories(
            Map<String, Object> data, sprout.app.sakmvp1.models.CreditRequirements creditReqs) {

        List<sprout.app.sakmvp1.models.RequirementCategory> categories = new ArrayList<>();

        // V1의 rules 필드에서 강의 목록 추출
        Object rulesObj = data.get("rules");
        Map<String, List<sprout.app.sakmvp1.models.CourseRequirement>> coursesByCategory = new HashMap<>();

        if (rulesObj instanceof Map) {
            Map<String, Object> rules = (Map<String, Object>) rulesObj;

            // 학기별 강의를 카테고리별로 그룹화
            for (Map.Entry<String, Object> semesterEntry : rules.entrySet()) {
                String semester = semesterEntry.getKey();

                if (semesterEntry.getValue() instanceof Map) {
                    Map<String, Object> semesterData = (Map<String, Object>) semesterEntry.getValue();

                    for (Map.Entry<String, Object> categoryEntry : semesterData.entrySet()) {
                        String category = categoryEntry.getKey();

                        if (categoryEntry.getValue() instanceof List) {
                            List<Object> courses = (List<Object>) categoryEntry.getValue();

                            if (!coursesByCategory.containsKey(category)) {
                                coursesByCategory.put(category, new ArrayList<>());
                            }

                            for (Object courseObj : courses) {
                                if (courseObj instanceof Map) {
                                    Map<String, Object> courseMap = (Map<String, Object>) courseObj;
                                    // V1 데이터는 한글 필드명 사용: "과목명", "학점"
                                    String courseName = (String) courseMap.get("과목명");
                                    int credits = getIntValue(courseMap, "학점", 3);
                                    // V1 데이터: 전공필수, 교양필수, 학부공통, 전공선택, 전공심화 등 모든 과목 추적 필요
                                    boolean mandatory = courseMap.containsKey("mandatory") ?
                                        (boolean) courseMap.get("mandatory") :
                                        (category.contains("필수") || category.equals("학부공통") ||
                                         category.equals("전공선택") || category.equals("전공심화"));

                                    sprout.app.sakmvp1.models.CourseRequirement courseReq =
                                        new sprout.app.sakmvp1.models.CourseRequirement();
                                    courseReq.setName(courseName);
                                    courseReq.setCredits(credits);
                                    courseReq.setMandatory(mandatory);

                                    coursesByCategory.get(category).add(courseReq);
                                }
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "  카테고리별 과목 그룹화 완료: " + coursesByCategory.size() + "개 카테고리");

        // 카테고리별로 RequirementCategory 생성
        for (Map.Entry<String, List<sprout.app.sakmvp1.models.CourseRequirement>> entry : coursesByCategory.entrySet()) {
            String categoryName = entry.getKey();
            List<sprout.app.sakmvp1.models.CourseRequirement> courses = entry.getValue();

            sprout.app.sakmvp1.models.RequirementCategory category =
                new sprout.app.sakmvp1.models.RequirementCategory(
                    categoryName, categoryName, "list");

            category.setCourses(courses);
            category.setRequired(creditReqs.getRequiredCredits(categoryName));
            category.setRequiredType("credits");

            categories.add(category);

            Log.d(TAG, "    - " + categoryName + ": " + courses.size() + "개 과목, " +
                  category.getRequired() + "학점 필요");
        }

        return categories;
    }

    /**
     * 통합 졸업요건 규칙 로드 완료 리스너
     */
    public interface OnGraduationRulesLoadedListener {
        void onSuccess(sprout.app.sakmvp1.models.GraduationRules rules);
        void onFailure(Exception e);
    }

    /**
     * V1→V2 변환 완료 리스너
     */
    private interface OnV1ToV2ConversionListener {
        void onSuccess(sprout.app.sakmvp1.models.GraduationRules rules);
        void onFailure(Exception e);
    }
}
