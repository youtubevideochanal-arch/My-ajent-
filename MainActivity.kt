package com.example.myagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AgentScreen()
            }
        }
    }
}

@Composable
fun AgentScreen(viewModel: AgentViewModel = viewModel()) {
    val isAgentOn by viewModel.isAgentOn.collectAsState()
    var rotation by remember { mutableFloatStateOf(0f) }

    // Gola ghumane ke liye animation
    LaunchedEffect(isAgentOn) {
        while (isAgentOn) {
            rotation += 1f
            kotlinx.coroutines.delay(16) // 60 FPS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A)), // Dark background
        contentAlignment = Alignment.Center
    ) {
        // 3D Gola (Orb) - Jo ghumega
        Canvas(
            modifier = Modifier
                .size(250.dp)
                .rotate(rotation)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FFFF), Color(0xFF8A2BE2)),
                    center = center,
                    radius = size.minDimension / 2
                )
            )
        }

        // ON/OFF Button aur Text
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isAgentOn) "AI AGENT ONLINE" else "AI AGENT OFFLINE",
                color = if (isAgentOn) Color.Green else Color.Red,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // ON/OFF Toggle
            Switch(
                checked = isAgentOn,
                onCheckedChange = { viewModel.toggleAgent(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Cyan,
                    checkedTrackColor = Color.DarkGray,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
