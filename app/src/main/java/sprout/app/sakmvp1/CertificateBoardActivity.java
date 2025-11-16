package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher; // [추가] 검색 감지용
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText; // [추가]
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sprout.app.sakmvp1.CertificateAdapter;

public class CertificateBoardActivity extends BaseActivity implements CertificateAdapter.OnBookmarkClickListener {

    private static final String TAG = "CertBoardActivity";
    private RecyclerView recyclerView;
    private CertificateAdapter adapter;

    // [수정] 리스트를 2개로 분리 (전체 데이터 보관용 / 화면 출력용)
    private final List<Certificate> allCertificateList = new ArrayList<>(); // 원본 데이터
    private final List<Certificate> displayedList = new ArrayList<>();      // 검색 필터링 후 보여줄 데이터

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ListenerRegistration certificateListener;
    private CollectionReference certCollection;
    private ChipGroup chipGroupFilters;
    private EditText editSearch; // [추가] 검색창 변수
    private String currentFilter = "전체";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_board);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();
        certCollection = db.collection("certificates");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("자격증 모음");
        }

        // [추가] 검색창 연결
        editSearch = findViewById(R.id.edit_search);

        recyclerView = findViewById(R.id.recycler_view_certificates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터에는 displayedList(화면 출력용)를 전달
        adapter = new CertificateAdapter(displayedList, this);
        recyclerView.setAdapter(adapter);

        chipGroupFilters = findViewById(R.id.chip_group_department);

        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                group.check(R.id.chip_all);
                return;
            }
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                currentFilter = chip.getText().toString();
                // 칩이 바뀌면 검색창 초기화 (선택 사항)
                editSearch.setText("");
                loadCertificates();
            }
        });
        chipGroupFilters.check(R.id.chip_all);

        // [추가] 검색창 텍스트 변화 감지 리스너 (TextWatcher)
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 글자가 바뀔 때마다 필터링 메서드 호출
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // [추가] 검색어에 따라 리스트를 걸러주는 메서드
    private void filterList(String text) {
        displayedList.clear(); // 화면 목록 비우기

        if (text.isEmpty()) {
            // 검색어가 없으면 원본 데이터를 모두 보여줌
            displayedList.addAll(allCertificateList);
        } else {
            // 검색어가 있으면 포함된 것만 추가
            String query = text.toLowerCase().trim(); // 대소문자 무시 및 공백 제거
            for (Certificate item : allCertificateList) {
                // 자격증 이름(title)이나 내용 등에 검색어가 포함되어 있는지 확인
                // (Certificate 클래스에 getTitle() 혹은 getName()이 있다고 가정)
                if (item.getTitle() != null && item.getTitle().toLowerCase().contains(query)) {
                    displayedList.add(item);
                }
                // 만약 다른 필드(예: 발급기관)로도 검색하고 싶다면 여기에 || 조건 추가
            }
        }
        adapter.notifyDataSetChanged(); // 리사이클러뷰 갱신
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_certificate_board, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_show_bookmarks) {
            Intent intent = new Intent(this, MyBookmarksActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadCertificates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (certificateListener != null) {
            certificateListener.remove();
        }
    }

    private void loadCertificates() {
        if (certificateListener != null) {
            certificateListener.remove();
        }

        Query query = certCollection.orderBy("bookmarkCount", Query.Direction.DESCENDING);

        if (!currentFilter.equals("전체")) {
            query = query.whereEqualTo("department", currentFilter);
        }

        certificateListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Listen failed.", error);
                Toast.makeText(this, "데이터 로드 실패: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (value != null) {
                // 1. 원본 리스트(allCertificateList)에 DB 데이터 저장
                allCertificateList.clear();
                for (Certificate doc : value.toObjects(Certificate.class)) {
                    allCertificateList.add(doc);
                }

                // 2. 현재 검색창에 입력된 텍스트로 즉시 필터링하여 화면에 표시
                // (데이터 로드 직후 검색어가 남아있을 수 있으므로)
                String searchText = editSearch.getText().toString();
                filterList(searchText);
            }
        });
    }

    @Override
    public void onBookmarkClick(Certificate certificate) {
        if (currentUserId == null) return;

        DocumentReference docRef = certCollection.document(certificate.getId());

        db.runTransaction(transaction -> {
            Certificate latestCert = transaction.get(docRef).toObject(Certificate.class);
            if (latestCert == null) {
                throw new FirebaseFirestoreException("Document does not exist",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Map<String, Boolean> bookmarks = latestCert.getBookmarks();
            if (bookmarks == null) {
                bookmarks = new HashMap<>();
            }

            if (bookmarks.containsKey(currentUserId)) {
                bookmarks.remove(currentUserId);
                transaction.update(docRef, "bookmarkCount", FieldValue.increment(-1));
                transaction.update(docRef, "bookmarks", bookmarks);
            } else {
                bookmarks.put(currentUserId, true);
                transaction.update(docRef, "bookmarkCount", FieldValue.increment(1));
                transaction.update(docRef, "bookmarks", bookmarks);
            }
            return null;

        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Bookmark transaction success!");
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Bookmark transaction failure.", e);
            Toast.makeText(this, "작업에 실패했습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}