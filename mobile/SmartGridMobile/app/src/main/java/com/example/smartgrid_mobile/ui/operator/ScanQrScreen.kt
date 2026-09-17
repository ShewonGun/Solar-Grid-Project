/* ============================================================================
 * File        : ScanQrScreen.kt
 * Purpose     : Camera screen where a grid operator scans the prosumer's
 *               transaction QR code. Decoding happens on device; the token is
 *               then sent to the Web API, which decides whether it is valid.
 * Author      : SmartGrid Mobile Team
 * Created     : 2026-09-17
 * ==========================================================================*/
package com.example.smartgrid_mobile.ui.operator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartgrid_mobile.ui.common.BannerTone
import com.example.smartgrid_mobile.ui.common.EmptyStateBlock
import com.example.smartgrid_mobile.ui.common.MessageBanner
import com.example.smartgrid_mobile.ui.common.PageHeaderCard
import com.example.smartgrid_mobile.ui.common.PrimaryButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    viewModel: OperatorViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCamera by remember { mutableStateOf(isCameraGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    LaunchedEffect(Unit) {
        if (!hasCamera) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Leaves for the result screen as soon as the service confirms the token.
    LaunchedEffect(state.stage) {
        if (state.stage == OperatorStage.VERIFIED) onVerified()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scan QR", fontWeight = FontWeight.SemiBold) },
                // Sits on the page background, matching the rest of the app.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MessageBanner(state.errorMessage, BannerTone.ERROR)

            PageHeaderCard(
                icon = Icons.Default.QrCodeScanner,
                title = if (state.stage == OperatorStage.VERIFYING) {
                    "Checking code"
                } else {
                    "Point at the code"
                },
                subtitle = "Prosumer transaction QR",
                // Makes clear the phone is not the authority on the token.
                footnote = "The code is checked against the service before any " +
                    "transfer is finalised."
            )

            if (hasCamera) {
                CameraPanel(
                    scanning = state.stage == OperatorStage.IDLE,
                    onQrCode = viewModel::onQrScanned
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    EmptyStateBlock(
                        icon = Icons.Default.NoPhotography,
                        title = "Camera permission needed",
                        body = "Operator mode uses the camera to read the prosumer's " +
                            "transaction QR code. Nothing is recorded or uploaded."
                    )
                    PrimaryButton(
                        text = "Allow camera",
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
            }
        }
    }
}

/** Live camera preview with the QR analyser bound to it. */
@Composable
private fun CameraPanel(scanning: Boolean, onQrCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = rememberAnalysisExecutor()

    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    val previewView = PreviewView(viewContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                    providerFuture.addListener({
                        bindCamera(
                            provider = providerFuture.get(),
                            previewView = previewView,
                            lifecycleOwner = lifecycleOwner,
                            executor = executor,
                            onQrCode = onQrCode
                        )
                    }, ContextCompat.getMainExecutor(viewContext))

                    previewView
                }
            )

            // Covers the preview while the token is being checked, so the operator
            // can see that something is happening.
            if (!scanning) {
                Surface(color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Binds the preview and the analyser to the back camera for this screen's lifetime. */
private fun bindCamera(
    provider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    executor: ExecutorService,
    onQrCode: (String) -> Unit
) {
    val preview = Preview.Builder().build().apply {
        setSurfaceProvider(previewView.surfaceProvider)
    }

    val analysis = ImageAnalysis.Builder()
        // Only the newest frame matters; older ones are dropped rather than queued.
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply { setAnalyzer(executor, QrCodeAnalyzer(onQrCode)) }

    provider.unbindAll()
    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
}

/** Single background thread for frame analysis, shut down with the screen. */
@Composable
private fun rememberAnalysisExecutor(): ExecutorService {
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }
    return executor
}

/** True when the camera permission has already been granted. */
private fun isCameraGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
