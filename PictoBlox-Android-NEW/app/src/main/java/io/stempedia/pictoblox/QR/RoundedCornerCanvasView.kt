package io.stempedia.pictoblox.QR

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Nullable


class RoundedCornerCanvasView : View {
    private var mPaint: Paint? = null
    private var mCornerRadius = 0f

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        mPaint = Paint()
        mPaint!!.color = Color.BLACK
        mPaint!!.alpha = 20
        mPaint!!.isAntiAlias = true
        mCornerRadius = 30f // Customize the corner radius here
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = canvas.width
        val height = canvas.height
        // Draw the rounded corner overlay
        val path = Path()
        path.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), mCornerRadius, mCornerRadius, Path.Direction.CW)
        canvas.clipPath(path, Region.Op.DIFFERENCE)
        canvas.drawColor(Color.parseColor("#50000000")) // Customize the overlay color here
    }
}