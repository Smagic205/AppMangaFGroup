package com.example.bookapp.Repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bookapp.Model.Voucher;
import com.example.bookapp.Utils.Constants;
import com.example.bookapp.Utils.FirebaseUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository lấy dữ liệu voucher cho người dùng.
 * Đọc thủ công từng field, hỗ trợ cả 2 cách đặt tên field trong Firestore:
 *   - "kind" hoặc "type" cho loại voucher
 *   - "active" hoặc "isActive" cho trạng thái
 */
public class VoucherRepository {

    /** Parse thủ công DocumentSnapshot → Voucher, chấp nhận cả hai tên field */
    private Voucher fromDoc(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        // Log raw data để debug
        Map<String, Object> raw = doc.getData();
        System.err.println("[VOUCHER_PARSE] id=" + doc.getId() + " raw=" + raw);

        Voucher v = new Voucher();
        v.setVoucherId(doc.getId());
        v.setCode(doc.getString("code"));

        // Đọc loại voucher — thử "kind" trước, nếu null thử "type"
        String kind = doc.getString("kind");
        if (kind == null) kind = doc.getString("type");
        v.setKind(kind);

        // Đọc giá trị giảm
        Double value = doc.getDouble("value");
        v.setValue(value != null ? value : 0);

        v.setStartDate(doc.getTimestamp("startDate"));
        v.setEndDate(doc.getTimestamp("endDate"));

        // Đọc trạng thái active — thử "active" trước, nếu null thử "isActive"
        Boolean active = doc.getBoolean("active");
        if (active == null) active = doc.getBoolean("isActive");
        v.setActive(Boolean.TRUE.equals(active));

        System.err.println("[VOUCHER_PARSE] → code=" + v.getCode()
                + " kind=" + v.getKind()
                + " value=" + v.getValue()
                + " active=" + v.isActive()
                + " endDate=" + v.getEndDate());

        return v;
    }

    /** Lấy tất cả voucher còn hiệu lực */
    public LiveData<List<Voucher>> getAvailableVouchers() {
        MutableLiveData<List<Voucher>> liveData = new MutableLiveData<>();

        System.err.println("[VOUCHER] getAvailableVouchers() bắt đầu...");

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_VOUCHERS)
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    System.err.println("[VOUCHER] Số docs nhận được: " + querySnapshot.size());
                    List<Voucher> vouchers = new ArrayList<>();
                    Timestamp now = Timestamp.now();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Voucher voucher = fromDoc(doc);
                        if (voucher == null) continue;

                        boolean isActive = voucher.isActive();
                        boolean notExpired = voucher.getEndDate() == null
                                || voucher.getEndDate().compareTo(now) >= 0;

                        System.err.println("[VOUCHER] Filter: code=" + voucher.getCode()
                                + " isActive=" + isActive + " notExpired=" + notExpired);

                        if (isActive && notExpired) {
                            vouchers.add(voucher);
                        }
                    }
                    System.err.println("[VOUCHER] Voucher hợp lệ cuối cùng: " + vouchers.size());
                    liveData.setValue(vouchers);
                })
                .addOnFailureListener(e -> {
                    System.err.println("[VOUCHER] LỖI Firestore: " + e.getMessage());
                    liveData.setValue(new ArrayList<>());
                });

        return liveData;
    }

    /** Kiểm tra 1 mã voucher nhập tay có hợp lệ không */
    public LiveData<Voucher> getVoucherByCode(String code) {
        MutableLiveData<Voucher> liveData = new MutableLiveData<>();

        FirebaseUtils.getFirestore().collection(Constants.COLLECTION_VOUCHERS)
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        liveData.setValue(null);
                    } else {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Voucher voucher = fromDoc(doc);
                        if (voucher != null && voucher.isActive()) {
                            boolean notExpired = voucher.getEndDate() == null
                                    || voucher.getEndDate().compareTo(Timestamp.now()) >= 0;
                            liveData.setValue(notExpired ? voucher : null);
                        } else {
                            liveData.setValue(null);
                        }
                    }
                })
                .addOnFailureListener(e -> liveData.setValue(null));

        return liveData;
    }
}
