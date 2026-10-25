package com.xspy8.tapaway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : ComponentActivity() {

    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob and load ad
        MobileAds.initialize(this) {}
        loadInterstitialAd()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F9FE)
                ) {
                    GogoBlastGameScreen(
                        onLevelComplete = {
                            showInterstitial()
                        }
                    )
                }
            }
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-7664427102906771/6690346986",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            }
        )
    }

    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
            // Reload the next ad for future use
            loadInterstitialAd()
        } else {
            loadInterstitialAd()
        }
    }
}

@Composable
fun GogoBlastGameScreen(onLevelComplete: () -> Unit) {
    var level by remember { mutableStateOf(3) }
    var score by remember { mutableStateOf(1) }
    var isCompleted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "👑 x$score", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "GOGO! BLAST", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF3F51B5))
            Text(text = "⚙️", fontSize = 20.sp)
        }

        // Game Board Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (!isCompleted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Level $level", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Tap the button below to clear the level!", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    Button(
                        onClick = {
                            isCompleted = true
                            score += 1
                            onLevelComplete() // Triggers the AdMob Interstitial Ad!
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Clear Level", fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            } else {
                // Level Complete Screen (like the video)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F9FE))
                ) {
                    Text(text = "🏆", fontSize = 60.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Well done!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                    Text(text = "Level $level Complete", fontSize = 18.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Button(
                        onClick = {
                            level += 1
                            isCompleted = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Level $level", fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                    }
                }
            }
        }
    }
}
