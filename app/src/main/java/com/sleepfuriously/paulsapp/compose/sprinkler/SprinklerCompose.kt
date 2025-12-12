package com.sleepfuriously.paulsapp.compose.sprinkler

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.sleepfuriously.paulsapp.viewmodels.SprinklerViewmodel

/**
 * Holds compose functions for the Sprinkler portion of tha app.
 */


@Composable
fun ShowMainSprinkler(
    sprinklerViewmodel: SprinklerViewmodel,
    acceptJavaScript: Boolean,
    modifier: Modifier = Modifier
) {
    var backEnabled by rememberSaveable { mutableStateOf(false) }
    var webView: WebView? = null
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        backEnabled = view.canGoBack()
                    }
                }
                settings.javaScriptEnabled = acceptJavaScript
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.domStorageEnabled = true       // wow, this really did the trick!!

                loadUrl(sprinklerViewmodel.getUrl())
                webView = this
            }
        }, update = {
            webView = it
        })

    BackHandler(enabled = backEnabled) {
        webView?.goBack()
    }

}
