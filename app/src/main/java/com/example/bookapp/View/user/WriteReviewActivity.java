package com.example.bookapp.View.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Review;
import com.example.bookapp.Model.User;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.Timestamp;

import java.util.UUID;

public class WriteReviewActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_BOOK_ID = "extra_book_id";

    private ImageView ivBookCover;
    private TextView tvBookTitle, tvBookAuthor, tvRatingLabel;
    private ImageView[] stars;
    private EditText etComment;
    private Button btnSubmit;

    private String orderId, bookId;
    private int selectedRating = 0;

    private static final String[] RATING_LABELS = {
            "Chạm vào sao để đánh giá", "Rất tệ", "Tệ", "Bình thường", "Tốt", "Xuất sắc"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        loadBookInfo();
        setupStarClicks();

        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void bindViews() {
        ivBookCover = findViewById(R.id.iv_book_cover);
        tvBookTitle = findViewById(R.id.tv_book_title);
        tvBookAuthor = findViewById(R.id.tv_book_author);
        tvRatingLabel = findViewById(R.id.tv_rating_label);
        etComment = findViewById(R.id.et_comment);
        btnSubmit = findViewById(R.id.btn_submit_review);

        stars = new ImageView[]{
                findViewById(R.id.star1), findViewById(R.id.star2), findViewById(R.id.star3),
                findViewById(R.id.star4), findViewById(R.id.star5)
        };
    }

    private void loadBookInfo() {
        if (bookId == null) return;

        FirebaseUtils.getFirestore().collection("books").document(bookId)
                .get()
                .addOnSuccessListener(doc -> {
                    Book book = doc.toObject(Book.class);
                    if (book == null) return;

                    tvBookTitle.setText(book.getTitle());
                    Glide.with(this).load(book.getCoverImageUrl())
                            .placeholder(R.drawable.placeholder_book)
                            .into(ivBookCover);
                    // Tên tác giả cần join thêm từ collection "authors" qua book.getAuthorIds()
                    // - lược bớt ở đây, xử lý trong AuthorRepository khi wiring ViewModel thật
                });
    }

    private void setupStarClicks() {
        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating = starIndex;
                updateStarUI();
            });
        }
    }

    private void updateStarUI() {
        int activeColor = ContextCompat.getColor(this, R.color.star_rating);
        int inactiveColor = ContextCompat.getColor(this, R.color.divider);

        for (int i = 0; i < stars.length; i++) {
            stars[i].setColorFilter(i < selectedRating ? activeColor : inactiveColor);
        }
        tvRatingLabel.setText(RATING_LABELS[selectedRating]);
    }

    private void submitReview() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(comment)) {
            etComment.setError("Vui lòng nhập nhận xét");
            return;
        }

        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        btnSubmit.setEnabled(false);

        FirebaseUtils.getFirestore().collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    User currentUser = userDoc.toObject(User.class);
                    if (currentUser == null) return;

                    String reviewId = UUID.randomUUID().toString();
                    Review review = new Review(
                            reviewId,
                            bookId,
                            uid,
                            currentUser.getFullName(),
                            currentUser.getAvatarUrl(),
                            selectedRating,
                            comment,
                            orderId,
                            Timestamp.now()
                    );

                    FirebaseUtils.getFirestore().collection("reviews").document(reviewId)
                            .set(review)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                                // TODO: cập nhật lại rating/ratingCount trung bình của book,
                                // nên xử lý bằng Cloud Function trigger onCreate của collection reviews
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSubmit.setEnabled(true);
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }
}
