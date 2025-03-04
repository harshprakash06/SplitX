package com.xtemper.connectme.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.xtemper.connectme.DTO.PaymentUserDTO;
import com.xtemper.connectme.DTO.UserDTO;
import com.xtemper.connectme.Database.MongoDB;
import com.xtemper.connectme.Database.MySql;
import com.xtemper.connectme.Helper.MessagePayments;
import com.xtemper.connectme.Logger.UserLoginLogger;
import com.xtemper.connectme.Token.Security;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class PaymentController {
    private static final String UPI_REGEX = "^[a-zA-Z0-9._\\-]{2,256}@[a-zA-Z]{2,64}$";
    MongoDatabase mongoDatabase = MongoDB.getDatabase();
    // Payment collection for payment details
    MongoCollection<Document> collection = mongoDatabase.getCollection("Payments");
    // Message collection for message documents
    MongoCollection<Document> messageCollection = mongoDatabase.getCollection("Messages");

    // Single user split
    @PostMapping("/split/{id}")
    public splitResponse createSplit(@RequestBody splitBody splitBody, @PathVariable String id) throws JsonProcessingException, SQLException {
        String paymentId = UUID.randomUUID().toString();

        // Build queries for payments and messages
        BasicDBObject query = new BasicDBObject();
        BasicDBObject messageQuery = new BasicDBObject();
        query.put("paymentId", paymentId);
        messageQuery.put("messageId", id);

        // Fetch documents from the respective collections
        Document document = collection.find(query).first();
        Document messageDocument = messageCollection.find(messageQuery).first();

        // If the payment already exists or message does not exist, return error
        if (document != null || messageDocument == null)
            return new splitResponse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // Create a new Payment document and insert it into the Payments collection
        Document newPayment = new Document()
                .append("userId", splitBody.getUserId())
                .append("paymentId", paymentId)
                .append("amount", splitBody.getAmount())
                .append("receiver", splitBody.getReceiver())
                .append("messageId", id)
                .append("status", 1)
                .append("createdAt", System.currentTimeMillis());
        collection.insertOne(newPayment);

        // Retrieve and update the payments subdocument from the message document.
        // Depending on how your MessagePayments class is structured,
        // you may need to convert the embedded Document to a MessagePayments object.
        // Here we assume that the payments field is stored as a MessagePayments object.
        ObjectMapper objectMapper = new ObjectMapper();

// Convert the payments field to a JSON string and deserialize it into a MessagePayments instance

        Object paymentsField = messageDocument.get("payments");
        String messagePaymentJson;
        if (paymentsField == null) {
            messagePaymentJson = "{}";
        } else if (paymentsField instanceof Document) {
            messagePaymentJson = ((Document) paymentsField).toJson();
        } else {
            messagePaymentJson = paymentsField.toString();
        }

        MessagePayments messagePayments = objectMapper.readValue(
                messagePaymentJson,
                MessagePayments.class
        );

        // If the payments field is null, initialize it
        HashMap<String, Long>  messagePaymentFetch;
        if (messagePayments != null) {
            messagePaymentFetch = messagePayments.getPayments();
        } else {
            messagePaymentFetch = new HashMap<>();
        }
        // Add the new paymentId with the current timestamp as the key
        messagePaymentFetch.put(paymentId, System.currentTimeMillis());
        // Update the message document's payments field in the Messages collection.
        // Note: Make sure to update the correct collection (messageCollection).


        //Have to change and attach to each message
        messageCollection.updateOne(
                Filters.eq("messageId", id),
                Updates.set("payments", new Document("payments", messagePaymentFetch))
        );


        //Add the payment to userDetails database of both sender and receiver
        Connection connection = MySql.runQuery();
        assert connection != null;
        try{
            PreparedStatement pst_sender = connection.prepareStatement("UPDATE  userDetails SET payment_data =  JSON_ARRAY_APPEND(IFNULL(payment_data, JSON_ARRAY()), '$', ?) WHERE userID = ?;");
            PreparedStatement pst_receiver = connection.prepareStatement("UPDATE  userDetails SET payment_pending =  JSON_ARRAY_APPEND(IFNULL(payment_pending, JSON_ARRAY()), '$', ?) WHERE userID = ?;");
            pst_sender.setString(1, paymentId);
            pst_receiver.setString(1, paymentId);
            pst_sender.setInt(2,splitBody.getUserId());
            pst_receiver.setInt(2,splitBody.getReceiver());
            pst_sender.executeUpdate();
            pst_receiver.executeUpdate();
            pst_sender.close();
        }catch (SQLException ignored){
            return new splitResponse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }


        return new splitResponse(ResponseEntity.status(HttpStatus.CREATED).build(), paymentId, id);
    }


    @GetMapping("/getPayments")
    public ResponseEntity<?> getPayments(@RequestHeader String token , @RequestHeader String type)  {
        System.out.println(token + " " + type);
        UserDTO user = Security.decryptObject(token);
        if(user == null || user.getId() == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Connection con = MySql.runQuery();
       
            try{
                assert  con != null;
                PreparedStatement pst =null;
                if(type.equals("History") || type.equals("history") || type.equals("HISTORY")){
                     pst = con.prepareStatement("Select payment_history FROM userDetails WHERE userID = ? ");
                } else if (type.equals("pending") || type.equals("Pending") || type.equals("PENDING")) {
                    pst = con.prepareStatement("Select payment_pending FROM userDetails WHERE userID = ? ");
                } else if (type.equals("payments") || type.equals("Payments") || type.equals("PAYMENTS")) {
                    pst = con.prepareStatement("Select payment_data FROM userDetails WHERE userID = ? ");
                }
                else {
                    InvalidPaymentException exc = new InvalidPaymentException("Invalid payment type");
                    throw exc;
                }
                pst.setInt(1, user.getId());
                ResultSet rs = pst.executeQuery();
                if(rs.next()){
                    return new ResponseEntity<>(rs.getObject(1), HttpStatus.OK);
                }
            }catch (InvalidPaymentException e){
                ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            catch (Exception e){
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        
        
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);

    }


    @PostMapping("/payUser")
    public ResponseEntity<?> makePayment(@RequestHeader String token , @RequestHeader String paymentId){
        if(token == null || token.isEmpty() || paymentId == null || paymentId.isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserDTO user = Security.decryptObject(token);
        if(user == null || user.getId() == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        //FETCHING DATABASE
        MongoDatabase mongoDatabase = MongoDB.getDatabase();
        MongoCollection<Document> collection = mongoDatabase.getCollection("Payments");
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("paymentId", paymentId);
        Document document = collection.find(searchQuery).first();
        if(document == null)
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        //CREATING PAYMENT TO OBJECT
        Double amount = document.getDouble("amount");
        int receiver = document.getInteger("receiver");
        int userId = document.getInteger("userId");
        int status = document.getInteger("status");
        if(userId != user.getId())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if(status != 1)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        //fetching upi id
        Connection con = MySql.runQuery();
        assert con != null;
        try(PreparedStatement pst = con.prepareStatement("SELECT upi_id FROM userDetails where userID = ?")){
            pst.setInt(1,receiver);
            ResultSet rs = pst.executeQuery();
            if(!rs.next())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No upi id");
            String receiverUpi= rs.getString(1);
            if(!Pattern.matches(UPI_REGEX, receiverUpi))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid upi");
            UserLoginLogger.logPayment(userId,paymentId,receiver,amount,receiverUpi);
            return ResponseEntity.status(HttpStatus.OK).body(new upiResponseBody(receiverUpi,amount));
        }catch (Exception ignored){}
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


    @PostMapping("/confirmPayment")
    public ResponseEntity<?> confirmPayment(@RequestHeader String token , @RequestHeader String paymentId){
        if(token == null || token.isEmpty() || paymentId == null || paymentId.isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserDTO user = Security.decryptObject(token);
        if(user == null || user.getId() == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // Fetching payment detail
        MongoDatabase mongoDatabase = MongoDB.getDatabase();
        MongoCollection<Document> collection = mongoDatabase.getCollection("Payments");
        Document document = collection.find(new Document("paymentId", paymentId)).first();
        if(document == null || document.getInteger("status") != 1 )
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if(!Objects.equals(document.getInteger("userId"), user.getId()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // UPDATING PAYMENT STATUS
        try{
            collection.updateOne(new Document("paymentId", paymentId), new Document("$set", new Document("status", 2)));
            return ResponseEntity.status(HttpStatus.OK).body("Payment confirmed successfully");
        }catch (Exception ignored){}
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}

class upiResponseBody{
    String upi_id;
    double amount;

    public upiResponseBody(String upi_id, double amount) {
        this.upi_id = upi_id;
        this.amount = amount;
    }

    public String getUpi_id() {
        return upi_id;
    }

    public void setUpi_id(String upi_id) {
        this.upi_id = upi_id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}






class splitResponse {
    ResponseEntity<String> responseEntity;
    String paymentId;
    String messageId;

    public splitResponse(ResponseEntity<String> responseEntity, String paymentId, String messageId) {
        this.responseEntity = responseEntity;
        this.paymentId = paymentId;
        this.messageId = messageId;
    }

    public splitResponse() {
    }

    public splitResponse(ResponseEntity<String> responseEntity) {
        this.responseEntity = responseEntity;
    }

    public ResponseEntity<String> getResponseEntity() {
        return responseEntity;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getMessageId() {
        return messageId;
    }
}

class splitBody {
    private int userId;
    private double amount;
    private String token;
    private int receiver;

    public splitBody(int userId, double amount, String token, int receiver) {
        this.userId = userId;
        this.amount = amount;
        this.token = token;
        this.receiver = receiver;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


class InvalidPaymentException extends Exception {
    public InvalidPaymentException(String message) {
        super(message);
    }
}