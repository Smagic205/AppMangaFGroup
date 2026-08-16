package com.example.bookapp.Repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bookapp.Model.Order;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseCallback;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lớp DUY NHẤT được phép gọi Firestore cho collection "orders".
 * Dùng bởi CheckoutActivity (tạo đơn), OrderHistoryActivity (danh sách + lọc),
 * OrderDetailActivity (chi tiết + hủy đơn).
 */
public class OrderRepository {

    /** Tạo đơn hàng mới. Trả về orderId vừa tạo qua callback để Checkout điều hướng tiếp. */
    public void createOrder(Order order, FirebaseCallback<String> callback) {
        String orderId = UUID.randomUUID().toString();
        order.setOrderId(orderId);

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .set(order)
                .addOnSuccessListener(unused -> callback.onSuccess(orderId))
                .addOnFailureListener(callback::onFailure);
    }

    public LiveData<Order> getOrder(String orderId) {
        MutableLiveData<Order> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .get()
                .addOnSuccessListener(doc -> {
                    Order order = doc.toObject(Order.class);
                    if (order != null) order.setOrderId(doc.getId());
                    liveData.setValue(order);
                })
                .addOnFailureListener(e -> liveData.setValue(null));

        return liveData;
    }

    /**
     * Danh sách đơn của 1 user, lọc theo trạng thái nếu có (dùng cho Tab trong
     * OrderHistoryActivity). status = null hoặc "all" -> lấy toàn bộ đơn.
     */
    public LiveData<List<Order>> getOrdersByUser(String uid, @Nullable String status) {
        MutableLiveData<List<Order>> liveData = new MutableLiveData<>();

        // Lấy tất cả đơn hàng của userId, bỏ qua .orderBy và .whereEqualTo("orderStatus")
        // để tránh lỗi FAILED_PRECONDITION do thiếu composite index trên Firestore.
        Query query = FirebaseUtils.getFirestore().collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("userId", uid);

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Order> orders = new ArrayList<>();
                    querySnapshot.forEach(doc -> {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        
                        // Lọc theo trạng thái trên máy khách (client-side)
                        boolean isAllTab = status == null || "all".equals(status);
                        if (isAllTab) {
                            String orderStatus = order.getOrderStatus();
                            if (!"cancelled".equals(orderStatus) && !"delivered".equals(orderStatus)) {
                                orders.add(order);
                            }
                        } else if (status.equals(order.getOrderStatus())) {
                            orders.add(order);
                        }
                    });
                    
                    // Sắp xếp theo ngày tạo mới nhất (descending) trên máy khách
                    orders.sort((o1, o2) -> {
                        if (o1.getCreatedAt() != null && o2.getCreatedAt() != null) {
                            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                        }
                        return 0;
                    });
                    
                    liveData.setValue(orders);
                })
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    public void updateOrderStatus(String orderId, String newStatus, FirebaseCallback<Void> callback) {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .update("orderStatus", newStatus)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteOrder(String orderId, FirebaseCallback<Void> callback) {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
