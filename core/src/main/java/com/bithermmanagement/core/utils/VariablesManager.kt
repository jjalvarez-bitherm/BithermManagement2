package com.bithermmanagement.core.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.bithermmanagement.core.data.LoginConfig
import com.bithermmanagement.core.data.UserData
import java.io.IOException

@Singleton
class VariablesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var variablesJson: JsonObject? = null
    private var userData: JsonObject? = null
    private var loginConfig: LoginConfig? = null
    var currentUser: UserData? = null

    init {
        loadVariables()
        loadLoginConfig()
    }

    private fun loadVariables() {
        try {
            val jsonString = context.assets.open("variables.json").bufferedReader().use { it.readText() }
            variablesJson = Gson().fromJson(jsonString, JsonObject::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLoginConfig() {
        try {
            val jsonString = context.assets.open("variables.json").bufferedReader().use { it.readText() }
            val jsonObject = Gson().fromJson(jsonString, JsonObject::class.java)
            val loginModule = jsonObject.getAsJsonObject("login_module")
            if (loginModule != null) {
                loginConfig = LoginConfig(
                    splashDuration = loginModule.getAsJsonObject("splash_screen")?.get("duration_ms")?.asInt ?: 2000,
                    loginTitle = loginModule.getAsJsonObject("login_screen")?.get("title")?.asString ?: "Iniciar Sesión",
                    usernameHint = loginModule.getAsJsonObject("login_screen")?.get("username_hint")?.asString ?: "Usuario",
                    passwordHint = loginModule.getAsJsonObject("login_screen")?.get("password_hint")?.asString ?: "Contraseña",
                    loginButtonText = loginModule.getAsJsonObject("login_screen")?.get("login_button_text")?.asString ?: "Entrar",
                    forgotPasswordText = loginModule.getAsJsonObject("login_screen")?.get("forgot_password_text")?.asString ?: "¿Olvidaste tu contraseña?",
                    biometricEnabled = loginModule.getAsJsonObject("biometric")?.get("enabled")?.asBoolean ?: true
                )
            } else {
                loginConfig = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            loginConfig = null // Ensure config is null on error
        }
    }

    fun getGoogleSheetsConfig(): GoogleSheetsConfig? {
        return try {
            val sheetsConfig = variablesJson?.getAsJsonObject("google_sheets")
            if (sheetsConfig != null) {
                GoogleSheetsConfig(
                    spreadsheetId = sheetsConfig.get("spreadsheet_id")?.asString ?: "",
                    credentialsFile = sheetsConfig.get("credentials_file")?.asString ?: "credentials.json",
                    usersSheet = sheetsConfig.getAsJsonObject("sheets")?.getAsJsonObject("users")?.get("name")?.asString ?: "Usuarios",
                    userColumns = sheetsConfig.getAsJsonObject("sheets")?.getAsJsonObject("users")?.getAsJsonObject("columns")?.let { columns ->
                        UserColumns(
                            username = columns.get("username")?.asString ?: "A",
                            password = columns.get("password")?.asString ?: "B",
                            name = columns.get("name")?.asString ?: "C",
                            email = columns.get("email")?.asString ?: "D",
                            role = columns.get("role")?.asString ?: "E",
                            active = columns.get("active")?.asString ?: "F",
                            lastLogin = columns.get("last_login")?.asString ?: "G"
                        )
                    }
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setUserData(data: JsonObject) {
        userData = data
    }

    fun getUserData(): JsonObject? = userData

    fun getUserDataField(field: String): String? {
        return userData?.get(field)?.asString
    }

    fun saveCurrentUser(user: UserData) {
        currentUser = user
    }

    fun clearCurrentUser() {
        currentUser = null
    }

    fun getLoginConfig(): LoginConfig? {
        return loginConfig
    }

    data class LoginConfig(
        val splashDuration: Int,
        val loginTitle: String,
        val usernameHint: String,
        val passwordHint: String,
        val loginButtonText: String,
        val forgotPasswordText: String,
        val biometricEnabled: Boolean
    )

    data class GoogleSheetsConfig(
        val spreadsheetId: String,
        val credentialsFile: String,
        val usersSheet: String,
        val userColumns: UserColumns?
    )

    data class UserColumns(
        val username: String,
        val password: String,
        val name: String,
        val email: String,
        val role: String,
        val active: String,
        val lastLogin: String
    )
} 