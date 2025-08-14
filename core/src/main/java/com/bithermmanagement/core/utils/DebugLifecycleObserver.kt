package com.bithermmanagement.core.utils

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.lang.reflect.Field
import android.os.Bundle
import android.util.Log

class DebugLifecycleObserver(private val context: Context) : DefaultLifecycleObserver {
    
    private var debugOverlay: DebugOverlayView? = null
    private var isDebugMode = false
    
    companion object {
        private var instance: DebugLifecycleObserver? = null
        
        fun getInstance(context: Context): DebugLifecycleObserver {
            if (instance == null) {
                instance = DebugLifecycleObserver(context)
            }
            return instance!!
        }
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d("DebugLifecycleObserver", "onStart called")
        setupDebugMode()
        if (isDebugMode) {
            showDebugOverlay()
        }
    }
    
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("DebugLifecycleObserver", "onResume called")
        if (isDebugMode) {
            showDebugOverlay()
        }
    }
    
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d("DebugLifecycleObserver", "onPause called")
        hideDebugOverlay()
    }
    
    private fun setupDebugMode() {
        isDebugMode = DebugConfigManager.isDebugModeEnabled(context)
        Log.d("DebugLifecycleObserver", "setupDebugMode: isDebugMode = $isDebugMode")
        if (isDebugMode) {
            registerFragmentLifecycleCallbacks()
        }
    }
    
    private fun registerFragmentLifecycleCallbacks() {
        val activity = context as? FragmentActivity ?: return
        Log.d("DebugLifecycleObserver", "registerFragmentLifecycleCallbacks: Activity = ${activity.javaClass.simpleName}")
        
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    v: View,
                    savedInstanceState: Bundle?
                ) {
                    super.onFragmentViewCreated(fm, f, v, savedInstanceState)
                    Log.d("DebugLifecycleObserver", "onFragmentViewCreated: Fragment = ${f.javaClass.simpleName}")
                    if (isDebugMode) {
                        updateDebugInfo(f, v)
                    }
                }
                
                override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                    super.onFragmentViewDestroyed(fm, f)
                    Log.d("DebugLifecycleObserver", "onFragmentViewDestroyed: Fragment = ${f.javaClass.simpleName}")
                    if (isDebugMode) {
                        clearDebugInfo()
                    }
                }
            }, true
        )
        Log.d("DebugLifecycleObserver", "registerFragmentLifecycleCallbacks: Callbacks registered successfully")
    }
    
    private fun updateDebugInfo(fragment: Fragment, view: View) {
        Log.d("DebugLifecycleObserver", "updateDebugInfo: Fragment = ${fragment.javaClass.simpleName}, Overlay = ${debugOverlay != null}")
        val debugInfo = collectDebugInfo(fragment, view)
        debugOverlay?.updateInfo(debugInfo)
    }
    
    private fun collectDebugInfo(fragment: Fragment, view: View): DebugInfo {
        val fragmentName = fragment.javaClass.simpleName
        val layoutId = getLayoutId(view)
        val layoutName = getLayoutName(layoutId, fragment)
        val childFragments = getChildFragments(fragment)
        val viewHierarchy = analyzeViewHierarchy(view)
        
        return DebugInfo(
            fragmentName = fragmentName,
            layoutId = layoutId,
            layoutName = layoutName,
            childFragments = childFragments,
            viewHierarchy = viewHierarchy,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun getLayoutId(view: View): Int {
        return try {
            // Intentar obtener el ID del layout de varias formas
            val id = view.id
            if (id != View.NO_ID) {
                Log.d("DebugLifecycleObserver", "getLayoutId: Success using view.id = $id")
                return id
            }
            
            // Intentar obtener el ID del fragment
            val fragment = view.tag as? Fragment
            if (fragment != null) {
                val layoutId = fragment.arguments?.getInt("layout_id", -1) ?: -1
                if (layoutId != -1) {
                    Log.d("DebugLifecycleObserver", "getLayoutId: Success using fragment layout_id = $layoutId")
                    return layoutId
                }
            }
            
            // Si no funciona, intentar con reflection (último recurso)
            try {
                val field = View::class.java.getDeclaredField("mID")
                field.isAccessible = true
                val layoutId = field.getInt(view)
                Log.d("DebugLifecycleObserver", "getLayoutId: Success using reflection = $layoutId")
                return layoutId
            } catch (reflectionException: Exception) {
                Log.d("DebugLifecycleObserver", "getLayoutId: Reflection failed: ${reflectionException.message}")
            }
            
            Log.d("DebugLifecycleObserver", "getLayoutId: All methods failed, returning -1")
            -1
        } catch (e: Exception) {
            Log.d("DebugLifecycleObserver", "getLayoutId: Error getting layout ID: ${e.message}")
            -1
        }
    }
    
    private fun getLayoutName(layoutId: Int, fragment: Fragment): String {
        if (layoutId == -1) {
            // Intentar obtener el layout name del fragment
            return try {
                val fragmentName = fragment.javaClass.simpleName
                // Mapear nombres de fragment a layouts conocidos
                when {
                    fragmentName.contains("InspeccionActual") -> "fragment_inspeccion_actual"
                    fragmentName.contains("InspeccionEquipo") -> "fragment_inspeccion_equipo"
                    fragmentName.contains("InspeccionBusqueda") -> "fragment_inspeccion_busqueda"
                    fragmentName.contains("InspeccionConfiguracion") -> "fragment_inspeccion_configuracion"
                    fragmentName.contains("InspeccionListado") -> "fragment_inspeccion_listado"
                    fragmentName.contains("SubMenuList") -> "fragment_sub_menu_list"
                    fragmentName.contains("MainMenu") -> "fragment_main_menu"
                    fragmentName.contains("MiniGaleria") -> "fragment_mini_galeria"
                    fragmentName.contains("UsuariosPerfil") -> "fragment_usuarios_perfil"
                    else -> "Unknown"
                }
            } catch (e: Exception) {
                Log.d("DebugLifecycleObserver", "getLayoutName: Error mapping fragment name: ${e.message}")
                "Unknown"
            }
        }
        
        return try {
            context.resources.getResourceEntryName(layoutId)
        } catch (e: Exception) {
            Log.d("DebugLifecycleObserver", "getLayoutName: Error getting layout name for ID $layoutId: ${e.message}")
            "Unknown"
        }
    }
    
    private fun getChildFragments(fragment: Fragment): List<String> {
        return fragment.childFragmentManager.fragments.map { it.javaClass.simpleName }
    }
    
    private fun analyzeViewHierarchy(view: View): ViewHierarchyInfo {
        val totalViews = countViews(view)
        val recyclerViews = countRecyclerViews(view)
        val viewPagers = countViewPagers(view)
        val fragments = countFragmentContainers(view)
        
        return ViewHierarchyInfo(
            totalViews = totalViews,
            recyclerViews = recyclerViews,
            viewPagers = viewPagers,
            fragments = fragments
        )
    }
    
    private fun countViews(view: View): Int {
        var count = 1
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                count += countViews(view.getChildAt(i))
            }
        }
        return count
    }
    
    private fun countRecyclerViews(view: View): Int {
        var count = if (view is RecyclerView) 1 else 0
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                count += countRecyclerViews(view.getChildAt(i))
            }
        }
        return count
    }
    
    private fun countViewPagers(view: View): Int {
        var count = if (view is ViewPager2) 1 else 0
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                count += countViewPagers(view.getChildAt(i))
            }
        }
        return count
    }
    
    private fun countFragmentContainers(view: View): Int {
        var count = if (view is FrameLayout && view.id != android.R.id.content) 1 else 0
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                count += countFragmentContainers(view.getChildAt(i))
            }
        }
        return count
    }
    
    private fun showDebugOverlay() {
        val activity = context as? FragmentActivity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        
        Log.d("DebugLifecycleObserver", "showDebugOverlay: Activity = ${activity.javaClass.simpleName}")
        
        if (debugOverlay == null) {
            debugOverlay = DebugOverlayView(context)
            Log.d("DebugLifecycleObserver", "showDebugOverlay: Created new overlay")
        }
        
        if (debugOverlay?.parent == null) {
            rootView.addView(debugOverlay)
            Log.d("DebugLifecycleObserver", "showDebugOverlay: Added overlay to root view")
            
            // Mostrar información inicial si hay un fragment activo
            val currentFragment = activity.supportFragmentManager.fragments.lastOrNull()
            if (currentFragment != null && currentFragment.view != null) {
                Log.d("DebugLifecycleObserver", "showDebugOverlay: Updating with current fragment")
                updateDebugInfo(currentFragment, currentFragment.view!!)
            }
        }
    }
    
    private fun hideDebugOverlay() {
        debugOverlay?.let { overlay ->
            (overlay.parent as? ViewGroup)?.removeView(overlay)
        }
    }
    
    private fun clearDebugInfo() {
        debugOverlay?.clearInfo()
    }
    
    fun toggleDebugMode() {
        isDebugMode = !isDebugMode
        DebugConfigManager.setDebugMode(context, isDebugMode)
        
        if (isDebugMode) {
            showDebugOverlay()
        } else {
            hideDebugOverlay()
        }
    }
    
    fun updateDebugMode() {
        val newDebugMode = DebugConfigManager.isDebugModeEnabled(context)
        Log.d("DebugLifecycleObserver", "updateDebugMode: newDebugMode = $newDebugMode, current = $isDebugMode")
        if (newDebugMode != isDebugMode) {
            isDebugMode = newDebugMode
            if (isDebugMode) {
                Log.d("DebugLifecycleObserver", "updateDebugMode: Enabling debug mode")
                showDebugOverlay()
                registerFragmentLifecycleCallbacks()
            } else {
                Log.d("DebugLifecycleObserver", "updateDebugMode: Disabling debug mode")
                hideDebugOverlay()
            }
        }
    }
}

data class DebugInfo(
    val fragmentName: String,
    val layoutId: Int,
    val layoutName: String,
    val childFragments: List<String>,
    val viewHierarchy: ViewHierarchyInfo,
    val timestamp: Long
)

data class ViewHierarchyInfo(
    val totalViews: Int,
    val recyclerViews: Int,
    val viewPagers: Int,
    val fragments: Int
)
