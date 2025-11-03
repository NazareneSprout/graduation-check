package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.Collections; // [추가]
import java.util.Comparator;  // [추가]
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sprout.app.sakmvp1.R;

public class MyBookmarksActivity extends AppCompatActivity implements CertificateAdapter.OnBookmarkClickListener {

    private static final String TAG = "MyBookmarksActivity";
    private RecyclerView recyclerView;
    private CertificateAdapter adapter;
    private final List<Certificate> certificateList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration certificateListener;
    private CollectionReference certCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookmarks);

        // Firestore 및 Auth 초기화
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();
        certCollection = db.collection("certificates");

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("내 북마크");
        }

        // 리사이클러뷰 설정
        recyclerView = findViewById(R.id.recycler_view_bookmarks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // CertificateAdapter 재사용
        adapter = new CertificateAdapter(certificateList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadMyBookmarks(); // 북마크된 목록만 로드
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (certificateListener != null) {
            certificateListener.remove();
        }
    }

    /**
     * [수정] Firestore 쿼리에서 orderBy("title") 제거
     * [수정] 데이터를 앱(Java)에서 직접 정렬
     */
    private void loadMyBookmarks() {
        if (certificateListener != null) {
            certificateListener.remove();
        }

        // [수정] "내 북마크"만 필터링 (Firestore에서 정렬 제거)
        Query query = certCollection
                .whereEqualTo("bookmarks." + currentUserId, true);
        // .orderBy("title", Query.Direction.ASCENDING); // <-- FAILED_PRECONDITION 오류 발생 원인

        certificateListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Listen failed.", error);
                // "데이터 로드 실패: FAILED_PRECONDITION..." 토스트가 이 부분에서 발생합니다.
                Toast.makeText(this, "데이터 로드 실패: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (value != null) {
                // [수정] Firestore에서 가져온 문서를 임시 리스트에 담기
                List<Certificate> docs = value.toObjects(Certificate.class);

                // [수정] 앱(Java)에서 직접 제목순으로 정렬
                try {
                    Collections.sort(docs, new Comparator<Certificate>() {
                        @Override
                        public int compare(Certificate c1, Certificate c2) {
                            // title이 null일 경우를 대비 (안전장치)
                            if (c1.getTitle() == null) return -1;
                            if (c2.getTitle() == null) return 1;
                            return c1.getTitle().compareTo(c2.getTitle());
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Sorting failed", e);
                }


                certificateList.clear();
                certificateList.addAll(docs); // 정렬된 리스트를 어댑터에 추가
                adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * CertificateBoardActivity와 동일한 북마크 해제 로직
     */
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

            // 이 화면에서는 북마크 해제만 일어남
            if (bookmarks.containsKey(currentUserId)) {
                bookmarks.remove(currentUserId);
                transaction.update(docRef, "bookmarkCount", FieldValue.increment(-1));
                transaction.update(docRef, "bookmarks", bookmarks);
            }
            // (북마크 추가 로직은 없어도 됨)

            return null;

        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Bookmark transaction success! (Unbooked)");
            // 실시간 리스너가 자동으로 목록을 갱신합니다.
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

