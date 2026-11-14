package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.URLUtil;
import android.content.ClipData;
import android.content.ClipboardManager;

public class MainActivity extends Activity {
    private static final String HOME = "https://craftcv-hees13.v2.appdeploy.ai/";
    private static final String HOST = "craftcv-hees13.v2.appdeploy.ai";
    private static final int PICK_FILE = 301;
    private WebView webView;
    private LinearLayout root;
    private TextView message;
    private android.webkit.ValueCallback<Uri[]> pendingFiles;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18,56,70));
        getWindow().setNavigationBarColor(Color.rgb(18,56,70));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
                return insets;
            });
        }
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setBackgroundColor(Color.rgb(18,56,70));
        toolbar.setPadding(dp(8),dp(5),dp(8),dp(5));
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(46)));
        addAction(toolbar,"‹",v -> { if (webView.canGoBack()) webView.goBack(); else webView.loadUrl(HOME); });
        addAction(toolbar,"CraftCV",v -> webView.loadUrl(HOME));
        addAction(toolbar,"↻",v -> webView.reload());
        addAction(toolbar,"Share",v -> share());
        addAction(toolbar,"Privacy",v -> openExternal(HOME+"privacy.html"));
        message = new TextView(this);
        message.setTextColor(Color.DKGRAY);
        message.setPadding(dp(22),dp(16),dp(22),dp(16));
        message.setGravity(Gravity.CENTER);
        message.setVisibility(View.GONE);
        root.addView(message);
        webView = new WebView(this);
        root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
        configure();
        webView.loadUrl(HOME);
    }

    private int dp(int value){return Math.round(getResources().getDisplayMetrics().density*value);}
    private void addAction(LinearLayout layout,String label,View.OnClickListener click){
        Button button=new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(label.equals("CraftCV") ? 16 : 12);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(8),0,dp(8),0);
        button.setOnClickListener(click);
        layout.addView(button,new LinearLayout.LayoutParams(0,-1,label.equals("CraftCV")?2:1));
    }
    private boolean trusted(Uri uri){
        return "https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost());
    }
    private void openExternal(String target){
        try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(target))); }
        catch (ActivityNotFoundException e) {Toast.makeText(this,"No browser available",Toast.LENGTH_LONG).show();}
    }
    private void share(){
        Intent intent=new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,HOME);
        startActivity(Intent.createChooser(intent,"Share CraftCV"));
    }
    private void configure(){
        WebSettings settings=webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView,false);
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();
                if (trusted(uri)) return false;
                if ("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme())) {
                    openExternal(uri.toString());return true;
                }
                return true;
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
                if(request.isForMainFrame()){
                    message.setText("CraftCV couldn't connect. Check your internet connection, then tap Refresh.");
                    message.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onPageFinished(WebView view,String url){
                if(!message.getText().toString().isEmpty() && trusted(Uri.parse(url))){
                    message.setVisibility(View.GONE);
                    message.setText("");
                }
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){
                handler.cancel();
                message.setText("Secure connection failed. Please try again later.");
                message.setVisibility(View.VISIBLE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView view,android.webkit.ValueCallback<Uri[]> callback,FileChooserParams params){
                if(pendingFiles!=null) pendingFiles.onReceiveValue(null);
                pendingFiles=callback;
                try { startActivityForResult(params.createIntent(),PICK_FILE);return true; }
                catch(Exception e){pendingFiles=null;callback.onReceiveValue(null);Toast.makeText(MainActivity.this,"File picker unavailable",Toast.LENGTH_LONG).show();return false;}
            }
        });
        webView.setDownloadListener((url,userAgent,contentDisposition,mimeType,contentLength)->{
            Uri uri=Uri.parse(url);
            if(!trusted(uri)){openExternal(url);return;}
            try {
                DownloadManager.Request req=new DownloadManager.Request(uri);
                req.setMimeType(mimeType);
                req.addRequestHeader("User-Agent",userAgent);
                String cookie=CookieManager.getInstance().getCookie(url);
                if(cookie!=null)req.addRequestHeader("Cookie",cookie);
                req.setTitle(URLUtil.guessFileName(url,contentDisposition,mimeType));
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalFilesDir(MainActivity.this,Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,contentDisposition,mimeType));
                ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(req);
                Toast.makeText(MainActivity.this,"Downloading file",Toast.LENGTH_SHORT).show();
            } catch(Exception e){Toast.makeText(MainActivity.this,"Download unavailable",Toast.LENGTH_LONG).show();}
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_FILE && pendingFiles!=null){
            pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));
            pendingFiles=null;
        }
    }
    @Override public void onBackPressed(){
        if(webView!=null && webView.canGoBack())webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(webView!=null){root.removeView(webView);webView.destroy();webView=null;}
        super.onDestroy();
    }
}
