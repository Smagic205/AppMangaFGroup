package com.example.bookapp.View.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.bookapp.Model.Address;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;

import java.util.UUID;

public class AddEditAddressActivity extends AppCompatActivity {

    public static final String EXTRA_ADDRESS_ID = "extra_address_id";

    private TextInputEditText etName, etPhone, etDetailAddress;
    private AutoCompleteTextView actvProvince, actvDistrict, actvWard;
    private Button btnSave;

    private String editingAddressId = null; // null = đang thêm mới

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_address);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etDetailAddress = findViewById(R.id.et_detail_address);
        actvProvince = findViewById(R.id.actv_province);
        actvDistrict = findViewById(R.id.actv_district);
        actvWard = findViewById(R.id.actv_ward);
        btnSave = findViewById(R.id.btn_save_address);

        setupProvinceDropdown();

        editingAddressId = getIntent().getStringExtra(EXTRA_ADDRESS_ID);
        if (editingAddressId != null) {
            toolbar.setTitle("Sửa địa chỉ");
            loadExistingAddress(editingAddressId);
        }

        btnSave.setOnClickListener(v -> saveAddress());
    }

    /**
     * TODO: Thay danh sách mẫu này bằng bộ dữ liệu tỉnh/thành đầy đủ
     * (63 tỉnh thành) - có thể lưu sẵn trong 1 file JSON assets/provinces.json.
     */
    private void setupProvinceDropdown() {
        String[] sampleProvinces = {"Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, sampleProvinces);
        actvProvince.setAdapter(adapter);
    }

    private void loadExistingAddress(String addressId) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        FirebaseUtils.getFirestore()
                .collection("users").document(uid)
                .collection("addresses").document(addressId)
                .get()
                .addOnSuccessListener(doc -> {
                    Address address = doc.toObject(Address.class);
                    if (address == null) return;

                    etName.setText(address.getName());
                    etPhone.setText(address.getPhone());
                    actvProvince.setText(address.getProvince(), false);
                    actvDistrict.setText(address.getDistrict(), false);
                    actvWard.setText(address.getWard(), false);
                    etDetailAddress.setText(address.getDetailAddress());
                });
    }

    private void saveAddress() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String province = actvProvince.getText().toString().trim();
        String district = actvDistrict.getText().toString().trim();
        String ward = actvWard.getText().toString().trim();
        String detail = etDetailAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(province)
                || TextUtils.isEmpty(district) || TextUtils.isEmpty(ward) || TextUtils.isEmpty(detail)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        String addressId = editingAddressId != null ? editingAddressId : UUID.randomUUID().toString();
        Address address = new Address(addressId, name, phone, province, district, ward, detail);

        DocumentReference ref = FirebaseUtils.getFirestore()
                .collection("users").document(uid)
                .collection("addresses").document(addressId);

        ref.set(address)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã lưu địa chỉ", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
