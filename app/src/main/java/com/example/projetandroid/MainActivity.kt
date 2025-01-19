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
import androidx.compose.material3.Slider
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
import androidx.media3.common.util.Log
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
                    parsedLyrics = null
                    try {
                        withContext(Dispatchers.IO) { // Opérations réseau sur un thread IO
                            println("DG-Keith : Récupération parole ${lyricsUrl}")
                            val lyricsText = URL(lyricsUrl).readText()
                            parsedLyrics = LyricParser(lyricsText)
                            println("DG-Keith : Paroles récupérés ${parsedLyrics}")
                        }
                        onParsedLyricsAvailable(parsedLyrics!!)
                    } catch (e: Exception) {
                        println("Erreur lors du chargement des paroles : ${e.message}")
                    }
                }

                // Gestion des cas pour le chemin de la musique
                val songPath = song.path?.replace(".md", ".mp3")
                songPath?.let {
                    println("DG-Keith : Chemin des paroles ${songPath}")
                    // Appeler MusicPlayer uniquement si parsedLyrics est disponible
                    if (parsedLyrics != null) {
                        println("DG-Keith : Parseur lyrics $parsedLyrics")
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
    var lyricsIndex = remember { mutableStateOf(0) } // Utilisation de remember pour garder l'état de l'index
    var currentLyrics by remember { mutableStateOf<List<Lyric>?>(null) } // Les paroles actuelles
    var lyricsProgress by remember { mutableStateOf(0f) } // Suivi de la progression des paroles

    // Utilisation de 'remember' pour maintenir l'instance ExoPlayer sur les changements de chanson
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    // State pour garder une trace de l'URL de la chanson actuelle
    var currentSongUrl by remember { mutableStateOf(songUrl) }

    // Une fois une chanson sélectionnée, on prépare et lance la musique
    LaunchedEffect(songUrl) {
        // Réinitialiser les paroles à chaque nouvelle chanson
        currentLyrics = null // Réinitialisation des paroles
        lyricsProgress = 0f // Réinitialiser la progression des paroles

        // Si l'URL de la chanson a changé, on reconfigure ExoPlayer mais on ne le libère pas
        if (currentSongUrl != songUrl) {
            exoPlayer.stop()  // Arrêter la musique avant de préparer une nouvelle chanson
            exoPlayer.release()
        }

        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(songUrl)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true // Lancer immédiatement la lecture

        // Mettre à jour l'URL actuelle pour éviter de reconfigurer le player inutilement
        currentSongUrl = songUrl
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0f) }
    var currentTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (parsedLyrics != null) {
                    val currentPosition = exoPlayer.currentPosition

                    // Vérifier si nous devons changer l'index des paroles
                    if (lyricsIndex.value < parsedLyrics.lyrics.size &&
                        currentPosition >= parsedLyrics.lyrics[lyricsIndex.value][0].startOffset &&
                        currentPosition <= parsedLyrics.lyrics[lyricsIndex.value].last().endOffset
                    ) {
                        currentLyrics = parsedLyrics.lyrics[lyricsIndex.value]
                    }

                    // Mettre à jour l'index des paroles lorsque la chanson passe à la suivante
                    if (currentPosition >= parsedLyrics.lyrics[lyricsIndex.value].last().endOffset) {
                        lyricsIndex.value += 1
                        currentLyrics = null
                    }

                    // Mettre à jour la progression
                    currentTime = exoPlayer.currentPosition
                    currentProgress = exoPlayer.currentPosition / exoPlayer.duration.toFloat()

                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(runnable)

        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    // Composant UI pour la lecture de la musique
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 5000).coerceAtLeast(0)) }) {
                Text("-5s")
            }
            Button(onClick = {
                isPlaying = if (isPlaying) {
                    exoPlayer.pause()
                    false
                } else {
                    exoPlayer.play()
                    true
                }
            }) {
                Text(if (isPlaying) "Pause" else "Play")
            }
            Button(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 5000).coerceAtMost(exoPlayer.duration)) }) {
                Text("+5s")
            }
        }

        Slider(
            value = currentProgress,
            onValueChange = { newValue ->
                currentProgress = newValue
                exoPlayer.seekTo((newValue * exoPlayer.duration).toLong())
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Text("Temps actuel : ${currentTime / 1000}s", style = TextStyle(fontSize = 16.sp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentLyrics?.let {
                currentLyrics!!.forEach { lyric ->
                    //println("DG-Keith : $lyric")
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
    println("Les paroles test $lyric")
    if (lyric != null) {
        var progress by remember { mutableFloatStateOf(0f) }

        val totalDuration = (lyric.endOffset - lyric.startOffset).toFloat()
        val lyricDuration = totalDuration / 100f  // Pour une mise à jour plus fluide

        LaunchedEffect(lyric.startOffset) {
            // Calculer la progression en fonction du temps de la chanson
            while (progress < 1f) {
                delay((lyricDuration).toLong())
                progress += 0.01f
            }
        }

        // Mise à jour de la fonction HighlightedTextWithMask pour afficher les paroles avec un effet de progression
        HighlightedTextWithMask(
            fullText = lyric.sentence,
            progress = progress
        )
    }
}
