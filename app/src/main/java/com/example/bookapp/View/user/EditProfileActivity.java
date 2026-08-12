package com.example.bookapp.View.user;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.bookapp.Model.User;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Màn hình chỉnh sửa thông tin cá nhân.
 * Mở từ ProfileFragment khi bấm ib_edit_profile.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private ImageButton ibChangeAvatar;
    private EditText etFullName, etEmail, etPhone;
    private TextView etBirthday;
    private View llSelectBirthday;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private Button btnSave;
    private ProgressBar pbSaving;

    private Uri selectedAvatarUri = null;
    private final Calendar birthdayCalendar = Calendar.getInstance();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(ivAvatar);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        bindViews();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadCurrentUserData();

        ibChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        llSelectBirthday.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void bindViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        ibChangeAvatar = findViewById(R.id.ib_change_avatar);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etBirthday = findViewById(R.id.et_birthday);
        rgGender = findViewById(R.id.rg_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        rbOther = findViewById(R.id.rb_other);
        btnSave = findViewById(R.id.btn_save_profile);
        pbSaving = findViewById(R.id.pb_saving);
        llSelectBirthday = findViewById(R.id.ll_select_birthday);
    }

    /**
     * Đổ dữ liệu user hiện tại lên form. Trong thực tế nên lấy từ
     * UserRepository / UserViewModel thay vì gọi Firestore trực tiếp ở Activity.
     */
    private void loadCurrentUserData() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        FirebaseUtils.getFirestore().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) return;

                    etFullName.setText(user.getFullName());
                    etEmail.setText(user.getEmail());
                    etPhone.setText(user.getPhone());

                    if ("female".equalsIgnoreCase(user.getGender())) {
                        rbFemale.setChecked(true);
                    } else if ("other".equalsIgnoreCase(user.getGender())) {
                        rbOther.setChecked(true);
                    } else {
                        rbMale.setChecked(true);
                    }

                    if (user.getBirthday() != null) {
                        birthdayCalendar.setTime(user.getBirthday().toDate());
                        updateBirthdayText();
                    }

                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(ivAvatar);
                    }
                });
    }

    private void showDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    birthdayCalendar.set(year, month, dayOfMonth);
                    updateBirthdayText();
                },
                birthdayCalendar.get(Calendar.YEAR),
                birthdayCalendar.get(Calendar.MONTH),
                birthdayCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateBirthdayText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etBirthday.setText(sdf.format(birthdayCalendar.getTime()));
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Vui lòng nhập họ tên");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            return;
        }

        String gender = rbFemale.isChecked() ? "female" : rbOther.isChecked() ? "other" : "male";
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        setLoading(true);

        if (selectedAvatarUri != null) {
            // Upload ảnh mới lên Firebase Storage trước, rồi mới cập nhật Firestore
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("avatars/" + uid + ".jpg");

            ref.putFile(selectedAvatarUri)
                    .addOnSuccessListener(taskSnapshot ->
                            ref.getDownloadUrl().addOnSuccessListener(downloadUri ->
                                    updateUserDocument(uid, fullName, phone, gender, downloadUri.toString())))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Upload ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            updateUserDocument(uid, fullName, phone, gender, null);
        }
    }

    private void updateUserDocument(String uid, String fullName, String phone,
                                     String gender, @Nullable String avatarUrl) {
        FirebaseFirestore db = FirebaseUtils.getFirestore();

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("birthday", new Timestamp(birthdayCalendar.getTime()));
        if (avatarUrl != null) {
            updates.put("avatarUrl", avatarUrl);
        }

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Đã cập nhật hồ sơ", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        pbSaving.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}
