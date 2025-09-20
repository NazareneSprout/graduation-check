package sprout.app.sakmvp1;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirebaseDataManager {
    private static final String TAG = "FirebaseDataManager";
    private FirebaseFirestore db;

    public FirebaseDataManager() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseFirestore 인스턴스 초기화 성공");
        } catch (Exception e) {
            Log.e(TAG, "FirebaseFirestore 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }


    // 학번 데이터 조회
    public interface OnStudentYearsLoadedListener {
        void onSuccess(List<String> years);
        void onFailure(Exception e);
    }

    public void loadStudentYears(OnStudentYearsLoadedListener listener) {
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

                    Log.d(TAG, "학번 데이터 로드 성공: " + years.size() + "개 - " + years);
                    listener.onSuccess(years);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학번 데이터 로드 실패", e);
                    // 기본값 제공
                    List<String> defaultYears = new ArrayList<>();
                    defaultYears.add("2020");
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
                            departmentsSet.add(department);
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

    public void loadGraduationRequirements(String department, String track, String year,
                                         OnGraduationRequirementsLoadedListener listener) {
        // 실제 문서 ID 형식: "IT학부_멀티미디어_2020"
        String documentId = department + "_" + track + "_" + year;

        Log.d(TAG, "졸업 요건 조회 시작: " + documentId);

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
}