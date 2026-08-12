package com.example.bookapp.View.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.OrderItemAdapter;
import com.example.bookapp.Model.Order;
import com.example.bookapp.R;
import com.example.bookapp.ViewModel.OrderDetailViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private TextView tvOrderId, tvOrderStatus, tvAddressNamePhone, tvAddressDetail,
            tvTotalPrice, tvShippingFee, tvDiscount, tvFinalTotal, tvNote;
    private RecyclerView rvOrderItems;
    private Button btnCancelOrder, btnReviewOrder;

    private String orderId;

    private OrderDetailViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();

        viewModel = new ViewModelProvider(this).get(OrderDetailViewModel.class);

        viewModel.getOrder().observe(this, order -> {
            if (order != null) bindOrderData(order);
        });

        viewModel.getCancelSuccess().observe(this, success -> {
            // loadOrder đã được gọi lại bên trong ViewModel sau khi hủy thành công
        });

        if (orderId != null) viewModel.loadOrder(orderId);

        btnCancelOrder.setOnClickListener(v -> confirmCancelOrder());
        btnReviewOrder.setOnClickListener(v -> {
            Order currentOrder = viewModel.getOrder().getValue();
            // Mở màn viết đánh giá cho từng sách trong đơn (đơn giản hoá: mở sách đầu tiên)
            if (currentOrder != null && !currentOrder.getItems().isEmpty()) {
                Intent intent = new Intent(this, WriteReviewActivity.class);
                intent.putExtra(WriteReviewActivity.EXTRA_ORDER_ID, orderId);
                intent.putExtra(WriteReviewActivity.EXTRA_BOOK_ID, currentOrder.getItems().get(0).getBookId());
                startActivity(intent);
            }
        });
    }

    private void bindViews() {
        tvOrderId = findViewById(R.id.tv_order_id);
        tvOrderStatus = findViewById(R.id.tv_order_status);
        tvAddressNamePhone = findViewById(R.id.tv_address_name_phone);
        tvAddressDetail = findViewById(R.id.tv_address_detail);
        rvOrderItems = findViewById(R.id.rv_order_items);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        tvShippingFee = findViewById(R.id.tv_shipping_fee);
        tvDiscount = findViewById(R.id.tv_discount);
        tvFinalTotal = findViewById(R.id.tv_final_total);
        tvNote = findViewById(R.id.tv_note);
        btnCancelOrder = findViewById(R.id.btn_cancel_order);
        btnReviewOrder = findViewById(R.id.btn_review_order);

        rvOrderItems.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
    }

    private void bindOrderData(Order order) {
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        tvOrderId.setText("Đơn hàng #" + order.getOrderId());
        tvOrderStatus.setText(mapStatusToLabel(order.getOrderStatus()));

        if (order.getShippingAddress() != null) {
            tvAddressNamePhone.setText(order.getShippingAddress().getName()
                    + "  |  " + order.getShippingAddress().getPhone());
            tvAddressDetail.setText(order.getShippingAddress().getFullAddress());
        }

        rvOrderItems.setAdapter(new OrderItemAdapter(order.getItems()));

        tvTotalPrice.setText(currencyFormat.format(order.getTotalPrice()) + "đ");
        tvShippingFee.setText(currencyFormat.format(order.getShippingFee()) + "đ");
        tvDiscount.setText("-" + currencyFormat.format(order.getDiscountAmount()) + "đ");
        tvFinalTotal.setText(currencyFormat.format(order.getFinalTotal()) + "đ");
        tvNote.setText("Ghi chú: " + (order.getNote() == null || order.getNote().isEmpty()
                ? "Không có" : order.getNote()));

        // Chỉ cho hủy khi đơn còn ở trạng thái đầu, chỉ cho đánh giá khi đã giao xong
        boolean canCancel = "pending".equals(order.getOrderStatus()) || "confirmed".equals(order.getOrderStatus());
        boolean canReview = "delivered".equals(order.getOrderStatus());

        btnCancelOrder.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        btnReviewOrder.setVisibility(canReview ? View.VISIBLE : View.GONE);
    }

    private String mapStatusToLabel(String status) {
        switch (status) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "packing": return "Đang đóng gói";
            case "shipping": return "Đang giao";
            case "delivered": return "Đã giao";
            case "cancelled": return "Đã hủy";
            case "returned": return "Trả hàng";
            default: return status;
        }
    }

    private void confirmCancelOrder() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc muốn hủy đơn hàng này?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> viewModel.cancelOrder(orderId))
                .setNegativeButton("Không", null)
                .show();
    }
}
