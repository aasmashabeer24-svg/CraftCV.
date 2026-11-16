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
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
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
    private static final int CREATE_PDF = 411;
    private static final int CREATE_DOCX = 412;
    private byte[] pendingDocx;
    private byte[] pendingPdf;
    private Uri lastSavedPdf;
    private final String[] fields = {"Full name", "Professional title", "Email", "Phone", "Location",
            "Professional summary", "Work experience", "Education", "Skills", "Projects"};
    private final String[] keys = {"name","role","email","phone","location","summary","experience","education","skills","projects"};
    private final EditText[] inputs = new EditText[keys.length];
    private final int[] colors = {0xFF194C5D,0xFF3C584D,0xFF774960,0xFF303D48,0xFF274054,0xFF354458,0xFF417C69,0xFFB05241,0xFF3E5068,0xFF317071,0xFF906D59,0xFF252525};
    private final String[] themes = {"Editorial","Modern","Creative","Minimal","Executive","Classic","Fresh","Bold","Academic","Compact","Elegant","Monochrome"};
    private int style = 0;
    private String currentId = "";
    private LinearLayout content;
    private TextView preview;
    private TextView status;
    private boolean loading = false;
    private final android.os.Handler saveHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable savePending=()->{if(!loading)save();};

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
        brand.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(brand);
        TextView subtitle=label("NATIVE RESUME STUDIO  •  WORKS OFFLINE",10,Color.rgb(74,128,125));
        subtitle.setLetterSpacing(.12f);content.addView(subtitle);
        TextView welcome=label("Create something unforgettable.",23,Color.rgb(27,59,70));
        welcome.setTypeface(null,android.graphics.Typeface.BOLD);welcome.setPadding(0,dp(25),0,dp(6));content.addView(welcome);
        TextView intro=label("Your CVs stay on this phone. Export a PDF anytime, even without an internet connection.",13,Color.rgb(104,126,134));
        intro.setPadding(0,0,0,dp(14));content.addView(intro);
        LinearLayout commands=new LinearLayout(this);
        commands.setOrientation(LinearLayout.HORIZONTAL);content.addView(commands);
        addCommand(commands,"My CVs",this::chooseResume);
        addCommand(commands,"New CV",()->createNew(true));
        addCommand(commands,"Save",this::save);
        addCommand(commands,"PDF",this::exportPDF);
        LinearLayout moreCommands=new LinearLayout(this);moreCommands.setOrientation(LinearLayout.HORIZONTAL);content.addView(moreCommands);
        addCommand(moreCommands,"Duplicate",this::duplicateResume);
        addCommand(moreCommands,"Delete",this::deleteResume);
        addCommand(moreCommands,"Share PDF",this::sharePdf);
        addCommand(moreCommands,"Cover letter",this::openCoverLetter);
        LinearLayout quickActions=new LinearLayout(this);quickActions.setOrientation(LinearLayout.HORIZONTAL);content.addView(quickActions);
        addCommand(quickActions,"Student starter",this::studentStarter);
        addCommand(quickActions,"Resume check",this::resumeCheck);
        addCommand(quickActions,"Surprise style",this::surpriseStyle);
        addCommand(quickActions,"Word DOCX",this::exportDocx);
        TextView choose=label("PICK YOUR DESIGN",11,Color.rgb(62,112,111));
        choose.setPadding(0,dp(23),0,dp(9));choose.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(choose);
        content.addView(label("12 color and typography styles. Several share the same basic layout.",11,Color.rgb(105,125,130)));
        for(int group=0;group<3;group++){
            LinearLayout themeRow=new LinearLayout(this);content.addView(themeRow);
            for(int i=group*4;i<(group+1)*4;i++){
                final int selected=i;
                Button button=action(themes[i],()->{style=selected;updatePreview();save();});
                button.setTextSize(10);
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[i]));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);
                p.setMargins(0,0,dp(4),dp(4));themeRow.addView(button,p);
            }
        }
        TextView title=label("YOUR DETAILS",11,Color.rgb(62,112,111));
        title.setTypeface(null,android.graphics.Typeface.BOLD);title.setPadding(0,dp(25),0,dp(8));content.addView(title);
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
            input.addTextChangedListener(new android.text.TextWatcher(){
                @Override public void beforeTextChanged(CharSequence value,int start,int count,int after){}
                @Override public void onTextChanged(CharSequence value,int start,int before,int count){}
                @Override public void afterTextChanged(android.text.Editable value){
                    if(loading)return;
                    updatePreview();
                    saveHandler.removeCallbacks(savePending);
                    saveHandler.postDelayed(savePending,1100);
                }
            });
        }
        status=label("",12,Color.rgb(63,125,104));
        status.setPadding(0,dp(15),0,dp(10));content.addView(status);
        Button previewButton=action("Update live preview",this::updatePreview);
        content.addView(previewButton,new LinearLayout.LayoutParams(-1,dp(49)));
        TextView previewHeading=label("LIVE PREVIEW",11,Color.rgb(62,112,111));
        previewHeading.setPadding(0,dp(22),0,dp(12));previewHeading.setTypeface(null,android.graphics.Typeface.BOLD);
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
        saveHandler.removeCallbacks(savePending);
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
    private void studentStarter(){
        new AlertDialog.Builder(this).setTitle("New student / fresher CV?")
            .setMessage("Create a blank first-job CV without fake work history. Unsaved changes in the current CV will be replaced.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Create",(dialog,which)->{
                saveHandler.removeCallbacks(savePending);
                createNew(false);
                inputs[1].setText("Student / Fresh Graduate");
                inputs[6].setText("");
                inputs[7].setText("");
                inputs[8].setText("");
                status.setText("Add education, projects and genuine skills. You do not need job experience.");
                save();
            }).show();
    }
    private void resumeCheck(){
        java.util.ArrayList<String> suggestions=new java.util.ArrayList<>();
        if(value(0,"").isEmpty())suggestions.add("Add your full name.");
        if(value(1,"").isEmpty())suggestions.add("Add a professional title or target role.");
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(value(2,"")).matches())suggestions.add("Add a valid email.");
        if(value(5,"").length()<45)suggestions.add("Write a short, factual summary (at least 45 characters).");
        if(value(7,"").isEmpty())suggestions.add("Add your education or relevant qualification.");
        if(value(8,"").isEmpty())suggestions.add("List skills relevant to the job.");
        if(value(6,"").isEmpty() && value(9,"").isEmpty())suggestions.add("Include an internship, project or real experience if available.");
        String result=suggestions.isEmpty()?"All basic CV completeness checks passed. Review your claims and PDF layout before applying.":"Suggested improvements:\n\n• "+android.text.TextUtils.join("\n• ",suggestions);
        new AlertDialog.Builder(this).setTitle("Resume completeness (not an ATS guarantee)")
            .setMessage(result).setPositiveButton("OK",null).show();
    }
    private void surpriseStyle(){
        int next=(int)(Math.random()*themes.length);
        if(next==style)next=(next+1)%themes.length;
        style=next;updatePreview();save();
        Toast.makeText(this,"New style: "+themes[style],Toast.LENGTH_SHORT).show();
    }
    private void openCoverLetter(){
        Intent intent=new Intent(this,CoverLetterActivity.class);
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_NAME,value(0,""));
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_ROLE,value(1,""));
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_SUMMARY,value(5,""));
        intent.putExtra(CoverLetterActivity.EXTRA_RESUME_SKILLS,value(8,""));
        startActivity(intent);
    }
    private void duplicateResume(){
        save();
        JSONObject copy=snapshot();
        currentId="native-"+System.currentTimeMillis()+"-copy";
        inputs[0].setText(copy.optString("name","")+" (Copy)");
        save();
        status.setText("Duplicated. Your original remains saved.");
    }
    private void deleteResume(){
        JSONArray items=readItems();
        if(items.length()<2){Toast.makeText(this,"Keep one CV. Create another before deleting.",Toast.LENGTH_LONG).show();return;}
        new AlertDialog.Builder(this).setTitle("Delete this resume?")
            .setMessage("This removes the selected local resume from this phone. Keep a PDF backup.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Delete",(dialog,which)->{
                try {
                    JSONArray remaining=new JSONArray();
                    for(int i=0;i<items.length();i++){
                        JSONObject item=items.optJSONObject(i);
                        if(item!=null && !currentId.equals(item.optString("id")))remaining.put(item);
                    }
                    preferences().edit().putString(KEY_ITEMS,remaining.toString()).apply();
                    load(remaining.getJSONObject(0));
                    status.setText("Local resume deleted.");
                }catch(Exception e){status.setText("Could not delete resume.");}
            }).show();
    }
    private void sharePdf(){
        if(lastSavedPdf==null){Toast.makeText(this,"Export and save a PDF first, then tap Share PDF.",Toast.LENGTH_LONG).show();return;}
        try{
            Intent share=new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM,lastSavedPdf);
            share.setClipData(android.content.ClipData.newUri(getContentResolver(),"CraftCV PDF",lastSavedPdf));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"Share your CraftCV resume"));
        }catch(Exception e){Toast.makeText(this,"Cannot share this PDF. Try saving a new copy.",Toast.LENGTH_LONG).show();}
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
        preview.setTextSize(style==2||style==7?16:style==9?13:14);
        preview.setTypeface(null,style==4||style==5?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
    }
    private String value(int index,String fallback){
        String text=inputs[index].getText().toString().trim();
        return text.isEmpty()?fallback:text;
    }
    private void exportDocx(){
        if(value(0,"").trim().isEmpty()){
            Toast.makeText(this,"Add your name before exporting",Toast.LENGTH_LONG).show();return;
        }
        save();
        try{
            pendingDocx=DocxExport.create(snapshot());
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.putExtra(Intent.EXTRA_TITLE,"CraftCV-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".docx");
            startActivityForResult(intent,CREATE_DOCX);
        }catch(Exception e){pendingDocx=null;status.setText("Word export failed: "+e.getMessage());}
    }
    private java.util.ArrayList<String> wrapPdf(String raw,Paint paint,float width){
        java.util.ArrayList<String> output=new java.util.ArrayList<>();
        for(String paragraph:raw.split("\\n",-1)){
            if(paragraph.trim().isEmpty()){output.add("");continue;}
            StringBuilder line=new StringBuilder();
            for(String word:paragraph.trim().split("\\s+")){
                if(paint.measureText(word)>width){
                    if(line.length()>0){output.add(line.toString());line.setLength(0);}
                    for(int j=0;j<word.length();j++){
                        String next=line.toString()+word.charAt(j);
                        if(paint.measureText(next)>width && line.length()>0){output.add(line.toString());line.setLength(0);}
                        line.append(word.charAt(j));
                    }
                }else{
                    String next=line.length()==0?word:line+" "+word;
                    if(paint.measureText(next)>width && line.length()>0){output.add(line.toString());line.setLength(0);}
                    if(line.length()>0)line.append(' ');
                    line.append(word);
                }
            }
            if(line.length()>0)output.add(line.toString());
        }
        return output;
    }
    private void exportPDF(){
        if(value(0,"").trim().isEmpty()){Toast.makeText(this,"Enter your name before exporting",Toast.LENGTH_LONG).show();return;}
        save();
        PdfDocument pdf=new PdfDocument();
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNo=1;
        PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());
        Canvas canvas=page.getCanvas();
        int y=57;
        int primary=colors[style];
        if(style==2||style==4||style==7){
            paint.setColor(primary);
            canvas.drawRect(0,0,595,27,paint);
        }
        paint.setTypeface(android.graphics.Typeface.create(style==0||style==5||style==10?"serif":"sans-serif",android.graphics.Typeface.BOLD));
        paint.setTextSize(style==7?28:style==9?20:24);paint.setColor(style==11?Color.BLACK:primary);
        for(String line:wrapPdf(value(0,"Your Name"),paint,500)){canvas.drawText(line,45,y,paint);y+=30;}
        paint.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
        paint.setTextSize(13);
        for(String line:wrapPdf(value(1,""),paint,500)){canvas.drawText(line,45,y,paint);y+=18;}
        paint.setTextSize(10);paint.setColor(Color.DKGRAY);
        for(String line:wrapPdf(value(2,"")+"  •  "+value(3,"")+"  •  "+value(4,""),paint,500)){canvas.drawText(line,45,y,paint);y+=15;}
        y+=13;paint.setColor(primary);paint.setStrokeWidth(1.4f);canvas.drawLine(45,y,550,y,paint);y+=28;
        for(int i=5;i<keys.length;i++){
            String contents=value(i,"").trim();if(contents.isEmpty())continue;
            if(y>730){pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=58;}
            paint.setColor(style==11?Color.BLACK:primary);paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(style==9?11:12);
            canvas.drawText(fields[i].toUpperCase(Locale.ROOT),45,y,paint);y+=20;
            paint.setTypeface(android.graphics.Typeface.DEFAULT);paint.setColor(Color.rgb(50,55,60));paint.setTextSize(10);
            for(String line:wrapPdf(contents,paint,500)){
                if(y>780){pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=58;}
                if(!line.isEmpty())canvas.drawText(line,45,y,paint);
                y+=line.isEmpty()?9:(style==9?14:16);
            }
            y+=style==9?8:15;
        }
        pdf.finishPage(page);
        try {
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            pdf.writeTo(buffer);
            pendingPdf=buffer.toByteArray();
            Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            save.addCategory(Intent.CATEGORY_OPENABLE);
            save.setType("application/pdf");
            save.putExtra(Intent.EXTRA_TITLE,"CraftCV-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".pdf");
            startActivityForResult(save,CREATE_PDF);
        }catch(Exception e){pendingPdf=null;status.setText("PDF export failed: "+e.getMessage());}
        finally {pdf.close();}
    }
    @Override protected void onPause(){
        saveHandler.removeCallbacks(savePending);
        save();
        super.onPause();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==CREATE_DOCX){
            if(resultCode==RESULT_OK && data!=null && data.getData()!=null && pendingDocx!=null){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                    if(out==null)throw new IllegalStateException("Could not open document");
                    out.write(pendingDocx);
                    status.setText("Editable Word DOCX saved to selected location.");
                }catch(Exception e){status.setText("Word save failed.");}
            }
            pendingDocx=null;
            return;
        }
        if(requestCode!=CREATE_PDF)return;
        if(resultCode==RESULT_OK && data!=null && data.getData()!=null && pendingPdf!=null){
            try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){
                if(stream==null)throw new IllegalStateException("No file output stream");
                stream.write(pendingPdf);
                lastSavedPdf=data.getData();
                status.setText("PDF saved. Tap Share PDF to send it to WhatsApp or email.");
                Toast.makeText(this,"Resume PDF saved",Toast.LENGTH_LONG).show();
            }catch(Exception e){status.setText("PDF save failed: "+e.getMessage());}
        }else{status.setText("PDF save canceled.");}
        pendingPdf=null;
    }
    private String fit(String s,int max){return s.length()>max?s.substring(0,max-1)+"…":s;}
}
