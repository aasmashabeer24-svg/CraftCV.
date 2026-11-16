package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
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

/** Optional online studio. NativeResumeActivity remains functional without this website. */
public final class MainActivity extends Activity {
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
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
        if(Build.VERSION.SDK_INT>=35) {
            root.setOnApplyWindowInsetsListener((v,insets)->{
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
                return insets;
            });
        }
        LinearLayout toolbar=new LinearLayout(this);
        toolbar.setBackgroundColor(Color.rgb(18,56,70));
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(50)));
        addAction(toolbar,"‹",v->finish());
        addAction(toolbar,"Online Studio",v->webView.loadUrl(HOME));
        addAction(toolbar,"Refresh",v->webView.reload());
        addAction(toolbar,"Privacy",v->openExternal(HOME+"privacy.html"));
        message=new TextView(this);
        message.setTextColor(Color.DKGRAY);
        message.setPadding(dp(18),dp(13),dp(18),dp(13));
        message.setVisibility(View.GONE);
        root.addView(message);
        webView=new WebView(this);
        root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
        configure();
        webView.loadUrl(HOME);
    }
    private int dp(int value){return Math.round(getResources().getDisplayMetrics().density*value);}
    private void addAction(LinearLayout bar,String label,View.OnClickListener action) {
        Button button=new Button(this);
        button.setText(label);
        button.setTextSize(label.equals("Online Studio")?15:12);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(action);
        bar.addView(button,new LinearLayout.LayoutParams(0,-1,label.equals("Online Studio")?2:1));
    }
    private boolean trusted(Uri uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost());
    }
    private void openExternal(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); }
        catch(ActivityNotFoundException error){Toast.makeText(this,"No browser found",Toast.LENGTH_LONG).show();}
    }
    private void configure() {
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
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView,false);
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();
                if(trusted(uri))return false;
                if("https".equalsIgnoreCase(uri.getScheme()))openExternal(uri.toString());
                return true;
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
                if(request.isForMainFrame()) {
                    message.setText("Online studio unavailable. Your offline resumes are safe. Return to the native editor.");
                    message.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onPageFinished(WebView view,String url) {
                if(trusted(Uri.parse(url)))message.setVisibility(View.GONE);
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){
                handler.cancel();
                message.setText("Secure connection failed. Return to the offline studio.");
                message.setVisibility(View.VISIBLE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView view,android.webkit.ValueCallback<Uri[]> callback,FileChooserParams params) {
                if(pendingFiles!=null)pendingFiles.onReceiveValue(null);
                pendingFiles=callback;
                try {startActivityForResult(params.createIntent(),PICK_FILE);return true;}
                catch(Exception error){pendingFiles=null;callback.onReceiveValue(null);return false;}
            }
        });
        webView.setDownloadListener((url,agent,disposition,mime,length)->{
            Uri uri=Uri.parse(url);
            if(!trusted(uri)){openExternal(url);return;}
            try {
                DownloadManager.Request request=new DownloadManager.Request(uri);
                request.setMimeType(mime);
                request.addRequestHeader("User-Agent",agent);
                String cookie=CookieManager.getInstance().getCookie(url);
                if(cookie!=null)request.addRequestHeader("Cookie",cookie);
                String name=URLUtil.guessFileName(url,disposition,mime);
                request.setTitle(name);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,name);
                ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this,"Download started",Toast.LENGTH_SHORT).show();
            }catch(Exception ignored){Toast.makeText(this,"Download unavailable",Toast.LENGTH_LONG).show();}
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==PICK_FILE&&pendingFiles!=null){
            pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));
            pendingFiles=null;
        }
    }
    @SuppressWarnings("deprecation")
    @Override public void onBackPressed(){
        if(webView!=null&&webView.canGoBack())webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if(webView!=null){root.removeView(webView);webView.destroy();webView=null;}
        super.onDestroy();
    }
}
