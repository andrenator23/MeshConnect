package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DeviceEntity
import com.example.data.MessageEntity
import com.example.p2p.MeshNetworkEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppUI(viewModel: ChatViewModel) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val secureChannelEnabled by viewModel.secureChannelEnabled.collectAsStateWithLifecycle()
    val wifiDirectActive by viewModel.wifiDirectActive.collectAsStateWithLifecycle()
    val networkLogs by viewModel.networkLogs.collectAsStateWithLifecycle()
    val selfName by viewModel.selfName.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Chats, 1 = Mesh Topology, 2 = Node Profile

    // Find current peer details
    val currentPeer = devices.find { it.id == activeChatId }
    val currentChatTitle = when (activeChatId) {
        "mesh_broadcast" -> "General Channel (Mesh Broadcast)"
        else -> currentPeer?.name ?: "Unknown Mesh Destination"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hive,
                                contentDescription = "Mesh Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MeshConnect",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.5).sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (wifiDirectActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SECURE OFFLINE NETWORK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleSecureHandshake() },
                        modifier = Modifier.testTag("secure_toggle")
                    ) {
                        Icon(
                            imageVector = if (secureChannelEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Security Toggle",
                            tint = if (secureChannelEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.startDiscovery() },
                        modifier = Modifier.testTag("scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Scan Peers",
                            tint = if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.ChatBubble, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    modifier = Modifier.testTag("tab_chats")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Hub, contentDescription = "Topology Map") },
                    label = { Text("Mesh Map") },
                    modifier = Modifier.testTag("tab_topology")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Profile & Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> ChatsTab(
                    devices = devices,
                    activeMessages = activeMessages,
                    activeChatId = activeChatId,
                    currentChatTitle = currentChatTitle,
                    currentPeer = currentPeer,
                    secureChannelEnabled = secureChannelEnabled,
                    wifiDirectActive = wifiDirectActive,
                    onChatSelect = { viewModel.selectChat(it) },
                    onSendMessage = { text, file, name, size -> viewModel.sendMessage(text, file, name, size) },
                    onSimulateReply = { text -> viewModel.injectIncomingPeerSimulation(activeChatId, text) },
                    onSimulateFileRx = { viewModel.injectIncomingPeerSimulation(activeChatId, "", true) }
                )
                1 -> MeshTopologyTab(
                    devices = devices,
                    isScanning = isScanning,
                    networkLogs = networkLogs,
                    onStartScan = { viewModel.startDiscovery() },
                    onAddNode = { name, type, strength -> viewModel.addSimulatedNode(name, type, strength) },
                    onToggleOnline = { id, online -> viewModel.toggleNodeOnlineState(id, online) },
                    onDeleteNode = { viewModel.deleteNode(it) },
                    onClearLogs = { viewModel.meshEngine.addLog("Re-initializing developer logs...", MeshNetworkEngine.LogType.INFO) }
                )
                2 -> ProfileSettingsTab(
                    selfName = selfName,
                    onUpdateName = { viewModel.updateSelfName(it) },
                    onWipeData = { viewModel.wipeLocalStorage() }
                )
            }
        }
    }
}

// ==========================================
// CHATS TAB IMPLEMENTATION (MASTER-DETAIL FOR SMARTPHONES)
// ==========================================
@Composable
fun ChatsTab(
    devices: List<DeviceEntity>,
    activeMessages: List<MessageEntity>,
    activeChatId: String,
    currentChatTitle: String,
    currentPeer: DeviceEntity?,
    secureChannelEnabled: Boolean,
    wifiDirectActive: Boolean,
    onChatSelect: (String) -> Unit,
    onSendMessage: (String, String?, String?, Long?) -> Unit,
    onSimulateReply: (String) -> Unit,
    onSimulateFileRx: () -> Unit
) {
    var showPeerListOnMobile by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Conversation List column (visible on tablet or when toggled on mobile)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "MESH CONVERSATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Mesh Broadcast Group
                    item {
                        ConversationListItem(
                            title = "General Channel (Mesh Broadcast)",
                            subtitle = "Dynamic flood-routing to nearby devices",
                            icon = Icons.Default.CellTower,
                            isOnline = true,
                            isSelected = activeChatId == "mesh_broadcast",
                            rssi = null,
                            hopCount = null,
                            isMeshGroup = true,
                            onClick = { onChatSelect("mesh_broadcast") }
                        )
                    }

                    items(devices) { device ->
                        ConversationListItem(
                            title = device.name,
                            subtitle = if (device.isOnline) "Direct: ${device.communicationType}" else "Offline / Out of Range",
                            icon = when (device.communicationType) {
                                "WI_FI" -> Icons.Default.Wifi
                                "MESH" -> Icons.Default.Lan
                                else -> Icons.Default.Bluetooth
                            },
                            isOnline = device.isOnline,
                            isSelected = activeChatId == device.id,
                            rssi = device.rssi,
                            hopCount = device.hopCount,
                            isMeshGroup = false,
                            onClick = { onChatSelect(device.id) }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(2f)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(0.dp))
        ) {
            ChatLogView(
                currentChatTitle = currentChatTitle,
                activeMessages = activeMessages,
                currentPeer = currentPeer,
                secureChannelEnabled = secureChannelEnabled,
                wifiDirectActive = wifiDirectActive,
                onSendMessage = onSendMessage,
                onSimulateReply = onSimulateReply,
                onSimulateFileRx = onSimulateFileRx
            )
        }
    }
}

@Composable
fun ConversationListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isOnline: Boolean,
    isSelected: Boolean,
    rssi: Int?,
    hopCount: Int?,
    isMeshGroup: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isMeshGroup) MaterialTheme.colorScheme.primaryContainer 
                        else if (isOnline) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isMeshGroup) MaterialTheme.colorScheme.primary 
                           else if (isOnline) MaterialTheme.colorScheme.tertiary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOnline) MaterialTheme.colorScheme.onSurfaceVariant else Color.Red.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isMeshGroup && isOnline) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (rssi != null) {
                        Text(
                            text = "📶 $rssi dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rssi > -70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (hopCount != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hopCount == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(1.dp)
                        ) {
                            Text(
                                text = "Hops: $hopCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatLogView(
    currentChatTitle: String,
    activeMessages: List<MessageEntity>,
    currentPeer: DeviceEntity?,
    secureChannelEnabled: Boolean,
    wifiDirectActive: Boolean,
    onSendMessage: (String, String?, String?, Long?) -> Unit,
    onSimulateReply: (String) -> Unit,
    onSimulateFileRx: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    var selectedTracePath by remember { mutableStateOf<MessageEntity?>(null) }

    // Scroll to bottom on receipt of new messages
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Window Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentPeer == null || currentPeer.isOnline) MaterialTheme.colorScheme.tertiary 
                                else Color.Red.copy(alpha = 0.7f)
                            )
                    )
                    Text(
                        text = currentChatTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Secure tag
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            imageVector = if (secureChannelEnabled) Icons.Default.VerifiedUser else Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (secureChannelEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (secureChannelEnabled) "AES E2E Security Active" else "Clear-text payload (Insecure)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (secureChannelEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentPeer != null) {
                        Text(
                            text = "Transport: ${if (currentPeer.hopCount == 1) currentPeer.communicationType else "Multi-Hop Mesh"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Message Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Textsms,
                        contentDescription = "Empty",
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Offline Local Messages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Type a secure off-grid message. It will advertise and route dynamically via surrounding peer beacons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeMessages) { message ->
                        MessageBubble(
                            message = message,
                            onTraceClick = { selectedTracePath = message }
                        )
                    }
                }
            }
        }

        // Quick simulation helper shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Simulate Node Action:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AssistChip(
                onClick = { onSimulateReply("Coordinates received. Deploying off-grid beacons now.") },
                label = { Text("Peer Text Message") }
            )

            AssistChip(
                onClick = { onSimulateFileRx() },
                label = { Text("WiFi File Transfer") },
                leadingIcon = { Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(12.dp)) }
            )
        }

        // Reply input bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showFileOptions by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        onClick = { showFileOptions = true },
                        modifier = Modifier.testTag("file_attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showFileOptions,
                        onDismissRequest = { showFileOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Send Photo (blueprint_v2.png - 4.8MB)") },
                            onClick = {
                                onSendMessage("", "mock://blueprint_v2.png", "blueprint_v2.png", 4800000L)
                                showFileOptions = false
                            },
                            leadingIcon = { Icon(Icons.Default.Photo, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Send Data (survey_gps.bin - 12MB)") },
                            onClick = {
                                onSendMessage("", "mock://survey_gps.bin", "survey_gps.bin", 12000000L)
                                showFileOptions = false
                            },
                            leadingIcon = { Icon(Icons.Default.Dns, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Send Doc (tactical_ops.pdf - 25MB)") },
                            onClick = {
                                onSendMessage("", "mock://tactical_ops.pdf", "tactical_ops.pdf", 25000000L)
                                showFileOptions = false
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                        )
                    }
                }

                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Type secure off-grid message...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            onSendMessage(textState, null, null, null)
                            textState = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_message_button"),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }

    // Packet hops path detail trace dialog
    selectedTracePath?.let { msg ->
        PacketTraceDialog(
            message = msg,
            onDismiss = { selectedTracePath = null }
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onTraceClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = formatter.format(Date(message.timestamp))

    val isSelf = message.isOutgoing
    val alignment = if (isSelf) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = alignment
        ) {
            // Sender display label (if not self)
            if (!isSelf) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSelf) 16.dp else 2.dp,
                            bottomEnd = if (isSelf) 2.dp else 16.dp
                        )
                    )
                    .background(
                        if (isSelf) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    // Check if it has a Wi-Fi file payload
                    if (message.filePath != null) {
                        WifiFileAttachmentCard(
                            fileName = message.fileName ?: "file.dat",
                            fileSize = message.fileSize ?: 0L,
                            progress = message.fileProgress,
                            isSelf = isSelf
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            color = (if (isSelf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                .copy(alpha = 0.6f)
                        )

                        if (isSelf) {
                            val (tickIcon, colorState) = when (message.status) {
                                "PENDING" -> Pair(Icons.Default.HourglassEmpty, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                                "SENT" -> Pair(Icons.Default.Check, MaterialTheme.colorScheme.onPrimary)
                                "ROUTED" -> Pair(Icons.Default.ForkRight, MaterialTheme.colorScheme.onPrimary)
                                else -> Pair(Icons.Default.DoneAll, MaterialTheme.colorScheme.secondary) // DELIVERED
                            }
                            Icon(
                                imageVector = tickIcon,
                                contentDescription = message.status,
                                modifier = Modifier.size(13.dp),
                                tint = colorState
                            )
                        }
                    }
                }
            }

            // Interactive Hop-path trace clicker subtext
            if (message.hopPath.isNotEmpty()) {
                Surface(
                    onClick = onTraceClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hive,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Route: ${message.hopPath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WifiFileAttachmentCard(
    fileName: String,
    fileSize: Long,
    progress: Int?,
    isSelf: Boolean
) {
    val displaySize = remember(fileSize) {
        if (fileSize > 1024 * 1024) "${String.format("%.1f", fileSize / (1024.0 * 1024.0))} MB"
        else "${fileSize / 1024} KB"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelf) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) Icons.Default.Image else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$displaySize • Wi-Fi P2P Direct",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (progress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Transferring: $progress%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "18.4 MB/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// PACKET ROUTE INFO DIALOG
// ==========================================
@Composable
fun PacketTraceDialog(
    message: MessageEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Traffic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Offline Mesh Routing Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TraceItemRow(label = "Packet ID", value = message.id.take(16) + "...", icon = Icons.Default.Fingerprint)
                    TraceItemRow(label = "Security Protocol", value = "AES-GCM (ECDH Cryptographic Handshake)", icon = Icons.Default.Security)
                    TraceItemRow(label = "Status Vector", value = "DELIVERED & ACKNOWLEDGED", icon = Icons.Default.PlaylistAddCheck)
                    TraceItemRow(label = "Signal Transit Path", value = message.hopPath, icon = Icons.Default.AltRoute)
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Text(
                    text = "Bluetooth and Wi-Fi mesh networking automatically routes secure data packets store-and-forward hopping across safe adjacent devices without cellular tower tower range or network signals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close Telemetry")
                }
            }
        }
    }
}

@Composable
fun TraceItemRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==========================================
// MESH MAP & DIAGNOSTICS TAB
// ==========================================
@Composable
fun MeshTopologyTab(
    devices: List<DeviceEntity>,
    isScanning: Boolean,
    networkLogs: List<MeshNetworkEngine.NetworkLog>,
    onStartScan: () -> Unit,
    onAddNode: (String, String, String) -> Unit,
    onToggleOnline: (String, Boolean) -> Unit,
    onDeleteNode: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    var showAddNodeForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ============== BENTO BLOCK 1: NETWORK TOPOLOGY DYNAMIC HERO (FULL WIDTH) ==============
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                Color(0xFF1E1B4B) // Premium dark violet finish
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                // Glow circles visual element
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(100.dp)
                ) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.width,
                        center = Offset(size.width * 1.1f, -size.height * 0.1f)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NETWORK TOPOLOGY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.2.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFF34D399),
                            contentColor = Color(0xFF064E3B)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${devices.count { it.isOnline } + 1}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Active Nodes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Graph visual indicator
                        Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.width(3.dp).height(12.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.width(3.dp).height(18.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.width(3.dp).height(14.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.dp)))
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Mesh integrity: 98%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ============== BENTO BLOCKS 2 & 3: SPLIT MINI METRIC PANELS ==============
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Bento Block 2: Bluetooth metric
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(136.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Channel",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "BLUETOOTH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.61f)
                        )
                        Text(
                            text = "6.2 m",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Avg Proximity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Bento Block 3: WiFi metrics
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(136.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "WiFi direct capacity",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WIFI-P2P",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.61f)
                        )
                        Text(
                            text = "480 Mbps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Burst Capacity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ============== BENTO BLOCK 4: SCANNER RADAR & CONTROLS ==============
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LOCAL GRAPH RADAR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Circular scanning concentric visualizer
                val infiniteTransition = rememberInfiniteTransition()
                val radiusProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .drawBehind {
                            val center = Offset(size.width / 2, size.height / 2)
                            val maxRadius = size.width / 2

                            // Outer waves
                            drawCircle(
                                color = Color(0xFF6366F1),
                                radius = maxRadius * if (isScanning) radiusProgress else 0.5f,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 2.dp.toPx()
                                ),
                                alpha = if (isScanning) (1f - radiusProgress) else 0.25f
                            )
                            drawCircle(
                                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                radius = maxRadius * 0.75f,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.dp.toPx()
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Radar Antenna",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onStartScan,
                        enabled = !isScanning,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...", fontSize = 13.sp)
                        } else {
                            Text("Scan Local Mesh", fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showAddNodeForm = true },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Custom Node", fontSize = 13.sp)
                    }
                }
            }
        }

        // ============== BENTO BLOCK 5: MESH DIRECTORY & SHORTEST ROUTE VECTORS ==============
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "NEARBY CHANNELS & ROUTE NODES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 10.dp),
                    letterSpacing = 0.5.sp
                )

                if (devices.isEmpty()) {
                    Text(
                        text = "No wireless nodes recorded. Scan or add node.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    devices.forEach { device ->
                        NodeControlRow(
                            device = device,
                            onToggleOnline = { onToggleOnline(device.id, device.isOnline) },
                            onDelete = { onDeleteNode(device.id) }
                        )
                    }
                }
            }
        }

        // ============== BENTO BLOCK 6: AUDIT HANDSHAKE LOGS LOGISTICS ==============
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECURITY AUDIT PROTOCOL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = onClearLogs, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Logs", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(networkLogs) { log ->
                            val color = when (log.type) {
                                MeshNetworkEngine.LogType.SECURE -> MaterialTheme.colorScheme.secondary
                                MeshNetworkEngine.LogType.BLUETOOTH -> Color(0xFF3B82F6)
                                MeshNetworkEngine.LogType.WI_FI -> Color(0xFFA855F7)
                                MeshNetworkEngine.LogType.ROUTING -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                            val timestampStr = formatter.format(Date(log.timestamp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "[$timestampStr] ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNodeForm) {
        AddSimulatedNodeDialog(
            onDismiss = { showAddNodeForm = false },
            onAdd = { name, type, strength ->
                onAddNode(name, type, strength)
                showAddNodeForm = false
            }
        )
    }
}

@Composable
fun NodeControlRow(
    device: DeviceEntity,
    onToggleOnline: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (device.communicationType) {
                    "WI_FI" -> Icons.Default.Wifi
                    "MESH" -> Icons.Default.Lan
                    else -> Icons.Default.Bluetooth
                },
                contentDescription = null,
                tint = if (device.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (device.isOnline) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "Hop Count: ${device.hopCount} • ${if (device.isOnline) "ONLINE" else "DISCONNECTED"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (device.isOnline) MaterialTheme.colorScheme.secondary else Color.Red.copy(alpha = 0.6f)
                )
            }

            // Power button to toggle online (Mesh Healing Demonstration)
            IconButton(onClick = onToggleOnline) {
                Icon(
                    imageVector = if (device.isOnline) Icons.Default.PowerSettingsNew else Icons.Default.PowerOff,
                    contentDescription = "Toggle Node",
                    tint = if (device.isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Node",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSimulatedNodeDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var connectionType by remember { mutableStateOf("BLUETOOTH") } // BLUETOOTH, WI_FI, MESH
    var signalStrength by remember { mutableStateOf("STRONG") } // STRONG, MEDIUM, WEAK

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Simulate New Mesh Peer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Beacon Name") },
                    placeholder = { Text("e.g. Phone C - Warehouse") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = "Transport Capability",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("BLUETOOTH", "WI_FI", "MESH").forEach { type ->
                        FilterChip(
                            selected = connectionType == type,
                            onClick = { connectionType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Text(
                    text = "Channel Link Signal Strength",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("STRONG", "MEDIUM", "WEAK").forEach { str ->
                        FilterChip(
                            selected = signalStrength == str,
                            onClick = { signalStrength = str },
                            label = { Text(str) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(name, connectionType, signalStrength) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sprout Node")
                    }
                }
            }
        }
    }
}

// ==========================================
// PROFILE & SETTINGS TAB
// ==========================================
@Composable
fun ProfileSettingsTab(
    selfName: String,
    onUpdateName: (String) -> Unit,
    onWipeData: () -> Unit
) {
    var editName by remember { mutableStateOf(selfName) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "OFF-GRID NODE PROFILE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Text(text = "Current Mesh Broadcast ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "self_node", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                TextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Your Discovered Mesh Display Name") },
                    trailingIcon = {
                        IconButton(onClick = { onUpdateName(editName) }) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save Profile Name")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Text(
            text = "SYSTEM OFF-GRID PREFERENCES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsToggleRow(
                    title = "AES-GCM Encryption Key Verification",
                    description = "Encrypt point-to-point and flood mesh payloads using secure local curves.",
                    initial = true
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                SettingsToggleRow(
                    title = "Background Store & Forward Relaying",
                    description = "Allow other offline devices to securely use your antenna to hop packets.",
                    initial = true
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                SettingsToggleRow(
                    title = "Adaptive Power Scaling (RSSI Proximity)",
                    description = "Increase antenna gain dynamically on low quality channels to prevent dropouts.",
                    initial = false
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showWipeConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hard Wipe Local Telemetry & Schemas")
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Erase Local Databases?") },
            text = { Text("This will permanently drop the local Room database caches, purging all off-grid message caches and active connections directories.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWipeData()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Wipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    initial: Boolean
) {
    var checked by remember { mutableStateOf(initial) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}
