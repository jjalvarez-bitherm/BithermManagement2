package com.bithermmanagement.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bithermmanagement.R

class CategorizedPhotoViewerFragment : Fragment() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var tvCounter: TextView
    private lateinit var tvCategory: TextView
    
    private var photos: Array<String> = emptyArray()
    private var initialIndex: Int = 0
    private var category: String = ""
    private var equipoId: String? = null
    
    companion object {
        fun newInstance(
            photos: Array<String>,
            initialIndex: Int,
            category: String,
            equipoId: String?
        ): CategorizedPhotoViewerFragment {
            return CategorizedPhotoViewerFragment().apply {
                arguments = Bundle().apply {
                    putStringArray("photos", photos)
                    putInt("initial_index", initialIndex)
                    putString("category", category)
                    putString("equipo_id", equipoId)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photos = it.getStringArray("photos") ?: emptyArray()
            initialIndex = it.getInt("initial_index", 0)
            category = it.getString("category", "")
            equipoId = it.getString("equipo_id")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categorized_photo_viewer, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        setupViewPager()
        updateUI()
    }
    
    private fun setupViews(view: View) {
        viewPager = view.findViewById(R.id.viewPagerPhotos)
        btnBack = view.findViewById(R.id.btnBack)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnDelete = view.findViewById(R.id.btnDelete)
        tvCounter = view.findViewById(R.id.tvPhotoCounter)
        tvCategory = view.findViewById(R.id.tvPhotoCategory)
        
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        btnEdit.setOnClickListener {
            editCurrentPhoto()
        }
        
        btnDelete.setOnClickListener {
            deleteCurrentPhoto()
        }
    }
    
    private fun setupViewPager() {
        val adapter = CategorizedPhotoViewerAdapter(photos.toList())
        viewPager.adapter = adapter
        viewPager.setCurrentItem(initialIndex, false)
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUI()
            }
        })
    }
    
    private fun updateUI() {
        val currentPosition = viewPager.currentItem + 1
        tvCounter.text = "$currentPosition / ${photos.size}"
        tvCategory.text = getCategoryDisplayName(category)
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
    
    private fun editCurrentPhoto() {
        val currentPhoto = photos[viewPager.currentItem]
        // TODO: Abrir editor de fotos con la imagen actual
    }
    
    private fun deleteCurrentPhoto() {
        // TODO: Implementar confirmación y eliminación de foto
    }
}