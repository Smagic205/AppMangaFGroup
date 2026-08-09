package com.example.bookapp.Model;

import com.google.firebase.Timestamp;

/**
 * Model tương ứng bảng "vouchers"
 */
public class Voucher {

    private String voucherId;
    private String code;
    private String type;   // "percent" | "fixed" | "freeship"
    private double value;
    private Timestamp startDate;
    private Timestamp endDate;
    private boolean isActive;

    public Voucher() {
    }

    public Voucher(String voucherId, String code, String type, double value,
                    Timestamp startDate, Timestamp endDate, boolean isActive) {
        this.voucherId = voucherId;
        this.code = code;
        this.type = type;
        this.value = value;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Tính số tiền được giảm dựa trên tổng đơn hàng.
     */
    public double calculateDiscount(double orderTotal) {
        switch (type) {
            case "percent":
                return orderTotal * (value / 100.0);
            case "fixed":
                return value;
            case "freeship":
                return 0; // xử lý riêng ở phần shippingFee
            default:
                return 0;
        }
    }
}
