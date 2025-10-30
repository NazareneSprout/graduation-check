package sprout.app.sakmvp1;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * 관리자용 서류 파일 Adapter
 */
public class ManageDocumentFileAdapter extends RecyclerView.Adapter<ManageDocumentFileAdapter.ViewHolder> {

    private final List<DocumentFile> fileList;
    private OnFileActionListener listener;

    public interface OnFileActionListener {
        void onEdit(DocumentFile file);
        void onDelete(DocumentFile file, int position);
    }

    public ManageDocumentFileAdapter(List<DocumentFile> fileList, OnFileActionListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_document_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentFile file = fileList.get(position);
        holder.tvFileName.setText(file.getName());

        if (!TextUtils.isEmpty(file.getDescription())) {
            holder.tvFileDescription.setText(file.getDescription());
            holder.tvFileDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvFileDescription.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(file);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(file, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        TextView tvFileDescription;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileDescription = itemView.findViewById(R.id.tv_file_description);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
