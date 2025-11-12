package sprout.app.sakmvp1.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import sprout.app.sakmvp1.R;

public class GroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 3가지 뷰 타입 정의
    private static final int TYPE_MY_CALENDAR = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_GROUP = 2;

    private List<Object> items;
    private String currentCalendarId; // 현재 *선택된* 캘린더 ID
    private String myUserId; // *사용자 본인*의 ID (생성자로 받음)
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String calendarId);
    }

    // [수정] 생성자에 myUserId 매개변수 추가
    public GroupAdapter(List<Object> items, String currentCalendarId, String myUserId, OnItemClickListener listener) {
        this.items = items;
        this.currentCalendarId = currentCalendarId;
        this.myUserId = myUserId; // 생성자로 받은 내 ID 저장
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item.equals("MY_CALENDAR")) return TYPE_MY_CALENDAR;
        if (item.equals("SEPARATOR")) return TYPE_SEPARATOR;
        if (item instanceof GroupModel) return TYPE_GROUP;

        // [수정] 알 수 없는 타입이 들어오면 -1 대신 예외를 던짐
        throw new IllegalArgumentException("Invalid object type at position " + position + ": " + item.toString());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_MY_CALENDAR) {
            View view = inflater.inflate(R.layout.item_calendar_header, parent, false);
            return new MyCalendarViewHolder(view);
        }
        if (viewType == TYPE_SEPARATOR) {
            View view = inflater.inflate(R.layout.item_separator, parent, false);
            return new SeparatorViewHolder(view);
        }
        if (viewType == TYPE_GROUP) {
            View view = inflater.inflate(R.layout.item_group_card, parent, false);
            return new GroupViewHolder(view);
        }
        // getItemViewType에서 예외가 발생하므로 이 라인은 실행되지 않음
        throw new IllegalArgumentException("Invalid view type: " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_MY_CALENDAR) {
            MyCalendarViewHolder vh = (MyCalendarViewHolder) holder;

            // [수정] items.get(1) 대신 생성자에서 받은 myUserId 사용
            vh.checkBox.setChecked(currentCalendarId.equals(myUserId));
            vh.itemView.setOnClickListener(v -> listener.onItemClick(myUserId));

        } else if (viewType == TYPE_GROUP) {
            GroupViewHolder vh = (GroupViewHolder) holder;
            GroupModel group = (GroupModel) items.get(position);

            vh.tvTitle.setText(group.getGroupName());
            int count = group.getMembers() != null ? group.getMembers().size() : 0;
            vh.tvDesc.setText(group.getDescription() + " · " + count + "명");
            vh.checkBox.setChecked(currentCalendarId.equals(group.getDocumentId()));
            vh.itemView.setOnClickListener(v -> listener.onItemClick(group.getDocumentId()));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- ViewHolders (변경 없음) ---
    public static class MyCalendarViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        public MyCalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.img_check);
        }
    }
    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        public SeparatorViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        CheckBox checkBox;
        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_group_title);
            tvDesc = itemView.findViewById(R.id.text_group_desc);
            checkBox = itemView.findViewById(R.id.img_check);
        }
    }
}