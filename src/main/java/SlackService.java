import DAO.UserSettingsDAO;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;

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
        String report = slackSummaryService.generateDailySummary();
        sendMessageToSlack(report);
        updateLastCheckTime();
    }

    private void sendMessageToSlack(String text) {
        Slack slack = Slack.getInstance();

        try {
            var openResponse = slack.methods(token).conversationsOpen(req -> req.users(List.of(userId)));

            if (openResponse.isOk()) {
                String channelId = openResponse.getChannel().getId();
                var response = slack.methods(token).chatPostMessage(req -> req.channel(channelId).text(text));

                if (!response.isOk()) {
                    System.err.println("Error sending message: " + response.getError());
                }
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }

    public void updateLastCheckTime() throws SQLException {
        userSettingsDAO.updateLastCheckTime(userId, new java.sql.Timestamp(System.currentTimeMillis()));
    }
}
