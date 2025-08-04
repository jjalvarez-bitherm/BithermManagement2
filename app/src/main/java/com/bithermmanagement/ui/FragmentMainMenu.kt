package com.bithermmanagement.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.data.UserData
import kotlinx.coroutines.launch
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.MenuEntity

class FragmentMainMenu : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MainMenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FragmentMainMenu", "onCreate llamado")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FragmentMainMenu", "onCreateView llamado")
        val view = inflater.inflate(R.layout.fragment_main_menu, container, false)
        recyclerView = view.findViewById(R.id.recycler_main_menu)
        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager
        Log.d("FragmentMainMenu", "GridLayoutManager configurado")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragmentMainMenu", "onViewCreated llamado")
        val context = requireContext()
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val menus: List<MenuEntity> = db.menuDao().getAllMenus()
                Log.d("FragmentMainMenu", "Menus obtenidos: ${menus.size}")
                adapter = MainMenuAdapter(menus)
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Log.e("FragmentMainMenu", "Error al cargar menús: ", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("FragmentMainMenu", "onResume llamado")
    }

    // Métodos de ejemplo para obtener el usuario y el manager (ajusta según tu app)
    private fun obtenerUsuarioLogueado(): UserData? {
        return (activity as? MainMenuActivity)?.intent?.getParcelableExtra<UserData>("USER_DATA")
    }
    private fun obtenerGoogleSheetsManager(): GoogleSheetsManager? {
        return try {
            val credentialsStream = requireContext().assets.open("credentials.json")
            GoogleSheetsManager(credentialsStream, requireContext())
        } catch (e: Exception) {
            null
        }
    }
} 