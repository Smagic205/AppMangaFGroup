package com.example.bookapp.View.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.bookapp.R;


import com.example.bookapp.Utils.DataSeeder;
import com.example.bookapp.Utils.FirebaseCallback;


import com.example.bookapp.Utils.FirebaseUtils;
import com.example.bookapp.ViewModel.LoginViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar pbLogin;

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Seed data 1 lần duy nhất, dùng SharedPreferences để cờ đánh dấu
//        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
//        boolean isSeeded = prefs.getBoolean("is_data_seeded", false);
//        if (!isSeeded) {
//            DataSeeder.seedFullCatalog(new FirebaseCallback<Void>() {
//                @Override
//                public void onSuccess(Void result) {
//                    prefs.edit().putBoolean("is_data_seeded", true).apply();
//                    Toast.makeText(getApplicationContext(), "Seed Data Success!", Toast.LENGTH_LONG).show();
//                }
//
//                @Override
//                public void onFailure(Exception e) {
//                    Toast.makeText(getApplicationContext(), "Seed Data Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                }
//            });
//        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        pbLogin = findViewById(R.id.pb_login);

        TextView tvRegister = findViewById(R.id.tv_register);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.getLoginSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                setLoading(false);
                navigateByRole();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                setLoading(false);
                Toast.makeText(this, "Đăng nhập thất bại, vui lòng kiểm tra lại", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogin.setOnClickListener(v -> doLogin());

        // TODO: btn_google_login khi wiring Google Sign-In thật
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Nếu đã đăng nhập từ trước (token còn hiệu lực) -> vào thẳng Home
        if (FirebaseUtils.isLoggedIn()) {
            navigateByRole();
        }
    }

    private void doLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        setLoading(true);
        viewModel.login(email, password);
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        pbLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }



    /**
     * Sau khi xác thực Firebase thành công (login mới HOẶC session cũ còn hiệu lực ở
     * onStart()), kiểm tra role trước khi quyết định vào HomeActivity (User) hay
     * AdminDashboardActivity (Admin) — đúng thiết kế "1 project, phân luồng bằng role".
     *
     * Dùng RoleChecker.checkRoleSilently() (đã merge từ phía Admin vào RoleChecker.java
     * trước đó) nên KHÔNG cần sửa LoginViewModel — chỉ cần UID hiện tại sau khi
     * FirebaseAuth xác thực xong, hàm này tự query Firestore lấy field role.
     */
    private void navigateByRole() {
        com.example.bookapp.Utils.RoleChecker.checkRoleSilently(this, isAdmin -> {
            if (isAdmin) {
                goToAdminDashboard();
            } else {
                goToHome();
            }
        });
    }

    private void goToAdminDashboard() {
        Intent intent = new Intent(this, com.example.bookapp.View.admin.AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
