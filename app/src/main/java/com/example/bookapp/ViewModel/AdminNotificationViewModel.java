package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Notification;
import com.example.bookapp.Repository.AdminNotificationRepository;
import com.example.bookapp.Utils.FirebaseCallback;

import java.util.List;

/** Dùng cho ManageNotificationActivity. */
public class AdminNotificationViewModel extends ViewModel {

    private final AdminNotificationRepository repository = new AdminNotificationRepository();

    private final MutableLiveData<Boolean> sendSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<Notification>> loadRecentSent(int limit) {
        return repository.getRecentSentNotifications(limit);
    }

    public LiveData<Boolean> getSendSuccess() {
        return sendSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * targetUserId null = gửi broadcast cho TẤT CẢ user (ứng với chip_target_all).
     * type dùng Constants.NOTIF_TYPE_PROMO hoặc NOTIF_TYPE_SYSTEM tùy chip_type đang chọn.
     */
    public void send(String targetUserId, String title, String content, String type) {
        if (title == null || title.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập tiêu đề thông báo");
            return;
        }
        if (content == null || content.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập nội dung thông báo");
            return;
        }

        repository.sendNotification(targetUserId, title.trim(), content.trim(), type,
                new FirebaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        sendSuccess.setValue(true);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        errorMessage.setValue(e.getMessage());
                    }
                });
    }
}
