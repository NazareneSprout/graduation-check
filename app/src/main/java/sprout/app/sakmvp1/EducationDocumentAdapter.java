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
 * 교양 문서 목록 전용 RecyclerView Adapter
 * 교양 관련 필드만 표시
 */
public class EducationDocumentAdapter extends RecyclerView.Adapter<EducationDocumentAdapter.ViewHolder> {

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
                .inflate(R.layout.item_education_document, parent, false);
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
        private TextView tvGeneralRequired, tvGeneralElective, tvLiberalArts;
        private View layoutGeneralRequired, layoutGeneralElective, layoutLiberalArts;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocId = itemView.findViewById(R.id.tv_doc_id);
            tvGeneralRequired = itemView.findViewById(R.id.tv_general_required);
            tvGeneralElective = itemView.findViewById(R.id.tv_general_elective);
            tvLiberalArts = itemView.findViewById(R.id.tv_liberal_arts);

            layoutGeneralRequired = (View) tvGeneralRequired.getParent();
            layoutGeneralElective = (View) tvGeneralElective.getParent();
            layoutLiberalArts = (View) tvLiberalArts.getParent();

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

            // 교양필수
            if (requirement.getGeneralRequired() > 0) {
                layoutGeneralRequired.setVisibility(View.VISIBLE);
                tvGeneralRequired.setText(requirement.getGeneralRequired() + "학점");
            } else {
                layoutGeneralRequired.setVisibility(View.GONE);
            }

            // 교양선택
            if (requirement.getGeneralElective() > 0) {
                layoutGeneralElective.setVisibility(View.VISIBLE);
                tvGeneralElective.setText(requirement.getGeneralElective() + "학점");
            } else {
                layoutGeneralElective.setVisibility(View.GONE);
            }

            // 소양
            if (requirement.getLiberalArts() > 0) {
                layoutLiberalArts.setVisibility(View.VISIBLE);
                tvLiberalArts.setText(requirement.getLiberalArts() + "학점");
            } else {
                layoutLiberalArts.setVisibility(View.GONE);
            }
        }
    }
}
