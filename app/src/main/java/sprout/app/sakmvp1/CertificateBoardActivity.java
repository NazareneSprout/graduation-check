package sprout.app.sakmvp1;

import android.content.Intent; // [추가]
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import sprout.app.sakmvp1.R;

// Certificate 및 CertificateAdapter import (D-Day/조회수 제거 버전)
import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.CertificateAdapter;

// OnBookmarkClickListener 인터페이스 구현
public class CertificateBoardActivity extends AppCompatActivity implements CertificateAdapter.OnBookmarkClickListener {

    private static final String TAG = "CertBoardActivity";
    private RecyclerView recyclerView;
    private CertificateAdapter adapter;
    private final List<Certificate> certificateList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ListenerRegistration certificateListener;
    private CollectionReference certCollection;
    private ChipGroup chipGroupFilters;
    private String currentFilter = "전체";

    // [삭제] 북마크 필터 관련 변수 2줄 삭제
    // private boolean isShowingOnlyBookmarks = false;
    // private MenuItem menuShowBookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_board);

        // Firestore 및 Auth 초기화
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

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("자격증 모음");
        }

        // 리사이클러뷰 설정
        recyclerView = findViewById(R.id.recycler_view_certificates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CertificateAdapter(certificateList, this);
        recyclerView.setAdapter(adapter);

        // [수정] XML 레이아웃과 동일한 ID로 변경
        chipGroupFilters = findViewById(R.id.chip_group_department);

        // [수정] setOnCheckedChangeListener 사용 (단일 선택)
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                // 사용자가 칩을 다시 눌러 선택 해제 시 '전체'를 기본값으로
                group.check(R.id.chip_all);
                return;
            }

            Chip chip = group.findViewById(checkedId);

            if (chip != null) {
                // [삭제] 칩 클릭 시 북마크 필터 해제 로직 삭제
                // if (isShowingOnlyBookmarks) { ... }

                currentFilter = chip.getText().toString();
                Log.d(TAG, "Filter changed to: " + currentFilter);
                loadCertificates(); // 데이터 다시 로드
            }
        });

        // [추가] 기본으로 '전체' 칩 선택
        chipGroupFilters.check(R.id.chip_all);
    }

    // [수정] 툴바에 메뉴(북마크 버튼) 생성 (내용 간소화)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // res/menu/menu_certificate_board.xml 파일을 참조
        inflater.inflate(R.menu.menu_certificate_board, menu);
        // [삭제] menuShowBookmarks 관련 코드 삭제
        // [삭제] updateBookmarkIcon() 호출 삭제
        return true;
    }

    // [수정] 툴바의 버튼 클릭 시 (새 액티비티 호출)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // 안드로이드 홈 버튼(뒤로가기) 클릭 처리
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        // [수정] 북마크 아이콘 클릭 시
        if (item.getItemId() == R.id.action_show_bookmarks) {
            // "내 북마크" 화면(MyBookmarksActivity)으로 이동
            Intent intent = new Intent(this, MyBookmarksActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // [삭제] updateBookmarkIcon() 메서드 전체 삭제

    @Override
    protected void onStart() {
        super.onStart();
        loadCertificates(); // "전체" 필터로 데이터 로드 시작
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (certificateListener != null) {
            certificateListener.remove();
        }
    }

    /**
     * [수정됨] "북마크 필터" 관련 로직 삭제
     */
    private void loadCertificates() {
        if (certificateListener != null) {
            certificateListener.remove();
        }

        Query query;

        // [삭제] if (isShowingOnlyBookmarks) { ... } 블록 전체 삭제

        // [수정] 항상 칩 필터 기준으로 쿼리 실행
        query = certCollection.orderBy("bookmarkCount", Query.Direction.DESCENDING);

        if (!currentFilter.equals("전체")) {
            query = query.whereEqualTo("department", currentFilter);
        }

        // 3. 실시간 스냅샷 리스너 등록 (쿼리 실행)
        certificateListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Listen failed.", error);
                Toast.makeText(this, "데이터 로드 실패: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (value != null) {
                certificateList.clear();
                for (Certificate doc : value.toObjects(Certificate.class)) {
                    certificateList.add(doc);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 어댑터에서 북마크 버튼 클릭 시 호출될 메서드
     * (이 메서드는 변경 없음)
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

