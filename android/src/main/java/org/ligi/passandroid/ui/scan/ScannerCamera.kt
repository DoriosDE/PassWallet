package org.ligi.passandroid.ui.scan

import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/**
 * Shows the camera picture in [previewView] and looks for barcodes in it with [decoder].
 * The first barcode found goes to [onBarcode] on the main thread; after that the camera does not start again.
 *
 * @param onTorchChanged told whether the torch is on, whenever that may have changed
 */
class ScannerCamera(
    private val activity: AppCompatActivity,
    private val previewView: PreviewView,
    private val decoder: BarcodeDecoder,
    private val onTorchChanged: (enabled: Boolean) -> Unit,
    private val onBarcode: (ScannedBarcode) -> Unit,
) {

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var torchEnabled = false

    @Volatile
    private var found = false

    fun start() {
        if (found) return
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            bind(provider)
        }, ContextCompat.getMainExecutor(activity))
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }

    /** stops the camera for good - call when the activity is destroyed */
    fun release() {
        stop()
        analysisExecutor.shutdownNow()
    }

    fun toggleTorch() {
        val currentCamera = camera ?: return
        if (!currentCamera.cameraInfo.hasFlashUnit()) return
        torchEnabled = !torchEnabled
        currentCamera.cameraControl.enableTorch(torchEnabled)
        onTorchChanged(torchEnabled)
    }

    /** switches between back and front camera, if the device has both */
    fun switchLens() {
        val provider = cameraProvider ?: return
        val nextLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        val selector = CameraSelector.Builder().requireLensFacing(nextLens).build()
        if (!provider.hasCamera(selector)) return
        lensFacing = nextLens
        torchEnabled = false
        onTorchChanged(torchEnabled)
        bind(provider)
    }

    private fun bind(provider: ProcessCameraProvider) {
        provider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(analysisExecutor) { image ->
            try {
                if (found) return@setAnalyzer
                val barcode = decoder.decode(image) ?: return@setAnalyzer
                found = true
                activity.runOnUiThread { onBarcode(barcode) }
            } catch (_: RuntimeException) {
                // Keep scanning. Camera analyzers should be boring, not crashy.
            } finally {
                image.close()
            }
        }

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            camera = provider.bindToLifecycle(activity, selector, preview, analysis)
            camera?.cameraControl?.enableTorch(torchEnabled)
            onTorchChanged(torchEnabled)
        } catch (_: IllegalArgumentException) {
            lensFacing = CameraSelector.LENS_FACING_BACK
            torchEnabled = false
            bind(provider)
        }
    }
}
