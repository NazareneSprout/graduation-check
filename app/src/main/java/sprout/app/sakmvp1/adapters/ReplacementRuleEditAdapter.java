package sprout.app.sakmvp1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.models.ReplacementRule;
import java.util.ArrayList;
import java.util.List;

/**
 * 대체과목 규칙 편집용 RecyclerView Adapter
 */
public class ReplacementRuleEditAdapter extends RecyclerView.Adapter<ReplacementRuleEditAdapter.ViewHolder> {

    private static final String TAG = "ReplaceRuleEditAdapter";
    private List<ReplacementRule> rules;
    private OnRuleActionListener listener;

    public interface OnRuleActionListener {
        void onDeleteRule(ReplacementRule rule, int position);
    }

    public ReplacementRuleEditAdapter() {
        this.rules = new ArrayList<>();
    }

    public void setRules(List<ReplacementRule> rules) {
        android.util.Log.d(TAG, "setRules 호출 - 규칙 개수: " + (rules != null ? rules.size() : "null"));
        this.rules = rules != null ? rules : new ArrayList<>();
        notifyDataSetChanged();
        android.util.Log.d(TAG, "notifyDataSetChanged 완료 - getItemCount(): " + getItemCount());
    }

    public void setOnRuleActionListener(OnRuleActionListener listener) {
        this.listener = listener;
    }

    public void addRule(ReplacementRule rule) {
        rules.add(rule);
        notifyItemInserted(rules.size() - 1);
    }

    public void removeRule(int position) {
        if (position >= 0 && position < rules.size()) {
            rules.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<ReplacementRule> getRules() {
        return new ArrayList<>(rules);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_replacement_rule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        android.util.Log.d(TAG, "onBindViewHolder 호출 - position: " + position);
        ReplacementRule rule = rules.get(position);

        // 폐지된 과목 표시
        ReplacementRule.CourseInfo discontinuedCourse = rule.getDiscontinuedCourse();
        if (discontinuedCourse != null) {
            String discontinuedText = discontinuedCourse.getName() +
                " (" + discontinuedCourse.getCredits() + "학점)";
            holder.tvDiscontinuedCourse.setText(discontinuedText);
            android.util.Log.d(TAG, "  폐지과목: " + discontinuedText);
        }

        // 대체 과목들 표시
        List<ReplacementRule.CourseInfo> replacementCourses = rule.getReplacementCourses();
        if (replacementCourses != null && !replacementCourses.isEmpty()) {
            StringBuilder sb = new StringBuilder("→ ");
            for (int i = 0; i < replacementCourses.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(replacementCourses.get(i).getName());
            }
            holder.tvReplacementCourses.setText(sb.toString());
            android.util.Log.d(TAG, "  대체과목: " + sb.toString());
        } else {
            holder.tvReplacementCourses.setText("→ (대체과목 없음)");
            android.util.Log.d(TAG, "  대체과목: 없음");
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteRule(rule, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiscontinuedCourse;
        TextView tvReplacementCourses;
        MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvDiscontinuedCourse = itemView.findViewById(R.id.tv_discontinued_course);
            tvReplacementCourses = itemView.findViewById(R.id.tv_replacement_courses);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
