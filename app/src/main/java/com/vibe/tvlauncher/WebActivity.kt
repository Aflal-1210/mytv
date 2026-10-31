package com.vibe.tvlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

class WebActivity : Activity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_BROWSER = "browser"
        const val EXTRA_TV_UA = "tv_ua"

        private const val TV_UA =
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.0) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "76.0.3809.146/6.0 TV Safari/537.36"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
    }

    private lateinit var web: WebView
    private lateinit var container: FrameLayout
    private lateinit var page: LinearLayout
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startUrl = intent.getStringExtra(EXTRA_URL) ?: "https://www.google.com"
        val isBrowser = intent.getBooleanExtra(EXTRA_BROWSER, false)
        val tvUa = intent.getBooleanExtra(EXTRA_TV_UA, false)

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = if (tvUa) TV_UA else DESKTOP_UA
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            setBackgroundColor(Color.BLACK)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) = false
                override fun onPageFinished(v: WebView, url: String) {
                    urlField?.setText(url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                    customView = view
                    customCallback = cb
                    page.visibility = View.GONE
                    container.addView(
                        view,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }

                override fun onHideCustomView() {
                    customView?.let { container.removeView(it) }
                    customView = null
                    customCallback?.onCustomViewHidden()
                    customCallback = null
                    page.visibility = View.VISIBLE
                }
            }
        }

        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        if (isBrowser) page.addView(buildBar())
        page.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        container = FrameLayout(this)
        container.addView(page)
        setContentView(container)

        web.loadUrl(startUrl)
        web.requestFocus()
    }

    private var urlField: EditText? = null

    private fun buildBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(0xFF1A1A22.toInt())
        }
        val back = Button(this).apply {
            text = "Back"
            setOnClickListener { if (web.canGoBack()) web.goBack() else finish() }
        }
        val field = EditText(this).apply {
            setSingleLine()
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Search or type a web address"
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { v, _, _ ->
                web.loadUrl(normalize(v.text.toString()))
                web.requestFocus()
                true
            }
        }
        urlField = field
        val go = Button(this).apply {
            text = "Go"
            setOnClickListener { web.loadUrl(normalize(field.text.toString())); web.requestFocus() }
        }
        val exit = Button(this).apply {
            text = "Home"
            setOnClickListener { finish() }
        }
        bar.addView(back)
        bar.addView(field, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(go)
        bar.addView(exit)
        return bar
    }

    private fun normalize(input: String): String {
        val s = input.trim()
        return when {
            s.startsWith("http://") || s.startsWith("https://") -> s
            s.contains(" ") || !s.contains(".") ->
                "https://www.google.com/search?q=" + java.net.URLEncoder.encode(s, "UTF-8")
            else -> "https://$s"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> customCallback?.onCustomViewHidden().also {
                customView?.let { v -> container.removeView(v) }
                customView = null
                page.visibility = View.VISIBLE
            }
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
        // The web screens run in their own process; end it so all the memory goes back to the box.
        if (isFinishing) android.os.Process.killProcess(android.os.Process.myPid())
    }
}
