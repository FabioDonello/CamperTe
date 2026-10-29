package camperte.android

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import camperte.CamperRepository

class MainActivity : ComponentActivity() {

    private val viewModel: ListingsViewModel by viewModels {
        viewModelFactory {
            initializer { ListingsViewModel(CamperRepository(), LocationProvider(applicationContext)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamperTeTheme {
                val filtersState = viewModel.filtersState
                if (filtersState == null) {
                    ListingsScreen(
                        state = viewModel.state,
                        onRefresh = viewModel::refresh,
                        onRetry = viewModel::retry,
                        onOpenListing = ::openListing,
                        onOpenFilters = viewModel::openFilters,
                    )
                } else {
                    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        viewModel.onLocationPermissionResult(it)
                    }
                    BackHandler(onBack = viewModel::closeFilters)
                    FiltersScreen(
                        state = filtersState,
                        onChange = viewModel::editFilters,
                        onChooseArea = viewModel::chooseArea,
                        onAllowLocation = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                        onDismissLocation = viewModel::dismissLocationRequest,
                        onOpenLocationSettings = ::openAppSettings,
                        onReset = viewModel::resetFilters,
                        onClose = viewModel::closeFilters,
                    )
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.onReturnToApp()
    }

    private fun openListing(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app can open $url", e)
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app settings screen", e)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
