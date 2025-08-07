package com.github.aarmam.eid.dca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.aarmam.eid.dca.ui.MainScreen
import com.github.aarmam.eid.dca.ui.theme.EIDWithDigitalCredentialsAPITheme
import com.github.aarmam.eid.dca.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            EIDWithDigitalCredentialsAPITheme {
                MainScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onRegisterClick = { canNumber, pin2 ->
                        viewModel.registerIdCard(this, canNumber, pin2)
                    },
                    onRemoveClick = {
                        viewModel.removeIdCard()
                    }
                )
            }
        }
    }
}