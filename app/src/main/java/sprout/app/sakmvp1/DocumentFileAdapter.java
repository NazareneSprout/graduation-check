package sprout.app.sakmvp1;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 서류 파일 목록 Adapter
 */
public class DocumentFileAdapter extends RecyclerView.Adapter<DocumentFileAdapter.ViewHolder> {

    private final List<DocumentFile> fileList;
    private OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(DocumentFile file);
    }

    public DocumentFileAdapter(List<DocumentFile> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document_file, parent, false);
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

        // URL이 있으면 링크 아이콘 표시
        if (!TextUtils.isEmpty(file.getUrl())) {
            holder.ivLinkIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivLinkIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(file);
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
        ImageView ivLinkIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileDescription = itemView.findViewById(R.id.tv_file_description);
            ivLinkIcon = itemView.findViewById(R.id.iv_link_icon);
        }
    }
}
