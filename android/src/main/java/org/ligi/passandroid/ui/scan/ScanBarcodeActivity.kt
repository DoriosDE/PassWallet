package org.ligi.passandroid.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ligi.passandroid.R

/**
 * Scans a barcode with the camera or reads it from a picked image.
 * Returns the barcode in [EXTRA_SCAN_RESULT] and its ZXing format name in [EXTRA_SCAN_RESULT_FORMAT].
 */
class ScanBarcodeActivity : AppCompatActivity() {

    private val decoder = BarcodeDecoder()
    private lateinit var layout: ScannerLayout
    private lateinit var camera: ScannerCamera

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            camera.start()
        } else {
            layout.showPermissionDenied()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            decodePickedImage(uri)
        } else if (hasCameraPermission()) {
            camera.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        layout = ScannerLayout(
            this,
            onClose = ::finish,
            onToggleTorch = { camera.toggleTorch() },
            onReadImage = {
                camera.stop()
                imagePickerLauncher.launch("image/*")
            },
            onSwitchCamera = { camera.switchLens() },
        )
        setContentView(layout.root)
        camera = ScannerCamera(this, layout.previewView, decoder, layout::showTorch, ::deliverResult)

        if (hasCameraPermission()) {
            camera.start()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) camera.start()
    }

    override fun onPause() {
        camera.stop()
        super.onPause()
    }

    override fun onDestroy() {
        camera.release()
        super.onDestroy()
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun decodePickedImage(uri: Uri) {
        camera.stop()
        lifecycleScope.launch {
            val barcode = withContext(Dispatchers.Default) {
                runCatching { contentResolver.loadPickedImage(uri)?.let(decoder::decode) }.getOrNull()
            }
            if (barcode != null) {
                deliverResult(barcode)
            } else {
                Toast.makeText(this@ScanBarcodeActivity, R.string.scan_image_failed, Toast.LENGTH_SHORT).show()
                if (hasCameraPermission()) camera.start()
            }
        }
    }

    private fun deliverResult(barcode: ScannedBarcode) {
        if (isFinishing || isDestroyed) return
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(EXTRA_SCAN_RESULT_FORMAT, barcode.format)
                putExtra(EXTRA_SCAN_RESULT, barcode.text)
            },
        )
        finish()
    }

    companion object {
        const val EXTRA_SCAN_RESULT = "SCAN_RESULT"
        const val EXTRA_SCAN_RESULT_FORMAT = "SCAN_RESULT_FORMAT"
    }
}
