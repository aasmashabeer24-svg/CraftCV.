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
import android.widget.FrameLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import android.webkit.URLUtil;
import android.content.ClipData;
import android.content.ClipboardManager;

public class MainActivity extends Activity {
    private static final String HOME = BuildConfig.CRAFTCV_WEB_URL;
    private static final Uri HOME_URI = Uri.parse(HOME);
    private static final String HOST = HOME_URI.getHost();
    private static final int PICK_FILE = 301;
    // Official Google test unit: never replace with a partner-bidding ID for direct AdMob.
    private static final String TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    private WebView webView;
    private FrameLayout bannerContainer;
    private AdView bannerAd;
    private ConsentInformation consentInformation;
    private boolean adsStarted = false;
    private boolean pageLoadFailed = false;
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
        bannerContainer = new FrameLayout(this);
        bannerContainer.setVisibility(View.GONE);
        root.addView(bannerContainer, new LinearLayout.LayoutParams(-1, -2));
        initializeConsentAndTestAds();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                () -> {
                    if (webView != null && webView.canGoBack()) webView.goBack();
                    else finish();
                }
            );
        }
        webView.loadUrl(HOME);
    }

    /** Request current consent status before ever attempting a test ad request. */
    private void initializeConsentAndTestAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(this, params,
            () -> {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> {
                    if (consentInformation.canRequestAds()) startTestBanner();
                });
            },
            error -> {
                // If the UMP request fails, do not request a fresh ad without consent.
                if (consentInformation.canRequestAds()) startTestBanner();
            }
        );
    }

    private void startTestBanner() {
        // Keep all release builds ad-free until a valid STANDARD production unit is configured.
        if (!BuildConfig.DEBUG || adsStarted || bannerContainer == null) return;
        adsStarted = true;
        new Thread(() -> MobileAds.initialize(getApplicationContext(), initializationStatus -> {
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                bannerAd = new AdView(this);
                bannerAd.setAdUnitId(TEST_BANNER_ID);
                bannerAd.setAdSize(AdSize.BANNER);
                bannerAd.setAdListener(new AdListener() {
                    @Override public void onAdLoaded() { bannerContainer.setVisibility(View.VISIBLE); }
                    @Override public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError error) { bannerContainer.setVisibility(View.GONE); }
                });
                bannerContainer.addView(bannerAd, new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));
                bannerAd.loadAd(new AdRequest.Builder().build());
            });
        })).start();
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
        return "https".equalsIgnoreCase(uri.getScheme())
            && HOST.equalsIgnoreCase(uri.getHost())
            && uri.getPort() == HOME_URI.getPort();
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
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon){
                pageLoadFailed = false;
            }
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
                    pageLoadFailed = true;
                    message.setText("CraftCV couldn't connect. Check your internet connection, then tap Refresh.");
                    message.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,android.webkit.WebResourceResponse response){
                if(request.isForMainFrame() && response.getStatusCode() >= 400){
                    pageLoadFailed = true;
                    message.setText("CraftCV is temporarily unavailable (HTTP "+response.getStatusCode()+"). Tap Refresh to retry.");
                    message.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onPageFinished(WebView view,String url){
                if(!pageLoadFailed && !message.getText().toString().isEmpty() && trusted(Uri.parse(url))){
                    message.setVisibility(View.GONE);
                    message.setText("");
                }
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){
                handler.cancel();
                pageLoadFailed = true;
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
    @SuppressWarnings("deprecation")
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){
        if(webView!=null && webView.canGoBack())webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(bannerAd!=null){bannerAd.destroy();bannerAd=null;}
        if(webView!=null){root.removeView(webView);webView.destroy();webView=null;}
        super.onDestroy();
    }
}
