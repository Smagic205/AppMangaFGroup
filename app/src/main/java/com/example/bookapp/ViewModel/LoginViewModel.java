package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Utils.FirebaseUtils;

/**
 * ViewModel cho LoginActivity.
 * Giữ trạng thái đăng nhập qua xoay màn hình, tránh gọi lại Firebase Auth không cần thiết.
 */
public class LoginViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getLoginSuccess() { return _loginSuccess; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    /**
     * Gọi Firebase Auth signInWithEmailAndPassword.
     * Kết quả được post vào LiveData để Activity observe và cập nhật UI.
     */
    public void login(String email, String password) {
        FirebaseUtils.getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> _loginSuccess.setValue(true))
                .addOnFailureListener(e -> _errorMessage.setValue(e.getMessage()));
    }

    /**
     * Gọi Firebase Auth signInWithCredential cho đăng nhập Google.
     */
    public void loginWithGoogle(com.google.firebase.auth.AuthCredential credential) {
        FirebaseUtils.getAuth().signInWithCredential(credential)
                .addOnSuccessListener(authResult -> _loginSuccess.setValue(true))
                .addOnFailureListener(e -> _errorMessage.setValue(e.getMessage()));
    }
}
