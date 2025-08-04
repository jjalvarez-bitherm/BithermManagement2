package com.bithermmanagement.ui.items

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import com.bithermmanagement.fichaje.FichajeActivity

class FragmentUsuariosFichaje : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fichaje_launcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configurar el botón para lanzar la actividad de fichaje
        view.findViewById<View>(R.id.btnLaunchFichaje).setOnClickListener {
            val intent = Intent(requireContext(), FichajeActivity::class.java)
            startActivity(intent)
        }
        
        // Mostrar información del usuario actual
        val userData = (activity as? com.bithermmanagement.ui.MainMenuActivity)?.intent?.getParcelableExtra<com.bithermmanagement.data.UserData>("USER_DATA")
        userData?.let { user ->
            view.findViewById<TextView>(R.id.tvUserInfo).text = "Usuario: ${user.apodo}"
        }
    }
} 