@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisense.remote.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisense.remote.model.DiscoveredTv

@Composable
fun MainScreen(viewModel: RemoteViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val discoveredTvs by viewModel.discoveredTvs.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val kbBuffer by viewModel.kbBuffer.collectAsState()
    val context = LocalContext.current

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            if (!matches.isNullOrEmpty()) {
                val spoken = matches[0]
                viewModel.sendText(spoken)
                Toast.makeText(context, "🎤 \"$spoken\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Mic permission
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            voiceLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Microphone permission needed for voice search", Toast.LENGTH_SHORT).show()
        }
    }

    var showKeyboard by remember { mutableStateOf(false) }
    var ipInput by remember { mutableStateOf("") }
    var macInput by remember { mutableStateOf("") }
    var showPairing by remember { mutableStateOf(false) }
    var pairingCodeInput by remember { mutableStateOf("") }

    // Detect pairing code from TV
    LaunchedEffect(state.pairingCode) {
        if (state.pairingCode.isNotEmpty() && !state.paired) {
            showPairing = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // ═══ HEADER ═══
        Header(state)

        Spacer(Modifier.height(12.dp))

        // ═══ CONNECTION PANEL (when not connected) ═══
        if (!state.connected) {
            ConnectionPanel(
                ipInput = ipInput,
                onIpChange = { ipInput = it },
                macInput = macInput,
                onMacChange = { macInput = it },
                onConnect = { viewModel.connect(ipInput, macInput) },
                onDiscover = { viewModel.discoverTvs() },
                isDiscovering = isDiscovering,
                discoveredTvs = discoveredTvs,
                onSelectTv = { ipInput = it.ip; viewModel.connect(it.ip) },
                errorMessage = state.errorMessage,
            )

            // Pairing section
            if (showPairing && state.pairingCode.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                PairingSection(
                    tvCode = state.pairingCode,
                    inputCode = pairingCodeInput,
                    onCodeChange = { pairingCodeInput = it },
                    onPair = {
                        viewModel.sendPairingCode(pairingCodeInput)
                        showPairing = false
                    }
                )
            }
        }

        // ═══ REMOTE CONTROL (when connected) ═══
        if (state.connected) {
            // TV info bar
            TvInfoBar(state.name)

            Spacer(Modifier.height(10.dp))

            // Top actions: Keyboard | Voice | Power | Mute
            TopActionsRow(
                onKeyboard = { showKeyboard = !showKeyboard },
                onVoice = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        voiceLauncher.launch(intent)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onPower = { viewModel.sendKey("power") },
                onMute = { viewModel.sendKey("mute") }
            )

            Spacer(Modifier.height(8.dp))

            // Quick text bar
            QuickTextBar(
                onSend = { text -> viewModel.sendText(text) },
                isSending = isSending
            )

            Spacer(Modifier.height(12.dp))

            // D-Pad
            DPad(onKey = { viewModel.sendKey(it) })

            Spacer(Modifier.height(8.dp))

            // Nav row: Back, Home, Menu, Exit
            NavigationRow(onKey = { viewModel.sendKey(it) })

            Spacer(Modifier.height(8.dp))

            // Volume & Channel Rockers
            VolumeChannelRow(
                volume = state.volume,
                onKey = { viewModel.sendKey(it) }
            )

            Spacer(Modifier.height(8.dp))

            // Number Pad
            NumberPad(onKey = { viewModel.sendKey(it) })

            Spacer(Modifier.height(8.dp))

            // Color Buttons
            ColorButtonsRow(onKey = { viewModel.sendKey(it) })

            Spacer(Modifier.height(8.dp))

            // Transport Controls
            TransportRow(onKey = { viewModel.sendKey(it) })

            Spacer(Modifier.height(8.dp))

            // App Shortcuts
            AppShortcutsRow(onApp = { viewModel.launchApp(it) })

            Spacer(Modifier.height(16.dp))
        }

        // Footer
        Footer()

        Spacer(Modifier.height(120.dp)) // Space for keyboard overlay
    }

    // ═══ QWERTY KEYBOARD OVERLAY ═══
    AnimatedVisibility(
        visible = showKeyboard,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.fillMaxSize()
    ) {
        QwertyKeyboardOverlay(
            buffer = kbBuffer,
            onAddChar = { viewModel.addCharToKb(it) },
            onRemoveChar = { viewModel.removeCharFromKb() },
            onClear = { viewModel.clearKb() },
            onSend = { viewModel.sendKbBuffer() },
            onClose = { showKeyboard = false }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun Header(state: com.hisense.remote.model.TvState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A73E8).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Tv, contentDescription = null,
                    tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Hisense Remote", fontWeight = FontWeight.W600, fontSize = 16.sp,
                    color = Color(0xFFE8EAED))
                Text("Vidaa Smart TV", fontSize = 11.sp, color = Color(0xFF9AA0A6))
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (state.connected) Color(0xFF34A853) else Color(0xFFEA4335))
                    .then(if (state.connected) Modifier.shadow(6.dp, CircleShape) else Modifier)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (state.connected) "Connected" else "Disconnected",
                fontSize = 12.sp,
                color = if (state.connected) Color(0xFF34A853) else Color(0xFF9AA0A6)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// CONNECTION PANEL
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun ConnectionPanel(
    ipInput: String, onIpChange: (String) -> Unit,
    macInput: String, onMacChange: (String) -> Unit,
    onConnect: () -> Unit, onDiscover: () -> Unit,
    isDiscovering: Boolean,
    discoveredTvs: List<DiscoveredTv>,
    onSelectTv: (DiscoveredTv) -> Unit,
    errorMessage: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Connect to TV", fontWeight = FontWeight.W600, fontSize = 15.sp,
                color = Color(0xFFE8EAED))

            // Error
            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEA4335).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.ErrorOutline, null,
                            tint = Color(0xFFEA4335), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage, color = Color(0xFFEA4335), fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = ipInput, onValueChange = onIpChange,
                placeholder = { Text("TV IP Address (e.g. 192.168.1.100)",
                    color = Color(0xFF9AA0A6)) },
                leadingIcon = { Icon(Icons.Filled.Computer, null, tint = Color(0xFF9AA0A6)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1A73E8),
                    unfocusedBorderColor = Color(0xFF333650),
                    cursorColor = Color(0xFF1A73E8),
                    focusedTextColor = Color(0xFFE8EAED),
                    unfocusedTextColor = Color(0xFFE8EAED),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = macInput, onValueChange = onMacChange,
                placeholder = { Text("MAC Address (optional for WoL)",
                    color = Color(0xFF9AA0A6)) },
                leadingIcon = { Icon(Icons.Filled.Devices, null, tint = Color(0xFF9AA0A6)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1A73E8),
                    unfocusedBorderColor = Color(0xFF333650),
                    cursorColor = Color(0xFF1A73E8),
                    focusedTextColor = Color(0xFFE8EAED),
                    unfocusedTextColor = Color(0xFFE8EAED),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🔗 Connect", fontWeight = FontWeight.W500)
                }

                OutlinedButton(
                    onClick = onDiscover,
                    enabled = !isDiscovering,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF9AA0A6)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Discover", fontWeight = FontWeight.W500)
                }
            }

            // Discovery results
            if (isDiscovering) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning network...", color = Color(0xFF9AA0A6), fontSize = 13.sp)
                }
            }

            if (discoveredTvs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                discoveredTvs.forEach { tv ->
                    Card(
                        onClick = { onSelectTv(tv) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF222533)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Tv, null, tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tv.name, fontSize = 14.sp, fontWeight = FontWeight.W500,
                                    color = Color(0xFFE8EAED))
                                Text("${tv.ip}:${tv.port}", fontSize = 12.sp,
                                    color = Color(0xFF9AA0A6))
                            }
                            Text("✓ Found", fontSize = 12.sp, color = Color(0xFF34A853))
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// PAIRING SECTION
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun PairingSection(
    tvCode: String, inputCode: String,
    onCodeChange: (String) -> Unit, onPair: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D27)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("🔐 Pair with TV", fontWeight = FontWeight.W600, fontSize = 15.sp,
                color = Color(0xFFE8EAED))
            Spacer(Modifier.height(8.dp))
            Text("Enter the 4-digit code shown on your TV:",
                color = Color(0xFF9AA0A6), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))

            // Code display from TV
            Text(tvCode,
                fontSize = 36.sp, fontWeight = FontWeight.W700,
                color = Color(0xFFFBBC04), letterSpacing = 8.sp,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = inputCode, onValueChange = { if (it.length <= 4) onCodeChange(it) },
                placeholder = { Text("0000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp, fontWeight = FontWeight.W700, letterSpacing = 8.sp,
                    textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1A73E8),
                    unfocusedBorderColor = Color(0xFF333650),
                    cursorColor = Color(0xFF1A73E8),
                    focusedTextColor = Color(0xFFE8EAED),
                    unfocusedTextColor = Color(0xFFE8EAED),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onPair,
                enabled = inputCode.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✅ Pair", fontWeight = FontWeight.W500)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// TV INFO BAR
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TvInfoBar(name: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222533)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📺 $name", fontWeight = FontWeight.W500, fontSize = 13.sp,
                color = Color(0xFFE8EAED))
            Text("● Online", fontSize = 12.sp, color = Color(0xFF34A853))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// TOP ACTIONS
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TopActionsRow(
    onKeyboard: () -> Unit, onVoice: () -> Unit,
    onPower: () -> Unit, onMute: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TopActionBtn(Icons.Filled.Keyboard, "Keyboard", Color(0xFF7C4DFF), onKeyboard)
        TopActionBtn(Icons.Filled.Mic, "Voice", Color(0xFF34A853), onVoice)
        TopActionBtn(Icons.Filled.PowerSettingsNew, "Power", Color(0xFFEA4335), onPower)
        TopActionBtn(Icons.Filled.VolumeOff, "Mute", Color(0xFF9AA0A6), onMute)
    }
}

@Composable
private fun RowScope.TopActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, color: Color, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D3B)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 9.sp, color = Color(0xFF9AA0A6))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// QUICK TEXT BAR
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun QuickTextBar(
    onSend: (String) -> Unit, isSending: Boolean
) {
    var text by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222533)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Type to search...", color = Color(0xFF9AA0A6)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotEmpty()) { onSend(text); text = "" }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color(0xFF1A73E8),
                    focusedTextColor = Color(0xFFE8EAED),
                    unfocusedTextColor = Color(0xFFE8EAED),
                ),
                modifier = Modifier.weight(1f)
            )

            SmallIconBtn(Icons.Filled.Close) { text = "" }
            SmallIconBtn(Icons.Filled.Send) {
                if (text.isNotEmpty()) { onSend(text); text = "" }
            }
        }
    }
}

@Composable
private fun SmallIconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector,
                         onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D3B)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFF9AA0A6), modifier = Modifier.size(18.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// D-PAD
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun DPad(onKey: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DPadBtn(Icons.Filled.KeyboardArrowUp) { onKey("up") }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPadBtn(Icons.Filled.KeyboardArrowLeft) { onKey("left") }
            // OK button
            Card(
                onClick = { onKey("ok") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8)),
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("OK", fontWeight = FontWeight.W700, fontSize = 14.sp,
                        color = Color.White)
                }
            }
            DPadBtn(Icons.Filled.KeyboardArrowRight) { onKey("right") }
        }
        Spacer(Modifier.height(4.dp))
        DPadBtn(Icons.Filled.KeyboardArrowDown) { onKey("down") }
    }
}

@Composable
private fun DPadBtn(icon: androidx.compose.ui.graphics.vector.ImageVector,
                    onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D3B)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(56.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// NAVIGATION ROW
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun NavigationRow(onKey: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NavBtn(Icons.Filled.ArrowBack, "Back") { onKey("back") }
        NavBtn(Icons.Filled.Home, "Home") { onKey("home") }
        NavBtn(Icons.Filled.Menu, "Menu") { onKey("menu") }
        NavBtn(Icons.Filled.Close, "Exit") { onKey("exit") }
    }
}

@Composable
private fun RowScope.NavBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D3B)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 9.sp, color = Color(0xFF9AA0A6))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// VOLUME & CHANNEL
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun VolumeChannelRow(volume: Int, onKey: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RockerGroup("VOL", volume, onKey, "volume_up", "volume_down")
        RockerGroup("CH", null, onKey, "channel_up", "channel_down")
    }
}

@Composable
private fun RowScope.RockerGroup(
    label: String, volume: Int?,
    onKey: (String) -> Unit, upKey: String, downKey: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222533)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.W700, fontSize = 11.sp,
                color = Color(0xFF9AA0A6), modifier = Modifier.width(24.dp))

            Spacer(Modifier.width(4.dp))

            // Up
            CircleBtn(Icons.Filled.ArrowDropUp, onClick = { onKey(upKey) })

            Spacer(Modifier.width(4.dp))

            // Volume bar
            if (volume != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF0F1117))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((volume.coerceIn(0, 100)) / 100f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1A73E8))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF0F1117))
                )
            }

            Spacer(Modifier.width(4.dp))

            // Down
            CircleBtn(Icons.Filled.ArrowDropDown, onClick = { onKey(downKey) })
        }
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector,
                      onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D3B)),
        shape = CircleShape,
        modifier = Modifier.size(36.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// NUMBER PAD
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun NumberPad(onKey: (String) -> Unit) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("TXT", "0", "INFO"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        numbers.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { n ->
                    Card(
                        onClick = { onKey(n.lowercase()) },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2A2D3B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.height(42.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(n,
                                fontSize = if (n.all { it.isDigit() }) 18.sp else 13.sp,
                                fontWeight = FontWeight.W600,
                                color = if (n.all { it.isDigit() }) Color(0xFFE8EAED)
                                    else Color(0xFF9AA0A6)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// COLOR BUTTONS
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun ColorButtonsRow(onKey: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ColorBtn("Red", Color(0xFFD93025)) { onKey("red") }
        ColorBtn("Green", Color(0xFF1E8E3E)) { onKey("green") }
        ColorBtn("Yellow", Color(0xFFF9AB00)) { onKey("yellow") }
        ColorBtn("Blue", Color(0xFF1967D2)) { onKey("blue") }
    }
}

@Composable
private fun RowScope.ColorBtn(
    label: String, color: Color, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.W600,
                color = if (label == "Yellow") Color(0xFF1A1A2E) else Color.White)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// TRANSPORT CONTROLS
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun TransportRow(onKey: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportCircle(Icons.Filled.FastRewind) { onKey("rewind") }
        TransportCircle(Icons.Filled.FiberManualRecord) { onKey("record") }
        // Play button (larger)
        Card(
            onClick = { onKey("play") },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8)),
            shape = CircleShape,
            modifier = Modifier.size(52.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White,
                    modifier = Modifier.size(28.dp))
            }
        }
        TransportCircle(Icons.Filled.Stop) { onKey("stop") }
        TransportCircle(Icons.Filled.FastForward) { onKey("fastforward") }
    }
}

@Composable
private fun TransportCircle(icon: androidx.compose.ui.graphics.vector.ImageVector,
                             onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2D3B)),
        shape = CircleShape,
        modifier = Modifier.size(44.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// APP SHORTCUTS
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun AppShortcutsRow(onApp: (String) -> Unit) {
    Column {
        Text("APPS", fontSize = 11.sp, fontWeight = FontWeight.W600,
            color = Color(0xFF9AA0A6), letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppShortcut("N", "Netflix", Color(0xFFE50914)) { onApp("netflix") }
            AppShortcut("▶", "YouTube", Color(0xFFFF0000)) { onApp("youtube") }
            AppShortcut("P", "Prime", Color(0xFF00A8E1)) { onApp("prime") }
            AppShortcut("D+", "Disney+", Color(0xFF113CCF)) { onApp("disney") }
        }
    }
}

@Composable
private fun RowScope.AppShortcut(
    logo: String, name: String, color: Color, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222533)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(logo, fontSize = 18.sp, fontWeight = FontWeight.W700, color = color)
            Spacer(Modifier.height(4.dp))
            Text(name, fontSize = 9.sp, color = Color(0xFF9AA0A6))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// FOOTER
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun Footer() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Make sure your TV and device are on the same WiFi network",
            fontSize = 11.sp, color = Color(0xFF9AA0A6).copy(alpha = 0.7f))
        Spacer(Modifier.height(2.dp))
        Text("Hisense Vidaa Remote v1.0 — MQTT on port 36669",
            fontSize = 10.sp, color = Color(0xFF545454))
    }
}

// ═══════════════════════════════════════════════════════════════════
// QWERTY KEYBOARD OVERLAY
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun QwertyKeyboardOverlay(
    buffer: String,
    onAddChar: (String) -> Unit,
    onRemoveChar: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var showNumbers by remember { mutableStateOf(false) }

    val letterRows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("z","x","c","v","b","n","m"),
    )
    val numbers = listOf("1","2","3","4","5","6","7","8","9","0")
    val symbols = listOf("-","_",".",",","!","?","@","#","/",":")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2130)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                start = 6.dp, end = 6.dp, top = 10.dp,
                bottom = 10.dp
            )
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Keyboard, null, tint = Color(0xFF7C4DFF),
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("TV Keyboard", fontSize = 13.sp, fontWeight = FontWeight.W600,
                    color = Color(0xFF9AA0A6))
                Spacer(Modifier.weight(1f))
                // Clear
                SmallKeyBtn(Icons.Filled.ClearAll, onClick = onClear)
                Spacer(Modifier.width(4.dp))
                // Close
                SmallKeyBtn(Icons.Filled.Close, onClick = onClose, color = Color(0xFFEA4335))
            }

            Spacer(Modifier.height(6.dp))

            // Text preview
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1117)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (buffer.isEmpty()) {
                        Text("Tap keys to type...",
                            color = Color(0xFF9AA0A6), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 13.sp)
                    } else {
                        Text(buffer, fontSize = 15.sp, color = Color(0xFFE8EAED),
                            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    if (buffer.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(2.dp)
                                .height(18.dp)
                                .background(Color(0xFF1A73E8))
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Keys
            if (!showNumbers) {
                letterRows.forEach { row ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        row.forEach { ch ->
                            KeyBtn(if (isShifted) ch.uppercase() else ch) {
                                onAddChar(ch)
                                if (isShifted && ch.all { it.isLetter() }) isShifted = false
                            }
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    numbers.forEach { n -> KeyBtn(n) { onAddChar(n) } }
                }
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    symbols.forEach { s -> KeyBtn(s) { onAddChar(s) } }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Bottom row
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                // Shift
                ModBtn(
                    icon = if (isShifted) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    active = isShifted,
                    modifier = Modifier.weight(14f)
                ) { isShifted = !isShifted }

                Spacer(Modifier.width(4.dp))

                // 123/ABC
                ModBtn(
                    icon = if (showNumbers) Icons.Filled.Keyboard else Icons.Filled.Dialpad,
                    modifier = Modifier.weight(14f)
                ) { showNumbers = !showNumbers }

                Spacer(Modifier.width(4.dp))

                // Space
                Card(
                    onClick = { onAddChar(" ") },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3040)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(50f).height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SpaceBar, null, tint = Color(0xFF9AA0A6),
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Space", fontSize = 11.sp, color = Color(0xFF9AA0A6))
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Backspace
                ModBtn(
                    icon = Icons.Filled.Backspace,
                    modifier = Modifier.weight(14f)
                ) { onRemoveChar() }

                Spacer(Modifier.width(4.dp))

                // Send
                Card(
                    onClick = onSend,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(22f).height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Send, null, tint = Color.White,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Send", fontSize = 11.sp, fontWeight = FontWeight.W600,
                            color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyBtn(label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3040)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(38.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W500,
                color = Color(0xFFE8EAED))
        }
    }
}

@Composable
private fun ModBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (active) Color(0xFF7C4DFF) else Color(0xFF3A3D50)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SmallKeyBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color = Color(0xFF9AA0A6)
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3D50)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}
