package com.bithermmanagement.ui.items

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.R
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.EquipoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.multimedia.MiniGaleriaFragment
import androidx.fragment.app.commit
import android.util.Log
import com.bithermmanagement.database.dao.InspeccionDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.bithermmanagement.database.entities.Equipo
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.app.AlertDialog
import android.content.Context
import org.json.JSONObject

@AndroidEntryPoint
class FragmentInspeccionEquipo : Fragment(), MiniGaleriaFragment.OnFotoActualizadaListener {
    companion object {
        private const val ARG_EQUIPO_ID = "equipo_id"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        
        // Valores prefijados para spinners
        private val valoresInstalacionPorDefecto = listOf(
            "MANIFOLD VAPOR",
            "MANIFOLD CONDENSADO", 
            "EN LINEA",
            "PIANILLO"
        )
        
        private val valoresAislamientoPorDefecto = listOf(
            "V/V VAPOR",
            "V/V CONDENSADO",
            "V/V VAPOR/COND",
            "TVS 4V/V PISTÓN",
            "TVS (OTRAS)",
            "V/V VAPOR GRIPADA",
            "V/V COND. GRIPADA",
            "V/Vs GRIPADAS",
            "TVS GRIPADA",
            "TVS TRASPASADA"
        )
        
        fun newInstance(equipoId: String): FragmentInspeccionEquipo {
            val fragment = FragmentInspeccionEquipo()
            val args = Bundle()
            args.putString(ARG_EQUIPO_ID, equipoId)
            fragment.arguments = args
            return fragment
        }
    }
    
    @Inject
    lateinit var inspeccionDao: InspeccionDao

    private var equipos = mutableListOf<Equipo>()
    private var coloresEstados: Map<String, Int> = emptyMap()
    private var indexActual: Int = 0
    private var equipoSeleccionadoId: String? = null
    private var estadosUnicos: List<String> = emptyList()
    
    // Control de estado de cards
    private var cardAbiertoActual: String? = null // "datosGenerales", "observaciones", "caracteristicas"
    
    // Valores anteriores para detectar cambios en spinners dependientes
    private var areaAnterior: String = ""
    private var marcaAnterior: String = ""
    private var modeloAnterior: String = ""
    private var presionEntradaAnterior: String = ""
    // Views
    private lateinit var spinnerArea: Spinner
    private lateinit var spinnerUnidad: Spinner
    private lateinit var spinnerInstalacion: Spinner
    private lateinit var spinnerAislamiento: Spinner
    private lateinit var spinnerEstado: Spinner
    private lateinit var spinnerMarca: Spinner
    private lateinit var spinnerModelo: Spinner
    private lateinit var spinnerTipo: Spinner
    private lateinit var spinnerDiametro: Spinner
    private lateinit var spinnerConexion: Spinner
    private lateinit var spinnerPIN: Spinner
    private lateinit var spinnerPOUT: Spinner
    private lateinit var spinnerDescarga: Spinner
    private lateinit var spinnerAplicacion: Spinner
    private lateinit var spinnerServicio: Spinner
    private lateinit var etUbicacion: EditText
    private lateinit var etLinea: EditText
    private lateinit var etObservaciones: EditText
    private lateinit var etIncidencias: EditText
    private lateinit var tvGpsCoord: TextView
    private lateinit var tvGpsAcc: TextView
    private lateinit var tvIdEquipo: TextView
    private lateinit var tvIdEquipoFixed: TextView
    private lateinit var textView: TextView
    private lateinit var tvInspectorFecha: TextView
    private lateinit var btnGuardar: Button
    private lateinit var btnAnteriorEstado: ImageButton
    private lateinit var btnSiguienteEstado: ImageButton
    private lateinit var miniGaleriaFragment: MiniGaleriaFragment
    private lateinit var headerDatosGenerales: LinearLayout
    private lateinit var layoutDatosGenerales: LinearLayout
    private lateinit var headerObservaciones: LinearLayout
    private lateinit var layoutObservaciones: LinearLayout
    private lateinit var headerCaracteristicas: LinearLayout
    private lateinit var layoutCaracteristicas: LinearLayout
    private lateinit var iconCollapseDatosGenerales: ImageView
    private lateinit var iconCollapseObservaciones: ImageView
    private lateinit var iconCollapseCaracteristicas: ImageView
    private lateinit var btnGpsTag: ImageButton
    
    // Variables de control de GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var gpsRequestInProgress = false
    private var gpsTimeoutHandler: Handler? = null
    private var gpsTimeoutRunnable: Runnable? = null
    private var gpsDialog: AlertDialog? = null
    private var gpsDialogView: View? = null
    private var gpsLocationCallback: LocationCallback? = null
    private var gpsLocationRequest: LocationRequest? = null
    private var gpsBestLocation: Location? = null
    private var gpsBestPrecision: Float = Float.MAX_VALUE
    private var gpsMaxWaitMillis: Long = 5000
    private var gpsStartTime: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inspeccion_equipo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener el ID del equipo seleccionado
        equipoSeleccionadoId = arguments?.getString(ARG_EQUIPO_ID)
        Log.d("FragmentInspeccionEquipo", "Equipo seleccionado ID: $equipoSeleccionadoId")
        
        initializeViews(view)
        
        // Inicializar colapsables
        setupCollapsibleCards()
        
        // Navegación por flechas
        btnAnteriorEstado.setOnClickListener { mostrarEquipo(indexActual - 1) }
        btnSiguienteEstado.setOnClickListener { mostrarEquipo(indexActual + 1) }
        btnGuardar.setOnClickListener { guardarYAvanzar() }
        
        // Configurar GPS
        setupGPS()
        
        // Cargar colores de estados desde JSON
        coloresEstados = cargarColoresEstados()
        
        // Cargar datos y mostrar equipo seleccionado
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            equipos.clear()
            equipos.addAll(withContext(Dispatchers.IO) { db.equipoDao().getAllEquipos() })
            estadosUnicos = equipos.mapNotNull { it.estado }.distinct().sorted()
            
            Log.d("FragmentInspeccionEquipo", "=== VERIFICACIÓN DE DATOS ===")
            Log.d("FragmentInspeccionEquipo", "Total equipos cargados: ${equipos.size}")
            if (equipos.isNotEmpty()) {
                val primerEquipo = equipos[0]
                Log.d("FragmentInspeccionEquipo", "Primer equipo: ID=${primerEquipo.id}")
                Log.d("FragmentInspeccionEquipo", "Primer equipo: Area=${primerEquipo.area}, Unidad=${primerEquipo.unidad}")
                Log.d("FragmentInspeccionEquipo", "Primer equipo: Marca=${primerEquipo.marca}, Modelo=${primerEquipo.modelo}")
                Log.d("FragmentInspeccionEquipo", "Primer equipo: Estado=${primerEquipo.estado}")
                Log.d("FragmentInspeccionEquipo", "Primer equipo: Ubicacion=${primerEquipo.ubicacion}")
            } else {
                Log.e("FragmentInspeccionEquipo", "⚠️ NO HAY EQUIPOS EN LA BASE DE DATOS")
            }
            Log.d("FragmentInspeccionEquipo", "Estados únicos: $estadosUnicos")
            Log.d("FragmentInspeccionEquipo", "=== FIN VERIFICACIÓN ===")
            
            // Configurar spinners primero
            setupSpinners()
            
            // Buscar y mostrar el equipo seleccionado
            if (equipoSeleccionadoId != null) {
                val indiceEquipo = equipos.indexOfFirst { it.id == equipoSeleccionadoId }
                if (indiceEquipo >= 0) {
                    Log.d("FragmentInspeccionEquipo", "Mostrando equipo seleccionado en índice: $indiceEquipo")
                    mostrarEquipo(indiceEquipo)
                } else {
                    Log.e("FragmentInspeccionEquipo", "No se encontró el equipo: $equipoSeleccionadoId")
                    if (equipos.isNotEmpty()) mostrarEquipo(0)
                }
            } else {
                Log.d("FragmentInspeccionEquipo", "No hay equipo seleccionado, mostrando el primero")
                if (equipos.isNotEmpty()) mostrarEquipo(0)
            }
        }
    }

    private fun initializeViews(view: View) {
        Log.d("FragmentInspeccionEquipo", "Inicializando views")
        
        // Spinners de DATOS GENERALES
        spinnerArea = view.findViewById(R.id.spinnerArea)
        spinnerUnidad = view.findViewById(R.id.spinnerUnidad)
        spinnerInstalacion = view.findViewById(R.id.spinnerInstalacion)
        spinnerAislamiento = view.findViewById(R.id.spinnerAislamiento)
        spinnerEstado = view.findViewById(R.id.spinnerEstado)
        
        // Spinners de CARACTERÍSTICAS TÉCNICAS
        spinnerMarca = view.findViewById(R.id.spinnerMarca)
        spinnerModelo = view.findViewById(R.id.spinnerModelo)
        spinnerTipo = view.findViewById(R.id.spinnerTipo)
        spinnerDiametro = view.findViewById(R.id.spinnerDiametro)
        spinnerConexion = view.findViewById(R.id.spinnerConexion)
        spinnerPIN = view.findViewById(R.id.spinnerPIN)
        spinnerPOUT = view.findViewById(R.id.spinnerPOUT)
        spinnerDescarga = view.findViewById(R.id.spinnerDescarga)
        spinnerAplicacion = view.findViewById(R.id.spinnerAplicacion)
        spinnerServicio = view.findViewById(R.id.spinnerServicio)
        
        // EditTexts
        etUbicacion = view.findViewById(R.id.etUbicacion)
        etLinea = view.findViewById(R.id.etLinea)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        etIncidencias = view.findViewById(R.id.etIncidencias)
        
        // TextViews
        tvGpsCoord = view.findViewById(R.id.tvGpsCoord)
        tvGpsAcc = view.findViewById(R.id.tvGpsAcc)
        tvIdEquipo = view.findViewById(R.id.tvIdEquipo)
        tvIdEquipoFixed = view.findViewById(R.id.tvIdEquipoFixed)
        textView = view.findViewById(R.id.textView)
        tvInspectorFecha = view.findViewById(R.id.tvInspectorFecha)
        
        // Botones
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnAnteriorEstado = view.findViewById(R.id.btnAnteriorEstado)
        btnSiguienteEstado = view.findViewById(R.id.btnSiguienteEstado)
        btnGpsTag = view.findViewById(R.id.btnGpsTag)
        
        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Cards
        headerDatosGenerales = view.findViewById(R.id.headerDatosGenerales)
        layoutDatosGenerales = view.findViewById(R.id.layoutDatosGenerales)
        headerObservaciones = view.findViewById(R.id.headerObservaciones)
        layoutObservaciones = view.findViewById(R.id.layoutObservaciones)
        headerCaracteristicas = view.findViewById(R.id.headerCaracteristicas)
        layoutCaracteristicas = view.findViewById(R.id.layoutCaracteristicas)
        iconCollapseDatosGenerales = view.findViewById(R.id.iconCollapseDatosGenerales)
        iconCollapseObservaciones = view.findViewById(R.id.iconCollapseObservaciones)
        iconCollapseCaracteristicas = view.findViewById(R.id.iconCollapseCaracteristicas)
        
        // Mini galería
        miniGaleriaFragment = childFragmentManager.findFragmentById(R.id.miniGaleriaFragment) as MiniGaleriaFragment
        miniGaleriaFragment.setOnFotoActualizadaListener(this)
        
        Log.d("FragmentInspeccionEquipo", "Views inicializados correctamente")
    }

    private suspend fun setupSpinners() {
        Log.d("FragmentInspeccionEquipo", "Configurando spinners...")
        
        try {
            // Configurar spinners de DATOS GENERALES
            val areas = inspeccionDao.getAreasUnicas().ifEmpty { listOf("PQ", "PT", "PS", "PU", "PV", "PW", "PX", "PY", "PZ") }
            val areaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, areas)
            areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerArea.adapter = areaAdapter
            
            val unidades = inspeccionDao.getUnidadesUnicas().ifEmpty { listOf("U1", "U2", "U3", "U4", "U5") }
            val unidadAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, unidades)
            unidadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerUnidad.adapter = unidadAdapter
            
            val estados = inspeccionDao.getEstadosUnicos().ifEmpty { 
                listOf("OPERATIVO", "MANTENIMIENTO", "FUERA DE SERVICIO", "REPARACIÓN") 
            }
            
            // Cargar colores primero para poder ordenar
            coloresEstados = cargarColoresEstados()
            
            // Ordenar estados según el orden del JSON (mantener orden original)
            val estadosOrdenados = estados.sortedWith { estado1, estado2 ->
                // Obtener las claves del JSON en el orden original
                val clavesJson = coloresEstados.keys.toList()
                
                // Buscar la posición de cada estado en el JSON
                val posicion1 = clavesJson.indexOf(estado1.uppercase().trim())
                val posicion2 = clavesJson.indexOf(estado2.uppercase().trim())
                
                // Si ambos están en el JSON, ordenar por su posición
                if (posicion1 != -1 && posicion2 != -1) {
                    posicion1.compareTo(posicion2)
                } else if (posicion1 != -1) {
                    -1 // estado1 está en JSON, va primero
                } else if (posicion2 != -1) {
                    1 // estado2 está en JSON, va primero
                } else {
                    // Si ninguno está en JSON, ordenar alfabéticamente
                    estado1.compareTo(estado2)
                }
            }
            
            val estadoAdapter = EstadoSpinnerAdapter(requireContext(), estadosOrdenados)
            spinnerEstado.adapter = estadoAdapter
            
            // RECREAR DE CERO: Spinner de INSTALACIÓN
            Log.d("SpinnerDebug", "=== RECREANDO SPINNER INSTALACIÓN ===")
            val instalaciones = valoresInstalacionPorDefecto
            Log.d("SpinnerDebug", "Valores instalación: $instalaciones")
            val instalacionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, instalaciones)
            instalacionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerInstalacion.adapter = instalacionAdapter
            Log.d("SpinnerDebug", "Adapter instalación configurado con ${instalaciones.size} elementos")
            
            // RECREAR DE CERO: Spinner de AISLAMIENTO
            Log.d("SpinnerDebug", "=== RECREANDO SPINNER AISLAMIENTO ===")
            val aislamientos = valoresAislamientoPorDefecto
            Log.d("SpinnerDebug", "Valores aislamiento: $aislamientos")
            val aislamientoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, aislamientos)
            aislamientoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAislamiento.adapter = aislamientoAdapter
            Log.d("SpinnerDebug", "Adapter aislamiento configurado con ${aislamientos.size} elementos")
            
            // Configurar listeners básicos para instalación y aislamiento
            configurarListenersBasicos()
            
            // Configurar spinners de CARACTERÍSTICAS TÉCNICAS
            val marcas = inspeccionDao.getMarcasUnicas().ifEmpty { listOf("SPIRAX SARCO", "ARMSTRONG", "GESTRA", "OTROS") }
            val marcaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, marcas)
            marcaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMarca.adapter = marcaAdapter
            
            val modelos = inspeccionDao.getModelosUnicos().ifEmpty { listOf("Modelo 1", "Modelo 2", "Modelo 3") }
            val modeloAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modelos)
            modeloAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerModelo.adapter = modeloAdapter
            
            val tipos = inspeccionDao.getTiposUnicos().ifEmpty { listOf("Tipo 1", "Tipo 2", "Tipo 3") }
            val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
            tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipo.adapter = tipoAdapter
            
            val diametros = inspeccionDao.getDiametrosUnicos().ifEmpty { listOf("DN15", "DN20", "DN25", "DN32", "DN40", "DN50") }
            val diametroAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, diametros)
            diametroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDiametro.adapter = diametroAdapter
            
            val conexiones = inspeccionDao.getConexionesUnicas().ifEmpty { listOf("Rosca", "Brida", "Soldadura") }
            val conexionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conexiones)
            conexionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerConexion.adapter = conexionAdapter
            
            val presionesEntrada = inspeccionDao.getPresionesEntradaUnicas().ifEmpty { listOf("1 bar", "2 bar", "3 bar", "4 bar", "5 bar") }
            val presEntradaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presionesEntrada)
            presEntradaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPIN.adapter = presEntradaAdapter
            
            val presionesSalida = inspeccionDao.getPresionesSalidaUnicas().ifEmpty { listOf("0.1 bar", "0.2 bar", "0.3 bar", "0.4 bar", "0.5 bar") }
            val presSalidaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presionesSalida)
            presSalidaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPOUT.adapter = presSalidaAdapter
            
            val descargas = inspeccionDao.getDescargasUnicas().ifEmpty { listOf("Continua", "Intermittente", "Pulsante") }
            val descargaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, descargas)
            descargaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDescarga.adapter = descargaAdapter
            
            val aplicaciones = inspeccionDao.getAplicacionesUnicas().ifEmpty { listOf("Calefacción", "Proceso", "Vapor") }
            val aplicacionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, aplicaciones)
            aplicacionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAplicacion.adapter = aplicacionAdapter
            
            val servicios = inspeccionDao.getServiciosUnicos().ifEmpty { listOf("Vapor", "Agua", "Aire") }
            val servicioAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, servicios)
            servicioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerServicio.adapter = servicioAdapter
            
            Log.d("FragmentInspeccionEquipo", "Spinners configurados correctamente")
            
            // Configurar listeners para spinners dependientes
            configurarSpinnersDependientes()
            
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "Error configurando spinners: ${e.message}", e)
        }
    }

    private fun configurarSpinnersDependientes() {
        // ÁREA → UNIDAD (solo cuando cambie el valor)
        spinnerArea.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val areaSeleccionada = parent?.getItemAtPosition(position)?.toString() ?: ""
                if (areaSeleccionada.isNotEmpty() && areaSeleccionada != areaAnterior) {
                    Log.d("SpinnerDebug", "Área cambiada de '$areaAnterior' a '$areaSeleccionada'")
                    areaAnterior = areaSeleccionada
                    actualizarUnidadesPorArea(areaSeleccionada)
                    // Autoabrir el dropdown de unidad solo cuando cambie el valor
                    spinnerUnidad.post {
                        spinnerUnidad.performClick()
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // MARCA → MODELO (solo cuando cambie el valor)
        spinnerMarca.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val marcaSeleccionada = parent?.getItemAtPosition(position)?.toString() ?: ""
                if (marcaSeleccionada.isNotEmpty() && marcaSeleccionada != marcaAnterior) {
                    Log.d("SpinnerDebug", "Marca cambiada de '$marcaAnterior' a '$marcaSeleccionada'")
                    marcaAnterior = marcaSeleccionada
                    actualizarModelosPorMarca(marcaSeleccionada)
                    // Autoabrir el dropdown de modelo solo cuando cambie el valor
                    spinnerModelo.post {
                        spinnerModelo.performClick()
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // MODELO → TIPO (solo cuando cambie el valor)
        spinnerModelo.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val modeloSeleccionado = parent?.getItemAtPosition(position)?.toString() ?: ""
                val marcaSeleccionada = spinnerMarca.selectedItem?.toString() ?: ""
                if (modeloSeleccionado.isNotEmpty() && marcaSeleccionada.isNotEmpty() && modeloSeleccionado != modeloAnterior) {
                    Log.d("SpinnerDebug", "Modelo cambiado de '$modeloAnterior' a '$modeloSeleccionado' para marca: $marcaSeleccionada")
                    modeloAnterior = modeloSeleccionado
                    actualizarTiposPorMarcaModelo(marcaSeleccionada, modeloSeleccionado)
                    // Autoabrir el dropdown de tipo solo cuando cambie el valor
                    spinnerTipo.post {
                        spinnerTipo.performClick()
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // PRESIÓN ENTRADA → PRESIÓN SALIDA (solo cuando cambie el valor)
        spinnerPIN.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val presionEntrada = parent?.getItemAtPosition(position)?.toString() ?: ""
                if (presionEntrada.isNotEmpty() && presionEntrada != presionEntradaAnterior) {
                    Log.d("SpinnerDebug", "Presión entrada cambiada de '$presionEntradaAnterior' a '$presionEntrada'")
                    presionEntradaAnterior = presionEntrada
                    actualizarPresionesSalidaPorEntrada(presionEntrada)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun mostrarEquipo(index: Int) {
        if (equipos.isEmpty() || index !in equipos.indices) return
        
        indexActual = index
        val equipo = equipos[index]
        Log.d("MOSTRAR_EQUIPO", "Mostrando equipo: $equipo")
        Log.d("MOSTRAR_EQUIPO", "Índice: $index, Total equipos: ${equipos.size}")
        Log.d("MOSTRAR_EQUIPO", "ID del equipo: ${equipo.id}")
        Log.d("MOSTRAR_EQUIPO", "Área: ${equipo.area}, Unidad: ${equipo.unidad}")
        
        // Llenar campos principales
        // Mostrar ID del equipo en el TAG fijo (parte superior no scrollable)
        tvIdEquipoFixed.text = equipo.id ?: ""
        tvIdEquipoFixed.setTypeface(null, android.graphics.Typeface.BOLD)
        
        // El tvIdEquipo dentro del scroll se mantiene oculto
        tvIdEquipo.visibility = View.GONE
        
        // Llenar spinners de DATOS GENERALES
        spinnerArea.setSelection(getSpinnerIndex(spinnerArea, equipo.area ?: ""))
        spinnerUnidad.setSelection(getSpinnerIndex(spinnerUnidad, equipo.unidad ?: ""))
        spinnerInstalacion.setSelection(getSpinnerIndex(spinnerInstalacion, equipo.instalacion ?: ""))
        spinnerAislamiento.setSelection(getSpinnerIndex(spinnerAislamiento, equipo.aislamiento ?: ""))
        spinnerEstado.setSelection(getSpinnerIndex(spinnerEstado, equipo.estado ?: ""))
        
        // Aplicar color dependiente del estado
        aplicarColorEstado(equipo.estado ?: "")
        
        // Actualizar valores anteriores para detectar cambios
        areaAnterior = equipo.area ?: ""
        marcaAnterior = equipo.marca ?: ""
        modeloAnterior = equipo.modelo ?: ""
        presionEntradaAnterior = equipo.presEntrada ?: ""
        
        // Llenar EditTexts de DATOS GENERALES
        etUbicacion.setText(equipo.ubicacion ?: "")
        etLinea.setText(equipo.linea ?: "")
        
        // Mostrar coordenadas GPS existentes (formato de 6 decimales)
        equipo.gpsCoord?.let { gpsCoord ->
            if (gpsCoord.isNotEmpty()) {
                try {
                    val coords = gpsCoord.split(",")
                    if (coords.size == 2) {
                        val lat = coords[0].trim().toDouble()
                        val lon = coords[1].trim().toDouble()
                        tvGpsCoord.text = "GPS: ${"%.6f".format(lat)}, ${"%.6f".format(lon)}"
                    } else {
                        tvGpsCoord.text = "GPS: $gpsCoord"
                    }
                } catch (e: Exception) {
                    tvGpsCoord.text = "GPS: $gpsCoord"
                }
            } else {
                tvGpsCoord.text = "GPS: --"
            }
        } ?: run {
            tvGpsCoord.text = "GPS: --"
        }
        
        // Mostrar precisión GPS
        equipo.gpsAcc?.let { gpsAcc ->
            if (gpsAcc.isNotEmpty()) {
                tvGpsAcc.text = "Precisión: ${gpsAcc}m"
            } else {
                tvGpsAcc.text = "Precisión: --"
            }
        } ?: run {
            tvGpsAcc.text = "Precisión: --"
        }
        
        // Llenar campos de CARACTERÍSTICAS TÉCNICAS
        spinnerMarca.setSelection(getSpinnerIndex(spinnerMarca, equipo.marca ?: ""))
        spinnerModelo.setSelection(getSpinnerIndex(spinnerModelo, equipo.modelo ?: ""))
        spinnerTipo.setSelection(getSpinnerIndex(spinnerTipo, equipo.tipo ?: ""))
        spinnerDiametro.setSelection(getSpinnerIndex(spinnerDiametro, equipo.diametro ?: ""))
        spinnerConexion.setSelection(getSpinnerIndex(spinnerConexion, equipo.conexion ?: ""))
        spinnerPIN.setSelection(getSpinnerIndex(spinnerPIN, equipo.presEntrada ?: ""))
        spinnerPOUT.setSelection(getSpinnerIndex(spinnerPOUT, equipo.presSalida ?: ""))
        spinnerDescarga.setSelection(getSpinnerIndex(spinnerDescarga, equipo.descarga ?: ""))
        spinnerAplicacion.setSelection(getSpinnerIndex(spinnerAplicacion, equipo.aplicacion ?: ""))
        spinnerServicio.setSelection(getSpinnerIndex(spinnerServicio, equipo.servicio ?: ""))
        
        // Llenar campos de OBSERVACIONES
        etObservaciones.setText(equipo.nota ?: "")
        etIncidencias.setText(equipo.incidencias ?: "")
        
        // Llenar TextView de características técnicas (formato: marca - modelo (diametro - conexion))
        val marca = equipo.marca ?: "-"
        val modelo = equipo.modelo ?: "-"
        val diametro = equipo.diametro ?: "-"
        val conexion = equipo.conexion ?: "-"
        textView.text = "$marca - $modelo ($diametro - $conexion)"
        
        // Llenar TextView de información del inspector (formato: fecha - inspector (detector))
        val inspector = equipo.identidadInspector ?: "-"
        val fecha = equipo.fechaInspeccion ?: "-"
        val detector = equipo.detectorUtilizado ?: "-"
        tvInspectorFecha.text = "$fecha - $inspector ($detector)"
        
        // Actualizar mini galería
        equipo.id?.let { equipoId ->
            miniGaleriaFragment.setEquipoId(equipoId)
            Log.d("MOSTRAR_EQUIPO", "Mini galería actualizada para equipo: $equipoId")
        }
        
        Log.d("MOSTRAR_EQUIPO", "Campos llenados correctamente")
    }
    
    private fun getSpinnerIndex(spinner: Spinner, value: String): Int {
        if (value.isEmpty()) return 0
        val adapter = spinner.adapter
        if (adapter == null) {
            Log.w("getSpinnerIndex", "Adapter es null para spinner: ${spinner.id}")
            return 0
        }
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                return i
            }
        }
        return 0
    }

    // Funciones para spinners dependientes
    private fun actualizarUnidadesPorArea(area: String) {
        lifecycleScope.launch {
            try {
                val unidades = inspeccionDao.getUnidadesPorArea(area)
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, unidades)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerUnidad.adapter = adapter
                Log.d("SpinnerDebug", "Unidades actualizadas para área '$area': $unidades")
            } catch (e: Exception) {
                Log.e("SpinnerDebug", "Error al actualizar unidades: ${e.message}", e)
            }
        }
    }

    private fun actualizarModelosPorMarca(marca: String) {
        lifecycleScope.launch {
            try {
                val modelos = inspeccionDao.getModelosUnicosPorMarcas(listOf(marca))
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modelos)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerModelo.adapter = adapter
                // Limpiar selección actual
                spinnerModelo.setSelection(0)
                // Actualizar tipos por marca y modelo
                actualizarTiposPorMarcaModelo(marca, "")
                Log.d("SpinnerDebug", "Modelos actualizados para marca '$marca': $modelos")
            } catch (e: Exception) {
                Log.e("SpinnerDebug", "Error al actualizar modelos: ${e.message}", e)
            }
        }
    }

    private fun actualizarTiposPorMarcaModelo(marca: String, modelo: String) {
        lifecycleScope.launch {
            try {
                val tipos = if (marca.isNotEmpty() && modelo.isNotEmpty()) {
                    inspeccionDao.getTiposUnicosPorMarcaModelo(marca, modelo)
                } else {
                    inspeccionDao.getTiposUnicos()
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerTipo.adapter = adapter
                // Limpiar selección actual
                spinnerTipo.setSelection(0)
                Log.d("SpinnerDebug", "Tipos actualizados para marca '$marca' y modelo '$modelo': $tipos")
            } catch (e: Exception) {
                Log.e("SpinnerDebug", "Error al actualizar tipos: ${e.message}", e)
            }
        }
    }

    private fun actualizarPresionesSalidaPorEntrada(presionEntrada: String) {
        lifecycleScope.launch {
            try {
                val presionesSalida = if (presionEntrada.isNotEmpty()) {
                    // Obtener presiones de salida menores que la de entrada
                    inspeccionDao.getPresionesSalidaMenoresQue(presionEntrada)
                } else {
                    inspeccionDao.getPresionesSalidaUnicas()
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, presionesSalida)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPOUT.adapter = adapter
                // Limpiar selección actual
                spinnerPOUT.setSelection(0)
                Log.d("SpinnerDebug", "Presiones de salida actualizadas para entrada '$presionEntrada': $presionesSalida")
            } catch (e: Exception) {
                Log.e("SpinnerDebug", "Error al actualizar presiones de salida: ${e.message}", e)
            }
        }
    }

    private fun guardarYAvanzar() {
        // TODO: Guardar cambios del equipo actual
        mostrarEquipo(indexActual + 1)
    }

    private fun setupCollapsibleCards() {
        // Configurar cards colapsibles con toggle
        headerDatosGenerales.setOnClickListener {
            toggleCard("datosGenerales", layoutDatosGenerales, iconCollapseDatosGenerales)
        }
        
        headerObservaciones.setOnClickListener {
            toggleCard("observaciones", layoutObservaciones, iconCollapseObservaciones)
        }
        
        headerCaracteristicas.setOnClickListener {
            toggleCard("caracteristicas", layoutCaracteristicas, iconCollapseCaracteristicas)
        }
        
        // Inicializar estado de cards (todos cerrados por defecto)
        if (cardAbiertoActual == null) {
            // Si no hay card abierto, cerrar todos
            layoutDatosGenerales.visibility = View.GONE
            layoutObservaciones.visibility = View.GONE
            layoutCaracteristicas.visibility = View.GONE
            iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
            iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
            iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_more)
        } else {
            // Restaurar el estado del card que estaba abierto
            restaurarEstadoCards()
        }
    }
    
    private fun toggleCard(cardId: String, layout: LinearLayout, icon: ImageView) {
        if (cardAbiertoActual == cardId) {
            // Cerrar el card actual
            layout.visibility = View.GONE
            icon.setImageResource(R.drawable.ic_expand_more)
            cardAbiertoActual = null
            Log.d("CardsDebug", "Card '$cardId' cerrado")
        } else {
            // Cerrar todos los cards y abrir el seleccionado
            layoutDatosGenerales.visibility = View.GONE
            layoutObservaciones.visibility = View.GONE
            layoutCaracteristicas.visibility = View.GONE
            iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
            iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
            iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_more)
            
            // Abrir el card seleccionado
            layout.visibility = View.VISIBLE
            icon.setImageResource(R.drawable.ic_expand_less)
            cardAbiertoActual = cardId
            Log.d("CardsDebug", "Card '$cardId' abierto")
        }
    }
    
    private fun restaurarEstadoCards() {
        when (cardAbiertoActual) {
            "datosGenerales" -> {
                layoutDatosGenerales.visibility = View.VISIBLE
                layoutObservaciones.visibility = View.GONE
                layoutCaracteristicas.visibility = View.GONE
                iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_less)
                iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
                iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_more)
            }
            "observaciones" -> {
                layoutDatosGenerales.visibility = View.GONE
                layoutObservaciones.visibility = View.VISIBLE
                layoutCaracteristicas.visibility = View.GONE
                iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
                iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_less)
                iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_more)
            }
            "caracteristicas" -> {
                layoutDatosGenerales.visibility = View.GONE
                layoutObservaciones.visibility = View.GONE
                layoutCaracteristicas.visibility = View.VISIBLE
                iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
                iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
                iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_less)
            }
        }
        Log.d("CardsDebug", "Estado de cards restaurado: $cardAbiertoActual")
    }
    
    // ==================== FUNCIONES DE GPS ====================
    
    private fun setupGPS() {
        Log.d("FragmentInspeccionEquipo", "Configurando GPS")
        btnGpsTag.setOnClickListener {
            Log.d("FragmentInspeccionEquipo", "Solicitando actualización de GPS")
            if (!gpsRequestInProgress) {
                mostrarDialogoGPS()
            }
        }
    }

    private fun mostrarDialogoGPS() {
        gpsRequestInProgress = true
        gpsMaxWaitMillis = 5000
        gpsBestLocation = null
        gpsBestPrecision = Float.MAX_VALUE
        gpsStartTime = System.currentTimeMillis()
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Captura de GPS")
        val dialogView = layoutInflater.inflate(R.layout.dialog_gps_capture, null)
        gpsDialogView = dialogView
        val tvEstado = dialogView.findViewById<TextView>(R.id.tvEstadoGPS)
        val tvPrecision = dialogView.findViewById<TextView>(R.id.tvPrecisionGPS)
        tvEstado.text = "Obteniendo ubicación..."
        tvPrecision.text = "Precisión: -- m"
        builder.setView(dialogView)
        builder.setCancelable(false)
        builder.setPositiveButton("Aceptar esta precisión", null)
        builder.setNeutralButton("Esperar más", null)
        builder.setNegativeButton("Cancelar") { _, _ -> cancelarCapturaGPS() }
        gpsDialog = builder.create()
        gpsDialog?.setOnShowListener {
            gpsDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            gpsDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = false
        }
        gpsDialog?.show()
        iniciarCapturaGPS(tvEstado, tvPrecision)
    }

    private fun iniciarCapturaGPS(tvEstado: TextView, tvPrecision: TextView) {
        val fusedLocationProvider = fusedLocationClient
        gpsLocationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 20
        }
        gpsLocationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Log.d("GPS", "onLocationResult llamado")
                val location = result.lastLocation
                if (location != null) {
                    val precision = location.accuracy
                    Log.d("GPS", "Lat: ${location.latitude}, Lon: ${location.longitude}, Precisión: $precision")
                    tvEstado.text = "Ubicación recibida"
                    tvPrecision.text = "Precisión: ${"%.1f".format(precision)} m"
                    if (precision < gpsBestPrecision) {
                        gpsBestPrecision = precision
                        gpsBestLocation = location
                    }
                    val elapsed = System.currentTimeMillis() - gpsStartTime
                    if (gpsBestPrecision <= 10f) {
                        finalizarDialogoGPS(true)
                        fusedLocationProvider.removeLocationUpdates(this)
                    } else if (elapsed >= 20000) { // 20 segundos
                        Log.d("GPS", "Timeout alcanzado (20s)")
                        finalizarDialogoGPS(false)
                        fusedLocationProvider.removeLocationUpdates(this)
                    }
                } else {
                    Log.d("GPS", "Location es null")
                }
            }
            override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
                Log.d("GPS", "onLocationAvailability: ${availability.isLocationAvailable}")
            }
        }
        if (checkLocationPermission()) {
            fusedLocationProvider.requestLocationUpdates(gpsLocationRequest!!, gpsLocationCallback!!, Looper.getMainLooper())
        }
        // Handler para timeout manual
        gpsTimeoutHandler = Handler(Looper.getMainLooper())
        gpsTimeoutRunnable = Runnable {
            if (gpsRequestInProgress) {
                Log.d("GPS", "Timeout handler (20s)")
                finalizarDialogoGPS(false)
                fusedLocationProvider.removeLocationUpdates(gpsLocationCallback!!)
            }
        }
        gpsTimeoutHandler?.postDelayed(gpsTimeoutRunnable!!, 20000)
    }

    private fun finalizarDialogoGPS(precisionOk: Boolean) {
        gpsRequestInProgress = false
        gpsTimeoutHandler?.removeCallbacks(gpsTimeoutRunnable!!)
        val tvEstado = gpsDialogView?.findViewById<TextView>(R.id.tvEstadoGPS)
        val tvPrecision = gpsDialogView?.findViewById<TextView>(R.id.tvPrecisionGPS)
        val precision = gpsBestPrecision
        val location = gpsBestLocation
        val btnPositivo = gpsDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNeutral = gpsDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)
        val btnNegativo = gpsDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)
        if (location == null) {
            tvEstado?.text = "No se pudo obtener ubicación."
            tvPrecision?.text = "Precisión: -- m"
            btnPositivo?.isEnabled = false
            btnNeutral?.isEnabled = false
            btnNegativo?.isEnabled = true
            return
        }
        tvEstado?.text = "Precisión obtenida: ${"%.1f".format(precision)} m"
        tvPrecision?.text = "Coordenadas: ${location.latitude}, ${location.longitude}"
        btnNegativo?.setOnClickListener { cancelarCapturaGPS() }
        if (precision <= 10f) {
            btnPositivo?.text = "Asignar"
            btnPositivo?.isEnabled = true
            btnNeutral?.isEnabled = false
            btnPositivo?.setOnClickListener {
                // Formatear coordenadas con 6 decimales para mostrar
                val latFormateada = "%.6f".format(location.latitude)
                val lonFormateada = "%.6f".format(location.longitude)
                tvGpsCoord.text = "GPS: $latFormateada, $lonFormateada"
                tvGpsAcc.text = "Precisión: ${"%.1f".format(precision)} m"
                
                // Guardar en el equipo actual (NO sobrescribir etUbicacion)
                if (indexActual in equipos.indices) {
                    equipos[indexActual].gpsCoord = "${location.latitude}, ${location.longitude}"
                    equipos[indexActual].gpsAcc = precision.toString()
                }
                
                gpsDialog?.dismiss()
                Log.d("GPS", "Coordenadas guardadas: ${location.latitude}, ${location.longitude}")
            }
        } else {
            btnPositivo?.text = "Aceptar esta precisión"
            btnPositivo?.isEnabled = true
            btnNeutral?.isEnabled = true
            btnPositivo?.setOnClickListener {
                // Formatear coordenadas con 6 decimales para mostrar
                val latFormateada = "%.6f".format(location.latitude)
                val lonFormateada = "%.6f".format(location.longitude)
                tvGpsCoord.text = "GPS: $latFormateada, $lonFormateada"
                tvGpsAcc.text = "Precisión: ${"%.1f".format(precision)} m"
                
                // Guardar en el equipo actual (NO sobrescribir etUbicacion)
                if (indexActual in equipos.indices) {
                    equipos[indexActual].gpsCoord = "${location.latitude}, ${location.longitude}"
                    equipos[indexActual].gpsAcc = precision.toString()
                }
                
                gpsDialog?.dismiss()
                Log.d("GPS", "Coordenadas guardadas con baja precisión: ${location.latitude}, ${location.longitude}")
            }
            btnNeutral?.setOnClickListener {
                gpsDialog?.dismiss()
                mostrarDialogoGPS()
            }
        }
    }

    private fun cancelarCapturaGPS() {
        gpsRequestInProgress = false
        gpsTimeoutHandler?.removeCallbacks(gpsTimeoutRunnable!!)
        gpsDialog?.dismiss()
        val fusedLocationProvider = fusedLocationClient
        gpsLocationCallback?.let { fusedLocationProvider.removeLocationUpdates(it) }
    }

    private fun checkLocationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mostrarDialogoGPS()
                }
            }
        }
    }

    private fun configurarListenersBasicos() {
        Log.d("SpinnerDebug", "Configurando listeners básicos...")
        
        // Listener básico para instalación
        spinnerInstalacion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val instalacionSeleccionada = parent?.getItemAtPosition(position)?.toString() ?: ""
                Log.d("SpinnerDebug", "Instalación seleccionada: '$instalacionSeleccionada'")
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Listener básico para aislamiento
        spinnerAislamiento.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val aislamientoSeleccionado = parent?.getItemAtPosition(position)?.toString() ?: ""
                Log.d("SpinnerDebug", "Aislamiento seleccionado: '$aislamientoSeleccionado'")
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
    
    private fun cargarColoresEstados(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        try {
            // Primero intentar leer desde el archivo dinámico
            val dynamicFile = java.io.File(requireContext().filesDir, "estados_colores.json")
            val jsonString = if (dynamicFile.exists()) {
                Log.d("FragmentInspeccionEquipo", "Leyendo colores desde archivo dinámico")
                dynamicFile.readText()
            } else {
                Log.d("FragmentInspeccionEquipo", "Archivo dinámico no existe, usando assets")
                requireContext().assets.open("estados_colores.json").bufferedReader().use { it.readText() }
            }
            
            val json = JSONObject(jsonString)
            json.keys().forEach { estado ->
                val colorHex = json.getString(estado)
                try {
                    map[estado] = android.graphics.Color.parseColor(colorHex)
                    Log.d("FragmentInspeccionEquipo", "Color cargado: $estado -> $colorHex")
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionEquipo", "Error parsing color for estado $estado: $colorHex", e)
                }
            }
            Log.d("FragmentInspeccionEquipo", "Total colores cargados: ${map.size}")
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "Error loading estados_colores.json", e)
        }
        return map
    }
    
    private fun aplicarColorEstado(estado: String) {
        val color = coloresEstados[estado.uppercase().trim()] ?: android.graphics.Color.GRAY
        
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(color)
            setStroke(2, android.graphics.Color.WHITE)
        }
        spinnerEstado.background = drawable
        spinnerEstado.setPopupBackgroundResource(android.R.color.transparent)
        
        // Configurar color del texto del spinner
        val colorTexto = if (esColorClaro(color)) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
        
        // Aplicar color al texto del spinner
        for (i in 0 until spinnerEstado.childCount) {
            val child = spinnerEstado.getChildAt(i)
            if (child is TextView) {
                child.setTextColor(colorTexto)
                child.gravity = android.view.Gravity.CENTER
                child.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
        
        spinnerEstado.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val estadoSeleccionado = parent?.getItemAtPosition(position)?.toString() ?: ""
                aplicarColorEstado(estadoSeleccionado)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
    
    private fun esColorClaro(color: Int): Boolean {
        val luminosidad = calcularLuminosidad(color)
        return luminosidad > 0.5
    }
    
    private fun calcularLuminosidad(color: Int): Double {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        
        // Fórmula para calcular luminosidad
        return (0.299 * red + 0.587 * green + 0.114 * blue) / 255
    }
    
    private fun calcularTono(color: Int): Double {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        
        // Convertir RGB a HSV para obtener el tono
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(red, green, blue, hsv)
        return hsv[0].toDouble() // H (tono)
    }

    // Adapter personalizado para el spinner de estado con colores
    private inner class EstadoSpinnerAdapter(
        context: Context,
        private val estados: List<String>
    ) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, estados) {
        
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            val estado = estados[position]
            
            // Obtener color del JSON
            val colorFondo = coloresEstados[estado.uppercase().trim()] ?: android.graphics.Color.GRAY
            
            // Determinar color del texto basado en el color de fondo
            val colorTexto = if (esColorClaro(colorFondo)) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
            
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f
                setColor(colorFondo)
                setStroke(1, android.graphics.Color.BLACK)
            }
            view.background = drawable
            
            // Configurar texto con altura aumentada
            if (view is TextView) {
                view.setTextColor(colorTexto)
                view.textSize = 16f
                view.setPadding(16, 20, 16, 20) // Aumentar padding vertical para más altura
                view.gravity = android.view.Gravity.CENTER
                view.setTypeface(null, android.graphics.Typeface.BOLD)
                view.minHeight = 80 // Altura mínima para facilitar el toque
            }
            
            return view
        }
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val estado = estados[position]
            aplicarColorEstado(estado)
            return view
        }
        
        private fun esColorClaro(color: Int): Boolean {
            val luminosidad = calcularLuminosidad(color)
            return luminosidad > 0.5
        }
    }

    override fun onFotoActualizada(tipoFoto: String, rutaFoto: String) {
        Log.d("FragmentInspeccionEquipo", "Foto actualizada - Tipo: $tipoFoto, Ruta: $rutaFoto")
        
        // Actualizar la base de datos con la nueva ruta de foto
        lifecycleScope.launch {
            try {
                val equipoActual = equipos.getOrNull(indexActual)
                if (equipoActual != null) {
                    val equipoActualizado = when (tipoFoto) {
                        "EQUIPO" -> equipoActual.copy(urlFotoEquipo = rutaFoto)
                        "UBICACION" -> equipoActual.copy(urlFotoUbicacion = rutaFoto)
                        "MANIFOLD" -> equipoActual.copy(urlFotoManifold = rutaFoto)
                        else -> {
                            // Para fotos extra, añadir a la lista existente
                            val fotosExtraActuales = equipoActual.urlFotosExtra ?: ""
                            val fotosExtraList = if (fotosExtraActuales.isEmpty()) {
                                listOf(rutaFoto)
                            } else {
                                fotosExtraActuales.split(";").toMutableList().apply {
                                    add(rutaFoto)
                                }
                            }
                            equipoActual.copy(urlFotosExtra = fotosExtraList.joinToString(";"))
                        }
                    }
                    
                    inspeccionDao.actualizarEquipo(equipoActualizado)
                    Log.d("FragmentInspeccionEquipo", "Base de datos actualizada para tipo: $tipoFoto")
                    
                    // Actualizar la lista local
                    equipos[indexActual] = equipoActualizado
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionEquipo", "Error actualizando base de datos: ${e.message}", e)
            }
        }
    }
} 