package com.vanta.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanta.app.data.AppDatabase
import com.vanta.app.data.models.ButtonBinding
import com.vanta.app.data.models.Workflow
import com.vanta.app.data.models.WorkflowStep
import com.vanta.app.ui.theme.VantaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var capturedKeyFlow = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

    private val keyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.vanta.app.KEY_PRESSED") {
                val keyCode = intent.getIntExtra("KEYCODE", -1)
                if (keyCode != -1) {
                    capturedKeyFlow.value = keyCode
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(keyReceiver, IntentFilter("com.vanta.app.KEY_PRESSED"), RECEIVER_EXPORTED)
        } else {
            registerReceiver(keyReceiver, IntentFilter("com.vanta.app.KEY_PRESSED"))
        }

        setContent {
            VantaTheme {
                var currentScreen by remember { mutableStateOf("DASHBOARD") }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentScreen == "DASHBOARD") {
                        val workflows by database.vantaDao().getAllWorkflows().collectAsState(initial = emptyList())
                        val bindings by database.vantaDao().getAllBindings().collectAsState(initial = emptyList())
                        
                        DashboardScreen(
                            workflows = workflows,
                            bindings = bindings,
                            onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            onCreateNew = { currentScreen = "CREATE" }
                        )
                    } else if (currentScreen == "CREATE") {
                        val capturedKey by capturedKeyFlow.collectAsState()
                        CreateBindingScreen(
                            capturedKey = capturedKey,
                            onClearKey = { capturedKeyFlow.value = null },
                            onSave = { key, wfName, steps ->
                                if (steps.isEmpty() && key == 9999) {
                                    // Triggered by "Forza Bixby", just set the captured key
                                    capturedKeyFlow.value = 9999
                                    return@CreateBindingScreen
                                }
                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.vantaDao()
                                    val wfId = dao.insertWorkflow(Workflow(name = wfName))
                                    steps.forEachIndexed { index, step ->
                                        dao.insertWorkflowStep(step.copy(workflowId = wfId, orderIndex = index))
                                    }
                                    dao.insertBinding(ButtonBinding(keyCode = key, actionType = 0, workflowId = wfId))
                                }
                                currentScreen = "DASHBOARD"
                            },
                            onCancel = { currentScreen = "DASHBOARD" }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(keyReceiver)
    }
}

@Composable
fun DashboardScreen(
    workflows: List<Workflow>,
    bindings: List<ButtonBinding>,
    onOpenAccessibilitySettings: () -> Unit,
    onCreateNew: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew, containerColor = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text(
                text = "VANTA",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            Button(
                onClick = onOpenAccessibilitySettings,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apri Impostazioni Accessibilità", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("I tuoi Keybinds", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (bindings.isEmpty()) {
                Text("Nessun bind creato. Clicca sul + in basso a destra.", color = MaterialTheme.colorScheme.tertiary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bindings) { binding ->
                        BindingRow(binding, workflows.find { it.id == binding.workflowId })
                    }
                }
            }
        }
    }
}

@Composable
fun BindingRow(binding: ButtonBinding, workflow: Workflow?) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Tasto (Codice): ${binding.keyCode}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text("Azione: ${workflow?.name ?: "Unknown"}", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBindingScreen(
    capturedKey: Int?,
    onClearKey: () -> Unit,
    onSave: (Int, String, List<WorkflowStep>) -> Unit,
    onCancel: () -> Unit
) {
    var workflowName by remember { mutableStateOf("Nuova Automazione") }
    var steps by remember { mutableStateOf(listOf<WorkflowStep>()) }
    var showStepDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Crea Nuovo Keybind", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))

        // Key Capture Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. Premi un pulsante fisico del telefono", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (capturedKey == null) {
                    Text("In ascolto... (premi Volume Su, Volume Giù, ecc.)", color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onClearKey(); onSave(9999, "Bixby (S10)", emptyList()) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text("Forza Assegnazione Tasto Bixby (S10)", color = MaterialTheme.colorScheme.onTertiary)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        val keyName = if (capturedKey == 9999) "Bixby (Speciale)" else "$capturedKey"
                        Text("Tasto Rilevato: $keyName", color = MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = onClearKey) { Text("Riprova") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = workflowName,
            onValueChange = { workflowName = it },
            label = { Text("Nome Automazione") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("2. Azioni da eseguire", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Button(onClick = { showStepDialog = true }) { Text("+ Azione") }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(steps) { step ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text("${step.actionType}: ${step.actionData}", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = onCancel) { Text("Annulla") }
            Button(
                onClick = { if (capturedKey != null && steps.isNotEmpty()) onSave(capturedKey, workflowName, steps) },
                enabled = capturedKey != null && steps.isNotEmpty()
            ) { Text("Salva Keybind") }
        }
    }

    if (showStepDialog) {
        var selectedType by remember { mutableStateOf("SET_VOLUME") }
        var stepData by remember { mutableStateOf("50") }

        AlertDialog(
            onDismissRequest = { showStepDialog = false },
            title = { Text("Aggiungi Azione") },
            text = {
                Column {
                    // Semplice selettore
                    val types = listOf("SET_VOLUME", "DELAY", "TOGGLE_WIFI", "LAUNCH_APP")
                    types.forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedType = type }) {
                            RadioButton(selected = (selectedType == type), onClick = { selectedType = type })
                            Text(type)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stepData,
                        onValueChange = { stepData = it },
                        label = { Text("Parametro (es. 50, 1000ms, pacchetto)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    steps = steps + WorkflowStep(workflowId = 0, orderIndex = steps.size, actionType = selectedType, actionData = stepData)
                    showStepDialog = false
                }) { Text("Aggiungi") }
            }
        )
    }
}
