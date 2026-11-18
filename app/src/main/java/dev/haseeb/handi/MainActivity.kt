package dev.haseeb.handi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.haseeb.handi.core.designsystem.theme.HandiTheme
import dev.haseeb.handi.ui.navigation.HandiNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val nav = rememberNavController()
            navController = nav
            HandiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HandiNavHost(nav = nav)
                }
            }
        }
    }

    /** A deep link (shared recipe link, issue #8) tapped while the app is already running
     * arrives here, not in [onCreate] -- NavController only inspects the Activity's *original*
     * launch intent automatically, so a tap on a warm app needs this forwarded explicitly. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController?.handleDeepLink(intent)
    }
}
