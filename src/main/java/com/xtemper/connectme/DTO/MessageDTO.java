package com.xtemper.connectme.DTO;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xtemper.connectme.Helper.MessageEntry;
import com.xtemper.connectme.Helper.MessagePayments;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDTO {

    private Object _id;
    private String messageID;
    private List<MessageEntry> messages = new ArrayList<>();
    private ArrayList<Integer> tokens;
    private MessagePayments payments = new MessagePayments();
    public MessageDTO() {}
    public MessageDTO(Object _id, String messageID, List<MessageEntry> messages, ArrayList<Integer> tokens , MessagePayments payments) {
        this._id = _id;
        this.messageID = messageID;
        this.messages.addAll(messages) ;
        this.tokens = tokens;
        this.payments = payments;
    }

    public MessagePayments getPayments() {
        return payments;
    }

    public void setPayments(MessagePayments payments) {
        this.payments = payments;
    }

    public Object get_id() {
        return _id;
    }
    public void set_id(Object _id) {
        this._id = _id;
    }
    public String getMessageID() {
        return messageID;
    }
    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }
    public List<MessageEntry> getMessages() {
        return messages;
    }
    public void setMessages(List<MessageEntry> messages) {
        this.messages = messages;
    }
    public ArrayList<Integer> getTokens() {
        return tokens;
    }
    public void setTokens(ArrayList<Integer> tokens) {
        this.tokens = tokens;
    }

    @Override
    public String  toString() {
        return "MessageSchema{" +
                "messageID='" + messageID + '\'' +
                ", messages=" + messages +
                ", tokens=" + tokens +
                '}';
    }
}
