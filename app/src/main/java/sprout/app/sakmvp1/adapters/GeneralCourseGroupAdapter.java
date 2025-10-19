package sprout.app.sakmvp1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.models.GeneralCourseGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * 교양 과목 그룹 어댑터
 * - 단일 과목과 선택 과목 그룹을 구분하여 표시
 */
public class GeneralCourseGroupAdapter extends RecyclerView.Adapter<GeneralCourseGroupAdapter.ViewHolder> {

    private List<GeneralCourseGroup> groups;
    private OnGroupActionListener listener;

    public interface OnGroupActionListener {
        void onDeleteGroup(GeneralCourseGroup group, int position);
    }

    public GeneralCourseGroupAdapter() {
        this.groups = new ArrayList<>();
    }

    public void setGroups(List<GeneralCourseGroup> groups) {
        this.groups = groups != null ? groups : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnGroupActionListener(OnGroupActionListener listener) {
        this.listener = listener;
    }

    public void addGroup(GeneralCourseGroup group) {
        groups.add(group);
        notifyItemInserted(groups.size() - 1);
    }

    public void removeGroup(int position) {
        if (position >= 0 && position < groups.size()) {
            groups.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearGroups() {
        groups.clear();
        notifyDataSetChanged();
    }

    public List<GeneralCourseGroup> getGroups() {
        return new ArrayList<>(groups);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_general_course_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GeneralCourseGroup group = groups.get(position);

        // 타입 뱃지 설정
        holder.tvTypeBadge.setText(group.getTypeLabel());
        if (group.isSingle()) {
            holder.tvTypeBadge.setBackgroundResource(R.drawable.badge_single);
        } else {
            holder.tvTypeBadge.setBackgroundResource(R.drawable.badge_option);
        }

        // 학점 표시
        holder.tvCredit.setText(group.getCredit() + "학점");

        // 과목 리스트 표시
        holder.llCoursesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (int i = 0; i < group.getCourseNames().size(); i++) {
            String courseName = group.getCourseNames().get(i);
            TextView courseView = new TextView(holder.itemView.getContext());
            courseView.setText((group.isOptionGroup() ? "• " : "") + courseName);
            courseView.setTextSize(14);
            courseView.setTextColor(holder.itemView.getContext().getColor(android.R.color.black));

            // 단일 과목이면 볼드체, 선택 과목은 일반체
            if (group.isSingle()) {
                courseView.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            // 선택 과목 그룹은 약간의 왼쪽 여백 추가
            if (group.isOptionGroup()) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(16, 4, 0, 4);
                courseView.setLayoutParams(params);
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 4, 0, 4);
                courseView.setLayoutParams(params);
            }

            holder.llCoursesContainer.addView(courseView);
        }

        // 삭제 버튼
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteGroup(group, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTypeBadge;
        TextView tvCredit;
        LinearLayout llCoursesContainer;
        MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTypeBadge = itemView.findViewById(R.id.tv_type_badge);
            tvCredit = itemView.findViewById(R.id.tv_credit);
            llCoursesContainer = itemView.findViewById(R.id.ll_courses_container);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
