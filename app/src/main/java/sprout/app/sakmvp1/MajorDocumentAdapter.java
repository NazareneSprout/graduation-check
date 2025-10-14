package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 전공 문서 목록 전용 RecyclerView Adapter
 * 전공 관련 필드만 표시 (총학점, 잔여학점 제외)
 */
public class MajorDocumentAdapter extends RecyclerView.Adapter<MajorDocumentAdapter.ViewHolder> {

    private List<GraduationRequirement> requirements = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(GraduationRequirement requirement);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(GraduationRequirement requirement);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setRequirements(List<GraduationRequirement> requirements) {
        this.requirements = requirements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_major_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GraduationRequirement requirement = requirements.get(position);
        holder.bind(requirement);
    }

    @Override
    public int getItemCount() {
        return requirements.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDocId;
        private TextView tvMajorRequired, tvMajorElective, tvMajorAdvanced, tvDepartmentCommon;
        private View layoutMajorRequired, layoutMajorElective, layoutMajorAdvanced, layoutDepartmentCommon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocId = itemView.findViewById(R.id.tv_doc_id);
            tvMajorRequired = itemView.findViewById(R.id.tv_major_required);
            tvMajorElective = itemView.findViewById(R.id.tv_major_elective);
            tvMajorAdvanced = itemView.findViewById(R.id.tv_major_advanced);
            tvDepartmentCommon = itemView.findViewById(R.id.tv_department_common);

            layoutMajorRequired = (View) tvMajorRequired.getParent();
            layoutMajorElective = (View) tvMajorElective.getParent();
            layoutMajorAdvanced = (View) tvMajorAdvanced.getParent();
            layoutDepartmentCommon = (View) tvDepartmentCommon.getParent();

            itemView.setOnClickListener(v -> {
                if (listener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(requirements.get(getBindingAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    return longClickListener.onItemLongClick(requirements.get(getBindingAdapterPosition()));
                }
                return false;
            });
        }

        public void bind(GraduationRequirement requirement) {
            tvDocId.setText(requirement.getId());

            // 전공필수
            if (requirement.getMajorRequired() > 0) {
                layoutMajorRequired.setVisibility(View.VISIBLE);
                tvMajorRequired.setText(requirement.getMajorRequired() + "학점");
            } else {
                layoutMajorRequired.setVisibility(View.GONE);
            }

            // 전공선택
            if (requirement.getMajorElective() > 0) {
                layoutMajorElective.setVisibility(View.VISIBLE);
                tvMajorElective.setText(requirement.getMajorElective() + "학점");
            } else {
                layoutMajorElective.setVisibility(View.GONE);
            }

            // 전공심화
            if (requirement.getMajorAdvanced() > 0) {
                layoutMajorAdvanced.setVisibility(View.VISIBLE);
                tvMajorAdvanced.setText(requirement.getMajorAdvanced() + "학점");
            } else {
                layoutMajorAdvanced.setVisibility(View.GONE);
            }

            // 학부공통
            if (requirement.getDepartmentCommon() > 0) {
                layoutDepartmentCommon.setVisibility(View.VISIBLE);
                tvDepartmentCommon.setText(requirement.getDepartmentCommon() + "학점");
            } else {
                layoutDepartmentCommon.setVisibility(View.GONE);
            }
        }
    }
}
