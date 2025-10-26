package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugFirestoreActivity extends AppCompatActivity {

    private static final String TAG = "DebugFirestore";
    private TextView tvOutput;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);

        // 버튼 추가
        Button btnListAll = new Button(this);
        btnListAll.setText("전공/교양 연결 확인");
        btnListAll.setOnClickListener(v -> {
            listAllDocumentConnections();
            // 로그에도 출력
            logConnectionsToLogcat();
        });

        Button btnCopyIT = new Button(this);
        btnCopyIT.setText("IT학부_2024 → IT학부_2023");
        btnCopyIT.setOnClickListener(v -> copyDocument("교양_IT학부_2024", "교양_IT학부_2023", "2023"));

        Button btnFixDeptCommon = new Button(this);
        btnFixDeptCommon.setText("학부공통필수 → 학부공통 병합");
        btnFixDeptCommon.setOnClickListener(v -> fixDepartmentCommonCategory());

        tvOutput = new TextView(this);
        tvOutput.setTextSize(12);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(tvOutput);

        mainLayout.addView(btnListAll);
        mainLayout.addView(btnCopyIT);
        mainLayout.addView(btnFixDeptCommon);
        mainLayout.addView(scrollView);

        setContentView(mainLayout);

        db = FirebaseFirestore.getInstance();

        // 자동으로 문서 구조 로그 출력
        checkGraduationRequirementsStructure();
    }

    private void checkV2Collection() {
        StringBuilder output = new StringBuilder();
        output.append("=== Firestore 컬렉션 조회 ===\n\n");

        // v1 컬렉션 먼저 확인
        output.append("[v1] graduation_requirements:\n");
        db.collection("graduation_requirements")
            .get()
            .addOnSuccessListener(v1Snapshots -> {
                if (v1Snapshots.isEmpty()) {
                    output.append("  ❌ 비어있음\n\n");
                } else {
                    output.append("  ✅ ").append(v1Snapshots.size()).append("개 문서\n");
                    boolean foundSample = false;
                    for (QueryDocumentSnapshot doc : v1Snapshots) {
                        output.append("    - ").append(doc.getId()).append("\n");

                        // IT학부 문서 하나 샘플로 상세 조회
                        if (!foundSample && doc.getId().startsWith("IT학부")) {
                            foundSample = true;
                            output.append("\n[샘플 문서 상세: ").append(doc.getId()).append("]\n");
                            Map<String, Object> data = doc.getData();
                            for (String key : data.keySet()) {
                                Object value = data.get(key);
                                if (value instanceof List) {
                                    output.append("  ").append(key).append(": List(")
                                          .append(((List<?>) value).size()).append("개)\n");
                                    // 첫 3개 항목만 표시
                                    List<?> list = (List<?>) value;
                                    for (int i = 0; i < Math.min(3, list.size()); i++) {
                                        output.append("    [").append(i).append("] ")
                                              .append(list.get(i).toString()).append("\n");
                                    }
                                } else {
                                    output.append("  ").append(key).append(": ").append(value).append("\n");
                                }
                            }
                            output.append("\n");
                        }
                    }
                    output.append("\n");
                }

                tvOutput.setText(output.toString());
                // v2 컬렉션 확인
                checkV2();
            });
    }

    private void checkV2() {
        StringBuilder output = new StringBuilder(tvOutput.getText().toString());
        output.append("[v2] graduation_requirements_v2:\n");

        tvOutput.setText(output.toString());

        db.collection("graduation_requirements_v2")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    output.append("  ❌ 비어있음\n\n");
                    Log.w(TAG, "graduation_requirements_v2 컬렉션이 비어있습니다.");
                } else {
                    output.append("✅ 총 ").append(queryDocumentSnapshots.size()).append("개의 문서\n\n");
                    Log.d(TAG, "총 " + queryDocumentSnapshots.size() + "개의 문서 발견");

                    int index = 1;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();
                        String cohort = document.getString("cohort");
                        String department = document.getString("department");
                        String track = document.getString("track");
                        String version = document.getString("version");

                        output.append("[").append(index).append("] ").append(docId).append("\n");
                        output.append("    cohort: ").append(cohort != null ? cohort : "N/A").append("\n");
                        output.append("    department: ").append(department != null ? department : "N/A").append("\n");
                        output.append("    track: ").append(track != null ? track : "N/A").append("\n");
                        output.append("    version: ").append(version != null ? version : "N/A").append("\n");

                        Log.d(TAG, "[" + index + "] " + docId + " - cohort: " + cohort +
                              ", dept: " + department + ", track: " + track);

                        // categories 확인
                        Object categoriesObj = document.get("categories");
                        if (categoriesObj instanceof java.util.List) {
                            java.util.List<?> categories = (java.util.List<?>) categoriesObj;
                            output.append("    categories: ").append(categories.size()).append("개\n");

                            for (Object catObj : categories) {
                                if (catObj instanceof java.util.Map) {
                                    java.util.Map<?, ?> cat = (java.util.Map<?, ?>) catObj;
                                    String catName = (String) cat.get("name");
                                    Object coursesObj = cat.get("courses");
                                    int coursesCount = 0;
                                    if (coursesObj instanceof java.util.List) {
                                        coursesCount = ((java.util.List<?>) coursesObj).size();
                                    }
                                    output.append("      * ").append(catName).append(": ")
                                          .append(coursesCount).append("개 과목\n");
                                }
                            }
                        } else {
                            output.append("    categories: 없음\n");
                        }

                        // replacementRules 확인
                        Object rulesObj = document.get("replacementRules");
                        if (rulesObj instanceof java.util.List) {
                            int rulesCount = ((java.util.List<?>) rulesObj).size();
                            output.append("    replacementRules: ").append(rulesCount).append("개\n");
                        }

                        output.append("\n");
                        index++;
                    }
                }

                tvOutput.setText(output.toString());
            })
            .addOnFailureListener(e -> {
                output.append("❌ 오류 발생: ").append(e.getMessage()).append("\n");
                Log.e(TAG, "오류 발생", e);
                tvOutput.setText(output.toString());
            });
    }

    /**
     * 모든 학부/트랙/학번별 전공/교양 문서 연결 확인
     */
    private void listAllDocumentConnections() {
        tvOutput.setText("문서 연결 확인 중...\n");

        db.collection("graduation_requirements_v2")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                StringBuilder output = new StringBuilder();
                output.append("=== 학부/트랙/학번별 연결 문서 ===\n\n");

                if (querySnapshot.isEmpty()) {
                    output.append("❌ v2 문서가 없습니다\n");
                    tvOutput.setText(output.toString());
                    return;
                }

                // 학부별로 그룹화
                java.util.Map<String, java.util.List<QueryDocumentSnapshot>> departmentMap = new java.util.HashMap<>();

                for (QueryDocumentSnapshot document : querySnapshot) {
                    String department = getStringValue(document, "department");
                    if (department != null) {
                        if (!departmentMap.containsKey(department)) {
                            departmentMap.put(department, new java.util.ArrayList<>());
                        }
                        departmentMap.get(department).add(document);
                    }
                }

                // 학부별로 출력
                for (String department : departmentMap.keySet()) {
                    output.append("━━━━━━━━━━━━━━━━━━━━━━\n");
                    output.append("📚 ").append(department).append("\n");
                    output.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

                    java.util.List<QueryDocumentSnapshot> docs = departmentMap.get(department);

                    // cohort 순서로 정렬
                    docs.sort((d1, d2) -> {
                        String c1 = getStringValue(d1, "cohort");
                        String c2 = getStringValue(d2, "cohort");
                        if (c1 == null) c1 = "";
                        if (c2 == null) c2 = "";
                        return c1.compareTo(c2);
                    });

                    for (QueryDocumentSnapshot doc : docs) {
                        String docId = doc.getId();
                        String cohort = getStringValue(doc, "cohort");
                        String track = getStringValue(doc, "track");
                        String generalDocRef = getStringValue(doc, "generalEducationDocRef");

                        output.append("  🎓 ").append(cohort).append("학번");
                        if (track != null && !track.isEmpty()) {
                            output.append(" - ").append(track);
                        }
                        output.append("\n");
                        output.append("     문서ID: ").append(docId).append("\n");
                        output.append("     교양참조: ").append(generalDocRef != null ? generalDocRef : "❌ 미설정").append("\n\n");
                    }
                }

                tvOutput.setText(output.toString());
                Log.d(TAG, output.toString());
            })
            .addOnFailureListener(e -> {
                tvOutput.setText("❌ 오류: " + e.getMessage());
                Log.e(TAG, "문서 조회 실패", e);
            });
    }

    /**
     * 안전하게 String 값 가져오기
     */
    private String getStringValue(QueryDocumentSnapshot doc, String field) {
        try {
            Object value = doc.get(field);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 모든 교양 문서 목록 확인
     */
    private void listAllGyoyangDocuments() {
        tvOutput.setText("문서 목록 로딩 중...\n");

        db.collection("graduation_requirements")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                StringBuilder output = new StringBuilder();
                output.append("=== graduation_requirements 컬렉션 문서 목록 ===\n\n");

                if (querySnapshot.isEmpty()) {
                    output.append("❌ 문서가 없습니다\n");
                    tvOutput.setText(output.toString());
                    return;
                }

                int count = 0;
                for (QueryDocumentSnapshot document : querySnapshot) {
                    String docId = document.getId();

                    // cohort 필드 안전하게 가져오기
                    String cohort = "N/A";
                    try {
                        Object cohortObj = document.get("cohort");
                        if (cohortObj != null) {
                            cohort = cohortObj.toString();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "cohort 필드 읽기 실패: " + e.getMessage());
                    }

                    // 교양 공통 문서만 표시
                    if (docId.contains("교양")) {
                        count++;
                        output.append("[").append(count).append("] ").append(docId).append("\n");
                        output.append("    cohort: ").append(cohort).append("\n\n");
                    }
                }

                if (count == 0) {
                    output.append("❌ 교양 관련 문서가 없습니다\n");
                }

                tvOutput.setText(output.toString());
                Log.d(TAG, output.toString());
            })
            .addOnFailureListener(e -> {
                tvOutput.setText("❌ 오류: " + e.getMessage());
                Log.e(TAG, "문서 목록 조회 실패", e);
            });
    }

    /**
     * 교양 공통 문서 확인
     */
    private void checkGyoyangDocument(String docId) {
        tvOutput.setText("문서 로딩 중...\n");

        db.collection("graduation_requirements").document(docId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    tvOutput.setText("❌ 문서가 존재하지 않습니다: " + docId);
                    return;
                }

                StringBuilder output = new StringBuilder();
                output.append("=== ").append(docId).append(" 문서 구조 ===\n\n");

                Map<String, Object> data = documentSnapshot.getData();
                if (data == null) {
                    tvOutput.setText("❌ 문서 데이터가 null입니다");
                    return;
                }

                output.append("최상위 키: ").append(data.keySet()).append("\n\n");

                // rules 확인
                Object rulesObj = data.get("rules");
                if (rulesObj instanceof Map) {
                    Map<String, Object> rules = (Map<String, Object>) rulesObj;
                    output.append("rules 키: ").append(rules.keySet()).append("\n\n");

                    // 교양 확인
                    Object gyoyangObj = rules.get("교양");
                    if (gyoyangObj instanceof Map) {
                        Map<String, Object> gyoyangMap = (Map<String, Object>) gyoyangObj;
                        output.append("교양 Map 키: ").append(gyoyangMap.keySet()).append("\n\n");

                        // 각 카테고리 확인
                        for (String category : new String[]{"교양필수", "교양선택", "소양"}) {
                            Object categoryObj = gyoyangMap.get(category);
                            if (categoryObj instanceof List) {
                                List<?> categoryList = (List<?>) categoryObj;
                                output.append("■ ").append(category).append(": ").append(categoryList.size()).append("개\n");

                                // 첫 2개 샘플 표시
                                for (int i = 0; i < Math.min(2, categoryList.size()); i++) {
                                    Object item = categoryList.get(i);
                                    if (item instanceof Map) {
                                        Map<String, Object> itemMap = (Map<String, Object>) item;
                                        output.append("  [").append(i).append("] 키: ").append(itemMap.keySet()).append("\n");
                                        output.append("      과목명: ").append(itemMap.get("과목명")).append("\n");
                                        output.append("      학점: ").append(itemMap.get("학점")).append("\n");
                                    }
                                }
                                output.append("\n");
                            }
                        }
                    }
                }

                tvOutput.setText(output.toString());
                Log.d(TAG, output.toString());
            })
            .addOnFailureListener(e -> {
                tvOutput.setText("❌ 오류: " + e.getMessage());
                Log.e(TAG, "문서 로드 실패", e);
            });
    }

    /**
     * graduation_requirements 컬렉션 전체 구조 확인
     */
    private void checkGraduationRequirementsStructure() {
        Log.d(TAG, "=".repeat(60));
        Log.d(TAG, "graduation_requirements 컬렉션 구조 분석 시작");
        Log.d(TAG, "=".repeat(60));

        db.collection("graduation_requirements")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "총 문서 수: " + querySnapshot.size());

                // 문서 타입별로 분류
                List<QueryDocumentSnapshot> gradDocs = new ArrayList<>();  // 졸업요건_ 문서
                List<QueryDocumentSnapshot> majorDocs = new ArrayList<>(); // 전공 문서
                List<QueryDocumentSnapshot> generalDocs = new ArrayList<>(); // 교양 문서

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String docId = doc.getId();
                    if (docId.startsWith("졸업요건_")) {
                        gradDocs.add(doc);
                    } else if (docId.startsWith("교양_")) {
                        generalDocs.add(doc);
                    } else {
                        majorDocs.add(doc);
                    }
                }

                Log.d(TAG, "\n[문서 타입별 개수]");
                Log.d(TAG, "- 졸업요건 문서: " + gradDocs.size() + "개");
                Log.d(TAG, "- 전공 문서: " + majorDocs.size() + "개");
                Log.d(TAG, "- 교양 문서: " + generalDocs.size() + "개");

                // 졸업요건 문서 샘플 확인
                if (!gradDocs.isEmpty()) {
                    Log.d(TAG, "\n" + "=".repeat(60));
                    Log.d(TAG, "[졸업요건 문서 샘플]");
                    Log.d(TAG, "=".repeat(60));
                    QueryDocumentSnapshot sample = gradDocs.get(0);
                    logDocumentStructure(sample);
                }

                // 전공 문서 샘플 확인
                if (!majorDocs.isEmpty()) {
                    Log.d(TAG, "\n" + "=".repeat(60));
                    Log.d(TAG, "[전공 문서 샘플]");
                    Log.d(TAG, "=".repeat(60));
                    QueryDocumentSnapshot sample = majorDocs.get(0);
                    logDocumentStructure(sample);
                }

                // 교양 문서 샘플 확인
                if (!generalDocs.isEmpty()) {
                    Log.d(TAG, "\n" + "=".repeat(60));
                    Log.d(TAG, "[교양 문서 샘플]");
                    Log.d(TAG, "=".repeat(60));
                    QueryDocumentSnapshot sample = generalDocs.get(0);
                    logDocumentStructure(sample);
                }

                Log.d(TAG, "\n" + "=".repeat(60));
                Log.d(TAG, "구조 분석 완료");
                Log.d(TAG, "=".repeat(60));
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "구조 분석 실패", e);
            });
    }

    /**
     * 문서 구조를 로그로 출력
     */
    private void logDocumentStructure(QueryDocumentSnapshot doc) {
        String docId = doc.getId();
        Map<String, Object> data = doc.getData();

        Log.d(TAG, "\n문서 ID: " + docId);
        Log.d(TAG, "\n[최상위 필드]");

        for (String key : data.keySet()) {
            Object value = data.get(key);

            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                Log.d(TAG, "  " + key + ": Map (" + map.size() + "개 키)");

                // rules 필드는 더 자세히 분석
                if ("rules".equals(key)) {
                    logRulesStructure(map);
                }
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                Log.d(TAG, "  " + key + ": List (" + list.size() + "개 항목)");
            } else {
                Log.d(TAG, "  " + key + ": " + value);
            }
        }
    }

    /**
     * rules 구조 자세히 로그
     */
    private void logRulesStructure(Map<?, ?> rules) {
        Log.d(TAG, "\n  [rules 구조 상세]");

        for (Object keyObj : rules.keySet()) {
            String key = keyObj.toString();
            Object value = rules.get(key);

            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                Log.d(TAG, "    " + key + ": Map");

                // 학기 데이터인 경우 (예: "1학년 1학기")
                for (Object subKeyObj : map.keySet()) {
                    String subKey = subKeyObj.toString();
                    Object subValue = map.get(subKey);

                    if (subValue instanceof List) {
                        List<?> list = (List<?>) subValue;
                        Log.d(TAG, "      " + subKey + ": List (" + list.size() + "개 과목)");

                        // 첫 번째 과목 샘플 출력
                        if (!list.isEmpty() && list.get(0) instanceof Map) {
                            Map<?, ?> course = (Map<?, ?>) list.get(0);
                            Log.d(TAG, "        샘플: " + course.keySet());
                        }
                    }
                }
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                Log.d(TAG, "    " + key + ": List (" + list.size() + "개 항목)");

                // 첫 번째 항목 샘플 출력
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    Map<?, ?> item = (Map<?, ?>) list.get(0);
                    Log.d(TAG, "      샘플: " + item.keySet());
                }
            }
        }
    }

    /**
     * 연결 정보를 logcat에 출력 (분석용)
     */
    private void logConnectionsToLogcat() {
        db.collection("graduation_requirements_v2")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "=".repeat(50));
                Log.d(TAG, "전공/교양 문서 연결 분석");
                Log.d(TAG, "=".repeat(50));

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String docId = doc.getId();
                    String department = getStringValue(doc, "department");
                    String cohort = getStringValue(doc, "cohort");
                    String track = getStringValue(doc, "track");
                    String generalRef = getStringValue(doc, "generalEducationDocRef");

                    Log.d(TAG, String.format("[%s_%s_%s] -> 교양: %s",
                        department, track, cohort,
                        generalRef != null ? generalRef : "미설정"));
                }

                Log.d(TAG, "=".repeat(50));
            });
    }

    /**
     * 교양_공통_2022 데이터를 다른 연도로 복사 (cohort만 변경)
     */
    private void copyDocument(String sourceDocId, String targetDocId, String newCohort) {
        tvOutput.setText("복사 시작: " + sourceDocId + " → " + targetDocId + "\n");

        // 소스 문서 읽기
        db.collection("graduation_requirements").document(sourceDocId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    tvOutput.setText("❌ 소스 문서가 존재하지 않습니다: " + sourceDocId);
                    return;
                }

                Map<String, Object> data = documentSnapshot.getData();
                if (data == null) {
                    tvOutput.setText("❌ 문서 데이터가 null입니다");
                    return;
                }

                // 데이터 복사 (cohort만 변경)
                Map<String, Object> newData = new HashMap<>(data);
                newData.put("cohort", newCohort);

                // 타겟 문서에 저장
                db.collection("graduation_requirements").document(targetDocId)
                    .set(newData)
                    .addOnSuccessListener(aVoid -> {
                        tvOutput.setText("✅ 복사 완료!\n" +
                                "소스: " + sourceDocId + "\n" +
                                "타겟: " + targetDocId + "\n" +
                                "Cohort: " + newCohort);
                        Log.d(TAG, "복사 완료: " + sourceDocId + " → " + targetDocId);
                    })
                    .addOnFailureListener(e -> {
                        tvOutput.setText("❌ 저장 실패: " + e.getMessage());
                        Log.e(TAG, "저장 실패", e);
                    });
            })
            .addOnFailureListener(e -> {
                tvOutput.setText("❌ 소스 문서 로드 실패: " + e.getMessage());
                Log.e(TAG, "소스 문서 로드 실패", e);
            });
    }

    /**
     * "학부공통필수" 카테고리를 "학부공통"으로 병합
     * IT학부_인공지능_2020 문서에서 rules.majors 구조의 "학부공통필수"를 "학부공통"으로 변경
     */
    private void fixDepartmentCommonCategory() {
        String docId = "IT학부_인공지능_2020";
        tvOutput.setText("🔧 학부공통필수 → 학부공통 병합 시작...\n문서: " + docId + "\n\n");
        Log.d(TAG, "========================================");
        Log.d(TAG, "학부공통필수 → 학부공통 병합 시작");
        Log.d(TAG, "문서: " + docId);
        Log.d(TAG, "========================================");

        db.collection("graduation_requirements").document(docId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    String msg = "❌ 문서가 존재하지 않습니다: " + docId;
                    tvOutput.setText(msg);
                    Log.e(TAG, msg);
                    return;
                }

                Map<String, Object> data = documentSnapshot.getData();
                if (data == null) {
                    String msg = "❌ 문서 데이터가 null입니다";
                    tvOutput.setText(msg);
                    Log.e(TAG, msg);
                    return;
                }

                // rules.majors 구조 확인
                Map<String, Object> rules = (Map<String, Object>) data.get("rules");
                if (rules == null) {
                    String msg = "❌ rules 필드가 없습니다";
                    tvOutput.setText(msg);
                    Log.e(TAG, msg);
                    return;
                }

                Map<String, Object> majors = (Map<String, Object>) rules.get("majors");
                if (majors == null) {
                    String msg = "❌ rules.majors 필드가 없습니다";
                    tvOutput.setText(msg);
                    Log.e(TAG, msg);
                    return;
                }

                // "학부공통필수" 찾기
                Object deptCommonRequired = majors.get("학부공통필수");
                if (deptCommonRequired == null) {
                    String msg = "⚠️ '학부공통필수' 카테고리가 없습니다";
                    tvOutput.setText(msg);
                    Log.w(TAG, msg);
                    return;
                }

                // "학부공통"으로 이름 변경
                majors.remove("학부공통필수");
                majors.put("학부공통", deptCommonRequired);

                Log.d(TAG, "✓ '학부공통필수' → '학부공통' 변경 완료");
                tvOutput.append("✓ 카테고리 이름 변경: 학부공통필수 → 학부공통\n\n");

                // Firestore에 업데이트
                db.collection("graduation_requirements").document(docId)
                    .update("rules.majors", majors)
                    .addOnSuccessListener(aVoid -> {
                        String msg = "✅ Firestore 업데이트 성공!\n\n" +
                                "변경사항:\n" +
                                "- '학부공통필수' 카테고리 삭제\n" +
                                "- '학부공통' 카테고리로 병합\n\n" +
                                "이제 졸업요건 검사를 다시 실행하세요.";
                        tvOutput.append(msg);
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "✅ Firestore 업데이트 성공");
                        Log.d(TAG, "========================================");
                    })
                    .addOnFailureListener(e -> {
                        String msg = "❌ Firestore 업데이트 실패: " + e.getMessage();
                        tvOutput.append(msg);
                        Log.e(TAG, "Firestore 업데이트 실패", e);
                    });
            })
            .addOnFailureListener(e -> {
                String msg = "❌ 문서 로드 실패: " + e.getMessage();
                tvOutput.setText(msg);
                Log.e(TAG, "문서 로드 실패", e);
            });
    }
}
