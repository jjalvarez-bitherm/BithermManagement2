package com.bithermmanagement.inspection

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.inspection.R
import com.bithermmanagement.inspection.MultiSelectFilterAdapter
// TODO: Adaptar el import del adaptador MultiSelectFilterAdapter

class MultiSelectFilterDialog(
    context: Context,
    private val titulo: String,
    private val opciones: List<String>,
    private val seleccionados: MutableSet<String>,
    private val onFilterUpdated: () -> Unit
) : Dialog(context) {

    private lateinit var txtTitulo: TextView
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnCancelar: Button
    private lateinit var btnAceptar: Button
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
        searchView = findViewById(R.id.searchViewFiltro)
        recyclerView = findViewById(R.id.recyclerOpcionesFiltro)
        btnCancelar = findViewById(R.id.btnCancelarFiltro)
        btnAceptar = findViewById(R.id.btnAceptarFiltro)

        txtTitulo.text = titulo

        // Forzar color azul corporativo en todos los subcomponentes del SearchView
        searchView.post {
            fun forceBlueOnTextViews(view: android.view.View) {
                if (view is TextView) {
                    view.setTextColor(context.getColor(R.color.bitherm_blue))
                    view.setHintTextColor(context.getColor(R.color.bitherm_blue))
                    view.setBackgroundColor(context.getColor(android.R.color.white))
                } else if (view is android.view.ViewGroup) {
                    for (i in 0 until view.childCount) {
                        forceBlueOnTextViews(view.getChildAt(i))
                    }
                }
            }
            forceBlueOnTextViews(searchView)
        }

        // Forzar color azul en el icono de la lupa
        val searchMagIconId = searchView.context.resources.getIdentifier("android:id/search_mag_icon", null, null)
        val searchMagIcon = searchView.findViewById<android.widget.ImageView>(searchMagIconId)
        searchMagIcon?.setColorFilter(context.getColor(R.color.bitherm_blue))

        // Forzar color azul en el botón de limpiar texto
        val searchCloseBtnId = searchView.context.resources.getIdentifier("android:id/search_close_btn", null, null)
        val searchCloseBtn = searchView.findViewById<android.widget.ImageView>(searchCloseBtnId)
        searchCloseBtn?.setColorFilter(context.getColor(R.color.bitherm_blue))
    }

    private fun setupRecyclerView() {
        adapter = MultiSelectFilterAdapter(opciones, seleccionados) {
            // Callback cuando cambia la selección
        }
        adapter.setMostrarMarcarCoincidentes(true)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MultiSelectFilterDialog.adapter
        }
    }

    private fun setupListeners() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
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
    }
} 