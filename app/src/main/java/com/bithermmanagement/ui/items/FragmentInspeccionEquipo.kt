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
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.data.SettingsManager
import com.bithermmanagement.data.GoogleAuthAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.bithermmanagement.database.entities.Equipo
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
            "SIN BLOQUEO",
            "V/V VAPOR",
            "V/V CONDENSADO",
            "V/V ANT-POST",
            " ",
            "TVS 4V/V",
            "TVS (OTRA)",
            " ",
            "V/V VAPOR GRIPADA",
            "V/V COND. GRIPADA",
            "V/Vs GRIPADAS",
            " ",
            "TVS TRASPASADA",
            "TVS GRIPADA"
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
    private var equipo: Equipo? = null
    private var coloresEstados: Map<String, Int> = emptyMap()
    private var indexActual: Int = 0
    private var equipoSeleccionadoId: String? = null
    private var estadosUnicos: List<String> = emptyList()
    
    // Control de estado de cards
    private var cardAbiertoActual: String? = null // "datosGenerales", "observaciones", "caracteristicas", "historial"
    
    // Control de paginación del historial
    private var historialCompleto = mutableListOf<HistorialInspeccion>()
    private var historialMostrado = mutableListOf<HistorialInspeccion>()
    private val LIMITE_INICIAL = 5
    
    // Control de paginación de reparaciones
    private var reparacionesCompleto = mutableListOf<ReparacionEquipo>()
    private var reparacionesMostrado = mutableListOf<ReparacionEquipo>()
    
    // Valores anteriores para detectar cambios en spinners dependientes
    private var areaAnterior: String = ""
    private var marcaAnterior: String = ""
    private var modeloAnterior: String = ""
    private var presionEntradaAnterior: String = ""
    // Views
    private lateinit var spinnerArea: Spinner
    private lateinit var spinnerUnidad: Spinner
    
    // Views del card HISTORIAL
    private lateinit var headerHistorial: LinearLayout
    private lateinit var layoutHistorial: LinearLayout
    private lateinit var iconCollapseHistorial: ImageView
    private lateinit var tablaHistorial: LinearLayout
    private lateinit var btnCargarMasHistorial: Button
    
    // Views del card REPARACIONES
    private lateinit var headerReparaciones: LinearLayout
    private lateinit var layoutReparaciones: LinearLayout
    private lateinit var iconCollapseReparaciones: ImageView
    private lateinit var tablaReparaciones: LinearLayout
    private lateinit var btnCargarMasReparaciones: Button
    private lateinit var spinnerInstalacion: Spinner
    private lateinit var spinnerAislamiento: Spinner
    private lateinit var spinnerEstado: Spinner
    private lateinit var spinnerPeriodicidad: Spinner
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
    private lateinit var etNombreManifold: AutoCompleteTextView
    private lateinit var checkboxByPass: CheckBox
    private lateinit var etObservaciones: EditText
    private lateinit var etIncidencias: EditText
    private lateinit var tvGpsCoord: TextView
    private lateinit var tvGpsAcc: TextView
    private lateinit var tvIdEquipo: TextView
    private lateinit var tvIdEquipoFixed: TextView
    private lateinit var textView: TextView
    private lateinit var tvInspectorFecha: TextView
    private lateinit var tvStatus: TextView
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
            // Cargar equipos desde la hoja FLOTA según la inspección seleccionada
            cargarEquiposDesdeBaseDatos()
            
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
        spinnerPeriodicidad = view.findViewById(R.id.spinnerPeriodicidad)
        
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
        etNombreManifold = view.findViewById(R.id.autoInstalacionMf)
        checkboxByPass = view.findViewById(R.id.checkboxByPass)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        etIncidencias = view.findViewById(R.id.etIncidencias)
        
        // Forzar color blanco en campos de observaciones
        etObservaciones.setTextColor(android.graphics.Color.WHITE)
        etIncidencias.setTextColor(android.graphics.Color.WHITE)
        etObservaciones.setHintTextColor(android.graphics.Color.parseColor("#BBBBBB"))
        etIncidencias.setHintTextColor(android.graphics.Color.parseColor("#BBBBBB"))
        
        // Aplicar color de hint consistente a todos los campos
        etUbicacion.setHintTextColor(android.graphics.Color.parseColor("#BBBBBB"))
        etLinea.setHintTextColor(android.graphics.Color.parseColor("#BBBBBB"))
        
        // TextViews
        tvGpsCoord = view.findViewById(R.id.tvGpsCoord)
        tvGpsAcc = view.findViewById(R.id.tvGpsAcc)
        tvIdEquipo = view.findViewById(R.id.tvIdEquipo)
        tvIdEquipoFixed = view.findViewById(R.id.tvIdEquipoFixed)
        textView = view.findViewById(R.id.textView)
        tvInspectorFecha = view.findViewById(R.id.tvInspectorFecha)
        tvStatus = view.findViewById(R.id.tvStatus)
        
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
        headerHistorial = view.findViewById(R.id.headerHistorial)
        layoutHistorial = view.findViewById(R.id.layoutHistorial)
        tablaHistorial = view.findViewById(R.id.tablaHistorial)
        btnCargarMasHistorial = view.findViewById(R.id.btnCargarMasHistorial)
        headerReparaciones = view.findViewById(R.id.headerReparaciones)
        layoutReparaciones = view.findViewById(R.id.layoutReparaciones)
        iconCollapseReparaciones = view.findViewById(R.id.iconCollapseReparaciones)
        tablaReparaciones = view.findViewById(R.id.tablaReparaciones)
        btnCargarMasReparaciones = view.findViewById(R.id.btnCargarMasReparaciones)
        iconCollapseDatosGenerales = view.findViewById(R.id.iconCollapseDatosGenerales)
        iconCollapseObservaciones = view.findViewById(R.id.iconCollapseObservaciones)
        iconCollapseCaracteristicas = view.findViewById(R.id.iconCollapseCaracteristicas)
        iconCollapseHistorial = view.findViewById(R.id.iconCollapseHistorial)
        
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
            val areaAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, areas)
            areaAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerArea.adapter = areaAdapter
            
            val unidades = inspeccionDao.getUnidadesUnicas().ifEmpty { listOf("U1", "U2", "U3", "U4", "U5") }
            val unidadAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, unidades)
            unidadAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerUnidad.adapter = unidadAdapter
            
            // Cargar colores primero para obtener los estados de inspección del JSON
            coloresEstados = cargarColoresEstados()
            
            // Usar directamente los estados del JSON (estados de inspección, no status del equipo)
            // Mantener el orden original del JSON
            val estadosOrdenados = coloresEstados.keys.toList()
                
            Log.d("FragmentInspeccionEquipo", "Estados de inspección cargados desde JSON: ${estadosOrdenados.size} estados")
            
            val estadoAdapter = EstadoSpinnerAdapter(requireContext(), estadosOrdenados)
            spinnerEstado.adapter = estadoAdapter
            
            // Configurar spinner de PERIODICIDAD
            val periodicidades = listOf("(mon) Diario", "(mon) Semanal", "Mensual", "Bimensual", "Trimestral", "Anual", "Estacional")
            val periodicidadAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, periodicidades)
            periodicidadAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerPeriodicidad.adapter = periodicidadAdapter
            
            // Establecer "Mensual" como valor por defecto
            val defaultIndex = periodicidades.indexOf("Mensual")
            if (defaultIndex >= 0) {
                spinnerPeriodicidad.setSelection(defaultIndex)
            }
            
            // RECREAR DE CERO: Spinner de INSTALACIÓN
            Log.d("SpinnerDebug", "=== RECREANDO SPINNER INSTALACIÓN ===")
            val instalaciones = valoresInstalacionPorDefecto
            Log.d("SpinnerDebug", "Valores instalación: $instalaciones")
            val instalacionAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, instalaciones)
            instalacionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerInstalacion.adapter = instalacionAdapter
            Log.d("SpinnerDebug", "Adapter instalación configurado con ${instalaciones.size} elementos")
            
            // RECREAR DE CERO: Spinner de AISLAMIENTO
            Log.d("SpinnerDebug", "=== RECREANDO SPINNER AISLAMIENTO ===")
            val aislamientos = valoresAislamientoPorDefecto
            Log.d("SpinnerDebug", "Valores aislamiento: $aislamientos")
            val aislamientoAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, aislamientos)
            aislamientoAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerAislamiento.adapter = aislamientoAdapter
            Log.d("SpinnerDebug", "Adapter aislamiento configurado con ${aislamientos.size} elementos")
            
            // Configurar listeners básicos para instalación y aislamiento
            configurarListenersBasicos()
            
            // Configurar spinners de CARACTERÍSTICAS TÉCNICAS
            val marcas = inspeccionDao.getMarcasUnicas().ifEmpty { listOf("SPIRAX SARCO", "ARMSTRONG", "GESTRA", "OTROS") }
            val marcaAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, marcas)
            marcaAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerMarca.adapter = marcaAdapter
            
            val modelos = inspeccionDao.getModelosUnicos().ifEmpty { listOf("Modelo 1", "Modelo 2", "Modelo 3") }
            val modeloAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, modelos)
            modeloAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerModelo.adapter = modeloAdapter
            
            val tipos = inspeccionDao.getTiposUnicos().ifEmpty { listOf("Tipo 1", "Tipo 2", "Tipo 3") }
            val tipoAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, tipos)
            tipoAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerTipo.adapter = tipoAdapter
            
            val diametros = inspeccionDao.getDiametrosUnicos().ifEmpty { listOf("DN15", "DN20", "DN25", "DN32", "DN40", "DN50") }
            val diametroAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, diametros)
            diametroAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerDiametro.adapter = diametroAdapter
            
            val conexiones = inspeccionDao.getConexionesUnicas().ifEmpty { listOf("Rosca", "Brida", "Soldadura") }
            val conexionAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, conexiones)
            conexionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerConexion.adapter = conexionAdapter
            
            val presionesEntrada = inspeccionDao.getPresionesEntradaUnicas().ifEmpty { listOf("1 bar", "2 bar", "3 bar", "4 bar", "5 bar") }
            val presEntradaAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, presionesEntrada)
            presEntradaAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerPIN.adapter = presEntradaAdapter
            
            val presionesSalida = inspeccionDao.getPresionesSalidaUnicas().ifEmpty { listOf("0.1 bar", "0.2 bar", "0.3 bar", "0.4 bar", "0.5 bar") }
            val presSalidaAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, presionesSalida)
            presSalidaAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerPOUT.adapter = presSalidaAdapter
            
            val descargas = inspeccionDao.getDescargasUnicas().ifEmpty { listOf("Continua", "Intermittente", "Pulsante") }
            val descargaAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, descargas)
            descargaAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerDescarga.adapter = descargaAdapter
            
            val aplicaciones = inspeccionDao.getAplicacionesUnicas().ifEmpty { listOf("Calefacción", "Proceso", "Vapor") }
            val aplicacionAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, aplicaciones)
            aplicacionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
            spinnerAplicacion.adapter = aplicacionAdapter
            
            val servicios = inspeccionDao.getServiciosUnicos().ifEmpty { listOf("Vapor", "Agua", "Aire") }
            val servicioAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_white_text, servicios)
            servicioAdapter.setDropDownViewResource(R.layout.spinner_dropdown_white_text)
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
        equipo = equipos[index] // Asignar a la variable de instancia
        Log.d("MOSTRAR_EQUIPO", "Mostrando equipo: $equipo")
        Log.d("MOSTRAR_EQUIPO", "Índice: $index, Total equipos: ${equipos.size}")
        Log.d("MOSTRAR_EQUIPO", "ID del equipo: ${equipo?.id}")
        Log.d("MOSTRAR_EQUIPO", "=== DATOS DEL EQUIPO EN MEMORIA ===")
        Log.d("MOSTRAR_EQUIPO", "area='${equipo?.area}' (null=${equipo?.area == null}, empty=${equipo?.area?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "unidad='${equipo?.unidad}' (null=${equipo?.unidad == null}, empty=${equipo?.unidad?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "instalacion='${equipo?.instalacion}' (null=${equipo?.instalacion == null}, empty=${equipo?.instalacion?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "ubicacion='${equipo?.ubicacion}' (null=${equipo?.ubicacion == null}, empty=${equipo?.ubicacion?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "linea='${equipo?.linea}' (null=${equipo?.linea == null}, empty=${equipo?.linea?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "marca='${equipo?.marca}' (null=${equipo?.marca == null}, empty=${equipo?.marca?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "modelo='${equipo?.modelo}' (null=${equipo?.modelo == null}, empty=${equipo?.modelo?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "estado='${equipo?.estado}' (null=${equipo?.estado == null}, empty=${equipo?.estado?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "flota='${equipo?.flota}' (null=${equipo?.flota == null}, empty=${equipo?.flota?.isEmpty() == true})")
        Log.d("MOSTRAR_EQUIPO", "=== FIN DATOS EN MEMORIA ===")
        
        // Llenar campos principales
        // Mostrar ID del equipo en el TAG fijo (parte superior no scrollable)
        tvIdEquipoFixed.text = equipo?.id ?: ""
        tvIdEquipoFixed.setTypeface(null, android.graphics.Typeface.BOLD)
        
        // El tvIdEquipo dentro del scroll se mantiene oculto
        tvIdEquipo.visibility = View.GONE
        
        // Llenar spinners de DATOS GENERALES
        spinnerArea.setSelection(getSpinnerIndex(spinnerArea, equipo?.area ?: ""))
        spinnerUnidad.setSelection(getSpinnerIndex(spinnerUnidad, equipo?.unidad ?: ""))
        spinnerInstalacion.setSelection(getSpinnerIndex(spinnerInstalacion, equipo?.instalacion ?: ""))
        spinnerAislamiento.setSelection(getSpinnerIndex(spinnerAislamiento, equipo?.aislamiento ?: ""))
        spinnerEstado.setSelection(getSpinnerIndex(spinnerEstado, equipo?.estado ?: ""))
        spinnerPeriodicidad.setSelection(getSpinnerIndex(spinnerPeriodicidad, equipo?.periodicidad ?: ""))
        
        // Aplicar color dependiente del estado
        aplicarColorEstado(equipo?.estado ?: "")
        
        // Actualizar valores anteriores para detectar cambios
        areaAnterior = equipo?.area ?: ""
        marcaAnterior = equipo?.marca ?: ""
        modeloAnterior = equipo?.modelo ?: ""
        presionEntradaAnterior = equipo?.presEntrada ?: ""
        
        // Llenar EditTexts de DATOS GENERALES
        etUbicacion.setText(equipo?.ubicacion ?: "")
        etLinea.setText(equipo?.linea ?: "")
        etNombreManifold.setText(equipo?.instalacionMf ?: "")
        checkboxByPass.isChecked = equipo?.byPass ?: false
        
        // Mostrar coordenadas GPS existentes (formato de 6 decimales)
        equipo?.gpsCoord?.let { gpsCoord ->
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
        equipo?.gpsAcc?.let { gpsAcc ->
            if (gpsAcc.isNotEmpty()) {
                tvGpsAcc.text = "Precisión: ${gpsAcc}m"
            } else {
                tvGpsAcc.text = "Precisión: --"
            }
        } ?: run {
            tvGpsAcc.text = "Precisión: --"
        }
        
        // Llenar campos de CARACTERÍSTICAS TÉCNICAS
        spinnerMarca.setSelection(getSpinnerIndex(spinnerMarca, equipo?.marca ?: ""))
        spinnerModelo.setSelection(getSpinnerIndex(spinnerModelo, equipo?.modelo ?: ""))
        spinnerTipo.setSelection(getSpinnerIndex(spinnerTipo, equipo?.tipo ?: ""))
        spinnerDiametro.setSelection(getSpinnerIndex(spinnerDiametro, equipo?.diametro ?: ""))
        spinnerConexion.setSelection(getSpinnerIndex(spinnerConexion, equipo?.conexion ?: ""))
        spinnerPIN.setSelection(getSpinnerIndex(spinnerPIN, equipo?.presEntrada ?: ""))
        spinnerPOUT.setSelection(getSpinnerIndex(spinnerPOUT, equipo?.presSalida ?: ""))
        spinnerDescarga.setSelection(getSpinnerIndex(spinnerDescarga, equipo?.descarga ?: ""))
        spinnerAplicacion.setSelection(getSpinnerIndex(spinnerAplicacion, equipo?.aplicacion ?: ""))
        spinnerServicio.setSelection(getSpinnerIndex(spinnerServicio, equipo?.servicio ?: ""))
        
        // Llenar campos de OBSERVACIONES
        etObservaciones.setText(equipo?.nota ?: "")
        etIncidencias.setText(equipo?.incidencias ?: "")
        
        // Forzar color blanco después de llenar los campos
        etObservaciones.setTextColor(android.graphics.Color.WHITE)
        etIncidencias.setTextColor(android.graphics.Color.WHITE)
        
        // Llenar TextView de características técnicas (formato: marca - modelo (diametro - conexion))
        val marca = equipo?.marca ?: "-"
        val modelo = equipo?.modelo ?: "-"
        val diametro = equipo?.diametro ?: "-"
        val conexion = equipo?.conexion ?: "-"
        textView.text = "$marca - $modelo ($diametro - $conexion)"
        
        // Llenar TextView de información del inspector (formato: fecha - inspector (detector))
        val inspector = equipo?.identidadInspector ?: "-"
        val fecha = equipo?.fechaInspeccion ?: "-"
        val detector = equipo?.detectorUtilizado ?: "-"
        tvInspectorFecha.text = "$fecha - $inspector ($detector)"
        
        // Mostrar FLOTA si está disponible (no se usa para colorear)
        val flota = equipo?.flota ?: ""
        if (flota.isNotEmpty()) {
            tvStatus.text = "FLOTA: $flota"
            tvStatus.visibility = View.VISIBLE
            Log.d("FragmentInspeccionEquipo", "FLOTA del equipo: $flota")
        } else {
            tvStatus.visibility = View.GONE
        }
        
        // Actualizar mini galería
        equipo?.id?.let { equipoId ->
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
        Log.d("FragmentInspeccionEquipo", "=== INICIO GUARDAR Y AVANZAR ===")
        
        equipo?.let { equipoActual ->
            // Obtener datos actuales del usuario logueado
            val fechaActual = obtenerFechaActual()
            val inspectorActual = obtenerInspectorActual()
            val detectorActual = obtenerDetectorActual()
            
            Log.d("FragmentInspeccionEquipo", "Datos del usuario actual:")
            Log.d("FragmentInspeccionEquipo", "  - Fecha: $fechaActual")
            Log.d("FragmentInspeccionEquipo", "  - Inspector: $inspectorActual")
            Log.d("FragmentInspeccionEquipo", "  - Detector: $detectorActual")
            
            // Obtener coordenadas GPS actuales al momento de guardar
            val lastLocation = com.bithermmanagement.ui.MainMenuActivity.GpsProvider.lastLocation
            val coordenadasGPS = if (lastLocation != null) {
                "${lastLocation.latitude}, ${lastLocation.longitude}"
            } else {
                equipoActual.gpsCoord ?: ""
            }
            
            val precisionGPS = if (lastLocation != null) {
                "${"%.1f".format(lastLocation.accuracy)}m"
            } else {
                equipoActual.gpsAcc ?: ""
            }
            
            Log.d("FragmentInspeccionEquipo", "GPS actual:")
            Log.d("FragmentInspeccionEquipo", "  - Coordenadas: $coordenadasGPS")
            Log.d("FragmentInspeccionEquipo", "  - Precisión: $precisionGPS")
            
            // Obtener datos de los campos
            val estado = spinnerEstado.selectedItem?.toString() ?: ""
            val ubicacion = etUbicacion.text.toString()
            val nota = etObservaciones.text.toString()
            val incidencias = etIncidencias.text.toString()
            val linea = etLinea.text.toString()
            val nombreManifold = etNombreManifold.text.toString()
            val byPass = checkboxByPass.isChecked
            val instalacion = spinnerInstalacion.selectedItem?.toString() ?: ""
            val aislamiento = spinnerAislamiento.selectedItem?.toString() ?: ""
            
            Log.d("FragmentInspeccionEquipo", "Datos de los campos:")
            Log.d("FragmentInspeccionEquipo", "  - Estado: $estado")
            Log.d("FragmentInspeccionEquipo", "  - Ubicación: $ubicacion")
            Log.d("FragmentInspeccionEquipo", "  - Nota: $nota")
            Log.d("FragmentInspeccionEquipo", "  - Incidencias: $incidencias")
            Log.d("FragmentInspeccionEquipo", "  - Línea: $linea")
            Log.d("FragmentInspeccionEquipo", "  - Instalación: $instalacion")
            Log.d("FragmentInspeccionEquipo", "  - Nombre Manifold: $nombreManifold")
            Log.d("FragmentInspeccionEquipo", "  - By-Pass: $byPass")
            Log.d("FragmentInspeccionEquipo", "  - Aislamiento: $aislamiento")
            
            // Verificar si se modificaron campos manuales (no automáticos)
            val camposModificados = mutableListOf<String>()
            
            // CAMPOS AUTOMÁTICOS (NO cuentan como modificados):
            // - fechaInspeccion (se actualiza automáticamente)
            // - identidadInspector (se actualiza automáticamente)
            // - detectorUtilizado (se actualiza automáticamente)
            
            // CAMPOS MANUALES (SÍ cuentan como modificados):
            // - estado (se modifica manualmente en el spinner, pero si no se cambia se guarda el preseleccionado)
            if (estado != equipoActual.estado) camposModificados.add("estado")
            if (ubicacion != (equipoActual.ubicacion ?: "")) camposModificados.add("ubicacion")
            if (nota != (equipoActual.nota ?: "")) camposModificados.add("nota")
            if (incidencias != (equipoActual.incidencias ?: "")) camposModificados.add("incidencias")
            if (linea != (equipoActual.linea ?: "")) camposModificados.add("linea")
            if (nombreManifold != (equipoActual.instalacionMf ?: "")) camposModificados.add("instalacionMf")
            if (byPass != equipoActual.byPass) camposModificados.add("byPass")
            if (instalacion != (equipoActual.instalacion ?: "")) camposModificados.add("instalacion")
            if (aislamiento != (equipoActual.aislamiento ?: "")) camposModificados.add("aislamiento")
            
            // GPS: Solo considerar cambio si hay diferencia significativa (más de 10 metros)
            val gpsOriginal = equipoActual.gpsCoord ?: ""
            if (gpsOriginal.isNotEmpty() && coordenadasGPS.isNotEmpty()) {
                try {
                    val coordsOriginal = gpsOriginal.split(", ")
                    val coordsActual = coordenadasGPS.split(", ")
                    if (coordsOriginal.size == 2 && coordsActual.size == 2) {
                        val latOriginal = coordsOriginal[0].toDouble()
                        val lonOriginal = coordsOriginal[1].toDouble()
                        val latActual = coordsActual[0].toDouble()
                        val lonActual = coordsActual[1].toDouble()
                        
                        // Calcular distancia aproximada (simplificado)
                        val distancia = Math.sqrt(
                            Math.pow(latActual - latOriginal, 2.0) + 
                            Math.pow(lonActual - lonOriginal, 2.0)
                        ) * 111000 // Aproximación a metros
                        
                        if (distancia > 10) { // Más de 10 metros de diferencia
                            camposModificados.add("gpsCoord")
                        }
                    } else {
                        // Si no se pueden parsear, considerar como cambio
                        if (coordenadasGPS != gpsOriginal) camposModificados.add("gpsCoord")
                    }
                } catch (e: Exception) {
                    // Si hay error en el cálculo, usar comparación simple
                    if (coordenadasGPS != gpsOriginal) camposModificados.add("gpsCoord")
                }
            } else if (gpsOriginal.isNotEmpty() && coordenadasGPS != gpsOriginal) {
                // Solo considerar cambio si el original no estaba vacío
                camposModificados.add("gpsCoord")
            }
            // Si gpsOriginal está vacío y coordenadasGPS tiene valor, NO es un cambio (es normal)
            
            // Precisión GPS: Solo considerar cambio si hay diferencia significativa
            val precisionOriginal = equipoActual.gpsAcc ?: ""
            if (precisionOriginal.isNotEmpty() && precisionGPS.isNotEmpty()) {
                try {
                    val precisionOriginalNum = precisionOriginal.replace("m", "").replace(",", ".").toDouble()
                    val precisionActualNum = precisionGPS.replace("m", "").replace(",", ".").toDouble()
                    if (Math.abs(precisionActualNum - precisionOriginalNum) > 5) { // Más de 5 metros de diferencia
                        camposModificados.add("gpsAcc")
                    }
                } catch (e: Exception) {
                    if (precisionGPS != precisionOriginal) camposModificados.add("gpsAcc")
                }
            } else if (precisionOriginal.isNotEmpty() && precisionGPS != precisionOriginal) {
                // Solo considerar cambio si el original no estaba vacío
                camposModificados.add("gpsAcc")
            }
            // Si precisionOriginal está vacío y precisionGPS tiene valor, NO es un cambio (es normal)
            
            // Verificar si se modificó la periodicidad
            val periodicidadActual = spinnerPeriodicidad.selectedItem?.toString() ?: ""
            if (periodicidadActual != (equipoActual.periodicidad ?: "")) camposModificados.add("periodicidad")
            
            Log.d("FragmentInspeccionEquipo", "=== ANÁLISIS DE CAMPOS ===")
            Log.d("FragmentInspeccionEquipo", "Estado actual: '$estado' vs original: '${equipoActual.estado}'")
            Log.d("FragmentInspeccionEquipo", "Ubicación actual: '$ubicacion' vs original: '${equipoActual.ubicacion}'")
            Log.d("FragmentInspeccionEquipo", "Nota actual: '$nota' vs original: '${equipoActual.nota}'")
            Log.d("FragmentInspeccionEquipo", "Incidencias actual: '$incidencias' vs original: '${equipoActual.incidencias}'")
            Log.d("FragmentInspeccionEquipo", "Línea actual: '$linea' vs original: '${equipoActual.linea}'")
            Log.d("FragmentInspeccionEquipo", "GPS Coord actual: '$coordenadasGPS' vs original: '${equipoActual.gpsCoord}'")
            Log.d("FragmentInspeccionEquipo", "GPS Acc actual: '$precisionGPS' vs original: '${equipoActual.gpsAcc}'")
            Log.d("FragmentInspeccionEquipo", "Periodicidad actual: '$periodicidadActual' vs original: '${equipoActual.periodicidad}'")
            Log.d("FragmentInspeccionEquipo", "Campos modificados: ${camposModificados.joinToString(", ")}")
            
            // Determinar si es solo inspección o modificación
            val soloInspeccion = camposModificados.isEmpty()
            val modificadoLocal = !soloInspeccion
            
            Log.d("FragmentInspeccionEquipo", "Solo inspección: $soloInspeccion, Modificado local: $modificadoLocal")
            Log.d("FragmentInspeccionEquipo", "=== FIN ANÁLISIS ===")
            
            // Crear equipo actualizado con los nuevos datos
            val equipoActualizado = equipoActual.copy(
                fechaInspeccion = fechaActual,
                identidadInspector = inspectorActual,
                detectorUtilizado = detectorActual,
                estado = estado,
                ubicacion = ubicacion,
                nota = nota,
                incidencias = incidencias,
                linea = linea,
                instalacion = instalacion,
                instalacionMf = nombreManifold,
                byPass = byPass,
                aislamiento = aislamiento,
                gpsCoord = coordenadasGPS,
                gpsAcc = precisionGPS,
                periodicidad = periodicidadActual,
                modificadoLocal = modificadoLocal
            )
            
            Log.d("FragmentInspeccionEquipo", "Equipo actualizado creado:")
            Log.d("FragmentInspeccionEquipo", "  - ID: ${equipoActualizado.id}")
            Log.d("FragmentInspeccionEquipo", "  - Fecha inspección: ${equipoActualizado.fechaInspeccion}")
            Log.d("FragmentInspeccionEquipo", "  - Inspector: ${equipoActualizado.identidadInspector}")
            Log.d("FragmentInspeccionEquipo", "  - Detector: ${equipoActualizado.detectorUtilizado}")
            
            // Guardar en la base de datos
            lifecycleScope.launch {
                try {
                    inspeccionDao.actualizarEquipo(equipoActualizado)
                    Log.d("FragmentInspeccionEquipo", "✅ Equipo actualizado exitosamente en BD: ${equipoActualizado.id}")
                    
                    // Actualizar el equipo local
                    equipo = equipoActualizado
                    equipos[indexActual] = equipoActualizado
                    
                    // Actualizar la información del inspector en la UI
                    actualizarInformacionInspector()
                    
                    // Guardar datos de inspección en Google Sheets
                    guardarInspeccionEnGoogleSheets(equipoActualizado)
                    
                    Log.d("FragmentInspeccionEquipo", "✅ Datos guardados correctamente, navegando al siguiente...")
                    
                    // Navegar al siguiente purgador
                    mostrarEquipo(indexActual + 1)
                    
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionEquipo", "❌ Error guardando equipo: ${e.message}", e)
                    // Mostrar error al usuario
                    mostrarError("Error al guardar: ${e.message}")
                }
            }
        } ?: run {
            Log.e("FragmentInspeccionEquipo", "❌ Equipo es null, no se puede guardar")
            mostrarError("No hay equipo seleccionado para guardar")
        }
        
        Log.d("FragmentInspeccionEquipo", "=== FIN GUARDAR Y AVANZAR ===")
    }

    private fun obtenerFechaActual(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun obtenerInspectorActual(): String {
        // Obtener del SharedPreferences
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        val swWeb = prefs.getString("sw_web", "SW WEB") ?: "SW WEB"
        Log.d("FragmentInspeccionEquipo", "Inspector actual obtenido: $swWeb")
        return swWeb
    }

    private fun obtenerDetectorActual(): String {
        // Obtener del SharedPreferences
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        val equipoAsignado = prefs.getString("equipo_asignado", "Detector") ?: "Detector"
        Log.d("FragmentInspeccionEquipo", "Detector actual obtenido: $equipoAsignado")
        return equipoAsignado
    }

    private fun mostrarError(mensaje: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun actualizarInformacionInspector() {
        equipo?.let { equipo ->
            val fechaEstado = equipo.fechaInspeccion ?: ""
            val inspector = equipo.identidadInspector ?: ""
            val detector = equipo.detectorUtilizado ?: ""
            
            Log.d("FragmentInspeccionEquipo", "=== DEBUG DATOS INSPECTOR ===")
            Log.d("FragmentInspeccionEquipo", "fechaInspeccion: '$fechaEstado'")
            Log.d("FragmentInspeccionEquipo", "identidadInspector: '$inspector'")
            Log.d("FragmentInspeccionEquipo", "detectorUtilizado: '$detector'")
            Log.d("FragmentInspeccionEquipo", "Equipo ID: ${equipo.id}")
            
            val textoInspector = if (fechaEstado.isNotEmpty() && inspector.isNotEmpty()) {
                "$fechaEstado - $inspector ($detector)"
            } else {
                "Sin datos de inspección anterior"
            }
            
            // Actualizar solo el campo que existe
            tvInspectorFecha.text = textoInspector
            
            Log.d("FragmentInspeccionEquipo", "Texto final inspector: '$textoInspector'")
            Log.d("FragmentInspeccionEquipo", "=== FIN DEBUG ===")
        } ?: run {
            Log.e("FragmentInspeccionEquipo", "Equipo es null, no se puede actualizar información")
            tvInspectorFecha.text = "Sin datos de inspección anterior"
        }
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
        
        headerHistorial.setOnClickListener {
            toggleCard("historial", layoutHistorial, iconCollapseHistorial)
        }
        
        headerReparaciones.setOnClickListener {
            toggleCard("reparaciones", layoutReparaciones, iconCollapseReparaciones)
        }
        
        // Inicializar estado de cards (todos cerrados por defecto)
        if (cardAbiertoActual == null) {
            // Si no hay card abierto, cerrar todos
            layoutDatosGenerales.visibility = View.GONE
            layoutObservaciones.visibility = View.GONE
            layoutCaracteristicas.visibility = View.GONE
            layoutHistorial.visibility = View.GONE
            iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
            iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
            iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_more)
            iconCollapseHistorial.setImageResource(R.drawable.ic_expand_more)
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
            layoutHistorial.visibility = View.GONE
            iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
            iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
            iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_more)
            iconCollapseHistorial.setImageResource(R.drawable.ic_expand_more)
            
            // Abrir el card seleccionado
            layout.visibility = View.VISIBLE
            icon.setImageResource(R.drawable.ic_expand_less)
            cardAbiertoActual = cardId
            Log.d("CardsDebug", "Card '$cardId' abierto")
            
            // Si se abre el card de historial, cargar los datos
            if (cardId == "historial") {
                cargarHistorialEquipo()
            }
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
                layoutHistorial.visibility = View.GONE
                iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
                iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
                iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_less)
                iconCollapseHistorial.setImageResource(R.drawable.ic_expand_more)
            }
            "historial" -> {
                layoutDatosGenerales.visibility = View.GONE
                layoutObservaciones.visibility = View.GONE
                layoutCaracteristicas.visibility = View.GONE
                layoutHistorial.visibility = View.VISIBLE
                iconCollapseDatosGenerales.setImageResource(R.drawable.ic_expand_more)
                iconCollapseObservaciones.setImageResource(R.drawable.ic_expand_more)
                iconCollapseCaracteristicas.setImageResource(R.drawable.ic_expand_more)
                iconCollapseHistorial.setImageResource(R.drawable.ic_expand_less)
            }
        }
        Log.d("CardsDebug", "Estado de cards restaurado: $cardAbiertoActual")
    }
    
    private fun cargarHistorialEquipo() {
        Log.d("FragmentInspeccionEquipo", "Cargando historial del equipo")
        
        // Limpiar tabla existente (mantener solo el header)
        val childCount = tablaHistorial.childCount
        for (i in childCount - 1 downTo 1) { // Mantener el header (índice 0)
            tablaHistorial.removeViewAt(i)
        }
        
        // Obtener datos del historial desde la configuración (usar nuevas claves)
        val prefs = requireContext().getSharedPreferences("configuracion_inspeccion", Context.MODE_PRIVATE)
        // Usar libro_id y hoja_flota (nuevas claves del sistema simplificado)
        val libroId = prefs.getString("libro_id", null) ?: prefs.getString("db_inspecciones_libro_id", null)
        val hoja = prefs.getString("hoja_flota", null) ?: prefs.getString("db_inspecciones_hoja", "FLOTA")
        
        // Debug: Verificar todas las claves de configuración
        Log.d("FragmentInspeccionEquipo", "=== DEBUG CONFIGURACIÓN ===")
        Log.d("FragmentInspeccionEquipo", "Todas las claves en SharedPreferences:")
        val allKeys = prefs.all.keys
        for (key in allKeys) {
            val value = prefs.all[key]
            // Convertir el valor a String de forma segura
            val valueStr = when (value) {
                is String -> value
                is Int -> value.toString()
                is Boolean -> value.toString()
                is Float -> value.toString()
                is Long -> value.toString()
                else -> value?.toString() ?: "null"
            }
            Log.d("FragmentInspeccionEquipo", "  $key = $valueStr")
        }
        
        Log.d("FragmentInspeccionEquipo", "=== DIAGNÓSTICO HISTORIAL ===")
        Log.d("FragmentInspeccionEquipo", "Libro ID: $libroId")
        Log.d("FragmentInspeccionEquipo", "Hoja: $hoja")
        Log.d("FragmentInspeccionEquipo", "Equipo ID: ${equipo?.id}")
        
        if (libroId == null) {
            Log.e("FragmentInspeccionEquipo", "No hay libro DB inspecciones configurado")
            // Mostrar mensaje de error en la tabla
            mostrarMensajeError("Configura primero 'DB inspecciones' en la pantalla de configuración")
            return
        }
        
        // Cargar historial en background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sheetsManager = obtenerGoogleSheetsManager()
                if (sheetsManager == null) {
                    Log.e("FragmentInspeccionEquipo", "No se pudo crear GoogleSheetsManager")
                    return@launch
                }
                
                // Leer datos del equipo desde la hoja FLOTA
                val historial = obtenerHistorialEquipo(sheetsManager, libroId, hoja ?: "FLOTA", equipo?.id ?: "")
                
                Log.d("FragmentInspeccionEquipo", "Historial obtenido: ${historial.size} registros")
                
                withContext(Dispatchers.Main) {
                    if (historial.isEmpty()) {
                        mostrarMensajeError("No se encontraron datos históricos para este equipo")
                    } else {
                        // Ordenar en orden inverso (más recientes primero) y guardar historial completo
                        historialCompleto.clear()
                        historialCompleto.addAll(historial.sortedByDescending { it.numero })
                        
                        // Mostrar solo los primeros 5
                        mostrarHistorialPaginado()
                    }
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionEquipo", "Error cargando historial: ${e.message}", e)
            }
        }
    }
    
    private suspend fun obtenerHistorialEquipo(sheetsManager: GoogleSheetsManager, libroId: String, hoja: String, equipoId: String): List<HistorialInspeccion> {
        return try {
            // Leer toda la hoja para encontrar el equipo
            // La estructura es: Fila 1 (títulos), Fila 2 (cabeceras), Fila 3+ (datos)
            val range = "$hoja!A:ZZ"
            val values = sheetsManager.leerRango(libroId, range)
            
            if (values.isEmpty()) {
                Log.w("FragmentInspeccionEquipo", "No se encontraron datos en la hoja")
                return emptyList()
            }
            
            // Buscar la fila del equipo (empezar desde índice 2 = fila 3, después de las cabeceras en fila 2)
            val filaEquipo = values.drop(2).find { fila: List<Any?> ->
                fila.isNotEmpty() && fila.size > 1 && fila[1]?.toString()?.trim() == equipoId // Columna B (TAG)
            }
            
            if (filaEquipo == null) {
                Log.w("FragmentInspeccionEquipo", "No se encontró el equipo $equipoId en la hoja")
                return emptyList()
            }
            
            // Leer la fila 2 (índice 1) para obtener los números de inspección desde las cabeceras
            // Las cabeceras tienen formato "ESTADO 25", "FECHA 25", "INSPECTOR 25", "DETECTOR 25", "NOTA 25"
            val fila2 = if (values.size > 1) values[1] else emptyList<Any?>() // Fila 2 (índice 1)
            val numerosInspeccion = mutableListOf<Int>()
            
            Log.d("FragmentInspeccionEquipo", "Fila 2 (cabeceras) tiene ${fila2.size} columnas")
            Log.d("FragmentInspeccionEquipo", "Primeras 20 columnas de fila 2: ${fila2.take(20).map { it?.toString() }}")
            
            val numerosConIndices = mutableMapOf<Int, Int>() // número -> índice de columna ESTADO
            
            // Buscar columnas con formato "ESTADO 25", "ESTADO 26", etc.
            for (i in fila2.indices) {
                val valor = fila2[i]?.toString()?.trim() ?: ""
                // Buscar patrones como "ESTADO 25", "ESTADO 26", etc.
                val match = Regex("ESTADO\\s+(\\d+)", RegexOption.IGNORE_CASE).find(valor)
                if (match != null) {
                    val numero = match.groupValues[1].toIntOrNull()
                    if (numero != null && numero >= 25 && numero <= 50) {
                        numerosInspeccion.add(numero)
                        numerosConIndices[numero] = i // Guardar el índice de la columna ESTADO
                        Log.d("FragmentInspeccionEquipo", "Número de inspección encontrado: $numero en columna $i (ESTADO)")
                    }
                }
            }
            
            // Crear historial basado en los números de inspección encontrados
            val historial = mutableListOf<HistorialInspeccion>()
            
            for (numero in numerosInspeccion.sorted()) {
                // Obtener el índice de la columna ESTADO para este número de inspección
                val indiceEstado = numerosConIndices[numero] ?: continue
                
                // Las columnas están en orden: ESTADO, FECHA, INSPECTOR, DETECTOR, NOTA
                // Entonces: ESTADO = indiceEstado, FECHA = indiceEstado + 1, INSPECTOR = indiceEstado + 2, DETECTOR = indiceEstado + 3, NOTA = indiceEstado + 4
                
                Log.d("FragmentInspeccionEquipo", "Procesando inspección $numero: índice ESTADO=$indiceEstado")
                
                if (indiceEstado < filaEquipo.size) {
                    var estado = filaEquipo.getOrNull(indiceEstado)?.toString()?.trim() ?: ""
                    var fecha = filaEquipo.getOrNull(indiceEstado + 1)?.toString()?.trim() ?: ""
                    var inspector = filaEquipo.getOrNull(indiceEstado + 2)?.toString()?.trim() ?: ""
                    var detector = filaEquipo.getOrNull(indiceEstado + 3)?.toString()?.trim() ?: ""
                    var nota = filaEquipo.getOrNull(indiceEstado + 4)?.toString()?.trim() ?: ""
                    
                    // Filtrar #REF! - convertir a vacío
                    if (estado == "#REF!") estado = ""
                    if (fecha == "#REF!") fecha = ""
                    if (nota == "#REF!") nota = ""
                    
                    Log.d("FragmentInspeccionEquipo", "Inspección $numero: estado='$estado', fecha='$fecha', nota='$nota'")
                    
                    if (estado.isNotEmpty() || fecha.isNotEmpty() || nota.isNotEmpty()) {
                        historial.add(HistorialInspeccion(numero, fecha, estado, nota))
                    }
                } else {
                    Log.w("FragmentInspeccionEquipo", "Índice ESTADO $indiceEstado fuera de rango para inspección $numero")
                }
            }
            
            historial
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "Error obteniendo historial: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun mostrarHistorialPaginado() {
        // Limpiar tabla existente (mantener header)
        val childCount = tablaHistorial.childCount
        for (i in childCount - 1 downTo 1) { // Mantener el header (índice 0)
            tablaHistorial.removeViewAt(i)
        }
        
        // Mostrar solo los primeros elementos según el límite
        historialMostrado.clear()
        val limite = if (historialMostrado.isEmpty()) LIMITE_INICIAL else historialCompleto.size
        historialMostrado.addAll(historialCompleto.take(limite))
        
        // Mostrar los elementos
        mostrarHistorialEnTabla(historialMostrado)
        
        // Mostrar/ocultar botón "Cargar más"
        if (historialCompleto.size > historialMostrado.size) {
            btnCargarMasHistorial.visibility = View.VISIBLE
        btnCargarMasHistorial.setOnClickListener {
            cargarMasHistorial()
        }
        
        btnCargarMasReparaciones.setOnClickListener {
            cargarMasReparaciones()
        }
        } else {
            btnCargarMasHistorial.visibility = View.GONE
        }
    }
    
    private fun cargarMasHistorial() {
        // Mostrar todos los elementos
        historialMostrado.clear()
        historialMostrado.addAll(historialCompleto)
        
        // Actualizar la tabla
        mostrarHistorialEnTabla(historialMostrado)
        
        // Ocultar botón
        btnCargarMasHistorial.visibility = View.GONE
    }
    
    private fun cargarReparacionesEquipo() {
        Log.d("FragmentInspeccionEquipo", "Cargando reparaciones del equipo")
        
        // Limpiar tabla existente (mantener header)
        val childCount = tablaReparaciones.childCount
        for (i in 1 until childCount) { // Empezar desde 1 para mantener el header
            tablaReparaciones.removeViewAt(1)
        }
        
        // Obtener configuración de reparaciones (usar hoja de reparaciones)
        val prefs = requireContext().getSharedPreferences("configuracion_inspeccion", Context.MODE_PRIVATE)
        val libroId = prefs.getString("libro_id", null) ?: prefs.getString("db_libro_id", null)
        val hojaReparaciones = prefs.getString("hoja_reparaciones", null) ?: prefs.getString("db_reparaciones_hoja", "REPARACIONES")
        
        Log.d("FragmentInspeccionEquipo", "=== DIAGNÓSTICO REPARACIONES ===")
        Log.d("FragmentInspeccionEquipo", "Libro ID: $libroId")
        Log.d("FragmentInspeccionEquipo", "Hoja Reparaciones: $hojaReparaciones")
        Log.d("FragmentInspeccionEquipo", "Equipo ID: ${equipo?.id}")
        
        if (libroId == null) {
            mostrarMensajeErrorReparaciones("Configura primero el libro en la pantalla de configuración")
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sheetsManager = obtenerGoogleSheetsManager()
                if (sheetsManager != null) {
                    val reparaciones = obtenerReparacionesEquipo(sheetsManager, libroId, hojaReparaciones ?: "REPARACIONES", equipo?.id ?: "")
                    
                    withContext(Dispatchers.Main) {
                        if (reparaciones.isEmpty()) {
                            mostrarMensajeErrorReparaciones("No se encontraron reparaciones para este equipo")
                        } else {
                            // Ordenar por fecha descendente (más recientes primero)
                            reparacionesCompleto.clear()
                            reparacionesCompleto.addAll(reparaciones.sortedByDescending { it.fecha })
                            
                            // Mostrar paginado
                            mostrarReparacionesPaginado()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        mostrarMensajeErrorReparaciones("Error de configuración de Google Sheets")
                    }
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionEquipo", "Error cargando reparaciones: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    mostrarMensajeErrorReparaciones("Error cargando reparaciones: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun obtenerReparacionesEquipo(
        sheetsManager: GoogleSheetsManager,
        libroId: String,
        hoja: String,
        equipoId: String
    ): List<ReparacionEquipo> {
        Log.d("FragmentInspeccionEquipo", "Obteniendo reparaciones para equipo: $equipoId desde hoja: $hoja")
        
        try {
            // Leer toda la hoja de reparaciones
            // Estructura esperada: Fila 1 (títulos), Fila 2 (cabeceras), Fila 3+ (datos)
            val range = "$hoja!A:ZZ"
            val data = sheetsManager.leerRango(libroId, range)
            
            if (data.isEmpty() || data.size < 2) {
                Log.w("FragmentInspeccionEquipo", "No se encontraron datos en la hoja $hoja")
                return emptyList()
            }
            
            // Leer cabeceras de la fila 2 (índice 1)
            val headerRow = if (data.size > 1) data[1] else emptyList<Any?>()
            
            // Buscar índice de columna TAG (normalmente columna B, índice 1)
            val tagIndex = headerRow.indexOfFirst { 
                it?.toString()?.uppercase()?.trim()?.contains("TAG", ignoreCase = true) == true 
            }.takeIf { it >= 0 } ?: 1 // Por defecto columna B (índice 1)
            
            // Buscar índice de columna FECHA
            val fechaIndex = headerRow.indexOfFirst { 
                it?.toString()?.uppercase()?.trim()?.contains("FECHA", ignoreCase = true) == true 
            }.takeIf { it >= 0 } ?: -1
            
            // Buscar índice de columna REPARACION
            val reparacionIndex = headerRow.indexOfFirst { 
                it?.toString()?.uppercase()?.trim()?.let { header ->
                    header.contains("REPARACION", ignoreCase = true) || 
                    header.contains("DESCRIPCION", ignoreCase = true) ||
                    header.contains("OBSERVACION", ignoreCase = true)
                } == true
            }.takeIf { it >= 0 } ?: -1
            
            Log.d("FragmentInspeccionEquipo", "Índices encontrados: TAG=$tagIndex, FECHA=$fechaIndex, REPARACION=$reparacionIndex")
            
            // Buscar todas las filas donde el TAG coincida con el equipoId (empezar desde fila 3, índice 2)
            val reparaciones = mutableListOf<ReparacionEquipo>()
            
            for (i in 2 until data.size) {
                val fila = data[i]
                if (fila.size > tagIndex) {
                    val tag = fila.getOrNull(tagIndex)?.toString()?.trim() ?: ""
                    if (tag.equals(equipoId, ignoreCase = true)) {
                        val fecha = if (fechaIndex >= 0 && fechaIndex < fila.size) {
                            fila.getOrNull(fechaIndex)?.toString()?.trim() ?: ""
                        } else ""
                        
                        val reparacion = if (reparacionIndex >= 0 && reparacionIndex < fila.size) {
                            fila.getOrNull(reparacionIndex)?.toString()?.trim() ?: ""
                        } else ""
                        
                        if (fecha.isNotEmpty() || reparacion.isNotEmpty()) {
                        reparaciones.add(
                            ReparacionEquipo(
                                    id = "rep_${i}_${reparaciones.size}",
                                    fecha = fecha,
                                    tipo = "", // No hay tipo en la hoja de reparaciones
                                    descripcion = reparacion
                            )
                        )
                            Log.d("FragmentInspeccionEquipo", "Reparación encontrada: fecha=$fecha, reparacion=$reparacion")
                        }
                    }
                }
            }
            
            Log.d("FragmentInspeccionEquipo", "Total reparaciones encontradas: ${reparaciones.size}")
            return reparaciones
            
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "Error obteniendo reparaciones: ${e.message}", e)
            return emptyList()
        }
    }
    
    private fun mostrarReparacionesPaginado() {
        // Limpiar tabla (mantener header)
        val childCount = tablaReparaciones.childCount
        for (i in 1 until childCount) {
            tablaReparaciones.removeViewAt(1)
        }
        
        // Mostrar solo los primeros elementos
        reparacionesMostrado.clear()
        val limiteInicial = 5
        val elementosAMostrar = minOf(limiteInicial, reparacionesCompleto.size)
        reparacionesMostrado.addAll(reparacionesCompleto.take(elementosAMostrar))
        
        // Mostrar en tabla
        mostrarReparacionesEnTabla(reparacionesMostrado)
        
        // Mostrar/ocultar botón "Cargar más"
        if (reparacionesCompleto.size > reparacionesMostrado.size) {
            btnCargarMasReparaciones.visibility = View.VISIBLE
        } else {
            btnCargarMasReparaciones.visibility = View.GONE
        }
    }
    
    private fun mostrarReparacionesEnTabla(reparaciones: List<ReparacionEquipo>) {
        reparaciones.forEach { reparacion ->
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 12, 16, 12)
                setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
            
            // Fecha (formato DD/MM/AA)
            val tvFecha = TextView(requireContext()).apply {
                text = formatearFecha(reparacion.fecha)
                setTextColor(resources.getColor(android.R.color.white, null))
                textSize = 12f
                gravity = android.view.Gravity.CENTER or android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
                setPadding(8, 0, 8, 0)
            }
            
            // Reparación (descripción)
            val tvReparacion = TextView(requireContext()).apply {
                text = reparacion.descripcion
                setTextColor(resources.getColor(android.R.color.white, null))
                textSize = 12f
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                setPadding(8, 0, 8, 0)
            }
            
            fila.addView(tvFecha)
            fila.addView(tvReparacion)
            
            // Divisor horizontal
            val divisor = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    setMargins(16, 4, 16, 4)
                }
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }
            
            tablaReparaciones.addView(fila)
            tablaReparaciones.addView(divisor)
        }
    }
    
    private fun cargarMasReparaciones() {
        // Mostrar todos los elementos
        reparacionesMostrado.clear()
        reparacionesMostrado.addAll(reparacionesCompleto)
        
        // Actualizar la tabla
        mostrarReparacionesEnTabla(reparacionesMostrado)
        
        // Ocultar el botón
        btnCargarMasReparaciones.visibility = View.GONE
    }
    
    private fun mostrarMensajeErrorReparaciones(mensaje: String) {
        val filaError = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }
        
        val tvError = TextView(requireContext()).apply {
            text = mensaje
            setTextColor(resources.getColor(android.R.color.white, null))
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        filaError.addView(tvError)
        tablaReparaciones.addView(filaError)
    }
    
    private suspend fun guardarInspeccionEnGoogleSheets(equipo: Equipo) {
        try {
            val sharedPrefs = requireContext().getSharedPreferences("configuracion_inspeccion", Context.MODE_PRIVATE)
            // Usar nuevas claves del sistema simplificado
            val libroId = sharedPrefs.getString("libro_id", null) ?: sharedPrefs.getString("db_libro_id", null)
            // Para guardar, usar la hoja de inspección actual, no FLOTA
            val hoja = sharedPrefs.getString("hoja_inspeccion", null) ?: sharedPrefs.getString("db_inspecciones_hoja", "FLOTA")
            val numeroInspeccion = sharedPrefs.getString("numero_inspeccion", "25")
            
            if (libroId == null) {
                Log.w("FragmentInspeccionEquipo", "No hay libro DB inspecciones configurado para guardar")
                return
            }
            
            Log.d("FragmentInspeccionEquipo", "Guardando inspección en Google Sheets:")
            Log.d("FragmentInspeccionEquipo", "  - Libro ID: $libroId")
            Log.d("FragmentInspeccionEquipo", "  - Hoja: $hoja")
            Log.d("FragmentInspeccionEquipo", "  - Número inspección: $numeroInspeccion")
            Log.d("FragmentInspeccionEquipo", "  - Equipo ID: ${equipo.id}")
            
            val sheetsManager = obtenerGoogleSheetsManager()
            if (sheetsManager == null) {
                Log.e("FragmentInspeccionEquipo", "No se pudo crear GoogleSheetsManager para guardar")
                return
            }
            
            // Leer toda la hoja para encontrar la fila del equipo
            val data = sheetsManager.leerRango(libroId, "$hoja!A:ZZ")
            if (data.isEmpty()) {
                Log.w("FragmentInspeccionEquipo", "No se encontraron datos en la hoja para guardar")
                return
            }
            
            // Buscar la fila del equipo (columna B = TAG)
            // Fila 1 = índice 0 (títulos), Fila 2 = índice 1 (cabeceras), Fila 3+ = índice 2+ (datos)
            var filaEquipo = -1
            for (i in 2 until data.size) { // Empezar desde fila 3 (índice 2) después de cabeceras en fila 2
                val fila = data[i]
                if (fila.size > 1) {
                    val tag = fila[1]?.toString()?.trim()
                    if (tag == equipo.id) {
                        filaEquipo = i + 1 // +1 porque las filas en Google Sheets empiezan en 1
                        break
                    }
                }
            }
            
            if (filaEquipo == -1) {
                Log.w("FragmentInspeccionEquipo", "No se encontró la fila del equipo ${equipo.id} en la hoja")
                return
            }
            
            // Leer fila 2 (índice 1) para encontrar la columna del número de inspección
            // Las cabeceras están en la fila 2
            val fila2 = data[1] // Fila 2 (índice 1) - cabeceras
            var columnaInspeccion = -1
            
            for (i in fila2.indices) {
                val valor = fila2[i]?.toString()?.trim()
                if (valor == numeroInspeccion) {
                    columnaInspeccion = i + 1 // +1 porque las columnas en Google Sheets empiezan en 1
                    break
                }
            }
            
            if (columnaInspeccion == -1) {
                Log.w("FragmentInspeccionEquipo", "No se encontró la columna para inspección $numeroInspeccion")
                return
            }
            
            // Calcular las columnas donde escribir los datos
            val columnaEstado = columnaInspeccion + 1
            val columnaFecha = columnaInspeccion + 2
            val columnaNota = columnaInspeccion + 3
            val columnaInspector = columnaInspeccion + 4
            val columnaDetector = columnaInspeccion + 5
            
            Log.d("FragmentInspeccionEquipo", "Guardando en fila $filaEquipo:")
            Log.d("FragmentInspeccionEquipo", "  - Estado en columna $columnaEstado")
            Log.d("FragmentInspeccionEquipo", "  - Fecha en columna $columnaFecha")
            Log.d("FragmentInspeccionEquipo", "  - Nota en columna $columnaNota")
            Log.d("FragmentInspeccionEquipo", "  - Inspector en columna $columnaInspector")
            Log.d("FragmentInspeccionEquipo", "  - Detector en columna $columnaDetector")
            
            // Convertir números de columna a letras (A=1, B=2, etc.)
            fun numeroAColumna(numero: Int): String {
                var resultado = ""
                var n = numero - 1
                while (n >= 0) {
                    resultado = ('A' + (n % 26)) + resultado
                    n = n / 26 - 1
                }
                return resultado
            }
            
            val columnaEstadoLetra = numeroAColumna(columnaEstado)
            val columnaFechaLetra = numeroAColumna(columnaFecha)
            val columnaNotaLetra = numeroAColumna(columnaNota)
            val columnaInspectorLetra = numeroAColumna(columnaInspector)
            val columnaDetectorLetra = numeroAColumna(columnaDetector)
            
            // Escribir los datos en Google Sheets
            val rangoEstado = "$hoja!$columnaEstadoLetra$filaEquipo"
            val rangoFecha = "$hoja!$columnaFechaLetra$filaEquipo"
            val rangoNota = "$hoja!$columnaNotaLetra$filaEquipo"
            val rangoInspector = "$hoja!$columnaInspectorLetra$filaEquipo"
            val rangoDetector = "$hoja!$columnaDetectorLetra$filaEquipo"
            
            Log.d("FragmentInspeccionEquipo", "Escribiendo datos:")
            Log.d("FragmentInspeccionEquipo", "  - Estado '$rangoEstado': ${equipo.estado}")
            Log.d("FragmentInspeccionEquipo", "  - Fecha '$rangoFecha': ${equipo.fechaInspeccion}")
            Log.d("FragmentInspeccionEquipo", "  - Nota '$rangoNota': ${equipo.nota}")
            Log.d("FragmentInspeccionEquipo", "  - Inspector '$rangoInspector': ${equipo.identidadInspector}")
            Log.d("FragmentInspeccionEquipo", "  - Detector '$rangoDetector': ${equipo.detectorUtilizado}")
            
            // Escribir cada campo por separado
            sheetsManager.escribirRango(libroId, rangoEstado, listOf(listOf(equipo.estado)))
            sheetsManager.escribirRango(libroId, rangoFecha, listOf(listOf(equipo.fechaInspeccion ?: "")))
            sheetsManager.escribirRango(libroId, rangoNota, listOf(listOf(equipo.nota ?: "")))
            sheetsManager.escribirRango(libroId, rangoInspector, listOf(listOf(equipo.identidadInspector ?: "")))
            sheetsManager.escribirRango(libroId, rangoDetector, listOf(listOf(equipo.detectorUtilizado ?: "")))
            
            Log.d("FragmentInspeccionEquipo", "✅ Inspección guardada exitosamente en Google Sheets")
            
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "❌ Error guardando inspección en Google Sheets: ${e.message}", e)
        }
    }
    
    private suspend fun cargarEquiposDesdeBaseDatos() {
        Log.d("FragmentInspeccionEquipo", "Cargando equipos desde base de datos local")
        
        try {
            // Cargar equipos desde la base de datos local
            val equiposBD = withContext(Dispatchers.IO) {
                inspeccionDao.getAllEquipos()
            }
            
            if (equiposBD.isEmpty()) {
                Log.w("FragmentInspeccionEquipo", "No se encontraron equipos en la base de datos local")
                return
            }
            
            Log.d("FragmentInspeccionEquipo", "Equipos cargados desde BD: ${equiposBD.size}")
            
            // Buscar y mostrar datos específicos del equipo A-00002
            val equipoA00002 = equiposBD.find { it.id == "A-00002" }
            equipoA00002?.let { eq ->
                Log.d("FragmentInspeccionEquipo", "=== DATOS BD PARA A-00002 ===")
                Log.d("FragmentInspeccionEquipo", "id=${eq.id}")
                Log.d("FragmentInspeccionEquipo", "area='${eq.area}' (null=${eq.area == null}, empty=${eq.area?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "unidad='${eq.unidad}' (null=${eq.unidad == null}, empty=${eq.unidad?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "instalacion='${eq.instalacion}' (null=${eq.instalacion == null}, empty=${eq.instalacion?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "ubicacion='${eq.ubicacion}' (null=${eq.ubicacion == null}, empty=${eq.ubicacion?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "linea='${eq.linea}' (null=${eq.linea == null}, empty=${eq.linea?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "marca='${eq.marca}' (null=${eq.marca == null}, empty=${eq.marca?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "modelo='${eq.modelo}' (null=${eq.modelo == null}, empty=${eq.modelo?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "tipo='${eq.tipo}' (null=${eq.tipo == null}, empty=${eq.tipo?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "estado='${eq.estado}' (null=${eq.estado == null}, empty=${eq.estado?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "flota='${eq.flota}' (null=${eq.flota == null}, empty=${eq.flota?.isEmpty() == true})")
                Log.d("FragmentInspeccionEquipo", "=== FIN DATOS BD A-00002 ===")
            } ?: Log.w("FragmentInspeccionEquipo", "⚠️ Equipo A-00002 NO encontrado en BD")
            
            // Actualizar la lista de equipos
            equipos.clear()
            equipos.addAll(equiposBD)
            estadosUnicos = equipos.mapNotNull { it.estado }.distinct().sorted()
            
            Log.d("FragmentInspeccionEquipo", "Equipos cargados desde BD: ${equipos.size}")
            
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "Error cargando equipos desde BD: ${e.message}", e)
        }
    }
    
    private suspend fun cargarEquiposDesdeGoogleSheets() {
        Log.d("FragmentInspeccionEquipo", "Cargando equipos desde Google Sheets")
        
        try {
            // Obtener configuración de Google Sheets (usar nuevas claves)
            val prefs = requireContext().getSharedPreferences("configuracion_inspeccion", Context.MODE_PRIVATE)
            val libroId = prefs.getString("libro_id", null) ?: prefs.getString("db_libro_id", null)
            // Para cargar equipos, usar hoja FLOTA
            val hoja = prefs.getString("hoja_flota", null) ?: prefs.getString("db_inspecciones_hoja", "FLOTA")
            val numeroInspeccion = prefs.getString("numero_inspeccion", "25")
            
            Log.d("FragmentInspeccionEquipo", "=== CONFIGURACIÓN GOOGLE SHEETS ===")
            Log.d("FragmentInspeccionEquipo", "Libro ID: $libroId")
            Log.d("FragmentInspeccionEquipo", "Hoja: $hoja")
            Log.d("FragmentInspeccionEquipo", "Número inspección: $numeroInspeccion")
            
            if (libroId == null) {
                Log.e("FragmentInspeccionEquipo", "No hay libro Google Sheets configurado")
                return
            }
            
            // Ejecutar en hilo de IO para evitar deadlock
            val sheetsManager = withContext(Dispatchers.IO) {
                obtenerGoogleSheetsManager()
            }
            if (sheetsManager == null) {
                Log.e("FragmentInspeccionEquipo", "No se pudo crear GoogleSheetsManager")
                return
            }
            
            // Leer cabeceras de la hoja (fila 2)
            val headerResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                .get(libroId, "$hoja!A2:ZZ2")
                .execute()
            val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList()
            
            Log.d("FragmentInspeccionEquipo", "Cabeceras encontradas: ${headerRow.joinToString(", ")}")
            
            // Leer datos desde la hoja (desde fila 3, después de las cabeceras en fila 2)
            val dataResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                .get(libroId, "$hoja!A3:ZZ")
                .execute()
            val dataRows = dataResponse.getValues() ?: emptyList()
            
            if (dataRows.isEmpty()) {
                Log.w("FragmentInspeccionEquipo", "No se encontraron datos en la hoja")
                return
            }
            
            Log.d("FragmentInspeccionEquipo", "Datos obtenidos: ${dataRows.size} filas con ${headerRow.size} columnas")
            
            // Procesar datos para crear equipos usando el mapeo de columnas
            val equiposGS = mutableListOf<Equipo>()
            
            // Cargar mapeo de columnas desde assets
            val mapeoJson = requireContext().assets.open("mapeo_columnas.json").bufferedReader().use { it.readText() }
            val mapeoColumnas = JSONObject(mapeoJson)
            
            // Leer fila 2 (cabeceras) para obtener números de inspección
            // dataRows[0] corresponde a la fila 3 del spreadsheet, pero necesitamos la fila 2 (cabeceras)
            // Vamos a leer la fila 2 directamente desde headerRow que ya tenemos
            val fila2 = headerRow // Fila 2 (cabeceras) ya la tenemos en headerRow
            val numerosConIndices = mutableMapOf<Int, Int>() // número -> índice
            
            for (i in fila2.indices) {
                val valor = fila2[i]?.toString()?.trim()
                if (valor != null && valor.matches(Regex("\\d+"))) {
                    val numero = valor.toIntOrNull()
                    if (numero != null && numero in 25..50) {
                        numerosConIndices[numero] = i
                        Log.d("FragmentInspeccionEquipo", "Número de inspección encontrado: $numero en columna $i")
                    }
                }
            }
            
            // Función para obtener valor de una fila usando el mapeo de columnas
            fun obtenerValor(fila: List<Any?>, campo: String): String {
                val columnaMapeada = mapeoColumnas.optString(campo, "")
                val indiceColumna = headerRow.indexOf(columnaMapeada)
                return if (indiceColumna != -1 && fila.size > indiceColumna) {
                    fila[indiceColumna]?.toString()?.trim() ?: ""
                } else ""
            }
            
            // Procesar cada fila de datos
            for (rowIndex in dataRows.indices) {
                val fila = dataRows[rowIndex]
                if (fila.size <= 1) continue // Saltar filas vacías
                
                val equipoId = obtenerValor(fila, "id")
                if (equipoId.isEmpty()) continue
                
                // Obtener datos básicos del equipo usando el mapeo de columnas
                val area = obtenerValor(fila, "area")
                val unidad = obtenerValor(fila, "unidad")
                val manifold = obtenerValor(fila, "manifold")
                val lineaEquipo = obtenerValor(fila, "lineaEquipo")
                val ubicacion = obtenerValor(fila, "ubicacion")
                val gpsCoord = obtenerValor(fila, "gpsCoord")
                val gpsAcc = obtenerValor(fila, "gpsAcc")
                val marca = obtenerValor(fila, "marca")
                val modelo = obtenerValor(fila, "modelo")
                val tipo = obtenerValor(fila, "tipo")
                val diametro = obtenerValor(fila, "diametro")
                val conexion = obtenerValor(fila, "conexion")
                val presEntrada = obtenerValor(fila, "presEntrada")
                val presSalida = obtenerValor(fila, "presSalida")
                val byPass = obtenerValor(fila, "byPass")
                val aislamiento = obtenerValor(fila, "aislamiento")
                val descarga = obtenerValor(fila, "descarga")
                val aplicacion = obtenerValor(fila, "aplicacion")
                val foto = obtenerValor(fila, "foto")
                val fotoUbic = obtenerValor(fila, "fotoUbic")
                val fotoMf = obtenerValor(fila, "fotoMf")
                val fotoExtra = obtenerValor(fila, "fotoExtra")
                val orden = obtenerValor(fila, "orden")
                val ordenJuan = obtenerValor(fila, "ordenJuan")
                val ordenPaco = obtenerValor(fila, "ordenPaco")
                val ordenTome = obtenerValor(fila, "ordenTome")
                val badActors = obtenerValor(fila, "badActors")
                val modificaciones = obtenerValor(fila, "modificaciones")
                
                // Buscar la última inspección con datos para este equipo
                var ultimoEstado = ""
                var ultimaFecha = ""
                var ultimaNota = ""
                var ultimoInspector = ""
                var ultimoDetector = ""
                
                // Buscar en todas las inspecciones disponibles, de la más reciente a la más antigua
                val inspeccionesOrdenadas = numerosConIndices.keys.sortedDescending()
                
                for (numeroInspeccionActual in inspeccionesOrdenadas) {
                    val indiceEstado = numerosConIndices[numeroInspeccionActual] ?: continue
                    // Las columnas están en orden: ESTADO, FECHA, INSPECTOR, DETECTOR, NOTA
                    // Entonces: ESTADO = indiceEstado, FECHA = indiceEstado + 1, INSPECTOR = indiceEstado + 2, DETECTOR = indiceEstado + 3, NOTA = indiceEstado + 4
                    
                    val estado = fila.getOrNull(indiceEstado)?.toString()?.trim() ?: ""
                    val fecha = fila.getOrNull(indiceEstado + 1)?.toString()?.trim() ?: ""
                    val inspector = fila.getOrNull(indiceEstado + 2)?.toString()?.trim() ?: ""
                    val detector = fila.getOrNull(indiceEstado + 3)?.toString()?.trim() ?: ""
                    val nota = fila.getOrNull(indiceEstado + 4)?.toString()?.trim() ?: ""
                    
                    // Si encontramos datos en esta inspección, los usamos
                    if (estado.isNotBlank() || fecha.isNotBlank() || nota.isNotBlank()) {
                        ultimoEstado = estado
                        ultimaFecha = fecha
                        ultimaNota = nota
                        ultimoInspector = inspector
                        ultimoDetector = detector
                        Log.d("FragmentInspeccionEquipo", "Equipo $equipoId: usando datos de inspección $numeroInspeccionActual")
                        break
                    }
                }
                
                // Filtrar #REF!
                val estadoLimpio = if (ultimoEstado == "#REF!") "" else ultimoEstado
                val fechaLimpia = if (ultimaFecha == "#REF!") "" else ultimaFecha
                val notaLimpia = if (ultimaNota == "#REF!") "" else ultimaNota
                val inspectorLimpio = if (ultimoInspector == "#REF!") "" else ultimoInspector
                val detectorLimpio = if (ultimoDetector == "#REF!") "" else ultimoDetector
                
                // Crear equipo con datos mapeados correctamente
                val equipo = Equipo(
                    id = equipoId,
                    instalacion = manifold,
                    unidad = unidad,
                    area = area,
                    linea = lineaEquipo,
                    marca = marca,
                    modelo = modelo,
                    tipo = tipo,
                    periodicidad = "", // No está en el mapeo actual
                    diametro = diametro,
                    conexion = conexion,
                    aislamiento = aislamiento,
                    presEntrada = presEntrada,
                    presSalida = presSalida,
                    byPass = byPass.equals("true", ignoreCase = true),
                    descarga = descarga,
                    aplicacion = aplicacion,
                    servicio = "", // No está en el mapeo actual
                    ubicacion = ubicacion,
                    estado = estadoLimpio,
                    flota = null, // FLOTA se obtiene de la columna FLOTA en la importación
                    fechaInspeccion = fechaLimpia,
                    nota = notaLimpia,
                    identidadInspector = inspectorLimpio,
                    detectorUtilizado = detectorLimpio,
                    incidencias = "",
                    gpsCoord = "",
                    urlFotoEquipo = null,
                    urlFotoUbicacion = null,
                    orden = 0.0,
                    gpsAcc = "",
                    extra = null,
                    modificadoLocal = false,
                    instalacionMf = "",
                    urlFotoManifold = null,
                    urlFotosExtra = null
                )
                
                equiposGS.add(equipo)
            }
            
            // Actualizar la lista de equipos
            equipos.clear()
            equipos.addAll(equiposGS)
            estadosUnicos = equipos.mapNotNull { it.estado }.distinct().sorted()
            
            Log.d("FragmentInspeccionEquipo", "Equipos cargados desde Google Sheets: ${equipos.size}")
            
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "Error cargando equipos desde Google Sheets: ${e.message}", e)
        }
    }
    
    private fun mostrarHistorialEnTabla(historial: List<HistorialInspeccion>) {
        Log.d("FragmentInspeccionEquipo", "Mostrando ${historial.size} registros de historial")
        
        for (registro in historial) {
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 12, 16, 12) // Más espaciado interno
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 0 // Sin margen inferior
                }
            }
            
            // Columna 1: Fecha (20%) - Sin paréntesis y año corto
            val fechaFormateada = formatearFecha(registro.fecha)
            val tvFecha = TextView(requireContext()).apply {
                text = fechaFormateada
                setTextColor(resources.getColor(android.R.color.white, null))
                textSize = 12f
                gravity = android.view.Gravity.CENTER or android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
                setPadding(8, 0, 8, 0) // Espaciado entre columnas
            }
            
            // Columna 2: Estado (30%) - Con color de fondo
            val tvEstado = TextView(requireContext()).apply {
                text = registro.estado
                textSize = 12f
                gravity = android.view.Gravity.CENTER or android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
                setPadding(8, 0, 8, 0) // Espaciado entre columnas
                
                // Aplicar color de fondo según el estado
                if (registro.estado.isNotEmpty()) {
                    val colorFondo = coloresEstados[registro.estado.uppercase().trim()] ?: android.graphics.Color.GRAY
                    val colorTexto = if (esColorClaro(colorFondo)) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                    
                    setTextColor(colorTexto)
                    
                    // Crear drawable con color de fondo y bordes redondeados
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 8f
                        setColor(colorFondo)
                    }
                    background = drawable
                } else {
                    setTextColor(resources.getColor(android.R.color.white, null))
                }
            }
            
            // Columna 3: Notas (45%)
            val tvNotas = TextView(requireContext()).apply {
                text = registro.nota
                setTextColor(resources.getColor(android.R.color.white, null))
                textSize = 12f
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.45f)
                setPadding(8, 0, 8, 0) // Espaciado entre columnas
            }
            
            fila.addView(tvFecha)
            fila.addView(tvEstado)
            fila.addView(tvNotas)
            
            // Añadir línea divisoria horizontal (solo líneas, sin bordes redondeados)
            val divisor = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    setMargins(16, 0, 16, 0)
                }
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }
            
            tablaHistorial.addView(fila)
            tablaHistorial.addView(divisor)
        }
    }
    
    private fun formatearFecha(fecha: String): String {
        if (fecha.isEmpty()) return ""
        
        try {
            // Intentar parsear diferentes formatos de fecha
            val formatos = listOf(
                "dd/MM/yyyy",
                "dd/MM/yy", 
                "dd-MM-yyyy",
                "dd-MM-yy",
                "yyyy-MM-dd"
            )
            
            for (formato in formatos) {
                try {
                    val dateFormat = java.text.SimpleDateFormat(formato, java.util.Locale.getDefault())
                    val fechaParsed = dateFormat.parse(fecha)
                    if (fechaParsed != null) {
                        // Convertir a formato dd/MM/yy (año corto)
                        val formatoSalida = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                        return formatoSalida.format(fechaParsed)
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente formato
                }
            }
            
            // Si no se puede parsear, devolver la fecha original
            return fecha
        } catch (e: Exception) {
            return fecha
        }
    }
    
    data class HistorialInspeccion(
        val numero: Int,
        val fecha: String,
        val estado: String,
        val nota: String
    )
    
    data class ReparacionEquipo(
        val id: String,
        val fecha: String,
        val tipo: String,
        val descripcion: String
    )
    
    private fun mostrarMensajeError(mensaje: String) {
        val filaError = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            background = resources.getDrawable(R.drawable.bg_edittext_dark, null)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4
            }
        }
        
        val tvError = TextView(requireContext()).apply {
            text = mensaje
            setTextColor(resources.getColor(android.R.color.holo_red_light, null))
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        filaError.addView(tvError)
        tablaHistorial.addView(filaError)
    }
    
    private fun obtenerGoogleSheetsManager(): GoogleSheetsManager? {
        return try {
            val context = requireContext()
            val settingsManager = SettingsManager(context)
            val settings = settingsManager.getSettings()
            
            val credentialsStream = if (settings.useOAuth) {
                null // Para OAuth no necesitamos credenciales
            } else {
                settingsManager.getCredentialsInputStream() // Para Service Account sí necesitamos credenciales
            }
            
            val authAdapter = GoogleAuthAdapter(
                context = context,
                useOAuth = settings.useOAuth,
                oAuthEmail = settings.oAuthEmail,
                credentialsStream = credentialsStream
            )
            
            GoogleSheetsManager(
                authAdapter = authAdapter,
                context = context
            )
        } catch (e: Exception) {
            Log.e("FragmentInspeccionEquipo", "Error creando GoogleSheetsManager: ${e.message}", e)
            null
        }
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
        
        // Listener para instalación - activar/desactivar nombre de manifold
        spinnerInstalacion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val instalacionSeleccionada = parent?.getItemAtPosition(position)?.toString() ?: ""
                Log.d("SpinnerDebug", "Instalación seleccionada: '$instalacionSeleccionada'")
                
                // Activar campo de nombre de manifold solo si es MANIFOLD VAPOR o MANIFOLD CONDENSADO
                val esManifold = instalacionSeleccionada == "MANIFOLD VAPOR" || instalacionSeleccionada == "MANIFOLD CONDENSADO"
                etNombreManifold.isEnabled = esManifold
                etNombreManifold.alpha = if (esManifold) 1.0f else 0.5f // Indicador visual
                
                if (!esManifold) {
                    // Limpiar el campo si no es manifold
                    etNombreManifold.setText("")
                }
                
                Log.d("SpinnerDebug", "Nombre de manifold ${if (esManifold) "activado" else "desactivado"}")
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