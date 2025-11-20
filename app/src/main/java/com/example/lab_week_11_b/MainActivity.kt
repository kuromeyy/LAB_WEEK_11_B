package com.example.lab_week_11_b

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 3
    }

    // Provider File Manager
    private lateinit var providerFileManager: ProviderFileManager

    // FileInfo untuk foto dan video
    private var photoInfo: FileInfo? = null
    private var videoInfo: FileInfo? = null

    // Flag untuk cek user sedang record video atau foto
    private var isCapturingVideo = false

    // Activity Result Launcher untuk foto/video
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init ProviderFileManager
        providerFileManager = ProviderFileManager(
            applicationContext,
            FileHelper(applicationContext),
            contentResolver,
            Executors.newSingleThreadExecutor(),
            MediaContentHelper()
        )

        // Launcher untuk ambil foto
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) {
                providerFileManager.insertImageToStore(photoInfo)
            }

        // Launcher untuk record video
        takeVideoLauncher =
            registerForActivityResult(ActivityResultContracts.CaptureVideo()) {
                providerFileManager.insertVideoToStore(videoInfo)
            }

        // Tombol Foto
        findViewById<Button>(R.id.photo_button).setOnClickListener {
            isCapturingVideo = false
            checkStoragePermission {
                openImageCapture()
            }
        }

        // Tombol Video
        findViewById<Button>(R.id.video_button).setOnClickListener {
            isCapturingVideo = true
            checkStoragePermission {
                openVideoCapture()
            }
        }
    }

    // Buka kamera untuk ambil foto
    private fun openImageCapture() {
        photoInfo = providerFileManager.generatePhotoUri(System.currentTimeMillis())
        takePictureLauncher.launch(photoInfo!!.uri)   // FIXED: non-null
    }

    // Buka kamera untuk record video
    private fun openVideoCapture() {
        videoInfo = providerFileManager.generateVideoUri(System.currentTimeMillis())
        takeVideoLauncher.launch(videoInfo!!.uri)     // FIXED: non-null
    }

    // Cek permission untuk Android < Q
    private fun checkStoragePermission(onPermissionGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
                PackageManager.PERMISSION_GRANTED -> onPermissionGranted()
                else -> ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        } else {
            onPermissionGranted()
        }
    }

    // Handle hasil permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isCapturingVideo) {
                    openVideoCapture()
                } else {
                    openImageCapture()
                }
            }
        }
    }
}
