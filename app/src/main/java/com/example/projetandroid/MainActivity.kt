package com.example.projetandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projetandroid.ui.theme.ProjetAndroidTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

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
        val playlistJsonFile = URL("https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/playlist.json").readText()

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
    var showPlaylist by remember { mutableStateOf(false) }
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
            }
        }
    }
}
