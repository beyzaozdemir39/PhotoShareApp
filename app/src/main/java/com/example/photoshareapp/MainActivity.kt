package com.example.photoshareapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photoshareapp.ui.theme.PhotoShareAppTheme

const val SELECT_IMAGE_REQUEST = "image/*"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoShareAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoShareApp()
                }
            }
        }
    }
}
@Composable
fun PhotoShareApp() {
    val capturedLocation: Location? = null
    var editedText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var yazi by remember { mutableStateOf("") }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Seçilen fotoğrafın URI'sini sakla
                selectedImageUri = uri
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fotoğraf seçme düğmesi
        Button(
            onClick = {
                // Fotoğraf seçme işlemleri
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                launcher.launch(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Fotoğraf Seç")
        }

        // Fotoğrafın önizlemesi
        selectedImageUri?.let { uri ->
            val bitmap = getBitmapFromUri(uri)
            bitmap?.let {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Resmi çiz
                    drawImage(
                        image = it,
                        topLeft = Offset(0f, 0f),
                        alpha = 1f
                    )

                    // Metin Yaz Üstüne
                    val paint = Paint().apply {
                        color = Color.WHITE
                        textSize = 20.sp.toPx()
                    }
                    // İstenilen yazıyı ve konumu yaz
                    val combinedText = buildString {
                        append(yazi)
                        capturedLocation?.let { location ->
                            append("\nLat: ${location.latitude}, Lon: ${location.longitude}")
                        }
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        combinedText,
                        20f,
                        50f,
                        paint
                    )
                }
            }
        }
        // Metin giriş alanı
        OutlinedTextField(
            value = editedText,
            onValueChange = { editedText = it },
            label = { Text("Yazıyı Girin") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    // Klavye "Done" düğmesine basılınca klavyeyi kapat
                    // Paylaşım işlemleri
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = SELECT_IMAGE_REQUEST
                        // Seçilen fotoğrafı ekleyin
                        putExtra(Intent.EXTRA_STREAM, selectedImageUri)
                        // İstenilen yazıyı ekleyin
                        putExtra(Intent.EXTRA_TEXT, yazi)
                    }
                    context.startActivity(shareIntent)
                }
            )
        )

        // Kaydet ve Paylaş düğmeleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    // Kaydetme işlemleri
                    yazi = editedText
                }
            ) {
                Text("Kaydet")
            }

            Button(
                onClick = {
                    // Paylaşım işlemleri
                    shareImageWithTextAndLocation(selectedImageUri, yazi, capturedLocation, context)
                }
            ) {
                Text("Paylaş")
            }
        }
    }
}
// Paylaşım işlemini gerçekleştiren fonksiyon
private fun shareImageWithTextAndLocation(
    imageUri: Uri?,
    text: String,
    location: Location?,
    context: Context
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = SELECT_IMAGE_REQUEST

        // Seçilen fotoğrafı ekleyin
        putExtra(Intent.EXTRA_STREAM, imageUri)

        // İstenilen yazıyı ekleyin
        val combinedText = buildString {
            append(text)
            location?.let { loc ->
                append("\nLat: ${loc.latitude}, Lon: ${loc.longitude}")
            }
        }
        putExtra(Intent.EXTRA_TEXT, combinedText)
    }
    context.startActivity(shareIntent)
}
@Composable
fun getBitmapFromUri(uri: Uri): ImageBitmap? {
    return runCatching {
        val parcelFileDescriptor =
            LocalContext.current.contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
        val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor?.close()
        bitmap?.asImageBitmap()
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PhotoShareAppTheme {
        PhotoShareApp()
    }
}


