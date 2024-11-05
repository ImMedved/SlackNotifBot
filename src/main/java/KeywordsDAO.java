import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class KeywordsDAO {
    public void addKeyword(String userId, String keyword) throws SQLException {
        String query = "INSERT INTO Keywords (userId, keyword) VALUES (?, ?)";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, userId);
            statement.setString(2, keyword);
            statement.executeUpdate();
        }
    }
}
