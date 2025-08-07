package com.github.aarmam.eid.dca.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.aarmam.eid.dca.ui.theme.EIDWithDigitalCredentialsAPITheme
import com.github.aarmam.eid.dca.viewmodel.MainUiState

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    uiState: MainUiState = MainUiState(),
    onRegisterClick: (String, String) -> Unit = { _, _ -> },
    onRemoveClick: () -> Unit = {}
) {

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "eID with Digital Credentials API",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Show error message if present
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Show loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState.surname != null && uiState.givenName != null && uiState.serialNumber != null) {
                Text(
                    text = "Registered Authentication Credential details",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Surname: ${uiState.surname}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Given Name: ${uiState.givenName}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Serial Number: ${uiState.serialNumber}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Button(
                    onClick = onRemoveClick
                ) {
                    Text(text = "Remove authentication credential")
                }
            } else {
                var canNumber by remember { mutableStateOf("") }
                var pin2Number by remember { mutableStateOf("") }
                var activeInput by remember { mutableStateOf("can") } // "can" or "pin2"

                Text(
                    text = "When registering your ID card, the application creates a authentication credential signed by the ID card. The signing operation requires PIN2. Tap your ID card on the NFC reader and click Create Credential.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // CAN Number Input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "CAN number:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.Start
                    ) {
                        repeat(6) { index ->
                            Card(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(horizontal = 2.dp)
                                    .clickable { activeInput = "can" },
                                shape = RoundedCornerShape(6.dp),
                                colors = if (activeInput == "can") {
                                    androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                } else {
                                    androidx.compose.material3.CardDefaults.cardColors()
                                }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = if (index < canNumber.length) canNumber[index].toString() else "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // PIN2 Input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "PIN2:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.Start
                    ) {
                        repeat(5) { index ->
                            Card(
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(horizontal = 2.dp)
                                    .clickable { activeInput = "pin2" },
                                shape = RoundedCornerShape(6.dp),
                                colors = if (activeInput == "pin2") {
                                    androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                } else {
                                    androidx.compose.material3.CardDefaults.cardColors()
                                }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = if (index < pin2Number.length) "●" else "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Single Shared Keypad
                NumericKeypad(
                    currentValue = if (activeInput == "can") canNumber else pin2Number,
                    maxLength = if (activeInput == "can") 6 else 5,
                    onValueChange = { newValue ->
                        if (activeInput == "can") {
                            canNumber = newValue
                        } else {
                            pin2Number = newValue
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { onRegisterClick(canNumber, pin2Number) },
                    enabled = canNumber.length == 6 && pin2Number.length == 5 && !uiState.isLoading
                ) {
                    Text(text = "Create credential")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    EIDWithDigitalCredentialsAPITheme {
        MainScreen(
            uiState = MainUiState(
                surname = "DEMO",
                givenName = "TEST",
                serialNumber = "PNOEE-00000000000"
            )
        )
    }
}

@Composable
private fun NumericKeypad(
    currentValue: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Row 1: 1, 2, 3
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            repeat(3) { index ->
                NumericButton(
                    digit = (index + 1).toString(),
                    onClick = {
                        if (currentValue.length < maxLength) {
                            onValueChange(currentValue + (index + 1).toString())
                        }
                    }
                )
            }
        }

        // Row 2: 4, 5, 6
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            repeat(3) { index ->
                NumericButton(
                    digit = (index + 4).toString(),
                    onClick = {
                        if (currentValue.length < maxLength) {
                            onValueChange(currentValue + (index + 4).toString())
                        }
                    }
                )
            }
        }

        // Row 3: 7, 8, 9
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            repeat(3) { index ->
                NumericButton(
                    digit = (index + 7).toString(),
                    onClick = {
                        if (currentValue.length < maxLength) {
                            onValueChange(currentValue + (index + 7).toString())
                        }
                    }
                )
            }
        }

        // Row 4: Clear, 0, (empty)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .size(60.dp)
                    .clickable {
                        if (currentValue.isNotEmpty()) {
                            onValueChange(currentValue.dropLast(1))
                        }
                    },
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "⌫",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            NumericButton(
                digit = "0",
                onClick = {
                    if (currentValue.length < maxLength) {
                        onValueChange(currentValue + "0")
                    }
                }
            )

            // Empty space for symmetry
            Spacer(modifier = Modifier.size(60.dp))
        }
    }
}

@Composable
private fun NumericButton(
    digit: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}