package com.example.bookapp.View.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.BookAdapter;
import com.example.bookapp.Adapter.user.SearchCategoryAdapter;
import com.example.bookapp.Adapter.user.SearchKeywordAdapter;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Category;
import com.example.bookapp.R;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";

    private EditText etSearch;
    private ImageButton ibBack, ibClearSearch;
    private TextView tvCancelSearch, tvClearRecent, tvResultCount, tvEmptySearchMessage,
            tvActiveCategoryChip;
    private ProgressBar pbSearchLoading;
    private LinearLayout llBrowseSection, llResultSection, llEmptySearch,
            llRecentSearches, llActiveFilter, llSearchHeader;
    private RecyclerView rvSearchCategories, rvRecentSearches, rvPopularKeywords, rvSearchResults;
    private ChipGroup cgSortOptions;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>(); // TODO: lưu SharedPreferences để giữ qua các lần mở app
    private final List<String> popularKeywords = new ArrayList<>();
    private final List<Book> resultBookList = new ArrayList<>();

    private String activeCategoryId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        activeCategoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);

        bindViews();
        setupRecyclerViews();
        setupClicks();
        loadCategories();
        loadPopularKeywords();

        if (activeCategoryId != null) {
            showActiveCategoryFilter();
            searchBooks("", activeCategoryId, "relevance");
        } else {
            showBrowseSection();
        }
    }

    private void bindViews() {
        etSearch = findViewById(R.id.et_search);
        ibBack = findViewById(R.id.ib_back);
        ibClearSearch = findViewById(R.id.ib_clear_search);
        tvCancelSearch = findViewById(R.id.tv_cancel_search);
        tvClearRecent = findViewById(R.id.tv_clear_recent);
        tvResultCount = findViewById(R.id.tv_result_count);
        tvEmptySearchMessage = findViewById(R.id.tv_empty_search_message);
        tvActiveCategoryChip = findViewById(R.id.tv_active_category_chip);
        pbSearchLoading = findViewById(R.id.pb_search_loading);
        llBrowseSection = findViewById(R.id.ll_browse_section);
        llResultSection = findViewById(R.id.ll_result_section);
        llEmptySearch = findViewById(R.id.ll_empty_search);
        llRecentSearches = findViewById(R.id.ll_recent_searches);
        llActiveFilter = findViewById(R.id.ll_active_filter);
        llSearchHeader = findViewById(R.id.ll_search_header);
        rvSearchCategories = findViewById(R.id.rv_search_categories);
        rvRecentSearches = findViewById(R.id.rv_recent_searches);
        rvPopularKeywords = findViewById(R.id.rv_popular_keywords);
        rvSearchResults = findViewById(R.id.rv_search_results);
        cgSortOptions = findViewById(R.id.cg_sort_options);
    }

    private void setupRecyclerViews() {
        rvSearchCategories.setLayoutManager(new GridLayoutManager(this, 3));
        rvSearchCategories.setAdapter(new SearchCategoryAdapter(categoryList, category -> {
            activeCategoryId = category.getCategoryId();
            showActiveCategoryFilter();
            searchBooks(etSearch.getText().toString().trim(), activeCategoryId, currentSortKey());
        }));

        rvRecentSearches.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecentSearches.setAdapter(new SearchKeywordAdapter(recentSearches, this::runSearchFromKeyword));

        rvPopularKeywords.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPopularKeywords.setAdapter(new SearchKeywordAdapter(popularKeywords, this::runSearchFromKeyword));

        rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
        rvSearchResults.setAdapter(new BookAdapter(resultBookList, book -> {
            Intent intent = new Intent(this, BookDetailActivity.class);
            intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getBookId());
            startActivity(intent);
        }));
    }

    private void setupClicks() {
        ibBack.setOnClickListener(v -> finish());
        tvCancelSearch.setOnClickListener(v -> finish());

        ibClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            showBrowseSection();
        });

        tvClearRecent.setOnClickListener(v -> {
            recentSearches.clear();
            rvRecentSearches.getAdapter().notifyDataSetChanged();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                ibClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            runSearchFromKeyword(etSearch.getText().toString().trim());
            return true;
        });

        cgSortOptions.setOnCheckedStateChangeListener((group, checkedIds) ->
                searchBooks(etSearch.getText().toString().trim(), activeCategoryId, currentSortKey()));
    }

    private void runSearchFromKeyword(String keyword) {
        if (keyword.isEmpty()) return;

        etSearch.setText(keyword);
        etSearch.setSelection(keyword.length());

        if (!recentSearches.contains(keyword)) {
            recentSearches.add(0, keyword);
            rvRecentSearches.getAdapter().notifyDataSetChanged();
        }

        searchBooks(keyword, activeCategoryId, currentSortKey());
    }

    private String currentSortKey() {
        int checkedId = cgSortOptions.getCheckedChipId();
        if (checkedId == R.id.chip_sort_price_asc) return "price_asc";
        if (checkedId == R.id.chip_sort_price_desc) return "price_desc";
        if (checkedId == R.id.chip_sort_rating) return "rating";
        if (checkedId == R.id.chip_sort_bestseller) return "bestseller";
        return "relevance";
    }

    private void showActiveCategoryFilter() {
        for (Category category : categoryList) {
            if (category.getCategoryId().equals(activeCategoryId)) {
                tvActiveCategoryChip.setText(category.getName());
                break;
            }
        }
        llActiveFilter.setVisibility(View.VISIBLE);
    }

    private void showBrowseSection() {
        llBrowseSection.setVisibility(View.VISIBLE);
        llResultSection.setVisibility(View.GONE);
        llEmptySearch.setVisibility(View.GONE);
    }

    private void loadCategories() {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_CATEGORIES)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    categoryList.clear();
                    querySnapshot.forEach(doc -> {
                        Category category = doc.toObject(Category.class);
                        category.setCategoryId(doc.getId());
                        categoryList.add(category);
                    });
                    rvSearchCategories.getAdapter().notifyDataSetChanged();
                });
    }

    /**
     * TODO: thay danh sách mẫu này bằng thống kê từ khóa được tìm nhiều nhất thật
     * (vd 1 collection "searchStats" tăng dần counter mỗi lần searchBooks() chạy).
     */
    private void loadPopularKeywords() {
        popularKeywords.add("Đắc Nhân Tâm");
        popularKeywords.add("Nhà Giả Kim");
        popularKeywords.add("Tư duy nhanh và chậm");
        popularKeywords.add("Sách thiếu nhi");
        rvPopularKeywords.getAdapter().notifyDataSetChanged();
    }

    /**
     * Firestore không hỗ trợ full-text search - đây là tìm theo tiền tố tên sách
     * (title >= keyword && title <= keyword + "\uf8ff"), đủ dùng cho quy mô nhỏ.
     * Nếu cần tìm gần đúng/toàn văn thật sự, nên tích hợp Algolia hoặc Typesense.
     */
    private void searchBooks(String keyword, @Nullable String categoryId, String sortKey) {
        pbSearchLoading.setVisibility(View.VISIBLE);
        llBrowseSection.setVisibility(View.GONE);

        com.google.firebase.firestore.Query query = FirebaseUtils.getFirestore()
                .collection(Constants.COLLECTION_BOOKS)
                .whereEqualTo("isActive", true);

        if (categoryId != null) {
            query = query.whereArrayContains("categoryIds", categoryId);
        }
        if (!keyword.isEmpty()) {
            query = query.orderBy("title")
                    .startAt(keyword)
                    .endAt(keyword + "\uf8ff");
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
                .addOnSuccessListener(querySnapshot -> {
                    pbSearchLoading.setVisibility(View.GONE);

                    resultBookList.clear();
                    querySnapshot.forEach(doc -> {
                        Book book = doc.toObject(Book.class);
                        book.setBookId(doc.getId());
                        resultBookList.add(book);
                    });
                    rvSearchResults.getAdapter().notifyDataSetChanged();
                    tvResultCount.setText(resultBookList.size() + " kết quả");

                    boolean isEmpty = resultBookList.isEmpty();
                    llResultSection.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    llEmptySearch.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    if (isEmpty) {
                        tvEmptySearchMessage.setText("Không tìm thấy kết quả phù hợp");
                    }
                })
                .addOnFailureListener(e -> {
                    pbSearchLoading.setVisibility(View.GONE);
                    // Lỗi thường gặp ở đây: thiếu composite index cho whereArrayContains + orderBy
                    // - Firestore sẽ trả link tạo index sẵn trong Logcat, bấm link đó là xong.
                });
    }
}
