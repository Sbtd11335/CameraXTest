package com.cistus.cameraxtest

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cistus.cameraxtest.ui.theme.CameraXTestTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

typealias LumaListener = (luma: Double) -> Unit



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraCaptureScreen()
                }
            }
        }
    }
}

@Composable
fun toDp(px: Float): Dp {
    with(LocalDensity.current) {
        return px.toDp()
    }
}

@Composable
fun CameraCaptureScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("CameraCaptureScreen", "Permission granted")
        } else {
            Log.d("CameraCaptureScreen", "Permission denied")
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(factory = { context ->
                val previewView = androidx.camera.view.PreviewView(context)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder().build()

                    try {
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraCaptureScreen", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            }, modifier = Modifier.fillMaxSize())
        }
        Button(
            onClick = {
                val photoFile = File(
                    context.externalMediaDirs.first(),
                    SimpleDateFormat(
                        "yyyy-MM-dd-HH-mm-ss-SSS",
                        Locale.US
                    ).format(System.currentTimeMillis()) + ".jpg"
                )

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraCaptureScreen", "Photo capture failed: ${exc.message}", exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            photoUri = Uri.fromFile(photoFile)
                            Log.d("CameraCaptureScreen", "Photo capture succeeded: $photoUri")
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take Photo")
        }
        photoUri?.let {
            Text("Photo saved to: $it")
        }
    }
}