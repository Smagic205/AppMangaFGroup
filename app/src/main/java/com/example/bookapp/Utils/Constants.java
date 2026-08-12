package com.example.bookapp.Utils;

public class Constants {

    // Tên collection Firestore - sửa 1 chỗ duy nhất nếu sau này đổi tên bảng
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_BOOKS = "books";
    public static final String COLLECTION_CATEGORIES = "categories";
    public static final String COLLECTION_AUTHORS = "authors";
    public static final String COLLECTION_PUBLISHERS = "publishers";
    public static final String COLLECTION_ORDERS = "orders";
    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_VOUCHERS = "vouchers";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String SUBCOLLECTION_ADDRESSES = "addresses";
    public static final String SUBCOLLECTION_CART_ITEMS = "items";

    // Giá trị field "role" trong bảng users
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    // Giá trị field "orderStatus" trong bảng orders
    public static final String ORDER_PENDING = "pending";
    public static final String ORDER_CONFIRMED = "confirmed";
    public static final String ORDER_PACKING = "packing";
    public static final String ORDER_SHIPPING = "shipping";
    public static final String ORDER_DELIVERED = "delivered";
    public static final String ORDER_CANCELLED = "cancelled";
    public static final String ORDER_RETURNED = "returned";

    // Giá trị field "type" trong bảng notifications
    public static final String NOTIF_TYPE_ORDER = "order";
    public static final String NOTIF_TYPE_PROMO = "promo";
    public static final String NOTIF_TYPE_SYSTEM = "system";

    // Giá trị field "type" trong bảng vouchers
    public static final String VOUCHER_PERCENT = "percent";
    public static final String VOUCHER_FIXED = "fixed";
    public static final String VOUCHER_FREESHIP = "freeship";
}
