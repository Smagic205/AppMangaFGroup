package com.example.bookapp.Repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bookapp.Model.Book;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DUY NHẤT được phép gọi Firestore cho collection "books".
 * Dùng bởi HomeFragment, SearchActivity, BookDetailActivity, CartFragment,
 * CheckoutActivity, MyReviewsActivity, WriteReviewActivity.
 */
public class BookRepository {

    public LiveData<Book> getBook(String bookId) {
        MutableLiveData<Book> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .document(bookId)
                .get()
                .addOnSuccessListener(doc -> {
                    Book book = doc.toObject(Book.class);
                    if (book != null) book.setBookId(doc.getId());
                    liveData.setValue(book);
                })
                .addOnFailureListener(e -> liveData.setValue(null));

        return liveData;
    }

    /** Sách ghim isFeatured=true - dùng cho khối "Nổi bật hôm nay" ở HomeFragment. */
    public LiveData<List<Book>> getFeaturedBooks() {
        MutableLiveData<List<Book>> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .whereEqualTo("isFeatured", true)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(qs -> liveData.setValue(toBookList(qs)))
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    /** Toàn bộ sách đang bán - dùng cho khối "Dành cho bạn" ở HomeFragment. */
    public LiveData<List<Book>> getAllActiveBooks() {
        MutableLiveData<List<Book>> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(qs -> liveData.setValue(toBookList(qs)))
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    /** Sách liên quan ở BookDetailActivity - loại bỏ chính cuốn đang xem. */
    public LiveData<List<Book>> getRelatedBooks(String excludeBookId) {
        MutableLiveData<List<Book>> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .whereEqualTo("isActive", true)
                .limit(10)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Book> books = new ArrayList<>();
                    qs.forEach(doc -> {
                        if (!doc.getId().equals(excludeBookId)) {
                            Book book = doc.toObject(Book.class);
                            book.setBookId(doc.getId());
                            books.add(book);
                        }
                    });
                    liveData.setValue(books);
                })
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    /**
     * Tìm kiếm sách theo tên/thể loại/sắp xếp - logic chuyển nguyên từ SearchActivity.
     * Firestore không hỗ trợ full-text search: tìm theo tiền tố tên sách
     * (title >= keyword && title <= keyword + "\uf8ff"). Nếu cần tìm gần đúng/toàn văn
     * thật sự, nên tích hợp Algolia hoặc Typesense ở bản nâng cấp sau.
     */
    public LiveData<List<Book>> searchBooks(String keyword, @Nullable String categoryId, String sortKey) {
        MutableLiveData<List<Book>> liveData = new MutableLiveData<>();

        Query query = FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .whereEqualTo("isActive", true);

        if (categoryId != null) {
            query = query.whereArrayContains("categoryIds", categoryId);
        }

        if (!keyword.isEmpty()) {
            query = query.orderBy("title").startAt(keyword).endAt(keyword + "\uf8ff");
        } else {
            switch (sortKey) {
                case "price_asc":
                    query = query.orderBy("price", Query.Direction.ASCENDING);
                    break;
                case "price_desc":
                    query = query.orderBy("price", Query.Direction.DESCENDING);
                    break;
                case "rating":
                    query = query.orderBy("rating", Query.Direction.DESCENDING);
                    break;
                case "bestseller":
                    query = query.orderBy("soldCount", Query.Direction.DESCENDING);
                    break;
                default:
                    query = query.orderBy("soldCount", Query.Direction.DESCENDING);
            }
        }

        query.limit(40).get()
                .addOnSuccessListener(qs -> liveData.setValue(toBookList(qs)))
                .addOnFailureListener(e -> {
                    // Lỗi thường gặp: thiếu composite index cho whereArrayContains + orderBy -
                    // Firestore trả link tạo index sẵn trong Logcat, bấm link đó là xong.
                    liveData.setValue(new ArrayList<>());
                });

        return liveData;
    }

    /** Tăng viewCount mỗi lần xem chi tiết sách - không cần chờ kết quả trả về. */
    public void incrementViewCount(String bookId, int currentViewCount) {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .document(bookId)
                .update("viewCount", currentViewCount + 1);
    }

    private List<Book> toBookList(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        List<Book> books = new ArrayList<>();
        querySnapshot.forEach(doc -> {
            Book book = doc.toObject(Book.class);
            book.setBookId(doc.getId());
            books.add(book);
        });
        return books;
    }
}
