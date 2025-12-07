package sprout.app.sakmvp1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.models.OtherRequirementGroup;

/**
 * 기타 졸업요건 그룹 RecyclerView 어댑터
 */
public class OtherRequirementGroupAdapter extends RecyclerView.Adapter<OtherRequirementGroupAdapter.ViewHolder> {

    private List<OtherRequirementGroup> groups;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onEditClick(OtherRequirementGroup group, int position);
    }

    public OtherRequirementGroupAdapter(OnGroupClickListener listener) {
        this.groups = new ArrayList<>();
        this.listener = listener;
    }

    public void setGroups(List<OtherRequirementGroup> groups) {
        this.groups = groups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_other_requirement_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OtherRequirementGroup group = groups.get(position);
        holder.bind(group, position);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupTitle;
        TextView tvRequirementCount;
        MaterialButton btnEdit;
        LinearLayout layoutRequirements;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupTitle = itemView.findViewById(R.id.tvGroupTitle);
            tvRequirementCount = itemView.findViewById(R.id.tvRequirementCount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            layoutRequirements = itemView.findViewById(R.id.layoutRequirements);
        }

        void bind(OtherRequirementGroup group, int position) {
            tvGroupTitle.setText(group.getGroupTitle());
            tvRequirementCount.setText(group.getRequirementCountText());

            // 편집 버튼
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(group, position);
                }
            });

            // 요건 목록 표시
            layoutRequirements.removeAllViews();
            if (group.getRequirements() != null) {
                for (OtherRequirementGroup.RequirementItem item : group.getRequirements()) {
                    View requirementView = createRequirementItemView(item);
                    layoutRequirements.addView(requirementView);
                }
            }
        }

        /**
         * 개별 요건 항목 View 생성
         */
        private View createRequirementItemView(OtherRequirementGroup.RequirementItem item) {
            View view = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_requirement_detail, layoutRequirements, false);

            TextView tvName = view.findViewById(R.id.tvRequirementName);
            TextView tvDescription = view.findViewById(R.id.tvRequirementDescription);

            tvName.setText("• " + item.getName());
            tvDescription.setText(item.getDescription());

            return view;
        }
    }
}
