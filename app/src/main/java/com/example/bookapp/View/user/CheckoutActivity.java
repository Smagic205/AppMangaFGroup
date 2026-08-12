package com.example.bookapp.View.user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.OrderItemAdapter;
import com.example.bookapp.Model.Address;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.CartItem;
import com.example.bookapp.Model.Order;
import com.example.bookapp.Model.OrderItem;
import com.example.bookapp.Model.Voucher;
import com.example.bookapp.R;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.Timestamp;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Mở từ CartFragment với danh sách bookId đã chọn (EXTRA_SELECTED_BOOK_IDS).
 * Đọc lại các CartItem tương ứng từ Firestore + join book info để build OrderItem.
 */
public class CheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_BOOK_IDS = "extra_selected_book_ids";

    private static final double DEFAULT_SHIPPING_FEE = 30000;

    private TextView tvAddressNamePhone, tvAddressDetail, tvSubtotal, tvShippingFee,
            tvDiscount, tvFinalTotal, tvVoucherStatus;
    private LinearLayout llSelectAddress, llSelectVoucher, llPlaceOrderBar;
    private RecyclerView rvCheckoutItems;
    private RadioGroup rgPaymentMethod;
    private EditText etNote;
    private Button btnPlaceOrder;

    private final List<String> selectedBookIds = new ArrayList<>();
    private final List<OrderItem> orderItems = new ArrayList<>();
    private Address selectedAddress;
    private Voucher selectedVoucher;
    private double shippingFee = DEFAULT_SHIPPING_FEE;

    private final ActivityResultLauncher<Intent> selectAddressLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String addressId = result.getData()
                            .getStringExtra(AddressListActivity.EXTRA_SELECTED_ADDRESS);
                    if (addressId != null) loadSelectedAddress(addressId);
                }
            });

    private final ActivityResultLauncher<Intent> selectVoucherLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String code = result.getData()
                            .getStringExtra(SelectVoucherActivity.EXTRA_SELECTED_VOUCHER_CODE);
                    if (code != null) loadVoucherByCode(code);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        List<String> ids = getIntent().getStringArrayListExtra(EXTRA_SELECTED_BOOK_IDS);
        if (ids != null) selectedBookIds.addAll(ids);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setupClicks();
        loadCheckoutItems();
    }

    private void bindViews() {
        tvAddressNamePhone = findViewById(R.id.tv_address_name_phone);
        tvAddressDetail = findViewById(R.id.tv_address_detail);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvShippingFee = findViewById(R.id.tv_shipping_fee);
        tvDiscount = findViewById(R.id.tv_discount);
        tvFinalTotal = findViewById(R.id.tv_final_total);
        tvVoucherStatus = findViewById(R.id.tv_voucher_status);
        llSelectAddress = findViewById(R.id.ll_select_address);
        llSelectVoucher = findViewById(R.id.ll_select_voucher);
        llPlaceOrderBar = findViewById(R.id.ll_place_order_bar);
        rvCheckoutItems = findViewById(R.id.rv_checkout_items);
        rgPaymentMethod = findViewById(R.id.rg_payment_method);
        etNote = findViewById(R.id.et_note);
        btnPlaceOrder = findViewById(R.id.btn_place_order);

        rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClicks() {
        llSelectAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressListActivity.class);
            intent.putExtra(AddressListActivity.EXTRA_SELECT_MODE, true);
            selectAddressLauncher.launch(intent);
        });

        llSelectVoucher.setOnClickListener(v ->
                selectVoucherLauncher.launch(new Intent(this, SelectVoucherActivity.class)));

        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    /**
     * Đọc lại CartItem theo đúng danh sách bookId được chọn từ CartFragment,
     * join thêm thông tin sách để tạo snapshot OrderItem (xem OrderItem.java).
     */
    private void loadCheckoutItems() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null || selectedBookIds.isEmpty()) return;

        int[] remaining = {selectedBookIds.size()};

        for (String bookId : selectedBookIds) {
            FirebaseUtils.getFirestore()
                    .collection("carts").document(uid)
                    .collection(Constants.SUBCOLLECTION_CART_ITEMS).document(bookId)
                    .get()
                    .addOnSuccessListener(cartDoc -> {
                        CartItem cartItem = cartDoc.toObject(CartItem.class);
                        if (cartItem == null) {
                            checkAllLoaded(remaining);
                            return;
                        }

                        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                                .document(bookId)
                                .get()
                                .addOnSuccessListener(bookDoc -> {
                                    Book book = bookDoc.toObject(Book.class);
                                    if (book != null) {
                                        orderItems.add(new OrderItem(
                                                bookId, book.getTitle(), book.getCoverImageUrl(),
                                                cartItem.getPriceAtAdd(), cartItem.getQuantity()));
                                    }
                                    checkAllLoaded(remaining);
                                });
                    });
        }
    }

    private void checkAllLoaded(int[] remaining) {
        remaining[0]--;
        if (remaining[0] <= 0) {
            rvCheckoutItems.setAdapter(new OrderItemAdapter(orderItems));
            recalculateTotal();
        }
    }

    private void loadSelectedAddress(String addressId) {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        FirebaseUtils.getFirestore()
                .collection("users").document(uid)
                .collection(Constants.SUBCOLLECTION_ADDRESSES).document(addressId)
                .get()
                .addOnSuccessListener(doc -> {
                    Address address = doc.toObject(Address.class);
                    if (address == null) return;

                    address.setAddressId(doc.getId());
                    selectedAddress = address;
                    tvAddressNamePhone.setText(address.getName() + "  |  " + address.getPhone());
                    tvAddressDetail.setText(address.getFullAddress());
                });
    }

    private void loadVoucherByCode(String code) {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_VOUCHERS)
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) return;
                    Voucher voucher = querySnapshot.getDocuments().get(0).toObject(Voucher.class);
                    if (voucher == null) return;

                    voucher.setVoucherId(querySnapshot.getDocuments().get(0).getId());
                    selectedVoucher = voucher;
                    tvVoucherStatus.setText("Đã áp dụng mã " + voucher.getCode());
                    if (Constants.VOUCHER_FREESHIP.equals(voucher.getType())) {
                        shippingFee = 0;
                    }
                    recalculateTotal();
                });
    }

    private double getSubtotal() {
        double subtotal = 0;
        for (OrderItem item : orderItems) {
            subtotal += item.getLineTotal();
        }
        return subtotal;
    }

    private void recalculateTotal() {
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        double subtotal = getSubtotal();
        double discount = selectedVoucher != null ? selectedVoucher.calculateDiscount(subtotal) : 0;
        double finalTotal = subtotal + shippingFee - discount;

        tvSubtotal.setText(currencyFormat.format(subtotal) + "đ");
        tvShippingFee.setText(currencyFormat.format(shippingFee) + "đ");
        tvDiscount.setText("-" + currencyFormat.format(discount) + "đ");
        tvFinalTotal.setText(currencyFormat.format(Math.max(finalTotal, 0)) + "đ");
    }

    private void placeOrder() {
        if (selectedAddress == null) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (orderItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        int selectedPaymentId = rgPaymentMethod.getCheckedRadioButtonId();
        // TODO: map selectedPaymentId (rb_cod / rb_bank_transfer / rb_momo) sang
        // phương thức thanh toán thật nếu cần lưu field paymentMethod riêng.

        double subtotal = getSubtotal();
        double discount = selectedVoucher != null ? selectedVoucher.calculateDiscount(subtotal) : 0;
        double finalTotal = Math.max(subtotal + shippingFee - discount, 0);

        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId, uid, orderItems, subtotal, shippingFee, discount,
                selectedVoucher != null ? selectedVoucher.getCode() : null,
                finalTotal, Constants.ORDER_PENDING, Constants.ORDER_PENDING,
                selectedAddress, etNote.getText().toString().trim(), Timestamp.now()
        );

        btnPlaceOrder.setEnabled(false);

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_ORDERS).document(orderId)
                .set(order)
                .addOnSuccessListener(unused -> {
                    clearPurchasedCartItems(uid);
                    Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OrderDetailActivity.class);
                    intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPlaceOrder.setEnabled(true);
                    Toast.makeText(this, "Lỗi đặt hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearPurchasedCartItems(String uid) {
        for (String bookId : selectedBookIds) {
            FirebaseUtils.getFirestore()
                    .collection("carts").document(uid)
                    .collection(Constants.SUBCOLLECTION_CART_ITEMS).document(bookId)
                    .delete();
        }
    }
}
