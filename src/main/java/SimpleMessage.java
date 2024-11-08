public class SimpleMessage {
    private final String text;
    private final String channelId;
    private final String userId; // Добавляем поле userId

    public SimpleMessage(String text, String channelId, String userId) {
        this.text = text;
        this.channelId = channelId;
        this.userId = userId;
    }

    public String getText() { return text; }
    public String getChannelId() { return channelId; }
    public String getUserId() { return userId; }

    @Override
    public String toString() {
        return "SimpleMessage{" +
                "text='" + text + '\'' +
                ", channelId='" + channelId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
