package com.cryptoapp.subspace;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.common.base.Stopwatch;
import com.google.common.hash.Hashing;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CHOOSE_PHOTO = 2;
    private static final String SETUP_SETTINGS = "Setup Settings";
    private static final String FIRST_TIME_SETUP = "FirstTimeSetup";
    private boolean firstTimeSetup;
    double balance;
    int currentListPosition;

    //bitcoinj wallet objects
    private Wallet wallet;
    private SPVBlockStore chainStore;
    private BlockChain chain;
    private PeerGroup peerGroup;

    //walletappkit

    private WalletAppKit kit;
    private File chainFile;
    private String mCurrentPhotoPath;
    private Uri fileUri;
    private Long earliestKeyCreationTime = 1537142400L;
    private ImageView mPhotoView;
    private RecyclerView recyclerView;
    DeterministicSeed seed;
    private Bitmap image;
    CheckpointManager checkpointManager;
    private WalletAdapter walletAdapter;
    private ArrayList<SSWallet> ssWalletList = new ArrayList<>();
    private FloatingActionMenu fabMenu;
    private ProgressBar progressBar;
    private Context mContext;
    public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";
    NetworkParameters params = TestNet3Params.get();
    long unixTime = System.currentTimeMillis() / 1000L;
    FloatingActionButton mGetPhoto, mTakePhoto;
    Stopwatch sw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //TODO: STOP OS FROM BACKING UP PHOTOS AUTOMATICALLY
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chainFile = new File(getFilesDir(), "subspace.spvchain");

        try {
            chainStore = new SPVBlockStore(params, chainFile);
            chain = new BlockChain(params, chainStore);
            peerGroup = new PeerGroup(params, chain);
            CheckpointManager.checkpoint(params, getAssets()
                            .open("checkpoints-testnet.txt"),
                    chainStore, earliestKeyCreationTime);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BlockStoreException e) {
            e.printStackTrace();
        }

        Log.i("File is present", String.valueOf(isFilePresent(MainActivity.this.getResources().getString(R.string.app_name))));
        SharedPreferences sharedPreferences = getSharedPreferences(SETUP_SETTINGS, MODE_PRIVATE);
        mGetPhoto = findViewById(R.id.choosePhoto);
        progressBar = findViewById(R.id.indeterminateBar);
        mTakePhoto = findViewById(R.id.takePhoto);
        recyclerView = findViewById(R.id.recyclerView);
        fabMenu = findViewById(R.id.fabMenu);
        walletAdapter = new WalletAdapter(ssWalletList);
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



        if(sharedPreferences.getBoolean(FIRST_TIME_SETUP, true)) {
            Log.i("first time setup", "Blockchain SPV initializing....");

            Toast.makeText(this, "Blockchain SPV initializing!", Toast.LENGTH_SHORT).show();
            sharedPreferences.edit().putBoolean(FIRST_TIME_SETUP, false);
        } else {
            Toast.makeText(this, "Blockchain SPV already written!", Toast.LENGTH_SHORT).show();
        }


    }
    public boolean isFilePresent(String fileName) {
        String path = getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        return file.exists();
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
                        "com.cryptoapp.subspace.fileprovider",
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

               addWallet(img, 0);

                new ConvertBitmapToNewWallet().execute(img);
            }

            if (reqCode == REQUEST_CHOOSE_PHOTO) {
                if(resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        balance = 2;
                        image = selectedImage;

                        ssWalletList.add(
                                new SSWallet(selectedImage, balance)
                        );
                        walletAdapter.notifyDataSetChanged();
                        currentListPosition = ssWalletList.size() - 1;

                        String imgString = Base64.encodeToString(getBytesFromBitmap(selectedImage),
                                Base64.NO_WRAP);


                        String hashed = Hashing.sha512()
                                .hashBytes(getBytesFromBitmap(selectedImage))
                                .toString();

                        String hashed2 = Hashing.sha512()
                                .hashString(imgString, StandardCharsets.UTF_8)
                                .toString();

                        //check hash in console
                        Log.i("imgstringhash", hashed);
                        try {
                            byte[] seedBytes = hashed2.substring(0, 16).getBytes();
                            seed = new DeterministicSeed(seedBytes, "", earliestKeyCreationTime);
                            kit = new WalletAppKit(params, getFilesDir(), "subspace1");
                            kit.restoreWalletFromSeed(seed).setCheckpoints(getAssets()
                                    .open("checkpoints-testnet.txt"))
                                    .setDownloadListener(bListener)
                                    .setBlockingStartup(false)
                                    .startAsync();


                        } catch (IOException e) {
                            e.printStackTrace();


                        }


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
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
          String timeStamp = earliestKeyCreationTime.toString();
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


    private void addWallet(Bitmap bitmap, double balance) {

            System.out.println("size" + currentListPosition);
            ssWalletList.remove(currentListPosition);
        ssWalletList.add(
                new SSWallet(bitmap, balance)
        );

        walletAdapter.notifyDataSetChanged();
    }




    DownloadProgressTracker bListener = new DownloadProgressTracker() {
        @Override
        public void doneDownload() {
              Log.i("wallet info", kit.wallet().getBalance().toFriendlyString());
              setBalance();
        }



        @Override
        protected void progress(double pct, int blocksSoFar, Date date) {
            super.progress(pct, blocksSoFar, date);
            System.out.println("progress" + blocksSoFar);
            System.out.println("Percent done" + pct);

        }

        @Override
        public void onChainDownloadStarted(Peer peer, int blocksLeft) {
            super.onChainDownloadStarted(peer, blocksLeft);
            System.out.println("onChainDownloadStarted " + blocksLeft);
        }

        @Override
        public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft);
            System.out.println("onBlocksDownloaded " + blocksLeft);
            sw = Stopwatch.createStarted();

            if(sw.elapsed(TimeUnit.SECONDS) >= 10) {
                try {
                kit.restoreWalletFromSeed(seed).setCheckpoints(getAssets()
                            .open("checkpoints-testnet.txt"))
                            .setDownloadListener(bListener)
                            .setBlockingStartup(false)
                            .startAsync()
                            .awaitRunning();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            if(blocksLeft == 0) {
                setBalance();
            }
        }

        @Override
        protected void startDownload(int blocks) {
            super.startDownload(blocks);
            System.out.println("startDownload");
        }
    };


    public void setBalance() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView setbalance = recyclerView.findViewHolderForAdapterPosition(currentListPosition).itemView.findViewById(R.id.accountBalanceView);
                // Stuff that updates the UI
                setbalance.setText(kit.wallet().getBalance().toFriendlyString());
                balance = Double.parseDouble(kit.wallet().getBalance().toString());
                ssWalletList.remove(currentListPosition);
                ssWalletList.add(new SSWallet(image, balance));
            }
        });

        Log.i("wallet info", kit.wallet().getBalance().toFriendlyString());
    }






    // convert from bitmap to byte array
    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }




    // The types specified here are the input data type, the progress type, and the result type
    private class ConvertBitmapToRestoreWallet extends AsyncTask<Bitmap, String, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Bitmap bmp = bitmaps[0];
            String imgString = Base64.encodeToString(getBytesFromBitmap(bmp),
                    Base64.NO_WRAP);


            String hashed = Hashing.sha512()
                    .hashBytes(getBytesFromBitmap(bmp))
                    .toString();

            String hashed2 = Hashing.sha512()
                    .hashString(imgString, StandardCharsets.UTF_8)
                    .toString();



            //check hash in console
            Log.i("imgstringhash", hashed);
            try {
                byte[] seedBytes = hashed2.substring(0, 16).getBytes();
                DeterministicSeed seed = new DeterministicSeed(seedBytes, "", earliestKeyCreationTime);
                wallet = Wallet.fromSeed(params, seed);
                Log.i("wallet", wallet.toString());
                Log.i("wallet", wallet.currentReceiveAddress().toString());
                kit = new WalletAppKit(params, getFilesDir(), "subspace1");
                kit.restoreWalletFromSeed(seed).setCheckpoints(getAssets()
                        .open("checkpoints-testnet.txt"))
                        .setDownloadListener(bListener)
                        .setBlockingStartup(false)
                        .startAsync();


            } catch (IOException e) {
                e.printStackTrace();


            } finally {

            }


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

    private class ConvertBitmapToNewWallet extends AsyncTask<Bitmap, String, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Bitmap bmp = bitmaps[0];
            String imgString = Base64.encodeToString(getBytesFromBitmap(bmp),
                    Base64.NO_WRAP);


            String hashed = Hashing.sha512()
                    .hashBytes(getBytesFromBitmap(bmp))
                    .toString();

            String hashed2 = Hashing.sha512()
                    .hashString(imgString, StandardCharsets.UTF_8)
                    .toString();



            //check hash in console
            Log.i("imgstringhash", hashed);
            try {
                byte[] seedBytes = hashed2.substring(0, 16).getBytes();
                DeterministicSeed seed = new DeterministicSeed(seedBytes, "", earliestKeyCreationTime);
                WalletAppKit kit = new WalletAppKit(params, getFilesDir(), "subspace1");
                Log.i("Wallet", wallet.toString());



            }finally {

            }


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
