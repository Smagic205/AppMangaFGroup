package com.example.bookapp.Repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.bookapp.Model.Voucher;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseCallback;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminVoucherRepository {

    private final CollectionReference vouchersRef =
            FirebaseUtils.getFirestore().collection(Constants.COLLECTION_VOUCHERS);

    /** Realtime toàn bộ voucher — dùng cho ManageVoucherActivity. */
    public LiveData<List<Voucher>> observeAllVouchers() {
        return new FirestoreListLiveData<>(vouchersRef, Voucher.class);
    }

    public void addVoucher(@NonNull Voucher voucher, FirebaseCallback<Void> callback) {
        DocumentReference docRef = vouchersRef.document();
        voucher.setVoucherId(docRef.getId());
        docRef.set(voucher)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void updateVoucher(@NonNull Voucher voucher, FirebaseCallback<Void> callback) {
        vouchersRef.document(voucher.getVoucherId()).set(voucher)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /** Bật/tắt nhanh bằng switch trên item_admin_voucher (sw_voucher_active). */
    public void toggleActive(String voucherId, boolean isActive, FirebaseCallback<Void> callback) {
        vouchersRef.document(voucherId).update(Constants.FIELD_IS_ACTIVE, isActive)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /** Voucher là dữ liệu cấu hình, không bị đơn hàng cũ tham chiếu trực tiếp nên cho xóa cứng. */
    public void deleteVoucher(String voucherId, FirebaseCallback<Void> callback) {
        vouchersRef.document(voucherId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /** Sắp xếp sắp hết hạn lên trước — endDate tăng dần. */
    public List<Voucher> sortByExpiringFirst(List<Voucher> source) {
        List<Voucher> sorted = new ArrayList<>(source);
        sorted.sort((a, b) -> {
            if (a.getEndDate() == null || b.getEndDate() == null) return 0;
            return a.getEndDate().compareTo(b.getEndDate());
        });
        return sorted;
    }

    /** Sắp xếp giá trị giảm cao nhất lên trước. */
    public List<Voucher> sortByValueDesc(List<Voucher> source) {
        List<Voucher> sorted = new ArrayList<>(source);
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted;
    }

    public List<Voucher> filterByCode(List<Voucher> source, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return source;
        List<Voucher> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT).trim();
        for (Voucher v : source) {
            if (v.getCode() != null && v.getCode().toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                result.add(v);
            }
        }
        return result;
    }
}
