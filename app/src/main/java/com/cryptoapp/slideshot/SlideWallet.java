package com.cryptoapp.slideshot;

import android.graphics.Bitmap;
import android.transition.Slide;
import android.util.Log;

import com.google.common.base.Joiner;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;

/**
 * Created by DestinationX on 9/20/2018.
 */

public class SlideWallet {

    private Bitmap photo;
    private int balance;
    private byte[] photoInBytes;
    private byte[] mSeed = null;
    private NetworkParameters mParams = null;
    private List<String> mWordList = null;
    private DeterministicKey mKey = null;
    protected DeterministicKey mRoot = null;


    public SlideWallet(Bitmap photo) {
        this.photo = photo;
    }

    public SlideWallet(NetworkParameters params, MnemonicCode mc, byte[] seed) throws MnemonicException.MnemonicLengthException {
        this.mParams = params;
        this.mSeed = seed;

        mWordList = mc.toMnemonic(seed);
        Log.i("mnemonic", Joiner.on(" ").join(mWordList));
        byte[] hd_seed = MnemonicCode.toSeed(mWordList, "");
        Log.i("HD_SEED", hd_seed.toString());
        mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        Log.i("MasterPrivateKey",mKey.toString());
//        DeterministicKey t1 = HDKeyDerivation.deriveChildKey(mKey, purpose| ChildNumber.HARDENED_BIT);
//        int coin = SamouraiWallet.getInstance().isTestNet() ? (1 | ChildNumber.HARDENED_BIT) : ChildNumber.HARDENED_BIT;
//        mRoot = HDKeyDerivation.deriveChildKey(t1, coin);
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
