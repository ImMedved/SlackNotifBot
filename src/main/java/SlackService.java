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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SlackService {
    private final String token;
    private final String userId;
    private final UserSettingsDAO userSettingsDAO;
    private List<String> keywords;
    private List<String> importantUsers;
    private Timestamp lastCheckTime;

    public SlackService(String token, String userId, UserSettingsDAO userSettingsDAO) throws SQLException {
        this.token = token;
        this.userId = userId;
        this.userSettingsDAO = userSettingsDAO;
        loadUserSettings();
    }

    private void loadUserSettings() throws SQLException {
        this.keywords = userSettingsDAO.getUserKeywords(userId);
        this.importantUsers = userSettingsDAO.getImportantUsers(userId);
        this.lastCheckTime = userSettingsDAO.getLastCheckTime(userId);
    }

    // Получение списка всех каналов, в которых состоит пользователь
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

        return channelIds;
    }

    public List<Message> getUnreadChannelMessages(String channelId, String userId) throws IOException, SlackApiException, SQLException {
        Slack slack = Slack.getInstance();
        ConversationsHistoryResponse response = slack.methods(token).conversationsHistory(ConversationsHistoryRequest.builder()
                .channel(channelId)
                .build());

        List<Message> unreadMessages = new ArrayList<>();

        // Проверяем, не является ли response или его список сообщений null
        if (response != null && response.getMessages() != null) {
            for (Message message : response.getMessages()) {
                // Преобразуем timestamp сообщения и сравниваем с lastCheckTime
                double messageTimestamp = Double.parseDouble(message.getTs());
                if (lastCheckTime == null || messageTimestamp > lastCheckTime.getTime() / 1000.0) {
                    if (containsMention(message, userId) || containsKeywords(message)) {
                        unreadMessages.add(message);
                    }
                }
            }
        } else {
            System.err.println("Failed to retrieve messages or no messages found in channel: " + channelId);
        }

        return unreadMessages;
    }

    private boolean containsMention(Message message, String userId) {
        String text = message.getText();
        return text != null && (text.contains("<@" + userId + ">") || text.contains("@here") || text.contains("@channel"));
    }


    // Метод для генерации отчета, включая просмотр всех каналов
    public String generateDailySummary() throws IOException, SlackApiException, SQLException {
        List<Message> allMessages = new ArrayList<>();

        // Получаем все каналы, в которых пользователь состоит
        List<String> userChannels = getUserChannels();

        // Получаем сообщения из каждого канала
        for (String channelId : userChannels) {
            allMessages.addAll(getUnreadChannelMessages(channelId, userId));  // Добавляем userId в вызов
        }

        List<Message> importantDirectMessages = new ArrayList<>();
        List<Message> importantChannelMentions = new ArrayList<>();
        List<Message> importantThreadMentions = new ArrayList<>();
        List<Message> frequentThreadMentions = new ArrayList<>();
        List<Message> otherDirectMessages = new ArrayList<>();
        List<Message> keywordChannelMessages = new ArrayList<>();
        List<Message> keywordThreadMessages = new ArrayList<>();

        // Классифицируем сообщения по категориям
        for (Message msg : allMessages) {
            boolean isFromImportantUser = importantUsers.contains(msg.getUser());
            boolean containsKeywords = containsKeywords(msg);

            if (isFromImportantUser) {
                if (isInThread(msg)) {
                    importantThreadMentions.add(msg);
                } else {
                    importantChannelMentions.add(msg);
                }
            } else if (isInThread(msg) && hasFrequentMentions(msg)) {
                frequentThreadMentions.add(msg);
            } else if (containsKeywords) {
                if (isInThread(msg)) {
                    keywordThreadMessages.add(msg);
                } else {
                    keywordChannelMessages.add(msg);
                }
            }
        }

        // Формируем отчет
        StringBuilder report = new StringBuilder("Daily summary of your Slack messages:\n\n");

        report.append("**Direct Messages from Important Users:**\n");
        appendMessages(report, importantDirectMessages);

        report.append("\n**Channel Mentions from Important Users:**\n");
        appendMessages(report, importantChannelMentions);

        report.append("\n**Thread Mentions from Important Users:**\n");
        appendMessages(report, importantThreadMentions);

        report.append("\n**Frequent Mentions in Threads (Other Users):**\n");
        appendMessages(report, frequentThreadMentions);

        report.append("\n**Other Direct Messages:**\n");
        appendMessages(report, otherDirectMessages);

        report.append("\n**Keyword Messages in Channels (Other Users):**\n");
        appendMessages(report, keywordChannelMessages);

        report.append("\n**Keyword Messages in Threads (Other Users):**\n");
        appendMessages(report, keywordThreadMessages);

        return report.toString();
    }

    private boolean containsKeywords(Message message) {
        for (String keyword : keywords) {
            if (message.getText() != null && message.getText().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInThread(Message message) {
        return message.getThreadTs() != null && !message.getThreadTs().equals(message.getTs());
    }

    private boolean hasFrequentMentions(Message message) {
        return message.getText() != null && countMentions(message) > 1;
    }

    private int countMentions(Message message) {
        String text = message.getText();
        int count = 0;
        int index = 0;
        String mention = "<@" + userId + ">";
        while ((index = text.indexOf(mention, index)) != -1) {
            count++;
            index += mention.length();
        }
        return count;
    }

    private void appendMessages(StringBuilder report, List<Message> messages) {
        if (messages.isEmpty()) {
            report.append("- None\n");
        } else {
            for (Message msg : messages) {
                report.append("- ").append(msg.getUser()).append(": ").append(msg.getText()).append("\n");
            }
        }
    }

    // Метод для отправки сообщения пользователю
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

    // Метод для обновления времени последней проверки
    public void updateLastCheckTime() throws SQLException {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        userSettingsDAO.updateLastCheckTime(userId, currentTimestamp);
    }

}
