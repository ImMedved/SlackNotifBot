import java.util.List;

public class SlackBotApplication {
    public static void main(String[] args) {
        String token = "xoxb-88040645094-7968157008935-DaX8qN3420yhvoTrG0niCTN9"; // Токен Slack
        String userId = "U02V2H7FLUW"; // ID пользователя

        UserSettingsDAO userSettingsDAO = new UserSettingsDAO();
        SlackService slackService;

        try {
            // Создаем SlackService с токеном, userId и userSettingsDAO
            slackService = new SlackService(token, userId, userSettingsDAO);

            // Генерируем отчет
            String report = slackService.generateDailySummary();

            // Отправляем отчет
            slackService.sendMessage(userId, report);

            // Обновляем время последней проверки
            slackService.updateLastCheckTime();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}