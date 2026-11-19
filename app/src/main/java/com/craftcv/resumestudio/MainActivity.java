package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.DownloadManager;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.util.Base64;
import android.os.Handler;
import android.os.Looper;
import android.content.ClipData;
import java.io.OutputStream;
import org.json.JSONObject;
import org.json.JSONTokener;
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
    /** Optional initial online workflow, e.g. "cloud" for the Cloud Account screen. */
    public static final String EXTRA_START_PAGE = "craftcv_start_online_page";
    private static final String HOME = "https://craftcv-hees13.v2.appdeploy.ai/";
    private static final String HOST = "craftcv-hees13.v2.appdeploy.ai";
    private static final int PICK_FILE = 301;
    private static final int SAVE_WEB_DOWNLOAD = 302;
    private static final int MAX_BLOB_BYTES = 8 * 1024 * 1024;
    private byte[] pendingWebFile;
    private String pendingWebMime;
    private String pendingWebName;
    private boolean blobInProgress=false;
    private boolean pageFailed=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
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
        addAction(toolbar,"‹",v->navigateBackSafely());
        addAction(toolbar,"Online Studio",v->webView.loadUrl(HOME));
        addAction(toolbar,"Save PDF",v->printWebResume());
        addAction(toolbar,"Refresh",v->{if(webView!=null)webView.reload();else recreate();});
        message=new TextView(this);
        message.setTextColor(Color.DKGRAY);
        message.setPadding(dp(18),dp(13),dp(18),dp(13));
        message.setText("Opening online CraftCV…");
        message.setVisibility(View.VISIBLE);
        root.addView(message);
        webView=new WebView(this);
        webView.setBackgroundColor(0xFFF5F8F9);
        root.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
        configure();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                this::navigateBackSafely);
        }
        String start=getIntent().getStringExtra(EXTRA_START_PAGE);
        String route="cloud".equals(start)?"#cloud":"ai".equals(start)?"#ai":"";
        webView.loadUrl(HOME+route);
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
    private void printWebResume(){
        if(webView==null || webView.getUrl()==null || !trusted(Uri.parse(webView.getUrl()))){
            Toast.makeText(this,"Open CraftCV first before printing",Toast.LENGTH_LONG).show();
            return;
        }
        // A WebView on Home/Design/Library does not mount the resume paper.
        // Navigate to the real CV first, then print only after React has rendered it.
        String fragment=Uri.parse(webView.getUrl()).getFragment();
        if(!"cv-preview".equals(fragment)){
            webView.evaluateJavascript(
                "(function(){window.location.hash='cv-preview';return true;})()",
                result->handler.postDelayed(this::printVisibleWebResume,900)
            );
        }else{
            printVisibleWebResume();
        }
    }
    private void printVisibleWebResume(){
        if(webView==null || webView.getUrl()==null || !trusted(Uri.parse(webView.getUrl()))){
            Toast.makeText(this,"Online CV is not available",Toast.LENGTH_LONG).show();
            return;
        }
        String script="(function(){var paper=document.getElementById('resume-paper');"
            +"return !!paper && paper.getClientRects().length>0 && paper.offsetHeight>100;})()";
        webView.evaluateJavascript(script,result->{
            if(!"true".equals(result)){
                Toast.makeText(this,"CV preview is not visible yet. Open Finished CV and retry.",Toast.LENGTH_LONG).show();
                return;
            }
            try{
                PrintManager manager=(PrintManager)getSystemService(Context.PRINT_SERVICE);
                if(manager==null)throw new IllegalStateException("Print service unavailable");
                manager.print("CraftCV Resume",webView.createPrintDocumentAdapter("CraftCV Resume"),
                    new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR).build());
            }catch(Exception e){
                Toast.makeText(this,"Could not open Save as PDF. Use native View CV instead.",Toast.LENGTH_LONG).show();
            }
        });
    }
    private void installDownloadAndPrintHooks(){
        if(webView==null || !trusted(Uri.parse(webView.getUrl())))return;
        String script="(function(){"
            +"window.print=function(){window.location.href='craftcv-print://open';};"
            +"if(window.__craftcvHooksInstalled)return;window.__craftcvHooksInstalled=true;"
            +"document.addEventListener('click',function(ev){"
            +"var a=ev.target.closest&&ev.target.closest('a[download]');"
            +"if(a&&a.href&&a.href.indexOf('blob:')===0){"
            +"ev.preventDefault();ev.stopPropagation();"
            +"window.location.href='craftcv-download://save?url='+encodeURIComponent(a.href)+'&name='+encodeURIComponent(a.getAttribute('download')||'');"
            +"}},true);"
            +"})();";
        webView.evaluateJavascript(script,null);
    }
    private void downloadBlob(String blobUrl,String filename,String mime){
        if(blobInProgress){Toast.makeText(this,"A download is already being prepared",Toast.LENGTH_SHORT).show();return;}
        if(webView==null || !trusted(Uri.parse(webView.getUrl())) || !blobUrl.startsWith("blob:")){
            Toast.makeText(this,"Download blocked: unexpected source",Toast.LENGTH_LONG).show();return;
        }
        blobInProgress=true;
        pendingWebName=(filename==null||filename.trim().isEmpty())?"CraftCV-Resume.docx":filename.replaceAll("[^a-zA-Z0-9._-]","_");
        if(pendingWebName.length()>100)pendingWebName=pendingWebName.substring(0,100);
        pendingWebMime=(mime==null||mime.isEmpty()||mime.equals("application/octet-stream"))
            ?"application/vnd.openxmlformats-officedocument.wordprocessingml.document":mime;
        String code="(function(){window.__craftcvBlobResult='WAIT';"
            +"fetch("+JSONObject.quote(blobUrl)+").then(function(r){return r.blob();})"
            +".then(function(blob){"
            +"if(blob.size>"+MAX_BLOB_BYTES+")throw Error('File larger than 8 MB');"
            +"var reader=new FileReader();"
            +"reader.onload=function(){window.__craftcvBlobResult=String(reader.result);};"
            +"reader.onerror=function(){window.__craftcvBlobResult='ERROR:Cannot read document';};"
            +"reader.readAsDataURL(blob);"
            +"})"
            +".catch(function(e){window.__craftcvBlobResult='ERROR:'+String(e.message||e);});"
            +"})();";
        webView.evaluateJavascript(code,null);
        handler.postDelayed(()->pollBlobResult(0),280);
    }
    private void pollBlobResult(int attempt){
        if(!blobInProgress || webView==null)return;
        if(attempt>=25){
            blobInProgress=false;
            Toast.makeText(this,"Download timed out. Try again.",Toast.LENGTH_LONG).show();
            return;
        }
        webView.evaluateJavascript("window.__craftcvBlobResult || 'WAIT'",value->{
            String decoded="WAIT";
            try{
                Object parsed=new JSONTokener(value).nextValue();
                if(parsed!=null)decoded=String.valueOf(parsed);
            }catch(Exception ignored){}
            if(decoded.equals("WAIT") || decoded.equals("null")){
                handler.postDelayed(()->pollBlobResult(attempt+1),280);
                return;
            }
            if(decoded.startsWith("ERROR:")){
                blobInProgress=false;
                Toast.makeText(this,decoded,Toast.LENGTH_LONG).show();
                return;
            }
            int separator=decoded.indexOf(";base64,");
            if(!decoded.startsWith("data:") || separator<0){
                blobInProgress=false;
                Toast.makeText(this,"The document could not be decoded",Toast.LENGTH_LONG).show();
                return;
            }
            try{
                String raw=decoded.substring(separator+8);
                if(raw.length()>MAX_BLOB_BYTES*2)throw new IllegalArgumentException("Too large");
                pendingWebFile=Base64.decode(raw,Base64.DEFAULT);
                if(pendingWebFile.length>MAX_BLOB_BYTES)throw new IllegalArgumentException("Too large");
                blobInProgress=false;
                Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(pendingWebMime);
                intent.putExtra(Intent.EXTRA_TITLE,pendingWebName);
                startActivityForResult(intent,SAVE_WEB_DOWNLOAD);
            }catch(Exception e){
                blobInProgress=false;pendingWebFile=null;
                Toast.makeText(this,"Could not prepare the file: "+e.getMessage(),Toast.LENGTH_LONG).show();
            }
            webView.evaluateJavascript("window.__craftcvBlobResult = null",null);
        });
    }
    private void showStatus(String status){
        if(message==null)return;
        message.setText(status);
        message.setVisibility(View.VISIBLE);
    }
    /**
     * A successful HTTP load is not proof the React UI rendered. On older Android
     * WebViews a script error can otherwise leave an entirely blank white page.
     */
    private void verifyOnlineScreen(WebView loadedView){
        if(loadedView==null || loadedView!=webView)return;
        String url=loadedView.getUrl();
        if(url==null || !trusted(Uri.parse(url)) || pageFailed)return;
        String script="(function(){var root=document.getElementById('root');"
            +"return !!root && !!root.innerText && root.innerText.trim().length>35;})()";
        loadedView.evaluateJavascript(script, result -> {
            if(webView!=loadedView || pageFailed)return;
            if("true".equals(result)){
                message.setVisibility(View.GONE);
            }else{
                showStatus("Online Studio did not finish displaying. Tap Refresh above, or press Back to use your offline CV builder. Your saved CVs are safe.");
            }
        });
    }
    private void configure() {
        WebSettings settings=webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // Respect the HTML device-width viewport instead of laying out a
        // desktop-width page and shrinking it to fit a phone screen.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setTextZoom(100);
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
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){
                pageFailed=false;
                showStatus("Opening CraftCV Online Studio…");
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();
                if("craftcv-print".equalsIgnoreCase(uri.getScheme())){
                    view.post(()->printWebResume());return true;
                }
                if("craftcv-download".equalsIgnoreCase(uri.getScheme())){
                    final String requested=uri.getQueryParameter("url");
                    final String fileName=uri.getQueryParameter("name");
                    view.post(()->downloadBlob(requested==null?"":requested,fileName,
                        fileName!=null&&fileName.endsWith(".pdf")?"application/pdf":
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
                    return true;
                }
                if(trusted(uri))return false;
                if("https".equalsIgnoreCase(uri.getScheme()))openExternal(uri.toString());
                return true;
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
                if(request.isForMainFrame()) {
                    pageFailed=true;
                    showStatus("Online Studio could not load. Tap Refresh above, or press Back for the offline CV builder. Your saved CVs remain safe.");
                }
            }
            @Override public void onPageFinished(WebView view,String url) {
                if(url!=null&&trusted(Uri.parse(url))){
                    installDownloadAndPrintHooks();
                    if(!pageFailed)handler.postDelayed(()->verifyOnlineScreen(view),1500);
                }else if(url==null||url.startsWith("about:blank")){
                    pageFailed=true;
                    showStatus("Page unavailable. Tap Refresh or Back to return to the offline CV builder.");
                }
            }
            @Override public boolean onRenderProcessGone(WebView view,android.webkit.RenderProcessGoneDetail detail){
                pageFailed=true;
                showStatus("Online Studio stopped responding. Tap Refresh to reopen it, or Back to use your offline CV.");
                root.removeView(view);
                view.destroy();
                webView=null;
                return true;
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){
                handler.cancel();
                pageFailed=true;
                showStatus("Secure connection failed. Tap Back to return to the offline CV builder.");
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
            if("blob".equalsIgnoreCase(uri.getScheme())){
                String suggested=URLUtil.guessFileName(url,disposition,mime);
                if(suggested==null||!suggested.contains("."))suggested="CraftCV-Resume.docx";
                downloadBlob(url,suggested,mime);
                return;
            }
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
        if(request==SAVE_WEB_DOWNLOAD){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingWebFile!=null){
                try(OutputStream output=getContentResolver().openOutputStream(data.getData())){
                    if(output==null)throw new IllegalStateException("File destination unavailable");
                    output.write(pendingWebFile);
                    output.flush();
                    Toast.makeText(this,"CraftCV file saved successfully",Toast.LENGTH_LONG).show();
                }catch(Exception e){
                    Toast.makeText(this,"File save failed: "+e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }
            pendingWebFile=null;
            return;
        }
        if(request==PICK_FILE&&pendingFiles!=null){
            pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));
            pendingFiles=null;
        }
    }
    private void navigateBackSafely(){
        if(webView!=null&&webView.canGoBack()){
            android.webkit.WebBackForwardList history=webView.copyBackForwardList();
            int previous=history.getCurrentIndex()-1;
            if(previous>=0){
                android.webkit.WebHistoryItem item=history.getItemAtIndex(previous);
                if(item!=null&&item.getUrl()!=null&&trusted(Uri.parse(item.getUrl()))){
                    webView.goBack();
                    return;
                }
            }
        }
        // Never navigate to about:blank, blob, or a custom-scheme URL.
        finish();
    }
    @SuppressWarnings("deprecation")
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){
        navigateBackSafely();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if(webView!=null){root.removeView(webView);webView.destroy();webView=null;}
        super.onDestroy();
    }
}
