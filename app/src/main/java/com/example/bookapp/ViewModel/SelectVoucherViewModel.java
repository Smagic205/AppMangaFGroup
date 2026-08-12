package com.example.bookapp.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bookapp.Model.Voucher;
import com.example.bookapp.Repository.VoucherRepository;

import java.util.List;

/**
 * ViewModel cho SelectVoucherActivity.
 * Tách load voucher + kiểm tra mã nhập tay ra khỏi Activity.
 */
public class SelectVoucherViewModel extends ViewModel {

    private final VoucherRepository voucherRepository = new VoucherRepository();

    private final MutableLiveData<List<Voucher>> _vouchers = new MutableLiveData<>();
    /** null = mã không hợp lệ; non-null = mã hợp lệ và có thể dùng. */
    private final MutableLiveData<Voucher> _validatedVoucher = new MutableLiveData<>();

    public LiveData<List<Voucher>> getVouchers() { return _vouchers; }
    public LiveData<Voucher> getValidatedVoucher() { return _validatedVoucher; }

    public void loadAvailableVouchers() {
        voucherRepository.getAvailableVouchers().observeForever(
                vouchers -> _vouchers.setValue(vouchers));
    }

    /** Kiểm tra mã voucher nhập tay có hợp lệ không. */
    public void validateCode(String code) {
        voucherRepository.getVoucherByCode(code).observeForever(
                voucher -> _validatedVoucher.setValue(voucher));
    }
}
