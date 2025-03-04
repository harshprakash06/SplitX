package com.xtemper.connectme.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtemper.connectme.DTO.UserDTO;
import com.xtemper.connectme.Logger.UserLoginLogger;
import com.xtemper.connectme.Token.Security;
import com.xtemper.connectme.Database.MySql;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/userRegistration")
public class UserLogin {


        @PostMapping("/login")
        public LoginResponse login(@RequestBody LoginRequest body) throws SQLException {
            // Validate input
            if (body.getUsername() == null || body.getPassword() == null) {
                return new LoginResponse(HttpStatus.UNAUTHORIZED, "Username or Password cannot be null", null);
            }

            String query = "SELECT * FROM userDetails WHERE userName = ? OR email = ?";
            try (Connection connection = MySql.runQuery()) {
                assert connection != null;
                try (PreparedStatement statement = connection.prepareStatement(query)) {

                    statement.setString(1, body.getUsername());
                    statement.setString(2, body.getUsername());
                    ResultSet resultSet = statement.executeQuery();

                    if (!resultSet.next()) {
                        UserLoginLogger.logLogin(-1, body.getUsername(), false);
                        return new LoginResponse(HttpStatus.UNAUTHORIZED, "User not found", null);
                    }

                    String securePassword = resultSet.getString("password");
                    if (!Security.passDecryption(securePassword).equals(body.getPassword())) {
                        UserLoginLogger.logLogin(resultSet.getInt("userID"), body.getUsername(), false);
                        return new LoginResponse(HttpStatus.UNAUTHORIZED, "Invalid password", null);
                    }

                    // Process friends and message IDs
                    String jsonFriends = resultSet.getString("friends");
                    String jsonMessageIds = resultSet.getString("messageIds");

                    ObjectMapper objectMapper = new ObjectMapper();
                    List<Integer> friends = (jsonFriends != null && !jsonFriends.isEmpty())
                            ? objectMapper.readValue(jsonFriends, new TypeReference<List<Integer>>() {})
                            : new ArrayList<>();

                    List<String> messageIds = (jsonMessageIds != null && !jsonMessageIds.isEmpty())
                            ? objectMapper.readValue(jsonMessageIds, new TypeReference<List<String>>() {})
                            : new ArrayList<>();

                    // Create UserDTO and encrypt
                    UserDTO userDTO = new UserDTO(
                            resultSet.getInt("userID"),
                            resultSet.getString("userName"),
                            resultSet.getString("email"),
                            friends,
                            messageIds,
                            resultSet.getString("upi_id")
                    );

                    Security security = new Security();
                    String encryptedUser = security.encrypt(userDTO);

                    // Log successful login
                    UserLoginLogger.logLogin(resultSet.getInt("userID"), body.getUsername(), true);

                    return new LoginResponse(HttpStatus.ACCEPTED, "Success", encryptedUser);

                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return new LoginResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null);
            } catch (SQLException e) {
                e.printStackTrace();
                return new LoginResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null);
            }
        }

        @PostMapping("/register")
        public LoginResponse register(@RequestBody SignUpRequest body) throws SQLException {
            // Check if username or email already exists
            String checkQuery = "SELECT COUNT(*) AS count FROM userDetails WHERE userName = ? OR email = ?";
            try (Connection connection = MySql.runQuery();
                 PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {

                checkStmt.setString(1, body.getUsername());
                checkStmt.setString(2, body.getEmail());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt("count") > 0) {
                    return new LoginResponse(HttpStatus.CONFLICT, "Username or Email already exists", null);
                }
            }

            String query = "INSERT INTO userDetails (userName, email, password) VALUES (?, ?, ?)";
            try (Connection connection = MySql.runQuery();
                 PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, body.getUsername());
                statement.setString(2, body.getEmail());
                Security security = new Security();
                statement.setString(3, security.passEncryption(body.getPassword()));
                int res = statement.executeUpdate();

                if (res == 0) {
                    return new LoginResponse(HttpStatus.UNAUTHORIZED, "User Already Registered", null);
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newUserId = generatedKeys.getInt(1);
                        UserDTO newUser = new UserDTO(newUserId, body.getUsername(), body.getEmail(), new ArrayList<>(), new ArrayList<>(), null);
                        String newToken = security.encrypt(newUser);

                        // Log successful registration
                        UserLoginLogger.logRegistration(newUserId, body.getUsername(), true);

                        return new LoginResponse(HttpStatus.ACCEPTED, "Registration successful", newToken);
                    } else {
                        return new LoginResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return new LoginResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return new LoginResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null);
            }
        }
    }



// Request class for login
class LoginRequest {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}

// Response class for login/registration
class LoginResponse {
    private HttpStatus status;
    private String message;
    private String token;

    public LoginResponse(HttpStatus status, String message, String token) {
        this.status = status;
        this.message = message;
        this.token = token;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
}

// Request class for sign-up/registration
class SignUpRequest {
    private String username;
    private String password;
    private String email;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
}
