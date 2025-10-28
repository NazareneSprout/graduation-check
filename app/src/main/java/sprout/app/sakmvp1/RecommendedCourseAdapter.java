package sprout.app.sakmvp1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        String category = course.getCategory();
        holder.chipCategory.setText(RecommendedCourse.getCategoryDisplayName(category));

        // 카테고리별 색상 적용
        int color = RecommendationResultActivity.getCategoryColor(category);
        holder.chipCategory.setChipBackgroundColor(ColorStateList.valueOf(color));
        // 텍스트 색상은 배경색의 밝기에 따라 자동 결정
        holder.chipCategory.setTextColor(getContrastColor(color));

        // 학점
        holder.tvCredits.setText(course.getCredits() + "학점");

        // 대체 가능 과목이 있는 경우 아코디언 표시
        if (course.hasAlternatives()) {
            holder.layoutAccordionHeader.setVisibility(View.VISIBLE);

            // 아코디언 레이블 업데이트
            int alternativesCount = course.getAlternativeCourses().size();
            holder.tvAlternativesLabel.setText("📋 대체 가능 과목 (" + alternativesCount + "개)");

            // 아코디언 초기 상태는 닫힌 상태
            holder.layoutAlternativesContent.setVisibility(View.GONE);
            holder.ivExpandIcon.setRotation(0);

            // 아코디언 헤더 클릭 이벤트
            holder.layoutAccordionHeader.setOnClickListener(v -> {
                boolean isExpanded = holder.layoutAlternativesContent.getVisibility() == View.VISIBLE;

                if (isExpanded) {
                    // 닫기
                    holder.layoutAlternativesContent.setVisibility(View.GONE);
                    holder.ivExpandIcon.animate().rotation(0).setDuration(200).start();
                } else {
                    // 열기
                    holder.layoutAlternativesContent.setVisibility(View.VISIBLE);
                    holder.ivExpandIcon.animate().rotation(180).setDuration(200).start();

                    // 대체 과목 리스트 생성
                    populateAlternatives(holder.layoutAlternativesContent, course.getAlternativeCourses());
                }
            });
        } else {
            // 대체 과목이 없으면 아코디언 숨김
            holder.layoutAccordionHeader.setVisibility(View.GONE);
            holder.layoutAlternativesContent.setVisibility(View.GONE);
        }
    }

    /**
     * 대체 가능한 과목 리스트를 동적으로 추가
     */
    private void populateAlternatives(LinearLayout container, List<String> alternatives) {
        // 기존 내용 제거
        container.removeAllViews();

        if (alternatives == null || alternatives.isEmpty()) {
            return;
        }

        // 각 대체 과목에 대해 TextView 생성
        for (String alternativeCourse : alternatives) {
            TextView tvAlternative = new TextView(container.getContext());
            tvAlternative.setText("  • " + alternativeCourse);
            tvAlternative.setTextSize(13);
            tvAlternative.setTextColor(container.getContext().getResources().getColor(android.R.color.black, null));
            tvAlternative.setPadding(8, 4, 8, 4);

            container.addView(tvAlternative);
        }
    }

    /**
     * 배경색에 대비되는 텍스트 색상 반환 (밝기 기반)
     */
    private int getContrastColor(int color) {
        // 색상의 밝기 계산
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.5 ? Color.BLACK : Color.WHITE;
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
        LinearLayout layoutAccordionHeader;
        TextView tvAlternativesLabel;
        android.widget.ImageView ivExpandIcon;
        LinearLayout layoutAlternativesContent;

        ViewHolder(View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            chipCategory = itemView.findViewById(R.id.chip_category);
            tvCredits = itemView.findViewById(R.id.tv_credits);
            layoutAccordionHeader = itemView.findViewById(R.id.layout_accordion_header);
            tvAlternativesLabel = itemView.findViewById(R.id.tv_alternatives_label);
            ivExpandIcon = itemView.findViewById(R.id.iv_expand_icon);
            layoutAlternativesContent = itemView.findViewById(R.id.layout_alternatives_content);
        }
    }
}
