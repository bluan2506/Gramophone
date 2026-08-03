package org.akanework.gramophone.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.BaseActivity

class SplashActivity : BaseActivity() {

    companion object {
        private const val MIN_VISIBLE_MS = 1200L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(MIN_VISIBLE_MS)
            goToMain()
        }
    }

    private fun goToMain() {
        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
