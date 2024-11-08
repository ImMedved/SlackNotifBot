import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.conversations.ConversationsJoinRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.response.conversations.ConversationsJoinResponse;
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

    public List<SimpleMessage> getUnreadDirectMessages(double lastCheckTimestamp) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        List<SimpleMessage> unreadDirectMessages = new ArrayList<>();

        ConversationsListResponse listResponse = slack.methods(token).conversationsList(req -> req
                .types(List.of(ConversationType.IM)));
        System.out.println("ConversationsHistoryResponse response in getUnreadDirectMessages is: " + listResponse);
        if (listResponse.getChannels() != null) {
            for (Conversation dmChannel : listResponse.getChannels()) {
                if (!dmChannel.getId().equals(botChannelId)) { // Убедитесь, что это не канал с ботом
                    ConversationsHistoryResponse historyResponse = slack.methods(token)
                            .conversationsHistory(req -> req.channel(dmChannel.getId()));

                    if (historyResponse.getMessages() != null) {
                        for (Message message : historyResponse.getMessages()) {
                            if (Double.parseDouble(message.getTs()) > lastCheckTimestamp) {
                                unreadDirectMessages.add(new SimpleMessage(message.getText(), dmChannel.getId(), message.getUser()));
                            }
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
            //for (Conversation channel : response.getChannels()) {
            //    channelIds.add(channel.getId());
            //}
            channelIds.add("C07UZVBBG68");
            addBotToChannel("C07UZVBBG68");
            //Todo:
            //  Не работает проверка на нахождение пользователя в канале,
            //  несмотря на то, что пользователь и так есть в канале, бот его продолжает добавлять.
            /*for (Conversation channel : response.getChannels()) {
                if (!channel.isMember()) {
                    addBotToChannel(channel.getId());
                }
            }*/
        }
        return channelIds;
    }

    public List<SimpleMessage> getUnreadChannelMessages(String channelId, double lastCheckTimestamp) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        List<SimpleMessage> unreadMessages = new ArrayList<>();

        ConversationsHistoryResponse response = slack.methods(token).conversationsHistory(req -> req.channel(channelId));
        System.out.println("ConversationsHistoryResponse response in getUnreadChannelMessages is: " + response);
        if (response.getMessages() != null) {
            for (Message message : response.getMessages()) {
                System.out.println("Messages in getUnreadChannelMessages method are: " + message);
                if (Double.parseDouble(message.getTs()) > lastCheckTimestamp) {
                    unreadMessages.add(new SimpleMessage(message.getText(), channelId, message.getUser()));
                }
            }
        }
        return unreadMessages;
    }


    public String getUserId() {
        return userId;
    }

    private void addBotToChannel(String channelId) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        ConversationsJoinResponse joinResponse = slack.methods(token).conversationsJoin(
                ConversationsJoinRequest.builder()
                        .token(token)
                        .channel(channelId)
                        .build()
        );

        if (!joinResponse.isOk()) {
            System.err.println("Failed to join channel " + channelId + ": " + joinResponse.getError());
        } else {
            System.out.println("Bot added to channel " + channelId);
        }
    }
}
