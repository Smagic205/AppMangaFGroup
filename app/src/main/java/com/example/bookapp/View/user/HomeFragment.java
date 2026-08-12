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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Adapter.user.BookAdapter;
import com.example.bookapp.Adapter.user.CategoryAdapter;
import com.example.bookapp.Model.Book;
import com.example.bookapp.Model.Category;
import com.example.bookapp.R;
import com.example.bookapp.Utils.FirebaseUtils;
import com.example.bookapp.ViewModel.HomeViewModel;

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

    private HomeViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Observe 4 LiveData, Fragment không gọi Firestore trực tiếp
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) tvUserName.setText(user.getFullName());
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryList.clear();
            if (categories != null) categoryList.addAll(categories);
            rvCategories.getAdapter().notifyDataSetChanged();
        });

        viewModel.getFeaturedBooks().observe(getViewLifecycleOwner(), books -> {
            featuredBookList.clear();
            if (books != null) featuredBookList.addAll(books);
            rvFeaturedBooks.getAdapter().notifyDataSetChanged();
        });

        viewModel.getAllBooks().observe(getViewLifecycleOwner(), books -> {
            pbLoading.setVisibility(View.GONE);
            allBookList.clear();
            if (books != null) allBookList.addAll(books);
            rvAllBooks.getAdapter().notifyDataSetChanged();

            boolean isEmpty = allBookList.isEmpty();
            llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvAllBooks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        pbLoading.setVisibility(View.VISIBLE);
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid != null) viewModel.load(uid);
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
}
