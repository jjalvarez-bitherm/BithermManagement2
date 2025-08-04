package com.bithermmanagement.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.bithermmanagement.core.data.SharedViewModel
import com.bithermmanagement.navigation.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding
    private lateinit var navController: NavController
    private val sharedViewModel: SharedViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_navigation) as NavHostFragment
        navController = navHostFragment.navController

        // Recibir datos del Intent y configurar la UI
        val userName = intent.getStringExtra("USER_NAME")
        val rolpound = intent.getIntExtra("USER_ROLPOUND", -1)

        if (userName != null && rolpound != -1) {
            binding.tvUserName.text = userName
            navigationViewModel.loadMenuData(rolpound, userName)
        }

        binding.ivLogout.setOnClickListener {
            // Limpiar credenciales guardadas
            val prefs = getSharedPreferences("bitherm_prefs", MODE_PRIVATE)
            prefs.edit()
                .remove("saved_user")
                .remove("saved_pass")
                .putBoolean("remember", false)
                .apply()
            
            // Redirigir al login usando el nombre de la clase como string
            val intent = Intent()
            intent.setClassName(this, "com.bithermmanagement.ui.login.LoginActivity")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.ivFavorite.setOnClickListener {
            val action = MainMenuFragmentDirections.actionMainMenuFragmentToSubMenuFragment(
                mainMenuKey = "Favoritos",
                mainMenuName = "Favoritos"
            )
            navController.navigate(action)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

