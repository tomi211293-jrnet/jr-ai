package com.jrai.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Page = Color(0xFF090B14)
private val Panel = Color(0xFF151A2A)
private val Accent = Color(0xFF8B7CFF)
private val Muted = Color(0xFFA7AEC3)

private enum class AppPage { CHAT, PHOTO, VIDEO, GALLERY }

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

@Composable
private fun ChatScreen(onPhoto: () -> Unit, onVideo: () -> Unit, onGallery: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("JR AI", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text("ASISTEN PRIBADI • CREATIVE STUDIO", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
            Icon(Icons.Default.AutoAwesome, contentDescription = "JR AI", tint = Accent, modifier = Modifier.size(28.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction("Foto", Icons.Default.Image, onPhoto, Modifier.weight(1f))
            QuickAction("Video", Icons.Default.Movie, onVideo, Modifier.weight(1f))
            QuickAction("Galeri", Icons.Default.Collections, onGallery, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(68.dp).background(
                            Brush.linearGradient(listOf(Color(0xFF5146A8), Color(0xFF24294A))), CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Text("Apa yang ingin kamu buat?", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Ceritakan ide, tanyakan sesuatu, atau kirim foto untuk memulai.",
                        color = Muted, fontSize = 13.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SuggestionCard("Bantu buat prompt foto sinematik", onClick = { draft = "Bantu aku membuat prompt foto sinematik." })
                        SuggestionCard("Beri ide video dari sebuah foto", onClick = { draft = "Beri aku ide video menarik dari sebuah foto." })
                        SuggestionCard("Buat konsep gambar realistis", onClick = { draft = "Bantu buat konsep gambar realistis." })
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                messages.forEach { message ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(modifier = Modifier.background(Color(0xFF30345A), RoundedCornerShape(16.dp)).padding(12.dp)) {
                            Text(message, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Pesanmu tersimpan di tampilan percakapan ini. Mesin AI belum dihubungkan, jadi JR AI belum bisa membalas atau membuat hasil. Tahap berikutnya adalah menyambungkan layanan AI.",
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("Tanyakan sesuatu kepada JR AI...", color = Muted) },
                    shape = RoundedCornerShape(16.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Lampiran foto (segera hadir)", tint = Muted)
                    Text(" Foto dan suara segera hadir", color = Muted, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (draft.isNotBlank()) {
                                messages.add(draft.trim())
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Kirim", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(13.dp)) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.size(5.dp))
        Text(label, fontSize = 12.sp)
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
