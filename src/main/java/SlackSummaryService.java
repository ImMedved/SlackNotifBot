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
    private List<String> keywords;
    private List<String> importantUsers;
    private Timestamp lastCheckTime;

    public SlackSummaryService(SlackDataService slackDataService, UserSettingsDAO userSettingsDAO, String userId) throws SQLException {
        this.slackDataService = slackDataService;
        this.userSettingsDAO = userSettingsDAO;
        loadUserSettings(userId);
    }

    private void loadUserSettings(String userId) throws SQLException {
        this.keywords = userSettingsDAO.getUserKeywords(userId);
        this.importantUsers = userSettingsDAO.getImportantUsers(userId);
        this.lastCheckTime = userSettingsDAO.getLastCheckTime(userId);
    }

    public String generateDailySummary(String userId) throws IOException, SlackApiException, SQLException {
        double lastCheckTimestamp = lastCheckTime != null ? lastCheckTime.getTime() / 1000.0 : 0;
        List<Message> allMessages = new ArrayList<>();

        List<String> userChannels = slackDataService.getUserChannels();

        for (String channelId : userChannels) {
            allMessages.addAll(slackDataService.getUnreadChannelMessages(channelId, userId, lastCheckTimestamp));
        }

        List<Message> importantDirectMessages = new ArrayList<>();
        List<Message> importantChannelMentions = new ArrayList<>();
        List<Message> importantThreadMentions = new ArrayList<>();
        List<Message> frequentThreadMentions = new ArrayList<>();
        List<Message> otherDirectMessages = new ArrayList<>();
        List<Message> keywordChannelMessages = new ArrayList<>();
        List<Message> keywordThreadMessages = new ArrayList<>();

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
        String mention = "<@" + slackDataService.getUserId() + ">";
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
}
