package com.example.projetandroid

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.projetandroid.ui.theme.ProjetAndroidTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    parsedLyrics?.let {
        MusicPlayer(
            songPath = it.information["songPath"] ?: "",
            parsedLyrics = parsedLyrics
        )
    }
}
@Composable
fun DisplayPlaylist( onParsedLyricsAvailable: (LyricParser) -> Unit) {
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    // var showPlaylist by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var expanded by remember { mutableStateOf(false) }  // Pour ouvrir/fermer le menu déroulant

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
                val lyricsUrl = URL_MASTER+song.path
                var parsedLyrics: LyricParser? = null
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val temp = URL(lyricsUrl)
                            val lyricsText = temp.readText()
                            parsedLyrics = LyricParser(lyricsText)

                            // Retour au thread principal pour appeler la fonction après que parsedLyrics soit mis à jour
                            withContext(Dispatchers.Main) {
                                onParsedLyricsAvailable(parsedLyrics!!)
                            }
                        } catch (e: Exception) {
                            // Gérer les exceptions liées à la lecture du fichier
                            println("oskour2")
                        }
                    }
                } catch (e: Exception) {
                    // Gérer les exceptions liées au lancement de la coroutine
                    println("oskour")
                }

                // Gestion des cas pour le chemin de la musique
                val songPath = if (!song.path.isNullOrBlank()) {
                    song.path.replace(".md", ".mp3")
                } else {
                    null // Ou une valeur par défaut si nécessaire
                }

                // Vérifie que songPath n'est pas vide
                songPath?.let {
                    //MusicPlayer(it, parsedLyrics)
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
    parsedLyrics: LyricParser?,
    modifier: Modifier = Modifier
) {
    if(songPath != "") {
        val context = LocalContext.current
        val songUrl = URL_MASTER + songPath

        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(songUrl)))
                prepare()
                playWhenReady = true
            }
        }

        val lyricsIndex = 0;
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (parsedLyrics != null) {
                    println(exoPlayer.currentPosition)
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(runnable)
        DisposableEffect(Unit) {
            onDispose {
                handler.removeCallbacks(runnable)
                exoPlayer.release()
            }
        }
    }
    else{
        println("error")
    }
}