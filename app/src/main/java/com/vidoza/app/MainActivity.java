package com.vidoza.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.widget.Toast;
import android.content.ActivityNotFoundException;
import android.view.View;
import android.graphics.Color;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        webView.setBackgroundColor(Color.WHITE);
        webView.setVisibility(View.VISIBLE);

        setContentView(webView);

        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // Storage
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // File / content access
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Allow file:// page to access HTTPS resources
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        // Media
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Better compatibility
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Cache
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // WebView client
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                super.onReceivedError(view, request, error);

                // Don't replace the page with a blank error screen.
                // The HTML application remains loaded.
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });

        // Chrome / file chooser
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallbackNew,
                    FileChooserParams fileChooserParams) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = filePathCallbackNew;

                Intent intent;

                try {
                    intent = fileChooserParams.createIntent();

                    // Allow video + image selection
                    intent.setType("*/*");
                    intent.putExtra(
                            Intent.EXTRA_MIME_TYPES,
                            new String[]{
                                    "video/*",
                                    "image/*"
                            }
                    );

                    startActivityForResult(intent, FILE_CHOOSER);

                    return true;

                } catch (ActivityNotFoundException e) {

                    filePathCallback = null;

                    Toast.makeText(
                            MainActivity.this,
                            "File picker not available",
                            Toast.LENGTH_SHORT
                    ).show();

                    return false;
                }
            }
        });

        // Native bridge
        webView.addJavascriptInterface(
                new NativeBridge(),
                "Native"
        );

        // Load Vidoza
        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    public class NativeBridge {

        @JavascriptInterface
        public void share(String title, String text) {

            Intent intent =
                    new Intent(Intent.ACTION_SEND);

            intent.setType("text/plain");

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    title
            );

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    text
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Share"
                    )
            );
        }

        @JavascriptInterface
        public void emailSupport(
                String subject,
                String body) {

            Intent intent =
                    new Intent(Intent.ACTION_SENDTO);

            intent.setData(
                    Uri.parse("mailto:")
            );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    subject
            );

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    body
            );

            try {

                startActivity(
                        Intent.createChooser(
                                intent,
                                "Send support message"
                        )
                );

            } catch (Exception e) {

                Toast.makeText(
                        MainActivity.this,
                        "No email app found",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        @JavascriptInterface
        public void toast(String message) {

            runOnUiThread(() ->
                    Toast.makeText(
                            MainActivity.this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                requestCode == FILE_CHOOSER &&
                filePathCallback != null
        ) {

            Uri[] result =
                    WebChromeClient
                            .FileChooserParams
                            .parseResult(
                                    resultCode,
                                    data
                            );

            filePathCallback.onReceiveValue(result);

            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (webView == null) {
            super.onBackPressed();
            return;
        }

        webView.evaluateJavascript(
                "window.appBack ? window.appBack() : 'exit'",
                value -> {

                    if ("\"exit\"".equals(value)) {
                        MainActivity.super.onBackPressed();
                    }
                }
        );
    }
}
