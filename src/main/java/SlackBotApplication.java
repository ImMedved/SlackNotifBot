public class SlackBotApplication {
    public static void main(String[] args) {
        String token = "xoxb";
        String userId = "U02V2H7FLUW";

        UserSettingsDAO userSettingsDAO = new UserSettingsDAO();
        SlackService slackService;

        try {
            slackService = new SlackService(token, userId, userSettingsDAO);

            String report = slackService.generateDailySummary();

            slackService.sendMessage(userId, report);

            slackService.updateLastCheckTime();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}