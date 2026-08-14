package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Book;
import com.example.bookapp.Repository.AdminBookRepository;
import com.example.bookapp.Utils.FirebaseCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Dùng cho ManageBookActivity. */
public class AdminBookViewModel extends ViewModel {

    public enum SortOption {NEWEST, PRICE_ASC, PRICE_DESC, BEST_SELLING}

    public enum StockFilter {ALL, IN_STOCK, OUT_OF_STOCK, HIDDEN}

    private final AdminBookRepository repository = new AdminBookRepository();

    private final LiveData<List<Book>> allBooks = repository.observeAllBooks();
    private final MediatorLiveData<List<Book>> displayedBooks = new MediatorLiveData<>();
    private List<Book> cachedList = new ArrayList<>();

    private String currentKeyword = "";
    private SortOption currentSort = SortOption.NEWEST;
    private StockFilter currentStockFilter = StockFilter.ALL;
    private String currentCategoryId = null; // null = không lọc theo thể loại

    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AdminBookViewModel() {
        displayedBooks.addSource(allBooks, list -> {
            cachedList = list != null ? list : new ArrayList<>();
            applyFilterSort();
        });
    }

    public LiveData<List<Book>> getDisplayedBooks() {
        return displayedBooks;
    }

    public LiveData<Boolean> getActionSuccess() {
        return actionSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void search(String keyword) {
        currentKeyword = keyword;
        applyFilterSort();
    }

    public void setSortOption(SortOption option) {
        currentSort = option;
        applyFilterSort();
    }

    /** Ứng với 4 chip chip_all/chip_in_stock/chip_out_stock/chip_hidden trong admin_activity_manage_book.xml. */
    public void setStockFilter(StockFilter filter) {
        currentStockFilter = filter;
        applyFilterSort();
    }

    /** null = bỏ lọc thể loại — dùng khi bấm chip_category_filter rồi chọn 1 thể loại cụ thể. */
    public void setCategoryFilter(String categoryId) {
        currentCategoryId = categoryId;
        applyFilterSort();
    }

    private void applyFilterSort() {
        List<Book> result = repository.filterByTitle(cachedList, currentKeyword);

        if (currentCategoryId != null) {
            List<Book> byCategory = new ArrayList<>();
            for (Book b : result) {
                if (b.getCategoryIds() != null && b.getCategoryIds().contains(currentCategoryId)) {
                    byCategory.add(b);
                }
            }
            result = byCategory;
        }

        switch (currentStockFilter) {
            case IN_STOCK:
                result = filterBy(result, b -> b.getStock() > 0 && b.isActive());
                break;
            case OUT_OF_STOCK:
                result = filterBy(result, b -> b.getStock() <= 0);
                break;
            case HIDDEN:
                result = filterBy(result, b -> !b.isActive());
                break;
            case ALL:
            default:
                // không lọc gì thêm
                break;
        }

        List<Book> sorted = new ArrayList<>(result);
        switch (currentSort) {
            case PRICE_ASC:
                sorted.sort(Comparator.comparingDouble(Book::getSalePrice));
                break;
            case PRICE_DESC:
                sorted.sort((a, b) -> Double.compare(b.getSalePrice(), a.getSalePrice()));
                break;
            case BEST_SELLING:
                sorted.sort((a, b) -> Integer.compare(b.getSoldCount(), a.getSoldCount()));
                break;
            case NEWEST:
            default:
                // Đã orderBy createdAt DESC sẵn từ Repository, giữ nguyên thứ tự.
                break;
        }

        displayedBooks.setValue(sorted);
    }

    private interface BookPredicate {
        boolean test(Book book);
    }

    private List<Book> filterBy(List<Book> source, BookPredicate predicate) {
        List<Book> result = new ArrayList<>();
        for (Book b : source) {
            if (predicate.test(b)) result.add(b);
        }
        return result;
    }

    /** Bật/tắt nhanh từ sw_book_active trong item — không cần mở AddEditBookActivity. */
    public void toggleBookActive(String bookId, boolean isActive) {
        repository.toggleActive(bookId, isActive, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                actionSuccess.setValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    /** Nút xóa trong menu 3 chấm — thực chất là xóa mềm (xem AdminBookRepository.softDeleteBook). */
    public void deleteBook(String bookId) {
        repository.softDeleteBook(bookId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                actionSuccess.setValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });
    }
}
