import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ImportantUsersDAO {
    public void addImportantUser(String userId, String importantUserId) throws SQLException {
        String query = "INSERT INTO ImportantUsers (userId, importantUserId) VALUES (?, ?)";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            statement.setString(2, importantUserId);
            statement.executeUpdate();
        }
    }
}
