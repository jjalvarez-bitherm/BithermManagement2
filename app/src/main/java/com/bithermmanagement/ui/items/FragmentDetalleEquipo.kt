package com.bithermmanagement.ui.items

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.R
import com.bithermmanagement.database.dao.InspeccionDao
import com.bithermmanagement.database.entities.Equipo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.AlertDialog
import org.json.JSONObject
import com.bithermmanagement.ui.navigation.DialogNavegacionTabs
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.os.Looper
import android.graphics.drawable.GradientDrawable
import com.bumptech.glide.Glide
import androidx.fragment.app.setFragmentResultListener
import java.io.File

@AndroidEntryPoint
class FragmentDetalleEquipo : Fragment() {

    @Inject
    lateinit var inspeccionDao: InspeccionDao

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var equipo: Equipo? = null

    // Views
    private lateinit var spinnerArea: MaterialAutoCompleteTextView
    private lateinit var spinnerUnidad: MaterialAutoCompleteTextView
    private lateinit var spinnerMarca: MaterialAutoCompleteTextView
    private lateinit var spinnerModelo: MaterialAutoCompleteTextView
    private lateinit var spinnerTipo: MaterialAutoCompleteTextView
    private lateinit var spinnerDiametro: MaterialAutoCompleteTextView
    private lateinit var spinnerConexion: MaterialAutoCompleteTextView
    private lateinit var spinnerInstalacion: MaterialAutoCompleteTextView
    private lateinit var spinnerFlota: MaterialAutoCompleteTextView
    private lateinit var spinnerAislamiento: MaterialAutoCompleteTextView
    private lateinit var spinnerPresEntrada: MaterialAutoCompleteTextView
    private lateinit var spinnerPresSalida: MaterialAutoCompleteTextView
    private lateinit var spinnerDescarga: MaterialAutoCompleteTextView
    private lateinit var spinnerAplicacion: MaterialAutoCompleteTextView
    private lateinit var spinnerServicio: MaterialAutoCompleteTextView
    private lateinit var etUbicacion: TextInputEditText
    private lateinit var tvGPS: TextView
    private lateinit var btnActualizarGPS: ImageButton
    private lateinit var btnNavegar: ImageButton
    private lateinit var etNota: TextInputEditText
    private lateinit var etIncidencias: TextInputEditText
    private lateinit var etLinea: TextInputEditText
    private lateinit var imageEquipo: ImageView
    private lateinit var tvIdEquipo: TextView
    private lateinit var autoEstado: MaterialAutoCompleteTextView

    private var gpsRequestInProgress = false
    private var gpsTimeoutHandler: Handler? = null
    private var gpsTimeoutRunnable: Runnable? = null
    private var gpsDialog: AlertDialog? = null
    private var gpsDialogView: View? = null
    private var gpsLocationCallback: com.google.android.gms.location.LocationCallback? = null
    private var gpsLocationRequest: com.google.android.gms.location.LocationRequest? = null
    private var gpsBestLocation: android.location.Location? = null
    private var gpsBestPrecision: Float = Float.MAX_VALUE
    private var gpsMaxWaitMillis: Long = 5000
    private var gpsStartTime: Long = 0L
    private var coloresEstados: Map<String, Int> = emptyMap()

    private val valoresInstalacionPorDefecto = listOf("MANIFOLD VAPOR", "MANIFOLD CONDENSADO", "EN LINEA", "PIANILLO")
    private val valoresAislamientoPorDefecto = listOf("V/V ANTEIOR Y POST", "V/V ANTERIOR", "TVS", "SIN AISLAMIENTO")

    // Variables para la navegación de purgadores
    private var listaEquipos: List<Equipo> = emptyList()
    private var indiceActual: Int = 0

    companion object {
        private const val ARG_LISTA_EQUIPOS = "lista_equipos"
        private const val ARG_INDICE = "indice"

        fun newInstance(lista: List<Equipo>, indice: Int): FragmentDetalleEquipo {
            val fragment = FragmentDetalleEquipo()
            val args = Bundle().apply {
                putParcelableArrayList(ARG_LISTA_EQUIPOS, ArrayList(lista))
                putInt(ARG_INDICE, indice)
            }
            fragment.arguments = args
            Log.d("FragmentDetalleEquipo", "newInstance: Recibida lista de ${lista.size} equipos, índice inicial: $indice")
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        arguments?.let { args ->
            listaEquipos = args.getParcelableArrayList(ARG_LISTA_EQUIPOS) ?: emptyList()
            indiceActual = args.getInt(ARG_INDICE, 0)
        }
        
        Log.d("FragmentDetalleEquipo", "onCreate: Lista recibida con ${listaEquipos.size} equipos, índice actual: $indiceActual")
        
        if (listaEquipos.isNotEmpty() && indiceActual < listaEquipos.size) {
            equipo = listaEquipos[indiceActual]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FragmentDetalleEquipo", "onCreateView")
        return inflater.inflate(R.layout.fragment_detalle_equipo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragmentDetalleEquipo", "onViewCreated")
        
        inicializarViews()
        configurarGPS()
        cargarColoresEstados()
        
        // Configurar listener para navegación GPS
        btnNavegar.setOnClickListener {
            Log.d("FragmentDetalleEquipo", "CLICK en btnNavegar - iniciando navegación GPS")
            equipo?.let { eq ->
                val gpsCoord = eq.gpsCoord
                
                if (!gpsCoord.isNullOrEmpty()) {
                    val coords = gpsCoord.split(",")
                    if (coords.size == 2) {
                        val latDestino = coords[0].trim().toDoubleOrNull() ?: 0.0
                        val lonDestino = coords[1].trim().toDoubleOrNull() ?: 0.0
                        
                        if (latDestino != 0.0 && lonDestino != 0.0) {
                            Log.d("FragmentDetalleEquipo", "NAVEGANDO a Google Maps: lat=$latDestino, lon=$lonDestino")
                            val gmmIntentUri = Uri.parse("geo:$latDestino,$lonDestino?q=$latDestino,$lonDestino")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            
                            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                                startActivity(mapIntent)
                            } else {
                                // Si no hay Google Maps, abrir con cualquier app de mapas
                                startActivity(mapIntent)
                            }
                        }
                    }
                }
            }
        }
        
        // Configurar listener para actualizar GPS
        btnActualizarGPS.setOnClickListener {
            solicitarActualizacionGPS()
        }
        
        // Configurar listener para imagen del equipo
        imageEquipo.setOnClickListener {
            Log.d("FragmentDetalleEquipo", "CLICK en imageEquipo - abrir visor/carrusel")
            equipo?.let { eq ->
                val area = eq.area ?: ""
                val unidad = eq.unidad ?: ""
                val idEquipo = eq.id
                
                // Obtener lista de imágenes del equipo
                val listaImagenes = obtenerListaImagenesEquipo(idEquipo)
                val indiceInicial = 0 // Empezar con la primera imagen
                
                Log.d("FragmentDetalleEquipo", "NAVEGANDO a VisorImagenesFragment con ${listaImagenes.size} imágenes")
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, VisorImagenesFragment.newInstance(listaImagenes, indiceInicial, area, unidad, idEquipo))
                    .addToBackStack(null)
                    .commit()
            }
        }
        
        // Configurar listener para cambios en spinners
        configurarListenersSpinners()
        
        // Configurar listener para fragment result
        setFragmentResultListener("ruta_actualizada") { _, bundle ->
            val nuevaRuta = bundle.getString("ruta", "")
            Log.d("FragmentDetalleEquipo", "Recibido setFragmentResultListener con ruta: $nuevaRuta")
            
            if (nuevaRuta.isNotEmpty()) {
                val archivo = File(nuevaRuta)
                val existe = archivo.exists()
                Log.d("FragmentDetalleEquipo", "¿Existe el archivo? $existe")
                
                if (existe) {
                    Log.d("FragmentDetalleEquipo", "Imagen cargada en imageEquipo")
                    Glide.with(this)
                        .load(archivo)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imageEquipo)
                } else {
                    Log.e("FragmentDetalleEquipo", "El archivo de la foto principal no existe: $nuevaRuta")
                }
            } else {
                Log.e("FragmentDetalleEquipo", "Ruta de foto principal vacía o nula")
            }
        }
    }

    private fun inicializarViews() {
        Log.d("FragmentDetalleEquipo", "Inicializando views")
        
        // Inicializar spinners
        spinnerArea = requireView().findViewById(R.id.spinnerArea)
        spinnerUnidad = requireView().findViewById(R.id.spinnerUnidad)
        spinnerMarca = requireView().findViewById(R.id.spinnerMarca)
        spinnerModelo = requireView().findViewById(R.id.spinnerModelo)
        spinnerTipo = requireView().findViewById(R.id.spinnerTipo)
        spinnerDiametro = requireView().findViewById(R.id.spinnerDiametro)
        spinnerConexion = requireView().findViewById(R.id.spinnerConexion)
        spinnerInstalacion = requireView().findViewById(R.id.spinnerInstalacion)
        spinnerFlota = requireView().findViewById(R.id.spinnerFlota)
        spinnerAislamiento = requireView().findViewById(R.id.spinnerAislamiento)
        spinnerPresEntrada = requireView().findViewById(R.id.spinnerPIN)
        spinnerPresSalida = requireView().findViewById(R.id.spinnerPOUT)
        spinnerDescarga = requireView().findViewById(R.id.spinnerDescarga)
        spinnerAplicacion = requireView().findViewById(R.id.spinnerAplicacion)
        spinnerServicio = requireView().findViewById(R.id.spinnerServicio)
        
        // Inicializar otros views
        etUbicacion = requireView().findViewById(R.id.etUbicacion)
        tvGPS = requireView().findViewById(R.id.tvGpsCoord)
        btnActualizarGPS = requireView().findViewById(R.id.btnGpsTag)
        btnNavegar = requireView().findViewById(R.id.btnWalk)
        etNota = requireView().findViewById(R.id.etObservaciones)
        etIncidencias = requireView().findViewById(R.id.etIncidencias)
        etLinea = requireView().findViewById(R.id.etLinea)
        imageEquipo = requireView().findViewById(R.id.tvIdEquipoFixed) // Usar el TextView como placeholder
        tvIdEquipo = requireView().findViewById(R.id.tvIdEquipo)
        autoEstado = requireView().findViewById(R.id.spinnerEstado)
        
        // Configurar spinners y poblar datos del equipo
        lifecycleScope.launch {
        configurarSpinners()
        poblarDatosEquipo()
        }
    }

    private suspend fun configurarSpinners() {
        Log.d("FragmentDetalleEquipo", "Configurando spinners (inicio)")
        
        try {
            // Cargar datos desde la BD (igual que FragmentInspeccionEquipo)
            val areas = inspeccionDao.getAreasUnicas().ifEmpty { listOf("PQ", "PT", "PS", "PU", "PV", "PW", "PX", "PY", "PZ") }
            val unidades = inspeccionDao.getUnidadesUnicas().ifEmpty { listOf("U1", "U2", "U3", "U4", "U5") }
            val marcas = inspeccionDao.getMarcasUnicas().ifEmpty { listOf("SPIRAX SARCO", "ARMSTRONG", "GESTRA", "OTROS") }
            val modelos = inspeccionDao.getModelosUnicos().ifEmpty { listOf("Modelo 1", "Modelo 2", "Modelo 3") }
            val tipos = inspeccionDao.getTiposUnicos().ifEmpty { listOf("Tipo 1", "Tipo 2", "Tipo 3") }
            val diametros = inspeccionDao.getDiametrosUnicos().ifEmpty { listOf("DN15", "DN20", "DN25", "DN32", "DN40", "DN50") }
            val conexiones = inspeccionDao.getConexionesUnicas().ifEmpty { listOf("Rosca", "Brida", "Soldadura") }
            val aplicaciones = inspeccionDao.getAplicacionesUnicas().ifEmpty { listOf("VAPOR", "CONDENSADO", "AIRE", "AGUA") }
            val servicios = inspeccionDao.getServiciosUnicos().ifEmpty { listOf("Vapor", "Agua", "Aire") }
            
            // Cargar estados de inspección desde colores_estados.json
            val estadosInspeccion = coloresEstados.keys.toList()
            Log.d("FragmentDetalleEquipo", "Estados de inspección cargados: $estadosInspeccion")
            
            // Configurar adapters
            spinnerArea.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, areas))
            spinnerUnidad.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, unidades))
            spinnerMarca.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, marcas))
            spinnerModelo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, modelos))
            spinnerTipo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos))
            spinnerDiametro.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, diametros))
            spinnerConexion.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, conexiones))
            spinnerInstalacion.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, valoresInstalacionPorDefecto))
            
            // Configurar spinner de FLOTA con valores fijos
            val valoresFlota = listOf("ACTIVO", "MONITORIZADO", "AFS", "ELIMINADO")
            spinnerFlota.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, valoresFlota))
            Log.d("FragmentDetalleEquipo", "Spinner FLOTA configurado con valores: $valoresFlota")
            
            spinnerAislamiento.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, valoresAislamientoPorDefecto))
            spinnerPresEntrada.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")))
            spinnerPresSalida.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("0", "1", "2", "3", "4", "5")))
            spinnerDescarga.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listOf("CONTINUA", "INTERMITENTE", "NULA")))
            spinnerAplicacion.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, aplicaciones))
            spinnerServicio.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, servicios))
            // Usar estados de inspección del JSON en lugar de valores hardcodeados
            autoEstado.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadosInspeccion))
            
            Log.d("FragmentDetalleEquipo", "Spinners configurados correctamente")
            
        } catch (e: Exception) {
            Log.e("FragmentDetalleEquipo", "Error configurando spinners: ${e.message}", e)
        }
    }

    private fun configurarListenersSpinners() {
        // Configurar listeners para spinners dependientes
        spinnerMarca.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                actualizarModelosPorMarca(s.toString())
            }
        })
    }

    private fun actualizarModelosPorMarca(marca: String) {
        Log.d("FragmentDetalleEquipo", "Actualizando modelos para marca: $marca")
        
        val modelos = when (marca.uppercase()) {
            "MARCA 1" -> listOf("MODELO A1", "MODELO A2", "MODELO A3")
            "MARCA 2" -> listOf("MODELO B1", "MODELO B2", "MODELO B3")
            "MARCA 3" -> listOf("MODELO C1", "MODELO C2", "MODELO C3")
            "MARCA 4" -> listOf("MODELO D1", "MODELO D2", "MODELO D3")
            else -> listOf("MODELO A", "MODELO B", "MODELO C", "MODELO D")
        }
        
        spinnerModelo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, modelos))
    }

    private fun poblarDatosEquipo() {
        Log.d("FragmentDetalleEquipo", "poblarDatosEquipo: equipo es ${if (equipo == null) "NULL" else "NO NULL"}")
        equipo?.let { eq ->
            Log.d("FragmentDetalleEquipo", "Datos del equipo: id=${eq.id}, area=${eq.area}, unidad=${eq.unidad}, ubicacion=${eq.ubicacion}, estado=${eq.estado}")
            tvIdEquipo.text = eq.id
            
            // Poblar spinners con datos del equipo
            spinnerArea.setText(eq.area ?: "")
            spinnerUnidad.setText(eq.unidad ?: "")
            spinnerMarca.setText(eq.marca ?: "")
            spinnerModelo.setText(eq.modelo ?: "")
            spinnerTipo.setText(eq.tipo ?: "")
            spinnerDiametro.setText(eq.diametro ?: "")
            spinnerConexion.setText(eq.conexion ?: "")
            spinnerInstalacion.setText(eq.instalacion ?: "")
            spinnerFlota.setText(eq.flota ?: "")
            spinnerAislamiento.setText(eq.aislamiento ?: "")
            spinnerPresEntrada.setText(eq.presEntrada ?: "")
            spinnerPresSalida.setText(eq.presSalida ?: "")
            spinnerDescarga.setText(eq.descarga ?: "")
            spinnerAplicacion.setText(eq.aplicacion ?: "")
            spinnerServicio.setText(eq.servicio ?: "")
            autoEstado.setText(eq.estado ?: "")
            
            // Poblar otros campos
            etUbicacion.setText(eq.ubicacion ?: "")
            etNota.setText(eq.nota ?: "")
            etIncidencias.setText(eq.incidencias ?: "")
            etLinea.setText(eq.linea ?: "")
            
            Log.d("FragmentDetalleEquipo", "Datos poblados: unidad=${spinnerUnidad.text}, ubicacion=${etUbicacion.text}")
            
            // Configurar GPS
            if (!eq.gpsCoord.isNullOrEmpty()) {
                tvGPS.text = "GPS: ${eq.gpsCoord}"
            } else {
                tvGPS.text = "GPS: No disponible"
            }
            
            // Cargar imagen del equipo
            cargarImagenEquipo(eq.id)
            
            // Aplicar color de estado
            aplicarColorEstado(eq.estado)
        }
    }

    private fun cargarImagenEquipo(equipoId: String) {
        val rutaFotoPrincipal = obtenerRutaFotoPrincipal(equipoId)
        
        if (rutaFotoPrincipal.isNotEmpty()) {
            val archivo = File(rutaFotoPrincipal)
            if (archivo.exists()) {
                Glide.with(this)
                    .load(archivo)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(imageEquipo)
            } else {
                // Cargar imagen por defecto
                imageEquipo.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            // Cargar imagen por defecto
            imageEquipo.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun obtenerRutaFotoPrincipal(equipoId: String): String {
        // Implementar lógica para obtener la ruta de la foto principal
        // Por ahora retornar una ruta vacía
        return ""
    }

    private fun obtenerListaImagenesEquipo(equipoId: String): List<String> {
        // Implementar lógica para obtener lista de imágenes del equipo
        // Por ahora retornar lista vacía
        return emptyList()
    }

    private fun aplicarColorEstado(estado: String?) {
        estado?.let { est ->
            val color = coloresEstados[est.uppercase()]
            if (color != null) {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8f
                    setColor(color)
                }
                autoEstado.background = drawable
            }
        }
    }

    private fun configurarGPS() {
        Log.d("FragmentDetalleEquipo", "Configurando GPS")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        Log.d("FragmentDetalleEquipo", "Solicitando actualización de GPS")
    }

    private fun solicitarActualizacionGPS() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permisos
            return
        }
        
        // Implementar lógica de GPS
        Log.d("FragmentDetalleEquipo", "Solicitando actualización de GPS")
    }

    private fun cargarColoresEstados() {
        try {
            Log.d("FragmentDetalleEquipo", "Leyendo colores desde archivo dinámico")
            val archivoDinamico = File(requireContext().getExternalFilesDir(null), "estados_colores.json")
            
            val jsonString = if (archivoDinamico.exists()) {
                archivoDinamico.readText()
            } else {
                Log.d("FragmentDetalleEquipo", "Archivo dinámico no existe, usando assets")
                requireContext().assets.open("estados_colores.json").bufferedReader().use { it.readText() }
            }
            
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, Int>()
            
            jsonObject.keys().forEach { estado ->
                val colorHex = jsonObject.getString(estado)
                try {
                    val color = android.graphics.Color.parseColor(colorHex)
                    map[estado.uppercase()] = color
                    Log.d("FragmentDetalleEquipo", "Color cargado: $estado -> $colorHex")
                } catch (e: Exception) {
                    Log.e("FragmentDetalleEquipo", "Error parsing color for estado $estado: $colorHex", e)
                }
            }
            
            coloresEstados = map
            Log.d("FragmentDetalleEquipo", "Total colores cargados: ${map.size}")
            
        } catch (e: Exception) {
            Log.e("FragmentDetalleEquipo", "Error loading estados_colores.json", e)
        }
    }
} 