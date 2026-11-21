package com.thiagoyagame.kpgame

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    // IDs REAIS DE PRODUÇÃO DO ADMOB (KPGAME)
    private val INTERSTITIAL_ID = "ca-app-pub-4105543667352464/2323187567"
    private val REWARDED_ID = "ca-app-pub-4105543667352464/7992823358"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(R.layout.activity_main)
        enableImmersiveMode()

        MobileAds.initialize(this) {}
        loadInterstitial()
        loadRewarded()

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.addJavascriptInterface(AdJsBridge(), "AndroidAds")
        webView.loadUrl("file:///android_asset/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("if(typeof togglePause==='function' && started){togglePause();}", null)
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            this,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(err: LoadAdError) { interstitialAd = null }
            }
        )
    }

    private fun loadRewarded() {
        RewardedAd.load(
            this,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(err: LoadAdError) { rewardedAd = null }
            }
        )
    }

    inner class AdJsBridge {
        @JavascriptInterface
        fun showInterstitial() {
            runOnUiThread {
                val ad = interstitialAd
                if (ad != null) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial()
                            enableImmersiveMode()
                            webView.evaluateJavascript("window._onInterstitialClosed && window._onInterstitialClosed();", null)
                        }
                        override fun onAdFailedToShowFullScreenContent(err: AdError) {
                            interstitialAd = null
                            loadInterstitial()
                            enableImmersiveMode()
                            webView.evaluateJavascript("window._onInterstitialClosed && window._onInterstitialClosed();", null)
                        }
                    }
                    ad.show(this@MainActivity)
                } else {
                    loadInterstitial()
                    webView.evaluateJavascript("window._onInterstitialClosed && window._onInterstitialClosed();", null)
                }
            }
        }

        @JavascriptInterface
        fun showRewarded() {
            runOnUiThread {
                val ad = rewardedAd
                if (ad != null) {
                    var earned = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            loadRewarded()
                            enableImmersiveMode()
                            if (earned) {
                                webView.evaluateJavascript("window._onRewardEarned && window._onRewardEarned();", null)
                            }
                            webView.evaluateJavascript("window._onRewardClosed && window._onRewardClosed();", null)
                        }
                        override fun onAdFailedToShowFullScreenContent(err: AdError) {
                            rewardedAd = null
                            loadRewarded()
                            enableImmersiveMode()
                            webView.evaluateJavascript("window._onRewardClosed && window._onRewardClosed();", null)
                        }
                    }
                    ad.show(this@MainActivity) { earned = true }
                } else {
                    loadRewarded()
                    webView.evaluateJavascript("window._onRewardClosed && window._onRewardClosed();", null)
                }
            }
        }
    }
}