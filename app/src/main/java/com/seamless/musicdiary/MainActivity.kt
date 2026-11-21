

    
    
            
                                                        ) {
                                                            Alignment.CenterVertically
                        package com.seamless.musicdiary

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONArray
import org.json.JSONObject

private data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri
)

private enum class Page {
    MUSIC, DIARY, SETTINGS
}

class MainActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = ExoPlayer.Builder(this).build()

        setContent {
            SeamlessApp(
                loadTracks = { readTracks() },

                play = { track ->
                    player?.setMediaItem(
                        MediaItem.fromUri(track.uri)
                    )
                    player?.prepare()
                    player?.play()
                },

                stop = {
                    player?.pause()
                },

                loadEntries = {
                    getSharedPreferences(
                        "seamless",
                        MODE_PRIVATE
                    ).getString("entries", "[]") ?: "[]"
                },

                saveEntries = { value ->
                    getSharedPreferences(
                        "seamless",
                        MODE_PRIVATE
                    ).edit()
                        .putString("entries", value)
                        .apply()
                }
            )
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun readTracks(): List<Track> {
        val list = mutableListOf<Track>()

        val collection =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->

            val idCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media._ID
            )

            val titleCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.TITLE
            )

            val artistCol = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST
            )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)

                list += Track(
                    id = id,
                    title = cursor.getString(titleCol)
                        ?: "Unknown title",
                    artist = cursor.getString(artistCol)
                        ?: "Unknown artist",
                    uri = ContentUris.withAppendedId(
                        collection,
                        id
                    )
                )
            }
        }

        return list
    }
}

@Composable
private fun SeamlessApp(
    loadTracks: () -> List<Track>,
    play: (Track) -> Unit,
    stop: () -> Unit,
    loadEntries: () -> String,
    saveEntries: (String) -> Unit
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    var page by remember {
        mutableStateOf(Page.MUSIC)
    }

    var tracks by remember {
        mutableStateOf(emptyList<Track>())
    }

    var selected by remember {
        mutableStateOf<Track?>(null)
    }

    var permission by remember {
        mutableStateOf(false)
    }

    var entries by remember {
        mutableStateOf(loadEntries())
    }

    var draft by remember {
        mutableStateOf("")
    }

    var title by remember {
        mutableStateOf("")
    }

    var accent by remember {
        mutableStateOf(false)
    }

    val permissionName =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val request =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permission = granted

            if (granted) {
                tracks = loadTracks()
            }
        }

    LaunchedEffect(Unit) {
        permission =
            ContextCompat.checkSelfPermission(
                context,
                permissionName
            ) == PackageManager.PERMISSION_GRANTED

        if (permission) {
            tracks = loadTracks()
        }
    }

    val bg =
        if (accent) Color(0xFFF4EEE6)
        else Color(0xFF111214)

    val fg =
        if (accent) Color(0xFF24211D)
        else Color(0xFFF4F1EC)

    val muted =
        if (accent) Color(0xFF77716A)
        else Color(0xFFAAA9A6)

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = bg,
            surface = bg,
            onBackground = fg,
            onSurface = fg,
            primary = Color(0xFFB9A58D)
        )
    ) {
        Scaffold(
            containerColor = bg,

            bottomBar = {
                NavigationBar(
                    containerColor =
                        if (accent) Color(0xFFE9E0D5)
                        else Color(0xFF191A1D)
                ) {
                    NavigationBarItem(
                        selected = page == Page.MUSIC,
                        onClick = { page = Page.MUSIC },
                        icon = {
                            Icon(Icons.Default.MusicNote, null)
                        },
                        label = { Text("Music") }
                    )

                    NavigationBarItem(
                        selected = page == Page.DIARY,
                        onClick = { page = Page.DIARY },
                        icon = {
                            Icon(Icons.Default.Book, null)
                        },
                        label = { Text("Diary") }
                    )

                    NavigationBarItem(
                        selected = page == Page.SETTINGS,
                        onClick = { page = Page.SETTINGS },
                        icon = {
                            Icon(Icons.Default.Tune, null)
                        },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { padding ->

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(18.dp))

                Text(
                    "Seamless",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = fg
                )

                Spacer(Modifier.height(16.dp))

                when (page) {

                    Page.MUSIC -> {
                        if (!permission) {
                            Text(
                                "Allow access to audio to show music stored on this phone.",
                                color = muted
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    request.launch(permissionName)
                                }
                            ) {
                                Text("Allow music access")
                            }

                        } else if (tracks.isEmpty()) {
                            Text(
                                "No music found yet. Add audio files to your phone, then refresh.",
                                color = muted
                            )

                            TextButton(
                                onClick = {
                                    tracks = loadTracks()
                                }
                            ) {
                                Text("Refresh library")
                            }

                        } else {
                            Text(
                                "${tracks.size} songs",
                                color = muted,
                                style = MaterialTheme.typography.labelLarge
                            )

                            LazyColumn(
                                contentPadding = PaddingValues(
                                    top = 8.dp,
                                    bottom = 16.dp
                                ),
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    tracks,
                                    key = { it.id },
                                    contentType = { "track" }
                                ) { track ->

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (selected?.id == track.id)
                                                    Color(0xFF302D29)
                                                else
                                                    Color(0xFF1B1C1F),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                selected = track
                                                play(track)
                                            }
                                            .padding(14.dp),

                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier
                                                .size(42.dp)
                                                .background(
                                                    Color(0xFF39352F),
                                                    CircleShape
                                                ),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                null,
                                                tint = Color(0xFFD6C3A9)
                                            )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(
                                            Modifier.weight(1f)
                                        ) {
                                            Text(
                                                track.title,
                                                color = fg,
                                                maxLines = 1,
                                                fontWeight =
                                                    FontWeight.Medium
                                            )

                                            Text(
                                                track.artist,
                                                color = muted,
                                                maxLines = 1,
                                                style =
                                                    MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        if (selected?.id == track.id) {
                                            Icon(
                                                Icons.Default.GraphicEq,
                                                null,
                                                tint = Color(0xFFD6C3A9)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Page.DIARY -> {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Entry title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = { Text("Write your memory…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            minLines = 5
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (
                                    draft.isNotBlank() ||
                                    title.isNotBlank()
                                ) {
                                    val arr =
                                        try {
                                            JSONArray(entries)
                                        } catch (_: Exception) {
                                            JSONArray()
                                        }

                                    arr.put(
                                        JSONObject()
                                            .put(
                                                "title",
                                                title.ifBlank {
                                                    "A memory"
                                                }
                                            )
                                            .put("body", draft)
                                            .put(
                                                "song",
                                                selected?.title ?: ""
                                            )
                                    )

                                    entries = arr.toString()
                                    saveEntries(entries)

                                    title = ""
                                    draft = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save memory")
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Saved memories",
                            color = muted,
                            style = MaterialTheme.typography.titleSmall
                        )

                        val saved = remember(entries) {
                            try {
                                val a = JSONArray(entries)

                                (0 until a.length()).map {
                                    a.getJSONObject(it)
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }

                        LazyColumn(
                            Modifier.weight(1f),
                            contentPadding =
                                PaddingValues(vertical = 8.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                saved.size,
                                key = { it }
                            ) { i ->

                                val item = saved[i]

                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0xFF1B1C1F),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        item.optString("title"),
                                        color = fg,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        item.optString("body"),
                                        color = muted,
                                        maxLines = 4
                                    )

                                    if (
                                        item.optString("song").isNotBlank()
                                    ) {
                                        Text(
                                            "♪ ${item.optString("song")}",
                                            color = Color(0xFFD6C3A9),
                                            style =
                                                MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Page.SETTINGS -> {
                        Text(
                            "Make it yours",
                            color = muted
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                "Warm light theme",
                                color = fg,
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = accent,
                                onCheckedChange = { accent = it }
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Developed by Shaikh Mzanak",
                            color = muted,
                            style =
                                MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 18.dp)
                        )
                    }
                }
            }
        }
    }
}
