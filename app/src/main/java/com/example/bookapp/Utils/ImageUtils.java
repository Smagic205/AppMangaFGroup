package com.example.bookapp.Utils;

import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.bookapp.R;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

/**
 * Gom logic upload ảnh lên Firebase Storage (ảnh bìa sách, avatar user/tác giả/NXB) và
 * load ảnh hiển thị bằng Glide vào 1 chỗ, tránh viết lại ở AddEditBookActivity,
 * EditProfileActivity, dialog thêm tác giả/NXB...
 *
 * Dùng được cho cả ImageView thường lẫn ShapeableImageView (vì ShapeableImageView kế
 * thừa trực tiếp từ ImageView).
 */
public class ImageUtils {

    public interface OnUploadCompleteListener {
        void onSuccess(String downloadUrl);

        void onFailure(Exception e);
    }

    private ImageUtils() {
    }

    /**
     * Upload 1 ảnh (Uri lấy từ thư viện ảnh/camera qua ActivityResultLauncher) lên
     * Firebase Storage, trả về downloadUrl để lưu vào Firestore (vd Book.coverImageUrl).
     *
     * @param folder thư mục đích trong Storage — dùng hằng số Constants.STORAGE_* (vd
     *               Constants.STORAGE_BOOK_COVERS)
     */
    public static void uploadImage(@NonNull Uri imageUri, @NonNull String folder,
                                    @NonNull OnUploadCompleteListener listener) {
        String fileName = folder + UUID.randomUUID() + ".jpg";
        StorageReference ref = FirebaseUtils.getStorageRef().child(fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> listener.onSuccess(uri.toString()))
                        .addOnFailureListener(listener::onFailure))
                .addOnFailureListener(listener::onFailure);
    }

    /** Tải ảnh từ URL vào ImageView/ShapeableImageView, dùng placeholder mặc định của app. */
    public static void loadImage(@NonNull ImageView imageView, @Nullable String imageUrl) {
        loadImage(imageView, imageUrl, R.drawable.bg_thumbnail_placeholder);
    }

    public static void loadImage(@NonNull ImageView imageView, @Nullable String imageUrl,
                                  @DrawableRes int placeholderResId) {
        Glide.with(imageView.getContext())
                .load(imageUrl)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    /** Tải ảnh đại diện (avatar) — dùng riêng để sau này dễ đổi placeholder avatar khác ảnh bìa sách. */
    public static void loadAvatar(@NonNull ImageView imageView, @Nullable String avatarUrl) {
        loadImage(imageView, avatarUrl, R.drawable.bg_thumbnail_placeholder);
    }
}
