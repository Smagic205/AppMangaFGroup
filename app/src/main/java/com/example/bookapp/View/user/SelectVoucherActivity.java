package com.example.bookapp.View.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.VoucherAdapter;
import com.example.bookapp.Model.Voucher;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Mở từ CheckoutActivity (ll_select_voucher). Trả về mã voucher đã chọn
 * qua setResult() để CheckoutActivity tính lại discountAmount / finalTotal.
 */
public class SelectVoucherActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_VOUCHER_CODE = "extra_selected_voucher_code";

    private EditText etVoucherCode;
    private TextView tvApplyCode;
    private RecyclerView rvVouchers;
    private Button btnConfirm;

    private VoucherAdapter adapter;
    private final List<Voucher> voucherList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_voucher);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etVoucherCode = findViewById(R.id.et_voucher_code);
        tvApplyCode = findViewById(R.id.tv_apply_code);
        rvVouchers = findViewById(R.id.rv_vouchers);
        btnConfirm = findViewById(R.id.btn_confirm_voucher);

        rvVouchers.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new VoucherAdapter(voucherList, voucher -> {
            // Đồng bộ ô nhập tay với voucher vừa chọn trong danh sách
            etVoucherCode.setText(voucher.getCode());
        });
        rvVouchers.setAdapter(adapter);

        loadAvailableVouchers();

        tvApplyCode.setOnClickListener(v -> applyManualCode());
        btnConfirm.setOnClickListener(v -> confirmSelection());
    }

    private void loadAvailableVouchers() {
        Timestamp now = Timestamp.now();

        FirebaseUtils.getFirestore().collection("vouchers")
                .whereEqualTo("isActive", true)
                .whereGreaterThanOrEqualTo("endDate", now)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    voucherList.clear();
                    querySnapshot.forEach(doc -> {
                        Voucher voucher = doc.toObject(Voucher.class);
                        voucher.setVoucherId(doc.getId());
                        voucherList.add(voucher);
                    });
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được danh sách voucher", Toast.LENGTH_SHORT).show());
    }

    private void applyManualCode() {
        String code = etVoucherCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) return;

        FirebaseUtils.getFirestore().collection("vouchers")
                .whereEqualTo("code", code)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Mã voucher không hợp lệ hoặc đã hết hạn", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Áp dụng mã " + code + " thành công", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmSelection() {
        String code = etVoucherCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Vui lòng chọn hoặc nhập 1 mã giảm giá", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SELECTED_VOUCHER_CODE, code);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
