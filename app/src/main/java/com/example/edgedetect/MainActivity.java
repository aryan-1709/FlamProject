package com.example.edgedetect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_PERMISSIONS = 100;

    private ImageView imageView;
    private Button btnSelect, btnCapture, btnDetect;

    private Bitmap selectedBitmap;
    private Uri photoURI;

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Initialization successful");
        } else {
            Log.d("OpenCV", "Initialization failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        imageView = findViewById(R.id.imageView);
        btnSelect = findViewById(R.id.btnSelectImage);
        btnCapture = findViewById(R.id.btnCaptureImage);
        btnDetect = findViewById(R.id.btnDetectEdges);

        // Request CAMERA permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSIONS);
        }

        btnSelect.setOnClickListener(v -> openGallery());
        btnCapture.setOnClickListener(v -> openCamera());
        btnDetect.setOnClickListener(v -> detectEdges());
    }

    private void openGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri imageUri = data.getData();
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(selectedBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                    imageView.setImageBitmap(selectedBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void detectEdges() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        Mat src = new Mat();
        Utils.bitmapToMat(selectedBitmap, src);
        Mat gray = new Mat();
        Mat edges = new Mat();

        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 1.5);
        Imgproc.Canny(gray, edges, 100, 200);

        Bitmap resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, resultBitmap);

        imageView.setImageBitmap(resultBitmap);
    }
}