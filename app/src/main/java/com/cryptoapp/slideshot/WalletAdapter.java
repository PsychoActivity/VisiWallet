package com.cryptoapp.slideshot;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DestinationX on 9/20/2018.
 */

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.ViewHolder> {

    private List<SlideWallet> mSlideWalletList = new ArrayList<SlideWallet>();

    public WalletAdapter(List<SlideWallet> slideWalletList) {
        this.mSlideWalletList = slideWalletList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);


        View contactView = inflater.inflate(R.layout.list_photo_wallet, parent, false);

        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
            SlideWallet slideWallet = mSlideWalletList.get(position);

            ImageView photo = holder.imageWalletView;
            photo.setImageBitmap(slideWallet.getPhoto());
            TextView balanceView = holder.accountBalanceView;
            balanceView.setText("0.00 BTC");

    }

    @Override
    public int getItemCount() {
        if (mSlideWalletList == null)
        return 0;
        else
        return  mSlideWalletList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView imageWalletView;
        public TextView accountBalanceView;

        public ViewHolder(View itemView) {
            super(itemView);

            imageWalletView = itemView.findViewById(R.id.photoView);
            accountBalanceView = itemView.findViewById(R.id.accountBalanceView);


        }

    }


}
