# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Jsoup
-keeppackagenames org.jsoup.nodes

# Keep data entity
-keep class com.schedule.app.data.entity.Course { *; }

# Keep parsers (used via reflection or JS bridge)
-keep class com.schedule.app.ui.import_.** { *; }

# Keep notification receivers
-keep class com.schedule.app.notification.** { *; }
