package com.example.bookapp.Utils;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Tránh gọi lặp FirebaseFirestore.getInstance() / FirebaseAuth.getInstance()
 * ở mọi Activity/Repository - gọi qua đây cho gọn và dễ thay đổi sau này
 * (vd bật offline persistence, đổi region...).
 */
public class FirebaseUtils {

    private static FirebaseFirestore firestoreInstance;

    public static FirebaseFirestore getFirestore() {
        if (firestoreInstance == null) {
            firestoreInstance = FirebaseFirestore.getInstance();
        }
        return firestoreInstance;
    }

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    @Nullable
    public static String getCurrentUserId() {
        FirebaseUser user = getAuth().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static boolean isLoggedIn() {
        return getAuth().getCurrentUser() != null;
    }
}
