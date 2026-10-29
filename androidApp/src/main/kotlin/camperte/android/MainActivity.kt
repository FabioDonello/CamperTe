package camperte.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private val viewModel: ListingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamperTeTheme {
                ListingsScreen(
                    state = viewModel.state,
                    onRefresh = viewModel::refresh,
                    onRetry = viewModel::retry,
                    onOpenListing = ::openListing,
                )
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.refresh()
    }

    private fun openListing(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app can open $url", e)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
