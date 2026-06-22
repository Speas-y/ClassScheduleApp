package com.schedule.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

/**
 * 启动页：展示短进入动画后进入主课表页面。
 * 使用 CountDownTimer 替代 Handler+postDelayed，自动绑定生命周期避免内存泄漏。
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1500L;

    private CountDownTimer splashTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.splashLogo);
        View title = findViewById(R.id.splashTitle);
        View subtitle = findViewById(R.id.splashSubtitle);

        logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_logo_enter));
        title.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_text_enter));
        subtitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_text_enter));

        splashTimer = new CountDownTimer(SPLASH_DURATION_MS, SPLASH_DURATION_MS) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (!isFinishing() && !isDestroyed()) {
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                            SplashActivity.this,
                            R.anim.main_enter,
                            R.anim.splash_exit
                    );
                    startActivity(new Intent(SplashActivity.this, MainActivity.class), options.toBundle());
                    finish();
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (splashTimer != null) {
            splashTimer.cancel();
            splashTimer = null;
        }
        super.onDestroy();
    }
}
