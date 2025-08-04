package com.bithermmanagement.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.data.UserData
import com.bithermmanagement.data.GoogleSheetsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class fragmentUsuariosPerfil : Fragment() {
    private lateinit var googleSheetsHelper: GoogleSheetsManager
    private val grupos = mutableMapOf<String, MutableList<Pair<String, String>>>()
    private val camposEditables = mutableMapOf<String, Boolean>()
    private var usuarioActual: String = ""
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val camposNoEditables = setOf(
        "COD", "NOMBRE", "APELLIDOS", "DNI", "FECHA_NAC", "CATEGORIA", "ALTA_EMP", "BAJA_EMP", "TELF_EMP", "EMAIL_EMP", "APP", "PASS", "ROL", "SW WEB", "FECHA_CAL", "R.MEDICO", "C.ACCESO", "SUP.EJEC"
    )

    private val mapeoCampos = listOf(
        // Card 1: Datos personales
        Triple("Datos Personales", "ID:", "COD"),
        Triple("Datos Personales", "DNI:", "DNI"),
        Triple("Datos Personales", "Nombre:", "NOMBRE"),
        Triple("Datos Personales", "Apellidos:", "APELLIDOS"),
        Triple("Datos Personales", "Fecha nac.:", "FECHA_NAC"),
        Triple("Datos Personales", "Nombre corto", "APP"),
        // Card 2: Información Laboral
        Triple("Información Laboral", "Categoría", "CATEGORIA"),
        Triple("Información Laboral", "Alta empresa", "ALTA_EMP"),
        Triple("Información Laboral", "Reconocimiento médico", "R.MEDICO"),
        Triple("Información Laboral", "Control de acceso", "C.ACCESO"),
        Triple("Información Laboral", "Supervisor ejecución", "SUP.EJEC"),
        // Card 3: Datos de contacto
        Triple("Datos de contacto", "Teléfono empresa", "TELF_EMP"),
        Triple("Datos de contacto", "Email empresa", "EMAIL_EMP"),
        Triple("Datos de contacto", "Teléfono personal", "TELF_PERS"),
        Triple("Datos de contacto", "Email personal", "EMAIL_PERS")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_usuarios_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val credentialsStream = requireContext().assets.open("credentials.json")
        googleSheetsHelper = GoogleSheetsManager(credentialsStream, requireContext())
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", 0)
        usuarioActual = prefs.getString("saved_user", "") ?: ""
        cargarDatosPerfil()
    }

    private fun cargarDatosPerfil() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = requireContext().getSharedPreferences("bitherm_prefs", 0)
                val usuarioActual = prefs.getString("saved_user", "") ?: ""
                val credentialsStream = requireContext().assets.open("credentials.json")
                val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())

                // Usar el nuevo método para obtener los datos agrupados por card
                val datosPorGrupo = sheetsManager.getUserProfileDataByApp(usuarioActual)
                if (datosPorGrupo.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        mostrarError("No se encontró el usuario en la hoja.")
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    mostrarDatosPorGrupo(datosPorGrupo)
                }
            } catch (e: Exception) {
                Log.e("PERFIL_DEBUG", "Error al cargar datos del perfil desde Sheets", e)
                withContext(Dispatchers.Main) {
                    mostrarError("Error al cargar los datos del perfil: ${e.message}")
                }
            }
        }
    }

    private fun mostrarDatosPorGrupo(datosPorGrupo: Map<String, List<Triple<String, String, Int>>>) {
        val layoutDatosPersonales = view?.findViewById<LinearLayout>(R.id.layout_datos_personales)
        val layoutInfoLaboral = view?.findViewById<LinearLayout>(R.id.layout_datos_empresa)
        val layoutCaducidades = view?.findViewById<LinearLayout>(R.id.layout_caducidades)
        val layoutOtros = view?.findViewById<LinearLayout>(R.id.layout_otros)
        val cardOtros = view?.findViewById<View>(R.id.card_otros)
        layoutDatosPersonales?.removeAllViews()
        layoutInfoLaboral?.removeAllViews()
        layoutCaducidades?.removeAllViews()
        layoutOtros?.removeAllViews()
        cardOtros?.visibility = View.GONE

        datosPorGrupo.forEach { (grupo, lista) ->
            val grupoNorm = grupo.trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            val layout = when {
                grupoNorm.contains("personal") -> layoutDatosPersonales
                grupoNorm.contains("empresa") -> layoutInfoLaboral
                grupoNorm.contains("caducidad") -> layoutCaducidades
                else -> layoutOtros
            }
            lista.forEach { (campo, valor, idx) ->
                val etiqueta = campoAmigable(campo)
                addCampoToLayoutSheets(layout, etiqueta, valor, campo, idx)
            }
            if (layout == layoutOtros && lista.isNotEmpty()) {
                cardOtros?.visibility = View.VISIBLE
            }
        }
    }

    private fun campoAmigable(campo: String): String {
        return when (campo.uppercase()) {
            "COD" -> "ID"
            "NOMBRE" -> "Nombre"
            "APELLIDOS" -> "Apellidos"
            "DNI" -> "DNI"
            "FECHA_NAC" -> "Fecha nacimiento"
            "APODO" -> "Apodo"
            "N_CUENTA" -> "Nº Cuenta"
            "TELF_PERS" -> "Teléfono personal"
            "EMAIL_PERS" -> "Email personal"
            "CATEGORIA" -> "Categoría"
            "ALTA_EMP" -> "Alta empresa"
            "BAJA_EMP" -> "Baja empresa"
            "TELF_EMP" -> "Teléfono empresa"
            "EMAIL_EMP" -> "Email empresa"
            "APP" -> "Usuario APP"
            "PASS" -> "Contraseña"
            "ROL" -> "Rol"
            "SW WEB" -> "SW Web"
            "EQUIPO_ASIGN" -> "Equipo asignado"
            "FECHA_CAL" -> "Fecha calibración"
            "R.MEDICO" -> "Reconocimiento médico"
            "C.ACCESO" -> "Control de acceso"
            "SUP.EJEC" -> "Supervisor ejecución"
            else -> campo.replace("_", " ").capitalize()
        }
    }

    private fun addCampoToLayoutSheets(layout: LinearLayout?, etiqueta: String, valor: String, campo: String, idx: Int) {
        val campoView = layoutInflater.inflate(R.layout.item_campo_perfil, null, false)
        val tvNombre = campoView.findViewById<TextView>(R.id.tv_nombre_campo)
        val tvValor = campoView.findViewById<TextView>(R.id.tv_valor_campo)
        val btnEditar = campoView.findViewById<ImageButton>(R.id.btn_editar)
        tvNombre.text = etiqueta
        tvValor.text = valor
        val campoEditable = etiqueta.contains("Teléfono") || etiqueta.contains("Email") || campo.equals("APODO", true) || campo.equals("N_CUENTA", true)
        if (campoEditable) {
            btnEditar.visibility = View.VISIBLE
            btnEditar.setOnClickListener {
                mostrarDialogoEdicion(etiqueta, valor) { nuevoValor ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val prefs = requireContext().getSharedPreferences("bitherm_prefs", 0)
                        val usuarioActual = prefs.getString("saved_user", "") ?: ""
                        val credentialsStream = requireContext().assets.open("credentials.json")
                        val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                        val exito = sheetsManager.updateUserCellByApp(usuarioActual, campo, nuevoValor)
                        withContext(Dispatchers.Main) {
                            if (exito) {
                                tvValor.text = nuevoValor
                                android.widget.Toast.makeText(requireContext(), "¡Datos guardados!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(requireContext(), "Error al guardar", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        } else {
            btnEditar.visibility = View.GONE
        }
        layout?.addView(campoView)
    }

    private fun mostrarDialogoEdicion(nombreCampo: String, valorActual: String, onGuardar: (String) -> Unit) {
        val editText = EditText(requireContext()).apply {
            setText(valorActual)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar $nombreCampo")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                onGuardar(editText.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarError(mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }
} 