package com.rickrock777.godseye;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFS = "gev_android";
    private static final String KEY_BACKEND_URL = "backend_url";
    private static final int REQ_MIC = 1001;
    private static final int REQ_LOCATION = 1002;

    private final Set<String> allowedWebPermissionResources = new HashSet<>(
            Arrays.asList(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
    );

    private SharedPreferences prefs;
    private WebView webView;
    private String backendUrl;
    private PermissionRequest pendingWebPermission;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        enterImmersiveMode();
        backendUrl = prefs.getString(KEY_BACKEND_URL, null);
        if (backendUrl == null || backendUrl.trim().isEmpty()) {
            showSetupScreen();
        } else {
            showBrowser(backendUrl);
        }
    }

    private void enterImmersiveMode() {
        Window window = getWindow();
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void showSetupScreen() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(7, 12, 18));

        TextView title = new TextView(this);
        title.setText("GOD'S EYE VIEW");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Android client for your self-hosted God's Eye View server");
        subtitle.setTextColor(Color.rgb(170, 190, 205));
        subtitle.setTextSize(15);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, dp(24));
        root.addView(subtitle, matchWrap());

        EditText urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.rgb(100, 120, 135));
        urlInput.setHint("https://your-gev-server.example");
        if (backendUrl != null) urlInput.setText(backendUrl);
        urlInput.setTextSize(15);
        urlInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(urlInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Button connect = new Button(this);
        connect.setText("CONNECT");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dp(14);
        root.addView(connect, buttonParams);

        TextView note = new TextView(this);
        note.setText("Production servers must use HTTPS. Debug builds also allow private-LAN HTTP addresses such as 192.168.x.x:4173.");
        note.setTextColor(Color.rgb(120, 145, 160));
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note, matchWrap());

        connect.setOnClickListener(v -> {
            String candidate = normalizeUrl(urlInput.getText().toString());
            if (!isAllowedBackendUrl(candidate)) {
                Toast.makeText(this, "Use HTTPS, or a private/local HTTP address in a debug build.", Toast.LENGTH_LONG).show();
                return;
            }
            backendUrl = candidate;
            prefs.edit().putString(KEY_BACKEND_URL, backendUrl).apply();
            showBrowser(backendUrl);
        });

        setContentView(root);
    }

    private void showBrowser(String url) {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        configureWebView(webView);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        Button menu = new Button(this);
        menu.setText("⋮");
        menu.setTextSize(22);
        menu.setTextColor(Color.WHITE);
        menu.setBackgroundColor(Color.argb(120, 12, 20, 28));
        menu.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.TOP | Gravity.END);
        menuParams.setMargins(0, dp(8), dp(8), 0);
        root.addView(menu, menuParams);
        menu.setOnClickListener(v -> showNativeMenu());

        setContentView(root);
        webView.loadUrl(url);
    }

    private void configureWebView(WebView view) {
        WebSettings s = view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " GodsEyeViewAndroid/0.1.0");

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                Uri uri = request.getUrl();
                Uri backend = Uri.parse(backendUrl);
                if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                        && sameHostAndPort(uri, backend)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No app can open this link.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermissionRequest(request));
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!sameHostAndPort(Uri.parse(origin), Uri.parse(backendUrl))) { callback.invoke(origin, false, false); return; }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, REQ_LOCATION);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                setContentView(view);
                enterImmersiveMode();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                customView = null;
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                customViewCallback = null;
                showBrowser(backendUrl);
            }
        });
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        if (!sameHostAndPort(request.getOrigin(), Uri.parse(backendUrl))) { request.deny(); return; }
        Set<String> requested = new HashSet<>(Arrays.asList(request.getResources()));
        if (!allowedWebPermissionResources.containsAll(requested)) {
            request.deny();
            return;
        }
        if (requested.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingWebPermission = request;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        request.grant(request.getResources());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC && pendingWebPermission != null) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) pendingWebPermission.grant(pendingWebPermission.getResources());
            else pendingWebPermission.deny();
            pendingWebPermission = null;
        } else if (requestCode == REQ_LOCATION && pendingGeoCallback != null) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        }
    }

    private void showNativeMenu() {
        String[] items = {"Reload", "Change server", "Open in browser", "About"};
        new AlertDialog.Builder(this)
                .setTitle("God's Eye View")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (webView != null) webView.reload();
                            break;
                        case 1:
                            showSetupScreen();
                            break;
                        case 2:
                            if (backendUrl != null) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(backendUrl)));
                            }
                            break;
                        case 3:
                            new AlertDialog.Builder(this)
                                    .setTitle("God's Eye View for Android")
                                    .setMessage("Android shell v0.1.0\n\nConnects to your own God's Eye View server. API keys remain on the server rather than being embedded in the APK.")
                                    .setPositiveButton("OK", null)
                                    .show();
                            break;
                    }
                })
                .show();
    }

    private boolean sameHostAndPort(Uri a, Uri b) {
        if (a.getHost() == null || b.getHost() == null) return false;
        if (!a.getHost().equalsIgnoreCase(b.getHost())) return false;
        if (a.getScheme() == null || !a.getScheme().equalsIgnoreCase(b.getScheme())) return false;
        int ap = effectivePort(a);
        int bp = effectivePort(b);
        return ap == bp;
    }

    private int effectivePort(Uri uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String normalizeUrl(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (!v.contains("://") && !v.isEmpty()) v = "https://" + v;
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }

    private boolean isAllowedBackendUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null) return false;
            if ("https".equalsIgnoreCase(scheme)) return true;
            return "http".equalsIgnoreCase(scheme) && isPrivateOrLocalHost(host);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPrivateOrLocalHost(String host) {
        String h = host.toLowerCase(Locale.US);
        if (h.equals("localhost") || h.equals("127.0.0.1") || h.endsWith(".local")) return true;
        if (h.startsWith("10.") || h.startsWith("192.168.")) return true;
        if (h.startsWith("172.")) {
            String[] parts = h.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            if (customViewCallback != null) customViewCallback.onCustomViewHidden();
            customView = null;
            customViewCallback = null;
            showBrowser(backendUrl);
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
