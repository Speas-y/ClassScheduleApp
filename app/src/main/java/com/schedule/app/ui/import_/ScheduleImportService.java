package com.schedule.app.ui.import_;

import android.content.Context;
import android.net.Uri;

import com.schedule.app.R;
import com.schedule.app.data.entity.Course;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 课表导入协调类：统一文件读取限制与 Markdown/JSON/HTML 解析顺序。
 */
public class ScheduleImportService {

    public static final int MAX_IMPORT_FILE_BYTES = 2 * 1024 * 1024;

    private final Context context;

    public ScheduleImportService(Context context) {
        this.context = context.getApplicationContext();
    }

    public String readTextFromUri(Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                throw new IOException("无法读取所选文件");
            }
            return readStreamAsUtf8String(is, MAX_IMPORT_FILE_BYTES);
        }
    }

    public ImportResult parseText(String text) {
        List<Course> courses = new KbcxMarkdownParser().parse(text);
        if (!courses.isEmpty()) {
            return new ImportResult(courses, "Markdown 文件");
        }

        courses = new ZhengfangKbListJsonParser().parse(text);
        if (!courses.isEmpty()) {
            return new ImportResult(courses, context.getString(R.string.import_method_json));
        }

        courses = new ZhengfangParser().parse(text);
        if (!courses.isEmpty()) {
            return new ImportResult(courses, "外部浏览器保存的 HTML");
        }

        return new ImportResult(courses, "");
    }

    private static String readStreamAsUtf8String(InputStream is, int maxBytes) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int total = 0;
        int n;
        while ((n = is.read(b)) != -1) {
            total += n;
            if (total > maxBytes) {
                throw new IOException("文件超过 2MB，请选择课表导出的 HTML、JSON 或 Markdown 文件");
            }
            buf.write(b, 0, n);
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }
}
