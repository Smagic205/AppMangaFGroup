package com.example.bookapp.View.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.admin.AdminDashboardMenuAdapter;
import com.example.bookapp.Adapter.admin.AdminDashboardStatAdapter;
import com.example.bookapp.Adapter.admin.AdminOrderMiniAdapter;
import com.example.bookapp.Adapter.admin.AdminTopBookAdapter;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.example.bookapp.Utils.ImageUtils;
import com.example.bookapp.Utils.SessionManager;
import com.example.bookapp.ViewModel.AdminDashboardViewModel;
import com.example.bookapp.View.user.LoginActivity;

public class AdminDashboardActivity extends AdminBaseActivity {

    private AdminDashboardViewModel viewModel;

    private AdminDashboardStatAdapter statAdapter;
    private AdminDashboardMenuAdapter menuAdapter;
    private AdminOrderMiniAdapter recentOrderAdapter;
    private AdminTopBookAdapter topBookAdapter;

    @Override
    protected void onAdminAccessGranted(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.admin_activity_admin_dashboard);

        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);

        setupHeader();
        setupRecyclerViews();
        observeViewModel();

        viewModel.loadStats();
    }

    private void setupHeader() {
        ImageView ivAvatar = findViewById(R.id.iv_detail_avatar);
        TextView tvName = findViewById(R.id.tv_admin_name);
        SessionManager session = new SessionManager(this);
        tvName.setText(session.getUserName());
        ImageUtils.loadAvatar(ivAvatar, session.getUserAvatar());

        findViewById(R.id.fl_notification).setOnClickListener(v -> {
            // TODO: mở NotificationActivity dùng chung với User khi có, hoặc
            // tạo màn thông báo riêng cho Admin nếu nghiệp vụ khác nhau đủ nhiều.
        });

        findViewById(R.id.fl_logout).setOnClickListener(v -> logout());

        findViewById(R.id.tv_view_all_orders).setOnClickListener(v ->
                startActivity(new Intent(this, ManageOrderActivity.class)));
    }

    private void logout() {
        FirebaseUtils.signOut();
        new SessionManager(this).clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerViews() {
        RecyclerView rvStats = findViewById(R.id.rv_stat_cards);
        statAdapter = new AdminDashboardStatAdapter();
        rvStats.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvStats.setAdapter(statAdapter);

        RecyclerView rvMenu = findViewById(R.id.rv_management_grid);
        menuAdapter = new AdminDashboardMenuAdapter(this::openMenuTarget);
        rvMenu.setLayoutManager(new GridLayoutManager(this, 3));
        rvMenu.setAdapter(menuAdapter);
        menuAdapter.setItems(viewModel.getMenuItems());

        RecyclerView rvRecentOrders = findViewById(R.id.rv_recent_orders);
        recentOrderAdapter = new AdminOrderMiniAdapter(order -> {
            Intent intent = new Intent(this, AdminOrderDetailActivity.class);
            intent.putExtra(com.example.bookapp.Utils.Constants.EXTRA_ORDER_ID, order.getOrderId());
            startActivity(intent);
        });
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(this));
        rvRecentOrders.setAdapter(recentOrderAdapter);

        RecyclerView rvTopBooks = findViewById(R.id.rv_top_books);
        topBookAdapter = new AdminTopBookAdapter(book -> {
            Intent intent = new Intent(this, AddEditBookActivity.class);
            intent.putExtra(com.example.bookapp.Utils.Constants.EXTRA_BOOK_ID, book.getBookId());
            intent.putExtra(com.example.bookapp.Utils.Constants.EXTRA_MODE_EDIT, true);
            startActivity(intent);
        });
        rvTopBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTopBooks.setAdapter(topBookAdapter);
    }

    private void observeViewModel() {
        viewModel.getStatCards().observe(this, statAdapter::setItems);
        viewModel.getRecentOrders().observe(this, recentOrderAdapter::setItems);
        viewModel.getTopSellingBooks().observe(this, topBookAdapter::setItems);
    }

    /** Điều hướng theo targetKey của AdminDashboardMenuItem (xem AdminDashboardViewModel.getMenuItems()). */
    private void openMenuTarget(com.example.bookapp.Adapter.admin.AdminDashboardMenuItem item) {
        Intent intent;
        switch (item.getTargetKey()) {
            case "manage_book":
                intent = new Intent(this, ManageBookActivity.class);
                break;
            case "manage_category":
                intent = new Intent(this, ManageCategoryActivity.class);
                break;
            case "manage_author":
                intent = new Intent(this, ManageAuthorActivity.class);
                break;
            case "manage_publisher":
                intent = new Intent(this, ManagePublisherActivity.class);
                break;
            case "manage_order":
                intent = new Intent(this, ManageOrderActivity.class);
                break;
            case "manage_user":
                intent = new Intent(this, ManageUserActivity.class);
                break;
            case "manage_voucher":
                intent = new Intent(this, ManageVoucherActivity.class);
                break;
            case "manage_notification":
                intent = new Intent(this, ManageNotificationActivity.class);
                break;
            case "statistic":
                intent = new Intent(this, StatisticActivity.class);
                break;
            default:
                return;
        }
        startActivity(intent);
    }
}
