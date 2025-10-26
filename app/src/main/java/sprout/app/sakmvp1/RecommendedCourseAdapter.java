package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

/**
 * 추천 과목 RecyclerView Adapter
 */
public class RecommendedCourseAdapter extends RecyclerView.Adapter<RecommendedCourseAdapter.ViewHolder> {

    private List<RecommendedCourse> courseList;

    public RecommendedCourseAdapter(List<RecommendedCourse> courseList) {
        this.courseList = courseList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommended_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendedCourse course = courseList.get(position);

        // 과목명
        holder.tvCourseName.setText(course.getCourseName());

        // 카테고리
        holder.chipCategory.setText(RecommendedCourse.getCategoryDisplayName(course.getCategory()));

        // 학점
        holder.tvCredits.setText(course.getCredits() + "학점");

        // 우선순위 레벨 표시
        String priorityText = getPriorityLabel(course.getPriority());
        holder.tvPriority.setText(priorityText);

        // 추천 이유
        if (course.getReason() != null && !course.getReason().isEmpty()) {
            holder.tvReason.setText(course.getReason());
            holder.tvReason.setVisibility(View.VISIBLE);
        } else {
            holder.tvReason.setVisibility(View.GONE);
        }
    }

    /**
     * 우선순위 숫자를 라벨로 변환
     */
    private String getPriorityLabel(int priority) {
        if (priority <= 10) {
            return "🔴 긴급";
        } else if (priority <= 20) {
            return "🟠 높음";
        } else if (priority <= 30) {
            return "🟡 보통";
        } else {
            return "🟢 낮음";
        }
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    /**
     * 데이터 업데이트
     */
    public void updateData(List<RecommendedCourse> newCourseList) {
        this.courseList = newCourseList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName;
        Chip chipCategory;
        TextView tvCredits;
        TextView tvPriority;
        TextView tvReason;

        ViewHolder(View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            chipCategory = itemView.findViewById(R.id.chip_category);
            tvCredits = itemView.findViewById(R.id.tv_credits);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            tvReason = itemView.findViewById(R.id.tv_reason);
        }
    }
}
