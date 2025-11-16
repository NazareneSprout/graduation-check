package sprout.app.sakmvp1;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 관리자용 서류 파일 관리 Activity
 */
public class ManageDocumentFilesActivity extends BaseActivity implements ManageDocumentFileAdapter.OnFileActionListener {

    private static final String TAG = "ManageDocFiles";

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private FloatingActionButton fabAddFile;
    private ManageDocumentFileAdapter adapter;
    private List<DocumentFile> fileList;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String folderId;

    private static final int PICK_FILE_REQUEST = 1001;

    private Dialog currentDialog;
    private EditText currentEtFileUrl;
    private EditText currentEtFileName;
    private TextView currentTvFileStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_document_folders); // 같은 레이아웃 재사용

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Intent에서 폴더 정보 가져오기
        folderId = getIntent().getStringExtra("folderId");
        String folderName = getIntent().getStringExtra("folderName");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(folderName + " - 파일 관리");
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_folders);
        emptyView = findViewById(R.id.empty_view);
        fabAddFile = findViewById(R.id.fab_add_folder);

        fileList = new ArrayList<>();
        adapter = new ManageDocumentFileAdapter(fileList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAddFile.setOnClickListener(v -> showAddFileDialog(null));

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
     * 파일 추가/편집 다이얼로그
     */
    private void showAddFileDialog(DocumentFile existingFile) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_file);

        TextView dialogTitle = dialog.findViewById(R.id.dialog_title);
        EditText etFileName = dialog.findViewById(R.id.et_file_name);
        EditText etFileDescription = dialog.findViewById(R.id.et_file_description);
        EditText etFileUrl = dialog.findViewById(R.id.et_file_url);
        MaterialButton btnSelectFile = dialog.findViewById(R.id.btn_select_file);
        TextView tvFileStatus = dialog.findViewById(R.id.tv_file_status);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);

        boolean isEdit = existingFile != null;

        if (isEdit) {
            dialogTitle.setText("파일 편집");
            etFileName.setText(existingFile.getName());
            etFileDescription.setText(existingFile.getDescription());
            etFileUrl.setText(existingFile.getUrl());
            etFileUrl.setEnabled(true); // 편집 모드에서는 URL 직접 수정 가능
            btnSelectFile.setVisibility(View.GONE); // 편집 모드에서는 파일 선택 버튼 숨김
        } else {
            dialogTitle.setText("파일 추가");
        }

        // 현재 다이얼로그 참조 저장
        currentDialog = dialog;
        currentEtFileUrl = etFileUrl;
        currentEtFileName = etFileName;
        currentTvFileStatus = tvFileStatus;

        // 파일 선택 버튼
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String fileName = etFileName.getText().toString().trim();
            String fileDescription = etFileDescription.getText().toString().trim();
            String fileUrl = etFileUrl.getText().toString().trim();

            if (TextUtils.isEmpty(fileName)) {
                Toast.makeText(this, "파일명을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEdit) {
                updateFile(existingFile.getId(), fileName, fileDescription, fileUrl);
            } else {
                addFile(fileName, fileDescription, fileUrl);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Firestore에 파일 추가
     */
    private void addFile(String fileName, String description, String url) {
        int nextOrder = fileList.size();
        DocumentFile file = new DocumentFile(fileName, description, url, nextOrder);

        db.collection("document_folders")
                .document(folderId)
                .collection("files")
                .add(file)
                .addOnSuccessListener(documentReference -> {
                    file.setId(documentReference.getId());
                    fileList.add(file);
                    adapter.notifyItemInserted(fileList.size() - 1);
                    updateEmptyView();
                    Toast.makeText(this, "파일이 추가되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding file", e);
                    Toast.makeText(this, "파일 추가 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Firestore 파일 업데이트
     */
    private void updateFile(String fileId, String newName, String newDescription, String newUrl) {
        db.collection("document_folders")
                .document(folderId)
                .collection("files")
                .document(fileId)
                .update(
                        "name", newName,
                        "description", newDescription,
                        "url", newUrl
                )
                .addOnSuccessListener(aVoid -> {
                    for (DocumentFile file : fileList) {
                        if (file.getId().equals(fileId)) {
                            file.setName(newName);
                            file.setDescription(newDescription);
                            file.setUrl(newUrl);
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "파일이 수정되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating file", e);
                    Toast.makeText(this, "파일 수정 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 파일 편집
     */
    @Override
    public void onEdit(DocumentFile file) {
        showAddFileDialog(file);
    }

    /**
     * 파일 삭제
     */
    @Override
    public void onDelete(DocumentFile file, int position) {
        new AlertDialog.Builder(this)
                .setTitle("파일 삭제")
                .setMessage(file.getName() + "을(를) 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.collection("document_folders")
                            .document(folderId)
                            .collection("files")
                            .document(file.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                fileList.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmptyView();
                                Toast.makeText(this, "파일이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error deleting file", e);
                                Toast.makeText(this, "파일 삭제 실패", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // URI 권한 획득
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException e) {
                    Log.w(TAG, "Could not take persistable URI permission", e);
                }

                // 파일 이름 가져오기
                String fileName = getFileName(uri);
                if (fileName != null && currentEtFileName != null && TextUtils.isEmpty(currentEtFileName.getText())) {
                    currentEtFileName.setText(fileName);
                }

                // 상태 표시
                if (currentTvFileStatus != null) {
                    currentTvFileStatus.setVisibility(View.VISIBLE);
                    currentTvFileStatus.setText("파일 업로드 중...");
                }

                // Firebase Storage에 업로드
                uploadFileToStorage(uri, fileName);
            }
        }
    }

    /**
     * URI에서 파일 이름 가져오기
     */
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * Firebase Storage에 파일 업로드
     */
    private void uploadFileToStorage(Uri fileUri, String fileName) {
        // Storage 경로 생성: document_files/{folderId}/{UUID}_{fileName}
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        StorageReference fileRef = storage.getReference()
                .child("document_files")
                .child(folderId)
                .child(uniqueFileName);

        // 파일 업로드
        UploadTask uploadTask = fileRef.putFile(fileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            // 업로드 진행률 계산
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            if (currentTvFileStatus != null) {
                currentTvFileStatus.setText(String.format("업로드 중... %.0f%%", progress));
            }
        }).addOnSuccessListener(taskSnapshot -> {
            // 업로드 성공 - 다운로드 URL 가져오기
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                if (currentEtFileUrl != null) {
                    currentEtFileUrl.setText(downloadUri.toString());
                }
                if (currentTvFileStatus != null) {
                    currentTvFileStatus.setText("✓ 파일 업로드 완료");
                    currentTvFileStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                }
                Toast.makeText(this, "파일이 업로드되었습니다", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "다운로드 URL 가져오기 실패", e);
                if (currentTvFileStatus != null) {
                    currentTvFileStatus.setText("✗ URL 가져오기 실패");
                    currentTvFileStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                }
                Toast.makeText(this, "URL 가져오기 실패", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "파일 업로드 실패", e);
            if (currentTvFileStatus != null) {
                currentTvFileStatus.setText("✗ 업로드 실패");
                currentTvFileStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
            }
            Toast.makeText(this, "파일 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
