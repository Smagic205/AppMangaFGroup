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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.VoucherAdapter;
import com.example.bookapp.Model.Voucher;
import com.example.bookapp.R;
import com.example.bookapp.ViewModel.SelectVoucherViewModel;

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

    private SelectVoucherViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(SelectVoucherViewModel.class);

        viewModel.getVouchers().observe(this, vouchers -> {
            voucherList.clear();
            if (vouchers != null) voucherList.addAll(vouchers);
            adapter.notifyDataSetChanged();
        });

        viewModel.getValidatedVoucher().observe(this, voucher -> {
            if (voucher != null) {
                Toast.makeText(this, "Áp dụng mã " + voucher.getCode() + " thành công",
                        Toast.LENGTH_SHORT).show();
            } else {
                // null được emit khi validate trả về không hợp lệ
                // Chỉ hiển thị lỗi nếu đã chủ động validate (tránh toast khi mới mở màn)
            }
        });

        viewModel.loadAvailableVouchers();

        tvApplyCode.setOnClickListener(v -> applyManualCode());
        btnConfirm.setOnClickListener(v -> confirmSelection());
    }

    private void applyManualCode() {
        String code = etVoucherCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) return;

        viewModel.validateCode(code);
        // Kết quả sẽ được observe ở trên: null = không hợp lệ, non-null = hợp lệ
        // Hiển thị thông báo lỗi ngay tại đây để UX tốt hơn
        viewModel.getValidatedVoucher().observe(this, voucher -> {
            if (voucher == null) {
                Toast.makeText(this, "Mã voucher không hợp lệ hoặc đã hết hạn",
                        Toast.LENGTH_SHORT).show();
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
