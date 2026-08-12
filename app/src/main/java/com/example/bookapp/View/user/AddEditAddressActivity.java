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
import androidx.lifecycle.ViewModelProvider;

import com.example.bookapp.Model.Address;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.example.bookapp.ViewModel.AddEditAddressViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

public class AddEditAddressActivity extends AppCompatActivity {

    public static final String EXTRA_ADDRESS_ID = "extra_address_id";

    private TextInputEditText etName, etPhone, etDetailAddress;
    private AutoCompleteTextView actvProvince, actvDistrict, actvWard;
    private Button btnSave;

    private String editingAddressId = null; // null = đang thêm mới

    private AddEditAddressViewModel viewModel;

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
        }

        viewModel = new ViewModelProvider(this).get(AddEditAddressViewModel.class);

        // Observe địa chỉ hiện có (chỉ dùng khi ở chế độ sửa)
        viewModel.getAddress().observe(this, address -> {
            if (address == null) return;
            etName.setText(address.getName());
            etPhone.setText(address.getPhone());
            actvProvince.setText(address.getProvince(), false);
            actvDistrict.setText(address.getDistrict(), false);
            actvWard.setText(address.getWard(), false);
            etDetailAddress.setText(address.getDetailAddress());
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Đã lưu địa chỉ", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Load địa chỉ hiện có nếu đang ở chế độ sửa
        if (editingAddressId != null) {
            String uid = FirebaseUtils.getCurrentUserId();
            if (uid != null) viewModel.loadAddress(uid, editingAddressId);
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

        viewModel.saveAddress(uid, address);
    }
}
