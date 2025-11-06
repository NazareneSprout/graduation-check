package sprout.app.sakmvp1;

/**
 * 채팅 메시지 모델
 */
public class ChatMessage {
    public static final int TYPE_BOT = 0;
    public static final int TYPE_USER = 1;

    private String message;
    private int type; // 0: 봇, 1: 사용자
    private long timestamp;
    private String actionType; // "navigate", "info", null
    private String actionData; // Activity 이름 또는 추가 데이터

    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String message, int type, String actionType, String actionData) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.actionType = actionType;
        this.actionData = actionData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionData() {
        return actionData;
    }

    public void setActionData(String actionData) {
        this.actionData = actionData;
    }

    public boolean isBot() {
        return type == TYPE_BOT;
    }

    public boolean hasAction() {
        return actionType != null && !actionType.isEmpty();
    }
}
