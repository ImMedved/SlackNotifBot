import DAO.UserSettingsDAO;
import com.slack.api.methods.SlackApiException;

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
        String lastCheckDate = lastCheckTime != null ? lastCheckTime.toString() : "unknown";

        List<SimpleMessage> directMessages = slackDataService.getUnreadDirectMessages(lastCheckTimestamp);
        List<SimpleMessage> importantDirectMessages = new ArrayList<>();
        List<SimpleMessage> otherDirectMessages = new ArrayList<>();

        for (SimpleMessage msg : directMessages) {
            if (importantUsers.contains(msg.getChannelId())) {
                importantDirectMessages.add(msg);
            } else {
                otherDirectMessages.add(msg);
            }
        }

        List<String> userChannels = slackDataService.getUserChannels();
        List<SimpleMessage> importantChannelMentions = new ArrayList<>();
        List<SimpleMessage> importantThreadMentions = new ArrayList<>();
        List<SimpleMessage> frequentThreadMentions = new ArrayList<>();
        List<SimpleMessage> keywordChannelMessages = new ArrayList<>();
        List<SimpleMessage> keywordThreadMessages = new ArrayList<>();
        List<SimpleMessage> mentionsInChannels = new ArrayList<>();

        for (String channelId : userChannels) {
            List<SimpleMessage> channelMessages = slackDataService.getUnreadChannelMessages(channelId, lastCheckTimestamp);
            for (SimpleMessage msg : channelMessages) {
                boolean isFromImportantUser = importantUsers.contains(msg.getChannelId());
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
                } else if (msg.getText() != null && msg.getText().contains("<@" + slackDataService.getUserId() + ">")) {
                    mentionsInChannels.add(msg);
                }
            }
        }

        int totalDirectMessages = importantDirectMessages.size() + otherDirectMessages.size();
        int totalChannelMessages = importantChannelMentions.size() + importantThreadMentions.size() +
                frequentThreadMentions.size() + keywordChannelMessages.size() + keywordThreadMessages.size() +
                mentionsInChannels.size();

        StringBuilder report = new StringBuilder();
        report.append("*━━━━━━━━━━━━━━━━━━━━━━━*\n")
                .append(":date: *Daily Slack Summary*\n")
                .append("Since *").append(lastCheckDate).append("*, you have *").append(totalDirectMessages)
                .append("* direct messages and *").append(totalChannelMessages).append("* messages in all your chats.\n")
                .append("\n");

        // Adding each section with improved formatting
        appendSection(report, ":small_blue_diamond: *Direct Messages from Important Users*:", importantDirectMessages);
        appendSection(report, ":small_blue_diamond: *Channel and Thread Mentions from Important Users*:", importantChannelMentions);
        appendSection(report, ":small_blue_diamond: *Mentions in Channels*:", mentionsInChannels);
        appendSection(report, ":small_blue_diamond: *Other Direct Messages*:", otherDirectMessages);
        appendSection(report, ":small_blue_diamond: *Keyword Messages in Threads and Channels*:", keywordThreadMessages);

        report.append("━━━━━━━━━━━━━━━━━━━━━━━");

        return report.toString();
    }

    private void appendSection(StringBuilder report, String title, List<SimpleMessage> messages) {
        report.append("\n").append(title).append("\n");
        if (messages.isEmpty()) {
            report.append("• *No messages found in this category*\n");
        } else {
            for (SimpleMessage msg : messages) {
                String messageLink = String.format("https://setronica.slack.com/archives/%s/", msg.getChannelId());
                report.append("→ [#").append(msg.getChannelId()).append("](").append(messageLink).append(") • ")
                        .append(msg.getText()).append("\n");
            }
        }
        report.append("\n");
    }


    private boolean containsKeywords(SimpleMessage message) {
        return keywords.stream().anyMatch(keyword -> message.getText() != null && message.getText().contains(keyword));
    }

    private boolean isInThread(SimpleMessage message) {
        return message.getChannelId() != null && !message.getChannelId().equals(message.getText());
    }

    private boolean hasFrequentMentions(SimpleMessage message) {
        return message.getText() != null && countMentions(message) > 1;
    }

    private int countMentions(SimpleMessage message) {
        String text = message.getText();
        String mention = "<@" + slackDataService.getUserId() + ">";
        return text != null ? text.split(mention, -1).length - 1 : 0;
    }

}
