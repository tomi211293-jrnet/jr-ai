package com.jrai.studio

import android.os.Bundle
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Page = Color(0xFF090B14)
private val Panel = Color(0xFF151A2A)
private val Accent = Color(0xFF8B7CFF)
private val Muted = Color(0xFFA7AEC3)

private enum class AppPage { CHAT, PHOTO, VIDEO, GALLERY }

private data class ChatMessage(
    val role: String,
    val content: String,
    val imageBase64: String? = null
)

private fun isImageGenerationRequest(text: String): Boolean {
    val normalized = text.lowercase()
    val triggers = listOf(
        "buat gambar", "buatkan gambar", "bikin gambar", "hasilkan gambar",
        "generate gambar", "buat foto", "buatkan foto", "bikin foto",
        "hasilkan foto", "generate foto", "buat ilustrasi", "buat lukisan"
    )
    return triggers.any { normalized.contains(it) }
}

private suspend fun requestJrAiImage(prompt: String): String = withContext(Dispatchers.IO) {
    val connection = (URL("https://jr-ai-backend.tomi211293.workers.dev/generate-image").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20000
        readTimeout = 180000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
    }

    try {
        val requestBody = JSONObject().put("prompt", prompt.take(1800)).toString()
        connection.outputStream.use { output ->
            output.write(requestBody.toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val responseJson = try { JSONObject(responseText) } catch (_: Exception) { JSONObject() }

        if (responseCode !in 200..299 || !responseJson.optBoolean("success", false)) {
            throw IOException(responseJson.optString("error").ifBlank {
                "Generator gambar gagal (HTTP $responseCode)."
            })
        }
        responseJson.optString("image").takeIf { it.isNotBlank() }
            ?: throw IOException("Generator belum mengirim hasil gambar.")
    } finally {
        connection.disconnect()
    }
}

private suspend fun requestJrAiReply(history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
    val connection = (URL("https://jr-ai-backend.tomi211293.workers.dev/chat").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20000
        readTimeout = 90000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
    }

    try {
        val messagesJson = JSONArray()
        history.takeLast(20).forEach { message ->
            messagesJson.put(
                JSONObject()
                    .put("role", message.role)
                    .put("content", message.content.take(4000))
            )
        }
        val requestBody = JSONObject().put("messages", messagesJson).toString()
        connection.outputStream.use { output ->
            output.write(requestBody.toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val responseJson = try {
            JSONObject(responseText)
        } catch (_: Exception) {
            JSONObject()
        }

        if (responseCode !in 200..299 || !responseJson.optBoolean("success", false)) {
            throw IOException(responseJson.optString("error").ifBlank {
                "Layanan AI gagal merespons (HTTP $responseCode)."
            })
        }

        responseJson.optString("reply").takeIf { it.isNotBlank() }
            ?: throw IOException("JR AI belum memberikan jawaban.")
    } finally {
        connection.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JrAiApp() }
    }
}

@Composable
private fun JrAiApp() {
    var currentPage by remember { mutableStateOf(AppPage.CHAT) }
    BackHandler(enabled = currentPage != AppPage.CHAT) { currentPage = AppPage.CHAT }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Page) {
            when (currentPage) {
                AppPage.CHAT -> ChatScreen(
                    onPhoto = { currentPage = AppPage.PHOTO },
                    onVideo = { currentPage = AppPage.VIDEO },
                    onGallery = { currentPage = AppPage.GALLERY }
                )
                AppPage.PHOTO -> PhotoScreen(onBack = { currentPage = AppPage.CHAT })
                AppPage.VIDEO -> VideoScreen(onBack = { currentPage = AppPage.CHAT })
                AppPage.GALLERY -> GalleryScreen(onBack = { currentPage = AppPage.CHAT })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(onPhoto: () -> Unit, onVideo: () -> Unit, onGallery: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    var attachmentSheetOpen by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF111522),
                drawerContentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("JR AI", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup menu", tint = Muted)
                    }
                }
                NavigationDrawerItem(
                    label = { Text("Obrolan baru", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        messages.clear()
                        statusMessage = ""
                        draft = ""
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color(0xFF20263A),
                        unselectedTextColor = Color.White,
                        unselectedIconColor = Color.White
                    )
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "Riwayat obrolan",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                )
                if (messages.isEmpty()) {
                    Text(
                        "Obrolan sebelumnya akan muncul di sini.",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                        Text(
                            messages.first().content.take(32),
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 30.dp).height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Buka menu obrolan", tint = Color.White)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("JR AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(48.dp))
            }

            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(54.dp).background(
                                Brush.linearGradient(listOf(Color(0xFF5146A8), Color(0xFF24294A))), CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
                        }
                        Text("Ada yang bisa saya bantu?", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Tanyakan apa saja atau mulai buat sesuatu.", color = Muted, fontSize = 13.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    messages.forEach { message ->
                        if (message.role == "user") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Box(
                                    modifier = Modifier.background(Color(0xFF30345A), RoundedCornerShape(20.dp)).padding(horizontal = 15.dp, vertical = 11.dp)
                                ) {
                                    Text(message.content, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                                    if (message.imageBase64 != null) {
                                        val bitmap = remember(message.imageBase64) {
                                            try {
                                                val bytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                            } catch (_: Exception) { null }
                                        }
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap,
                                                    contentDescription = "Gambar hasil AI",
                                                    modifier = Modifier.fillMaxWidth().height(280.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text("Gambar diterima tetapi tidak dapat ditampilkan.", color = Color.White, fontSize = 13.sp)
                                            }
                                            Text(message.content, color = Muted, fontSize = 12.sp)
                                        }
                                    } else {
                                        Text(message.content, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (isSending) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                            Text("JR AI sedang berpikir...", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                    if (statusMessage.isNotBlank()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                            Text(statusMessage, color = Color(0xFFFFD27A), fontSize = 13.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        minLines = 2,
                        maxLines = 6,
                        placeholder = { Text("Tanyakan apa saja", color = Muted) },
                        leadingIcon = {
                            IconButton(onClick = { attachmentSheetOpen = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Lampiran", tint = Color.White)
                            }
                        },
                        trailingIcon = {
                            if (draft.isBlank() || isSending) {
                                IconButton(onClick = { }) {
                                    if (isSending) {
                                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Mic, contentDescription = "Suara (segera hadir)", tint = Muted)
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val textToSend = draft.trim()
                                        if (textToSend.isNotEmpty() && !isSending) {
                                            messages.add(ChatMessage("user", textToSend))
                                            draft = ""
                                            statusMessage = ""
                                            isSending = true
                                            scope.launch {
                                                try {
                                                    if (isImageGenerationRequest(textToSend)) {
                                                        val image = requestJrAiImage(textToSend)
                                                        messages.add(ChatMessage("assistant", "Gambar berhasil dibuat.", image))
                                                    } else {
                                                        val reply = requestJrAiReply(messages.toList())
                                                        messages.add(ChatMessage("assistant", reply))
                                                    }
                                                } catch (error: Exception) {
                                                    statusMessage = error.message ?: "Tidak dapat terhubung ke JR AI."
                                                } finally {
                                                    isSending = false
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Kirim", tint = Accent)
                                }
                            }
                        },
                        shape = RoundedCornerShape(22.dp)
                    )
                }
            }
        }
    }

    if (attachmentSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { attachmentSheetOpen = false },
            sheetState = sheetState,
            containerColor = Color(0xFF171C2B),
            contentColor = Color.White,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Muted) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Tambahkan ke JR AI", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                AttachmentOption("Foto dari galeri", "Pilih foto dari perangkat", Icons.Default.Image) {
                    attachmentSheetOpen = false
                    onPhoto()
                }
                AttachmentOption("Buat gambar", "Hasilkan gambar dari deskripsi teks", Icons.Default.AutoAwesome) {
                    attachmentSheetOpen = false
                    onPhoto()
                }
                AttachmentOption("Buat video", "Buat video dari teks atau foto", Icons.Default.Movie) {
                    attachmentSheetOpen = false
                    onVideo()
                }
                AttachmentOption("Galeri saya", "Lihat hasil yang tersimpan", Icons.Default.Collections) {
                    attachmentSheetOpen = false
                    onGallery()
                }
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF242B40), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(Color(0xFF343B58), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Column(modifier = Modifier.padding(start = 13.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SuggestionCard(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(13.dp)
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(13.dp))
    }
}

@Composable
private fun PhotoScreen(onBack: () -> Unit) {
    var selectedPhoto by remember { mutableStateOf<android.net.Uri?>(null) }
    var editPrompt by remember { mutableStateOf("") }
    var createPrompt by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPhoto = uri
        message = if (uri != null) "Foto berhasil dipilih." else ""
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader("Buat Foto", onBack)
        Text("FOTO DARI TEKS", color = Color(0xFFC9C2FF), fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = createPrompt, onValueChange = { createPrompt = it }, modifier = Modifier.fillMaxWidth(),
            minLines = 3, label = { Text("Jelaskan foto yang kamu inginkan") },
            placeholder = { Text("Contoh: foto sinematik motor di jalan saat hujan malam...") }
        )
        Text("FOTO DARI FOTO", color = Color(0xFFC9C2FF), fontWeight = FontWeight.Bold)
        Text("Pilih foto, lalu jelaskan bagian yang ingin diubah.", color = Muted, fontSize = 13.sp)
        OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Image, contentDescription = null)
            Text(if (selectedPhoto == null) "Pilih Foto dari HP" else "Ganti Foto")
        }
        OutlinedTextField(
            value = editPrompt, onValueChange = { editPrompt = it }, modifier = Modifier.fillMaxWidth(),
            minLines = 3, label = { Text("Instruksi edit foto") },
            placeholder = { Text("Contoh: ubah latar menjadi pantai saat senja...") }
        )
        Button(
            onClick = {
                message = when {
                    createPrompt.isBlank() && (selectedPhoto == null || editPrompt.isBlank()) -> "Isi prompt atau pilih foto dan tulis instruksi."
                    else -> "Tampilan siap. Generator AI belum dihubungkan; belum ada gambar yang dibuat."
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) { Text("Generate Foto", color = Color.White) }
        if (message.isNotBlank()) Text(message, color = Color(0xFFFFD27A), fontSize = 13.sp)
        Text("Hasil akan ditampilkan di JR AI setelah mesin generator terhubung.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun VideoScreen(onBack: () -> Unit) {
    var selectedPhoto by remember { mutableStateOf<android.net.Uri?>(null) }
    var prompt by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("5 detik") }
    var message by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedPhoto = uri
        message = if (uri != null) "Foto berhasil dipilih sebagai bahan video." else ""
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader("Buat Video", onBack)
        Text("VIDEO DARI TEKS / FOTO", color = Color(0xFFC9C2FF), fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Image, contentDescription = null)
            Text(if (selectedPhoto == null) "Pilih Foto (opsional)" else "Ganti Foto")
        }
        OutlinedTextField(
            value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(),
            minLines = 4, label = { Text("Deskripsi adegan dan gerakan") },
            placeholder = { Text("Contoh: kamera mendekat perlahan, rambut tertiup angin...") }
        )
        Text("DURASI", color = Color(0xFFC9C2FF), fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("5 detik", "10 detik").forEach { item ->
                OutlinedButton(onClick = { duration = item }) {
                    Text(if (duration == item) "✓ $item" else item)
                }
            }
        }
        Button(
            onClick = {
                message = when {
                    prompt.isBlank() -> "Tuliskan deskripsi video terlebih dahulu."
                    else -> "Tampilan siap. Generator video belum dihubungkan; belum ada video yang dibuat."
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) { Text("Generate Video • $duration", color = Color.White) }
        if (message.isNotBlank()) Text(message, color = Color(0xFFFFD27A), fontSize = 13.sp)
        Text("Hasil video akan ditampilkan di JR AI setelah mesin generator terhubung.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White,
            modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GalleryScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeader("Galeri Saya", onBack)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Collections, contentDescription = null, tint = Accent)
                Text("Belum ada hasil", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Foto dan video hasil AI akan tersimpan di sini setelah generator dan penyimpanan diaktifkan.",
                    color = Muted, fontSize = 13.sp
                )
            }
        }
    }
}
