package hu.nutrinaplo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import hu.nutrinaplo.app.ui.MainViewModel
import hu.nutrinaplo.app.ui.NutriApp
import hu.nutrinaplo.app.ui.NutriTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriTheme {
                // A Surface adja a helyes (világos/sötét) alap szövegszínt minden képernyőnek.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NutriApp(vm)
                }
            }
        }
    }
}
