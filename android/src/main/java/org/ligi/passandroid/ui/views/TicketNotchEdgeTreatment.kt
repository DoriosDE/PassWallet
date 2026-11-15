package org.ligi.passandroid.ui.views

import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.ShapePath

/**
 * Cuts a wide, shallow half ellipse into the middle of an edge - the punched notch of a ticket.
 *
 * @param width total width of the notch along the edge
 * @param depth how far the notch reaches into the card
 */
class TicketNotchEdgeTreatment(private val width: Float, private val depth: Float) : EdgeTreatment() {

    override fun getEdgePath(length: Float, center: Float, interpolation: Float, shapePath: ShapePath) {
        val halfWidth = width / 2 * interpolation
        val notchDepth = depth * interpolation
        shapePath.lineTo(center - halfWidth, 0f)
        // positive y points into the card - sweeping from the left point (180°) back through the bottom (90°)
        shapePath.addArc(center - halfWidth, -notchDepth, center + halfWidth, notchDepth, 180f, -180f)
        shapePath.lineTo(length, 0f)
    }
}
