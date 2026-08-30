package com.zedge.automationhub

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST = 1001
    private val FILE_UPLOAD_REQUEST = 1002
    private var isRefreshing = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.dark_header, theme)
        window.navigationBarColor = resources.getColor(R.color.dark_header, theme)

        webView = findViewById(R.id.webView)
        bottomNav = findViewById(R.id.bottomNav)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        setupBottomNavigation()
        setupSwipeRefresh()

        webView.loadUrl("file:///android_asset/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            databaseEnabled = true
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                updateBottomNavState(url)
                injectAndroidBridge()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (fileChooserCallback != null) {
                    fileChooserCallback?.onReceiveValue(null)
                }
                fileChooserCallback = filePathCallback
                openFileChooser(fileChooserParams)
                return true
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
            }

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean,
                isUserGesture: Boolean, resultMsg: Message?
            ): Boolean {
                return false
            }
        }

        webView.setOnTouchListener { v, event ->
            v.performClick()
            false
        }
    }

    private fun openFileChooser(params: WebChromeClient.FileChooserParams?) {
        val intent = params?.createIntent() ?: return

        try {
            startActivityForResult(intent, FILE_UPLOAD_REQUEST)
        } catch (e: Exception) {
            fileChooserCallback = null
            Toast.makeText(this, "Cannot open file picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            val tabHash = when (item.itemId) {
                R.id.nav_home -> "#home"
                R.id.nav_upload -> "#upload"
                R.id.nav_24h -> "#24h"
                R.id.nav_schedule -> "#schedule"
                R.id.nav_distribute -> "#distribute"
                else -> "#home"
            }

            webView.evaluateJavascript(
                "window.location.hash = '${tabHash.replace("#", "")}';",
                null
            )
            true
        }
    }

    private fun updateBottomNavState(url: String?) {
        if (url == null) return
        val hash = if (url.contains("#")) url.substringAfter("#") else "home"
        val navId = when (hash) {
            "home" -> R.id.nav_home
            "upload" -> R.id.nav_upload
            "24h" -> R.id.nav_24h
            "schedule" -> R.id.nav_schedule
            "distribute" -> R.id.nav_distribute
            else -> R.id.nav_home
        }
        bottomNav.selectedItemId = navId
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
            resources.getColor(R.color.primary_pink, theme),
            resources.getColor(R.color.primary_blue, theme)
        )
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            webView.scrollY > 0
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun injectAndroidBridge() {
        webView.evaluateJavascript("""
            (function() {
                if (!window.AndroidBridge) {
                    window.AndroidBridge = {
                        showToast: function(msg) { },
                        isAndroid: true,
                        getAppVersion: function() { return '1.0.0'; }
                    };
                }
            })();
        """, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_UPLOAD_REQUEST) {
            val result = if (resultCode == Activity.RESULT_OK && data != null) {
                val clipData = data.clipData
                if (clipData != null) {
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                    uris.toTypedArray()
                } else {
                    data.data?.let { arrayOf(it) }
                }
            } else {
                null
            }

            fileChooserCallback?.onReceiveValue(result)
            fileChooserCallback = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webView.evaluateJavascript("window.location.hash") { hash ->
                val currentHash = hash?.replace("\"", "") ?: ""
                if (currentHash != "#home" && currentHash.isNotEmpty()) {
                    runOnUiThread {
                        bottomNav.selectedItemId = R.id.nav_home
                        webView.evaluateJavascript("window.location.hash = 'home';", null)
                    }
                } else {
                    runOnUiThread {
                        webView.evaluateJavascript("""
                            if (document.getElementById('premiumZoomModal') && document.getElementById('premiumZoomModal').classList.contains('open')) {
                                document.getElementById('premiumZoomModal').classList.remove('open');
                                'zoom_closed';
                            } else if (document.getElementById('unifiedItemModal') && document.getElementById('unifiedItemModal').classList.contains('open')) {
                                document.getElementById('unifiedItemModal').classList.remove('open');
                                'modal_closed';
                            } else if (document.getElementById('settingsModal') && document.getElementById('settingsModal').classList.contains('open')) {
                                document.getElementById('settingsModal').classList.remove('open');
                                'settings_closed';
                            } else {
                                'exit';
                            }
                        """) { result ->
                            val res = result?.replace("\"", "") ?: ""
                            if (res.contains("exit")) {
                                finishAffinity()
                            }
                        }
                    }
                }
            }
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
