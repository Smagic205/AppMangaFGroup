package com.example.bookapp.View.user;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bookapp.Model.Book;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.example.bookapp.ViewModel.BookDetailViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class BookDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BOOK_ID = "extra_book_id";

    private ImageView ivCover;
    private TextView tvTitle, tvAuthor, tvCategoryTag, tvRating, tvReviewCount, tvSoldCount,
            tvSalePrice, tvOriginalPrice, tvDiscountPercent, tvStockStatus,
            tvDescription, tvExpandDescription, tvSeeAllReviews, tvQuantity;
    private RecyclerView rvRelatedBooks, rvReviewsPreview;
    private ImageButton ibDecreaseQty, ibIncreaseQty, ibAddToCartIcon;
    private Button btnBuyNow;

    private String bookId;
    private Book currentBook;
    private int quantity = 1;

    private BookDetailViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setupClicks();

        viewModel = new ViewModelProvider(this).get(BookDetailViewModel.class);

        viewModel.getBook().observe(this, book -> {
            if (book != null) {
                currentBook = book;
                bindBookData(book);
                viewModel.loadRelatedBooks(bookId);
                viewModel.incrementViewCount(bookId, book.getViewCount());
            }
        });

        viewModel.getAddToCartSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        if (bookId != null) viewModel.loadBook(bookId);
    }

    private void bindViews() {
        ivCover = findViewById(R.id.iv_cover);
        tvTitle = findViewById(R.id.tv_title);
        tvAuthor = findViewById(R.id.tv_author);
        tvCategoryTag = findViewById(R.id.tv_category_tag);
        tvRating = findViewById(R.id.tv_rating);
        tvReviewCount = findViewById(R.id.tv_review_count);
        tvSoldCount = findViewById(R.id.tv_sold_count);
        tvSalePrice = findViewById(R.id.tv_sale_price);
        tvOriginalPrice = findViewById(R.id.tv_original_price);
        tvDiscountPercent = findViewById(R.id.tv_discount_percent);
        tvStockStatus = findViewById(R.id.tv_stock_status);
        tvDescription = findViewById(R.id.tv_description);
        tvExpandDescription = findViewById(R.id.tv_expand_description);
        tvSeeAllReviews = findViewById(R.id.tv_see_all_reviews);
        tvQuantity = findViewById(R.id.tv_quantity);
        rvRelatedBooks = findViewById(R.id.rv_related_books);
        rvReviewsPreview = findViewById(R.id.rv_reviews_preview);
        ibDecreaseQty = findViewById(R.id.ib_decrease_qty);
        ibIncreaseQty = findViewById(R.id.ib_increase_qty);
        ibAddToCartIcon = findViewById(R.id.ib_add_to_cart_icon);
        btnBuyNow = findViewById(R.id.btn_buy_now);

        rvRelatedBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvReviewsPreview.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClicks() {
        tvExpandDescription.setOnClickListener(v -> {
            // Toggle giữa rút gọn (maxLines cố định trong XML) và hiển thị đầy đủ
            boolean expanded = tvDescription.getMaxLines() != Integer.MAX_VALUE;
            tvDescription.setMaxLines(expanded ? Integer.MAX_VALUE : 4);
            tvExpandDescription.setText(expanded ? "Thu gọn" : "Xem thêm");
        });

        ibDecreaseQty.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });
        ibIncreaseQty.setOnClickListener(v -> {
            if (currentBook == null || quantity < currentBook.getStock()) {
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        ibAddToCartIcon.setOnClickListener(v -> addToCart());
        btnBuyNow.setOnClickListener(v -> addToCart());

        tvSeeAllReviews.setOnClickListener(v -> {
            // TODO: mở màn danh sách toàn bộ review của sách
        });
    }

    private void bindBookData(Book book) {
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        tvTitle.setText(book.getTitle());
        tvRating.setText(String.format(Locale.getDefault(), "%.1f", book.getRating()));
        tvReviewCount.setText("(" + book.getRatingCount() + " đánh giá)");
        tvSoldCount.setText("Đã bán " + book.getSoldCount());
        tvDescription.setText(book.getDescription());

        Glide.with(this).load(book.getCoverImageUrl())
                .placeholder(R.drawable.placeholder_book)
                .into(ivCover);

        if (book.isOnSale()) {
            tvSalePrice.setText(currencyFormat.format(book.getSalePrice()) + "đ");
            tvOriginalPrice.setText(currencyFormat.format(book.getPrice()) + "đ");
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvDiscountPercent.setVisibility(View.VISIBLE);
            int percent = (int) Math.round(100 - (book.getSalePrice() / book.getPrice() * 100));
            tvDiscountPercent.setText("-" + percent + "%");
        } else {
            tvSalePrice.setText(currencyFormat.format(book.getPrice()) + "đ");
            tvOriginalPrice.setVisibility(View.GONE);
            tvDiscountPercent.setVisibility(View.GONE);
        }

        boolean inStock = book.getStock() > 0;
        tvStockStatus.setText(inStock ? "Còn hàng" : "Hết hàng");
        btnBuyNow.setEnabled(inStock);
        ibAddToCartIcon.setEnabled(inStock);

        // Tên tác giả / danh mục cần join thêm từ collection "authors" / "categories"
        // qua book.getAuthorIds() / book.getCategoryIds() - lược bớt ở đây.
        tvAuthor.setText("");
        tvCategoryTag.setText("");
    }

    private void addToCart() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null || currentBook == null) return;

        double priceAtAdd = currentBook.isOnSale() ? currentBook.getSalePrice() : currentBook.getPrice();
        viewModel.addToCart(uid, currentBook.getBookId(), quantity, priceAtAdd);
    }
}
