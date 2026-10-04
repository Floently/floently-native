package com.floently.read

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ReadOriginalPdfView(
    file: File,
    modifier: Modifier = Modifier
) {
    var renderer by remember(file.absolutePath) {
        mutableStateOf<PdfRenderer?>(null)
    }
    var descriptor by remember(file.absolutePath) {
        mutableStateOf<ParcelFileDescriptor?>(null)
    }
    var pageIndex by remember(file.absolutePath) {
        mutableIntStateOf(0)
    }
    var bitmap by remember(file.absolutePath) {
        mutableStateOf<Bitmap?>(null)
    }
    var errorMessage by remember(file.absolutePath) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(file.absolutePath) {
        val openedDescriptor = runCatching {
            ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        }.getOrNull()

        val openedRenderer = openedDescriptor?.let {
            runCatching {
                PdfRenderer(it)
            }.getOrNull()
        }

        descriptor = openedDescriptor
        renderer = openedRenderer
        errorMessage =
            if (openedRenderer == null) {
                "The original PDF could not be opened."
            } else {
                null
            }

        onDispose {
            renderer = null
            descriptor = null
            runCatching {
                openedRenderer?.close()
            }
            runCatching {
                openedDescriptor?.close()
            }
        }
    }

    val pageCount = renderer?.pageCount ?: 0
    val boundedPage = pageIndex.coerceIn(
        0,
        (pageCount - 1).coerceAtLeast(0)
    )

    LaunchedEffect(
        renderer,
        boundedPage
    ) {
        val currentRenderer = renderer
        if (
            currentRenderer == null
            || pageCount <= 0
        ) {
            bitmap = null
            return@LaunchedEffect
        }

        bitmap = null
        errorMessage = null

        runCatching {
            withContext(Dispatchers.IO) {
                currentRenderer.openPage(
                    boundedPage
                ).use { page ->
                    val targetWidth = 1600
                    val scale =
                        targetWidth.toFloat()
                            / page.width
                                .coerceAtLeast(1)
                                .toFloat()
                    val targetHeight = (
                        page.height
                            .coerceAtLeast(1)
                            .toFloat() * scale
                        )
                        .roundToInt()
                        .coerceAtLeast(1)

                    val target = Bitmap.createBitmap(
                        targetWidth,
                        targetHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val matrix = Matrix().apply {
                        postScale(
                            scale,
                            scale
                        )
                    }

                    page.render(
                        target,
                        null,
                        matrix,
                        PdfRenderer.Page
                            .RENDER_MODE_FOR_DISPLAY
                    )
                    target
                }
            }
        }
            .onSuccess {
                bitmap = it
            }
            .onFailure {
                errorMessage =
                    it.message
                        ?: "The PDF page could not be rendered."
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface
            )
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalAlignment =
                Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
        ) {
            Button(
                enabled = boundedPage > 0,
                onClick = {
                    pageIndex = (
                        boundedPage - 1
                        ).coerceAtLeast(0)
                }
            ) {
                Text("Previous")
            }

            Text(
                if (pageCount > 0) {
                    "Page " +
                        (boundedPage + 1) +
                        " of " +
                        pageCount
                } else {
                    "Opening PDF"
                },
                style =
                    MaterialTheme.typography
                        .labelLarge,
                modifier = Modifier.weight(1f)
            )

            Button(
                enabled =
                    boundedPage + 1 < pageCount,
                onClick = {
                    pageIndex = (
                        boundedPage + 1
                        ).coerceAtMost(
                            (pageCount - 1)
                                .coerceAtLeast(0)
                        )
                }
            ) {
                Text("Next")
            }
        }

        when {
            errorMessage != null -> {
                Box(
                    contentAlignment =
                        Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        errorMessage
                            ?: "Could not render PDF.",
                        color =
                            MaterialTheme.colorScheme
                                .error
                    )
                }
            }

            bitmap == null -> {
                Box(
                    contentAlignment =
                        Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Image(
                    bitmap = requireNotNull(bitmap)
                        .asImageBitmap(),
                    contentDescription =
                        "Original PDF page " +
                            (boundedPage + 1),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(
                            rememberScrollState()
                        )
                )
            }
        }
    }
}
