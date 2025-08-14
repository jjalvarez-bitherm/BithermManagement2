package com.bithermmanagement.core.utils

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Button
import android.util.Log

class DebugOverlayView(context: Context) : FrameLayout(context) {
    
    private var fragmentNameText: TextView? = null
    private var layoutNameText: TextView? = null
    private var childFragmentsText: TextView? = null
    private var viewHierarchyText: TextView? = null
    private var okButton: Button? = null
    
    private val handler = Handler(Looper.getMainLooper())
    
    init {
        Log.d("DebugOverlayView", "🚀 Initializing DebugOverlayView")
        setupOverlay()
        Log.d("DebugOverlayView", "✅ DebugOverlayView initialized successfully")
    }
    
    private fun setupOverlay() {
        // Hacer el overlay MUY visible y centrado
        setBackgroundColor(android.graphics.Color.RED) // Fondo rojo sólido
        alpha = 1f // Siempre visible para debug
        visibility = View.VISIBLE // Forzar visibilidad
        
        // Agrandar: 120% horizontal (720) y 250% vertical (1000) para mejor visibilidad
        layoutParams = LayoutParams(
            720, // Ancho fijo de 720dp (120% de 600)
            1000, // Alto fijo de 1000dp (250% de 400) - MÁS ALTO
            Gravity.CENTER // Centrado en pantalla
        )
        
        createAndAddContentView()
        
        Log.d("DebugOverlayView", "🎯 setupOverlay: Overlay creado centrado, dimensiones 720x1000, visibility = $visibility")
    }
    
    private fun createAndAddContentView() {
        // Crear un LinearLayout simple con fondo azul para debug
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLUE) // Fondo azul para debug
            setPadding(32, 32, 32, 32) // Padding aumentado para el overlay más alto
            gravity = Gravity.CENTER
            
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Título
        val titleText = TextView(context).apply {
            text = "🐛 DEBUG INFO 🐛"
            textSize = 24f // Texto más grande para el overlay más alto
            setTextColor(android.graphics.Color.YELLOW) // Forzar color amarillo
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        
        // Crear TextViews simples con texto amarillo brillante
        fragmentNameText = TextView(context).apply {
            text = "📱 Fragment: Waiting..."
            textSize = 18f // Texto más grande
            setTextColor(android.graphics.Color.YELLOW) // Forzar color amarillo
            setPadding(0, 12, 0, 12)
        }
        
        layoutNameText = TextView(context).apply {
            text = "📄 Layout: Waiting..."
            textSize = 18f // Texto más grande
            setTextColor(android.graphics.Color.YELLOW) // Forzar color amarillo
            setPadding(0, 12, 0, 12)
        }
        
        childFragmentsText = TextView(context).apply {
            text = "🔗 Children: Waiting..."
            textSize = 18f // Texto más grande
            setTextColor(android.graphics.Color.YELLOW) // Forzar color amarillo
            setPadding(0, 12, 0, 12)
        }
        
        viewHierarchyText = TextView(context).apply {
            text = "🏗️ Views: Waiting..."
            textSize = 18f // Texto más grande
            setTextColor(android.graphics.Color.YELLOW) // Forzar color amarillo
            setPadding(0, 12, 0, 32)
        }
        
        // Botón OK - HACERLO MÁS VISIBLE
        okButton = Button(context).apply {
            text = "✅ OK - CERRAR"
            textSize = 20f // Texto más grande
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.GREEN)
            setPadding(64, 24, 64, 24) // Padding aumentado
            
            // Hacer el botón más visible
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                setMargins(0, 24, 0, 0)
            }
            
            setOnClickListener {
                Log.d("DebugOverlayView", "🔘 OK button clicked - Cerrando overlay")
                (parent as? ViewGroup)?.removeView(this@DebugOverlayView)
            }
        }
        
        // Añadir todos los elementos al layout (SIN timestamp)
        contentLayout.addView(titleText)
        contentLayout.addView(fragmentNameText)
        contentLayout.addView(layoutNameText)
        contentLayout.addView(childFragmentsText)
        contentLayout.addView(viewHierarchyText)
        contentLayout.addView(okButton)
        
        // Añadir el layout al overlay
        addView(contentLayout)
        
        Log.d("DebugOverlayView", "🎨 createAndAddContentView: Content layout creado con fondo azul, texto amarillo y botón OK GRANDE")
    }
    
    fun updateInfo(debugInfo: DebugInfo) {
        Log.d("DebugOverlayView", "📝 updateInfo: Fragment = ${debugInfo.fragmentName}, Layout = ${debugInfo.layoutName}")
        
        handler.post {
            Log.d("DebugOverlayView", "🔄 Actualizando TextViews...")
            
            // ACTUALIZAR CON INFORMACIÓN REAL
            fragmentNameText?.text = "📱 Fragment: ${debugInfo.fragmentName}"
            layoutNameText?.text = "📄 Layout: ${debugInfo.layoutName}"
            
            val childText = if (debugInfo.childFragments.isNotEmpty()) {
                debugInfo.childFragments.joinToString(", ")
            } else {
                "None"
            }
            childFragmentsText?.text = "🔗 Children: $childText"
            
            val hierarchy = debugInfo.viewHierarchy
            viewHierarchyText?.text = "🏗️ Views: ${hierarchy.totalViews} total, " +
                    "${hierarchy.recyclerViews} RecyclerViews, " +
                    "${hierarchy.viewPagers} ViewPagers, " +
                    "${hierarchy.fragments} containers"
            
            Log.d("DebugOverlayView", "✅ TextViews actualizados correctamente")
            Log.d("DebugOverlayView", "📊 Fragment: ${debugInfo.fragmentName}")
            Log.d("DebugOverlayView", "📊 Layout: ${debugInfo.layoutName}")
            Log.d("DebugOverlayView", "📊 Children: $childText")
            Log.d("DebugOverlayView", "📊 Views: ${hierarchy.totalViews} total")
            
            showWithAnimation()
            
            // Log para verificar dimensiones
            post {
                Log.d("DebugOverlayView", "📏 Overlay dimensions - Width: $width, Height: $height, X: $x, Y: $y")
                Log.d("DebugOverlayView", "👁️ Overlay visibility - Visible: $visibility, Alpha: $alpha")
            }
        }
    }
    
    private fun showWithAnimation() {
        // Para debug, mantener siempre visible
        alpha = 1f
        visibility = View.VISIBLE
        Log.d("DebugOverlayView", "🎭 showWithAnimation: Overlay visible, alpha = $alpha, visibility = $visibility")
    }
    
    fun clearInfo() {
        Log.d("DebugOverlayView", "🗑️ clearInfo: Limpiando información")
        handler.post {
            // NO LIMPIAR - MANTENER LA ÚLTIMA INFORMACIÓN
            Log.d("DebugOverlayView", "⚠️ clearInfo: NO se limpia la información para mantener debug visible")
        }
    }
    
    fun showPermanently() {
        alpha = 1f
        visibility = View.VISIBLE
        Log.d("DebugOverlayView", "👁️ showPermanently: Overlay visible permanentemente")
    }
    
    fun hidePermanently() {
        alpha = 0f
        visibility = View.GONE
        Log.d("DebugOverlayView", "🙈 hidePermanently: Overlay oculto permanentemente")
    }
}
