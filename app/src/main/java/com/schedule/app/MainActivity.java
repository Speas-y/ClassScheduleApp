package com.schedule.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.schedule.app.notification.AlarmScheduler;
import com.schedule.app.ui.course.AddCourseActivity;
import com.schedule.app.ui.import_.ImportActivity;
import com.schedule.app.ui.schedule.ScheduleFragment;
import com.schedule.app.ui.schedule.ScheduleViewModel;
import com.schedule.app.ui.settings.SettingsActivity;

/**
 * 主界面：容器内展示 {@link com.schedule.app.ui.schedule.ScheduleFragment}，
 * 负责通知权限（Android 13+）、菜单跳转（添加/导入/设置/本周）。
 */
public class MainActivity extends AppCompatActivity {

    private ScheduleViewModel viewModel;

    /** 用户授权 POST_NOTIFICATIONS 后，重新登记所有课前闹钟。 */
    private final ActivityResultLauncher<String> notificationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    AlarmScheduler.scheduleAllAlarms(this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ScheduleFragment())
                    .commit();
        }

        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.setWeek(viewModel.calculateCurrentWeek());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            startActivity(new Intent(this, AddCourseActivity.class));
            return true;
        } else if (id == R.id.action_import_jwxt) {
            startActivity(new Intent(this, ImportActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_today) {
            viewModel.setWeek(viewModel.calculateCurrentWeek());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
