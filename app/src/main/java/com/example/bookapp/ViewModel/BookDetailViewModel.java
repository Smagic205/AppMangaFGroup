package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Book;
import com.example.bookapp.Repository.AuthorRepository;
import com.example.bookapp.Repository.BookRepository;
import com.example.bookapp.Repository.CartRepository;
import com.example.bookapp.Repository.PublisherRepository;
import com.example.bookapp.Utils.FirebaseCallback;

import java.util.List;

/**
 * ViewModel cho BookDetailActivity.
 * Tách loadBookDetail(), loadRelatedBooks(), addToCart() ra khỏi Activity.
 */
public class BookDetailViewModel extends ViewModel {

    private final BookRepository bookRepository = new BookRepository();
    private final CartRepository cartRepository = new CartRepository();
    private final AuthorRepository authorRepository = new AuthorRepository();
    private final PublisherRepository publisherRepository = new PublisherRepository();

    private final MutableLiveData<Book> _book = new MutableLiveData<>();
    private final MutableLiveData<List<Book>> _relatedBooks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _addToCartSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<Book> getBook() { return _book; }
    public LiveData<List<Book>> getRelatedBooks() { return _relatedBooks; }
    public LiveData<Boolean> getAddToCartSuccess() { return _addToCartSuccess; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    public void loadBook(String bookId) {
        bookRepository.getBook(bookId).observeForever(book -> _book.setValue(book));
    }

    public void loadRelatedBooks(String excludeBookId) {
        bookRepository.getRelatedBooks(excludeBookId).observeForever(
                books -> _relatedBooks.setValue(books));
    }

    /**
     * Thêm sách vào giỏ hàng.
     * Tăng viewCount không cần chờ kết quả — gọi fire-and-forget.
     */
    public void addToCart(String uid, String bookId, int quantity, double priceAtAdd) {
        cartRepository.addToCart(uid, bookId, quantity, priceAtAdd, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                _addToCartSuccess.setValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.setValue(e.getMessage());
            }
        });
    }

    /** Tăng viewCount khi user mở trang chi tiết sách. Fire-and-forget, không cần kết quả. */
    public void incrementViewCount(String bookId, int currentViewCount) {
        bookRepository.incrementViewCount(bookId, currentViewCount);
    }
}
