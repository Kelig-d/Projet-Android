# Projet Android : Sing With Me

Bienvenue dans l'application **Sing With Me**, une application de karaoké Android développée en Kotlin. Ce projet a été réalisé pour permettre :

- D'afficher, chercher, selectionner un titre dans une liste de chanson
- Jouer une chanson en affichant les paroles de manière synchronisée avec le son

---

## Fonctionnalités

### 1. Gestion de la playlist
- Récupération dynamique de la playlist depuis l'URL `https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/playlist.json`
- Affichage des chansons disponibles avec leurs artistes.
- Indication des chansons verrouillées.


### 2. Player Karaoké
- Lecture des fichiers audio avec **ExoPlayer**.
- Affichage des paroles synchronisées avec l’audio.
- Gestion des paroles :
  - Mise en évidence dynamique des mots en cours de lecture.
  - Transition entre les lignes.
- Contrôles de lecture :
  - Lecture/Pause.
  - Avance/Recul de 5 secondes.


---

## Technologies Utilisées

- **Langage** : Kotlin.
- **Player audio** : ExoPlayer.
- **Parsing JSON** : Moshi.

---

## Architecture

### Structure du projet

- `MainActivity.kt` : Point d’entrée de l’application.
- `LyricParser.kt` : Parsing des fichiers karaoké et gestion des paroles.
- `Song.kt` : Modèle pour représenter une chanson (nom, artiste, chemin, état verrouillé).


---

## Installation

1. Clonez le repository depuis GitHub :
   ```bash
   git clone https://github.com/Kelig-d/Projet-Android
   ```

2. Importez le projet dans Android Studio.

3. Assurez-vous d’avoir les dépendances suivantes dans le fichier `build.gradle` :
   - ExoPlayer
   - Moshi
   - Jetpack Compose

4. Compilez et exécutez l'application sur un appareil Android ou un émulateur.

---

## Livrables

- **Code source** : Disponible dans le repository GitHub.
- **APK** : Téléchargeable via ce lien : [Lien vers l'APK](https://drive.google.com/file/d/17spWAAMKDW655AWcRu74DYuXw43y_Cav/view?usp=sharing).

---

## Auteur

Développé par Keith Moser et Kelig Villalard.

