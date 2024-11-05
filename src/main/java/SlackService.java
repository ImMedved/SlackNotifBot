import DAO.UserSettingsDAO;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsOpenRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SlackService {
    private final SlackDataService slackDataService;
    private final SlackSummaryService slackSummaryService;
    private final UserSettingsDAO userSettingsDAO;
    private final String userId;
    private final String token;

    public SlackService(String token, String userId, UserSettingsDAO userSettingsDAO) throws SQLException {
        this.userId = userId;
        this.userSettingsDAO = userSettingsDAO;
        this.token = token;

        this.slackDataService = new SlackDataService(token, userId);
        this.slackSummaryService = new SlackSummaryService(slackDataService, userSettingsDAO, userId);
    }

    public void sendDailySummary() throws IOException, SlackApiException, SQLException {
        String report = slackSummaryService.generateDailySummary(userId);
        System.out.println("Report is: " + report);
        sendMessageToSlack(userId ,report);
        updateLastCheckTime();
    }

    public void sendMessageToSlack(String userId, String text) {
        Slack slack = Slack.getInstance();

        try {
            // Открываем личный канал с пользователем (или получаем существующий)
            ConversationsOpenResponse openResponse = slack.methods(token).conversationsOpen(ConversationsOpenRequest.builder()
                    .users(List.of(userId))
                    .build());

            if (openResponse.isOk()) {
                String channelId = openResponse.getChannel().getId();

                // Отправляем сообщение в личный канал
                ChatPostMessageResponse response = slack.methods(token).chatPostMessage(ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text(text)
                        .build());

                if (!response.isOk()) {
                    System.err.println("Error sending message: " + response.getError());
                }
            } else {
                System.err.println("Error opening conversation: " + openResponse.getError());
            }
        } catch (IOException | SlackApiException e) {
            System.err.println("Exception occurred while sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Сделаем этот метод публичным для доступа из других классов
    public void updateLastCheckTime() throws SQLException {
        userSettingsDAO.updateLastCheckTime(userId, new java.sql.Timestamp(System.currentTimeMillis()));
    }
}
