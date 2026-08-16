package com.example.bookapp.View.admin;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.admin.AdminStatRankAdapter;
import com.example.bookapp.R;
import com.example.bookapp.Utils.PriceFormatter;
import com.example.bookapp.ViewModel.AdminStatisticViewModel;
import com.google.android.material.chip.ChipGroup;

public class StatisticActivity extends AdminBaseActivity {

    private AdminStatisticViewModel viewModel;
    private AdminStatRankAdapter topBooksAdapter;
    private AdminStatRankAdapter topCustomersAdapter;

    private android.widget.TextView tvRevenue, tvOrderCount, tvAov;

    @Override
    protected void onAdminAccessGranted(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.admin_activity_statistic);

        viewModel = new ViewModelProvider(this).get(AdminStatisticViewModel.class);

        setupToolbar(findViewById(R.id.tb_toolbar), "Thống kê chi tiết");
        bindSummaryViews();
        setupPeriodChips();
        setupRecyclerViews();
        observeViewModel();

        // Doanh thu/số đơn/TB theo kỳ mặc định (Tuần, đã checked=true sẵn trong layout).
        viewModel.loadRevenue(AdminStatisticViewModel.Period.WEEK);
        // Top sách/khách hàng là số liệu TOÀN THỜI GIAN, không phụ thuộc chip kỳ — chỉ tải 1 lần.
        viewModel.loadTopBooks(10);
        viewModel.loadTopCustomers(10);
    }

    private void bindSummaryViews() {
        tvRevenue = findViewById(R.id.tv_stat_revenue_value);
        tvOrderCount = findViewById(R.id.tv_stat_order_value);
        tvAov = findViewById(R.id.tv_stat_aov_value);
    }

    private void setupPeriodChips() {
        ChipGroup chipGroup = findViewById(R.id.cg_stat_period);
        chipGroup.setSingleSelection(true);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            AdminStatisticViewModel.Period period;
            if (id == R.id.chip_stat_day) {
                period = AdminStatisticViewModel.Period.DAY;
            } else if (id == R.id.chip_stat_month) {
                period = AdminStatisticViewModel.Period.MONTH;
            } else if (id == R.id.chip_stat_year) {
                period = AdminStatisticViewModel.Period.YEAR;
            } else {
                period = AdminStatisticViewModel.Period.WEEK;
            }
            viewModel.loadRevenue(period);
        });
    }

    private void setupRecyclerViews() {
        RecyclerView rvTopBooks = findViewById(R.id.rv_stat_top_books);
        rvTopBooks.setLayoutManager(new LinearLayoutManager(this));
        topBooksAdapter = new AdminStatRankAdapter();
        rvTopBooks.setAdapter(topBooksAdapter);

        RecyclerView rvTopCustomers = findViewById(R.id.rv_stat_top_customers);
        rvTopCustomers.setLayoutManager(new LinearLayoutManager(this));
        topCustomersAdapter = new AdminStatRankAdapter();
        rvTopCustomers.setAdapter(topCustomersAdapter);
    }

    private void observeViewModel() {
        viewModel.getRevenue().observe(this, revenue -> {
            if (revenue != null) tvRevenue.setText(PriceFormatter.formatVND(revenue));
        });
        viewModel.getOrderCount().observe(this, count -> {
            if (count != null) tvOrderCount.setText(String.valueOf(count));
        });
        viewModel.getAverageOrderValue().observe(this, aov -> {
            if (aov != null) tvAov.setText(PriceFormatter.formatVND(aov));
        });
        viewModel.getTopBooks().observe(this, topBooksAdapter::setItems);
        viewModel.getTopCustomers().observe(this, topCustomersAdapter::setItems);

        // TODO: chart_statistic_revenue (MPAndroidChart LineChart) cần dữ liệu doanh thu
        // TỪNG NGÀY trong kỳ đã chọn để vẽ đường biểu diễn xu hướng — AdminStatisticViewModel
        // hiện chỉ tính TỔNG cả kỳ (1 con số), chưa breakdown theo ngày. Muốn vẽ chart thật,
        // cần thêm 1 hàm ở AdminOrderRepository nhóm đơn hàng theo ngày trong khoảng thời
        // gian rồi trả về List<Entry>, xem đây là việc làm tiếp theo khi có nhu cầu.
    }
}
