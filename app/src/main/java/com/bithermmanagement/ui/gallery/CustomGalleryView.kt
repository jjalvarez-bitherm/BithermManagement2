package com.bithermmanagement.ui.gallery

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ui.activities.ImageViewerActivity
import kotlinx.coroutines.*
import java.io.File

class CustomGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnDelete: ImageView
    private lateinit var btnRename: ImageView
    private lateinit var btnMove: ImageView
    private lateinit var btnShare: ImageView
    private lateinit var rvFolders: RecyclerView
    private lateinit var rvPhotos: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var llEmpty: LinearLayout
    private lateinit var fabAddFolder: com.google.android.material.floatingactionbutton.FloatingActionButton

    // Adaptadores
    private var foldersAdapter: GalleryFolderAdapter? = null
    private var photosAdapter: GalleryPhotoAdapter? = null

    // Datos
    private var folders: List<GalleryFolder> = emptyList()
    private var currentFolder: GalleryFolder? = null
    private var currentSubfolder: File? = null
    private var navigationStack = mutableListOf<Pair<GalleryFolder, File?>>()

    // Estados
    private var isInFolderView = false

    // Callbacks
    var onBackPressed: (() -> Unit)? = null
    var onPhotoSelected: ((GalleryPhoto) -> Unit)? = null

    // Corrutinas
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initView()
        loadGalleryData()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.layout_gallery_main, this, true)
        
        // Obtener referencias de views
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        btnDelete = findViewById(R.id.btnDelete)
        btnRename = findViewById(R.id.btnRename)
        btnMove = findViewById(R.id.btnMove)
        btnShare = findViewById(R.id.btnShare)
        rvFolders = findViewById(R.id.rvFolders)
        rvPhotos = findViewById(R.id.rvPhotos)
        pbLoading = findViewById(R.id.pbLoading)
        llEmpty = findViewById(R.id.llEmpty)
        fabAddFolder = findViewById(R.id.fabAddFolder)

        // Configurar RecyclerViews
        setupRecyclerViews()

        // Configurar listeners
        setupListeners()

        Log.d("CustomGalleryView", "Vista inicializada")
    }

    private fun setupRecyclerViews() {
        // StaggeredGrid para carpetas (2 columnas con tamaños personalizados)
        rvFolders.layoutManager = androidx.recyclerview.widget.StaggeredGridLayoutManager(
            2, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
        )
        
        // Grid para fotos (3 columnas)
        rvPhotos.layoutManager = GridLayoutManager(context, 3)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            handleBackNavigation()
        }

        btnDelete.setOnClickListener {
            val selectedPhotos = photosAdapter?.getSelectedPhotos() ?: emptyList()
            if (selectedPhotos.isNotEmpty()) deletePhotos(selectedPhotos)
        }

        btnRename.setOnClickListener {
            val selectedPhotos = photosAdapter?.getSelectedPhotos() ?: emptyList()
            if (selectedPhotos.size == 1) renamePhoto(selectedPhotos.first())
        }

        btnMove.setOnClickListener {
            val selectedPhotos = photosAdapter?.getSelectedPhotos() ?: emptyList()
            if (selectedPhotos.isNotEmpty()) movePhotos(selectedPhotos)
        }

        btnShare.setOnClickListener {
            val selectedPhotos = photosAdapter?.getSelectedPhotos() ?: emptyList()
            if (selectedPhotos.isNotEmpty()) sharePhotos(selectedPhotos)
        }

        fabAddFolder.setOnClickListener {
            if (isInFolderView) {
                // Si estamos en una carpeta, mostrar opciones para añadir subcarpeta
                showCreateSubfolderDialog()
            } else {
                // Si estamos en la vista principal, no permitir crear nuevas carpetas principales
                Toast.makeText(context, "Las carpetas principales están predefinidas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGalleryData() {
        showLoading(true)
        
        scope.launch {
            try {
                val loadedFolders = withContext(Dispatchers.IO) {
                    PhotoScanner.scanAppPhotos(context)
                }
                
                folders = loadedFolders
                
                if (folders.isEmpty()) {
                    showEmpty(true)
                } else {
                    showMainView()
                }
                
                Log.d("CustomGalleryView", "Carpetas cargadas: ${folders.size}")
                
            } catch (e: Exception) {
                Log.e("CustomGalleryView", "Error cargando galería", e)
                showEmpty(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showMainView() {
        isInFolderView = false
        currentFolder = null
        
        tvTitle.text = "Galería"
        btnDelete.visibility = View.GONE
        btnRename.visibility = View.GONE
        btnMove.visibility = View.GONE
        btnShare.visibility = View.GONE
        fabAddFolder.visibility = View.GONE  // Ocultar FAB en vista principal
        
        rvFolders.visibility = View.VISIBLE
        rvPhotos.visibility = View.GONE
        
        // Configurar adaptador de carpetas
        if (foldersAdapter == null) {
            foldersAdapter = GalleryFolderAdapter(folders) { folder ->
                if (!folder.isUnderConstruction) {
                    showPhotosView(folder)
                } else {
                    Toast.makeText(context, "${folder.name} está en construcción", Toast.LENGTH_SHORT).show()
                }
            }
            rvFolders.adapter = foldersAdapter
        }
        
        Log.d("CustomGalleryView", "Mostrando vista de carpetas")
    }

    private fun showPhotosView(folder: GalleryFolder) {
        isInFolderView = true
        currentFolder = folder
        
        tvTitle.text = folder.name
        
        // Mostrar FAB solo para carpetas específicas que permiten subcarpetas
        fabAddFolder.visibility = if (folder.name in listOf("PROYECTOS", "PURGADORES", "FOTO NOTA", "MONITORIZACIÓN")) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        rvFolders.visibility = View.GONE
        rvPhotos.visibility = View.VISIBLE
        
        // Configurar adaptador de fotos
        photosAdapter = GalleryPhotoAdapter(
            photos = folder.photos,
            onPhotoClick = { photo, position ->
                Log.d("CustomGalleryView", "Elemento seleccionado: ${photo.file.name}, isSubfolder: ${photo.isSubfolder}")
                if (photo.isSubfolder) {
                    // Es una subcarpeta - navegar a ella
                    navigateToSubfolder(photo.file)
                } else {
                    // Es una imagen - abrir visor
                    openImageViewer(photo, folder.photos.filter { !it.isSubfolder }, position)
                }
            },
            onPhotoLongClick = { photo, position ->
                Log.d("CustomGalleryView", "Foto long click: ${photo.file.name}")
                photosAdapter?.enterSelectionMode(position)
                updateSelectionUI()
                true
            }
        )
        
        // Configurar listener para cambios de selección
        photosAdapter?.setOnSelectionChangedListener {
            updateSelectionUI()
        }
        
        rvPhotos.adapter = photosAdapter
        
        Log.d("CustomGalleryView", "Mostrando fotos de: ${folder.name}, total: ${folder.photos.size}")
    }

    private fun updateSelectionUI() {
        val adapter = photosAdapter
        if (adapter != null && adapter.isSelectionMode) {
            val selectedCount = adapter.getSelectedCount()
            // Mostrar botones solo si hay selección
            if (selectedCount > 0) {
                btnDelete.visibility = View.VISIBLE
                btnMove.visibility = View.VISIBLE
                btnShare.visibility = View.VISIBLE
                // Renombrar solo con 1 foto
                btnRename.visibility = if (selectedCount == 1) View.VISIBLE else View.GONE
            } else {
                btnDelete.visibility = View.GONE
                btnRename.visibility = View.GONE
                btnMove.visibility = View.GONE
                btnShare.visibility = View.GONE
            }
        } else {
            // Sin modo selección, ocultar todos
            btnDelete.visibility = View.GONE
            btnRename.visibility = View.GONE
            btnMove.visibility = View.GONE
            btnShare.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        pbLoading.visibility = if (show) View.VISIBLE else View.GONE
        rvFolders.visibility = if (show) View.GONE else rvFolders.visibility
        rvPhotos.visibility = if (show) View.GONE else rvPhotos.visibility
        llEmpty.visibility = if (show) View.GONE else llEmpty.visibility
    }

    private fun showEmpty(show: Boolean) {
        llEmpty.visibility = if (show) View.VISIBLE else View.GONE
        rvFolders.visibility = if (show) View.GONE else rvFolders.visibility
        rvPhotos.visibility = if (show) View.GONE else rvPhotos.visibility
    }

    fun getSelectedPhotos(): List<GalleryPhoto> {
        return photosAdapter?.getSelectedPhotos() ?: emptyList()
    }

    fun isInSelectionMode(): Boolean {
        return photosAdapter?.isSelectionMode == true
    }

    fun exitSelectionMode() {
        photosAdapter?.exitSelectionMode()
        updateSelectionUI()
    }

    private fun showCreateSubfolderDialog() {
        val folder = currentFolder ?: return
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle("Crear carpeta en ${folder.name}")
        
        val input = android.widget.EditText(context)
        input.hint = "Nombre de la nueva carpeta"
        builder.setView(input)
        
        builder.setPositiveButton("Crear") { _, _ ->
            val folderName = input.text.toString().trim()
            if (folderName.isNotEmpty()) {
                createSubfolder(folder, folderName)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        
        builder.show()
    }
    
    private fun createSubfolder(parentFolder: GalleryFolder, folderName: String) {
        try {
            val baseDir = when (parentFolder.name) {
                "PROYECTOS" -> File(context.filesDir, "proyectos")
                "PURGADORES" -> File(context.filesDir, "purgadores")
                "FOTO NOTA" -> File(context.filesDir, "foto_notas")
                "MONITORIZACIÓN" -> File(context.filesDir, "monitorizacion")
                else -> return
            }
            
            val newFolder = File(baseDir, folderName)
            if (!newFolder.exists()) {
                newFolder.mkdirs()
                Toast.makeText(context, "Carpeta '$folderName' creada", Toast.LENGTH_SHORT).show()
                // Recargar la vista de carpetas para mostrar la nueva subcarpeta
                refreshFolderView()
            } else {
                Toast.makeText(context, "La carpeta '$folderName' ya existe", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CustomGalleryView", "Error creando carpeta", e)
            Toast.makeText(context, "Error creando carpeta", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun refreshFolderView() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val folders = PhotoScanner.scanAppPhotos(context)
                withContext(Dispatchers.Main) {
                    foldersAdapter?.updateFolders(folders)
                    Log.d("CustomGalleryView", "Vista de carpetas actualizada")
                }
            } catch (e: Exception) {
                Log.e("CustomGalleryView", "Error actualizando carpetas", e)
            }
        }
    }
    
    fun showPhotoActionsDialog(photos: List<GalleryPhoto>) {
        val actions = arrayOf("Borrar", "Renombrar", "Mover", "Compartir")
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle("Acciones (${photos.size} foto${if (photos.size > 1) "s" else ""})")
        builder.setItems(actions) { _, which ->
            when (which) {
                0 -> deletePhotos(photos)
                1 -> if (photos.size == 1) renamePhoto(photos.first()) else Toast.makeText(context, "Solo puedes renombrar una foto a la vez", Toast.LENGTH_SHORT).show()
                2 -> movePhotos(photos)
                3 -> sharePhotos(photos)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
    
    private fun deletePhotos(photos: List<GalleryPhoto>) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle("Confirmar borrado")
        builder.setMessage("¿Estás seguro de que quieres borrar ${photos.size} foto${if (photos.size > 1) "s" else ""}?")
        
        builder.setPositiveButton("Borrar") { _, _ ->
            try {
                photos.forEach { it.file.delete() }
                Toast.makeText(context, "${photos.size} foto${if (photos.size > 1) "s borradas" else " borrada"}", Toast.LENGTH_SHORT).show()
                exitSelectionMode()
                
                // ✅ FIX: Recargar la carpeta actual en lugar de toda la galería
                if (currentSubfolder != null) {
                    // Recargar subcarpeta actual
                    navigateToSubfolder(currentSubfolder!!)
                } else if (currentFolder != null) {
                    // Recargar carpeta actual
                    reloadCurrentFolder()
                } else {
                    // Recargar vista principal
                    loadGalleryData()
                }
            } catch (e: Exception) {
                Log.e("CustomGalleryView", "Error borrando fotos", e)
                Toast.makeText(context, "Error borrando fotos", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
    
    private fun renamePhoto(photo: GalleryPhoto) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle("Renombrar foto")
        
        val input = android.widget.EditText(context)
        input.setText(photo.file.nameWithoutExtension)
        builder.setView(input)
        
        builder.setPositiveButton("Renombrar") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                try {
                    val extension = photo.file.extension
                    val newFile = File(photo.file.parent, "$newName.$extension")
                    if (photo.file.renameTo(newFile)) {
                        Toast.makeText(context, "Foto renombrada", Toast.LENGTH_SHORT).show()
                        loadGalleryData()
                    } else {
                        Toast.makeText(context, "Error renombrando foto", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("CustomGalleryView", "Error renombrando foto", e)
                    Toast.makeText(context, "Error renombrando foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
    
    private fun movePhotos(photos: List<GalleryPhoto>) {
        // TODO: Implementar diálogo para seleccionar carpeta destino
        Toast.makeText(context, "Función de mover en desarrollo", Toast.LENGTH_SHORT).show()
    }
    
    private fun sharePhotos(photos: List<GalleryPhoto>) {
        try {
            val shareIntent = android.content.Intent().apply {
                if (photos.size == 1) {
                    action = android.content.Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, 
                        androidx.core.content.FileProvider.getUriForFile(context, 
                            "${context.packageName}.fileprovider", photos.first().file))
                } else {
                    action = android.content.Intent.ACTION_SEND_MULTIPLE
                    type = "image/*"
                    val uris = photos.map { photo ->
                        androidx.core.content.FileProvider.getUriForFile(context, 
                            "${context.packageName}.fileprovider", photo.file)
                    }
                    putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, ArrayList(uris))
                }
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir fotos"))
        } catch (e: Exception) {
            Log.e("CustomGalleryView", "Error compartiendo fotos", e)
            Toast.makeText(context, "Error compartiendo fotos", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Función para navegar a una subcarpeta
    private fun navigateToSubfolder(subfolderFile: File) {
        if (!subfolderFile.exists() || !subfolderFile.isDirectory) {
            Log.w("CustomGalleryView", "Subcarpeta no válida: ${subfolderFile.absolutePath}")
            return
        }
        
        Log.d("CustomGalleryView", "Navegando a subcarpeta: ${subfolderFile.name}")
        
        // Guardar estado actual en el stack de navegación
        currentFolder?.let { folder ->
            navigationStack.add(Pair(folder, currentSubfolder))
        }
        
        // Establecer nueva subcarpeta actual
        currentSubfolder = subfolderFile
        
        // Cargar fotos de la subcarpeta
        scope.launch {
            try {
                val subfolderPhotos = withContext(Dispatchers.IO) {
                    subfolderFile.listFiles()?.filter { file ->
                        file.isFile && (file.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp"))
                    }?.map { file ->
                        GalleryPhoto(file, file.lastModified(), "", false)
                    }?.sortedByDescending { it.timestamp } ?: emptyList()
                }
                
                // Actualizar UI
                updateSubfolderView(subfolderFile.name, subfolderPhotos)
                
            } catch (e: Exception) {
                Log.e("CustomGalleryView", "Error cargando subcarpeta", e)
                Toast.makeText(context, "Error cargando subcarpeta", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Función para actualizar la vista de subcarpeta
    private fun updateSubfolderView(subfolderName: String, photos: List<GalleryPhoto>) {
        isInFolderView = true
        btnBack.visibility = View.VISIBLE
        tvTitle.text = subfolderName
        btnDelete.visibility = View.GONE
        btnRename.visibility = View.GONE
        btnMove.visibility = View.GONE
        btnShare.visibility = View.GONE
        fabAddFolder.visibility = View.GONE
        
        rvFolders.visibility = View.GONE
        rvPhotos.visibility = View.VISIBLE
        
        // Configurar adaptador de fotos para subcarpeta
        photosAdapter = GalleryPhotoAdapter(
            photos = photos,
            onPhotoClick = { photo, position ->
                openImageViewer(photo, photos, position)
            },
            onPhotoLongClick = { photo, position ->
                photosAdapter?.enterSelectionMode(position)
                updateSelectionUI()
                true
            }
        )
        
        photosAdapter?.setOnSelectionChangedListener {
            updateSelectionUI()
        }
        
        rvPhotos.adapter = photosAdapter
        
        // Actualizar estado vacío
        if (photos.isEmpty()) {
            llEmpty.visibility = View.VISIBLE
            rvPhotos.visibility = View.GONE
        } else {
            llEmpty.visibility = View.GONE
            rvPhotos.visibility = View.VISIBLE
        }
        
        Log.d("CustomGalleryView", "Mostrando subcarpeta: $subfolderName, fotos: ${photos.size}")
    }
    
    // Función para abrir el visor de imágenes
    private fun openImageViewer(photo: GalleryPhoto, allPhotos: List<GalleryPhoto>, currentIndex: Int) {
        Log.d("CustomGalleryView", "Abriendo visor para: ${photo.file.name}")
        
        try {
            // Filtrar solo las imágenes (no subcarpetas)
            val imagePhotos = allPhotos.filter { !it.isSubfolder }
            val imageIndex = imagePhotos.indexOf(photo)
            
            if (imageIndex >= 0) {
                ImageViewerActivity.start(context, photo, imagePhotos, imageIndex)
            } else {
                // Si no se encuentra en la lista, abrir solo esta imagen
                ImageViewerActivity.start(context, photo)
            }
        } catch (e: Exception) {
            Log.e("CustomGalleryView", "Error abriendo visor de imágenes", e)
            Toast.makeText(context, "Error abriendo imagen", Toast.LENGTH_SHORT).show()
        }
    }
    
    // ✅ FIX: Función para recargar la carpeta actual después de eliminar
    private fun reloadCurrentFolder() {
        val folder = currentFolder ?: return
        
        scope.launch {
            try {
                val updatedFolders = withContext(Dispatchers.IO) {
                    PhotoScanner.scanAppPhotos(context)
                }
                
                val updatedFolder = updatedFolders.find { it.name == folder.name }
                if (updatedFolder != null) {
                    showPhotosView(updatedFolder)
                }
            } catch (e: Exception) {
                Log.e("CustomGalleryView", "Error recargando carpeta", e)
            }
        }
    }
    
    // Modificar el botón de retroceso para manejar navegación de subcarpetas
    private fun handleBackNavigation() {
        if (currentSubfolder != null) {
            // Estamos en una subcarpeta, volver a la carpeta padre
            if (navigationStack.isNotEmpty()) {
                val (folder, subfolder) = navigationStack.removeLastOrNull() ?: return
                currentSubfolder = subfolder
                currentFolder = folder
                showPhotosView(folder)
            } else {
                currentSubfolder = null
                // Recargar carpeta padre para ver cambios
                currentFolder?.let { reloadCurrentFolder() } ?: showMainView()
            }
        } else if (isInFolderView) {
            // Estamos en vista de carpeta, volver a vista principal
            showMainView()
        } else {
            // Ya estamos en vista principal, invocar callback
            onBackPressed?.invoke()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}