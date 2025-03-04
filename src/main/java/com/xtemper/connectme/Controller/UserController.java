package com.xtemper.connectme.Controller;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.xtemper.connectme.DTO.UserDTO;
import com.xtemper.connectme.Database.MongoDB;
import com.xtemper.connectme.Database.MySql;
import com.xtemper.connectme.Helper.ListDuplicateRemover;
import com.xtemper.connectme.Token.Security;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RestController
@SuppressWarnings("ALL")
public class UserController {
    @GetMapping("/user")
    public idResponse user(@RequestHeader("token") String token) {
        try {
            if (token == null || token.isEmpty()) {
                return new idResponse(HttpStatus.BAD_REQUEST, null);
            }
            UserDTO user = Security.decryptObject(token);
            if (user == null) {
                return new idResponse(HttpStatus.UNAUTHORIZED, null);
            }
            return new idResponse(HttpStatus.OK, user);
        } catch (Exception e) {
            e.printStackTrace();  // This should log the stack trace
            return new idResponse(HttpStatus.BAD_GATEWAY, null);
        }
    }

    @PutMapping("/add friend")
    public FriendResponse addFriend(@RequestBody Token jsonToken ) {
        try{
            UserDTO user = Security.decryptObject(jsonToken.getToken());
            if(user == null)
                return new FriendResponse(HttpStatus.UNAUTHORIZED);
            Connection connection = MySql.runQuery();
            PreparedStatement pst = connection.prepareStatement("UPDATE userDetails SET friends =  JSON_ARRAY_APPEND(IFNULL(friends, JSON_ARRAY()), '$', ?) WHERE userID = ?;");
            pst.setInt(1, jsonToken.getFriendID());
            pst.setInt(2,user.getId());
            pst.executeUpdate();
            pst.close();
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE userDetails SET friends =  JSON_ARRAY_APPEND(IFNULL(friends, JSON_ARRAY()), '$', ?) WHERE userID = ?;");
            preparedStatement.setInt(2, jsonToken.getFriendID());
            preparedStatement.setInt(1,user.getId());
            preparedStatement.executeUpdate();
            preparedStatement.close();
            List<Integer> friends = user.getFriends();
            friends.add(jsonToken.getFriendID());
            Security sec = new Security();
            String newToken = sec.encrypt(user);
            return new FriendResponse(HttpStatus.CONTINUE , newToken);
        }catch(Exception e){
            return new FriendResponse(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/userprofile/{id}", produces = "application/json")
    public UserProfileResponse userProfile(@PathVariable String id, @RequestHeader String token)  {
        Security security = new Security();
        UserDTO user = security.decryptObject(token);
        if(user == null)
            return new UserProfileResponse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        Connection connection = MySql.runQuery();
        try {
            // Verify that the token owner exists in the database.
            // Using a parameterized query here as well.
            PreparedStatement verifyStmt = connection.prepareStatement("SELECT 1 FROM userDetails WHERE userID=?");
            verifyStmt.setInt(1, user.getId());
            ResultSet verifyRs = verifyStmt.executeQuery();
            if (!verifyRs.next()) {  // If no rows are returned, then the user does not exist.
                return new UserProfileResponse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(HttpStatus.valueOf("Invalid request")));
            }
            verifyRs.close();
            verifyStmt.close();

            PreparedStatement pst = connection.prepareStatement("SELECT * FROM userDetails WHERE userName = ? or email=? or userID = ?");
            pst.setString(1, id);
            pst.setString(2, id);
            pst.setString(3, id);
            ResultSet resultSet = pst.executeQuery();
            if (!resultSet.next()) {
                resultSet.close();
                pst.close();
                return new UserProfileResponse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
            }


            int userId = resultSet.getInt("userID");
            String userName = resultSet.getString("userName");

            return new UserProfileResponse(ResponseEntity.status(HttpStatus.OK).build(), userId, userName);
        } catch (SQLException e) {
            e.printStackTrace();
            return new UserProfileResponse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
    }

    @PutMapping("/userupi/add")
    public ResponseEntity<?> userUpiAdd(@RequestBody String upiId , @RequestHeader String token) throws SQLException {
        UserDTO user = Security.decryptObject(token);
        if(user ==null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Connection con = MySql.runQuery();
        PreparedStatement pst = con.prepareStatement("SELECT * FROM userDetails where userID=?");
        pst.setInt(1,user.getId());
        ResultSet resultSet = pst.executeQuery();
        if(!resultSet.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        resultSet.close();
        pst.close();
        PreparedStatement preparedStatement = con.prepareStatement("UPDATE userDetails SET upi_id = ? WHERE userID = ?");
        preparedStatement.setInt(2,user.getId());
        preparedStatement.setString(1,upiId);
        int resultSet1 = preparedStatement.executeUpdate();
        if(resultSet1 == 0) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        return ResponseEntity.status(HttpStatus.OK).build();

    }


    @GetMapping("/fetchfriends")
    public ResponseEntity<?> fetchFriends(@RequestHeader String token) {
        if(token == null || token.isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserDTO user = Security.decryptObject(token);
        if(user == null || user.getFriends() == null || user.getFriends().isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Connection connection = MySql.runQuery();
        List<HashMap<String, Object>> friends = new ArrayList<>();
        for(int i = 0 ; i < user.getFriends().size() ; i++) {
            try{
                PreparedStatement pst = connection.prepareStatement("SELECT userName from userDetails  where userID=? ");
                pst.setInt(1,user.getFriends().get(i));
                ResultSet resultSet = pst.executeQuery();

                if(resultSet.next() && user.getFriends().get(i) != user.getId()){
                    HashMap<String,Object> map = new HashMap<>();
                    String userName = resultSet.getString(1);
                    map.put(resultSet.getString(1),user.getFriends().get(i));
                    friends.add(map);
                }
            }catch (SQLException ignored){
                ignored.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

        }
        friends = ListDuplicateRemover.removeDuplicate(friends);
        return ResponseEntity.status(HttpStatus.OK).body(friends);
    }

    @GetMapping("/fetchmessages")
    public ResponseEntity<?> fetchMessages(@RequestHeader String token) {
        if(token == null || token.isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserDTO user = Security.decryptObject(token);
        if(user==null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if(user.getMessageId().isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        MongoDatabase mongoDatabase = MongoDB.getDatabase();
        if(mongoDatabase==null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        List<String> userMessages = user.getMessageId();
        if(userMessages.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        MongoCollection<Document> messageCollection = mongoDatabase.getCollection("Messages");
        BasicDBObject searchQuery = new BasicDBObject();


        List<Document> messages = new ArrayList<>();
        for(int i = 0 ; i < userMessages.size() ; i++) {
            searchQuery.put("messageId", userMessages.get(i));
            MongoCursor<Document> cursor = messageCollection.find(searchQuery).iterator();
            Document document = messageCollection.find(searchQuery).first();
            assert document != null;
            messages.add(document);
        }
        return ResponseEntity.status(HttpStatus.OK).body(messages);
    }

}

@SuppressWarnings("ALL")
class idResponse{
    private HttpStatus httpStatus;
    private UserDTO user;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {

        this.user = new UserDTO(user.getId(), user.getUsername(),user.getEmail(),user.getFriends(),user.getMessageId() , user.getUpiId());
    }

    public idResponse(HttpStatus httpStatus, UserDTO user) {
        this.httpStatus = httpStatus;
        this.user =  new UserDTO(user.getId(), user.getUsername(),user.getEmail(),user.getFriends(),user.getMessageId(), user.getUpiId());
    }

    public idResponse(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
@SuppressWarnings("ALL")
class Token{
    private String token;
    private int friendID;

    public int getFriendID() {
        return friendID;
    }

    public void setFriendID(int friendID) {
        this.friendID = friendID;
    }

    public Token(String token) {
        this.token = token;
    }

    public Token() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

@SuppressWarnings("ALL")
class FriendResponse{
    private HttpStatus httpStatus;
    private String Token;

    public FriendResponse(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public FriendResponse(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        Token = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return Token;
    }

    public void setMessage(String message) {
        Token = message;
    }
}

class UserProfileResponse {
    private int userId;
    private String userName;
    private ResponseEntity<HttpStatus> response;
    public UserProfileResponse(ResponseEntity<HttpStatus> response){this.response = response;}
    public UserProfileResponse(ResponseEntity<HttpStatus> response , int userId , String userName){
        this.response = response;
        this.userName = userName;
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public ResponseEntity<HttpStatus> getResponse() {
        return response;
    }

    public void setResponse(ResponseEntity<HttpStatus> response) {
        this.response = response;
    }
}