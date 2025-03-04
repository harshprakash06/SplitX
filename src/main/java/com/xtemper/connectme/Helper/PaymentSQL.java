package com.xtemper.connectme.Helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtemper.connectme.Database.MySql;
import java.sql.Connection;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSQL {
    public List<String> getPayments(int userID) {
        Connection connection = MySql.runQuery();
        List<String> payments = new ArrayList<>();
        try {
            assert connection != null;
            PreparedStatement pst = connection.prepareStatement("Select payment_data from userDetails where userID=?");
            pst.setInt(1, userID);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String jsonPayments = rs.getString("payment_data");
                ObjectMapper mapper = new ObjectMapper();
                payments = mapper.readValue(jsonPayments, new TypeReference<>() {
                });
            }
        } catch (Exception ignored) {
        }
        return payments;
    }

    public HttpStatus addPayment(int userID, String paymentData) {
        Connection connection = MySql.runQuery();
        assert connection != null;
        try {
            PreparedStatement pst = connection.prepareStatement("UPDATE userDetails SET payment_data= JSON_ARRAY_APPEND(IFNULL(payment_data, JSON_ARRAY()), '$', ?) WHERE userID=?");
            pst.setString(1, paymentData);
            pst.setInt(2, userID);
            pst.executeUpdate();
            return HttpStatus.OK;
        } catch (Exception ignored) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}

