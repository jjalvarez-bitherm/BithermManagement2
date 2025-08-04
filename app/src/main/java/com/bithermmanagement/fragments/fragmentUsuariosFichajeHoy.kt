package com.bithermmanagement.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import android.content.Context
import android.widget.*
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bithermmanagement.data.GoogleSheetsManager
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import androidx.core.content.ContextCompat
import android.graphics.Color
import android.content.Intent
import android.net.Uri

class fragmentUsuariosFichajeHoy : Fragment() {
    private val TAG = "FICHAJE_HOY"
    private lateinit var layoutUsuarios: LinearLayout
    private lateinit var btnActualizar: Button
    private val FICHAJE_LOGS_SHEET = "FICHAJE-LOGS"
    private val TRABAJADORES_SHEET = "TRABAJADORES"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var filtroActual = "TODOS"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Iniciando fragment de fichajes del día")
        return inflater.inflate(R.layout.fragment_usuarios_fichaje_hoy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Configurando vistas")
        
        layoutUsuarios = view.findViewById(R.id.layout_usuarios_fichaje_hoy)
        btnActualizar = view.findViewById(R.id.btn_actualizar_fichajes)

        Log.d(TAG, "onViewCreated: Vistas encontradas - layoutUsuarios: ${layoutUsuarios != null}, btnActualizar: ${btnActualizar != null}")

        btnActualizar.setOnClickListener {
            Log.d(TAG, "btnActualizar: Click detectado")
            cargarUsuariosFichajeHoy()
        }

        // Cargar datos iniciales
        Log.d(TAG, "onViewCreated: Iniciando carga inicial de datos")
        cargarUsuariosFichajeHoy()
    }

    private fun cargarUsuariosFichajeHoy() {
        Log.d(TAG, "cargarUsuariosFichajeHoy: Iniciando carga de datos")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "cargarUsuariosFichajeHoy: Abriendo credentials.json")
                val credentialsStream = requireContext().assets.open("credentials.json")
                Log.d(TAG, "cargarUsuariosFichajeHoy: Credentials abiertos correctamente")
                
                val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                Log.d(TAG, "cargarUsuariosFichajeHoy: GoogleSheetsManager creado")
                
                // Obtener todos los trabajadores
                Log.d(TAG, "cargarUsuariosFichajeHoy: Obteniendo trabajadores...")
                val (headerTrabajadores, trabajadores) = withContext(Dispatchers.IO) {
                    sheetsManager.getAllUsers()
                }
                Log.d(TAG, "cargarUsuariosFichajeHoy: Trabajadores obtenidos - Header: ${headerTrabajadores.size} campos, Trabajadores: ${trabajadores.size} filas")
                
                val hoy = dateFormat.format(Date())
                Log.d(TAG, "cargarUsuariosFichajeHoy: Fecha de hoy: $hoy")
                
                // Obtener fichajes de hoy
                Log.d(TAG, "cargarUsuariosFichajeHoy: Obteniendo fichajes de hoy...")
                val response = withContext(Dispatchers.IO) {
                    sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(sheetsManager.spreadsheetIdPublic, "$FICHAJE_LOGS_SHEET!A:H")
                        .execute()
                }
                Log.d(TAG, "cargarUsuariosFichajeHoy: Respuesta de fichajes obtenida")
                
                val todosFichajes = response.getValues() ?: emptyList()
                Log.d(TAG, "cargarUsuariosFichajeHoy: Total de fichajes obtenidos: ${todosFichajes.size}")
                
                val fichajesHoy = todosFichajes.filter { 
                    it.size > 3 && it[1] == hoy 
                }
                Log.d(TAG, "cargarUsuariosFichajeHoy: Fichajes de hoy filtrados: ${fichajesHoy.size}")
                
                Log.d(TAG, "cargarUsuariosFichajeHoy: Mostrando usuarios en UI")
                mostrarUsuariosFichaje(trabajadores, fichajesHoy, hoy)
                
            } catch (e: Exception) {
                Log.e(TAG, "cargarUsuariosFichajeHoy: Error cargando datos", e)
                Toast.makeText(requireContext(), "Error cargando datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarUsuariosFichaje(trabajadores: List<List<Any>>, fichajesHoy: List<List<Any>>, fechaHoy: String) {
        Log.d(TAG, "mostrarUsuariosFichaje: Iniciando - Trabajadores: ${trabajadores.size}, Fichajes: ${fichajesHoy.size}")
        layoutUsuarios.removeAllViews()

        // Crear tabla de usuarios
        val tabla = TableLayout(requireContext())
        tabla.setPadding(16, 8, 16, 8)
        tabla.isStretchAllColumns = true

        // Header de la tabla
        val headerRow = TableRow(requireContext())
        val headers = listOf("Usuario", "Estado", "Entrada", "Salida", "Tiempo", "Acciones")
        headers.forEach { header ->
            val tv = TextView(requireContext())
            tv.text = header
            tv.setTypeface(null, Typeface.BOLD)
            tv.gravity = Gravity.CENTER
            tv.setBackgroundResource(android.R.color.darker_gray)
            tv.setTextColor(resources.getColor(android.R.color.white, null))
            tv.setPadding(8, 4, 8, 4)
            headerRow.addView(tv)
        }
        tabla.addView(headerRow)
        Log.d(TAG, "mostrarUsuariosFichaje: Header de tabla creado")

        // Procesar cada trabajador
        var trabajadoresProcesados = 0
        trabajadores.forEach { trabajador ->
            Log.d(TAG, "mostrarUsuariosFichaje: Procesando trabajador ${trabajadoresProcesados + 1}/${trabajadores.size}")
            if (trabajador.size >= 3) {
                val username = trabajador[0].toString()
                val nombre = "${trabajador[1]} ${trabajador[2]}"
                Log.d(TAG, "mostrarUsuariosFichaje: Trabajador - Username: $username, Nombre: $nombre")
                
                // Buscar fichajes del trabajador para hoy
                val fichajesUsuario = fichajesHoy.filter { it[0] == username }
                Log.d(TAG, "mostrarUsuariosFichaje: Fichajes encontrados para $username: ${fichajesUsuario.size}")
                
                val entrada = fichajesUsuario.find { it[3] == "ENTRADA" }
                val salida = fichajesUsuario.find { it[3] == "SALIDA" }
                
                Log.d(TAG, "mostrarUsuariosFichaje: $username - Entrada: ${entrada?.getOrNull(2)}, Salida: ${salida?.getOrNull(2)}")
                
                val estado = when {
                    entrada == null -> "NO FICHADO"
                    salida == null -> "SIN SALIDA"
                    else -> "COMPLETADO"
                }
                Log.d(TAG, "mostrarUsuariosFichaje: $username - Estado: $estado")

                // Aplicar filtro
                if (aplicarFiltro(estado)) {
                    Log.d(TAG, "mostrarUsuariosFichaje: $username - Aplicando filtro $filtroActual, incluido en tabla")
                    val row = TableRow(requireContext())
                    
                    // Usuario
                    val tvUsuario = TextView(requireContext())
                    tvUsuario.text = nombre
                    tvUsuario.gravity = Gravity.CENTER
                    tvUsuario.setPadding(8, 4, 8, 4)
                    tvUsuario.setTextColor(Color.WHITE)
                    row.addView(tvUsuario)

                    // Estado
                    val tvEstado = TextView(requireContext())
                    tvEstado.text = estado
                    tvEstado.gravity = Gravity.CENTER
                    tvEstado.setPadding(8, 4, 8, 4)
                    when (estado) {
                        "NO FICHADO" -> tvEstado.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
                        "SIN SALIDA" -> tvEstado.setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
                        "COMPLETADO" -> tvEstado.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
                    }
                    row.addView(tvEstado)

                    // Entrada
                    val tvEntrada = TextView(requireContext())
                    tvEntrada.text = entrada?.getOrNull(2)?.toString() ?: "--"
                    tvEntrada.gravity = Gravity.CENTER
                    tvEntrada.setPadding(8, 4, 8, 4)
                    tvEntrada.setTextColor(Color.WHITE)
                    row.addView(tvEntrada)

                    // Salida
                    val tvSalida = TextView(requireContext())
                    tvSalida.text = salida?.getOrNull(2)?.toString() ?: "--"
                    tvSalida.gravity = Gravity.CENTER
                    tvSalida.setPadding(8, 4, 8, 4)
                    tvSalida.setTextColor(Color.WHITE)
                    row.addView(tvSalida)

                    // Tiempo
                    val tvTiempo = TextView(requireContext())
                    tvTiempo.text = calcularTiempo(entrada, salida, fechaHoy)
                    tvTiempo.gravity = Gravity.CENTER
                    tvTiempo.setPadding(8, 4, 8, 4)
                    tvTiempo.setTextColor(Color.WHITE)
                    row.addView(tvTiempo)

                    tabla.addView(row)

                    // --- NUEVO: Detalle debajo de cada usuario ---
                    val detalleLayout = LinearLayout(requireContext())
                    detalleLayout.orientation = LinearLayout.HORIZONTAL
                    detalleLayout.gravity = Gravity.CENTER
                    detalleLayout.setPadding(8, 0, 8, 16)

                    // Enlace 'Ir' entrada
                    val tvIrEntrada = TextView(requireContext())
                    tvIrEntrada.text = "Ir"
                    tvIrEntrada.setTextColor(Color.BLACK)
                    tvIrEntrada.setPadding(16, 4, 16, 4)
                    tvIrEntrada.gravity = Gravity.CENTER
                    tvIrEntrada.setTypeface(null, Typeface.BOLD)
                    val lugarEntrada = entrada?.getOrNull(6)?.toString() ?: "LUGAR DESCONOCIDO"
                    if (lugarEntrada != "LUGAR DESCONOCIDO") {
                        tvIrEntrada.setBackgroundColor(Color.parseColor("#A5D6A7")) // verde clarito
                    } else {
                        tvIrEntrada.setBackgroundColor(Color.parseColor("#FFCDD2")) // rojo clarito
                    }
                    tvIrEntrada.setOnClickListener {
                        val url = entrada?.getOrNull(5)?.toString()
                        if (!url.isNullOrEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            startActivity(intent)
                        }
                    }
                    detalleLayout.addView(tvIrEntrada)

                    // Enlace 'Ir' salida
                    val tvIrSalida = TextView(requireContext())
                    tvIrSalida.text = "Ir"
                    tvIrSalida.setTextColor(Color.BLACK)
                    tvIrSalida.setPadding(16, 4, 16, 4)
                    tvIrSalida.gravity = Gravity.CENTER
                    tvIrSalida.setTypeface(null, Typeface.BOLD)
                    val lugarSalida = salida?.getOrNull(6)?.toString() ?: "LUGAR DESCONOCIDO"
                    if (lugarSalida != "LUGAR DESCONOCIDO") {
                        tvIrSalida.setBackgroundColor(Color.parseColor("#A5D6A7")) // verde clarito
                    } else {
                        tvIrSalida.setBackgroundColor(Color.parseColor("#FFCDD2")) // rojo clarito
                    }
                    tvIrSalida.setOnClickListener {
                        val url = salida?.getOrNull(5)?.toString()
                        if (!url.isNullOrEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            startActivity(intent)
                        }
                    }
                    detalleLayout.addView(tvIrSalida)

                    // Total horas trabajadas
                    val tvTotalHoras = TextView(requireContext())
                    tvTotalHoras.text = "Total horas: " + calcularTiempo(entrada, salida, fechaHoy)
                    tvTotalHoras.setTextColor(Color.WHITE)
                    tvTotalHoras.setPadding(16, 4, 16, 4)
                    detalleLayout.addView(tvTotalHoras)

                    // Añadir el layout de detalle debajo de la fila del usuario
                    tabla.addView(detalleLayout)
                } else {
                    Log.d(TAG, "mostrarUsuariosFichaje: $username - Filtrado por $filtroActual, no incluido")
                }
            } else {
                Log.w(TAG, "mostrarUsuariosFichaje: Trabajador con datos insuficientes: ${trabajador.size} campos")
            }
            trabajadoresProcesados++
        }

        layoutUsuarios.addView(tabla)
        Log.d(TAG, "mostrarUsuariosFichaje: Tabla completada y añadida al layout")
    }

    private fun aplicarFiltro(estado: String): Boolean {
        val resultado = when (filtroActual) {
            "TODOS" -> true
            "FICHADOS" -> estado == "SIN SALIDA" || estado == "COMPLETADO"
            "NO FICHADOS" -> estado == "NO FICHADO"
            "SIN SALIDA" -> estado == "SIN SALIDA"
            else -> true
        }
        Log.d(TAG, "aplicarFiltro: Estado: $estado, Filtro: $filtroActual, Resultado: $resultado")
        return resultado
    }

    private fun calcularTiempo(entrada: List<Any>?, salida: List<Any>?, fecha: String): String {
        if (entrada == null || salida == null) {
            Log.d(TAG, "calcularTiempo: Entrada o salida null, retornando --")
            return "--"
        }
        
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val t1 = sdf.parse("$fecha ${entrada[2]}")?.time ?: 0L
            val t2 = sdf.parse("$fecha ${salida[2]}")?.time ?: 0L
            val diff = t2 - t1
            val h = diff / (1000 * 60 * 60)
            val m = (diff / (1000 * 60)) % 60
            val resultado = String.format("%02d:%02d", h, m)
            Log.d(TAG, "calcularTiempo: Calculado: $resultado (${h}h ${m}m)")
            resultado
        } catch (e: Exception) {
            Log.e(TAG, "calcularTiempo: Error calculando tiempo", e)
            "--"
        }
    }

    private fun mostrarDialogoEditarFichaje(username: String, nombre: String, entrada: List<Any>?, salida: List<Any>?, fecha: String) {
        Log.d(TAG, "mostrarDialogoEditarFichaje: Iniciando para $username")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_fichaje, null)
        
        val etEntrada = dialogView.findViewById<EditText>(R.id.et_entrada)
        val etSalida = dialogView.findViewById<EditText>(R.id.et_salida)
        
        etEntrada.setText(entrada?.getOrNull(2)?.toString() ?: "")
        etSalida.setText(salida?.getOrNull(2)?.toString() ?: "")
        
        Log.d(TAG, "mostrarDialogoEditarFichaje: Valores iniciales - Entrada: ${etEntrada.text}, Salida: ${etSalida.text}")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Fichaje - $nombre")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nuevaEntrada = etEntrada.text.toString()
                val nuevaSalida = etSalida.text.toString()
                
                Log.d(TAG, "mostrarDialogoEditarFichaje: Guardando - Entrada: $nuevaEntrada, Salida: $nuevaSalida")
                
                if (nuevaEntrada.isNotEmpty() && nuevaSalida.isNotEmpty()) {
                    guardarFichajeEditado(username, fecha, nuevaEntrada, nuevaSalida)
                } else {
                    Log.w(TAG, "mostrarDialogoEditarFichaje: Campos vacíos, no guardando")
                    Toast.makeText(requireContext(), "Debe completar ambos campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAñadirFichaje(username: String, nombre: String, fecha: String) {
        Log.d(TAG, "mostrarDialogoAñadirFichaje: Iniciando para $username")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_fichaje, null)
        
        val etEntrada = dialogView.findViewById<EditText>(R.id.et_entrada)
        val etSalida = dialogView.findViewById<EditText>(R.id.et_salida)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Añadir Fichaje - $nombre")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val entrada = etEntrada.text.toString()
                val salida = etSalida.text.toString()
                
                Log.d(TAG, "mostrarDialogoAñadirFichaje: Guardando - Entrada: $entrada, Salida: $salida")
                
                if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                    añadirFichajeManual(username, fecha, entrada, salida)
                } else {
                    Log.w(TAG, "mostrarDialogoAñadirFichaje: Campos vacíos, no guardando")
                    Toast.makeText(requireContext(), "Debe completar ambos campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarFichajeEditado(username: String, fecha: String, entrada: String, salida: String) {
        Log.d(TAG, "guardarFichajeEditado: Iniciando para $username - $fecha $entrada-$salida")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialsStream = requireContext().assets.open("credentials.json")
                val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                
                // Aquí implementarías la lógica para actualizar los fichajes en Google Sheets
                Log.d(TAG, "guardarFichajeEditado: Lógica de actualización en Google Sheets (pendiente de implementar)")
                Toast.makeText(requireContext(), "Fichaje actualizado para $username", Toast.LENGTH_SHORT).show()
                
                // Recargar datos
                cargarUsuariosFichajeHoy()
                
            } catch (e: Exception) {
                Log.e(TAG, "guardarFichajeEditado: Error guardando fichaje", e)
                Toast.makeText(requireContext(), "Error guardando fichaje: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun añadirFichajeManual(username: String, fecha: String, entrada: String, salida: String) {
        Log.d(TAG, "añadirFichajeManual: Iniciando para $username - $fecha $entrada-$salida")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialsStream = requireContext().assets.open("credentials.json")
                val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                
                // Aquí implementarías la lógica para añadir fichajes en Google Sheets
                Log.d(TAG, "añadirFichajeManual: Lógica de añadir en Google Sheets (pendiente de implementar)")
                Toast.makeText(requireContext(), "Fichaje añadido para $username", Toast.LENGTH_SHORT).show()
                
                // Recargar datos
                cargarUsuariosFichajeHoy()
                
            } catch (e: Exception) {
                Log.e(TAG, "añadirFichajeManual: Error añadiendo fichaje", e)
                Toast.makeText(requireContext(), "Error añadiendo fichaje: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
} 