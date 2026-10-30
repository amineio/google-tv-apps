package com.amine.postbox

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider

internal const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

internal fun ComponentActivity.hasLocalNetworkPermission(): Boolean =
    Build.VERSION.SDK_INT < 37 ||
        checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

class MainActivity : ComponentActivity() {
    private lateinit var model: PostboxViewModel
    private val networkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            model.permissionChanged(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model =
            ViewModelProvider(this, PostboxViewModel.factory(application, false))[
                PostboxViewModel::class.java]
        setContent { PostboxTheme { TvScreens(model, ::openReceiver) } }
    }

    private fun openReceiver() {
        model.enterAdd()
        if (!hasLocalNetworkPermission()) networkPermission.launch(LOCAL_NETWORK_PERMISSION)
        else model.permissionChanged(true)
    }

    override fun onResume() {
        super.onResume()
        model.setResumed(true, hasLocalNetworkPermission())
    }

    override fun onPause() {
        model.setResumed(false, hasLocalNetworkPermission())
        super.onPause()
    }
}
