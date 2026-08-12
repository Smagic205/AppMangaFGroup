package com.example.bookapp.View.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bookapp.Model.User;
import com.example.bookapp.R;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private ImageButton ibEditProfile;
    private TextView tvName, tvEmail, tvViewOrderHistory;
    private LinearLayout llMenuPersonalInfo, llMenuAddress, llMenuNotification,
            llMenuReviews, llMenuSettings,
            llStatusPending, llStatusShipping, llStatusDelivered, llStatusCancelled;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClicks();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
    }

    private void bindViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);
        ibEditProfile = view.findViewById(R.id.ib_edit_profile);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvViewOrderHistory = view.findViewById(R.id.tv_view_order_history);
        llMenuPersonalInfo = view.findViewById(R.id.ll_menu_personal_info);
        llMenuAddress = view.findViewById(R.id.ll_menu_address);
        llMenuNotification = view.findViewById(R.id.ll_menu_notification);
        llMenuReviews = view.findViewById(R.id.ll_menu_reviews);
        llMenuSettings = view.findViewById(R.id.ll_menu_settings);
        llStatusPending = view.findViewById(R.id.ll_status_pending);
        llStatusShipping = view.findViewById(R.id.ll_status_shipping);
        llStatusDelivered = view.findViewById(R.id.ll_status_delivered);
        llStatusCancelled = view.findViewById(R.id.ll_status_cancelled);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupClicks() {
        ibEditProfile.setOnClickListener(v ->
                startActivity(new Intent(getContext(), EditProfileActivity.class)));
        llMenuPersonalInfo.setOnClickListener(v ->
                startActivity(new Intent(getContext(), EditProfileActivity.class)));
        llMenuAddress.setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddressListActivity.class)));
        llMenuNotification.setOnClickListener(v ->
                startActivity(new Intent(getContext(), NotificationActivity.class)));
        llMenuReviews.setOnClickListener(v ->
                startActivity(new Intent(getContext(), MyReviewsActivity.class)));
        llMenuSettings.setOnClickListener(v -> {
            // TODO: màn Cài đặt chưa có trong phạm vi hiện tại
        });

        tvViewOrderHistory.setOnClickListener(v -> openOrderHistory(null));
        llStatusPending.setOnClickListener(v -> openOrderHistory(Constants.ORDER_PENDING));
        llStatusShipping.setOnClickListener(v -> openOrderHistory(Constants.ORDER_SHIPPING));
        llStatusDelivered.setOnClickListener(v -> openOrderHistory(Constants.ORDER_DELIVERED));
        llStatusCancelled.setOnClickListener(v -> openOrderHistory(Constants.ORDER_CANCELLED));

        btnLogout.setOnClickListener(v -> {
            FirebaseUtils.getAuth().signOut();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });
    }

    private void openOrderHistory(@Nullable String statusFilter) {
        Intent intent = new Intent(getContext(), OrderHistoryActivity.class);
        if (statusFilter != null) {
            intent.putExtra(OrderHistoryActivity.EXTRA_INITIAL_STATUS, statusFilter);
        }
        startActivity(intent);
    }

    private void loadUserInfo() {
        String uid = FirebaseUtils.getCurrentUserId();
        if (uid == null) return;

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    User user = doc.toObject(User.class);
                    if (user == null) return;

                    tvName.setText(user.getFullName());
                    tvEmail.setText(user.getEmail());

                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(ivAvatar);
                    }
                });
    }
}
