package com.serhat.imageresizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.serhat.imageresizer.databinding.ActivityMainBinding;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = MainActivity.this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.setMainActivity(this);
    }

    public void selectImage() {
        if (checkPerms()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 100);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    public void resizeImage(View view, Bitmap bitmap, String width, String height, int percentage, boolean resizeType) {
        if (bitmap == null) {
            Toast.makeText(context, getString(R.string.msg_you_must_select_an_image), Toast.LENGTH_SHORT).show();
        } else {
            Bitmap resized;

            if (resizeType) { //percentage
                resized = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() - (bitmap.getWidth() * percentage / 100), bitmap.getHeight() - (bitmap.getHeight() * percentage / 100), true);
            } else { //custom
                int newWidth, newHeight;

                try {
                    newWidth = Integer.parseInt(width);
                    newHeight = Integer.parseInt(height);
                } catch(Exception e) {
                    Toast.makeText(context, getString(R.string.msg_dimensions_invalid), Toast.LENGTH_SHORT).show();
                    return;
                }

                resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            binding.imgSelectedImage.setImageBitmap(resized);
            Snackbar.make(view, getString(R.string.msg_image_resized), Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.btn_save), v -> {
                if (storeImage(resized)) {
                    Snackbar.make(v, getString(R.string.msg_saved), Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(v, getString(R.string.msg_image_couldnt_saved), Snackbar.LENGTH_SHORT).show();
                }
            }).show();
        }
    }

    private boolean checkPerms() {
        int permWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);

        return permWrite == PackageManager.PERMISSION_GRANTED && permRead == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData();

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver() , imageUri);
                    binding.imgSelectedImage.setImageBitmap(bitmap);
                    binding.setBitmap(bitmap);

                    binding.txtWidth.setText(String.valueOf(bitmap.getWidth()));
                    binding.txtHeight.setText(String.valueOf(bitmap.getHeight()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean createDir(File sdIconStorageDir) {
        if (!sdIconStorageDir.exists()) {
            return sdIconStorageDir.mkdirs();
        } else {
            return true;
        }
    }

    public boolean storeImage(Bitmap imageData) {
        File sdIconStorageDir = new File(Environment.getExternalStorageDirectory() + getString(R.string.image_directory));
        if (!createDir(sdIconStorageDir)) return false;

        try {
            String filePath = sdIconStorageDir + File.separator + System.currentTimeMillis() + getString(R.string.image_mime_type);
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);

            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
            imageData.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();

            MediaScannerConnection.scanFile(this, new String[]{filePath}, null, (path, uri) -> { });
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}