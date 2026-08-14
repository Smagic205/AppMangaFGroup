package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Author;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Category;
import com.example.bookapp.Model.Publisher;
import com.example.bookapp.Repository.AdminAuthorRepository;
import com.example.bookapp.Repository.AdminBookRepository;
import com.example.bookapp.Repository.AdminCategoryRepository;
import com.example.bookapp.Repository.AdminPublisherRepository;
import com.example.bookapp.Utils.FirebaseCallback;

import java.util.ArrayList;
import java.util.List;

/** Dùng cho AddEditBookActivity — cả 2 chế độ Thêm mới và Sửa. */
public class AdminAddEditBookViewModel extends ViewModel {

    private final AdminBookRepository bookRepository = new AdminBookRepository();
    private final AdminCategoryRepository categoryRepository = new AdminCategoryRepository();
    private final AdminAuthorRepository authorRepository = new AdminAuthorRepository();
    private final AdminPublisherRepository publisherRepository = new AdminPublisherRepository();

    // Dữ liệu cho 3 dropdown/chip chọn — tải 1 lần khi mở màn, không cần realtime.
    private final LiveData<List<Category>> categories = categoryRepository.observeAllCategories();
    private final LiveData<List<Author>> authors = authorRepository.observeAllAuthors();
    private final LiveData<List<Publisher>> publishers = publisherRepository.observeAllPublishers();

    private final MutableLiveData<Book> loadedBook = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<Author>> getAuthors() {
        return authors;
    }

    public LiveData<List<Publisher>> getPublishers() {
        return publishers;
    }

    public LiveData<Book> getLoadedBook() {
        return loadedBook;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /** Gọi ở onCreate() nếu Intent có EXTRA_BOOK_ID (chế độ Sửa). */
    public void loadBookForEdit(String bookId) {
        bookRepository.getBookById(bookId).observeForever(loadedBook::setValue);
    }

    /**
     * bookId null = Thêm mới, khác null = Sửa. coverImageUrl phải upload xong trước khi
     * gọi hàm này (Activity gọi ImageUtils.uploadImage() trước, lấy url rồi mới gọi save).
     */
    public void saveBook(String bookId, String title, String description, String coverImageUrl,
                          List<String> authorIds, String publisherId, List<String> categoryIds,
                          double price, double salePrice, int stock,
                          boolean isFeatured, boolean isActive) {

        if (title == null || title.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập tên sách");
            return;
        }
        if (authorIds == null || authorIds.isEmpty()) {
            errorMessage.setValue("Vui lòng chọn tác giả");
            return;
        }
        if (publisherId == null || publisherId.isEmpty()) {
            errorMessage.setValue("Vui lòng chọn nhà xuất bản");
            return;
        }
        if (categoryIds == null || categoryIds.isEmpty()) {
            errorMessage.setValue("Vui lòng chọn ít nhất 1 thể loại");
            return;
        }
        if (price <= 0 || stock < 0) {
            errorMessage.setValue("Giá hoặc tồn kho không hợp lệ");
            return;
        }

        Book book = new Book();
        book.setBookId(bookId);
        book.setTitle(title.trim());
        book.setDescription(description);
        book.setCoverImageUrl(coverImageUrl);
        book.setAuthorIds(authorIds);
        book.setPublisherId(publisherId);
        book.setCategoryIds(categoryIds);
        book.setPrice(price);
        book.setSalePrice(salePrice);
        book.setStock(stock);
        book.setFeatured(isFeatured);
        book.setActive(isActive);

        isLoading.setValue(true);
        FirebaseCallback<Void> callback = new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.setValue(false);
                saveSuccess.setValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        };

        if (bookId == null) {
            bookRepository.addBook(book, callback);
        } else {
            bookRepository.updateBook(book, callback);
        }
    }
}
