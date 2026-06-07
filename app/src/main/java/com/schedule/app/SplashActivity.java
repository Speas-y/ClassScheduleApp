package com.schedule.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

/**
 * 启动页：展示短进入动画后进入主课表页面。
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openMainRunnable = () -> {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.main_enter,
                R.anim.splash_exit
        );
        startActivity(new Intent(this, MainActivity.class), options.toBundle());
        finish();
    };

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

        handler.postDelayed(openMainRunnable, SPLASH_DURATION_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openMainRunnable);
        super.onDestroy();
    }
}
