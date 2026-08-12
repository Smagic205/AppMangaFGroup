package com.example.bookapp.View.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private Button btnSendReset;
    private ProgressBar pbSending;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.et_email);
        btnSendReset = findViewById(R.id.btn_send_reset);
        pbSending = findViewById(R.id.pb_sending);

        findViewById(R.id.ib_back).setOnClickListener(v -> finish());
        findViewById(R.id.tv_back_to_login).setOnClickListener(v -> finish());

        btnSendReset.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            return;
        }

        setLoading(true);

        FirebaseUtils.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Đã gửi liên kết đặt lại mật khẩu tới " + email,
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        pbSending.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendReset.setEnabled(!loading);
    }
}
