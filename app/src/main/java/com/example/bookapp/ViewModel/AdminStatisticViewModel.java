package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Adapter.admin.AdminRankItem;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Order;
import com.example.bookapp.Repository.AdminBookRepository;
import com.example.bookapp.Repository.AdminOrderRepository;
import com.example.bookapp.Repository.AdminUserRepository;
import com.example.bookapp.Utils.PriceFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dùng cho StatisticActivity — doanh thu theo kỳ, top sách, top khách hàng. */
public class AdminStatisticViewModel extends ViewModel {

    public enum Period {DAY, WEEK, MONTH, YEAR}

    private final AdminOrderRepository orderRepository = new AdminOrderRepository();
    private final AdminBookRepository bookRepository = new AdminBookRepository();
    private final AdminUserRepository userRepository = new AdminUserRepository();

    private final MutableLiveData<Double> revenue = new MutableLiveData<>();
    private final MediatorLiveData<List<AdminRankItem>> topBooks = new MediatorLiveData<>();
    private final MediatorLiveData<List<AdminRankItem>> topCustomers = new MediatorLiveData<>();

    public LiveData<Double> getRevenue() {
        return revenue;
    }

    public LiveData<List<AdminRankItem>> getTopBooks() {
        return topBooks;
    }

    public LiveData<List<AdminRankItem>> getTopCustomers() {
        return topCustomers;
    }

    /** Gọi khi chọn chip Ngày/Tuần/Tháng/Năm — tính khoảng [fromDate, toDate] rồi lấy doanh thu. */
    public void loadRevenue(Period period) {
        Date[] range = calculateDateRange(period);
        LiveData<Double> source = orderRepository.getRevenueInRange(range[0], range[1]);
        revenue.setValue(null); // reset để UI hiện loading trong lúc chờ
        source.observeForever(revenue::setValue);
    }

    private Date[] calculateDateRange(Period period) {
        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();

        switch (period) {
            case DAY:
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                break;
            case WEEK:
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                break;
            case MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case YEAR:
                cal.set(Calendar.DAY_OF_YEAR, 1);
                break;
        }
        return new Date[]{cal.getTime(), toDate};
    }

    /** Top N sách bán chạy — map từ Book sang AdminRankItem để AdminStatRankAdapter dùng chung. */
    public void loadTopBooks(int limit) {
        LiveData<List<Book>> source = bookRepository.getTopSellingBooks(limit);
        topBooks.addSource(source, books -> {
            List<AdminRankItem> items = new ArrayList<>();
            if (books != null) {
                for (Book b : books) {
                    items.add(new AdminRankItem(b.getCoverImageUrl(), b.getTitle(),
                            "Đã bán " + b.getSoldCount()));
                }
            }
            topBooks.setValue(items);
        });
    }

    /**
     * Top N khách hàng mua nhiều đơn nhất — Firestore không hỗ trợ GROUP BY, nên đếm bằng
     * cách gom nhóm client-side trên danh sách đơn hàng đã tải (chấp nhận được ở quy mô
     * bài tập lớn; nếu dữ liệu lớn hơn nên chuyển sang Cloud Function tính sẵn).
     */
    public void loadTopCustomers(int limit) {
        LiveData<List<Order>> ordersSource = orderRepository.observeAllOrders();
        topCustomers.addSource(ordersSource, orders -> {
            if (orders == null) return;

            Map<String, Integer> orderCountByUser = new HashMap<>();
            Map<String, String> nameByUser = new HashMap<>();
            for (Order o : orders) {
                String uid = o.getUserId();
                if (uid == null) continue;
                orderCountByUser.put(uid, orderCountByUser.getOrDefault(uid, 0) + 1);
                if (o.getShippingAddress() != null) {
                    nameByUser.put(uid, o.getShippingAddress().getName());
                }
            }

            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(orderCountByUser.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            List<AdminRankItem> items = new ArrayList<>();
            int count = 0;
            for (Map.Entry<String, Integer> entry : sorted) {
                if (count >= limit) break;
                String name = nameByUser.getOrDefault(entry.getKey(), "Khách hàng");
                items.add(new AdminRankItem(null, name, entry.getValue() + " đơn hàng"));
                count++;
            }
            topCustomers.setValue(items);
        });
    }
}
