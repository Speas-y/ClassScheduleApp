package com.schedule.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.schedule.app.notification.AlarmScheduler;
import com.schedule.app.ui.course.AddCourseActivity;
import com.schedule.app.ui.import_.ImportActivity;
import com.schedule.app.ui.schedule.ScheduleFragment;
import com.schedule.app.ui.schedule.ScheduleViewModel;
import com.schedule.app.ui.settings.SettingsActivity;

/**
 * 主界面：容器内展示 {@link com.schedule.app.ui.schedule.ScheduleFragment}，
 * 负责通知权限（Android 13+）、底部导航跳转（课表/导入/设置）与悬浮添加入口。
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

        viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);
        setupMainNavigation();

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
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_schedule);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupMainNavigation() {
        FloatingActionButton fabAddCourse = findViewById(R.id.fabAddCourse);
        fabAddCourse.setOnClickListener(v -> startActivity(new Intent(this, AddCourseActivity.class)));

        // 底部导航只承载主要入口；添加课程保持为右下角悬浮按钮，贴近课表页的高频操作。
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_schedule);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_schedule) {
                viewModel.setWeek(viewModel.calculateCurrentWeek());
                return true;
            } else if (id == R.id.nav_import) {
                startActivity(new Intent(this, ImportActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }
}
