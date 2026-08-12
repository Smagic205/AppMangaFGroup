package com.example.bookapp.Adapter.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.Model.Voucher;
import com.example.bookapp.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    public interface OnVoucherSelectListener {
        void onSelect(Voucher voucher);
    }

    private final List<Voucher> vouchers;
    private final OnVoucherSelectListener listener;
    private String selectedVoucherId;

    public VoucherAdapter(List<Voucher> vouchers, OnVoucherSelectListener listener) {
        this.vouchers = vouchers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        holder.tvCode.setText(voucher.getCode());
        holder.tvDesc.setText(buildDescription(voucher));

        if (voucher.getEndDate() != null) {
            holder.tvExpiry.setText("HSD: " + sdf.format(voucher.getEndDate().toDate()));
        }

        holder.rbSelect.setChecked(voucher.getVoucherId().equals(selectedVoucherId));

        holder.itemView.setOnClickListener(v -> {
            selectedVoucherId = voucher.getVoucherId();
            notifyDataSetChanged();
            listener.onSelect(voucher);
        });
    }

    private String buildDescription(Voucher voucher) {
        switch (voucher.getType()) {
            case "percent":
                return "Giảm " + (int) voucher.getValue() + "% cho đơn hàng";
            case "fixed":
                return "Giảm " + (int) voucher.getValue() + "đ cho đơn hàng";
            case "freeship":
                return "Miễn phí vận chuyển toàn đơn";
            default:
                return "";
        }
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    public String getSelectedVoucherId() {
        return selectedVoucherId;
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvDesc, tvExpiry;
        RadioButton rbSelect;

        VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_voucher_code);
            tvDesc = itemView.findViewById(R.id.tv_voucher_desc);
            tvExpiry = itemView.findViewById(R.id.tv_voucher_expiry);
            rbSelect = itemView.findViewById(R.id.rb_voucher_select);
        }
    }
}
