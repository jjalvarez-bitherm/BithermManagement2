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
    private var areaSeleccionada: String? = null
    private var unidadSeleccionada: String? = null
    private var filtroEstado: Int = 0 // 0=Todos, 1=Inspeccionados, 2=Pendientes, 3=Monitorizados
    
    // Variables para el cronómetro
    private var cronometroActivo = false
    private var tiempoInicio: Long = 0
    private var tiempoAcumulado: Long = 0
    private var ultimaActividad: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val AUTO_PAUSE_DELAY = 5 * 60 * 1000L // 5 minutos
    
    // Opciones de filtro de estado
    private val opcionesFiltroEstado = listOf(
        "T" to "Todos los purgadores",
        "I" to "Purg. inspeccionados", 
        "P" to "Purgadores pendientes",
        "M" to "Todos + Monitorizados"
    )

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
            Log.d("FragmentInspeccionActual", "DEBUG: equiposCompletos cargados: ${equiposCompletos.size}")
            
            // Convertir Equipo a EquipoView para el adapter
            equipos = equiposCompletos.map { equipo ->
                EquipoView(
                    id = equipo.id,
                    estado = equipo.estado,
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
                    foto = equipo.urlFotoEquipo,
                    orden = equipo.orden,
                    instalacion = equipo.instalacion,
                    linea = equipo.linea,
                    aislamiento = equipo.aislamiento
                )
            }
            
            equiposFiltrados = equipos
            Log.d("FragmentInspeccionActual", "Cargados ${equipos.size} equipos desde BD")
            
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
            mostrarDialogoFiltro("Área", obtenerAreasUnicas()) { area ->
                areaSeleccionada = area
                btnFiltroArea.text = if (area != null) area else "AREA"
                filtrarEquipos()
            }
        }

        // Configurar botón de filtro UNIDAD
        btnFiltroUnidad.setOnClickListener {
            val unidades = if (areaSeleccionada != null) {
                equipos.filter { it.area == areaSeleccionada }
                    .map { it.unidad }
                    .filterNotNull()
                    .distinct()
                    .sorted()
            } else {
                equipos.map { it.unidad }.filterNotNull().distinct().sorted()
            }
            
            mostrarDialogoFiltro("Unidad", unidades) { unidad ->
                unidadSeleccionada = unidad
                btnFiltroUnidad.text = if (unidad != null) unidad else "UNIDAD"
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

    private fun mostrarDialogoFiltro(titulo: String, opciones: List<String>, onSeleccion: (String?) -> Unit) {
        val opcionesConNinguno = listOf("Todos") + opciones
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar $titulo")
            .setItems(opcionesConNinguno.toTypedArray()) { _, which ->
                val seleccion = if (which == 0) null else opcionesConNinguno[which]
                onSeleccion(seleccion)
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
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

    private fun mostrarDialogoFiltroEstado() {
        val opciones = opcionesFiltroEstado.map { it.second }.toTypedArray()
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Filtro de Estado")
            .setItems(opciones) { _, which ->
                filtroEstado = which
                tvFiltroEstado.text = opcionesFiltroEstado[which].first
                filtrarEquipos()
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
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

    private fun obtenerAreasUnicas(): List<String> {
        return equipos.map { it.area }.filterNotNull().distinct().sorted()
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

            // Filtro por área
            val coincideArea = areaSeleccionada == null || equipo.area == areaSeleccionada

            // Filtro por unidad
            val coincideUnidad = unidadSeleccionada == null || equipo.unidad == unidadSeleccionada

            // Filtro por estado
            val coincideEstado = when (filtroEstado) {
                0 -> true // Todos
                1 -> equipo.estado?.lowercase()?.contains("bien") == true // Inspeccionados
                2 -> equipo.estado?.lowercase()?.contains("bien") != true // Pendientes
                3 -> true // Todos + Monitorizados (por ahora todos)
                else -> true
            }

            coincideBusqueda && coincideArea && coincideUnidad && coincideEstado
        }

        equipoAdapter.updateData(equiposFiltrados)
        Log.d("FragmentInspeccionActual", "Filtrados ${equiposFiltrados.size} equipos de ${equipos.size} totales")
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
            val json = JSONObject(requireContext().assets.open("estados_colores.json").bufferedReader().use { it.readText() })
            json.keys().forEach { estado ->
                val colorHex = json.getString(estado)
                try {
                    map[estado] = android.graphics.Color.parseColor(colorHex)
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionActual", "Error parsing color for estado $estado: $colorHex", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FragmentInspeccionActual", "Error loading estados_colores.json", e)
        }
        return map
    }
} 