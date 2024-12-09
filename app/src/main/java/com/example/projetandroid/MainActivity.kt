package com.example.projetandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projetandroid.ui.theme.ProjetAndroidTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView

var URL_MASTER = "https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjetAndroidTheme {
                DisplayPlaylist()
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
fun DisplayPlaylist() {
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
                try{
                    CoroutineScope(Dispatchers.IO).launch {
                        val temp = URL(lyricsUrl)
                        val lyricsText = temp.readText()
                        val parsedLyrics = LyricParser(lyricsText)
                        println(parsedLyrics)
                    }
                }
                catch (e: Exception){
                    println(e)
                }

                // Gestion des cas pour le chemin de la musique
                val songPath = if (!song.path.isNullOrBlank()) {
                    song.path.replace(".md", ".mp3")
                } else {
                    null // Ou une valeur par défaut si nécessaire
                }

                // Vérifie que songPath n'est pas vide
                songPath?.let {
                    MusicPlayer(it)
                } ?: run {
                    println("Le chemin de la chanson est invalide.")
                }

            }
        }
    }
}

@Composable
fun MusicPlayer(songPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val songurl = URL_MASTER+songPath
    // Crée une instance d'ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(songurl)))
            prepare()
        }
    }

    // Libère ExoPlayer lorsque le composable est détruit
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Afficher l'interface utilisateur de PlayerView
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { PlayerView(context).apply { player = exoPlayer } }
    )
}
