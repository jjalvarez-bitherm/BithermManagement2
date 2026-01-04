package com.bithermmanagement.ui.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bithermmanagement.R
import com.bithermmanagement.ui.gallery.GalleryPhoto
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImageViewerActivity : AppCompatActivity() {
    
    private lateinit var photoView: PhotoView
    private lateinit var btnBack: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var tvImageTitle: TextView
    private lateinit var tvImageInfo: TextView
    private var currentPhoto: GalleryPhoto? = null
    private var photos: List<GalleryPhoto> = emptyList()
    private var currentIndex: Int = 0
    
    companion object {
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_PHOTO_LIST = "photo_list"
        const val EXTRA_CURRENT_INDEX = "current_index"
        
        fun start(context: Context, photo: GalleryPhoto, allPhotos: List<GalleryPhoto> = listOf(photo), currentIndex: Int = 0) {
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_PHOTO_PATH, photo.file.absolutePath)
                putStringArrayListExtra(EXTRA_PHOTO_LIST, ArrayList(allPhotos.map { it.file.absolutePath }))
                putExtra(EXTRA_CURRENT_INDEX, currentIndex)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        
        initViews()
        setupUI()
        loadImageData()
        setupClickListeners()
    }
    
    private fun initViews() {
        photoView = findViewById(R.id.photoView)
        btnBack = findViewById(R.id.btnBack)
        btnShare = findViewById(R.id.btnShare)
        tvImageTitle = findViewById(R.id.tvImageTitle)
        tvImageInfo = findViewById(R.id.tvImageInfo)
    }
    
    private fun setupUI() {
        // Ocultar la barra de estado para inmersión completa
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
    
    private fun loadImageData() {
        val photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH)
        val photoList = intent.getStringArrayListExtra(EXTRA_PHOTO_LIST) ?: emptyList()
        currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
        
        if (photoPath == null) {
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Convertir las rutas de archivos de vuelta a GalleryPhoto objects
        photos = photoList.mapNotNull { path ->
            val file = File(path)
            if (file.exists() && !file.isDirectory) {
                GalleryPhoto(file, file.lastModified(), "", false)
            } else null
        }
        
        currentPhoto = GalleryPhoto(File(photoPath), File(photoPath).lastModified(), "", false)
        
        loadCurrentImage()
    }
    
    private fun loadCurrentImage() {
        currentPhoto?.let { photo ->
            // Cargar imagen con Glide
            Glide.with(this)
                .load(photo.file)
                .into(photoView)
                
            // Actualizar título
            tvImageTitle.text = photo.file.name
            
            // Actualizar información de la imagen
            updateImageInfo(photo)
        }
    }
    
    private fun updateImageInfo(photo: GalleryPhoto) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateText = dateFormat.format(Date(photo.timestamp))
        
        val fileSize = photo.file.length() / 1024 // KB
        val sizeText = when {
            fileSize < 1024 -> "${fileSize} KB"
            else -> "${fileSize / 1024} MB"
        }
        
        val info = if (photos.size > 1) {
            "${currentIndex + 1} de ${photos.size} • $dateText • $sizeText"
        } else {
            "$dateText • $sizeText"
        }
        
        tvImageInfo.text = info
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnShare.setOnClickListener {
            shareImage()
        }
        
        // Swipe gestures para navegar entre imágenes
        if (photos.size > 1) {
            setupSwipeGestures()
        }
    }
    
    private fun setupSwipeGestures() {
        // TODO: Implementar gestos de deslizamiento para navegar entre imágenes
        // Por ahora, solo mostramos una imagen
    }
    
    private fun shareImage() {
        currentPhoto?.let { photo ->
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    photo.file
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "Compartir imagen"))
            } catch (e: Exception) {
                Toast.makeText(this, "Error al compartir la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
}