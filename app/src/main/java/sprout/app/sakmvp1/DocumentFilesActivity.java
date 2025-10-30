package sprout.app.sakmvp1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 서류 파일 목록 Activity (사용자용)
 */
public class DocumentFilesActivity extends AppCompatActivity implements DocumentFileAdapter.OnFileClickListener {

    private static final String TAG = "DocumentFiles";

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private DocumentFileAdapter adapter;
    private List<DocumentFile> fileList;
    private FirebaseFirestore db;
    private String folderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_files);

        db = FirebaseFirestore.getInstance();

        // Intent에서 폴더 정보 가져오기
        folderId = getIntent().getStringExtra("folderId");
        String folderName = getIntent().getStringExtra("folderName");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(folderName);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_files);
        emptyView = findViewById(R.id.empty_view);

        fileList = new ArrayList<>();
        adapter = new DocumentFileAdapter(fileList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (folderId != null) {
            loadFiles();
        }
    }

    /**
     * Firestore에서 파일 목록 불러오기 (하위 컬렉션)
     */
    private void loadFiles() {
        db.collection("document_folders")
                .document(folderId)
                .collection("files")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fileList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentFile file = document.toObject(DocumentFile.class);
                        file.setId(document.getId());
                        fileList.add(file);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading files", e);
                    Toast.makeText(this, "파일 로드 실패", Toast.LENGTH_SHORT).show();
                    updateEmptyView();
                });
    }

    /**
     * 빈 화면 표시 여부 업데이트
     */
    private void updateEmptyView() {
        if (fileList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * 파일 클릭 리스너
     */
    @Override
    public void onFileClick(DocumentFile file) {
        // URL이 있으면 브라우저로 열기
        if (!TextUtils.isEmpty(file.getUrl())) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getUrl()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "링크를 열 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, file.getName(), Toast.LENGTH_SHORT).show();
        }
    }
}
