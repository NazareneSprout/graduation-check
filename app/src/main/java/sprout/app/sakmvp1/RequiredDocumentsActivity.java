package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
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
 * 필요서류 모아보기 Activity (사용자용)
 */
public class RequiredDocumentsActivity extends AppCompatActivity implements DocumentFolderAdapter.OnFolderClickListener {

    private static final String TAG = "RequiredDocuments";

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private DocumentFolderAdapter adapter;
    private List<DocumentFolder> folderList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_required_documents);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_folders);
        emptyView = findViewById(R.id.empty_view);

        folderList = new ArrayList<>();
        adapter = new DocumentFolderAdapter(folderList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadFolders();
    }

    /**
     * Firestore에서 폴더 목록 불러오기
     */
    private void loadFolders() {
        db.collection("document_folders")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    folderList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        DocumentFolder folder = document.toObject(DocumentFolder.class);
                        folder.setId(document.getId());
                        folderList.add(folder);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading folders", e);
                    Toast.makeText(this, "폴더 로드 실패", Toast.LENGTH_SHORT).show();
                    updateEmptyView();
                });
    }

    /**
     * 빈 화면 표시 여부 업데이트
     */
    private void updateEmptyView() {
        if (folderList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * 폴더 클릭 리스너
     */
    @Override
    public void onFolderClick(DocumentFolder folder) {
        Intent intent = new Intent(this, DocumentFilesActivity.class);
        intent.putExtra("folderId", folder.getId());
        intent.putExtra("folderName", folder.getName());
        startActivity(intent);
    }
}
