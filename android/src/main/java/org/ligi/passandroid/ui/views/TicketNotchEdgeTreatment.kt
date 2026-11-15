package org.ligi.passandroid.ui.views

import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.ShapePath

/**
 * Cuts a half circle into the middle of an edge - the punched notch of a ticket.
 */
class TicketNotchEdgeTreatment(private val radius: Float) : EdgeTreatment() {

    override fun getEdgePath(length: Float, center: Float, interpolation: Float, shapePath: ShapePath) {
        val notchRadius = radius * interpolation
        shapePath.lineTo(center - notchRadius, 0f)
        // the arc bulges into the card (positive y is "inside" for an edge path)
        shapePath.addArc(center - notchRadius, -notchRadius, center + notchRadius, notchRadius, 180f, -180f)
        shapePath.lineTo(length, 0f)
    }
}
