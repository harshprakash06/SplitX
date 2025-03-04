package com.xtemper.connectme.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL) // Ensures null fields aren't skipped
public class UserDTO {
    private Integer id;
    private String username;
    private String email;
    private String password;
    private List<Integer> friends = new ArrayList<>();;
    private List<String> messageId = new ArrayList<>();;
    private String token;
    private long expireTime;
    private String upiId;

    public List<String> getMessageId() {
        return messageId;
    }
    UserDTO(int id , String username , String email , List<Integer> friends , List<String> messageId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.friends = friends;
        this.messageId = messageId;
    }

    public void setMessageId(List<String> messageId) {
        if(messageId != null)
            this.messageId = messageId;
    }

    public List<Integer> getFriends() {
        return friends;
    }

    public void setFriends(List<Integer> friends) {
        if(friends != null)
            this.friends = friends;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public UserDTO() {}


    public void setToken(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", friends=" + friends +
                ", messageId=" + messageId +
                '}';
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public UserDTO(Integer id, String username, String email , List<Integer> friends, List<String> messageId, String upiId ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.friends = friends;
        this.messageId = messageId;
        this.upiId = upiId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
