package com.cryptoapp.slideshot;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
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
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CHOOSE_PHOTO = 2;


    private String mCurrentPhotoPath;


    private Uri fileUri;


    private ImageView mPhotoView;

    private RecyclerView recyclerView;

    private WalletAdapter walletAdapter;

    private ArrayList<SlideWallet> walletList = new ArrayList<>();

    private FloatingActionMenu fabMenu;

    private ProgressBar progressBar;

    private Context mContext;

    public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";


    NetworkParameters params = TestNet3Params.get();

    long unixTime = System.currentTimeMillis() / 1000L;
    FloatingActionButton mGetPhoto, mTakePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGetPhoto = findViewById(R.id.choosePhoto);
        progressBar = findViewById(R.id.indeterminateBar);
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
                startActivityForResult(photoPickerIntent, REQUEST_CHOOSE_PHOTO);
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

            if (reqCode == REQUEST_IMAGE_CAPTURE) {

                Uri uri = fileUri;

                Bitmap img = BitmapFactory.decodeFile(uri.getPath());
//

                walletList.add(
                        new SlideWallet(img)
                );
                walletAdapter.notifyDataSetChanged();

                new ConvertBitmapToWallet().execute(img);


            }

            if (reqCode == REQUEST_CHOOSE_PHOTO) {
                if(resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        walletList.add(
                                new SlideWallet(selectedImage)
                        );
                        walletAdapter.notifyDataSetChanged();

                        new ConvertBitmapToWallet().execute(selectedImage);


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(this, "No img selected", Toast.LENGTH_LONG).show();
                }

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



    // The types specified here are the input data type, the progress type, and the result type
    private class ConvertBitmapToWallet extends AsyncTask<Bitmap, String, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Bitmap bmp = bitmaps[0];
            String imgString = Base64.encodeToString(getBytesFromBitmap(bmp),
                    Base64.NO_WRAP);


            String hashed = Hashing.sha512()
                    .hashString(imgString, StandardCharsets.UTF_8)
                    .toString();

            //check hash in console
            Log.i("imgstringhash", hashed);
            try {
                InputStream wis = MainActivity.this.getResources().getAssets().open("BIP39/en.txt");
                MnemonicCode mc = new MnemonicCode(wis, BIP39_ENGLISH_SHA256);
                byte[] seed = hashed.substring(0, 32).getBytes();
                SlideWallet sw = new SlideWallet(params, mc, seed);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (MnemonicException.MnemonicLengthException e) {
                e.printStackTrace();
            }


//            DeterministicSeed seed = new DeterministicSeed(hashed.substring(0,32).getBytes(), "", unixTime);
//            Wallet wallet = Wallet.fromSeed(params, seed);
//                blockChain = new BlockChain(params, wallet, blockStore);
//                peerGroup.addWallet(wallet);
//                peerGroup.startAsync();
//            String seedPhrase = Joiner.on(" ").join(seed.getMnemonicCode());
////                    textView.setText(wallet.toString());
//            Log.i("wallet", wallet.toString());
//            Log.i("SlideWallet", wallet.currentReceiveAddress().toString());

            return null;
        }

        protected void onPreExecute() {
            // Runs on the UI thread before doInBackground
            // Good for toggling visibility of a progress indicator
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }


        protected void onProgressUpdate(String... values) {
            // Executes whenever publishProgress is called from doInBackground
            // Used to update the progress indicator
//            progressBar.setProgress(values[0]);

        }

        protected void onPostExecute(String result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
//            imageView.setImageBitmap(result);
//            // Hide the progress bar
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            final AlertDialog.Builder dialog =
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("DONE!");
            final AlertDialog alert = dialog.create();
            alert.show();

// Hide after some seconds
            final Handler handler  = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (alert.isShowing()) {
                        alert.dismiss();
                    }
                }
            };

            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    handler.removeCallbacks(runnable);
                }
            });

            handler.postDelayed(runnable, 1000);

            Log.i("onPostExecute called...", "All done!");
        }
    }



}
