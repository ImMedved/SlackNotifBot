import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.slack.api.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlackDataService {
    private final String token;
    private final String userId;

    public SlackDataService(String token, String userId) {
        this.token = token;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getUserChannels() throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        List<String> channelIds = new ArrayList<>();

        ConversationsListResponse response = slack.methods(token).conversationsList(ConversationsListRequest.builder()
                .types(List.of(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL))
                .build());

        if (response != null && response.getChannels() != null) {
            for (Conversation channel : response.getChannels()) {
                channelIds.add(channel.getId());
            }
        }
        System.out.println("channelIds are: " + channelIds);
        return channelIds;
    }

    public List<Message> getUnreadChannelMessages(String channelId, String userId, double lastCheckTimestamp) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        ConversationsHistoryResponse response = slack.methods(token).conversationsHistory(ConversationsHistoryRequest.builder()
                .channel(channelId)
                .build());

        List<Message> unreadMessages = new ArrayList<>();

        if (response != null && response.getMessages() != null) {
            for (Message message : response.getMessages()) {
                double messageTimestamp = Double.parseDouble(message.getTs());
                if (messageTimestamp > lastCheckTimestamp) {
                    unreadMessages.add(message);
                }
            }
        } else {
            System.err.println("Failed to retrieve messages or no messages found in channel: " + channelId);
        }
        System.out.println("unreadMessages are: " + unreadMessages);
        return unreadMessages;
    }

    public void sendMessage(String channelId, String text) {
        Slack slack = Slack.getInstance();
        try {
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(ChatPostMessageRequest.builder()
                    .channel(channelId)
                    .text(text)
                    .build());

            if (!response.isOk()) {
                System.err.println("Error sending message: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            System.err.println("Exception occurred while sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
