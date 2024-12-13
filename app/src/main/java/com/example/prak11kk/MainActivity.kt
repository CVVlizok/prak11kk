package com.example.prak11kk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prak11kk.ui.theme.Prak11kkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Prak11kkTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("ImageDownloader") }

    val screens = listOf("ImageDownloader", "Info", "Settings")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                screens.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                    )
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = currentScreen) },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = null)
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        screens.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                label = { Text(screen) },
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen }
                            )
                        }
                    }
                },
                content = { contentPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(16.dp)
                    ) {
                        when (currentScreen) {
                            "ImageDownloader" -> ImageDownloadApp()
                            "Info" -> InfoScreen()
                            "Settings" -> SettingsScreen()
                        }
                    }
                }
            )
        },
    )
}

@Composable
fun ImageDownloadApp() {
    var url by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Enter Image URL") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (url.isNotEmpty()) {
                    coroutineScope.launch {
                        val downloadedBitmap = downloadImage(url, context)
                        bitmap = downloadedBitmap
                    }
                } else {
                    Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download Image")
        }
        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(400.dp)
            )
        }
    }
}

@Composable
fun InfoScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Information Screen", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings Screen", style = MaterialTheme.typography.titleLarge)
    }
}

suspend fun downloadImage(url: String, context: Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val bitmap = downloadImageFromNetwork(url)

            if (bitmap != null) {
                saveImageToDisk(bitmap, context)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image downloaded successfully", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun downloadImageFromNetwork(url: String): Bitmap? {
    return try {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val inputStream: InputStream = response.body?.byteStream() ?: return null
            BitmapFactory.decodeStream(inputStream)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveImageToDisk(bitmap: Bitmap, context: Context) {
    val directory =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "downloadedImages")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File(directory, "downloaded_image.png")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    outputStream.flush()
    outputStream.close()
}
