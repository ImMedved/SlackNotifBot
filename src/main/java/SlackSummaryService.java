import DAO.UserSettingsDAO;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.Message;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SlackSummaryService {
    private final SlackDataService slackDataService;
    private final UserSettingsDAO userSettingsDAO;
    private final List<String> keywords;
    private final List<String> importantUsers;
    private final Timestamp lastCheckTime;

    public SlackSummaryService(SlackDataService slackDataService, UserSettingsDAO userSettingsDAO, String userId) throws SQLException {
        this.slackDataService = slackDataService;
        this.userSettingsDAO = userSettingsDAO;
        this.keywords = userSettingsDAO.getUserKeywords(userId);
        this.importantUsers = userSettingsDAO.getImportantUsers(userId);
        this.lastCheckTime = userSettingsDAO.getLastCheckTime(userId);
    }

    public String generateDailySummary() throws IOException, SlackApiException, SQLException {
        double lastCheckTimestamp = lastCheckTime != null ? lastCheckTime.getTime() / 1000.0 : 0;

        List<Message> directMessages = slackDataService.getUnreadDirectMessages(lastCheckTimestamp);
        List<Message> importantDirectMessages = new ArrayList<>();
        List<Message> otherDirectMessages = new ArrayList<>();

        for (Message msg : directMessages) {
            if (importantUsers.contains(msg.getUser())) {
                importantDirectMessages.add(msg);
            } else {
                otherDirectMessages.add(msg);
            }
        }

        List<String> userChannels = slackDataService.getUserChannels();
        List<Message> importantChannelMentions = new ArrayList<>();
        List<Message> importantThreadMentions = new ArrayList<>();
        List<Message> frequentThreadMentions = new ArrayList<>();
        List<Message> keywordChannelMessages = new ArrayList<>();
        List<Message> keywordThreadMessages = new ArrayList<>();

        for (String channelId : userChannels) {
            List<Message> channelMessages = slackDataService.getUnreadChannelMessages(channelId, lastCheckTimestamp);
            for (Message msg : channelMessages) {
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
        }

        StringBuilder report = new StringBuilder("Daily summary of your Slack messages:\n\n");
        appendSection(report, "Direct Messages from Important Users:", importantDirectMessages);
        appendSection(report, "Channel Mentions from Important Users:", importantChannelMentions);
        appendSection(report, "Thread Mentions from Important Users:", importantThreadMentions);
        appendSection(report, "Frequent Mentions in Threads (Other Users):", frequentThreadMentions);
        appendSection(report, "Other Direct Messages:", otherDirectMessages);
        appendSection(report, "Keyword Messages in Channels (Other Users):", keywordChannelMessages);
        appendSection(report, "Keyword Messages in Threads (Other Users):", keywordThreadMessages);

        return report.toString();
    }

    private boolean containsKeywords(Message message) {
        return keywords.stream().anyMatch(keyword -> message.getText() != null && message.getText().contains(keyword));
    }

    private boolean isInThread(Message message) {
        return message.getThreadTs() != null && !message.getThreadTs().equals(message.getTs());
    }

    private boolean hasFrequentMentions(Message message) {
        return message.getText() != null && countMentions(message) > 1;
    }

    private int countMentions(Message message) {
        String text = message.getText();
        String mention = "<@" + slackDataService.getUserId() + ">";
        return text != null ? text.split(mention, -1).length - 1 : 0;
    }

    private void appendSection(StringBuilder report, String title, List<Message> messages) {
        report.append("\n").append(title).append("\n");
        if (messages.isEmpty()) {
            report.append("- None\n");
        } else {
            messages.forEach(msg -> report.append("- ").append(msg.getUser()).append(": ").append(msg.getText()).append("\n"));
        }
    }
}
