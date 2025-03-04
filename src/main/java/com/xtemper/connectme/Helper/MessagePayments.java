package com.xtemper.connectme.Helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagePayments{
    int user;
    HashMap<String, Long> payments = new HashMap<>();
    public MessagePayments() {}
    public MessagePayments(int user, HashMap<String, Long>   payments) { this.user = user; this.payments = payments;}
    public void setPayments(HashMap<String, Long>   payments) {
        this.payments = payments;
    }
    public int getUser() {return user;}
    public HashMap<String, Long>   getPayments() { return payments;}
    public void setUser(int user) { this.user = user;}

}
