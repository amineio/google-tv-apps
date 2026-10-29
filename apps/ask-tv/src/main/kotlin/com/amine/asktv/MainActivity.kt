package com.amine.asktv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity:ComponentActivity() {
    private lateinit var model:AskViewModel
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed -> model.permissionResult(allowed) }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        model=ViewModelProvider(this,object:ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=AskViewModel(application) as T
        })[AskViewModel::class.java]
        setContent { AskTheme { AskScreen(model,onAsk={
            if(model.ask(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)) permission.launch(Manifest.permission.RECORD_AUDIO)
        },onExit={finish()}) } }
        authenticate(intent)
    }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent); setIntent(intent); authenticate(intent) }
    private fun authenticate(intent:Intent) {
        if(intent.getBooleanExtra("authenticate",false) || intent.action=="com.amine.asktv.AUTHENTICATE") {
            intent.removeExtra("authenticate"); intent.action=Intent.ACTION_MAIN
            model.signIn()
        }
    }
    override fun onStop() { model.stopped(); super.onStop() }
}
