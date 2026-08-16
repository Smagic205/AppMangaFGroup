package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Review;
import com.example.bookapp.Repository.ReviewRepository;

import java.util.List;

public class BookReviewsViewModel extends ViewModel {
    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final MutableLiveData<List<Review>> _reviews = new MutableLiveData<>();

    public LiveData<List<Review>> getReviews() {
        return _reviews;
    }

    public void loadReviews(String bookId) {
        reviewRepository.getAllReviewsByBook(bookId).observeForever(reviews -> {
            _reviews.setValue(reviews);
        });
    }
}
