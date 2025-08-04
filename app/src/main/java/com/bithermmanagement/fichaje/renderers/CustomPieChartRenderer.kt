package com.bithermmanagement.fichaje.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import com.bithermmanagement.R
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.cos
import kotlin.math.sin

class CustomPieChartRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler,
    private val handColor: Int,
    private val elapsedHours: Float
) : PieChartRenderer(chart, animator, viewPortHandler) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = handColor
        isAntiAlias = true
    }

    private val ringPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 30f // Ancho del anillo
        color = android.graphics.Color.parseColor("#4CAF50") // Color verde
        isAntiAlias = true
    }

    private val dividerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = android.graphics.Color.parseColor("#CCCCCC") // Color gris
        isAntiAlias = true
    }

    private val favicon: Drawable? = chart.context.getDrawable(R.drawable.ic_launcher_foreground)

    override fun drawExtras(c: Canvas?) {
        if (c == null) return
        
        val center = mChart.centerCircleBox
        val radius = mChart.radius * 1.1f
        
        // Dibujar el anillo exterior
        c.drawCircle(center.x, center.y, mChart.radius * 0.85f, ringPaint)
        
        // Dibujar líneas divisorias
        val dividerRadius = mChart.radius * 0.85f
        val dividerAngle = 120f // Ángulo para la línea divisoria
        
        // Dibujar línea divisoria entre BAJAMED y PERM.RET
        val dividerRad = Math.toRadians(dividerAngle.toDouble())
        val startX = center.x + cos(dividerRad).toFloat() * (dividerRadius - 15f)
        val startY = center.y + sin(dividerRad).toFloat() * (dividerRadius - 15f)
        val endX = center.x + cos(dividerRad).toFloat() * (dividerRadius + 15f)
        val endY = center.y + sin(dividerRad).toFloat() * (dividerRadius + 15f)
        c.drawLine(startX, startY, endX, endY, dividerPaint)
        
        // Dibujar el favicon en el centro con tamaño aumentado
        favicon?.let { icon ->
            val iconSize = (mChart.holeRadius * 2.25f).toInt() // Aumentado a 150%
            val left = (center.x - iconSize / 2).toInt()
            val top = (center.y - iconSize / 2).toInt()
            val right = (center.x + iconSize / 2).toInt()
            val bottom = (center.y + iconSize / 2).toInt()
            
            icon.setBounds(left, top, right, bottom)
            icon.draw(c)
        }
        
        // Convertir horas a ángulo (ajustado para empezar en las 7:00)
        val angleRad = Math.toRadians(360.0 * (elapsedHours / 12.0) + 120.0) // 120 grados = posición 7:00
        
        val targetX = center.x + cos(angleRad).toFloat() * radius
        val targetY = center.y + sin(angleRad).toFloat() * radius
        
        // Dibujar la aguja
        c.drawLine(center.x, center.y, targetX, targetY, paint)
        
        // Dibujar un círculo pequeño en el centro
        paint.style = Paint.Style.FILL
        c.drawCircle(center.x, center.y, 10f, paint)
    }
} 