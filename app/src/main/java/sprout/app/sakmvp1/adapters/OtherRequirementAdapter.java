package sprout.app.sakmvp1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.models.OtherRequirement;

/**
 * 기타 졸업요건 RecyclerView 어댑터
 */
public class OtherRequirementAdapter extends RecyclerView.Adapter<OtherRequirementAdapter.ViewHolder> {

    private List<OtherRequirement> requirements;
    private OnRequirementClickListener listener;

    public interface OnRequirementClickListener {
        void onDeleteClick(OtherRequirement requirement, int position);
    }

    public OtherRequirementAdapter(OnRequirementClickListener listener) {
        this.requirements = new ArrayList<>();
        this.listener = listener;
    }

    public void setRequirements(List<OtherRequirement> requirements) {
        this.requirements = requirements;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < requirements.size()) {
            requirements.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_other_requirement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OtherRequirement requirement = requirements.get(position);
        holder.bind(requirement, position);
    }

    @Override
    public int getItemCount() {
        return requirements.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequirementName;
        TextView tvRequirementDescription;
        MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequirementName = itemView.findViewById(R.id.tvRequirementName);
            tvRequirementDescription = itemView.findViewById(R.id.tvRequirementDescription);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(OtherRequirement requirement, int position) {
            tvRequirementName.setText(requirement.getName());
            tvRequirementDescription.setText(requirement.getDescription());

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(requirement, position);
                }
            });
        }
    }
}
