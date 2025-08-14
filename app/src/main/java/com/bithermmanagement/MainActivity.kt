package com.bithermmanagement

import android.content.Intent
import android.os.Bundle
import com.bithermmanagement.core.base.DebugBaseActivity
import com.bithermmanagement.ui.login.LoginActivity

class MainActivity : DebugBaseActivity() {
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