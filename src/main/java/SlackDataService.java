import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.slack.api.model.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlackDataService {
    private final String token;
    private final String userId;
    private final String botChannelId = "D07UWMXNNM8"; // ID канала, где бот отправляет сообщения

    public SlackDataService(String token, String userId) {
        this.token = token;
        this.userId = userId;
    }

    public List<Message> getUnreadDirectMessages(double lastCheckTimestamp) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        List<Message> unreadDirectMessages = new ArrayList<>();

        ConversationsListResponse listResponse = slack.methods(token).conversationsList(req -> req
                .types(List.of(ConversationType.IM)));

        if (listResponse.getChannels() != null) {
            for (Conversation dmChannel : listResponse.getChannels()) {
                if (dmChannel.getId().equals(botChannelId)) continue;

                ConversationsHistoryResponse historyResponse = slack.methods(token)
                        .conversationsHistory(req -> req.channel(dmChannel.getId()));

                if (historyResponse.getMessages() != null) {
                    for (Message message : historyResponse.getMessages()) {
                        if (Double.parseDouble(message.getTs()) > lastCheckTimestamp) {
                            unreadDirectMessages.add(message);
                        }
                    }
                }
            }
        }
        return unreadDirectMessages;
    }

    public List<String> getUserChannels() throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        List<String> channelIds = new ArrayList<>();

        ConversationsListResponse response = slack.methods(token).conversationsList(req -> req
                .types(List.of(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL)));

        if (response.getChannels() != null) {
            for (Conversation channel : response.getChannels()) {
                channelIds.add(channel.getId());
            }
        }
        return channelIds;
    }

    public List<Message> getUnreadChannelMessages(String channelId, double lastCheckTimestamp) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        List<Message> unreadMessages = new ArrayList<>();

        ConversationsHistoryResponse response = slack.methods(token).conversationsHistory(req -> req.channel(channelId));

        if (response.getMessages() != null) {
            for (Message message : response.getMessages()) {
                if (Double.parseDouble(message.getTs()) > lastCheckTimestamp) {
                    unreadMessages.add(message);
                }
            }
        }
        return unreadMessages;
    }

    public String getUserId() {
        return userId;
    }
}
