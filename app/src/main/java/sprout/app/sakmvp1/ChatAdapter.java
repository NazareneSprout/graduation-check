package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * 챗봇 메시지 RecyclerView 어댑터
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messages;
    private OnActionClickListener actionListener;

    public interface OnActionClickListener {
        void onActionClick(ChatMessage message);
    }

    public ChatAdapter(List<ChatMessage> messages, OnActionClickListener actionListener) {
        this.messages = messages;
        this.actionListener = actionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_BOT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_bot, parent, false);
            return new BotViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof BotViewHolder) {
            BotViewHolder botHolder = (BotViewHolder) holder;
            botHolder.tvMessage.setText(message.getMessage());

            if (message.hasAction()) {
                botHolder.btnAction.setVisibility(View.VISIBLE);
                botHolder.btnAction.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onActionClick(message);
                    }
                });
            } else {
                botHolder.btnAction.setVisibility(View.GONE);
            }
        } else if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvMessage.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        MaterialButton btnAction;

        BotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }
    }
}
