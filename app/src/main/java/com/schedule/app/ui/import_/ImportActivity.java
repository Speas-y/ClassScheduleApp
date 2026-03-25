package com.schedule.app.ui.import_;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.schedule.app.R;
import com.schedule.app.data.entity.Course;
import com.schedule.app.data.repository.CourseRepository;
import com.schedule.app.notification.AlarmScheduler;

import java.util.List;

/**
 * 在应用内打开正方教务：手动登录并进入「学生课表」页面后，点击悬浮按钮导入。
 * 优先请求与 {@code fetch_kbcx.py} 相同的课表 JSON 接口，失败再解析当前页 HTML。
 */
public class ImportActivity extends AppCompatActivity {

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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        repository = new CourseRepository(getApplication());

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
        settings.setUserAgentString(settings.getUserAgentString().replace("Mobile", ""));

        webView.addJavascriptInterface(new JsBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
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
        if (item.getItemId() == R.id.action_change_jwxt_url) {
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

    private void showImportDialog(List<Course> courses, String methodHint) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_confirm_title)
                .setMessage(getString(R.string.import_confirm_message, courses.size(), methodHint))
                .setPositiveButton(R.string.import_confirm_ok, (dialog, which) -> {
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
