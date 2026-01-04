package com.bithermmanagement.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.CheckBox
import android.text.TextWatcher
import android.text.Editable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ui.items.MultiSelectFilterAdapter

class MultiSelectFilterDialog(
    context: Context,
    private val titulo: String,
    private val opciones: List<String>,
    private val seleccionados: MutableSet<String>,
    private val onFilterUpdated: () -> Unit
) : Dialog(context) {

    private lateinit var txtTitulo: TextView
    private lateinit var etBuscar: EditText
    private lateinit var btnMarcarCoincidentes: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCancelar: Button
    private lateinit var btnAceptar: Button
    private lateinit var checkboxVerInactivos: CheckBox
    private lateinit var checkboxVerMonitorizados: CheckBox
    private lateinit var adapter: MultiSelectFilterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_multi_select_filter)

        // Ajustar ancho y alto del diálogo - ancho 100%, alto 95%
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = (displayMetrics.heightPixels * 0.95).toInt()
        window?.setLayout(width, height)

        inicializarVistas()
        setupRecyclerView()
        setupListeners()
    }

    private fun inicializarVistas() {
        txtTitulo = findViewById(R.id.txtTituloFiltro)
        etBuscar = findViewById(R.id.etBuscarFiltro)
        btnMarcarCoincidentes = findViewById(R.id.btnMarcarCoincidentes)
        recyclerView = findViewById(R.id.recyclerOpcionesFiltro)
        btnCancelar = findViewById(R.id.btnCancelarFiltro)
        btnAceptar = findViewById(R.id.btnAceptarFiltro)
        checkboxVerInactivos = findViewById(R.id.checkboxVerInactivos)
        checkboxVerMonitorizados = findViewById(R.id.checkboxVerMonitorizados)

        txtTitulo.text = titulo
        
        // Asegurar que los checkboxes estén desmarcados por defecto
        checkboxVerInactivos.isChecked = false
        checkboxVerMonitorizados.isChecked = false
    }

    private fun setupRecyclerView() {
        // Configuración inicial simple
        adapter = MultiSelectFilterAdapter(opciones, seleccionados) {
            // Callback cuando cambia la selección
            actualizarBotonMarcarCoincidentes()
        }
        adapter.setMostrarMarcarCoincidentes(false) // Sin botón interno
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        
        actualizarBotonMarcarCoincidentes()
    }

    private fun setupListeners() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                aplicarFiltrosCheckboxes()
            }
        })

        btnCancelar.setOnClickListener {
            // Restaurar selección anterior
            seleccionados.clear()
            seleccionados.addAll(adapter.getOriginalSelection())
            dismiss()
        }

        btnAceptar.setOnClickListener {
            onFilterUpdated()
            dismiss()
        }

        // Listeners para los checkboxes
        checkboxVerInactivos.setOnCheckedChangeListener { _, _ ->
            aplicarFiltrosCheckboxes()
        }

        checkboxVerMonitorizados.setOnCheckedChangeListener { _, _ ->
            aplicarFiltrosCheckboxes()
        }

        // Listener para el botón de marcar coincidentes
        btnMarcarCoincidentes.setOnClickListener {
            val opcionesVisibles = getOpcionesVisibles()
            
            if (opcionesVisibles.isNotEmpty()) {
                val todosMarcados = opcionesVisibles.all { seleccionados.contains(it) }
                
                if (todosMarcados) {
                    // Desmarcar todos
                    opcionesVisibles.forEach { seleccionados.remove(it) }
                } else {
                    // Marcar todos
                    opcionesVisibles.forEach { seleccionados.add(it) }
                }
                
                adapter.notifyDataSetChanged()
                actualizarBotonMarcarCoincidentes()
            }
        }
    }

    private fun aplicarFiltrosCheckboxes() {
        val textoFiltro = etBuscar.text.toString()
        
        // Debug: ver qué opciones tenemos
        println("DEBUG: Opciones originales: ${opciones.size} items")
        opciones.forEach { println("  - '$it'") }
        
        // Paso 1: Filtrar vacíos
        var todasLasOpciones = opciones.filter { it.trim().isNotEmpty() }
        println("DEBUG: Después de filtrar vacíos: ${todasLasOpciones.size} items")
        
        // Paso 2: Determinar qué mostrar según checkboxes
        val mostrarInactivos = checkboxVerInactivos.isChecked
        val mostrarMonitorizados = checkboxVerMonitorizados.isChecked
        
        println("DEBUG: Ver inactivos = $mostrarInactivos, Ver monitorizados = $mostrarMonitorizados")
        
        var opcionesFiltradas: List<String> = if (!mostrarInactivos && !mostrarMonitorizados) {
            // Estado por defecto: SOLO activos (excluir AFS, eliminados, etc.)
            val activos = todasLasOpciones.filter { item ->
                val esAFS = item.contains("AFS", ignoreCase = true)
                val esEliminado = item.contains("eliminado", ignoreCase = true) || 
                                item.contains("inactivo", ignoreCase = true) ||
                                item.contains("desactivado", ignoreCase = true)
                val esMonitorizado = item.contains("monitorizado", ignoreCase = true) || 
                                   item.contains("monitor", ignoreCase = true)
                
                val esActivo = !esAFS && !esEliminado && !esMonitorizado
                if (!esActivo) {
                    println("DEBUG: Excluyendo '$item' (AFS:$esAFS, Eliminado:$esEliminado, Monitor:$esMonitorizado)")
                }
                esActivo
            }
            println("DEBUG: Equipos activos encontrados: ${activos.size}")
            activos
        } else {
            // Si algún checkbox está marcado, construir lista específica
            val resultado = mutableListOf<String>()
            
            if (mostrarInactivos) {
                val inactivos = todasLasOpciones.filter { item ->
                    item.contains("AFS", ignoreCase = true) ||
                    item.contains("eliminado", ignoreCase = true) ||
                    item.contains("inactivo", ignoreCase = true) ||
                    item.contains("desactivado", ignoreCase = true)
                }
                println("DEBUG: Inactivos encontrados: ${inactivos.size}")
                resultado.addAll(inactivos)
            }
            
            if (mostrarMonitorizados) {
                val monitorizados = todasLasOpciones.filter { item ->
                    item.contains("monitorizado", ignoreCase = true) ||
                    item.contains("monitor", ignoreCase = true)
                }
                println("DEBUG: Monitorizados encontrados: ${monitorizados.size}")
                resultado.addAll(monitorizados)
            }
            
            resultado.distinct()
        }
        
        // Paso 3: Aplicar filtro de texto
        if (textoFiltro.isNotBlank()) {
            opcionesFiltradas = opcionesFiltradas.filter { it.contains(textoFiltro, ignoreCase = true) }
            println("DEBUG: Después de filtro de texto '$textoFiltro': ${opcionesFiltradas.size} items")
        }
        
        println("DEBUG: Lista final: ${opcionesFiltradas.size} items")
        opcionesFiltradas.forEach { println("  -> '$it'") }

        // Paso 4: Actualizar adaptador
        adapter = MultiSelectFilterAdapter(opcionesFiltradas, seleccionados) {
            actualizarBotonMarcarCoincidentes()
        }
        adapter.setMostrarMarcarCoincidentes(false)
        recyclerView.adapter = adapter
        
        actualizarBotonMarcarCoincidentes()
    }

    private fun actualizarBotonMarcarCoincidentes() {
        try {
            val opcionesVisibles = getOpcionesVisibles()
            
            if (opcionesVisibles.isEmpty()) {
                btnMarcarCoincidentes.visibility = View.GONE
                return
            }
            
            btnMarcarCoincidentes.visibility = View.VISIBLE
            val todosMarcados = opcionesVisibles.all { seleccionados.contains(it) }
            
            btnMarcarCoincidentes.text = if (todosMarcados) {
                "Desmarcar ${opcionesVisibles.size} coincidentes"
            } else {
                "Marcar ${opcionesVisibles.size} coincidentes"
            }
        } catch (e: Exception) {
            btnMarcarCoincidentes.visibility = View.GONE
        }
    }
    
    private fun getOpcionesVisibles(): List<String> {
        val textoFiltro = etBuscar.text.toString()
        var lista = opciones.filter { it.trim().isNotEmpty() }
        
        // Equipos activos (por defecto)
        var equiposActivos = lista.filter { item ->
            !item.contains("AFS", true) && 
            !item.contains("eliminado", true) &&
            !item.contains("inactivo", true) &&
            !item.contains("desactivado", true) &&
            !item.contains("monitorizado", true) &&
            !item.contains("DESASFALTO", true)
        }

        // Lista final
        val listaFinal = mutableListOf<String>()
        listaFinal.addAll(equiposActivos)
        
        if (checkboxVerInactivos.isChecked) {
            val inactivos = lista.filter { item ->
                item.contains("AFS", true) || 
                item.contains("eliminado", true) ||
                item.contains("inactivo", true) ||
                item.contains("DESASFALTO", true)
            }
            listaFinal.addAll(inactivos)
        }
        
        if (checkboxVerMonitorizados.isChecked) {
            val monitorizados = lista.filter { item ->
                item.contains("monitorizado", true) ||
                item.contains("monitor", true)
            }
            listaFinal.addAll(monitorizados)
        }
        
        var resultado = listaFinal.distinct()
        
        if (textoFiltro.isNotBlank()) {
            resultado = resultado.filter { it.contains(textoFiltro, true) }
        }
        
        return resultado
    }
} 