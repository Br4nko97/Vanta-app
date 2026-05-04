package com.vanta.app.engine

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import com.vanta.app.data.models.WorkflowStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WorkflowExecutor(private val context: Context) {

    suspend fun executeSteps(steps: List<WorkflowStep>) {
        withContext(Dispatchers.IO) {
            for (step in steps) {
                try {
                    executeStep(step)
                } catch (e: Exception) {
                    Log.e("WorkflowExecutor", "Failed to execute step: ${step.actionType}", e)
                    // Optionally break or continue based on step config
                }
            }
        }
    }

    private suspend fun executeStep(step: WorkflowStep) {
        when (step.actionType) {
            "DELAY" -> {
                val delayMs = step.actionData.toLongOrNull() ?: 1000L
                delay(delayMs)
            }
            "LAUNCH_APP" -> {
                val packageName = step.actionData
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                } else {
                    Log.w("WorkflowExecutor", "App not found: $packageName")
                }
            }
            "TOGGLE_WIFI" -> {
                // Note: Direct WiFi toggling is deprecated/restricted in Android 10+, 
                // but we can try to open settings or use the panel. For the sake of example:
                val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            "SET_VOLUME" -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val targetVolume = step.actionData.toIntOrNull() ?: return
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val newVolume = (targetVolume / 100f * maxVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            }
            // Add more actions like simulating touch, toggling bluetooth, etc.
        }
    }
}
