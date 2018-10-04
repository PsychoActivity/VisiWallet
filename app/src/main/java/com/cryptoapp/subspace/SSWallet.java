package com.cryptoapp.subspace;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.common.base.Joiner;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.util.List;

/**
 * Created by DestinationX on 9/20/2018.
 */

public class SSWallet {

    private Bitmap photo;
    private double balance;
    private String balanceString;
    private byte[] photoInBytes;
    private byte[] mSeed = null;
    private NetworkParameters mParams = null;
    private List<String> mWordList = null;
    private DeterministicKey mKey = null;
    protected DeterministicKey mRoot = null;


    public SSWallet(Bitmap photo, String s) {
        this.photo = photo;
        this.balanceString = s;
    }

    public SSWallet(Bitmap photo, double balance) {
        this.photo = photo;
        this.balance = balance;
    }

    public SSWallet(NetworkParameters params, MnemonicCode mc, byte[] seed) throws MnemonicException.MnemonicLengthException {
        this.mParams = params;
        this.mSeed = seed;

        mWordList = mc.toMnemonic(seed);
        Log.i("mnemonic", Joiner.on(" ").join(mWordList));
        byte[] hd_seed = MnemonicCode.toSeed(mWordList, "");
        Log.i("HD_SEED", hd_seed.toString());
        mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        Log.i("MasterPrivateKey", mKey.toString());
        DeterministicKey t1 = HDKeyDerivation.deriveChildKey(mKey, ChildNumber.HARDENED_BIT);
        Log.i("DeterministicKey", t1.toString());
        int coin = (1 | ChildNumber.HARDENED_BIT);
        Log.i("Coin", String.valueOf(coin));
        mRoot = HDKeyDerivation.deriveChildKey(t1, coin);
        Log.i("root", mRoot.toString());
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

    public double getBalance() {
        return balance;
    }

    public String getBalanceString() {
        return balanceString;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
