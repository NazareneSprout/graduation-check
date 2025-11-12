package sprout.app.sakmvp1.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView; // [추가]
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import sprout.app.sakmvp1.R;

public class GroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 뷰 타입
    private static final int TYPE_MY_CALENDAR = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_GROUP = 2;

    // [추가] 현재 어댑터 모드
    public static final int MODE_NORMAL = 0; // 일반 (캘린더 선택)
    public static final int MODE_EDIT = 1;   // 수정
    public static final int MODE_DELETE = 2; // 삭제

    private int currentMode = MODE_NORMAL; // 기본값은 일반 모드

    private List<Object> items;
    private String currentCalendarId;
    private String myUserId;
    private OnItemClickListener listener;

    // [수정] 클릭 리스너 인터페이스 (3가지 클릭 이벤트)
    public interface OnItemClickListener {
        void onCalendarClick(String calendarId);
        void onEditClick(GroupModel group);
        void onDeleteClick(GroupModel group);
    }

    public GroupAdapter(List<Object> items, String currentCalendarId, String myUserId, OnItemClickListener listener) {
        this.items = items;
        this.currentCalendarId = currentCalendarId;
        this.myUserId = myUserId;
        this.listener = listener;
    }

    // [추가] 액티비티에서 모드를 변경할 메서드
    public void setMode(int mode) {
        this.currentMode = mode;
        notifyDataSetChanged(); // 모드가 바뀌면 리스트 전체 갱신
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item.equals("MY_CALENDAR")) return TYPE_MY_CALENDAR;
        if (item.equals("SEPARATOR")) return TYPE_SEPARATOR;
        if (item instanceof GroupModel) return TYPE_GROUP;

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
        throw new IllegalArgumentException("Invalid view type: " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_MY_CALENDAR) {
            MyCalendarViewHolder vh = (MyCalendarViewHolder) holder;

            // "내 캘린더"는 수정/삭제 모드일 때 체크박스를 숨김
            vh.checkBox.setVisibility(currentMode == MODE_NORMAL ? View.VISIBLE : View.GONE);
            vh.checkBox.setChecked(currentCalendarId.equals(myUserId));

            // "내 캘린더"는 일반 모드에서만 클릭 가능
            if (currentMode == MODE_NORMAL) {
                vh.itemView.setOnClickListener(v -> listener.onCalendarClick(myUserId));
            } else {
                vh.itemView.setOnClickListener(null); // 수정/삭제 모드에선 클릭 방지
            }

        } else if (viewType == TYPE_GROUP) {
            GroupViewHolder vh = (GroupViewHolder) holder;
            GroupModel group = (GroupModel) items.get(position);

            vh.tvTitle.setText(group.getGroupName());
            int count = group.getMembers() != null ? group.getMembers().size() : 0;
            vh.tvDesc.setText(group.getDescription() + " · " + count + "명");

            // [수정] 모드에 따라 아이콘 표시/숨김
            vh.checkBox.setVisibility(currentMode == MODE_NORMAL ? View.VISIBLE : View.GONE);
            vh.editIcon.setVisibility(currentMode == MODE_EDIT ? View.VISIBLE : View.GONE);
            vh.deleteIcon.setVisibility(currentMode == MODE_DELETE ? View.VISIBLE : View.GONE);

            vh.checkBox.setChecked(currentCalendarId.equals(group.getDocumentId()));

            // [수정] 모드에 따라 클릭 이벤트 변경
            if (currentMode == MODE_EDIT) {
                vh.itemView.setOnClickListener(v -> listener.onEditClick(group));
            } else if (currentMode == MODE_DELETE) {
                vh.itemView.setOnClickListener(v -> listener.onDeleteClick(group));
            } else { // MODE_NORMAL
                vh.itemView.setOnClickListener(v -> listener.onCalendarClick(group.getDocumentId()));
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // [삭제] getItemAt() 메서드 삭제 (스와이프에서만 필요했음)

    // --- ViewHolders ---
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

    // [수정] 뷰홀더에 수정/삭제 아이콘 추가
    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        CheckBox checkBox;
        ImageView editIcon, deleteIcon; // [추가]

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_group_title);
            tvDesc = itemView.findViewById(R.id.text_group_desc);
            checkBox = itemView.findViewById(R.id.img_check);
            editIcon = itemView.findViewById(R.id.icon_edit_mode); // [추가]
            deleteIcon = itemView.findViewById(R.id.icon_delete_mode); // [추가]
        }
    }
}