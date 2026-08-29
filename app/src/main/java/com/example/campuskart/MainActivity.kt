package com.example.campuskart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campuskart.setup.SetupCheck
import com.example.campuskart.ui.theme.CampusKartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusKartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupStatusScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * TEMPORARY (Day 1) diagnostics screen. It exists only so the Firebase and Cloudinary setup can
 * be verified on a real device before any feature work starts. The Login screen replaces this
 * on Day 3.
 */
@Composable
private fun SetupStatusScreen(modifier: Modifier = Modifier) {
    var results by remember { mutableStateOf<List<SetupCheck.Result>?>(null) }

    LaunchedEffect(Unit) {
        results = SetupCheck.runAll()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("CampusKart", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Day 1 setup check - not the real app UI",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        when (val current = results) {
            null -> CircularProgressIndicator()
            else -> current.forEach { result ->
                StatusCard(result)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(result: SetupCheck.Result) {
    val (symbol, tint) = when (result.status) {
        SetupCheck.Status.OK -> "PASS" to Color(0xFF2E7D32)
        SetupCheck.Status.WARN -> "WARN" to Color(0xFFE65100)
        SetupCheck.Status.FAIL -> "FAIL" to Color(0xFFC62828)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "$symbol  ${result.label}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Text(result.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusCardPreview() {
    CampusKartTheme {
        StatusCard(
            SetupCheck.Result(
                label = "Cloud Firestore",
                status = SetupCheck.Status.OK,
                detail = "Reached the database and completed a read",
            ),
        )
    }
}
