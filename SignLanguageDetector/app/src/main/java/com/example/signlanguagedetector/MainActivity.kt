package com.example.signlanguagedetector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.signlanguagedetector.databinding.ActivityMainBinding
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bitmap: Bitmap
    private lateinit var mpImg: MPImage
    private var objectDetector: ObjectDetector? = null

    var isFront = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.btnHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        viewBinding.btn.setOnClickListener {
            isFront = !(isFront)
            startCamera()
        }

        setObjectDetector()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    @OptIn(ExperimentalGetImage::class) private fun  startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            val imageAnalysis = ImageAnalysis.Builder()
//                 enable the following line if RGBA output is needed.
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                 .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//                bitmap = rotateBitmapToPortrait(imageProxy.toBitmap())
                if(isFront == true){
                    bitmap = flip(rotateBitmapToPortraitnegative(imageProxy.toBitmap()),0)!!
                }
                else{
                    bitmap = rotateBitmapToPortrait(imageProxy.toBitmap())
                }
                mpImg = BitmapImageBuilder(bitmap).build()


                runOnUiThread {
                    viewBinding.imager.setImageBitmap(bitmap)
                    predict()
                }
                imageProxy.close()
            })

            // Select back camera as a default
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            var cameraSelector : CameraSelector
            if (isFront == true){
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            }
            else{
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis)


            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun predict() {


        var detectionResult = objectDetector?.detect(mpImg)

        if (detectionResult != null){
            detectionResult.detections()?.map{
                val boxRect = RectF(
                    it.boundingBox().left,
                    it.boundingBox().top,
                    it.boundingBox().right,
                    it.boundingBox().bottom
                )
            }?.forEachIndexed { index, floats ->


                // Create text to display alongside detected objects
                val category = detectionResult.detections()!![index].categories()[0]
                viewBinding.txt.text = category.categoryName()
            }
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    fun rotateBitmapToPortrait(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()

        // Check the orientation of the bitmap
        when {
            bitmap.width > bitmap.height -> {
                // Landscape orientation, rotate counter-clockwise by 90 degrees
                matrix.postRotate(90f)
            }
            bitmap.height > bitmap.width -> {
                // Portrait orientation, no rotation needed
                return bitmap
            }
            else -> {
                // Square orientation, no rotation needed
                return bitmap
            }
        }

        // Create a new rotated bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun rotateBitmapToPortraitnegative(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()

        // Check the orientation of the bitmap
        when {
            bitmap.width > bitmap.height -> {
                // Landscape orientation, rotate counter-clockwise by 90 degrees
                matrix.postRotate(-90f)
            }
            bitmap.height > bitmap.width -> {
                // Portrait orientation, no rotation needed
                return bitmap
            }
            else -> {
                // Square orientation, no rotation needed
                return bitmap
            }
        }

        // Create a new rotated bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flip(src: Bitmap, type: Int): Bitmap? {
        // create new matrix for transformation
        val matrix = Matrix()
        matrix.preScale(-1.0f, 1.0f)

        // return transformed image
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    fun setObjectDetector(){
        val baseOptionsBuilder = BaseOptions.builder()
        baseOptionsBuilder.setDelegate(Delegate.CPU).setModelAssetPath("model.tflite").build()

        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setScoreThreshold(0.26f).setRunningMode(RunningMode.IMAGE)
            .setMaxResults(1)
        val options = optionsBuilder.build()

        objectDetector = ObjectDetector.createFromOptions(this,options)
    }

}