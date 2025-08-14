package com.bithermmanagement.core.base

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ProcessLifecycleOwner
import com.bithermmanagement.core.utils.DebugConfigManager
import com.bithermmanagement.core.utils.DebugGestureDetector
import com.bithermmanagement.core.utils.DebugLifecycleObserver
import android.view.ViewGroup
import android.widget.FrameLayout

abstract class DebugBaseActivity : AppCompatActivity() {
    
    private lateinit var debugObserver: DebugLifecycleObserver
    private lateinit var gestureDetector: DebugGestureDetector
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar sistema de debug
        initializeDebugSystem()
    }
    
    private fun initializeDebugSystem() {
        // Crear observer de lifecycle
        debugObserver = DebugLifecycleObserver.getInstance(this)
        
        // Registrar observer en el lifecycle de esta Activity específica
        lifecycle.addObserver(debugObserver)
        
        // Configurar detector de gestos
        gestureDetector = DebugGestureDetector.getInstance(this)
        gestureDetector.setDebugObserver(debugObserver)
        
        // Habilitar gestos en la vista raíz
        enableDebugGesturesOnRoot()
    }
    
    private fun enableDebugGesturesOnRoot() {
        // Esperar a que la vista esté lista
        findViewById<ViewGroup>(android.R.id.content).post {
            val rootView = findViewById<ViewGroup>(android.R.id.content)
            gestureDetector.attachToView(rootView)
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Verificar si el modo debug está habilitado
        if (DebugConfigManager.isDebugModeEnabled(this)) {
            showDebugEnabledNotification()
        }
    }
    
    private fun showDebugEnabledNotification() {
        // Mostrar notificación sutil de que el debug está activo
        val debugIndicator = createDebugIndicator()
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        
        if (debugIndicator.parent == null) {
            rootView.addView(debugIndicator)
        }
    }
    
    private fun createDebugIndicator(): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP or android.view.Gravity.START
            ).apply {
                setMargins(16, 100, 16, 16)
            }
            
            // Indicador visual sutil
            setBackgroundColor(android.graphics.Color.parseColor("#1AFF9800"))
            alpha = 0.3f
            
            // Auto-ocultar después de 2 segundos
            postDelayed({
                alpha = 0f
                (parent as? ViewGroup)?.removeView(this)
            }, 2000)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Limpiar observer
        ProcessLifecycleOwner.get().lifecycle.removeObserver(debugObserver)
    }
    
    // Métodos de conveniencia para el debug
    protected fun enableDebugMode() {
        DebugConfigManager.setDebugMode(this, true)
    }
    
    protected fun disableDebugMode() {
        DebugConfigManager.setDebugMode(this, false)
    }
    
    protected fun isDebugModeEnabled(): Boolean {
        return DebugConfigManager.isDebugModeEnabled(this)
    }
}
