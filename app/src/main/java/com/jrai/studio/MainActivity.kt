package com.jrai.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JrAiApp() }
    }
}

@Composable
private fun JrAiApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Page) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
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
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF29245B), Color(0xFF171D36), Color(0xFF111827))),
                            RoundedCornerShape(24.dp)
                        ).padding(22.dp)
                ) {
                    Column {
                        Text("Wujudkan ide
jadi karya.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Buat kreasi foto dan video dengan bantuan AI.", color = Muted, fontSize = 14.sp)
                        Spacer(Modifier.height(18.dp))
                        Text("AI CREATION • PHOTO • VIDEO", color = Color(0xFFC9C2FF), fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                }

                Text("Mulai berkarya", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                FeatureCard(
                    title = "Foto AI",
                    subtitle = "Ubah teks atau foto menjadi karya baru",
                    icon = { Icon(Icons.Default.Image, contentDescription = null, tint = Accent) }
                )
                FeatureCard(
                    title = "Video AI",
                    subtitle = "Rencanakan video dari prompt atau foto",
                    icon = { Icon(Icons.Default.Movie, contentDescription = null, tint = Accent) }
                )
                FeatureCard(
                    title = "Galeri Saya",
                    subtitle = "Tempat hasil kreasi akan tersimpan",
                    icon = { Icon(Icons.Default.Collections, contentDescription = null, tint = Accent) }
                )

                Spacer(Modifier.weight(1f))
                Text(
                    "Tahap awal • Antarmuka aplikasi",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
