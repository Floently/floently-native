package com.floently.read

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floently.shared.design.FloentlyDesignTokens
import com.floently.shared.design.FloentlyProduct
import com.floently.shared.design.floentlyPalette
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection

@Composable
fun ReadOriginalEpubView(
    packageValue: ReadLocalEpubPackage,
    modifier: Modifier = Modifier
) {
    val palette =
        floentlyPalette(
            FloentlyProduct.Read
        )
    var chapterIndex by remember(
        packageValue.rootDirectory,
        packageValue.metadata.spine.size
    ) {
        mutableIntStateOf(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                FloentlyDesignTokens
                    .Colors
                    .canvas
            )
    ) {
        ReadEpubWebView(
            packageValue = packageValue,
            chapterIndex =
                chapterIndex,
            onChapterChange = {
                chapterIndex = it
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    FloentlyDesignTokens
                        .Space
                        .s2
                ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    FloentlyDesignTokens
                        .Colors
                        .surface1
                )
                .padding(
                    horizontal =
                        FloentlyDesignTokens
                            .Space
                            .s2
                )
        ) {
            EpubChapterButton(
                label = "Previous",
                enabled =
                    chapterIndex > 0
            ) {
                chapterIndex -= 1
            }

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    "Chapter "
                        + (chapterIndex + 1)
                        + " of "
                        + packageValue
                            .metadata
                            .spine
                            .size,
                    color = palette.text,
                    style =
                        androidx.compose
                            .material3
                            .MaterialTheme
                            .typography
                            .labelMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )

                packageValue
                    .metadata
                    .title
                    .trim()
                    .takeIf {
                        it.isNotEmpty()
                    }
                    ?.let { title ->
                        Text(
                            title,
                            color =
                                palette.muted,
                            style =
                                androidx.compose
                                    .material3
                                    .MaterialTheme
                                    .typography
                                    .labelSmall,
                            maxLines = 1
                        )
                    }
            }

            EpubChapterButton(
                label = "Next",
                enabled =
                    chapterIndex + 1
                        < packageValue
                            .metadata
                            .spine
                            .size
            ) {
                chapterIndex += 1
            }
        }
    }
}

@Composable
private fun ReadEpubWebView(
    packageValue: ReadLocalEpubPackage,
    chapterIndex: Int,
    onChapterChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val root =
        remember(
            packageValue.rootDirectory
        ) {
            packageValue
                .rootDirectory
                .canonicalFile
        }
    val spineFiles =
        remember(
            packageValue.rootDirectory,
            packageValue.metadata.spine
        ) {
            packageValue
                .metadata
                .spine
                .mapNotNull { item ->
                    runCatching {
                        File(
                            root,
                            item.path
                        ).canonicalFile
                    }.getOrNull()
                        ?.takeIf {
                            isInsideRoot(
                                root,
                                it
                            )
                                && it.isFile
                        }
                }
        }
    var webView by remember {
        androidx.compose.runtime
            .mutableStateOf<WebView?>(
                null
            )
    }

    fun loadChapter(
        view: WebView,
        index: Int
    ) {
        val file =
            spineFiles.getOrNull(
                index
            ) ?: return
        val path =
            file.relativeTo(root)
                .invariantSeparatorsPath
        val target =
            epubUrlForPath(path)
        if (view.url != target) {
            view.loadUrl(target)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    AndroidView(
        factory = {
            WebView(context).apply {
                webView = this
                setBackgroundColor(
                    android.graphics.Color
                        .TRANSPARENT
                )
                settings.javaScriptEnabled =
                    false
                settings.domStorageEnabled =
                    false
                settings.databaseEnabled =
                    false
                settings.allowFileAccess =
                    false
                settings.allowContentAccess =
                    false
                settings.mixedContentMode =
                    WebSettings
                        .MIXED_CONTENT_NEVER_ALLOW
                settings.builtInZoomControls =
                    true
                settings.displayZoomControls =
                    false

                webViewClient =
                    object :
                        WebViewClient()
                    {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request:
                                WebResourceRequest?
                        ): WebResourceResponse? {
                            val uri =
                                request?.url
                                    ?: return blockedResponse()

                            if (
                                uri.scheme != "https"
                                || uri.host
                                    != EPUB_LOCAL_HOST
                            ) {
                                return blockedResponse()
                            }

                            val relative =
                                uri.path
                                    ?.removePrefix(
                                        "/"
                                    )
                                    .orEmpty()
                            val file =
                                runCatching {
                                    File(
                                        root,
                                        relative
                                    ).canonicalFile
                                }.getOrNull()
                                    ?: return blockedResponse()

                            if (
                                !isInsideRoot(
                                    root,
                                    file
                                )
                                || !file.isFile
                            ) {
                                return blockedResponse()
                            }

                            return runCatching {
                                WebResourceResponse(
                                    mimeTypeFor(
                                        file
                                    ),
                                    null,
                                    file.inputStream()
                                        .buffered()
                                )
                            }.getOrElse {
                                blockedResponse()
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request:
                                WebResourceRequest?
                        ): Boolean {
                            val uri =
                                request?.url
                                    ?: return true

                            if (
                                uri.scheme == "https"
                                && uri.host
                                    == EPUB_LOCAL_HOST
                            ) {
                                if (
                                    request.isForMainFrame
                                ) {
                                    val relative =
                                        uri.path
                                            ?.removePrefix(
                                                "/"
                                            )
                                            .orEmpty()
                                    val target =
                                        runCatching {
                                            File(
                                                root,
                                                relative
                                            ).canonicalFile
                                        }.getOrNull()

                                    if (
                                        target == null
                                        || !isInsideRoot(
                                            root,
                                            target
                                        )
                                    ) {
                                        return true
                                    }

                                    val index =
                                        spineFiles
                                            .indexOfFirst {
                                                it.path
                                                    == target.path
                                            }
                                    if (
                                        index >= 0
                                        && index
                                            != chapterIndex
                                    ) {
                                        onChapterChange(
                                            index
                                        )
                                    }
                                }
                                return false
                            }

                            if (
                                request.isForMainFrame
                                && (
                                    uri.scheme
                                        == "http"
                                    || uri.scheme
                                        == "https"
                                )
                            ) {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            uri
                                        )
                                    )
                                } catch (
                                    _: ActivityNotFoundException
                                ) {
                                    // External navigation is optional;
                                    // never relax the EPUB sandbox.
                                }
                            }

                            return true
                        }
                    }

                loadChapter(
                    this,
                    chapterIndex
                )
            }
        },
        update = { view ->
            loadChapter(
                view,
                chapterIndex
            )
        },
        modifier = modifier
    )
}

@Composable
private fun EpubChapterButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults
            .buttonColors(
                containerColor =
                    Color.Transparent,
                contentColor =
                    FloentlyDesignTokens
                        .Colors
                        .textPrimary,
                disabledContainerColor =
                    Color.Transparent,
                disabledContentColor =
                    FloentlyDesignTokens
                        .Colors
                        .textDisabled
            ),
        shape = RoundedCornerShape(
            FloentlyDesignTokens
                .Radius
                .m
        ),
        modifier = Modifier.size(
            FloentlyDesignTokens
                .Control
                .iconTarget
        )
    ) {
        Text(
            if (label == "Previous") {
                "‹"
            } else {
                "›"
            },
            fontWeight =
                FontWeight.Bold
        )
    }
}

private const val EPUB_LOCAL_HOST =
    "read-epub.local"

private fun epubUrlForPath(
    relativePath: String
): String {
    val encoded =
        relativePath
            .split("/")
            .joinToString("/") {
                Uri.encode(it)
            }

    return "https://"
        + EPUB_LOCAL_HOST
        + "/"
        + encoded
}

private fun isInsideRoot(
    root: File,
    candidate: File
): Boolean {
    val rootPath =
        root.canonicalPath
    val candidatePath =
        candidate.canonicalPath

    return candidatePath == rootPath
        || candidatePath.startsWith(
            rootPath
                + File.separator
        )
}

private fun mimeTypeFor(
    file: File
): String {
    val lower =
        file.name.lowercase()

    return when {
        lower.endsWith(".xhtml")
            || lower.endsWith(".html")
            || lower.endsWith(".htm") ->
            "application/xhtml+xml"

        lower.endsWith(".css") ->
            "text/css"

        lower.endsWith(".svg") ->
            "image/svg+xml"

        lower.endsWith(".xml")
            || lower.endsWith(".opf")
            || lower.endsWith(".ncx") ->
            "application/xml"

        lower.endsWith(".woff") ->
            "font/woff"

        lower.endsWith(".woff2") ->
            "font/woff2"

        lower.endsWith(".ttf") ->
            "font/ttf"

        lower.endsWith(".otf") ->
            "font/otf"

        else ->
            URLConnection
                .guessContentTypeFromName(
                    file.name
                )
                ?: "application/octet-stream"
    }
}

private fun blockedResponse():
    WebResourceResponse =
    WebResourceResponse(
        "text/plain",
        "UTF-8",
        403,
        "Blocked",
        emptyMap(),
        ByteArrayInputStream(
            ByteArray(0)
        )
    )
