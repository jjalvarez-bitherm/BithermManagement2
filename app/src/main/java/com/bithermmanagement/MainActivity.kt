package com.bithermmanagement

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bithermmanagement.ui.login.LoginActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirigir directamente a LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    companion object {
        const val SPREADSHEET_ID = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
    }
}