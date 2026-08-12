package com.example.bookapp.Repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bookapp.Model.Review;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseCallback;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lớp DUY NHẤT được phép gọi Firestore cho collection "reviews".
 * Dùng bởi WriteReviewActivity (viết mới), MyReviewsActivity (xem lại + xóa).
 */
public class ReviewRepository {

    public void addReview(Review review, FirebaseCallback<Void> callback) {
        String reviewId = UUID.randomUUID().toString();
        review.setReviewId(reviewId);

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_REVIEWS)
                .document(reviewId)
                .set(review)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);

        // TODO: nên trigger cập nhật lại rating/ratingCount trung bình của book
        // bằng Cloud Function (onCreate của collection reviews) thay vì tính trong app.
    }

    public LiveData<List<Review>> getReviewsByUser(String uid) {
        MutableLiveData<List<Review>> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    querySnapshot.forEach(doc -> {
                        Review review = doc.toObject(Review.class);
                        review.setReviewId(doc.getId());
                        reviews.add(review);
                    });
                    liveData.setValue(reviews);
                })
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    /** Review preview cho BookDetailActivity - lấy vài review mới nhất của 1 cuốn sách. */
    public LiveData<List<Review>> getReviewsByBook(String bookId, int limit) {
        MutableLiveData<List<Review>> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("bookId", bookId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    querySnapshot.forEach(doc -> {
                        Review review = doc.toObject(Review.class);
                        review.setReviewId(doc.getId());
                        reviews.add(review);
                    });
                    liveData.setValue(reviews);
                })
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    public void deleteReview(String reviewId, FirebaseCallback<Void> callback) {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_REVIEWS)
                .document(reviewId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);

        // TODO: cập nhật lại rating/ratingCount trung bình của book sau khi xóa -
        // cũng nên xử lý bằng Cloud Function như addReview().
    }
}
