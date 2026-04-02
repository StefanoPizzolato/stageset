package com.codex.stageset

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codex.stageset.ui.StageSetApp
import com.codex.stageset.ui.theme.StageSetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val container = (application as StageSetApplication).container

        setContent {
            StageSetTheme {
                StageSetApp(
                    previewSettingsRepository = container.previewSettingsRepository,
                    songRepository = container.songRepository,
                    setlistRepository = container.setlistRepository,
                )
            }
        }
    }
}
