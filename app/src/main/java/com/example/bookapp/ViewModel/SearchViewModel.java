package com.example.bookapp.ViewModel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Category;
import com.example.bookapp.Repository.BookRepository;
import com.example.bookapp.Repository.CategoryRepository;

import java.util.List;

/**
 * ViewModel cho SearchActivity.
 * Giữ kết quả tìm kiếm qua xoay màn hình, tránh gọi lại Firestore không cần thiết.
 */
public class SearchViewModel extends ViewModel {

    private final BookRepository bookRepository = new BookRepository();
    private final CategoryRepository categoryRepository = new CategoryRepository();

    private final MutableLiveData<List<Category>> _categories = new MutableLiveData<>();
    private final MutableLiveData<List<Book>> _searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public LiveData<List<Category>> getCategories() { return _categories; }
    public LiveData<List<Book>> getSearchResults() { return _searchResults; }
    public LiveData<Boolean> getIsLoading() { return _isLoading; }

    public void loadCategories() {
        categoryRepository.getActiveCategories().observeForever(
                categories -> _categories.setValue(categories));
    }

    /**
     * Tìm kiếm sách theo tên/thể loại/sắp xếp.
     * Logic query giữ nguyên trong BookRepository, ViewModel chỉ gọi và relay kết quả.
     */
    public void searchBooks(String keyword, @Nullable String categoryId, String sortKey) {
        _isLoading.setValue(true);
        bookRepository.searchBooks(keyword, categoryId, sortKey).observeForever(books -> {
            _searchResults.setValue(books);
            _isLoading.setValue(false);
        });
    }
}
