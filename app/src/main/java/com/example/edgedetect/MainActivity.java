package com.example.edgedetect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.edgedetect.databinding.ActivityMainBinding;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private Bitmap originalBitmap;
    private final int REQ_READ_IMAGES = 1001;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    static {
        if (!OpenCVLoader.initDebug()) {
            System.out.println("OpenCV init failed");
        } else {
            System.out.println("OpenCV loaded successfully");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            originalBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
                            binding.imageOriginal.setImageBitmap(originalBitmap);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                });

        binding.btnPick.setOnClickListener(v -> {
            if (!hasImagePermission()) {
                requestImagePermission();
                return;
            }
            pickImageFromGallery();
        });

        binding.btnCanny.setOnClickListener(v -> {
            if (originalBitmap != null) {
                Bitmap edges = runCanny(originalBitmap, 50, 150);
                binding.imageResult.setImageBitmap(edges);
            }
        });
    }

    private boolean hasImagePermission() {
        if (Build.VERSION.SDK_INT >= 33)
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestImagePermission() {
        if (Build.VERSION.SDK_INT >= 33)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQ_READ_IMAGES);
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_READ_IMAGES);
    }

    private void pickImageFromGallery() {
        Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pick.setType("image/*");
        pickImageLauncher.launch(pick);
    }

    private Bitmap runCanny(Bitmap input, double low, double high) {
        Mat src = new Mat();
        Utils.bitmapToMat(input, src);
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, low, high);
        Mat edgesRgba = new Mat();
        Imgproc.cvtColor(edges, edgesRgba, Imgproc.COLOR_GRAY2RGBA);
        Bitmap result = Bitmap.createBitmap(edgesRgba.cols(), edgesRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edgesRgba, result);
        src.release(); gray.release(); edges.release(); edgesRgba.release();
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_IMAGES && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            pickImageFromGallery();
    }
}
