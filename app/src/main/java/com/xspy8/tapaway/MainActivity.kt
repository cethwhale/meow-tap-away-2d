package com.xspy8.tapaway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        
        MobileAds.initialize(this) {}
        loadInterstitialAd()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F9FE)
                ) {
                    GogoBlastGameRoot(
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
            loadInterstitialAd()
        } else {
            loadInterstitialAd()
        }
    }
}

data class PuzzleArrow(val id: Int, val direction: String, var isCleared: Boolean)

@Composable
fun GogoBlastGameRoot(onLevelComplete: () -> Unit) {
    var level by remember { mutableStateOf(3) }
    var score by remember { mutableStateOf(1) }
    var isLevelWon by remember { mutableStateOf(false) }

    var arrows by remember(level) {
        mutableStateOf(
            listOf(
                PuzzleArrow(1, "⬆️", false),
                PuzzleArrow(2, "➡️", false),
                PuzzleArrow(3, "⬇️", false),
                PuzzleArrow(4, "⬅️", false)
            )
        )
    }

    val totalArrows = arrows.size
    val clearedCount = arrows.count { it.isCleared }
    val progressPercent = if (totalArrows > 0) (clearedCount * 100) / totalArrows else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "👑 x$score", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "GOGO! BLAST", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF3F51B5))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "⚙️", fontSize = 18.sp)
        }

        if (!isLevelWon) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Level $level", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Text(text = "$progressPercent%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF3F51B5),
                    trackColor = Color(0xFFE0E0E0),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "Tap the arrows to clear the board!", fontSize = 14.sp, color = Color.Gray)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        arrows.forEach { arrow ->
                            if (!arrow.isCleared) {
                                Surface(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clickable {
                                            arrows = arrows.map {
                                                if (it.id == arrow.id) it.copy(isCleared = true) else it
                                            }
                                            if (arrows.all { a -> a.isCleared }) {
                                                isLevelWon = true
                                                score += 1
                                                onLevelComplete()
                                            }
                                        },
                                    color = Color.White,
                                    shape = RoundedCornerShape(14.dp),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = arrow.direction, fontSize = 28.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = Color(0xFFFFF8E1),
                    shadowElevation = 2.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "🏆", fontSize = 40.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Well done!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                Text(text = "Level $level Complete", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "IQ: 110", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA000))
                
                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        level += 1
                        isLevelWon = false
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Level $level", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
