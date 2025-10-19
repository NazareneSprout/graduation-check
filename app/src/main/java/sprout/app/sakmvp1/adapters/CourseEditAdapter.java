package sprout.app.sakmvp1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.models.CourseRequirement;
import java.util.ArrayList;
import java.util.List;

/**
 * 과목 편집용 RecyclerView Adapter
 * 과목 목록을 표시하고 삭제 기능 제공
 */
public class CourseEditAdapter extends RecyclerView.Adapter<CourseEditAdapter.ViewHolder> {

    private List<CourseRequirement> courses;
    private OnCourseActionListener listener;
    private OnDataChangedListener dataChangedListener;

    public interface OnCourseActionListener {
        void onDeleteCourse(CourseRequirement course, int position);
    }

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public CourseEditAdapter() {
        this.courses = new ArrayList<>();
    }

    public void setCourses(List<CourseRequirement> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnCourseActionListener(OnCourseActionListener listener) {
        this.listener = listener;
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.dataChangedListener = listener;
    }

    public void addCourse(CourseRequirement course) {
        courses.add(course);
        notifyItemInserted(courses.size() - 1);
        // 데이터 변경 알림
        if (dataChangedListener != null) {
            dataChangedListener.onDataChanged();
        }
    }

    public void removeCourse(int position) {
        if (position >= 0 && position < courses.size()) {
            courses.remove(position);
            notifyItemRemoved(position);
            // 데이터 변경 알림
            if (dataChangedListener != null) {
                dataChangedListener.onDataChanged();
            }
        }
    }

    public void clearCourses() {
        courses.clear();
        notifyDataSetChanged();
    }

    public List<CourseRequirement> getCourses() {
        return new ArrayList<>(courses);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseRequirement course = courses.get(position);

        holder.tvCourseName.setText(course.getName());
        holder.tvCourseCredit.setText(course.getCredits() + "학점");

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteCourse(course, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName;
        TextView tvCourseCredit;
        MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            tvCourseCredit = itemView.findViewById(R.id.tv_course_credit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
