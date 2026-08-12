package com.example.bookapp.View.user;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.bookapp.Adapter.user.NotificationAdapter;
import com.example.bookapp.Model.Notification;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private SwipeRefreshLayout srl;
    private RecyclerView rvNotifications;
    private LinearLayout llEmpty;

    private NotificationAdapter adapter;
    private final List<Notification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        srl = findViewById(R.id.srl_notifications);
        rvNotifications = findViewById(R.id.rv_notifications);
        llEmpty = findViewById(R.id.ll_empty);

        rvNotifications.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList, (notification, position) -> {
            if (!notification.isRead()) {
                markAsRead(notification);
            }
            // TODO: điều hướng theo notification.getType() nếu cần
            // (vd type == "order" -> mở OrderDetailActivity)
        });
        rvNotifications.setAdapter(adapter);

        findViewById(R.id.tv_mark_all_read).setOnClickListener(v -> markAllAsRead());

        srl.setOnRefreshListener(this::loadNotifications);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        srl.setRefreshing(true);

        FirebaseUtils.getFirestore().collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    notificationList.clear();
                    querySnapshot.forEach(doc -> {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setNotificationId(doc.getId());
                        notificationList.add(notification);
                    });
                    adapter.notifyDataSetChanged();
                    srl.setRefreshing(false);

                    boolean isEmpty = notificationList.isEmpty();
                    llEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> srl.setRefreshing(false));
    }

    private void markAsRead(Notification notification) {
        notification.setRead(true);
        adapter.notifyDataSetChanged();

        FirebaseUtils.getFirestore().collection("notifications")
                .document(notification.getNotificationId())
                .update("isRead", true);
    }

    private void markAllAsRead() {
        for (Notification notification : notificationList) {
            if (!notification.isRead()) {
                markAsRead(notification);
            }
        }
    }
}
