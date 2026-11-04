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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private enum class AppPage { HOME, PHOTO, VIDEO, GALLERY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JrAiApp() }
    }
}

@Composable
private fun JrAiApp() {
    var currentPage by remember { mutableStateOf(AppPage.HOME) }
    BackHandler(enabled = currentPage != AppPage.HOME) { currentPage = AppPage.HOME }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Page) {
            when (currentPage) {
                AppPage.HOME -> HomeScreen(
                    onPhoto = { currentPage = AppPage.PHOTO },
                    onVideo = { currentPage = AppPage.VIDEO },
                    onGallery = { currentPage = AppPage.GALLERY }
                )
                AppPage.PHOTO -> PhotoScreen(onBack = { currentPage = AppPage.HOME })
                AppPage.VIDEO -> ToolScreen(
                    title = "Video AI",
                    description = "Tuliskan konsep video yang ingin kamu buat.",
                    hint = "Contoh: video sinematik suasana pegunungan...",
                    actionLabel = "Buat Video",
                    onBack = { currentPage = AppPage.HOME }
                )
                AppPage.GALLERY -> GalleryScreen(onBack = { currentPage = AppPage.HOME })
            }
        }
    }
}

@Composable
private fun HomeScreen(onPhoto: () -> Unit, onVideo: () -> Unit, onGallery: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("JR AI", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text("CREATIVE STUDIO", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Accent)
        }
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF29245B), Color(0xFF171D36), Color(0xFF111827))),
                RoundedCornerShape(24.dp)
            ).padding(22.dp)
        ) {
            Column {
                Text("Wujudkan ide\njadi karya.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Buat kreasi foto dan video dengan bantuan AI.", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                Text("AI CREATION • PHOTO • VIDEO", color = Color(0xFFC9C2FF), fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
        Text("Mulai berkarya", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        FeatureCard("Foto AI", "Ubah teks atau foto menjadi karya baru", { Icon(Icons.Default.Image, null, tint = Accent) }, onPhoto)
        FeatureCard("Video AI", "Rencanakan video dari prompt atau foto", { Icon(Icons.Default.Movie, null, tint = Accent) }, onVideo)
        FeatureCard("Galeri Saya", "Lihat hasil kreasi yang tersimpan", { Icon(Icons.Default.Collections, null, tint = Accent) }, onGallery)
        Text("Versi awal • Menu navigasi aktif", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.background(Color(0xFF252342), RoundedCornerShape(13.dp)).padding(12.dp),
                contentAlignment = Alignment.Center
            ) { icon() }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Accent, fontSize = 27.sp)
        }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White,
                modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
            Text("Foto AI", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text("BUAT FOTO DARI TEKS", color = Color(0xFFC9C2FF), fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = createPrompt,
            onValueChange = { createPrompt = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("Prompt gambar") },
            placeholder = { Text("Contoh: pemandangan desa saat matahari terbenam...") }
        )
        Text("EDIT FOTO DENGAN PROMPT", color = Color(0xFFC9C2FF), fontWeight = FontWeight.Bold)
        Text("Pilih foto dari HP, lalu tuliskan perubahan yang kamu inginkan.", color = Muted, fontSize = 13.sp)
        OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Image, contentDescription = null)
            Text(if (selectedPhoto == null) "Pilih Foto dari HP" else "Ganti Foto")
        }
        if (selectedPhoto != null) {
            Text("Foto sumber berhasil dipilih", color = Color(0xFF8FE3AE), fontSize = 13.sp)
        }
        OutlinedTextField(
            value = editPrompt,
            onValueChange = { editPrompt = it; message = "" },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("Prompt edit foto") },
            placeholder = { Text("Contoh: ubah latar menjadi pantai saat senja, pertahankan wajah dan pakaian asli...") }
        )
        Button(
            onClick = {
                message = when {
                    selectedPhoto == null -> "Pilih foto dari HP terlebih dahulu."
                    editPrompt.isBlank() -> "Tuliskan prompt edit foto terlebih dahulu."
                    else -> "Foto dan prompt siap. Mesin AI belum dihubungkan, sehingga hasil edit belum dapat dibuat."
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) { Text("Edit Foto dengan AI", color = Color.White) }
        if (message.isNotBlank()) Text(message, color = Color(0xFFFFD27A), fontSize = 13.sp)
        Text("Pemilihan foto dan prompt tersedia. Generator AI akan dihubungkan pada tahap berikutnya.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ToolScreen(title: String, description: String, hint: String, actionLabel: String, onBack: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White,
                modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text(description, color = Muted, fontSize = 14.sp)
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it; message = "" },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            label = { Text("Deskripsi / prompt") },
            placeholder = { Text(hint) }
        )
        Button(
            onClick = { message = if (prompt.isBlank()) "Isi deskripsi terlebih dahulu." else "Tampilan siap. Mesin AI belum dihubungkan, jadi hasil belum dapat dibuat." },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) { Text(actionLabel, color = Color.White) }
        if (message.isNotBlank()) Text(message, color = Color(0xFFFFD27A), fontSize = 13.sp)
        Text("Catatan: fitur pembuatan AI akan diaktifkan setelah layanan generator dihubungkan.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun GalleryScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White,
                modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
            Text("Galeri Saya", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Collections, contentDescription = null, tint = Accent)
                Spacer(Modifier.height(12.dp))
                Text("Belum ada hasil", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Hasil foto dan video akan tampil di sini setelah fitur AI dan penyimpanan diaktifkan.", color = Muted, fontSize = 13.sp)
            }
        }
    }
}
