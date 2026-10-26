package dev.prootcode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.prootcode.app.ui.SpikeScreen
import dev.prootcode.app.ui.theme.ProotCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val autoRun = intent.getBooleanExtra(EXTRA_AUTORUN, false)
        setContent {
            ProotCodeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpikeScreen(
                        autoRun = autoRun,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_AUTORUN = "autorun"
    }
}

@Preview(showBackground = true)
@Composable
private fun SpikeScreenPreview() {
    ProotCodeTheme(dynamicColor = false) {
        SpikeScreen(autoRun = false)
    }
}
