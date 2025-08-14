package com.bithermmanagement.core.utils

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.*

class DebugGestureDetector(private val context: Context) {
    
    private var debugObserver: DebugLifecycleObserver? = null
    private var tapCount = 0
    private var lastTapTime = 0L
    private val tapTimeout = 500L // 500ms entre taps
    private val requiredTaps = 3
    
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            handleTap()
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Doble tap para mostrar info permanente
            showPermanentDebugInfo()
            return true
        }
    })
    
    fun attachToView(view: View) {
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // No consumir el evento
        }
    }
    
    fun setDebugObserver(observer: DebugLifecycleObserver) {
        debugObserver = observer
    }
    
    private fun handleTap() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastTapTime > tapTimeout) {
            // Reset contador si pasó mucho tiempo
            tapCount = 1
        } else {
            tapCount++
        }
        
        lastTapTime = currentTime
        
        if (tapCount >= requiredTaps) {
            toggleDebugMode()
            tapCount = 0
        }
    }
    
    private fun toggleDebugMode() {
        debugObserver?.toggleDebugMode()
        
        val isDebugEnabled = DebugConfigManager.isDebugModeEnabled(context)
        val message = if (isDebugEnabled) "🐛 Debug Mode ENABLED" else "🐛 Debug Mode DISABLED"
        
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showPermanentDebugInfo() {
        // Mostrar información de debug permanente por 10 segundos
        debugObserver?.let { observer ->
            if (DebugConfigManager.isDebugModeEnabled(context)) {
                // Aquí podrías mostrar información adicional o hacer el overlay permanente
                Toast.makeText(context, "🔍 Debug Info: Permanent Mode", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private var instance: DebugGestureDetector? = null
        
        fun getInstance(context: Context): DebugGestureDetector {
            if (instance == null) {
                instance = DebugGestureDetector(context.applicationContext)
            }
            return instance!!
        }
    }
}

// Extensión para facilitar el uso
fun View.enableDebugGestures(context: Context) {
    val gestureDetector = DebugGestureDetector.getInstance(context)
    gestureDetector.attachToView(this)
}
