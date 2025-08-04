package com.bithermmanagement.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.core.data.SessionManager
import com.bithermmanagement.core.data.SharedViewModel
import com.bithermmanagement.core.data.db.User
import com.bithermmanagement.login.data.LoginResult
import com.bithermmanagement.login.databinding.ActivityLoginBinding
import com.bithermmanagement.navigation.NavigationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBiometric()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val user = binding.etUsername.text.toString()
            val pass = binding.etPassword.text.toString()
            viewModel.validateLogin(user, pass)
        }
    }

    private fun navigateToMain(user: User) {
        val intent = Intent(this, NavigationActivity::class.java).apply {
            putExtra("USER_NAME", user.name)
            putExtra("USER_ROLPOUND", user.rolpound)
        }
        startActivity(intent)
        finish()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginResult.collect { result ->
                when (result) {
                    is LoginResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val userData = result.data
                        sessionManager.setLoggedIn(true)
                        if (binding.cbRemember.isChecked) {
                            sessionManager.saveCredentials(
                                binding.etUsername.text.toString(),
                                binding.etPassword.text.toString()
                            )
                        }
                        val user = User(name = userData.nombre, rolpound = userData.rolPound)
                        sharedViewModel.setUser(user)
                        navigateToMain(user)
                    }
                    is LoginResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                        if (result.message.isNotEmpty()) {
                            Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    is LoginResult.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    sessionManager.getCredentials().let { (user, pass) ->
                        if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                            viewModel.validateLogin(user, pass)
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        binding.btnBiometric.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}

