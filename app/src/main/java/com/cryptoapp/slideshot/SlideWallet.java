package com.cryptoapp.slideshot;

import android.graphics.Bitmap;

/**
 * Created by DestinationX on 9/20/2018.
 */

public class SlideWallet {

    private Bitmap photo;
    private int balance;
    private byte[] photoInBytes;

    public SlideWallet(Bitmap photo) {
        this.photo = photo;
    }

    public SlideWallet() {

    }


    public Bitmap getPhoto() {
        return photo;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
    }

    public byte[] getPhotoInBytes() {
        return photoInBytes;
    }

    public void setPhotoInBytes(byte[] photoInBytes) {
        this.photoInBytes = photoInBytes;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
}
