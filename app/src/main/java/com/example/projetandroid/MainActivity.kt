package com.example.projetandroid

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.projetandroid.ui.theme.ProjetAndroidTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL

var URL_MASTER = "https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjetAndroidTheme {
                MusicApp()
            }
        }
    }
}

suspend fun ReadPlaylist(): List<Song> {
    return withContext(Dispatchers.IO) { // Effectuer les opérations réseau en arrière-plan
        val playlistJsonFile = URL(URL_MASTER+"playlist.json").readText()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory()) // Permet à Moshi de gérer les classes Kotlin
            .build()

        val listType = Types.newParameterizedType(List::class.java, Song::class.java)
        val adapter = moshi.adapter<List<Song>>(listType)

        adapter.fromJson(playlistJsonFile) ?: emptyList()
    }
}

@Composable
fun MusicApp() {
    // État partagé
    var parsedLyrics by remember { mutableStateOf<LyricParser?>(null) }
    // Composables enfants avec l'état partagé
    DisplayPlaylist(
        onParsedLyricsAvailable = { lyrics -> parsedLyrics = lyrics }
    )
}

@Composable
fun DisplayPlaylist( onParsedLyricsAvailable: (LyricParser) -> Unit) {
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    // var showPlaylist by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var expanded by remember { mutableStateOf(false) }  // Pour ouvrir/fermer le menu déroulant
    var parsedLyrics by remember { mutableStateOf<LyricParser?>(null) } // State pour parsedLyrics

    // Charger la playlist au démarrage
    LaunchedEffect(Unit) {
        songs = ReadPlaylist()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bouton pour afficher le menu déroulant avec les chansons
            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Choix de la musique")
            }

            // Menu déroulant affichant la liste des musiques
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                songs.forEach { song ->
                    DropdownMenuItem(
                        text = { Text(text = song.name) },
                        onClick = {
                            // Seule une chanson non verrouillée peut être sélectionnée
                            if (song.locked != true) {
                                selectedSong = song
                                expanded = false  // Fermer le menu après la sélection
                            }
                        },
                        enabled = song.locked != true // Désactive les chansons verrouillées
                    )
                }
            }

            // Affichage de la chanson sélectionnée
            selectedSong?.let { song ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Vous avez sélectionné : ${song.name} par ${song.artist}")

                // Charger les paroles si elles ne sont pas déjà disponibles
                LaunchedEffect(song.path) {
                    val lyricsUrl = URL_MASTER + song.path
                    try {
                        withContext(Dispatchers.IO) { // Opérations réseau sur un thread IO
                            val lyricsText = URL(lyricsUrl).readText()
                            parsedLyrics = LyricParser(lyricsText)
                        }
                        onParsedLyricsAvailable(parsedLyrics!!)
                    } catch (e: Exception) {
                        println("Erreur lors du chargement des paroles : ${e.message}")
                    }
                }

                // Gestion des cas pour le chemin de la musique
                val songPath = song.path?.replace(".md", ".mp3")
                songPath?.let {
                    // Appeler MusicPlayer uniquement si parsedLyrics est disponible
                    if (parsedLyrics != null) {
                        MusicPlayer(it, parsedLyrics)
                    } else {
                        Text("Chargement des paroles...")
                    }
                } ?: run {
                    println("Le chemin de la chanson est invalide.")
                }
            }
        }
    }
}

@Composable
fun MusicPlayer(
    songPath: String,
    parsedLyrics: LyricParser?
) {
    val context = LocalContext.current
    val songUrl = URL_MASTER + songPath
    var lyricsIndex = 0

    // State pour afficher le texte
    var currentLyrics by remember { mutableStateOf<List<Lyric?>?>(null) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(songUrl)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (parsedLyrics != null) {
                    val currentPosition = exoPlayer.currentPosition
                    if(currentPosition >= parsedLyrics.lyrics[lyricsIndex][0].startOffset && currentPosition <= parsedLyrics.lyrics[lyricsIndex].last().endOffset)
                        currentLyrics = parsedLyrics.lyrics[lyricsIndex]
                    if(currentPosition >= parsedLyrics.lyrics[lyricsIndex].last().endOffset && lyricsIndex <parsedLyrics.lyrics.size){
                        lyricsIndex+=1
                        currentLyrics = null
                    }

                }


                handler.postDelayed(this, 100)
            }
        }
        handler.post(runnable)

        onDispose {
            handler.removeCallbacks(runnable)
            exoPlayer.release()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        currentLyrics?.let {
            if (parsedLyrics != null) {
                currentLyrics!!.forEach { lyric ->
                    KaraokeText(lyric)
                }
            }
        }
    }


}

@Composable
fun HighlightedTextWithMask(fullText: String, progress: Float, textStyle: TextStyle = TextStyle(fontSize = 24.sp)) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = textStyle.fontSize.value * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // Mesurer la largeur totale du texte
            val textWidth = paint.measureText(fullText)

            // Calculer la largeur de la partie colorée
            val colorWidth = textWidth * progress

            // Créer le masque avec un dégradé dynamique
            val shader = LinearGradient(
                0f, 0f, textWidth, 0f,
                intArrayOf(Color.Red.toArgb(), Color.Black.toArgb()),
                floatArrayOf(colorWidth / textWidth, colorWidth / textWidth + 0.01f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader

            // Dessiner le texte avec le shader appliqué
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(fullText, 0f, size.height / 2 + paint.textSize / 2, paint)
            }
        }
    }
}

@Composable
fun KaraokeText(lyric: Lyric?) {
    if(lyric != null){
        var progress by remember { mutableFloatStateOf(0f) }

        val totalDuration = (lyric.endOffset - lyric.startOffset)
        LaunchedEffect(Unit) {
            while (progress < 1f) {
                progress += 0.01f
                delay((totalDuration/100).toLong())
            }
        }

        HighlightedTextWithMask(
            fullText = lyric.sentence,
            progress = progress
        )
    }

}


