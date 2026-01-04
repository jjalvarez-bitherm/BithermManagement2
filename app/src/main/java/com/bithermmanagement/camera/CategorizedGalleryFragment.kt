package com.bithermmanagement.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.google.android.material.tabs.TabLayout

class CategorizedGalleryFragment : Fragment() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var cameraManager: CameraManager
    private lateinit var galleryAdapter: CategorizedGalleryAdapter
    
    private var currentCategory = CameraManager.TIPO_GPS
    private var equipoId: String? = null
    
    companion object {
        fun newInstance(equipoId: String? = null): CategorizedGalleryFragment {
            return CategorizedGalleryFragment().apply {
                arguments = Bundle().apply {
                    putString("equipo_id", equipoId)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        equipoId = arguments?.getString("equipo_id")
        cameraManager = CameraManager(this)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categorized_gallery, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        setupTabs()
        setupRecyclerView()
        loadPhotos()
    }
    
    private fun setupViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerViewGallery)
        tvEmpty = view.findViewById(R.id.tvEmptyGallery)
    }
    
    private fun setupTabs() {
        val categories = listOf(
            "GPS" to CameraManager.TIPO_GPS,
            "DENUNCIAS" to CameraManager.TIPO_DENUNCIA,
            "NOTAS" to CameraManager.TIPO_NOTA,
            "PROYECTOS" to CameraManager.TIPO_PROYECTO
        )
        
        categories.forEach { (title, type) ->
            tabLayout.addTab(tabLayout.newTab().setText(title).setTag(type))
        }
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentCategory = tab.tag as String
                loadPhotos()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun setupRecyclerView() {
        galleryAdapter = CategorizedGalleryAdapter { photoPath ->
            openPhotoViewer(photoPath)
        }
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = galleryAdapter
        }
    }
    
    private fun loadPhotos() {
        val photos = cameraManager.getPhotosByType(currentCategory, equipoId)
        
        if (photos.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "No hay fotos de tipo ${getCategoryDisplayName(currentCategory)}"
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            galleryAdapter.updatePhotos(photos)
        }
    }
    
    private fun getCategoryDisplayName(category: String): String {
        return when (category) {
            CameraManager.TIPO_GPS -> "GPS"
            CameraManager.TIPO_DENUNCIA -> "Denuncias"
            CameraManager.TIPO_NOTA -> "Notas"
            CameraManager.TIPO_PROYECTO -> "Proyectos"
            else -> category
        }
    }
    
    private fun openPhotoViewer(photoPath: String) {
        // Abrir visor individual de foto con opciones de edición
        val photos = cameraManager.getPhotosByType(currentCategory, equipoId)
        val index = photos.indexOf(photoPath)
        
        val viewerFragment = CategorizedPhotoViewerFragment.newInstance(
            photos.toTypedArray(),
            index,
            currentCategory,
            equipoId
        )
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, viewerFragment)
            .addToBackStack(null)
            .commit()
    }
    
    fun refreshGallery() {
        loadPhotos()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar fotos por si se añadieron nuevas
        loadPhotos()
    }
}