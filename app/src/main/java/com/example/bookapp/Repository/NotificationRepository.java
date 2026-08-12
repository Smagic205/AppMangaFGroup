package com.example.bookapp.Repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bookapp.Model.Notification;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseCallback;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DUY NHẤT được phép gọi Firestore cho collection "notifications".
 * Dùng bởi NotificationActivity.
 */
public class NotificationRepository {

    public LiveData<List<Notification>> getNotifications(String uid) {
        MutableLiveData<List<Notification>> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> notifications = new ArrayList<>();
                    querySnapshot.forEach(doc -> {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setNotificationId(doc.getId());
                        notifications.add(notification);
                    });
                    liveData.setValue(notifications);
                })
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    public void markAsRead(String notificationId, FirebaseCallback<Void> callback) {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /** Đánh dấu nhiều thông báo đã đọc cùng lúc - dùng cho nút "Đánh dấu tất cả đã đọc". */
    public void markAllAsRead(List<String> notificationIds, FirebaseCallback<Void> callback) {
        if (notificationIds.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        WriteBatch batch = FirebaseUtils.getFirestore().batch();
        for (String id : notificationIds) {
            batch.update(FirebaseUtils.getFirestore()
                    .collection(Constants.COLLECTION_NOTIFICATIONS).document(id), "isRead", true);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
