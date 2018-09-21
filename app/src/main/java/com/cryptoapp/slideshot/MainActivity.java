package com.cryptoapp.slideshot;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;


    private String mCurrentPhotoPath;


    private Uri fileUri;


    private ImageView mPhotoView;

    private RecyclerView recyclerView;

    private WalletAdapter walletAdapter;

    private ArrayList<SlideWallet> walletList = new ArrayList<>();

    private FloatingActionMenu fabMenu;


    NetworkParameters params = TestNet3Params.get();

    long unixTime = System.currentTimeMillis() / 1000L;
    FloatingActionButton mGetPhoto, mTakePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGetPhoto = findViewById(R.id.choosePhoto);
        mTakePhoto = findViewById(R.id.takePhoto);
        recyclerView = findViewById(R.id.recyclerView);
        fabMenu = findViewById(R.id.fabMenu);
        walletAdapter = new WalletAdapter(walletList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(walletAdapter);

        mGetPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get photo from gallery
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
                fabMenu.close(true);
            }
        });

        mTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
                fabMenu.close(true);

            }
        });

    }

    private void dispatchTakePictureIntent() {

        //TODO: Add custom camera class instead of using INTENT

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                fileUri = Uri.fromFile(photoFile);
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.cryptoapp.slideshot.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
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

            if (reqCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

                Uri uri = fileUri;

                Bitmap img = BitmapFactory.decodeFile(uri.getPath());
//

                walletList.add(
                        new SlideWallet(img)
                );
                walletAdapter.notifyDataSetChanged();

                // get the base 64 string
                String imgString = Base64.encodeToString(getBytesFromBitmap(img),
                        Base64.NO_WRAP);

                String hashed = Hashing.sha512()
                        .hashString(imgString, StandardCharsets.UTF_8)
                        .toString();

                Log.i("imgstring", hashed);
                DeterministicSeed seed = new DeterministicSeed(hashed.substring(0,32).getBytes(), "", 100L);
                Wallet wallet = Wallet.fromSeed(params, seed);
                String walletString = Joiner.on(" ").join(seed.getMnemonicCode());
                Log.i("Wallet",walletString);
                Log.i("Wallet", wallet.currentReceiveAddress().toString());


            } else if (reqCode == 0 && resultCode == RESULT_OK) {
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    walletList.add(
                            new SlideWallet(selectedImage)
                    );
                    walletAdapter.notifyDataSetChanged();
                    String imgString = Base64.encodeToString(getBytesFromBitmap(selectedImage),
                            Base64.NO_WRAP);


                    String hashed = Hashing.sha512()
                            .hashString(imgString, StandardCharsets.UTF_8)
                            .toString();


                    Log.i("imgstringhash", hashed);
                    DeterministicSeed seed = new DeterministicSeed(hashed.substring(0,32).getBytes(), "", unixTime);
                    Wallet wallet = Wallet.fromSeed(params, seed);
//                blockChain = new BlockChain(params, wallet, blockStore);
//                peerGroup.addWallet(wallet);
//                peerGroup.startAsync();
                    String seedPhrase = Joiner.on(" ").join(seed.getMnemonicCode());
//                    textView.setText(wallet.toString());
                    Log.i("wallet", wallet.toString());
                    Log.i("SlideWallet", wallet.currentReceiveAddress().toString());



                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(this, "You haven't picked an Image",Toast.LENGTH_LONG).show();
            }
        }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }







        // convert from bitmap to byte array
        public byte[] getBytesFromBitmap(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return stream.toByteArray();
        }



}
