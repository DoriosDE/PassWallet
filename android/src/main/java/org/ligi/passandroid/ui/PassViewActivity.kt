package org.ligi.passandroid.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.ligi.kaxt.disableRotation
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassGroupFilter
import org.ligi.passandroid.model.PassStoreProjection
import org.ligi.passandroid.model.comparator.PassSortOrder
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.model.pass.PassVisualType

class PassViewActivity : PassViewActivityBase() {
    private lateinit var pagerAdapter: CollectionPagerAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasCurrentPass()) {
            return
        }

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            this.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        disableRotation()
        setContentView(R.layout.activity_pass_view_base)
        applyMaterialInsets(
            root = findViewById(R.id.pass_view_root),
            appBar = findViewById(R.id.appbar),
            content = findViewById(R.id.pager),
        )

        val topic = passStore.classifier.getTopic(currentPass, "")
        // grouped passes: swiping only switches between the passes of the same group
        val groupFilter = if (settings.isGroupingEnabled()) {
            PassGroupFilter.forPass(currentPass, passStore.classifier.getPassListByTopic(topic))
        } else {
            PassGroupFilter.All
        }
        pagerAdapter = CollectionPagerAdapter(this, PassStoreProjection(passStore, topic, settings.getSortOrder(), groupFilter))
        viewPager = findViewById(R.id.pager)
        viewPager.adapter = pagerAdapter
        val initialPosition = pagerAdapter.getPos(currentPass)
        if (initialPosition < 0) {
            tracker.trackException("pass ${currentPass.id} not present in pager projection", false)
            finish()
            return
        }
        viewPager.currentItem = initialPosition
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) {
                currentPass = pagerAdapter.getPass(pos)
                passStore.currentPass = currentPass
                updateGroupPositionSubtitle()
            }
        })
    }

    override fun refresh() {
        super.refresh()

        pagerAdapter.refresh(settings.getSortOrder())
        updateGroupPositionSubtitle()
    }

    /**
     * Shows "pass x of y" in the toolbar while looking at a group of passes.
     */
    private fun updateGroupPositionSubtitle() {
        val actionBar = supportActionBar ?: return
        val position = pagerAdapter.getPos(currentPass)
        actionBar.subtitle = if (pagerAdapter.isGroup && position >= 0) {
            getString(R.string.pass_group_position, position + 1, pagerAdapter.itemCount)
        } else {
            null
        }
    }

    inner class CollectionPagerAdapter(
            fa: FragmentActivity,
            private var passStoreProjection: PassStoreProjection
    ) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = passStoreProjection.passList.size

        val isGroup: Boolean
            get() = passStoreProjection.groupFilter is PassGroupFilter.OnlyGroup && itemCount > 1

        override fun createFragment(i: Int): Fragment {

            val pass = getPass(i)
            val visualType = PassVisualClassifier.classify(pass)
            if (visualType == PassVisualType.CREDENTIAL) {
                pass.visualType = PassVisualType.CREDENTIAL
            }

            val fragment =
                if (pass.visualType == PassVisualType.CREDENTIAL) {
                    PassViewPKFragment()
                } else {
                    when (pass.type) {
                        PassType.EVENT,
                        PassType.BOARDING,
                        PassType.PKBOARDING,
                        PassType.LOYALTY -> PassViewPKFragment()
                        else -> PassViewFragment()
                    }
                }

            fragment.arguments = bundleOf(EXTRA_KEY_UUID to pass.id)
            return fragment
        }

        fun getPass(i: Int): Pass {
            return passStoreProjection.passList[i]
        }

        fun getPos(pass: Pass): Int {
            return passStoreProjection.passList.indexOf(pass)
        }

        fun refresh(sortOrder: PassSortOrder) {
            passStoreProjection.refresh(sortOrder)
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        setSupportActionBar(findViewById(R.id.toolbar))

        configureActionBar()
        refresh()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_update).isVisible = mightPassBeAbleToUpdate(currentPass)
        menu.findItem(R.id.install_shortcut).isVisible = ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.update, menu)
        menuInflater.inflate(R.menu.shortcut, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            // opened from a list (active passes or archive): just go back there; otherwise (e.g. shortcut) open the list
            if (isTaskRoot) {
                startActivity(Intent(this, PassListActivity::class.java))
            }
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
