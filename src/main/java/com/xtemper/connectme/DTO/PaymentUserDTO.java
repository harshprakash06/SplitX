package com.xtemper.connectme.DTO;


public class PaymentUserDTO {
    private Integer userId;
    private String paymentId;
    private Double amount;
    private int receiver;
    private String messageId;
    private PaymentStatus status;
    private long time;

    public PaymentUserDTO(Integer userId,String paymentId, Double amount, int receiver, String messageId, int statusCode,long time) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.amount = amount;
        this.receiver = receiver;
        this.messageId = messageId;
        this.status = new PaymentStatus();
        this.status.setPaymentStatus(statusCode);
        this.time = time;
    }

    // Default constructor
    public PaymentUserDTO() {
        this.status = new PaymentStatus();
    }

    // Getters and Setters
    public long getTime() {
        return time;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    // Returns the status as a String (for example, "PENDING")
    public String getStatus() {
        return status.getPaymentStatus();
    }

    // Setter that accepts an integer status code
    public void setStatus(int statusCode) {
        if (this.status == null) {
            this.status = new PaymentStatus();
        }
        this.status.setPaymentStatus(statusCode);
    }
}

// Payment status enum for clarity and type-safety.
enum PaymentStatusEnum {
    PENDING,
    PENDING_VERIFICATION,
    VERIFIED
}

class PaymentStatus {
    private PaymentStatusEnum paymentStatus;

    // Sets the status based on an integer code.
    public void setPaymentStatus(int status) {
        switch (status) {
            case 1 -> this.paymentStatus = PaymentStatusEnum.PENDING;
            case 2 -> this.paymentStatus = PaymentStatusEnum.PENDING_VERIFICATION;
            case 3 -> this.paymentStatus = PaymentStatusEnum.VERIFIED;
            default -> throw new IllegalArgumentException("Invalid status code: " + status);
        }
    }

    // Retrieves the status as a String; returns "UNKNOWN" if the status hasn't been set.
    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus.name() : "UNKNOWN";
    }
}
