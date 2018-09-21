package com.cryptoapp.slideshot;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by DestinationX on 9/20/2018.
 */

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.PhotoCardHolder>{

    private List<WalletInfo> walletInfo;

    public CustomAdapter(List<WalletInfo> walletInfo) {
        this.walletInfo = walletInfo;
    }

    @NonNull
    @Override
    public PhotoCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.list_photo_wallet, parent, false);

        return new PhotoCardHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoCardHolder holder, int position) {
            WalletInfo info = walletInfo.get(position);
            PhotoCardHolder.photoView.setImageBitmap();
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class PhotoCardHolder extends RecyclerView.ViewHolder {

        static ImageView photoView;
        static TextView accountBalanceView;

        public PhotoCardHolder(View itemView) {
            super(itemView);

            photoView = itemView.findViewById(R.id.photoView);
            accountBalanceView = itemView.findViewById(R.id.accountBalanceView);



        }


    }
}
