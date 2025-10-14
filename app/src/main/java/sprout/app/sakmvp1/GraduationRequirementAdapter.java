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
 * 졸업요건 목록 RecyclerView Adapter
 */
public class GraduationRequirementAdapter extends RecyclerView.Adapter<GraduationRequirementAdapter.ViewHolder> {

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
                .inflate(R.layout.item_graduation_requirement, parent, false);
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
        private TextView tvTitle, tvTrack, tvTotalCredits;
        private TextView tvMajorRequired, tvMajorElective, tvMajorAdvanced;
        private TextView tvGeneralRequired, tvGeneralElective;
        private TextView tvLiberalArts, tvFreeElective, tvDepartmentCommon, tvRemainingCredits;
        private TextView tvUpdatedAt;

        // 부모 LinearLayout들
        private View layoutMajorAdvanced, layoutDepartmentCommon;
        private View layoutGeneralRequired, layoutGeneralElective;
        private View layoutLiberalArts, layoutFreeElective, layoutRemainingCredits;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTrack = itemView.findViewById(R.id.tv_track);
            tvTotalCredits = itemView.findViewById(R.id.tv_total_credits);
            tvMajorRequired = itemView.findViewById(R.id.tv_major_required);
            tvMajorElective = itemView.findViewById(R.id.tv_major_elective);
            tvMajorAdvanced = itemView.findViewById(R.id.tv_major_advanced);
            tvGeneralRequired = itemView.findViewById(R.id.tv_general_required);
            tvGeneralElective = itemView.findViewById(R.id.tv_general_elective);
            tvLiberalArts = itemView.findViewById(R.id.tv_liberal_arts);
            tvFreeElective = itemView.findViewById(R.id.tv_free_elective);
            tvDepartmentCommon = itemView.findViewById(R.id.tv_department_common);
            tvRemainingCredits = itemView.findViewById(R.id.tv_remaining_credits);
            tvUpdatedAt = itemView.findViewById(R.id.tv_updated_at);

            // 부모 레이아웃 참조
            layoutMajorAdvanced = (View) tvMajorAdvanced.getParent();
            layoutDepartmentCommon = (View) tvDepartmentCommon.getParent();
            layoutGeneralRequired = (View) tvGeneralRequired.getParent();
            layoutGeneralElective = (View) tvGeneralElective.getParent();
            layoutLiberalArts = (View) tvLiberalArts.getParent();
            layoutFreeElective = (View) tvFreeElective.getParent();
            layoutRemainingCredits = (View) tvRemainingCredits.getParent();

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
            tvTitle.setText(requirement.getDisplayTitle());
            tvTrack.setText(requirement.getDisplayTrack());

            // 각 필드의 합 계산
            int sumOfCredits = requirement.getMajorRequired()
                             + requirement.getMajorElective()
                             + requirement.getMajorAdvanced()
                             + requirement.getGeneralRequired()
                             + requirement.getGeneralElective()
                             + requirement.getLiberalArts()
                             + requirement.getFreeElective()
                             + requirement.getDepartmentCommon();

            // 잔여학점 = 127 - 합계 (Firestore에 잔여학점이 있으면 그 값 사용, 없으면 계산)
            int remainingCredits = requirement.getRemainingCredits();
            if (remainingCredits == 0 && sumOfCredits < 127) {
                remainingCredits = 127 - sumOfCredits;
            }

            // 총 이수학점 = 각 필드의 합 + 잔여학점 (항상 127학점)
            int totalCredits = sumOfCredits + remainingCredits;
            tvTotalCredits.setText(totalCredits + "학점");

            // 전공심화 또는 학부공통 (둘 중 하나만 표시, 부모 레이아웃도 함께 숨김)
            if (requirement.getMajorAdvanced() > 0) {
                layoutMajorAdvanced.setVisibility(View.VISIBLE);
                tvMajorAdvanced.setText(requirement.getMajorAdvanced() + "학점");
            } else {
                layoutMajorAdvanced.setVisibility(View.GONE);
            }

            if (requirement.getDepartmentCommon() > 0) {
                layoutDepartmentCommon.setVisibility(View.VISIBLE);
                tvDepartmentCommon.setText(requirement.getDepartmentCommon() + "학점");
            } else {
                layoutDepartmentCommon.setVisibility(View.GONE);
            }

            // 전공 필수/선택 (항상 표시)
            tvMajorRequired.setText(requirement.getMajorRequired() + "학점");
            tvMajorElective.setText(requirement.getMajorElective() + "학점");

            // 교양 필수/선택 (0이면 부모 레이아웃도 함께 숨김)
            if (requirement.getGeneralRequired() > 0) {
                layoutGeneralRequired.setVisibility(View.VISIBLE);
                tvGeneralRequired.setText(requirement.getGeneralRequired() + "학점");
            } else {
                layoutGeneralRequired.setVisibility(View.GONE);
            }

            if (requirement.getGeneralElective() > 0) {
                layoutGeneralElective.setVisibility(View.VISIBLE);
                tvGeneralElective.setText(requirement.getGeneralElective() + "학점");
            } else {
                layoutGeneralElective.setVisibility(View.GONE);
            }

            // 소양 (0이면 부모 레이아웃도 함께 숨김)
            if (requirement.getLiberalArts() > 0) {
                layoutLiberalArts.setVisibility(View.VISIBLE);
                tvLiberalArts.setText(requirement.getLiberalArts() + "학점");
            } else {
                layoutLiberalArts.setVisibility(View.GONE);
            }

            // 자율선택 또는 잔여학점 (둘 중 하나만 표시)
            if (requirement.getFreeElective() > 0) {
                layoutFreeElective.setVisibility(View.VISIBLE);
                tvFreeElective.setText(requirement.getFreeElective() + "학점");
                layoutRemainingCredits.setVisibility(View.GONE);
            } else if (remainingCredits > 0) {
                layoutFreeElective.setVisibility(View.GONE);
                layoutRemainingCredits.setVisibility(View.VISIBLE);
                tvRemainingCredits.setText(remainingCredits + "학점");
            } else {
                layoutFreeElective.setVisibility(View.GONE);
                layoutRemainingCredits.setVisibility(View.GONE);
            }

            tvUpdatedAt.setText("문서 ID: " + requirement.getId());
        }
    }
}
