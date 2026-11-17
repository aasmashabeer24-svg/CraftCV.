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
        Button back=button("‹ Edit CV",this::finish);
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
        addAction(actions,"Save PDF",()->savePdf(false));
        addAction(actions,"Word",this::saveWord);
        addAction(actions,"Share",()->savePdf(true));

        info=new TextView(this);
        info.setText("A4 document preview • Works offline");
        info.setTextColor(0xFF557A87);
        info.setTextSize(12);
        info.setPadding(dp(16),dp(13),dp(16),dp(10));
        root.addView(info);

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(false);
        root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        pages=new LinearLayout(this);
        pages.setOrientation(LinearLayout.VERTICAL);
        pages.setGravity(Gravity.CENTER_HORIZONTAL);
        pages.setPadding(dp(12),dp(5),dp(12),dp(25));
        scroller.addView(pages);
    }
    private void addAction(LinearLayout row,String name,Runnable task){
        Button b=button(name,task);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(47),1);
        p.setMargins(dp(2),0,dp(2),0);
        row.addView(b,p);
    }
    private void renderDocument(){
        try {
            pdfBytes=ResumePdf.generate(resume,style);
            previewFile=File.createTempFile("craftcv-preview-", ".pdf",getCacheDir());
            try(FileOutputStream file=new FileOutputStream(previewFile)){file.write(pdfBytes);}
            int width=getResources().getDisplayMetrics().widthPixels-dp(28);
            try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(previewFile,ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer=new PdfRenderer(fd)){
                int count=renderer.getPageCount();
                info.setText("Your CV • "+count+(count==1?" page":" pages")+" • A4 PDF • Ready to save");
                // Limit displayed pages to keep bitmap memory controlled on older phones.
                int shown=Math.min(count,8);
                for(int i=0;i<shown;i++){
                    try(PdfRenderer.Page page=renderer.openPage(i)){
                        int h=(int)Math.ceil(width*((double)page.getHeight()/page.getWidth()));
                        Bitmap bitmap=Bitmap.createBitmap(width,h,Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(Color.WHITE);
                        page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        ImageView image=new ImageView(this);
                        image.setImageBitmap(bitmap);
                        image.setAdjustViewBounds(true);
                        image.setBackgroundColor(Color.WHITE);
                        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        image.setElevation(dp(3));
                        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,h);
                        lp.setMargins(0,0,0,dp(16));
                        pages.addView(image,lp);
                    }
                }
                if(count>shown){
                    TextView warning=new TextView(this);
                    warning.setText("Showing the first "+shown+" pages. Saved PDFs contain all "+count+" pages.");
                    warning.setPadding(dp(12),dp(16),dp(12),dp(16));
                    pages.addView(warning);
                }
            }
        }catch(Exception e){
            info.setText("Could not prepare preview: "+e.getMessage());
            Toast.makeText(this,"Resume preview failed",Toast.LENGTH_LONG).show();
        }
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
                Toast.makeText(this,request==SAVE_PDF?"PDF saved":"Word document saved",Toast.LENGTH_LONG).show();
                if(request==SAVE_PDF){
                    savedPdfUri=data.getData();
                    if(shareAfterSave)sharePdf();
                }
            }
        }
        pendingBytes=null;shareAfterSave=false;
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
