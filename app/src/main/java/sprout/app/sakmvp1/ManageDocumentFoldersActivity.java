package sprout.app.sakmvp1;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자용 서류 폴더 관리 Activity
 */
public class ManageDocumentFoldersActivity extends AppCompatActivity implements ManageDocumentFolderAdapter.OnFolderActionListener {

    private static final String TAG = "ManageDocFolders";

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private FloatingActionButton fabAddFolder;
    private ManageDocumentFolderAdapter adapter;
    private List<DocumentFolder> folderList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_document_folders);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_folders);
        emptyView = findViewById(R.id.empty_view);
        fabAddFolder = findViewById(R.id.fab_add_folder);

        folderList = new ArrayList<>();
        adapter = new ManageDocumentFolderAdapter(folderList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAddFolder.setOnClickListener(v -> showAddFolderDialog(null));

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
     * 폴더 추가/편집 다이얼로그
     */
    private void showAddFolderDialog(DocumentFolder existingFolder) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_folder);

        TextView dialogTitle = dialog.findViewById(R.id.dialog_title);
        EditText etFolderName = dialog.findViewById(R.id.et_folder_name);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);

        boolean isEdit = existingFolder != null;

        if (isEdit) {
            dialogTitle.setText("폴더 편집");
            etFolderName.setText(existingFolder.getName());
        } else {
            dialogTitle.setText("폴더 추가");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String folderName = etFolderName.getText().toString().trim();

            if (TextUtils.isEmpty(folderName)) {
                Toast.makeText(this, "폴더명을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEdit) {
                updateFolder(existingFolder.getId(), folderName);
            } else {
                addFolder(folderName);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Firestore에 폴더 추가
     */
    private void addFolder(String folderName) {
        int nextOrder = folderList.size();
        DocumentFolder folder = new DocumentFolder(folderName, nextOrder);

        db.collection("document_folders")
                .add(folder)
                .addOnSuccessListener(documentReference -> {
                    folder.setId(documentReference.getId());
                    folderList.add(folder);
                    adapter.notifyItemInserted(folderList.size() - 1);
                    updateEmptyView();
                    Toast.makeText(this, "폴더가 추가되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding folder", e);
                    Toast.makeText(this, "폴더 추가 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Firestore 폴더 업데이트
     */
    private void updateFolder(String folderId, String newName) {
        db.collection("document_folders")
                .document(folderId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    for (DocumentFolder folder : folderList) {
                        if (folder.getId().equals(folderId)) {
                            folder.setName(newName);
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "폴더가 수정되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating folder", e);
                    Toast.makeText(this, "폴더 수정 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 폴더 편집
     */
    @Override
    public void onEdit(DocumentFolder folder) {
        showAddFolderDialog(folder);
    }

    /**
     * 폴더 삭제
     */
    @Override
    public void onDelete(DocumentFolder folder, int position) {
        new AlertDialog.Builder(this)
                .setTitle("폴더 삭제")
                .setMessage(folder.getName() + "을(를) 삭제하시겠습니까?\n폴더 안의 모든 파일도 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.collection("document_folders")
                            .document(folder.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                folderList.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmptyView();
                                Toast.makeText(this, "폴더가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error deleting folder", e);
                                Toast.makeText(this, "폴더 삭제 실패", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 파일 관리 화면으로 이동
     */
    @Override
    public void onManageFiles(DocumentFolder folder) {
        Intent intent = new Intent(this, ManageDocumentFilesActivity.class);
        intent.putExtra("folderId", folder.getId());
        intent.putExtra("folderName", folder.getName());
        startActivity(intent);
    }
}
