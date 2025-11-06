package sprout.app.sakmvp1;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;

/**
 * 자격증 관리 RecyclerView 어댑터 (관리자 전용)
 */
public class CertificateAdminAdapter extends RecyclerView.Adapter<CertificateAdminAdapter.ViewHolder> {

    private List<Certificate> certificates;
    private OnCertificateActionListener listener;

    /**
     * 자격증 액션 리스너 인터페이스
     */
    public interface OnCertificateActionListener {
        void onEdit(Certificate certificate);
        void onDelete(Certificate certificate);
    }

    public CertificateAdminAdapter(List<Certificate> certificates, OnCertificateActionListener listener) {
        this.certificates = certificates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_certificate_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Certificate certificate = certificates.get(position);

        // 자격증 이름
        holder.tvTitle.setText(certificate.getTitle());

        // 발급 기관
        holder.tvIssuer.setText(certificate.getIssuer());

        // 학부
        if (!TextUtils.isEmpty(certificate.getDepartment())) {
            holder.chipDepartment.setText(certificate.getDepartment());
            holder.chipDepartment.setVisibility(View.VISIBLE);
        } else {
            holder.chipDepartment.setVisibility(View.GONE);
        }

        // URL (선택사항)
        if (!TextUtils.isEmpty(certificate.getTargetUrl())) {
            holder.tvUrl.setText(certificate.getTargetUrl());
            holder.tvUrl.setVisibility(View.VISIBLE);
        } else {
            holder.tvUrl.setVisibility(View.GONE);
        }

        // 수정 버튼
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(certificate);
            }
        });

        // 삭제 버튼
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(certificate);
            }
        });
    }

    @Override
    public int getItemCount() {
        return certificates.size();
    }

    /**
     * ViewHolder 클래스
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvIssuer;
        Chip chipDepartment;
        TextView tvUrl;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvIssuer = itemView.findViewById(R.id.tv_issuer);
            chipDepartment = itemView.findViewById(R.id.chip_department);
            tvUrl = itemView.findViewById(R.id.tv_url);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
