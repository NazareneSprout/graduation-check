package sprout.app.sakmvp1;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Firestore 탐색/점검용 간단 뷰어 화면
 *
 * 기능
 * - 사용 가능한 컬렉션 목록 로드/선택
 * - 컬렉션 전체 문서 로드 / 특정 문서(ID) 로드
 * - 컬렉션 문서 개수 조회
 * - 사전 정의된 테스트 데이터 조회(로그 확인)
 *
 * UX/안정성
 * - WindowInsetsCompat로 시스템 인셋 처리(가드 포함)
 * - 회전 시 상태 저장/복원(선택 인덱스, 결과 목록, 카운트 텍스트)
 * - 로딩 중 버튼 비활성화(중복 클릭 방지)
 * - 컬렉션명 정렬 및 선택 복원
 * - 결과 항목 탭 시 클립보드 복사(디버깅 편의)
 */
public class DataViewerActivity extends AppCompatActivity {
    private static final String TAG = "DataViewerActivity";

    // ── SavedInstanceState 키 ────────────────────────────────────────────────
    private static final String S_SELECTED_COLLECTION_INDEX = "s_selected_collection_index";
    private static final String S_RESULTS_LIST = "s_results_list";
    private static final String S_COUNT_TEXT = "s_count_text";
    private static final String S_DOC_ID = "s_doc_id";

    // ── Firebase 매니저 ─────────────────────────────────────────────────────
    private FirebaseDataManager dataManager;

    // ── UI ──────────────────────────────────────────────────────────────────
    private Spinner spinnerCollections;
    private EditText editTextDocumentId;
    private Button btnLoadCollection, btnLoadDocument, btnGetCount, btnTestData;
    private ListView listViewResults;
    private TextView textViewCount;

    // ── 어댑터/데이터 ────────────────────────────────────────────────────────
    private ArrayAdapter<String> collectionsAdapter;
    private ArrayAdapter<String> resultsAdapter;
    private List<String> collections;  // 컬렉션 이름 목록
    private List<String> resultsList;  // 결과 문자열 목록

    // 상태 복원용
    private int restoredCollectionIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HighContrastHelper.applyHighContrastTheme(this);
        setContentView(R.layout.activity_data_viewer);

        // 시스템 인셋(상태바/내비게이션) 패딩 적용 ─ 루트 뷰가 없을 수도 있으니 가드
        setupSystemUI();

        initViews();
        setupAdapters();
        setupListeners();

        try {
            dataManager = FirebaseDataManager.getInstance();
            Log.d(TAG, "FirebaseDataManager 초기화 성공");

            // 연결 점검
            testFirebaseConnection();

            // 상태 복원: 컬렉션 선택 인덱스/리스트/카운트 텍스트/문서ID
            if (savedInstanceState != null) {
                restoredCollectionIndex = savedInstanceState.getInt(S_SELECTED_COLLECTION_INDEX, -1);
                ArrayList<String> savedResults = savedInstanceState.getStringArrayList(S_RESULTS_LIST);
                if (savedResults != null) {
                    resultsList.clear();
                    resultsList.addAll(savedResults);
                    resultsAdapter.notifyDataSetChanged();
                }
                textViewCount.setText(savedInstanceState.getString(S_COUNT_TEXT, ""));
                editTextDocumentId.setText(savedInstanceState.getString(S_DOC_ID, ""));
            }

            // 사용 가능한 컬렉션 목록 로드
            loadAvailableCollections();
        } catch (Exception e) {
            Log.e(TAG, "FirebaseDataManager 초기화 실패", e);
            Toast.makeText(this, "Firebase 초기화 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** WindowInsetsCompat로 루트 컨테이너에 시스템 바 패딩 적용(가드 포함) */
    private void setupSystemUI() {
        View root = findViewById(R.id.main);
        if (root == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void initViews() {
        spinnerCollections = findViewById(R.id.spinner_collections);
        editTextDocumentId = findViewById(R.id.edit_text_document_id);
        btnLoadCollection = findViewById(R.id.btn_load_collection);
        btnLoadDocument = findViewById(R.id.btn_load_document);
        btnGetCount = findViewById(R.id.btn_get_count);
        btnTestData = findViewById(R.id.btn_test_data);
        listViewResults = findViewById(R.id.list_view_results);
        textViewCount = findViewById(R.id.text_view_count);
    }

    private void setupAdapters() {
        collections = new ArrayList<>();
        collectionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, collections);
        collectionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCollections.setAdapter(collectionsAdapter);

        resultsList = new ArrayList<>();
        resultsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultsList);
        listViewResults.setAdapter(resultsAdapter);
    }

    private void setupListeners() {
        btnLoadCollection.setOnClickListener(v -> loadCollectionData());
        btnLoadDocument.setOnClickListener(v -> loadDocumentData());
        btnGetCount.setOnClickListener(v -> getDocumentCount());
        btnTestData.setOnClickListener(v -> createTestData());

        // 결과 항목 탭 시 클립보드 복사(개발 편의)
        listViewResults.setOnItemClickListener((parent, view, position, id) -> {
            String text = resultsList.get(position);
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("document", text));
            Toast.makeText(this, "클립보드로 복사됨", Toast.LENGTH_SHORT).show();
        });

        // 스피너 선택 변경 시 로그만(자동 로드는 사용자가 명시적으로 버튼을 누를 때)
        spinnerCollections.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "컬렉션 선택: " + collections.get(position));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /** 사용 가능한 컬렉션 목록 로드 후 정렬 및 선택 복원 */
    private void loadAvailableCollections() {
        setLoading(true);
        dataManager.loadAvailableCollections(new FirebaseDataManager.OnCollectionNamesLoadedListener() {
            @Override
            public void onSuccess(List<String> collectionNames) {
                setLoading(false);
                collections.clear();
                collections.addAll(collectionNames);

                // 보기 좋게 정렬(사전순)
                Collections.sort(collections, String.CASE_INSENSITIVE_ORDER);

                collectionsAdapter.notifyDataSetChanged();
                Log.d(TAG, "컬렉션 목록 로드 성공: " + collectionNames.size() + "개");

                // 이전 선택 인덱스 복원
                if (restoredCollectionIndex >= 0 && restoredCollectionIndex < collections.size()) {
                    spinnerCollections.setSelection(restoredCollectionIndex, false);
                }
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                Log.e(TAG, "컬렉션 목록 로드 실패", e);
                Toast.makeText(DataViewerActivity.this, "컬렉션 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 현재 스피너에서 선택된 컬렉션명 반환(없으면 토스트) */
    private String getSelectedCollection() {
        int position = spinnerCollections.getSelectedItemPosition();
        if (position >= 0 && position < collections.size()) {
            return collections.get(position);
        }
        Toast.makeText(this, "컬렉션을 선택해주세요.", Toast.LENGTH_SHORT).show();
        return null;
    }

    /** 컬렉션 전체 문서 로드 */
    private void loadCollectionData() {
        if (dataManager == null) {
            Toast.makeText(this, "DataManager가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedCollection = getSelectedCollection();
        if (selectedCollection == null) return;

        setLoading(true);
        clearResults();

        dataManager.loadAllDocumentsFromCollection(selectedCollection, new FirebaseDataManager.OnCollectionDataLoadedListener() {
            @Override
            public void onSuccess(List<Map<String, Object>> documents) {
                setLoading(false);
                resultsList.clear();
                for (Map<String, Object> doc : documents) {
                    resultsList.add(formatDocumentData(doc));
                }
                resultsAdapter.notifyDataSetChanged();
                Log.d(TAG, selectedCollection + " 컬렉션 데이터 로드 성공: " + documents.size() + "개 문서");
                Toast.makeText(DataViewerActivity.this, documents.size() + "개 문서를 로드했습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                Log.e(TAG, selectedCollection + " 컬렉션 데이터 로드 실패", e);
                Toast.makeText(DataViewerActivity.this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 특정 문서(ID) 로드 */
    private void loadDocumentData() {
        if (dataManager == null) {
            Toast.makeText(this, "DataManager가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedCollection = getSelectedCollection();
        String documentId = editTextDocumentId.getText().toString().trim();

        if (selectedCollection == null || documentId.isEmpty()) {
            Toast.makeText(this, "컬렉션과 문서 ID를 모두 선택/입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        clearResults();

        dataManager.loadDocument(selectedCollection, documentId, new FirebaseDataManager.OnDocumentLoadedListener() {
            @Override
            public void onSuccess(Map<String, Object> document) {
                setLoading(false);
                resultsList.clear();
                resultsList.add(formatDocumentData(document));
                resultsAdapter.notifyDataSetChanged();
                Log.d(TAG, "문서 로드 성공: " + documentId);
                Toast.makeText(DataViewerActivity.this, "문서를 로드했습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                Log.e(TAG, "문서 로드 실패: " + documentId, e);
                Toast.makeText(DataViewerActivity.this, "문서 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 컬렉션 문서 개수 조회 */
    private void getDocumentCount() {
        if (dataManager == null) {
            Toast.makeText(this, "DataManager가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedCollection = getSelectedCollection();
        if (selectedCollection == null) return;

        setLoading(true);
        dataManager.getDocumentCount(selectedCollection, new FirebaseDataManager.OnCountLoadedListener() {
            @Override
            public void onSuccess(int count) {
                setLoading(false);
                textViewCount.setText("문서 개수: " + count);
                Log.d(TAG, selectedCollection + " 컬렉션 문서 개수: " + count);
            }

            @Override
            public void onFailure(Exception e) {
                setLoading(false);
                Log.e(TAG, "문서 개수 조회 실패", e);
                Toast.makeText(DataViewerActivity.this, "개수 조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 보기 좋게 문서 Map -> 문자열 포맷 */
    private String formatDocumentData(Map<String, Object> document) {
        StringBuilder sb = new StringBuilder();

        // id 키가 없을 수도 있으니 null 가드
        Object id = document.get("id");
        if (id != null) sb.append("ID: ").append(id).append("\n");

        // 키 순서는 Map 구현체에 따라 비결정적임(순서 보장이 필요하면 정렬해서 출력)
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (!"id".equals(entry.getKey())) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    /** 테스트 데이터 조회(로그에서 상세 확인) */
    private void createTestData() {
        if (dataManager == null) {
            Toast.makeText(this, "DataManager가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "IT학부_멀티미디어_2020 문서 조회 시작");
        Toast.makeText(this, "Firestore에서 IT학부_멀티미디어_2020 문서 조회 중...", Toast.LENGTH_SHORT).show();

        // 새로운 상세 문서 구조 확인 메소드 사용(결과는 Logcat의 FirebaseDataManager 태그로 확인)
        dataManager.inspectSpecificDocument("IT학부_멀티미디어_2020");

        resultsList.clear();
        resultsList.add("IT학부_멀티미디어_2020 문서 조회 중...");
        resultsList.add("");
        resultsList.add("Logcat에서 다음 태그로 결과 확인:");
        resultsList.add("FirebaseDataManager");
        resultsList.add("");
        resultsList.add("찾고 있는 필드:");
        resultsList.add("- 교양선택");
        resultsList.add("- 소양");
        resultsList.add("- 자율선택");
        resultsList.add("- 전공필수");
        resultsList.add("- 학부공통");
        resultsAdapter.notifyDataSetChanged();

        Toast.makeText(DataViewerActivity.this, "문서 조회 요청 완료! 로그를 확인하세요.", Toast.LENGTH_SHORT).show();
    }

    /** Firebase 연결 간단 점검 */
    private void testFirebaseConnection() {
        if (dataManager == null) {
            Log.e(TAG, "DataManager가 null입니다");
            return;
        }

        dataManager.testFirebaseConnection(new FirebaseDataManager.OnConnectionTestListener() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "연결 테스트 성공: " + message);
                Toast.makeText(DataViewerActivity.this, "Firebase 연결 확인됨", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "연결 테스트 실패", e);
                String errorMsg = "Firebase 연결 실패: " + e.getMessage();
                Toast.makeText(DataViewerActivity.this, errorMsg, Toast.LENGTH_LONG).show();

                // 연결 실패 안내를 결과창에 표시
                resultsList.clear();
                resultsList.add("❌ Firebase 연결 실패\n\n오류: " + e.getMessage() +
                        "\n\n가능한 원인:\n" +
                        "1. 인터넷 연결 확인\n" +
                        "2. Firestore 보안 규칙 확인\n" +
                        "3. google-services.json 파일 확인");
                resultsAdapter.notifyDataSetChanged();
            }
        });
    }

    /** 결과 영역 초기화(이전 내용 제거) */
    private void clearResults() {
        resultsList.clear();
        resultsAdapter.notifyDataSetChanged();
        textViewCount.setText("");
    }

    /** 로딩 중 버튼 비활성화(중복 클릭/요청 방지) */
    private void setLoading(boolean loading) {
        btnLoadCollection.setEnabled(!loading);
        btnLoadDocument.setEnabled(!loading);
        btnGetCount.setEnabled(!loading);
        btnTestData.setEnabled(!loading);
    }

    // ── 상태 저장/복원 ────────────────────────────────────────────────────────
    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(S_SELECTED_COLLECTION_INDEX, spinnerCollections.getSelectedItemPosition());
        out.putStringArrayList(S_RESULTS_LIST, new ArrayList<>(resultsList));
        out.putString(S_COUNT_TEXT, textViewCount.getText() == null ? "" : textViewCount.getText().toString());
        out.putString(S_DOC_ID, editTextDocumentId.getText() == null ? "" : editTextDocumentId.getText().toString());
    }
}
