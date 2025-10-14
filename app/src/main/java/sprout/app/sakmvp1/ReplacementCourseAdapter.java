package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 대체과목 목록을 RecyclerView에 표시하기 위한 어댑터
 */
public class ReplacementCourseAdapter extends RecyclerView.Adapter<ReplacementCourseAdapter.ViewHolder> {

    private List<ReplacementCourse> courses = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(ReplacementCourse course);
    }

    public interface OnEditClickListener {
        void onEditClick(ReplacementCourse course);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(ReplacementCourse course);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.onEditClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setCourses(List<ReplacementCourse> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_replacement_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReplacementCourse course = courses.get(position);
        holder.bind(course);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiscontinuedCourseName;
        TextView tvCredit;
        ChipGroup chipGroupReplacements;
        TextView tvNote;
        TextView tvCreatedDate;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDiscontinuedCourseName = itemView.findViewById(R.id.tv_discontinued_course_name);
            tvCredit = itemView.findViewById(R.id.tv_credit);
            chipGroupReplacements = itemView.findViewById(R.id.chip_group_replacements);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvCreatedDate = itemView.findViewById(R.id.tv_created_date);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(ReplacementCourse course) {
            // 학과가 있으면 학과명과 과목명을 함께 표시
            String displayName = course.getDiscontinuedCourseName();
            if (course.getDepartment() != null && !course.getDepartment().isEmpty()) {
                displayName = "[" + course.getDepartment() + "] " + displayName;
            }
            tvDiscontinuedCourseName.setText(displayName);

            // 학점
            tvCredit.setText(course.getDiscontinuedCourseCredit() + "학점");

            // 대체 과목 목록 (Chip으로 표시)
            chipGroupReplacements.removeAllViews();
            List<String> replacements = course.getReplacementCourseNames();
            if (replacements != null && !replacements.isEmpty()) {
                for (String replacementName : replacements) {
                    Chip chip = new Chip(itemView.getContext());
                    chip.setText(replacementName);
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    chipGroupReplacements.addView(chip);
                }
            } else {
                Chip chip = new Chip(itemView.getContext());
                chip.setText("대체 과목 없음");
                chip.setClickable(false);
                chip.setCheckable(false);
                chipGroupReplacements.addView(chip);
            }

            // 비고 (있는 경우)
            if (course.getNote() != null && !course.getNote().trim().isEmpty()) {
                tvNote.setText("비고: " + course.getNote());
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }

            // 등록일
            String dateStr = dateFormat.format(new Date(course.getCreatedAt()));
            tvCreatedDate.setText("등록: " + dateStr);

            // 클릭 리스너
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(course);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (onEditClickListener != null) {
                    onEditClickListener.onEditClick(course);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClick(course);
                }
            });
        }
    }
}
