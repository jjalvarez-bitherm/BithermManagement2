package com.bithermmanagement.ui.items

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bithermmanagement.R
import com.bithermmanagement.utils.ImageUtils
import java.io.File
import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.Equipo
import kotlinx.coroutines.launch
import android.widget.ImageButton
import androidx.fragment.app.setFragmentResult
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class VisorImagenesFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var indicadorPaginas: LinearLayout
    private var imagenes: MutableList<String> = mutableListOf()
    private var indiceInicial: Int = 0
    private lateinit var adapter: VisorImagenesAdapter
    private var currentPhotoPath: String? = null
    private var areaActual: String? = null
    private var unidadActual: String? = null
    private var idEquipoActual: String? = null
    private lateinit var btnFotoPrincipal: ImageButton

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            tomarFoto()
        } else {
            Toast.makeText(
                requireContext(),
                "Se necesitan permisos de cámara y almacenamiento",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("VisorImagenesFragment", "Resultado de la cámara: resultCode=${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                Log.d("VisorImagenesFragment", "currentPhotoPath: $path")
                val file = File(path)
                if (file.exists()) {
                    Log.d("VisorImagenesFragment", "El archivo de la foto existe: $path")
                    // Si estamos sustituyendo una imagen
                    val posicionActual = viewPager.currentItem
                    if (posicionActual < imagenes.size) {
                        imagenes[posicionActual] = path
                        adapter.notifyItemChanged(posicionActual)
                        Log.d("VisorImagenesFragment", "Imagen sustituida en posición $posicionActual")
                    } else {
                        // Si estamos añadiendo una nueva imagen
                        imagenes.add(path)
                        adapter.notifyItemInserted(imagenes.size - 1)
                        viewPager.setCurrentItem(imagenes.size - 1, true)
                        Log.d("VisorImagenesFragment", "Imagen añadida en posición ${imagenes.size - 1}")
                    }
                } else {
                    Log.e("VisorImagenesFragment", "El archivo de la foto NO existe: $path")
                    Toast.makeText(requireContext(), "Error: La foto no se guardó correctamente", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("VisorImagenesFragment", "La captura de la foto fue cancelada o fallida")
            Toast.makeText(requireContext(), "La captura de la foto fue cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_IMAGENES = "imagenes"
        private const val ARG_INDICE = "indice"
        private const val ARG_AREA = "area"
        private const val ARG_UNIDAD = "unidad"
        private const val ARG_ID_EQUIPO = "id_equipo"
        fun newInstance(imagenes: List<String>, indice: Int = 0, area: String? = null, unidad: String? = null, idEquipo: String? = null): VisorImagenesFragment {
            val fragment = VisorImagenesFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_IMAGENES, ArrayList(imagenes))
            args.putInt(ARG_INDICE, indice)
            if (area != null) args.putString(ARG_AREA, area)
            if (unidad != null) args.putString(ARG_UNIDAD, unidad)
            if (idEquipo != null) args.putString(ARG_ID_EQUIPO, idEquipo)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagenes = it.getStringArrayList(ARG_IMAGENES)?.toMutableList() ?: mutableListOf()
            indiceInicial = it.getInt(ARG_INDICE, 0)
            areaActual = it.getString(ARG_AREA)
            unidadActual = it.getString(ARG_UNIDAD)
            idEquipoActual = it.getString(ARG_ID_EQUIPO)
        }
        // Recargar imágenes desde la carpeta si hay área e idEquipo
        if (areaActual != null && idEquipoActual != null) {
            imagenes = ImageUtils.getAllImagesForEquipo(requireContext(), areaActual!!, idEquipoActual!!).toMutableList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_visor_imagenes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPagerImagenes)
        indicadorPaginas = view.findViewById(R.id.indicadorPaginas)
        // Botón para marcar como foto principal
        btnFotoPrincipal = view.findViewById(R.id.btnFotoPrincipal)
        btnFotoPrincipal.setOnClickListener {
            marcarComoFotoPrincipal()
        }
        setupViewPager()
        setupIndicadorPaginas()
    }

    private fun setupViewPager() {
        adapter = VisorImagenesAdapter(
            imagenes = imagenes,
            onEliminarClick = { posicion ->
                eliminarImagen(posicion)
            },
            onSustituirClick = { posicion ->
                sustituirImagen(posicion)
            },
            onAnadirClick = {
                verificarPermisosYCapturar()
            },
            onHacerPrincipalClick = { posicion ->
                marcarComoFotoPrincipal(posicion)
            }
        )

        viewPager.adapter = adapter
        viewPager.setCurrentItem(indiceInicial, false)
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                actualizarIndicadorPaginas(position)
            }
        })
    }

    private fun setupIndicadorPaginas() {
        indicadorPaginas.removeAllViews()
        val totalPaginas = imagenes.size + 1 // +1 para la página de añadir
        
        for (i in 0 until totalPaginas) {
            val dot = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_dot)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.dot_margin)
                }
                layoutParams = params
            }
            indicadorPaginas.addView(dot)
        }
        
        actualizarIndicadorPaginas(viewPager.currentItem)
    }

    private fun actualizarIndicadorPaginas(posicionActual: Int) {
        for (i in 0 until indicadorPaginas.childCount) {
            val dot = indicadorPaginas.getChildAt(i) as ImageView
            if (i == posicionActual) {
                dot.setImageResource(R.drawable.ic_dot_selected)
            } else {
                dot.setImageResource(R.drawable.ic_dot)
            }
        }
    }

    private fun eliminarImagen(posicion: Int) {
        if (posicion < imagenes.size) {
            val imagenAEliminar = imagenes[posicion]
            val file = File(imagenAEliminar)
            if (file.exists()) {
                file.delete()
            }
            imagenes.removeAt(posicion)
            adapter.notifyItemRemoved(posicion)
            actualizarIndicadorPaginas(viewPager.currentItem)
            Toast.makeText(requireContext(), "Imagen eliminada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sustituirImagen(posicion: Int) {
        if (posicion < imagenes.size) {
            viewPager.setCurrentItem(posicion, true)
            verificarPermisosYCapturar()
        }
    }

    private fun verificarPermisosYCapturar() {
        val permisos = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val permisosNoConcedidos = permisos.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permisosNoConcedidos.isEmpty()) {
            tomarFoto()
        } else {
            requestPermissionLauncher.launch(permisosNoConcedidos.toTypedArray())
        }
    }

    private fun tomarFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        
        val photoURI = Uri.fromFile(photoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        
        takePictureLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val imageFileName = "IMG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun marcarComoFotoPrincipal(posicion: Int = viewPager.currentItem) {
        if (posicion < imagenes.size && posicion > 0) {
            val imagenPrincipal = imagenes[0]
            val imagenSeleccionada = imagenes[posicion]
            
            // Intercambiar posiciones
            imagenes[0] = imagenSeleccionada
            imagenes[posicion] = imagenPrincipal
            
            adapter.notifyDataSetChanged()
            viewPager.setCurrentItem(0, true)
            
            // Notificar al fragmento padre sobre el cambio de foto principal
            val bundle = Bundle()
            bundle.putString("ruta_foto", imagenSeleccionada)
            setFragmentResult("foto_principal_actualizada", bundle)
            
            Toast.makeText(requireContext(), "Foto principal actualizada", Toast.LENGTH_SHORT).show()
        }
    }
} 