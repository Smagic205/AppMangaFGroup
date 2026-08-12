package com.example.bookapp.ViewModel;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.User;
import com.example.bookapp.Repository.StorageRepository;
import com.example.bookapp.Repository.UserRepository;
import com.example.bookapp.Utils.FirebaseCallback;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel cho EditProfileActivity.
 * Tách load user hiện tại + update Firestore + upload avatar Storage ra khỏi Activity.
 * Upload ảnh đi qua StorageRepository (không thuộc Firestore thuần túy).
 */
public class EditProfileViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final StorageRepository storageRepository = new StorageRepository();

    private final MutableLiveData<User> _user = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<User> getUser() { return _user; }
    public LiveData<Boolean> getSaveSuccess() { return _saveSuccess; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public void loadUser(String uid) {
        userRepository.getUser(uid).observeForever(user -> _user.setValue(user));
    }

    /**
     * Lưu hồ sơ người dùng.
     * Nếu có ảnh mới (avatarUri != null): upload lên Storage trước, lấy URL rồi update Firestore.
     * Nếu không có ảnh mới: update Firestore thẳng (không thay đổi avatarUrl hiện tại).
     */
    public void saveProfile(String uid, String fullName, String phone,
                            String gender, Timestamp birthday,
                            @Nullable Uri avatarUri) {
        if (avatarUri != null) {
            storageRepository.uploadAvatar(uid, avatarUri, new FirebaseCallback<String>() {
                @Override
                public void onSuccess(String downloadUrl) {
                    updateFirestore(uid, fullName, phone, gender, birthday, downloadUrl);
                }

                @Override
                public void onFailure(Exception e) {
                    _errorMessage.setValue("Upload ảnh thất bại: " + e.getMessage());
                }
            });
        } else {
            updateFirestore(uid, fullName, phone, gender, birthday, null);
        }
    }

    private void updateFirestore(String uid, String fullName, String phone,
                                  String gender, Timestamp birthday,
                                  @Nullable String avatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("birthday", birthday);
        if (avatarUrl != null) {
            updates.put("avatarUrl", avatarUrl);
        }

        userRepository.updateUser(uid, updates, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                _saveSuccess.setValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.setValue("Lỗi: " + e.getMessage());
            }
        });
    }
}
