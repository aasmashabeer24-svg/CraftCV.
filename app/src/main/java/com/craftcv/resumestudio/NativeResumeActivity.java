package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Offline-first Android resume studio. This screen does not load AppDeploy.
 * Records and templates live in app-private storage; no login or tracking.
 */
public final class NativeResumeActivity extends Activity {
    private static final String PREFS = "craftcv_native_resumes_v1";
    private static final String KEY_ITEMS = "resumes";
    private static final String KEY_CURRENT = "current";
    private final String[] fields = {"Full name", "Professional title", "Email", "Phone", "Location",
            "Professional summary", "Work experience", "Education", "Skills", "Projects"};
    private final String[] keys = {"name","role","email","phone","location","summary","experience","education","skills","projects"};
    private final EditText[] inputs = new EditText[keys.length];
    private final int[] colors = {Color.rgb(25,76,93), Color.rgb(60,88,77), Color.rgb(119,73,96)};
    private final String[] themes = {"Editorial", "Modern", "Creative"};
    private int style = 0;
    private String currentId = "";
    private LinearLayout content;
    private TextView preview;
    private TextView status;
    private boolean loading = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17,44,60));
        getWindow().setNavigationBarColor(Color.rgb(17,44,60));
        buildUI();
        JSONObject initial = null;
        try {
            JSONArray array = readItems();
            String wanted = preferences().getString(KEY_CURRENT,"");
            for(int i=0;i<array.length();i++) {
                JSONObject item=array.optJSONObject(i);
                if(item!=null && wanted.equals(item.optString("id"))) { initial=item; break; }
            }
            if(initial==null && array.length()>0) initial=array.optJSONObject(0);
        } catch(Exception ignored) {}
        if(initial!=null) load(initial);
        else createNew(false);
    }

    private SharedPreferences preferences() { return getSharedPreferences(PREFS,MODE_PRIVATE); }
    private JSONArray readItems() {
        try {return new JSONArray(preferences().getString(KEY_ITEMS,"[]"));}
        catch(Exception ignored) {return new JSONArray();}
    }
    private int dp(int n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int size,int color) {
        TextView view=new TextView(this);
        view.setText(value);view.setTextSize(size);view.setTextColor(color);
        return view;
    }
    private Button action(String name, Runnable command) {
        Button b=new Button(this);
        b.setText(name);b.setAllCaps(false);b.setTextSize(12);
        b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(31,83,99)));
        b.setOnClickListener(v->command.run());
        return b;
    }
    private void buildUI() {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244,248,248));
        setContentView(scroll);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18),dp(18),dp(18),dp(38));
        scroll.addView(content);
        TextView brand=label("CraftCV",29,Color.rgb(22,57,72));
        brand.setTypeface(null,1);content.addView(brand);
        TextView subtitle=label("NATIVE RESUME STUDIO  •  WORKS OFFLINE",10,Color.rgb(74,128,125));
        subtitle.setLetterSpacing(.12f);content.addView(subtitle);
        TextView welcome=label("Create something unforgettable.",23,Color.rgb(27,59,70));
        welcome.setTypeface(null,1);welcome.setPadding(0,dp(25),0,dp(6));content.addView(welcome);
        TextView intro=label("Your CVs stay on this phone. Export a PDF anytime, even without an internet connection.",13,Color.rgb(104,126,134));
        intro.setPadding(0,0,0,dp(14));content.addView(intro);
        LinearLayout commands=new LinearLayout(this);
        commands.setOrientation(LinearLayout.HORIZONTAL);content.addView(commands);
        addCommand(commands,"My CVs",this::chooseResume);
        addCommand(commands,"New CV",()->createNew(true));
        addCommand(commands,"Save",this::save);
        addCommand(commands,"PDF",this::exportPDF);
        TextView choose=label("PICK YOUR DESIGN",11,Color.rgb(62,112,111));
        choose.setPadding(0,dp(23),0,dp(9));choose.setTypeface(null,1);content.addView(choose);
        LinearLayout themeRow=new LinearLayout(this);content.addView(themeRow);
        for(int i=0;i<themes.length;i++){
            final int selected=i;
            Button button=action(themes[i],()->{style=selected;updatePreview();save();});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(49),1);
            p.setMargins(0,0,dp(5),0);themeRow.addView(button,p);
        }
        TextView title=label("YOUR DETAILS",11,Color.rgb(62,112,111));
        title.setTypeface(null,1);title.setPadding(0,dp(25),0,dp(8));content.addView(title);
        for(int i=0;i<keys.length;i++) {
            TextView fieldLabel=label(fields[i],12,Color.rgb(47,79,90));
            fieldLabel.setPadding(0,dp(13),0,0);content.addView(fieldLabel);
            EditText input=new EditText(this);
            input.setTextSize(14);input.setSingleLine(i<5);
            input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(177,204,205)));
            if(i>=5){input.setMinLines(3);input.setGravity(Gravity.TOP);}
            if(i==2) input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            if(i==3) input.setInputType(InputType.TYPE_CLASS_PHONE);
            input.setHint(i>=6 ? "One achievement or detail per line" : fields[i]);
            inputs[i]=input;content.addView(input);
        }
        status=label("",12,Color.rgb(63,125,104));
        status.setPadding(0,dp(15),0,dp(10));content.addView(status);
        Button previewButton=action("Update live preview",this::updatePreview);
        content.addView(previewButton,new LinearLayout.LayoutParams(-1,dp(49)));
        TextView previewHeading=label("LIVE PREVIEW",11,Color.rgb(62,112,111));
        previewHeading.setPadding(0,dp(22),0,dp(12));previewHeading.setTypeface(null,1);
        content.addView(previewHeading);
        preview=label("",14,Color.rgb(33,55,68));
        preview.setPadding(dp(20),dp(24),dp(20),dp(24));
        preview.setBackgroundColor(Color.WHITE);content.addView(preview);
        Button advanced=action("Open advanced web studio (internet)",()->{
            startActivity(new Intent(this,MainActivity.class));
        });
        LinearLayout.LayoutParams advancedParams=new LinearLayout.LayoutParams(-1,dp(52));
        advancedParams.setMargins(0,dp(20),0,0);content.addView(advanced,advancedParams);
        TextView info=label("Advanced AI, cloud accounts and the full template gallery are still available through the separate online studio. The native editor above works without it.",12,Color.rgb(114,135,141));
        info.setPadding(0,dp(9),0,0);content.addView(info);
    }
    private void addCommand(LinearLayout row,String title,Runnable callback) {
        Button b=action(title,callback);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(49),1);
        params.setMargins(0,0,dp(5),0);
        row.addView(b,params);
    }
    private JSONObject snapshot() {
        JSONObject item=new JSONObject();
        try {
            item.put("id",currentId);item.put("style",style);
            for(int i=0;i<keys.length;i++)item.put(keys[i],inputs[i].getText().toString());
        } catch(Exception ignored){}
        return item;
    }
    private void load(JSONObject item) {
        loading=true;
        currentId=item.optString("id");
        style=Math.max(0,Math.min(themes.length-1,item.optInt("style",0)));
        for(int i=0;i<keys.length;i++)inputs[i].setText(item.optString(keys[i],""));
        loading=false;updatePreview();
        preferences().edit().putString(KEY_CURRENT,currentId).apply();
    }
    private void createNew(boolean confirm) {
        if(confirm){new AlertDialog.Builder(this).setMessage("Start a new resume? Unsaved changes will be lost.").setPositiveButton("Create",(d,w)->createNew(false)).setNegativeButton("Cancel",null).show();return;}
        currentId="native-"+System.currentTimeMillis();
        style=0;for(EditText input:inputs)input.setText("");
        updatePreview();status.setText("New resume created. Tap Save when ready.");
    }
    private void save() {
        if(loading)return;
        try {
            JSONArray old=readItems(),next=new JSONArray();
            for(int i=0;i<old.length();i++){
                JSONObject item=old.optJSONObject(i);
                if(item!=null && !currentId.equals(item.optString("id")))next.put(item);
            }
            next.put(snapshot());
            preferences().edit().putString(KEY_ITEMS,next.toString()).putString(KEY_CURRENT,currentId).apply();
            status.setText("Saved privately on this device.");
            updatePreview();
        }catch(Exception e){status.setText("Could not save resume.");}
    }
    private void chooseResume(){
        JSONArray items=readItems();
        if(items.length()==0){Toast.makeText(this,"Save your first CV to see it here",Toast.LENGTH_SHORT).show();return;}
        String[] names=new String[items.length()];
        for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);
            names[i]=item!=null ? item.optString("name","Untitled CV")+" — "+item.optString("role","") : "Resume";
        }
        new AlertDialog.Builder(this).setTitle("My saved resumes").setItems(names,(dialog,which)->{
            JSONObject selected=items.optJSONObject(which);
            if(selected!=null)load(selected);
        }).setPositiveButton("Done",null).show();
    }
    private void updatePreview(){
        if(preview==null)return;
        StringBuilder b=new StringBuilder();
        b.append(value(0,"Your Name").toUpperCase(Locale.ROOT)).append("\n");
        b.append(value(1,"Your Professional Title")).append("\n");
        b.append(value(2,"")).append("  ").append(value(3,"")).append("\n");
        for(int i=5;i<keys.length;i++){
            String line=value(i,"").trim();
            if(line.isEmpty())continue;
            b.append("\n").append(fields[i].toUpperCase(Locale.ROOT)).append("\n");
            b.append(line).append("\n");
        }
        preview.setText(b.toString());
        preview.setTextColor(colors[style]);
        preview.setTextSize(style==2?15:14);
    }
    private String value(int index,String fallback){
        String text=inputs[index].getText().toString().trim();
        return text.isEmpty()?fallback:text;
    }
    private void exportPDF(){
        if(value(0,"").trim().isEmpty()){Toast.makeText(this,"Enter your name before exporting",Toast.LENGTH_LONG).show();return;}
        save();
        PdfDocument pdf=new PdfDocument();
        Paint paint=new Paint(3);
        int pageNo=1;
        PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());
        Canvas canvas=page.getCanvas();
        int y=55;
        paint.setColor(colors[style]);paint.setTextSize(23);paint.setFakeBoldText(true);
        canvas.drawText(fit(value(0,"Your Name"),38),45,y,paint);y+=27;
        paint.setTextSize(13);paint.setFakeBoldText(false);
        canvas.drawText(fit(value(1,""),72),45,y,paint);y+=19;
        paint.setColor(Color.DKGRAY);paint.setTextSize(10);
        canvas.drawText(fit(value(2,"")+"   "+value(3,"")+"   "+value(4,""),95),45,y,paint);y+=34;
        for(int i=5;i<keys.length;i++){
            String contents=value(i,"").trim();if(contents.isEmpty())continue;
            if(y>744){pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=55;}
            paint.setColor(colors[style]);paint.setFakeBoldText(true);paint.setTextSize(12);
            canvas.drawText(fields[i].toUpperCase(Locale.ROOT),45,y,paint);y+=20;
            paint.setColor(Color.DKGRAY);paint.setFakeBoldText(false);paint.setTextSize(10);
            for(String para:contents.split("\n")){
                String line=para.trim();
                while(!line.isEmpty()){
                    int end=Math.min(line.length(),90);
                    if(end<line.length()){
                        int gap=line.lastIndexOf(' ',end);
                        if(gap>20)end=gap;
                    }
                    if(y>780){pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=55;}
                    canvas.drawText(line.substring(0,end),45,y,paint);y+=16;
                    line=line.substring(end).trim();
                }
            }
            y+=15;
        }
        pdf.finishPage(page);
        try {
            File folder=getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if(folder==null)throw new IllegalStateException("Documents storage unavailable");
            if(!folder.exists()&&!folder.mkdirs())throw new IllegalStateException("Cannot create documents folder");
            String filename="CraftCV-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".pdf";
            File out=new File(folder,filename);
            try(FileOutputStream stream=new FileOutputStream(out)){pdf.writeTo(stream);}
            status.setText("PDF saved: "+filename+" (app documents)");
            new AlertDialog.Builder(this).setTitle("PDF saved").setMessage("Saved to app documents. To share, open the Android file manager or use a compatible file-sharing feature in a later update.").setPositiveButton("OK",null).show();
        }catch(Exception e){status.setText("PDF export failed: "+e.getMessage());}
        finally {pdf.close();}
    }
    private String fit(String s,int max){return s.length()>max?s.substring(0,max-1)+"…":s;}
}
