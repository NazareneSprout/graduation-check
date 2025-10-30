package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * 관리자용 서류 폴더 Adapter
 */
public class ManageDocumentFolderAdapter extends RecyclerView.Adapter<ManageDocumentFolderAdapter.ViewHolder> {

    private final List<DocumentFolder> folderList;
    private OnFolderActionListener listener;

    public interface OnFolderActionListener {
        void onEdit(DocumentFolder folder);
        void onDelete(DocumentFolder folder, int position);
        void onManageFiles(DocumentFolder folder);
    }

    public ManageDocumentFolderAdapter(List<DocumentFolder> folderList, OnFolderActionListener listener) {
        this.folderList = folderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_document_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentFolder folder = folderList.get(position);
        holder.tvFolderName.setText(folder.getName());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(folder);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(folder, position);
            }
        });

        holder.btnManageFiles.setOnClickListener(v -> {
            if (listener != null) {
                listener.onManageFiles(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFolderName;
        MaterialButton btnEdit;
        MaterialButton btnDelete;
        MaterialButton btnManageFiles;

        ViewHolder(View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnManageFiles = itemView.findViewById(R.id.btn_manage_files);
        }
    }
}
