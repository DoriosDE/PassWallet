package org.ligi.passandroid.ui.passview

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.inject
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.ui.views.ZoomImageView
import org.ligi.passandroid.ui.EXTRA_PASS_ID
import org.ligi.passandroid.ui.getBitmap

class TouchImageActivity : AppCompatActivity() {

    val passStore: PassStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val touchImageView = ZoomImageView(this)

        setContentView(touchImageView)

        val pass = intent.getStringExtra(EXTRA_PASS_ID)?.let { passStore.getPass(it) }
        val bitmap = intent.getStringExtra(EXTRA_IMAGE)?.let { image -> pass?.getBitmap(passStore, image) }

        if (bitmap == null) {
            finish()
        } else {
            touchImageView.setImageBitmap(bitmap)

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        /** which picture of the pass to show, e.g. [org.ligi.passandroid.model.PassBitmapDefinitions.BITMAP_STRIP] */
        const val EXTRA_IMAGE = "image"
    }
}
