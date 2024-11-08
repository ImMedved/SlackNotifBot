import DAO.UserSettingsDAO;

public class SlackBotApplication {
    public static void main(String[] args) {
        String token = "xoxb-88040645094-7968157008935-z2T9z7R0CBQne4NwDkt9FJmO";
        String userId = "U02V2H7FLUW";

        UserSettingsDAO userSettingsDAO = new UserSettingsDAO();
        SlackService slackService;

        try {
            slackService = new SlackService(token, userId, userSettingsDAO);
            slackService.sendDailySummary();
            slackService.updateLastCheckTime();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
