package com.cryptoapp.slideshot;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final int RESULT_LOAD_IMG = 1;
    ImageView image_view;
    TextView textView;
    NetworkParameters params = TestNet3Params.get();
    long unixTime = System.currentTimeMillis() / 1000L;
    Button mGetPhoto, mTakePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image_view = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        mGetPhoto = findViewById(R.id.getPhoto);
        mTakePhoto = findViewById(R.id.takePhoto);

        mGetPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get photo from gallery
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
            }
        });

        mTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //take photo with camera
            }
        });

    }


    /*
    * Returns photo from gallery
    * Converts bitmap image into byte array with GetBytesFromBitmap
    * Hashes byte array with sha512
    * Uses substring of digest as Deterministic seed entropy value
    * TODO: change unixtime variable to reflect when the wallet was created from the image
    * TODO: and not when it is retrieved from gallery
    *
    * */

        @Override
        protected void onActivityResult(int reqCode, int resultCode, Intent data) {
            super.onActivityResult(reqCode, resultCode, data);


            if (resultCode == RESULT_OK) {
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    image_view.setImageBitmap(selectedImage);
                    String imgString = Base64.encodeToString(getBytesFromBitmap(selectedImage),
                            Base64.NO_WRAP);


                    String hashed = Hashing.sha512()
                            .hashString(imgString, StandardCharsets.UTF_8)
                            .toString();


                    Log.i("imgstringhash", hashed);
                    DeterministicSeed seed = new DeterministicSeed(hashed.substring(0,16).getBytes(), "", unixTime);
                    Wallet wallet = Wallet.fromSeed(params, seed);
//                blockChain = new BlockChain(params, wallet, blockStore);
//                peerGroup.addWallet(wallet);
//                peerGroup.startAsync();
                    String seedPhrase = Joiner.on(" ").join(seed.getMnemonicCode());
                    textView.setText(wallet.toString());
                    Log.i("wallet", wallet.toString());
                    Log.i("Wallet", wallet.currentReceiveAddress().toString());



                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                }

            }else {
                Toast.makeText(this, "You haven't picked an Image",Toast.LENGTH_LONG).show();
            }
        }







        // convert from bitmap to byte array
        public byte[] getBytesFromBitmap(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return stream.toByteArray();
        }



}
