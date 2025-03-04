package com.xtemper.connectme.Controller;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.xtemper.connectme.DTO.UserDTO;
import com.xtemper.connectme.Helper.MessageEntry;
import com.xtemper.connectme.DTO.MessageDTO;
import com.xtemper.connectme.Helper.MessagePayments;
import com.xtemper.connectme.Token.Security;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.xtemper.connectme.Database.MongoDB;
import org.bson.Document;
import com.xtemper.connectme.Database.MySql;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
public class MessageController {
    MongoDatabase mongoDatabase = MongoDB.getDatabase();
    MongoCollection<Document> messageCollection = mongoDatabase.getCollection("Messages");

    @PutMapping("/send/{id}")
    public ResponseEntity<String> sendNewMessage(@PathVariable String id , @RequestBody MessageBody message) {
        UserDTO user = Security.decryptObject(message.getSender());
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        int sender = user.getId();

        // Find the message in MongoDB
        BasicDBObject searchQuery = new BasicDBObject("messageId", id);
        Document document = messageCollection.find(searchQuery).first();

        if (document == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        // Convert Document to MessageSchema

        MessageDTO messageDTO =   new MessageDTO(document.get("_id" ) , (String) document.get("messageId"), (List<MessageEntry>) document.get("message"), (ArrayList<Integer>) document.get("users") , (MessagePayments) document.get("payments"));

        if (!messageDTO.getTokens().contains(sender))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Add new message to the existing messages list
        List<MessageEntry> messageEntries = messageDTO.getMessages();
        messageEntries.add(new MessageEntry(user.getUsername(), message.getMessage(), System.currentTimeMillis()));

        // Update the message document in MongoDB
        messageCollection.updateOne(
                searchQuery,
                new Document("$set", new Document("message", messageEntries))
        );

        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @PostMapping("/new conversation")
    public newConversationReturns newConversation( @RequestBody MessageBody message) {
        UserDTO user = Security.decryptObject(message.getSender());
        if(user==null)

            return new newConversationReturns(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        int sender = user.getId();
        MessageDTO newMessageDTO = new MessageDTO();
        String messageId = UUID.randomUUID().toString();
        newMessageDTO.setMessageID(messageId);
        ArrayList<Integer> tokens = new ArrayList<>();
        tokens.add(sender);
        if(message.getReceiver().length==0){
            return  new newConversationReturns(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
        if(message.getReceiver().length==1){
            tokens.add(message.getReceiver()[0]);
        }else{
            for(int i = 0 ; i < message.getReceiver().length;i++)
                tokens.add(message.getReceiver()[i]);
        }
        newMessageDTO.setTokens(tokens);
        MessageEntry newMessageEntry = new MessageEntry(user.getUsername(), message.getMessage(),System.currentTimeMillis());
        List<MessageEntry> messageEntries = new ArrayList<>();
        messageEntries.add(newMessageEntry);
        newMessageDTO.setMessages(messageEntries);
        String messageName = message.getMessageName();
        Connection connection = MySql.runQuery();
        String reciverUserName= "";
        if(message.getReceiver().length == 1){
        try{
            assert connection != null;
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT userName from userDetails where userID =?");
            preparedStatement.setInt(1, tokens.get(1));
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                reciverUserName = resultSet.getString("userName");
                messageName = user.getUsername() +"/"+ (reciverUserName);
            }
            else throw new SQLException();

        }catch (SQLException ignored){return new newConversationReturns(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());}

        }

        Document updateQuerry = new Document().append("messageId", messageId).append("messageName",messageName).append("message" , messageEntries).append("users", tokens);
        messageCollection.insertOne(updateQuerry);


        try{
            PreparedStatement pst = connection.prepareStatement("UPDATE  userDetails SET messageIds =  JSON_ARRAY_APPEND(IFNULL(messageIds, JSON_ARRAY()), '$', ?) WHERE userID = ?;");
            pst.setString(1,messageId);
            pst.setInt(2, user.getId());
            pst.executeUpdate();
            if(message.getReceiver().length == 1){
                pst.setString(1, messageId);
                pst.setInt(2,message.getReceiver()[0]);
                pst.executeUpdate();
            }else{
                for(int i = 0 ; i < message.getReceiver().length ; i++){
                    pst.setString(1, messageId);
                    pst.setInt(2,message.getReceiver()[i]);
                    pst.executeUpdate();
                }
            }
            pst.close();
            UserDTO newUser = new UserDTO(user.getId(),user.getUsername(), user.getEmail(),user.getFriends(),user.getMessageId(), user.getUpiId());
            Security security = new Security();
            return new newConversationReturns(ResponseEntity.status(HttpStatus.CREATED).body(messageId), security.encrypt(newUser));
        } catch (SQLException e) {
            return new newConversationReturns(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }

    }
}


class MessageBody{
    private String messageName;
    private String sender;
    private int[] receiver;
    private String message;


    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }


    public int[] getReceiver() {
        return receiver;
    }

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    public void setReceiver(int[] receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
class newConversationReturns{
    ResponseEntity<String>  response;
    String newToken;
    newConversationReturns(ResponseEntity<String> response){
        this.response = response;
    }

    newConversationReturns(ResponseEntity<String>  response, String newToken) {
        this.response = response;
        this.newToken = newToken;
    }
    newConversationReturns(){};

    public ResponseEntity<String> getResponse() {
        return response;
    }

    public void setResponse(ResponseEntity<String> response) {
        this.response = response;
    }

    public String getNewToken() {
        return newToken;
    }

    public void setNewToken(String newToken) {
        this.newToken = newToken;
    }
}