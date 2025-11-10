package sprout.app.sakmvp1.timetable;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sprout.app.sakmvp1.R;

/**
 * 저장된 시간표 목록을 표시하는 RecyclerView Adapter
 */
public class SavedTimetableAdapter extends RecyclerView.Adapter<SavedTimetableAdapter.ViewHolder> {

    private Context context;
    private List<SavedTimetable> timetableList;
    private OnTimetableActionListener listener;
    private SimpleDateFormat dateFormat;
    private String activeTimetableId;

    public interface OnTimetableActionListener {
        void onActivateTimetable(SavedTimetable timetable, int position);
        void onDeleteTimetable(SavedTimetable timetable, int position);
        void onEditTimetable(SavedTimetable timetable, int position);
    }

    public SavedTimetableAdapter(Context context, List<SavedTimetable> timetableList, OnTimetableActionListener listener) {
        this.context = context;
        this.timetableList = timetableList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
    }

    public void setActiveTimetableId(String activeTimetableId) {
        this.activeTimetableId = activeTimetableId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_timetable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedTimetable timetable = timetableList.get(position);

        // 시간표 이름
        holder.tvTimetableName.setText(timetable.getName());

        // 저장 날짜
        String savedDateText = "저장일 없음";
        if (timetable.getSavedDate() > 0) {
            Date date = new Date(timetable.getSavedDate());
            savedDateText = dateFormat.format(date) + " 저장";
        }
        holder.tvSavedDate.setText(savedDateText);

        // [수정됨] 수업 개수 (getCourseCount() 대신 schedules.size() 사용)
        int count = 0;
        if (timetable.getSchedules() != null) {
            count = timetable.getSchedules().size();
        }
        String courseCountText = "총 " + count + "개 수업";
        holder.tvCourseCount.setText(courseCountText);
        // [수정 완료]

        // 활성화 상태 표시
        // [수정됨] timetable.getId()가 null일 경우를 대비
        boolean isActive = timetable.getId() != null && timetable.getId().equals(activeTimetableId);
        if (isActive) {
            holder.layoutActiveBadge.setVisibility(View.VISIBLE);
            holder.btnActivate.setEnabled(false);
            holder.btnActivate.setText("활성화됨");
        } else {
            holder.layoutActiveBadge.setVisibility(View.GONE);
            holder.btnActivate.setEnabled(true);
            holder.btnActivate.setText("활성화");
        }

        // 활성화 버튼
        holder.btnActivate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivateTimetable(timetable, holder.getAdapterPosition());
            }
        });

        // 삭제 버튼
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteTimetable(timetable, holder.getAdapterPosition());
            }
        });

        // 수정 버튼
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditTimetable(timetable, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return timetableList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimetableName;
        TextView tvSavedDate;
        TextView tvCourseCount;
        LinearLayout layoutActiveBadge;
        MaterialButton btnActivate;
        MaterialButton btnEdit;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimetableName = itemView.findViewById(R.id.tv_timetable_name);
            tvSavedDate = itemView.findViewById(R.id.tv_saved_date);
            tvCourseCount = itemView.findViewById(R.id.tv_course_count);
            layoutActiveBadge = itemView.findViewById(R.id.layout_active_badge);
            btnActivate = itemView.findViewById(R.id.btn_activate);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
