package org.ligi.passandroid.ui.edit.dialogs

import android.app.Activity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.ligi.kaxt.inflate
import org.ligi.passandroid.R
import org.ligi.passandroid.location.LocationSearch
import org.ligi.passandroid.location.LocationSearchResult
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassLocation
import java.io.IOException
import java.util.Locale

fun showLocationEditDialog(
    activity: Activity,
    pass: PassImpl,
    refreshCallback: () -> Unit,
) {
    val view = activity.inflate(R.layout.location_edit)
    val existingLocation = pass.locations.firstOrNull()

    val searchInput = view.findViewById<TextInputEditText>(R.id.locationSearchInput)
    val searchButton = view.findViewById<MaterialButton>(R.id.locationSearchButton)
    val searchProgress = view.findViewById<ProgressBar>(R.id.locationSearchProgress)
    val searchStatus = view.findViewById<TextView>(R.id.locationSearchStatus)
    val resultsContainer = view.findViewById<LinearLayout>(R.id.locationResultsContainer)

    val nameInput = view.findViewById<TextInputEditText>(R.id.locationNameInput)
    val latitudeInput = view.findViewById<TextInputEditText>(R.id.locationLatitudeInput)
    val longitudeInput = view.findViewById<TextInputEditText>(R.id.locationLongitudeInput)
    val latitudeLayout = view.findViewById<TextInputLayout>(R.id.locationLatitudeLayout)
    val longitudeLayout = view.findViewById<TextInputLayout>(R.id.locationLongitudeLayout)
    val saveButton = view.findViewById<MaterialButton>(R.id.locationSaveButton)
    val removeButton = view.findViewById<MaterialButton>(R.id.locationRemoveButton)
    val dialog = BottomSheetDialog(activity)
    val locationSearch = LocationSearch()
    val searchScope = MainScope()

    fun setSearchLoading(isLoading: Boolean) {
        searchButton.isEnabled = !isLoading
        searchProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    fun selectSearchResult(result: LocationSearchResult) {
        nameInput.setText(result.displayName)
        latitudeInput.setText(formatCoordinate(result.latitude))
        longitudeInput.setText(formatCoordinate(result.longitude))
        searchStatus.text = activity.getString(R.string.location_result_selected)
    }

    fun showSearchResults(results: List<LocationSearchResult>) {
        resultsContainer.removeAllViews()
        if (results.isEmpty()) {
            searchStatus.text = activity.getString(R.string.location_no_results)
            return
        }

        searchStatus.text = activity.getString(R.string.location_select_result)
        val rowMargin = activity.resources.getDimensionPixelSize(R.dimen.rhythm)
        results.forEach { result ->
            val row = activity.layoutInflater.inflate(
                R.layout.item_location_pick,
                resultsContainer,
                false,
            ) as MaterialCardView
            row.findViewById<TextView>(R.id.locationPickName).text = result.displayName
            row.findViewById<TextView>(R.id.locationPickCoordinates).text =
                activity.getString(R.string.location_coordinates_format, result.latitude, result.longitude)
            row.setOnClickListener { selectSearchResult(result) }
            resultsContainer.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = rowMargin }
            )
        }
    }

    fun performLocationSearch() {
        val query = searchInput.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            searchStatus.text = activity.getString(R.string.location_search_empty)
            return
        }

        setSearchLoading(true)
        searchStatus.text = ""
        resultsContainer.removeAllViews()

        searchScope.launch {
            val results = try {
                locationSearch.search(query, Locale.getDefault().toLanguageTag())
            } catch (_: IOException) {
                null
            }
            setSearchLoading(false)
            if (results == null) {
                searchStatus.text = activity.getString(R.string.location_search_failed)
            } else {
                showSearchResults(results)
            }
        }
    }

    fun saveLocation(): Boolean {
        latitudeLayout.error = null
        longitudeLayout.error = null

        val latitude = latitudeInput.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        val longitude = longitudeInput.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()

        var hasError = false
        if (latitude == null || latitude !in -90.0..90.0) {
            latitudeLayout.error = activity.getString(R.string.invalid_location_coordinates)
            hasError = true
        }
        if (longitude == null || longitude !in -180.0..180.0) {
            longitudeLayout.error = activity.getString(R.string.invalid_location_coordinates)
            hasError = true
        }
        if (hasError) {
            return false
        }

        pass.locations = listOf(PassLocation().apply {
            name = nameInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            lat = latitude!!
            lon = longitude!!
        })
        refreshCallback.invoke()
        return true
    }

    nameInput.setText(existingLocation?.name.orEmpty())
    searchInput.setText(existingLocation?.name.orEmpty())
    existingLocation?.let {
        latitudeInput.setText(formatCoordinate(it.lat))
        longitudeInput.setText(formatCoordinate(it.lon))
    }

    searchButton.setOnClickListener { performLocationSearch() }
    searchInput.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performLocationSearch()
            true
        } else {
            false
        }
    }
    saveButton.setOnClickListener {
        if (saveLocation()) {
            dialog.dismiss()
        }
    }
    removeButton.visibility = if (existingLocation != null) View.VISIBLE else View.GONE
    removeButton.setOnClickListener {
        pass.locations = emptyList()
        refreshCallback.invoke()
        dialog.dismiss()
    }

    dialog.setOnDismissListener { searchScope.cancel() }
    dialog.setContentView(view)
    dialog.show()
}
