package sprout.app.sakmvp1;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        void onDownloadClick(DocumentFile file);
        void onShareClick(DocumentFile file);
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

        // URL이 있으면 다운로드/공유 버튼 표시
        if (!TextUtils.isEmpty(file.getUrl())) {
            holder.layoutActions.setVisibility(View.VISIBLE);
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }

        // 다운로드 버튼 클릭
        holder.ivDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(file);
            }
        });

        // 공유 버튼 클릭
        holder.ivShare.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareClick(file);
            }
        });

        // 아이템 클릭 (전체 영역)
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
        LinearLayout layoutActions;
        ImageView ivDownload;
        ImageView ivShare;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileDescription = itemView.findViewById(R.id.tv_file_description);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            ivDownload = itemView.findViewById(R.id.iv_download);
            ivShare = itemView.findViewById(R.id.iv_share);
        }
    }
}
