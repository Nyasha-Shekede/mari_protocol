package com.Mari.mobile.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.Mari.mobile.MariAppRoot
import com.Mari.mobileapp.core.agent.LocalAgentManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var localAgentManager: LocalAgentManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MariAppRoot()
        }
    }

    override fun onStart() {
        super.onStart()
        // Register receiver when Activity becomes visible to avoid leaks
        try {
            if (localAgentManager == null) localAgentManager = LocalAgentManager(this)
            localAgentManager?.registerLocalAgentHandler()
        } catch (_: Exception) { /* safe guard */ }
    }

    override fun onStop() {
        // Unregister when Activity is no longer visible
        try { localAgentManager?.unregisterLocalAgentHandler() } catch (_: Exception) {}
        super.onStop()
    }
}
