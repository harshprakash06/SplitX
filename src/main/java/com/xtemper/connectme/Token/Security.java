package com.xtemper.connectme.Token;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtemper.connectme.DTO.UserDTO;
import com.xtemper.connectme.Database.MySql;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class Security {
    private static final String AES_ALGORITHM = "AES";
    private static final String secretKey = ""; //generate security strings
    private static final String passwordKey ="";
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public String encrypt(UserDTO user) {
        try {
            String token = UUID.randomUUID().toString();
            user.setToken(token);
            user.setExpireTime(System.currentTimeMillis() + (24 * 60 * 60 * 1000));

            String json = objectMapper.writeValueAsString(user);

            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encryptedBytes = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public static UserDTO decryptObject(String encryptedData) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);


            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);


            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            String json = new String(decryptedBytes, StandardCharsets.UTF_8);

            UserDTO user = objectMapper.readValue(json, UserDTO.class);

            if (user == null) {
                System.out.println("Invalid or malformed token.");
                return null;
            }
            Connection con = MySql.runQuery();
            assert con != null;
            PreparedStatement pst = con.prepareStatement("select * from userDetails where userId=?");
            pst.setInt(1,user.getId());
            ResultSet res = pst.executeQuery();
            if (res.next()) {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonData = res.getString(6);
                if(jsonData != null){
                    ArrayList<String> messageList = objectMapper.readValue(jsonData, new TypeReference<ArrayList<String>>() {});
                    System.out.println(messageList);
                    user.setMessageId(messageList);
                }
                jsonData = res.getString(5);
                if(jsonData != null){
                    ArrayList<Integer> friendsList = objectMapper.readValue(jsonData, new TypeReference<ArrayList<Integer>>() {});
                    user.setFriends(friendsList);
                }


            }
            return user;
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return null;
        }
    }


    public  String passEncryption(String password){
        try{
            byte[] keyBytes = Base64.getDecoder().decode(passwordKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        }catch (Exception ignored){ return "";}
    }

    public static String passDecryption(String encryptPassword){
        try{
            byte[] keyBytes = Base64.getDecoder().decode(passwordKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptPassword)); // FIXED
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        }catch(Exception ignored){ return "";}
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Security security = new Security();
        ArrayList<Integer> arr = new ArrayList<Integer>();
        ArrayList<String> arr2 = new ArrayList<String>();
        arr.add(22);
        arr.add(23);
        arr.add(221);
        arr2.add("afshf");
        arr2.add("fsaf");
        UserDTO user = new UserDTO(8,"harsh","harsh@1223",arr,arr2,null);
        Security sec = new Security();

        UserDTO decryptedData = decryptObject("LZFUH7jds/hUkTn9yxmqpfXP5qxDKB7f8sDkraGRwgsJgkHbDX3oHaqUJTQiMxxpxkE/LDgjXb3WouxEOt7OIJsegcay9V4jGJmVxWcsji+/+g33MEnPDGEVJQFTGNiEDgzYE12//Yhl+YO02YsqN245BOdYuvQY8JM7VFZ7s" +
                "pKrmmTki7P3T1+EnmXmJhjjHyimWN3TvgJP76KrZ3syrA==");
        System.out.println(decryptedData);
    }
}