package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Category;
import com.example.bookapp.Model.User;
import com.example.bookapp.Repository.BookRepository;
import com.example.bookapp.Repository.CategoryRepository;
import com.example.bookapp.Repository.UserRepository;

import java.util.List;

/**
 * ViewModel cho HomeFragment.
 * Tách 3 nguồn dữ liệu (users, categories, books) thành 3 LiveData riêng.
 * Fragment chỉ observe — không tự gọi Firestore.
 */
public class HomeViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final CategoryRepository categoryRepository = new CategoryRepository();
    private final BookRepository bookRepository = new BookRepository();

    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> _categories = new MutableLiveData<>();
    private final MutableLiveData<List<Book>> _featuredBooks = new MutableLiveData<>();
    private final MutableLiveData<List<Book>> _allBooks = new MutableLiveData<>();

    public LiveData<User> getCurrentUser() { return _currentUser; }
    public LiveData<List<Category>> getCategories() { return _categories; }
    public LiveData<List<Book>> getFeaturedBooks() { return _featuredBooks; }
    public LiveData<List<Book>> getAllBooks() { return _allBooks; }

    /**
     * Gọi tất cả 3 nguồn dữ liệu song song.
     * Bởi vì ViewModel sống qua xoay màn hình, Fragment gọi load()
     * trong onViewCreated() — dữ liệu đã có sẵn sẽ được emit lại ngay.
     */
    public void load(String uid) {
        loadUser(uid);
        loadCategories();
        loadFeaturedBooks();
        loadAllBooks();
    }

    private void loadUser(String uid) {
        userRepository.getUser(uid).observeForever(user -> _currentUser.setValue(user));
    }

    private void loadCategories() {
        categoryRepository.getActiveCategories().observeForever(
                categories -> _categories.setValue(categories));
    }

    private void loadFeaturedBooks() {
        bookRepository.getFeaturedBooks().observeForever(
                books -> _featuredBooks.setValue(books));
    }

    private void loadAllBooks() {
        bookRepository.getAllActiveBooks().observeForever(
                books -> _allBooks.setValue(books));
    }
}
