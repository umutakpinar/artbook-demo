package com.umutakpinar.artbook_demo;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActivityChooserView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.viewbinding.ViewBinding;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.umutakpinar.artbook_demo.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        database = ArtActivity.this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

       try{
           if(info.equals("old")){
               binding.artName.setEnabled(false);
               binding.artistName.setEnabled(false);
               binding.date.setEnabled(false);
               binding.btnSave.setText("Delete art");

               binding.imageView.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       /* do nothing tıklanınca görsel seçmeyelim.*/
                   }
               });
               int artId = intent.getIntExtra("artId",0);
               Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[]{String.valueOf(artId)});
               int artNameIndex = cursor.getColumnIndex("artname");
               int artistNameIndex = cursor.getColumnIndex("paintername");
               int dateIndex = cursor.getColumnIndex("year");
               int imageIndex = cursor.getColumnIndex("image");

               while(cursor.moveToNext()){
                   binding.artName.setText(cursor.getString(artNameIndex).toString());
                   binding.artistName.setText(cursor.getString(artistNameIndex).toString());
                   binding.date.setText(cursor.getString(dateIndex).toString());
                   byte[] imageBytes = cursor.getBlob(imageIndex);
                   Bitmap reCreateBitmap = BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
                   binding.imageView.setImageBitmap(reCreateBitmap);
               }

               binding.btnSave.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       AlertDialog.Builder dialog = new AlertDialog.Builder(ArtActivity.this);
                       dialog.setMessage("Are you sure? It can not be undone.");
                       dialog.setPositiveButton("Yes, delete it!", new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {
                               Toast.makeText(ArtActivity.this,"Permanently deleted!",Toast.LENGTH_LONG).show();
                               String sqlString = "DELETE FROM arts WHERE id = ?";
                               SQLiteStatement deleteStatement = database.compileStatement(sqlString);
                               deleteStatement.bindString(1,String.valueOf(artId));
                               deleteStatement.execute();
                               Intent intent = new Intent(ArtActivity.this,MainActivity.class);
                               intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                               startActivity(intent);
                           }
                       });
                       dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {
                                /*cancelled do nothing*/
                           }
                       });
                   dialog.show();
                   }
               });

               cursor.close();
           }
           else if(info.equals("new")){

               registerLauncher();
           }
       }catch(Exception e){
           e.printStackTrace();
       }
    }


    public void saveData(View view){
        String artName = binding.artName.getText().toString();
        String artistName = binding.artistName.getText().toString();
        String date = binding.date.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try{
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");
            String query  = "INSERT INTO arts (artname,paintername,year,image) VALUES (?,?,?,?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(query);
            sqLiteStatement.bindString(1,artName);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,date);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){
        int width = (int)image.getWidth();
        int height = (int)image.getHeight();

        float bitMapRatio = (float)(width / (float)height);

        if(bitMapRatio > 1){ //demek ki width büyük
            width = maximumSize;
            height = (int)(width / bitMapRatio);
        }else{
            height = maximumSize;
            width = (int)(bitMapRatio * height);
        }

        return image.createScaledBitmap(image,width,height,true);
    }

    public void selectImage(View view){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Galeriden resim seçmek için bu izni almak zorundayız.",Snackbar.LENGTH_INDEFINITE).setAction("İzin ver", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }else{
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }else{
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }
    }

    public void registerLauncher(){

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //permission granted
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }else{
                    //permission denied
                    Toast.makeText(ArtActivity.this, "İzin reddedildi!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == RESULT_OK){
                    Intent intentFromResult = result.getData();
                    if(intentFromResult != null){
                       Uri imageData =  intentFromResult.getData();
                       //binding.imageView.setImageURI(imageData);

                        try {
                            if(Build.VERSION.SDK_INT >= 28){
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                            }
                            else{
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                            }
                            binding.imageView.setImageBitmap(selectedImage);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }


}

