package sprout.app.sakmvp1.timetable;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sprout.app.sakmvp1.R;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final ArrayList<LocalDate> days;
    private final List<CalendarEvent> events;

    // [추가됨] 클릭 리스너 인터페이스 정의
    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }
    private final OnEventClickListener listener;

    // 생성자 수정 (리스너 받도록)
    public CalendarAdapter(ArrayList<LocalDate> days, List<CalendarEvent> events, OnEventClickListener listener) {
        this.days = days;
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        // 높이 조절 (필요 시 사용)
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int) (parent.getHeight() * 0.166666666);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = days.get(position);
        holder.eventLayout.removeAllViews();

        if (date == null) {
            holder.dayText.setText("");
            return;
        }
        holder.dayText.setText(String.valueOf(date.getDayOfMonth()));

        for (CalendarEvent event : events) {
            LocalDate start = event.getStartLocalDate();
            LocalDate end = event.getEndLocalDate();

            boolean isInclude = false;

            if (event.isYearly) {
                // [매년 반복] 연도 무시하고 월/일만 비교
                // 현재 날짜(date)의 연도를 일정의 연도로 바꿔서 비교 (가상의 날짜 생성)
                LocalDate virtualDate = date.withYear(start.getYear());

                // 가상 날짜가 시작일~종료일 사이에 있는지 확인
                if ((virtualDate.isEqual(start) || virtualDate.isAfter(start)) &&
                        (virtualDate.isEqual(end) || virtualDate.isBefore(end))) {
                    isInclude = true;
                }
            } else {
                // [일반] 정확한 날짜 비교
                if ((date.isEqual(start) || date.isAfter(start)) &&
                        (date.isEqual(end) || date.isBefore(end))) {
                    isInclude = true;
                }
            }

            if (isInclude) {
                addEventBar(holder, date, event);
            }
        }
    }

    private void addEventBar(DayViewHolder holder, LocalDate currentDay, CalendarEvent event) {
        TextView bar = new TextView(holder.itemView.getContext());
        LocalDate start = event.getStartLocalDate();
        LocalDate end = event.getEndLocalDate();

        if (currentDay.isEqual(start) || currentDay.getDayOfWeek().getValue() == 7) {
            bar.setText(event.title);
        } else {
            bar.setText(" ");
        }

        bar.setTextSize(10);
        bar.setTextColor(0xFF000000); // Black
        bar.setSingleLine(true);
        bar.setPadding(4, 2, 4, 2);

        int bgResId;
        boolean isStart = currentDay.isEqual(start);
        boolean isEnd = currentDay.isEqual(end);

        if (isStart && isEnd) bgResId = R.drawable.bg_cal_event_single;
        else if (isStart) bgResId = R.drawable.bg_cal_event_start;
        else if (isEnd) bgResId = R.drawable.bg_cal_event_end;
        else bgResId = R.drawable.bg_cal_event_middle;

        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(holder.itemView.getContext(), bgResId).mutate();
        drawable.setColor(event.color);
        bar.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 2, 0, 2);
        bar.setLayoutParams(params);

        // [추가됨] 막대바 클릭 시 리스너 호출
        bar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });

        holder.eventLayout.addView(bar);
    }

    @Override
    public int getItemCount() { return days.size(); }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;
        LinearLayout eventLayout;
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.itemDayText);
            eventLayout = itemView.findViewById(R.id.itemEventLayout);
        }
    }
}