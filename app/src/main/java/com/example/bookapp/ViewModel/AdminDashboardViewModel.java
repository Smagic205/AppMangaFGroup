package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Adapter.admin.AdminDashboardMenuItem;
import com.example.bookapp.Adapter.admin.AdminDashboardStat;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Order;
import com.example.bookapp.R;
import com.example.bookapp.Repository.AdminBookRepository;
import com.example.bookapp.Repository.AdminOrderRepository;
import com.example.bookapp.Repository.AdminUserRepository;
import com.example.bookapp.Utils.PriceFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Dùng cho AdminDashboardActivity — trang chủ tổng quan. */
public class AdminDashboardViewModel extends ViewModel {

    private final AdminBookRepository bookRepository = new AdminBookRepository();
    private final AdminOrderRepository orderRepository = new AdminOrderRepository();
    private final AdminUserRepository userRepository = new AdminUserRepository();

    private final MediatorLiveData<List<AdminDashboardStat>> statCards = new MediatorLiveData<>();
    private Integer bookCount, orderCount, userCount;
    private Double revenueThisWeek;

    public LiveData<List<AdminDashboardStat>> getStatCards() {
        return statCards;
    }

    public LiveData<List<Order>> getRecentOrders() {
        return orderRepository.getRecentOrders(5);
    }

    public LiveData<List<Book>> getTopSellingBooks() {
        return bookRepository.getTopSellingBooks(10);
    }

    /**
     * Gọi 1 lần ở onCreate() Activity — tải song song 4 nguồn số liệu (đếm sách/đơn/user +
     * doanh thu tuần này), gộp thành danh sách 4 AdminDashboardStat khi ĐỦ CẢ 4 đã về, dùng
     * MediatorLiveData theo dõi nhiều nguồn cùng lúc thay vì lồng callback vào nhau.
     */
    public void loadStats() {
        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        Date fromDate = cal.getTime();

        statCards.addSource(orderRepository.getRevenueInRange(fromDate, toDate), value -> {
            revenueThisWeek = value;
            tryEmitStats();
        });
        statCards.addSource(orderRepository.countAllOrders(), value -> {
            orderCount = value;
            tryEmitStats();
        });
        statCards.addSource(userRepository.countAllUsers(), value -> {
            userCount = value;
            tryEmitStats();
        });
        statCards.addSource(bookRepository.countActiveBooks(), value -> {
            bookCount = value;
            tryEmitStats();
        });
    }

    /** Chỉ emit ra danh sách thẻ khi ĐỦ 4 giá trị đã tải xong, tránh hiện thẻ thiếu số liệu. */
    private void tryEmitStats() {
        if (revenueThisWeek == null || orderCount == null || userCount == null || bookCount == null) {
            return;
        }
        List<AdminDashboardStat> stats = new ArrayList<>();
        stats.add(new AdminDashboardStat(R.drawable.ic_revenue,
                PriceFormatter.formatVND(revenueThisWeek), "Doanh thu tuần", null));
        stats.add(new AdminDashboardStat(R.drawable.ic_orders_stat,
                String.valueOf(orderCount), "Đơn hàng", null));
        stats.add(new AdminDashboardStat(R.drawable.ic_users_stat,
                String.valueOf(userCount), "Người dùng", null));
        stats.add(new AdminDashboardStat(R.drawable.ic_products_stat,
                String.valueOf(bookCount), "Sản phẩm", null));
        statCards.setValue(stats);
    }

    /**
     * Danh sách tĩnh cho lưới 3 cột "Quản lý cửa hàng" — KHÔNG lấy từ Firestore, chỉ là
     * cấu hình điều hướng cố định. targetKey Activity sẽ dùng để switch-case mở đúng màn.
     */
    public List<AdminDashboardMenuItem> getMenuItems() {
        List<AdminDashboardMenuItem> items = new ArrayList<>();
        items.add(new AdminDashboardMenuItem(R.drawable.ic_book, "Quản lý sách", "manage_book"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_category, "Thể loại", "manage_category"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_author, "Tác giả", "manage_author"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_publisher, "Nhà xuất bản", "manage_publisher"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_order, "Đơn hàng", "manage_order"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_user, "Người dùng", "manage_user"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_voucher, "Voucher", "manage_voucher"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_notification, "Thông báo", "manage_notification"));
        items.add(new AdminDashboardMenuItem(R.drawable.ic_statistic, "Thống kê", "statistic"));
        return items;
    }
}
