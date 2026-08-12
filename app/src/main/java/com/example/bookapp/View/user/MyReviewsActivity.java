package com.example.bookapp.View.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.MyReviewAdapter;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Review;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn "Đánh giá của tôi" - mở từ ll_menu_reviews trong fragment_profile.xml.
 * Khác với WriteReviewActivity (viết đánh giá mới): đây là xem LẠI các
 * đánh giá mình đã viết trước đó, và cho phép xóa.
 */
public class MyReviewsActivity extends AppCompatActivity {

    private RecyclerView rvMyReviews;
    private LinearLayout llEmpty;

    private MyReviewAdapter adapter;
    private final List<Review> reviewList = new ArrayList<>();
    private final Map<String, String[]> bookInfoCache = new HashMap<>(); // bookId -> [title, coverUrl]

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMyReviews = findViewById(R.id.rv_my_reviews);
        llEmpty = findViewById(R.id.ll_empty);

        rvMyReviews.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new MyReviewAdapter(reviewList, bookInfoCache, (review, position) -> confirmDelete(review, position));
        rvMyReviews.setAdapter(adapter);

        loadMyReviews();
    }

    private void loadMyReviews() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        FirebaseUtils.getFirestore().collection("reviews")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reviewList.clear();
                    querySnapshot.forEach(doc -> {
                        Review review = doc.toObject(Review.class);
                        review.setReviewId(doc.getId());
                        reviewList.add(review);
                    });

                    updateEmptyState();
                    fetchBookInfoForReviews();
                });
    }

    /**
     * Review model không lưu title/coverImageUrl của sách (chỉ lưu bookId),
     * nên phải join thêm 1 lượt sang collection "books" để hiển thị.
     * Với số lượng review ít (thường vài chục), gọi từng get() riêng lẻ là
     * đủ nhanh; nếu sau này nhiều hơn, nên đổi sang whereIn theo batch 10.
     */
    private void fetchBookInfoForReviews() {
        if (reviewList.isEmpty()) return;

        int[] remaining = {reviewList.size()};

        for (Review review : reviewList) {
            if (bookInfoCache.containsKey(review.getBookId())) {
                remaining[0]--;
                if (remaining[0] == 0) adapter.notifyDataSetChanged();
                continue;
            }

            FirebaseUtils.getFirestore().collection("books").document(review.getBookId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            bookInfoCache.put(review.getBookId(),
                                    new String[]{book.getTitle(), book.getCoverImageUrl()});
                        }
                        remaining[0]--;
                        if (remaining[0] <= 0) adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] <= 0) adapter.notifyDataSetChanged();
                    });
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = reviewList.isEmpty();
        llEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvMyReviews.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void confirmDelete(Review review, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa đánh giá")
                .setMessage("Bạn có chắc muốn xóa đánh giá này?")
                .setPositiveButton("Xóa", (dialog, which) ->
                        FirebaseUtils.getFirestore().collection("reviews")
                                .document(review.getReviewId())
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    reviewList.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    updateEmptyState();
                                    // TODO: cập nhật lại rating/ratingCount trung bình của book
                                    // sau khi xóa review - nên xử lý bằng Cloud Function.
                                }))
                .setNegativeButton("Hủy", null)
                .show();
    }
}
