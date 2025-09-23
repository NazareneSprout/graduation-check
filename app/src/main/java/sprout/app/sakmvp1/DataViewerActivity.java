package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataViewerActivity extends AppCompatActivity {
    private static final String TAG = "DataViewerActivity";

    private FirebaseDataManager dataManager;
    private Spinner spinnerCollections;
    private EditText editTextDocumentId;
    private Button btnLoadCollection, btnLoadDocument, btnGetCount, btnTestData;
    private ListView listViewResults;
    private TextView textViewCount;

    private ArrayAdapter<String> collectionsAdapter;
    private ArrayAdapter<String> resultsAdapter;
    private List<String> collections;
    private List<String> resultsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_viewer);

        initViews();
        setupAdapters();
        setupListeners();

        try {
            dataManager = FirebaseDataManager.getInstance();
            Log.d(TAG, "FirebaseDataManager 초기화 성공");

            // Firebase 연결 테스트
            testFirebaseConnection();

            loadAvailableCollections();
        } catch (Exception e) {
            Log.e(TAG, "FirebaseDataManager 초기화 실패", e);
            Toast.makeText(this, "Firebase 초기화 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
    }

    private void loadAvailableCollections() {
        dataManager.loadAvailableCollections(new FirebaseDataManager.OnCollectionNamesLoadedListener() {
            @Override
            public void onSuccess(List<String> collectionNames) {
                collections.clear();
                collections.addAll(collectionNames);
                collectionsAdapter.notifyDataSetChanged();
                Log.d(TAG, "컬렉션 목록 로드 성공: " + collectionNames.size() + "개");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "컬렉션 목록 로드 실패", e);
                Toast.makeText(DataViewerActivity.this, "컬렉션 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCollectionData() {
        String selectedCollection = getSelectedCollection();
        if (selectedCollection == null) return;

        resultsList.clear();
        resultsAdapter.notifyDataSetChanged();

        dataManager.loadAllDocumentsFromCollection(selectedCollection, new FirebaseDataManager.OnCollectionDataLoadedListener() {
            @Override
            public void onSuccess(List<Map<String, Object>> documents) {
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
                Log.e(TAG, selectedCollection + " 컬렉션 데이터 로드 실패", e);
                Toast.makeText(DataViewerActivity.this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDocumentData() {
        String selectedCollection = getSelectedCollection();
        String documentId = editTextDocumentId.getText().toString().trim();

        if (selectedCollection == null || documentId.isEmpty()) {
            Toast.makeText(this, "컬렉션과 문서 ID를 모두 선택/입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        resultsList.clear();
        resultsAdapter.notifyDataSetChanged();

        dataManager.loadDocument(selectedCollection, documentId, new FirebaseDataManager.OnDocumentLoadedListener() {
            @Override
            public void onSuccess(Map<String, Object> document) {
                resultsList.clear();
                resultsList.add(formatDocumentData(document));
                resultsAdapter.notifyDataSetChanged();
                Log.d(TAG, "문서 로드 성공: " + documentId);
                Toast.makeText(DataViewerActivity.this, "문서를 로드했습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "문서 로드 실패: " + documentId, e);
                Toast.makeText(DataViewerActivity.this, "문서 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDocumentCount() {
        String selectedCollection = getSelectedCollection();
        if (selectedCollection == null) return;

        dataManager.getDocumentCount(selectedCollection, new FirebaseDataManager.OnCountLoadedListener() {
            @Override
            public void onSuccess(int count) {
                textViewCount.setText("문서 개수: " + count);
                Log.d(TAG, selectedCollection + " 컬렉션 문서 개수: " + count);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "문서 개수 조회 실패", e);
                Toast.makeText(DataViewerActivity.this, "개수 조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSelectedCollection() {
        int position = spinnerCollections.getSelectedItemPosition();
        if (position >= 0 && position < collections.size()) {
            return collections.get(position);
        }
        Toast.makeText(this, "컬렉션을 선택해주세요.", Toast.LENGTH_SHORT).show();
        return null;
    }

    private String formatDocumentData(Map<String, Object> document) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(document.get("id")).append("\n");

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            if (!"id".equals(entry.getKey())) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private void createTestData() {
        if (dataManager == null) {
            Toast.makeText(this, "DataManager가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "IT학부_멀티미디어_2020 문서 조회 시작");
        Toast.makeText(this, "Firestore에서 IT학부_멀티미디어_2020 문서 조회 중...", Toast.LENGTH_SHORT).show();

        // 새로운 상세 문서 구조 확인 메소드 사용
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

                // 연결 실패 시 결과 창에 표시
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

}