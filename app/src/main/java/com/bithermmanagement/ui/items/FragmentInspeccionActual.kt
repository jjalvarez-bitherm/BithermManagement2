package com.bithermmanagement.ui.items

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.dao.InspeccionDao
import com.bithermmanagement.database.entities.EquipoView
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FragmentInspeccionActual : Fragment() {
    private lateinit var recyclerPurgadores: RecyclerView
    private lateinit var equipoAdapter: EquipoAdapter
    private var equipos: List<EquipoView> = emptyList()
    private var equiposFiltrados: List<EquipoView> = emptyList()
    private var coloresEstados: Map<String, Int> = emptyMap()
    
    // Nuevos elementos de la UI
    private lateinit var etBuscar: EditText
    private lateinit var btnFiltroArea: Button
    private lateinit var btnFiltroUnidad: Button
    private lateinit var btnPlayPause: ImageButton
    private lateinit var tvFiltroEstado: TextView
    
    // Variables para filtros
    private var areasSeleccionadas: MutableSet<String> = mutableSetOf()
    private var unidadesSeleccionadas: MutableSet<String> = mutableSetOf()
    private var filtroVisualizacion: Int = 0 // 0=TODOS, 1=INSPECCIONADOS, 2=PDTE. INSPEC.
    private var mostrarAFS: Boolean = false
    private var mostrarELIM: Boolean = false
    private var mostrarMONIT: Boolean = false
    private var verInactivos: Boolean = false
    private var verMonitorizados: Boolean = false
    
    // Variables para el cronómetro
    private var cronometroActivo = false
    private var tiempoInicio: Long = 0
    private var tiempoAcumulado: Long = 0
    private var ultimaActividad: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val AUTO_PAUSE_DELAY = 5 * 60 * 1000L // 5 minutos
    
    // Opciones de filtro de visualización
    private val opcionesVisualizacion = listOf("TODOS", "INSPECCIONADOS", "PDTE. INSPEC.")

    companion object {
        private const val KEY_AREAS_SELECCIONADAS = "areas_seleccionadas"
        private const val KEY_UNIDADES_SELECCIONADAS = "unidades_seleccionadas"
        private const val KEY_FILTRO_VISUALIZACION = "filtro_visualizacion"
        private const val KEY_MOSTRAR_AFS = "mostrar_afs"
        private const val KEY_MOSTRAR_ELIM = "mostrar_elim"
        private const val KEY_MOSTRAR_MONIT = "mostrar_monit"
        private const val KEY_VER_INACTIVOS = "ver_inactivos"
        private const val KEY_VER_MONITORIZADOS = "ver_monitorizados"
        private const val KEY_TEXTO_BUSQUEDA = "texto_busqueda"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FragmentInspeccionActual", "onCreateView llamado")
        return inflater.inflate(R.layout.fragment_inspeccion_actual, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragmentInspeccionActual", "onViewCreated llamado")
        
        inicializarViews(view)
        configurarBarraSuperior(view)
        
        // Restaurar estado de filtros si existe
        if (savedInstanceState != null) {
            restaurarEstadoFiltros(savedInstanceState)
        }
        
        configurarNuevaBarraEstado()
        configurarCronometro()
        configurarFragmentResultListener()
        
        // Cargar colores de estados desde JSON
        coloresEstados = cargarColoresEstados()

        // Cargar equipos reales desde la base de datos
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val dao = db.inspeccionDao()
            val equiposCompletos = dao.getAllEquiposFull()
            Log.d("FragmentInspeccionActual", "=== CARGA DE DATOS ===")
            Log.d("FragmentInspeccionActual", "Total equipos en BD: ${equiposCompletos.size}")
            
            // Log detallado de los primeros 10 equipos para verificar estados
            equiposCompletos.take(10).forEachIndexed { index, equipo ->
                Log.d("FragmentInspeccionActual", "Equipo #${index + 1}: id=${equipo.id}, estado='${equipo.estado}', flota='${equipo.flota}', area='${equipo.area}', unidad='${equipo.unidad}'")
            }
            
            // Contar equipos con y sin estado
            val equiposConEstado = equiposCompletos.count { !it.estado.isNullOrEmpty() }
            val equiposSinEstado = equiposCompletos.count { it.estado.isNullOrEmpty() }
            Log.d("FragmentInspeccionActual", "Equipos con estado: $equiposConEstado, sin estado: $equiposSinEstado")
            
            // Obtener estados únicos
            val estadosUnicos = equiposCompletos.mapNotNull { it.estado }.filter { it.isNotEmpty() }.distinct()
            Log.d("FragmentInspeccionActual", "Estados únicos encontrados en BD: ${estadosUnicos.size} - ${estadosUnicos.take(10).joinToString(", ")}")
            
            // Convertir Equipo a EquipoView para el adapter
            equipos = equiposCompletos.map { equipo ->
                EquipoView(
                    id = equipo.id,
                    estado = equipo.estado,
                    flota = equipo.flota, // FLOTA: ACTIVO, MONITORIZADO, AFS, ELIMINADO (no se usa para colorear)
                    area = equipo.area,
                    unidad = equipo.unidad,
                    marca = equipo.marca,
                    modelo = equipo.modelo,
                    tipo = equipo.tipo,
                    diametro = equipo.diametro,
                    conexion = equipo.conexion,
                    presEntrada = equipo.presEntrada,
                    presSalida = equipo.presSalida,
                    descarga = equipo.descarga,
                    aplicacion = equipo.aplicacion,
                    servicio = equipo.servicio,
                    ubicacion = equipo.ubicacion,
                    fechasteado = equipo.fechaInspeccion,
                    nota = equipo.nota,
                    inspector = equipo.identidadInspector,
                    detector = equipo.detectorUtilizado,
                    incidencias = equipo.incidencias,
                    gps = equipo.gpsCoord,
                    gpsAcc = equipo.gpsAcc,
                    foto = equipo.urlFotoEquipo,
                    orden = equipo.orden,
                    instalacion = equipo.instalacion,
                    linea = equipo.linea,
                    aislamiento = equipo.aislamiento,
                    periodicidad = equipo.periodicidad,
                    byPass = equipo.byPass
                )
            }
            
            // Solo resetear filtros si es la primera vez (savedInstanceState == null)
            if (savedInstanceState == null) {
                areasSeleccionadas.clear()
                unidadesSeleccionadas.clear()
                filtroVisualizacion = 0 // TODOS
                mostrarAFS = false
                mostrarELIM = false
                mostrarMONIT = false
                verInactivos = false
                verMonitorizados = false
            }
            
            // Actualizar UI de filtros
            withContext(Dispatchers.Main) {
                if (savedInstanceState == null) {
                    etBuscar.setText("") // Limpiar búsqueda solo si es primera vez
                }
                actualizarTextoBotonFiltro(btnFiltroArea, areasSeleccionadas, "AREA")
                actualizarTextoBotonFiltro(btnFiltroUnidad, unidadesSeleccionadas, "UNIDAD")
                actualizarTextoFiltroEstado()
            }
            
            // Aplicar filtros (ya sea restaurados o por defecto)
            filtrarEquipos()
            
            Log.d("FragmentInspeccionActual", "=== FIN CARGA ===")
            Log.d("FragmentInspeccionActual", "Equipos cargados: ${equipos.size}, Equipos filtrados: ${equiposFiltrados.size}")
            Log.d("FragmentInspeccionActual", "Colores de estados cargados: ${coloresEstados.size} estados")
            Log.d("FragmentInspeccionActual", "Estados con color definido: ${coloresEstados.keys.joinToString(", ")}")
            
            val adapter = EquipoAdapter(requireContext(), equiposFiltrados, coloresEstados) { equipo ->
                // Navegación al fragmento de detalle
                Log.d("FragmentInspeccionActual", "CLICK en equipo desde adapter: id=${equipo.id}")
                
                val indiceSeleccionado = equiposCompletos.indexOfFirst { it.id == equipo.id }
                
                if (indiceSeleccionado == -1) {
                    Log.e("FragmentInspeccionActual", "ERROR: No se encontró el equipo ${equipo.id}")
                    return@EquipoAdapter
                }
                
                val fragment = FragmentInspeccionEquipo.newInstance(equipo.id)
                
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            
            equipoAdapter = adapter
            recyclerPurgadores.adapter = equipoAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guardar estado de filtros
        outState.putStringArrayList(KEY_AREAS_SELECCIONADAS, ArrayList(areasSeleccionadas))
        outState.putStringArrayList(KEY_UNIDADES_SELECCIONADAS, ArrayList(unidadesSeleccionadas))
        outState.putInt(KEY_FILTRO_VISUALIZACION, filtroVisualizacion)
        outState.putBoolean(KEY_MOSTRAR_AFS, mostrarAFS)
        outState.putBoolean(KEY_MOSTRAR_ELIM, mostrarELIM)
        outState.putBoolean(KEY_MOSTRAR_MONIT, mostrarMONIT)
        outState.putBoolean(KEY_VER_INACTIVOS, verInactivos)
        outState.putBoolean(KEY_VER_MONITORIZADOS, verMonitorizados)
        outState.putString(KEY_TEXTO_BUSQUEDA, etBuscar.text.toString())
    }

    private fun restaurarEstadoFiltros(savedInstanceState: Bundle) {
        areasSeleccionadas = (savedInstanceState.getStringArrayList(KEY_AREAS_SELECCIONADAS) ?: emptyList()).toMutableSet()
        unidadesSeleccionadas = (savedInstanceState.getStringArrayList(KEY_UNIDADES_SELECCIONADAS) ?: emptyList()).toMutableSet()
        filtroVisualizacion = savedInstanceState.getInt(KEY_FILTRO_VISUALIZACION, 0)
        mostrarAFS = savedInstanceState.getBoolean(KEY_MOSTRAR_AFS, false)
        mostrarELIM = savedInstanceState.getBoolean(KEY_MOSTRAR_ELIM, false)
        mostrarMONIT = savedInstanceState.getBoolean(KEY_MOSTRAR_MONIT, false)
        verInactivos = savedInstanceState.getBoolean(KEY_VER_INACTIVOS, false)
        verMonitorizados = savedInstanceState.getBoolean(KEY_VER_MONITORIZADOS, false)
        
        // Restaurar texto de búsqueda después de inicializar views
        val textoBusqueda = savedInstanceState.getString(KEY_TEXTO_BUSQUEDA, "")
        if (::etBuscar.isInitialized) {
            etBuscar.setText(textoBusqueda)
        }
    }

    private fun inicializarViews(view: View) {
        recyclerPurgadores = view.findViewById(R.id.recyclerPurgadores)
        recyclerPurgadores.layoutManager = LinearLayoutManager(requireContext())
        
        // Nuevos elementos de la UI
        etBuscar = view.findViewById(R.id.etBuscar)
        btnFiltroArea = view.findViewById(R.id.btnFiltroArea)
        btnFiltroUnidad = view.findViewById(R.id.btnFiltroUnidad)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        tvFiltroEstado = view.findViewById(R.id.tvFiltroEstado)
    }

    private fun configurarBarraSuperior(view: View) {
        val pestana = view.findViewById<View>(R.id.pestanaBarraSuperior)
        val iconoHamburguesaPestana = view.findViewById<View>(R.id.iconoHamburguesaPestana)
        val btnHamburger = view.findViewById<View>(R.id.btnHamburger)
        
        pestana.setOnClickListener {
            Log.d("FragmentInspeccionActual", "CLICK en pestanaBarraSuperior")
        }
        iconoHamburguesaPestana.setOnClickListener {
            Log.d("FragmentInspeccionActual", "CLICK en iconoHamburguesaPestana")
        }
        btnHamburger.setOnClickListener {
            Log.d("FragmentInspeccionActual", "CLICK en btnHamburger")
        }
    }

    private fun configurarNuevaBarraEstado() {
        // Configurar campo de búsqueda
        etBuscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filtrarEquipos()
            }
        })

        // Configurar botón de filtro ÁREA
        btnFiltroArea.setOnClickListener {
            mostrarDialogoFiltroMulti("Área", obtenerAreasUnicas()) { seleccionadas ->
                areasSeleccionadas = seleccionadas.toMutableSet()
                actualizarTextoBotonFiltro(btnFiltroArea, areasSeleccionadas, "AREA")
                filtrarEquipos()
            }
        }

        // Configurar botón de filtro UNIDAD
        btnFiltroUnidad.setOnClickListener {
            val unidades = obtenerUnidadesUnicas()
            mostrarDialogoFiltroMulti("Unidad", unidades) { seleccionadas ->
                unidadesSeleccionadas = seleccionadas.toMutableSet()
                actualizarTextoBotonFiltro(btnFiltroUnidad, unidadesSeleccionadas, "UNIDAD")
                filtrarEquipos()
            }
        }

        // Configurar filtro de estado
        tvFiltroEstado.setOnClickListener {
            mostrarDialogoFiltroEstado()
        }
    }

    private fun configurarCronometro() {
        btnPlayPause.setOnClickListener {
            if (cronometroActivo) {
                pausarCronometro()
            } else {
                iniciarCronometro()
            }
        }
    }

    private fun mostrarDialogoFiltroMulti(titulo: String, opciones: List<String>, onSeleccion: (Set<String>) -> Unit) {
        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_multi_select_filter, null)
        
        // Obtener referencias a los elementos del layout
        val txtTitulo = dialogView.findViewById<TextView>(R.id.txtTituloFiltro)
        val etBuscarFiltro = dialogView.findViewById<EditText>(R.id.etBuscarFiltro)
        val recyclerOpciones = dialogView.findViewById<RecyclerView>(R.id.recyclerOpcionesFiltro)
        val checkboxVerInactivos = dialogView.findViewById<CheckBox>(R.id.checkboxVerInactivos)
        val checkboxVerMonitorizados = dialogView.findViewById<CheckBox>(R.id.checkboxVerMonitorizados)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarFiltro)
        val btnAceptar = dialogView.findViewById<Button>(R.id.btnAceptarFiltro)
        
        // Configurar título
        txtTitulo.text = titulo
        
        // Configurar selección inicial según el tipo de filtro
        val seleccionInicial = when (titulo) {
            "Área" -> areasSeleccionadas.toMutableSet()
            "Unidad" -> unidadesSeleccionadas.toMutableSet()
            else -> mutableSetOf<String>()
        }
        
        // Configurar checkboxes de filtros de flota con valores actuales
        checkboxVerInactivos.isChecked = verInactivos
        checkboxVerMonitorizados.isChecked = verMonitorizados
        
        // Configurar RecyclerView
        recyclerOpciones.layoutManager = LinearLayoutManager(requireContext())
        val adapter = MultiSelectFilterAdapter(opciones, seleccionInicial) {
            // Callback cuando cambia la selección (no necesario hacer nada aquí)
        }
        recyclerOpciones.adapter = adapter
        
        // Configurar búsqueda
        etBuscarFiltro.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                adapter.filter(s?.toString() ?: "")
            }
        })
        
        // Crear el diálogo
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Configurar botón Cancelar
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }
        
        // Configurar botón Aceptar
        btnAceptar.setOnClickListener {
            // Guardar los valores seleccionados
            val seleccionadas = adapter.getSelectedItems()
            verInactivos = checkboxVerInactivos.isChecked
            verMonitorizados = checkboxVerMonitorizados.isChecked
            
            onSeleccion(seleccionadas)
            dialog.dismiss()
        }
        
        dialog.show()
        
        // Personalizar el diálogo - centrarlo en la pantalla
        dialog.window?.let { window ->
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setGravity(android.view.Gravity.CENTER)
        }
    }

    private fun mostrarDialogoFiltroEstado() {
        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filtro_visualizacion, null)
        
        // Obtener referencias a los elementos del layout
        val spinnerVisualizar = dialogView.findViewById<Spinner>(R.id.spinnerVisualizar)
        val checkboxMostrarAFS = dialogView.findViewById<CheckBox>(R.id.checkboxMostrarAFS)
        val checkboxMostrarELIM = dialogView.findViewById<CheckBox>(R.id.checkboxMostrarELIM)
        val checkboxMostrarMONIT = dialogView.findViewById<CheckBox>(R.id.checkboxMostrarMONIT)
        val btnOK = dialogView.findViewById<Button>(R.id.btnOK)
        
        // Configurar el spinner con las opciones
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcionesVisualizacion)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVisualizar.adapter = adapter
        
        // Establecer valores actuales
        spinnerVisualizar.setSelection(filtroVisualizacion)
        checkboxMostrarAFS.isChecked = mostrarAFS
        checkboxMostrarELIM.isChecked = mostrarELIM
        checkboxMostrarMONIT.isChecked = mostrarMONIT
        
        // Crear el diálogo
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()
        
        // Configurar el botón OK
        btnOK.setOnClickListener {
            // Guardar los valores seleccionados
            filtroVisualizacion = spinnerVisualizar.selectedItemPosition
            mostrarAFS = checkboxMostrarAFS.isChecked
            mostrarELIM = checkboxMostrarELIM.isChecked
            mostrarMONIT = checkboxMostrarMONIT.isChecked
            
            // Actualizar el texto del filtro
            actualizarTextoFiltroEstado()
            
            // Aplicar filtros
            filtrarEquipos()
            
            // Cerrar el diálogo
            dialog.dismiss()
        }
        
        dialog.show()
        
        // Personalizar el diálogo
        dialog.window?.let { window ->
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setGravity(android.view.Gravity.CENTER)
        }
    }
    
    private fun actualizarTextoFiltroEstado() {
        val texto = when (filtroVisualizacion) {
            0 -> "T" // TODOS
            1 -> "I" // INSPECCIONADOS
            2 -> "P" // PDTE. INSPEC.
            else -> "T"
        }
        tvFiltroEstado.text = texto
    }
    
    private fun actualizarTextoBotonFiltro(boton: Button, seleccionadas: Set<String>, textoPorDefecto: String) {
        when {
            seleccionadas.isEmpty() -> boton.text = textoPorDefecto
            seleccionadas.size == 1 -> boton.text = seleccionadas.first()
            else -> boton.text = "${seleccionadas.size} seleccionadas"
        }
    }
    
    private fun obtenerAreasUnicas(): List<String> {
        val equiposFiltrados = equipos.filter { equipo ->
            val flota = equipo.flota?.uppercase() ?: ""
            when {
                verInactivos && (flota == "AFS" || flota == "ELIMINADO") -> true
                verMonitorizados && flota == "MONITORIZADO" -> true
                flota.isEmpty() || flota == "ACTIVO" -> true
                else -> verInactivos || verMonitorizados
            }
        }
        return equiposFiltrados.map { it.area }.filterNotNull().distinct().sorted()
    }
    
    private fun obtenerUnidadesUnicas(): List<String> {
        val equiposFiltrados = equipos.filter { equipo ->
            val flota = equipo.flota?.uppercase() ?: ""
            // Si hay áreas seleccionadas, filtrar por ellas
            val coincideArea = areasSeleccionadas.isEmpty() || equipo.area in areasSeleccionadas
            
            val coincideFlota = when {
                verInactivos && (flota == "AFS" || flota == "ELIMINADO") -> true
                verMonitorizados && flota == "MONITORIZADO" -> true
                flota.isEmpty() || flota == "ACTIVO" -> true
                else -> verInactivos || verMonitorizados
            }
            
            coincideArea && coincideFlota
        }
        return equiposFiltrados.map { it.unidad }.filterNotNull().distinct().sorted()
    }

    private fun filtrarEquipos() {
        val textoBusqueda = etBuscar.text.toString().lowercase()
        
        equiposFiltrados = equipos.filter { equipo ->
            // Filtro por texto de búsqueda
            val coincideBusqueda = textoBusqueda.isEmpty() || 
                equipo.id.lowercase().contains(textoBusqueda) ||
                equipo.area?.lowercase()?.contains(textoBusqueda) == true ||
                equipo.unidad?.lowercase()?.contains(textoBusqueda) == true ||
                equipo.marca?.lowercase()?.contains(textoBusqueda) == true ||
                equipo.modelo?.lowercase()?.contains(textoBusqueda) == true

            // Filtro por área (múltiples seleccionadas)
            val coincideArea = areasSeleccionadas.isEmpty() || equipo.area in areasSeleccionadas

            // Filtro por unidad (múltiples seleccionadas)
            val coincideUnidad = unidadesSeleccionadas.isEmpty() || equipo.unidad in unidadesSeleccionadas

            // Filtro por visualización (TODOS, INSPECCIONADOS, PDTE. INSPEC.)
            val coincideVisualizacion = when (filtroVisualizacion) {
                0 -> true // TODOS: mostrar todos los equipos
                1 -> !equipo.estado.isNullOrEmpty() // INSPECCIONADOS: solo equipos con estado
                2 -> equipo.estado.isNullOrEmpty() // PDTE. INSPEC.: solo equipos sin estado
                else -> true
            }

            // Filtro por flota (AFS, ELIMINADO, MONITORIZADO)
            val flotaEquipo = equipo.flota?.uppercase() ?: ""
            val coincideFlota = when {
                flotaEquipo == "AFS" && !mostrarAFS -> false // Ocultar AFS si no está marcado
                flotaEquipo == "ELIMINADO" && !mostrarELIM -> false // Ocultar ELIMINADO si no está marcado
                flotaEquipo == "MONITORIZADO" && !mostrarMONIT -> false // Ocultar MONITORIZADO si no está marcado
                else -> true // Mostrar todos los demás (ACTIVO, null, etc.)
            }

            coincideBusqueda && coincideArea && coincideUnidad && coincideVisualizacion && coincideFlota
        }

        if (::equipoAdapter.isInitialized) {
            equipoAdapter.updateData(equiposFiltrados)
            Log.d("FragmentInspeccionActual", "Filtrados ${equiposFiltrados.size} equipos de ${equipos.size} totales")
            Log.d("FragmentInspeccionActual", "Filtros activos: visualizacion=$filtroVisualizacion, AFS=$mostrarAFS, ELIM=$mostrarELIM, MONIT=$mostrarMONIT")
        } else {
            Log.w("FragmentInspeccionActual", "equipoAdapter no inicializado aún, omitiendo actualización")
        }
    }

    private fun iniciarCronometro() {
        cronometroActivo = true
        tiempoInicio = System.currentTimeMillis()
        ultimaActividad = tiempoInicio
        btnPlayPause.setImageResource(R.drawable.ic_pause)
        
        Log.d("FragmentInspeccionActual", "Cronómetro iniciado")
        
        // Programar auto-pausa
        programarAutoPausa()
    }

    private fun pausarCronometro() {
        cronometroActivo = false
        tiempoAcumulado += System.currentTimeMillis() - tiempoInicio
        btnPlayPause.setImageResource(R.drawable.ic_play)
        
        Log.d("FragmentInspeccionActual", "Cronómetro pausado. Tiempo acumulado: ${tiempoAcumulado}ms")
    }

    private fun programarAutoPausa() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (cronometroActivo && System.currentTimeMillis() - ultimaActividad > AUTO_PAUSE_DELAY) {
                Log.d("FragmentInspeccionActual", "Auto-pausa por inactividad")
                pausarCronometro()
            }
        }, AUTO_PAUSE_DELAY)
    }

    fun registrarActividad() {
        ultimaActividad = System.currentTimeMillis()
        if (!cronometroActivo) {
            iniciarCronometro()
        }
    }

    fun obtenerTiempoAcumulado(): Long {
        return if (cronometroActivo) {
            tiempoAcumulado + (System.currentTimeMillis() - tiempoInicio)
        } else {
            tiempoAcumulado
        }
    }

    private fun configurarFragmentResultListener() {
        // Escuchar cuando se guarda un equipo para reiniciar el cronómetro
        setFragmentResultListener("equipo_guardado") { _, _ ->
            Log.d("FragmentInspeccionActual", "Equipo guardado detectado, reiniciando cronómetro")
            if (!cronometroActivo) {
                iniciarCronometro()
            } else {
                // Si ya está activo, solo registrar actividad
                registrarActividad()
            }
        }
    }

    private fun cargarColoresEstados(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        try {
            // Primero intentar leer desde el archivo dinámico (actualizado desde Google Sheets)
            val dynamicFile = java.io.File(requireContext().filesDir, "estados_colores.json")
            val jsonString = if (dynamicFile.exists()) {
                Log.d("FragmentInspeccionActual", "Leyendo colores desde archivo dinámico: ${dynamicFile.absolutePath}")
                dynamicFile.readText()
            } else {
                Log.d("FragmentInspeccionActual", "Archivo dinámico no existe, usando assets")
                requireContext().assets.open("estados_colores.json").bufferedReader().use { it.readText() }
            }
            
            val json = JSONObject(jsonString)
            Log.d("FragmentInspeccionActual", "=== CARGANDO COLORES DE ESTADOS ===")
            json.keys().forEach { estado ->
                val colorHex = json.getString(estado)
                try {
                    val estadoKey = estado.uppercase().trim()
                    map[estadoKey] = android.graphics.Color.parseColor(colorHex)
                    Log.d("FragmentInspeccionActual", "Color cargado: '$estado' -> '$estadoKey' -> $colorHex")
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionActual", "Error parsing color for estado '$estado': '$colorHex'", e)
                }
            }
            Log.d("FragmentInspeccionActual", "Total colores cargados: ${map.size}")
            Log.d("FragmentInspeccionActual", "Claves de estados con color: ${map.keys.joinToString(", ")}")
        } catch (e: Exception) {
            Log.e("FragmentInspeccionActual", "Error loading estados_colores.json", e)
        }
        return map
    }
}