package com.schedule.app.ui.import_;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.schedule.app.R;
import com.schedule.app.data.entity.Course;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.notification.AlarmScheduler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 在应用内打开正方教务：手动登录并进入「学生课表」页面后，点击悬浮按钮导入。
 * 优先请求与 {@code fetch_kbcx.py} 相同的课表 JSON 接口，失败再解析当前页 HTML。
 */
public class ImportActivity extends AppCompatActivity {

    /**
     * 默认 WebView UA 会带 wv 标记，部分教务系统会返回兼容性较差的内嵌页。
     * 这里使用标准移动 Chrome UA，让页面加载行为更接近手机 Chrome。
     */
    private static final String MOBILE_CHROME_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    /**
     * 相对路径与浏览器内课表页一致（位于 /kbcx/ 目录下），可自动带上登录 Cookie。
     */
    private static final String JS_FETCH_KB_JSON = """
            (function(){
              function selVal(id) {
                var el = document.getElementById(id);
                if (!el) return '';
                if (el.tagName === 'SELECT') {
                  var o = el.options[el.selectedIndex];
                  return o ? (o.value || '') : '';
                }
                return el.value || '';
              }
              try {
                var xnm = selVal('xnm');
                var xqm = selVal('xqm');
                var form = new URLSearchParams();
                form.set('xnm', xnm);
                form.set('xqm', xqm);
                form.set('kzlx', 'ck');
                form.set('xsdm', '');
                form.set('kclbdm', '');
                form.set('kclxdm', '');
                fetch('xskbcx_cxXsgrkb.html', {
                  method: 'POST',
                  credentials: 'include',
                  headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'application/json, text/javascript, */*; q=0.01'
                  },
                  body: form.toString()
                }).then(function(r) { return r.text(); })
                  .then(function(t) { AndroidBridge.onKbJson(t || ''); })
                  .catch(function() { AndroidBridge.onKbJson(''); });
              } catch (e) {
                AndroidBridge.onKbJson('');
              }
            })();
            """;

    private WebView webView;
    private ProgressBar progressBar;
    private CourseRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int loadingToken = 0;
    private final ActivityResultLauncher<String[]> externalFileImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    this::onExternalFilePicked);

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        repository = CourseRepository.getInstance(getApplication());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        FloatingActionButton fabImport = findViewById(R.id.fabImport);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(MOBILE_CHROME_USER_AGENT);

        webView.addJavascriptInterface(new JsBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                int token = ++loadingToken;
                mainHandler.postDelayed(() -> {
                    if (token == loadingToken && progressBar.getVisibility() == View.VISIBLE) {
                        Toast.makeText(ImportActivity.this,
                                "教务网页加载较慢，可检查网络/VPN，或稍后点右上角更换网址",
                                Toast.LENGTH_LONG).show();
                    }
                }, 15000);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadingToken++;
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    loadingToken++;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ImportActivity.this,
                            "教务网页加载失败，请检查网址、校园网或 VPN",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
                progressBar.setProgress(newProgress);
            }
        });

        fabImport.setOnClickListener(v -> extractSchedule());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });

        loadSavedUrl();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.import_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_open_external_browser) {
            openInExternalBrowser();
            return true;
        } else if (item.getItemId() == R.id.action_import_external_file) {
            externalFileImportLauncher.launch(new String[]{
                    "text/html",
                    "text/markdown",
                    "text/plain",
                    "application/json",
                    "text/*",
                    "*/*"
            });
            return true;
        } else if (item.getItemId() == R.id.action_change_jwxt_url) {
            showUrlInputDialog(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSavedUrl() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String url = prefs.getString("jwxt_url", "");

        if (url.isEmpty()) {
            showUrlInputDialog(false);
        } else {
            url = normalizeJwxUrl(url);
            webView.loadUrl(url);
        }
    }

    /**
     * @param allowCancelOnly 为 true 时，取消不会关闭 Activity（用于菜单「更换网址」）
     */
    private void showUrlInputDialog(boolean allowCancelOnly) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        EditText input = new EditText(this);
        input.setHint(R.string.jwxt_url_hint);
        String existing = prefs.getString("jwxt_url", "");
        if (!existing.isEmpty()) {
            input.setText(existing);
            input.setSelection(existing.length());
        }
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle(R.string.jwxt_url_title)
                .setMessage(R.string.jwxt_url_message)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        url = normalizeJwxUrl(url);
                        prefs.edit().putString("jwxt_url", url).apply();
                        webView.loadUrl(url);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if (!allowCancelOnly) {
                        finish();
                    }
                })
                .setCancelable(allowCancelOnly)
                .show();
    }

    /**
     * 只输入域名时默认用 HTTPS（避免 Android 9+ 默认禁止明文 HTTP）；若用户显式写了 http:// 则保留。
     * 仍保留 manifest 中 {@code usesCleartextTraffic}，供仅支持 HTTP 的教务站使用。
     */
    private static String normalizeJwxUrl(String url) {
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            return "https://" + u;
        }
        return u;
    }

    private void extractSchedule() {
        progressBar.setVisibility(View.VISIBLE);
        webView.evaluateJavascript(JS_FETCH_KB_JSON, null);
    }

    private void extractScheduleFromHtml() {
        String js = "(function() {"
                + "AndroidBridge.onHtmlReceived(document.documentElement.outerHTML);"
                + "})();";
        webView.evaluateJavascript(js, null);
    }

    private void openInExternalBrowser() {
        String url = webView.getUrl();
        if (url == null || url.isBlank() || url.startsWith("about:")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            url = prefs.getString("jwxt_url", "");
        }
        if (url == null || url.isBlank()) {
            showUrlInputDialog(false);
            return;
        }

        final String browserUrl = normalizeJwxUrl(url);
        new AlertDialog.Builder(this)
                .setTitle("外部浏览器导入")
                .setMessage("将用系统浏览器打开教务系统。若 App 内 WebView 很慢，可在浏览器登录并打开课表页，保存网页 HTML 或使用已有脚本导出 Markdown 后，回到本页选择「从浏览器文件导入」。")
                .setPositiveButton("打开浏览器", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)));
                    } catch (Exception e) {
                        Toast.makeText(this, "未找到可打开网页的浏览器", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onExternalFilePicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(this, "无法读取所选文件", Toast.LENGTH_SHORT).show();
                return;
            }

            String text = readStreamAsUtf8String(is);
            List<Course> courses = new KbcxMarkdownParser().parse(text);
            String method = "Markdown 文件";

            if (courses.isEmpty()) {
                courses = new ZhengfangKbListJsonParser().parse(text);
                method = getString(R.string.import_method_json);
            }

            if (courses.isEmpty()) {
                courses = new ZhengfangParser().parse(text);
                method = "外部浏览器保存的 HTML";
            }

            if (courses.isEmpty()) {
                Toast.makeText(this,
                        "未解析到课程。请确认文件是课表页 HTML、正方课表 JSON 或 kbcx_schedule.md。",
                        Toast.LENGTH_LONG).show();
                return;
            }

            showImportDialog(courses, method);
        } catch (IOException e) {
            Toast.makeText(this, "读取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String readStreamAsUtf8String(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = is.read(b)) != -1) {
            buf.write(b, 0, n);
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }

    private void showImportDialog(List<Course> courses, String methodHint) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_confirm_title)
                .setMessage(getString(R.string.import_confirm_message, courses.size(), methodHint))
                .setPositiveButton("合并导入", (dialog, which) -> {
                    repository.mergeCourses(courses, (added, skipped) -> runOnUiThread(() -> {
                        AlarmScheduler.scheduleAllAlarms(ImportActivity.this);
                        String msg = "新增 " + added + " 条";
                        if (skipped > 0) msg += "，跳过 " + skipped + " 条重复";
                        Toast.makeText(ImportActivity.this, msg, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        finish();
                    }));
                })
                .setNeutralButton("清空后导入", (dialog, which) -> {
                    repository.deleteAll();
                    repository.insertAllAndCallback(courses, () -> runOnUiThread(() -> {
                        AlarmScheduler.scheduleAllAlarms(ImportActivity.this);
                        Toast.makeText(ImportActivity.this,
                                getString(R.string.import_success, courses.size()),
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        finish();
                    }));
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                        progressBar.setVisibility(View.GONE))
                .show();
    }

    private class JsBridge {
        @JavascriptInterface
        public void onKbJson(String jsonText) {
            runOnUiThread(() -> {
                try {
                    List<Course> fromJson = new ZhengfangKbListJsonParser().parse(jsonText);
                    if (!fromJson.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        showImportDialog(fromJson,
                                getString(R.string.import_method_json));
                        return;
                    }
                } catch (Exception ignored) {
                }
                extractScheduleFromHtml();
            });
        }

        @JavascriptInterface
        public void onHtmlReceived(String html) {
            runOnUiThread(() -> {
                try {
                    ZhengfangParser parser = new ZhengfangParser();
                    List<Course> courses = parser.parse(html);

                    if (courses.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ImportActivity.this,
                                R.string.import_failed_html,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    showImportDialog(courses, getString(R.string.import_method_html));
                } catch (Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ImportActivity.this,
                            getString(R.string.import_parse_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
