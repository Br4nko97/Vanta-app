package com.vanta.app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.vanta.app.data.AppDatabase
import com.vanta.app.engine.WorkflowExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BixbyInterceptorActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // This activity is invisible and only launched by Samsung's Bixby button.
        Log.d("BixbyInterceptor", "Bixby button pressed, launching workflow!")
        
        val database = AppDatabase.getDatabase(this)
        val workflowExecutor = WorkflowExecutor(this)
        
        CoroutineScope(Dispatchers.Main).launch {
            val binding = withContext(Dispatchers.IO) {
                // 1082 is the standard internal Bixby keycode, or we use a custom fake one (e.g. 9999)
                database.vantaDao().getBindingForButton(9999, 0)
            }
            
            if (binding != null) {
                val steps = withContext(Dispatchers.IO) {
                    database.vantaDao().getStepsForWorkflow(binding.workflowId)
                }
                workflowExecutor.executeSteps(steps)
            }
            
            // Instantly close the invisible activity so the user never sees it
            finish()
        }
    }
}
