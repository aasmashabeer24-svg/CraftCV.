package com.craftcv.resumestudio;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dedicated, full-screen finished CV viewer.
 * Displays the same generated PDF bytes that are saved and shared by Android.
 */
public final class ResumePreviewActivity extends Activity {
    private static final int SAVE_PDF = 601, SAVE_DOCX = 602;
    private byte[] pdfBytes;
    private byte[] pendingBytes;
    private Uri savedPdfUri;
    private JSONObject resume;
    private int style;
    private boolean shareAfterSave=false;
    private LinearLayout pages;
    private TextView info;
    private File previewFile;
    private Button savePdfButton;
    private Button sharePdfButton;
    private Button openSavedButton;
    private Uri lastSavedUri;
    private String lastSavedMime;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143745);
        getWindow().setNavigationBarColor(0xFF143745);
        loadCurrentResume();
        buildLayout();
        renderDocument();
    }
    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private Button button(String label,Runnable task){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF20576A));
        b.setOnClickListener(v->task.run());return b;
    }
    private void loadCurrentResume(){
        resume=new JSONObject();
        SharedPreferences prefs=getSharedPreferences("craftcv_native_resumes_v1",MODE_PRIVATE);
        String id=prefs.getString("current","");
        try{
            JSONArray all=new JSONArray(prefs.getString("resumes","[]"));
            for(int i=0;i<all.length();i++){
                JSONObject item=all.optJSONObject(i);
                if(item!=null&&id.equals(item.optString("id"))){resume=item;break;}
            }
            // A stale selected ID must not produce a blank document when saved CVs still exist.
            if(resume.length()==0 && all.length()>0){
                JSONObject fallback=all.optJSONObject(0);
                if(fallback!=null)resume=fallback;
            }
        }catch(Exception ignored){}
        style=Math.max(0,Math.min(11,resume.optInt("style",0)));
    }
    private void buildLayout(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xFFE9F0F1);
        setContentView(root);

        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(14),dp(10),dp(14),dp(10));
        header.setBackgroundColor(0xFF173C4C);
        root.addView(header);

        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titleRow);
        Button back=button("‹ Back",this::finish);
        titleRow.addView(back,new LinearLayout.LayoutParams(dp(110),dp(45)));
        TextView title=new TextView(this);
        title.setText("Your Finished CV");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null,android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(12),0,0,0);
        titleRow.addView(title,new LinearLayout.LayoutParams(0,dp(45),1));

        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(actions);
        savePdfButton=addAction(actions,"Save PDF",()->savePdf(false));
        addAction(actions,"Word",this::saveWord);
        sharePdfButton=addAction(actions,"Share",()->savePdf(true));
        savePdfButton.setEnabled(false);
        sharePdfButton.setEnabled(false);

        info=new TextView(this);
        info.setText("A4 document preview • Works offline");
        info.setTextColor(0xFF557A87);
        info.setTextSize(12);
        info.setPadding(dp(16),dp(13),dp(16),dp(10));
        root.addView(info);

        openSavedButton=button("Open last saved PDF / Word file",this::openSavedFile);
        openSavedButton.setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams openParams=new LinearLayout.LayoutParams(-1,dp(48));
        openParams.setMargins(dp(14),0,dp(14),dp(8));
        root.addView(openSavedButton,openParams);

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(false);
        root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        pages=new LinearLayout(this);
        pages.setOrientation(LinearLayout.VERTICAL);
        pages.setGravity(Gravity.CENTER_HORIZONTAL);
        pages.setPadding(dp(12),dp(5),dp(12),dp(25));
        scroller.addView(pages);
    }
    private Button addAction(LinearLayout row,String name,Runnable task){
        Button b=button(name,task);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(47),1);
        p.setMargins(dp(2),0,dp(2),0);
        row.addView(b,p);
        return b;
    }
    private void renderDocument(){
        if(info!=null)info.setText("Preparing your A4 CV...");
        pages.removeAllViews();
        addPreviewHint("Creating your CV preview. Your saved details are safe and you can return to editing.");

        // PdfRenderer + bitmap allocation can stall the main thread on older Android phones.
        // Render off the UI thread, and only attach completed bitmaps on the UI thread.
        new Thread(()->{
            java.util.ArrayList<Bitmap> bitmaps=new java.util.ArrayList<>();
            byte[] generated=null;
            int count=0;
            String failure=null;
            File temp=null;
            try{
                generated=ResumePdf.generate(resume,style);
                temp=File.createTempFile("craftcv-preview-", ".pdf",getCacheDir());
                try(FileOutputStream output=new FileOutputStream(temp)){
                    output.write(generated);
                }
                int displayWidth=Math.max(dp(220),getResources().getDisplayMetrics().widthPixels-dp(28));
                int targetWidth=Math.min(displayWidth,dp(620));
                try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(temp,ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer renderer=new PdfRenderer(fd)){
                    count=renderer.getPageCount();
                    for(int i=0;i<Math.min(count,3);i++){
                        try(PdfRenderer.Page page=renderer.openPage(i)){
                            double ratio=(double)page.getHeight()/Math.max(1,page.getWidth());
                            int width=Math.min(targetWidth,(int)Math.sqrt(900000.0/ratio));
                            width=Math.max(180,width);
                            int height=Math.max(1,(int)Math.ceil(width*ratio));
                            Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
                            bitmap.eraseColor(Color.WHITE);
                            page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            bitmaps.add(bitmap);
                        }
                    }
                }
                if(count==0)failure="Your CV did not contain any PDF pages.";
            }catch(Exception error){
                failure="Could not render the CV preview: "+String.valueOf(error.getMessage());
            }catch(OutOfMemoryError memory){
                failure="This phone could not display the CV preview. Try closing other apps.";
            }finally{
                if(temp!=null)temp.delete();
            }

            final byte[] readyPdf=generated;
            final int pageCount=count;
            final String errorMessage=failure;
            runOnUiThread(()->{
                if(isFinishing() || isDestroyed()){
                    for(Bitmap bitmap:bitmaps)bitmap.recycle();
                    return;
                }
                pages.removeAllViews();
                if(readyPdf!=null){
                    pdfBytes=readyPdf;
                    savePdfButton.setEnabled(true);
                    sharePdfButton.setEnabled(true);
                }
                for(int i=0;i<bitmaps.size();i++){
                    Bitmap bitmap=bitmaps.get(i);
                    ImageView image=new ImageView(this);
                    image.setImageBitmap(bitmap);
                    image.setContentDescription("Resume PDF page "+(i+1));
                    image.setAdjustViewBounds(true);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    image.setBackgroundColor(Color.WHITE);
                    image.setElevation(dp(2));
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
                    lp.bottomMargin=dp(13);
                    pages.addView(image,lp);
                }
                if(errorMessage!=null){
                    info.setText("Preview needs attention");
                    addPreviewHint(errorMessage);
                    if(readyPdf!=null)addPreviewHint("The full PDF is available. Tap Save PDF to download it.");
                }else{
                    info.setText("Your finished CV • "+pageCount+
                        (pageCount==1?" page":" pages")+" • Ready to save");
                }
                if(pageCount>bitmaps.size()){
                    addPreviewHint("Showing the first "+bitmaps.size()+" of "+pageCount+
                        " pages to save device memory. Save PDF to see the full document.");
                }
                if(bitmaps.isEmpty() && errorMessage==null){
                    addPreviewHint("No preview image was produced. Please return to editing and try again.");
                }
            });
        },"CraftCV-PDF-Preview").start();
    }
    private void addPreviewHint(String message){
        TextView warning=new TextView(this);
        warning.setText(message);
        warning.setTextSize(13);
        warning.setTextColor(0xFF315B65);
        warning.setPadding(dp(14),dp(18),dp(14),dp(18));
        warning.setBackgroundColor(Color.WHITE);
        pages.addView(warning,new LinearLayout.LayoutParams(-1,-2));
    }
    private String name(){
        String value=resume.optString("name","Resume").trim().replaceAll("[^a-zA-Z0-9_-]+","-");
        return value.isEmpty()?"Resume":value.substring(0,Math.min(55,value.length()));
    }
    private void savePdf(boolean share){
        if(pdfBytes==null){Toast.makeText(this,"PDF is not ready",Toast.LENGTH_LONG).show();return;}
        shareAfterSave=share;
        pendingBytes=pdfBytes;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE,"CraftCV-"+name()+".pdf");
        try{startActivityForResult(intent,SAVE_PDF);}
        catch(Exception e){Toast.makeText(this,"No document picker found",Toast.LENGTH_LONG).show();}
    }
    private void saveWord(){
        try{
            pendingBytes=DocxExport.create(resume);
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.putExtra(Intent.EXTRA_TITLE,"CraftCV-"+name()+".docx");
            startActivityForResult(intent,SAVE_DOCX);
        }catch(Exception e){Toast.makeText(this,"Unable to prepare Word document",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SAVE_PDF && request!=SAVE_DOCX)return;
        if(result==RESULT_OK && data!=null && data.getData()!=null && pendingBytes!=null){
            boolean saved=false;
            try(OutputStream output=getContentResolver().openOutputStream(data.getData())){
                if(output==null)throw new IllegalStateException("Cannot write document");
                output.write(pendingBytes);
                output.flush();
                saved=true;
            }catch(Exception e){Toast.makeText(this,"File save failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}
            if(saved){
                lastSavedUri=data.getData();
                lastSavedMime=request==SAVE_PDF?"application/pdf":
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                openSavedButton.setVisibility(android.view.View.VISIBLE);
                String check=verifySavedDocument(lastSavedUri,request==SAVE_PDF);
                if(check.equals("Verified")){
                    info.setText((request==SAVE_PDF?"PDF":"Word")+" saved and file format verified.");
                    Toast.makeText(this,"File saved. Tap Open saved file to check it.",Toast.LENGTH_LONG).show();
                }else{
                    info.setText("File saved, but verification could not complete: "+check);
                    Toast.makeText(this,"File saved. Try opening it to verify.",Toast.LENGTH_LONG).show();
                }
                if(request==SAVE_PDF){
                    savedPdfUri=data.getData();
                    if(shareAfterSave)sharePdf();
                }
            }
        }
        pendingBytes=null;shareAfterSave=false;
    }
    private String verifySavedDocument(Uri uri,boolean pdf){
        try(InputStream input=getContentResolver().openInputStream(uri)){
            if(input==null)return "cannot reopen saved file";
            if(pdf){
                byte[] prefix=new byte[5];
                int count=0;
                while(count<5){
                    int read=input.read(prefix,count,5-count);
                    if(read<0)break;
                    count+=read;
                }
                return count==5 && "%PDF-".equals(new String(prefix,StandardCharsets.US_ASCII))
                    ?"Verified":"invalid PDF header";
            }
            boolean types=false,document=false,relationships=false;
            try(ZipInputStream zip=new ZipInputStream(input)){
                ZipEntry entry;
                int visited=0;
                while((entry=zip.getNextEntry())!=null && visited++<80){
                    String name=entry.getName();
                    if("[Content_Types].xml".equals(name))types=true;
                    if("word/document.xml".equals(name)){
                        document=true;
                        if(entry.getSize()==0)return "empty Word document";
                    }
                    if("_rels/.rels".equals(name))relationships=true;
                    zip.closeEntry();
                }
            }
            return types&&document&&relationships?"Verified":"missing Word document components";
        }catch(Exception e){
            // Some document providers allow write but not immediate re-read.
            return "storage provider did not allow a readback";
        }
    }
    private void openSavedFile(){
        if(lastSavedUri==null || lastSavedMime==null){
            Toast.makeText(this,"Save a PDF or Word file first.",Toast.LENGTH_LONG).show();return;
        }
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(lastSavedUri,lastSavedMime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newUri(getContentResolver(),"CraftCV saved CV",lastSavedUri));
            startActivity(Intent.createChooser(intent,"Open saved CraftCV file"));
        }catch(Exception error){
            Toast.makeText(this,"No compatible document viewer is installed. File is still saved.",Toast.LENGTH_LONG).show();
        }
    }
    private void sharePdf(){
        if(savedPdfUri==null){savePdf(true);return;}
        try{
            Intent share=new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM,savedPdfUri);
            share.setClipData(ClipData.newUri(getContentResolver(),"CraftCV PDF",savedPdfUri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"Send your resume"));
        }catch(Exception e){Toast.makeText(this,"Sharing unavailable",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onDestroy(){
        super.onDestroy();
        // Temp file only; the user's exported document lives in their selected folder.
        if(previewFile!=null)previewFile.delete();
    }
}
