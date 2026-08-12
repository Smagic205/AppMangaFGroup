package com.example.bookapp.View.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.BookAdapter;
import com.example.bookapp.Adapter.user.CategoryAdapter;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Category;
import com.example.bookapp.Model.User;
import com.example.bookapp.R;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvCategories, rvFeaturedBooks, rvAllBooks;
    private ProgressBar pbLoading;
    private LinearLayout llEmptyState, llSearchBar;
    private TextView tvGreeting, tvUserName, tvSearchHint, tvSeeAllCategory, tvSeeAllFeatured;
    private ImageButton ibNotification;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<Book> featuredBookList = new ArrayList<>();
    private final List<Book> allBookList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerViews();
        setupClicks();
        loadGreeting();
        loadCategories();
        loadFeaturedBooks();
        loadAllBooks();
    }

    private void bindViews(View view) {
        rvCategories = view.findViewById(R.id.rv_categories);
        rvFeaturedBooks = view.findViewById(R.id.rv_featured_books);
        rvAllBooks = view.findViewById(R.id.rv_all_books);
        pbLoading = view.findViewById(R.id.pb_loading);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        llSearchBar = view.findViewById(R.id.ll_search_bar);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvSearchHint = view.findViewById(R.id.tv_search_hint);
        tvSeeAllCategory = view.findViewById(R.id.tv_see_all_category);
        tvSeeAllFeatured = view.findViewById(R.id.tv_see_all_featured);
        ibNotification = view.findViewById(R.id.ib_notification);
    }

    private void setupRecyclerViews() {
        rvCategories.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(new CategoryAdapter(categoryList, this::openBooksByCategory));

        rvFeaturedBooks.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedBooks.setAdapter(new BookAdapter(featuredBookList, this::openBookDetail));

        rvAllBooks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAllBooks.setAdapter(new BookAdapter(allBookList, this::openBookDetail));
    }

    private void setupClicks() {
        llSearchBar.setOnClickListener(v -> startActivity(new Intent(getContext(), SearchActivity.class)));
        ibNotification.setOnClickListener(v -> startActivity(new Intent(getContext(), NotificationActivity.class)));
        // tv_see_all_category / tv_see_all_featured: điều hướng sang màn danh sách đầy đủ
        // riêng (chưa có trong phạm vi hiện tại) - tạm mở SearchActivity làm nơi duyệt chung.
        tvSeeAllCategory.setOnClickListener(v -> startActivity(new Intent(getContext(), SearchActivity.class)));
        tvSeeAllFeatured.setOnClickListener(v -> startActivity(new Intent(getContext(), SearchActivity.class)));
    }

    private void openBookDetail(Book book) {
        Intent intent = new Intent(getContext(), BookDetailActivity.class);
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, book.getBookId());
        startActivity(intent);
    }

    private void openBooksByCategory(Category category) {
        Intent intent = new Intent(getContext(), SearchActivity.class);
        intent.putExtra(SearchActivity.EXTRA_CATEGORY_ID, category.getCategoryId());
        startActivity(intent);
    }

    private void loadGreeting() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null && isAdded()) {
                        tvUserName.setText(user.getFullName());
                    }
                });
    }

    private void loadCategories() {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_CATEGORIES)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    categoryList.clear();
                    querySnapshot.forEach(doc -> {
                        Category category = doc.toObject(Category.class);
                        category.setCategoryId(doc.getId());
                        categoryList.add(category);
                    });
                    rvCategories.getAdapter().notifyDataSetChanged();
                });
    }

    private void loadFeaturedBooks() {
        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .whereEqualTo("isFeatured", true)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    featuredBookList.clear();
                    querySnapshot.forEach(doc -> {
                        Book book = doc.toObject(Book.class);
                        book.setBookId(doc.getId());
                        featuredBookList.add(book);
                    });
                    rvFeaturedBooks.getAdapter().notifyDataSetChanged();
                });
    }

    private void loadAllBooks() {
        pbLoading.setVisibility(View.VISIBLE);

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_BOOKS)
                .whereEqualTo("isActive", true)
                .limit(30)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    pbLoading.setVisibility(View.GONE);

                    allBookList.clear();
                    querySnapshot.forEach(doc -> {
                        Book book = doc.toObject(Book.class);
                        book.setBookId(doc.getId());
                        allBookList.add(book);
                    });
                    rvAllBooks.getAdapter().notifyDataSetChanged();

                    boolean isEmpty = allBookList.isEmpty();
                    llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvAllBooks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) pbLoading.setVisibility(View.GONE);
                });
    }
}
