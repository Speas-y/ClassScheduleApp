package com.schedule.app.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.schedule.app.data.entity.Course;

/**
 * Room 单库单表（courses），全应用通过 {@link #getInstance(Context)} 取同一实例。
 */
@Database(entities = {Course.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CourseDao courseDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "class_schedule_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
