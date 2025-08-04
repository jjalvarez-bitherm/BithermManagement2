package com.bithermmanagement.ui.favoritos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ui.MainMenuAdapter
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.data.UserData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.FavoritoEntity
import com.bithermmanagement.database.entities.SubMenuEntity

class FragmentFavoritos : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MainMenuAdapter.FavoritosAdapter
    private val TAG = "FavoritosFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView llamado")
        val view = inflater.inflate(R.layout.fragment_favoritos, container, false)
        recyclerView = view.findViewById(R.id.recycler_favoritos_menu)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated llamado")
        val userData = obtenerUsuarioLogueado()
        if (userData != null) {
            val usuarioLogin = userData.app
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                val favoritos: List<FavoritoEntity> = db.favoritoDao().getFavoritosByUsuario(usuarioLogin)
                val subMenus: List<SubMenuEntity> = db.subMenuDao().getAllSubMenus()
                val favoritosSubMenus = favoritos.mapNotNull { fav ->
                    subMenus.find { it.item == fav.item }
                }
                adapter = MainMenuAdapter.FavoritosAdapter(favoritosSubMenus)
                recyclerView.adapter = adapter
            }
        }
    }

    private fun obtenerUsuarioLogueado(): UserData? {
        val user = (activity as? com.bithermmanagement.ui.MainMenuActivity)?.intent?.getParcelableExtra<UserData>("USER_DATA")
        Log.d(TAG, "obtenerUsuarioLogueado: $user")
        return user
    }
    private fun obtenerGoogleSheetsManager(): GoogleSheetsManager? {
        return try {
            val credentialsStream = requireContext().assets.open("credentials.json")
            GoogleSheetsManager(credentialsStream, requireContext())
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener GoogleSheetsManager", e)
            null
        }
    }
} 