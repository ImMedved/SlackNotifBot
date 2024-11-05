import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserSettingsDAO {
    public List<String> getUserKeywords(String userId) throws SQLException {
        List<String> keywords = new ArrayList<>();
        String query = "SELECT keyword FROM Keywords WHERE userId = ?";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                keywords.add(rs.getString("keyword"));
            }
        }
        return keywords;
    }

    public List<String> getImportantUsers(String userId) throws SQLException {
        List<String> importantUsers = new ArrayList<>();
        String query = "SELECT importantUserId FROM ImportantUsers WHERE userId = ?";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                importantUsers.add(rs.getString("importantUserId"));
            }
        }
        return importantUsers;
    }

    public Timestamp getLastCheckTime(String userId) throws SQLException {
        String query = "SELECT lastCheckTime FROM UserSettings WHERE userId = ?";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getTimestamp("lastCheckTime");
            }
        }
        return null;
    }

    public void updateLastCheckTime(String userId, Timestamp timestamp) throws SQLException {
        String query = "UPDATE UserSettings SET lastCheckTime = ? WHERE userId = ?";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTimestamp(1, timestamp);
            statement.setString(2, userId);
            statement.executeUpdate();
        }
    }
}
