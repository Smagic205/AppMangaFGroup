package com.example.bookapp.Repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bookapp.Model.Voucher;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DUY NHẤT được phép gọi Firestore cho collection "vouchers".
 * Dùng bởi SelectVoucherActivity, CheckoutActivity.
 */
public class VoucherRepository {

    /** Voucher còn hiệu lực: isActive=true và endDate >= hiện tại. */
    public LiveData<List<Voucher>> getAvailableVouchers() {
        MutableLiveData<List<Voucher>> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_VOUCHERS)
                .whereEqualTo("isActive", true)
                .whereGreaterThanOrEqualTo("endDate", Timestamp.now())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Voucher> vouchers = new ArrayList<>();
                    querySnapshot.forEach(doc -> {
                        Voucher voucher = doc.toObject(Voucher.class);
                        voucher.setVoucherId(doc.getId());
                        vouchers.add(voucher);
                    });
                    liveData.setValue(vouchers);
                })
                .addOnFailureListener(e -> liveData.setValue(new ArrayList<>()));

        return liveData;
    }

    /** Kiểm tra 1 mã voucher nhập tay có hợp lệ không - dùng ở ô "Nhập mã" trong SelectVoucherActivity. */
    public LiveData<Voucher> getVoucherByCode(String code) {
        MutableLiveData<Voucher> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_VOUCHERS)
                .whereEqualTo("code", code)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        liveData.setValue(null); // mã không hợp lệ hoặc đã hết hạn
                    } else {
                        Voucher voucher = querySnapshot.getDocuments().get(0).toObject(Voucher.class);
                        if (voucher != null) {
                            voucher.setVoucherId(querySnapshot.getDocuments().get(0).getId());
                        }
                        liveData.setValue(voucher);
                    }
                })
                .addOnFailureListener(e -> liveData.setValue(null));

        return liveData;
    }
}
