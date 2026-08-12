package com.example.bookapp.Utils;

import android.app.Activity;
import android.widget.Toast;

import com.example.bookapp.Model.User;

/**
 * Gọi ở onCreate() của mọi Activity trong package View/admin để chặn
 * user thường (role != "admin") truy cập trái phép, kể cả khi họ cố tình
 * mở màn admin bằng deep link hoặc back-stack.
 *
 * Dùng kèm với Firestore Security Rules ở tầng backend - đây chỉ là lớp
 * chặn UI, không thay thế được rules.
 */
public class RoleChecker {

    public interface OnPermissionResult {
        void onGranted();
    }

    public static void checkAdminPermission(Activity activity, OnPermissionResult onGranted) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) {
            activity.finish();
            return;
        }

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null && user.isAdmin()) {
                        onGranted.onGranted();
                    } else {
                        Toast.makeText(activity, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                })
                .addOnFailureListener(e -> activity.finish());
    }
}
