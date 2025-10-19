package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import sprout.app.sakmvp1.models.Student;

/**
 * 학생 목록 RecyclerView Adapter
 */
public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private List<Student> students = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Student student);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvEmail, tvStudentYear, tvDepartment, tvTrack, tvLastCheckDate;
        private android.widget.ImageView ivCheckStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvEmail = itemView.findViewById(R.id.tv_student_email);
            tvStudentYear = itemView.findViewById(R.id.tv_student_year);
            tvDepartment = itemView.findViewById(R.id.tv_department);
            tvTrack = itemView.findViewById(R.id.tv_track);
            tvLastCheckDate = itemView.findViewById(R.id.tv_last_check_date);
            ivCheckStatus = itemView.findViewById(R.id.iv_check_status);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(students.get(position));
                }
            });
        }

        public void bind(Student student) {
            tvName.setText(student.getName());
            tvEmail.setText(student.getEmail() != null ? student.getEmail() : "이메일 없음");
            tvStudentYear.setText(student.getDisplayYear() + "학번");
            tvDepartment.setText(student.getDepartment() != null ? student.getDepartment() : "-");
            tvTrack.setText(student.getTrack() != null ? student.getTrack() : "-");

            // 졸업요건 검사 이력 표시
            if (student.getHasGraduationCheckHistory()) {
                tvLastCheckDate.setText(student.getFormattedLastCheckDate());
                ivCheckStatus.setImageResource(android.R.drawable.presence_online);
                ivCheckStatus.setColorFilter(0xFF4CAF50); // 녹색
            } else {
                tvLastCheckDate.setText("검사 이력 없음");
                ivCheckStatus.setImageResource(android.R.drawable.presence_offline);
                ivCheckStatus.setColorFilter(0xFF9E9E9E); // 회색
            }
        }
    }
}
