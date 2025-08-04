package com.bithermmanagement.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.data.UserData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.SubMenuEntity
import com.bithermmanagement.database.entities.PermisoEntity

class FragmentSubMenuList : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var mainMenuKey: String? = null

    companion object {
        private const val ARG_MAIN_MENU = "main_menu_key"
        fun newInstance(mainMenuKey: String): FragmentSubMenuList {
            val fragment = FragmentSubMenuList()
            val args = Bundle()
            args.putString(ARG_MAIN_MENU, mainMenuKey)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainMenuKey = arguments?.getString(ARG_MAIN_MENU)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_submenu_list, container, false)
        recyclerView = view.findViewById(R.id.recycler_submenu)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val userData = (activity as? com.bithermmanagement.ui.MainMenuActivity)?.intent?.getParcelableExtra<com.bithermmanagement.data.UserData>("USER_DATA")
        val usuarioLogin = userData?.app
        if (mainMenuKey != null && usuarioLogin != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(context)
                val subMenus: List<SubMenuEntity> = db.subMenuDao().getAllSubMenus().filter { it.menuPrincipal == mainMenuKey }
                val permisos: List<PermisoEntity> = db.permisoDao().getPermisosByUsuario(usuarioLogin)
                val permitidos = permisos.filter { it.permiso }.map { it.fragment }
                val subMenusPermitidos = subMenus.filter { it.item in permitidos }
                recyclerView.adapter = SubMenuAdapter(subMenusPermitidos)
            }
        }
    }

    private fun obtenerUsuarioLogueado(): UserData? {
        return (activity as? com.bithermmanagement.ui.MainMenuActivity)?.intent?.getParcelableExtra<UserData>("USER_DATA")
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