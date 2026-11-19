package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Optional online AI assistance inside the native Android app.
 * Only requested resume text (no name, email, phone, location, or photo) is sent
 * over HTTPS when the user explicitly taps Generate. Offline editor never needs AI.
 */
public final class AiAssistantActivity extends Activity {
    private static final String ENDPOINT="https://craftcv-hees13.v2.appdeploy.ai/api/resume-assistant";
    private static final String PREFS="craftcv_native_resumes_v1";
    private static final String[] MODES={"Rewrite my summary","Improve experience bullets","Compare with a job description"};
    private static final String[] API_MODES={"summary","bullets","review"};
    private static final int INK=0xFF203E49, TEAL=0xFF226E64, MUTED=0xFF718995;
    private EditText role, summary, skills, experience, jobDescription;
    private Spinner mode;
    private TextView response, status;
    private Button generate, useSummary, copy;
    private String lastResult="";
    private boolean working=false;
    private JSONObject currentResume=new JSONObject();

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(12));
        return d;
    }
    private TextView label(String value,int size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(INK);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(String caption,boolean primary,View.OnClickListener click){
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);b.setTextSize(13);
        b.setTextColor(primary?Color.WHITE:TEAL);
        b.setBackgroundTintList(null);b.setBackground(shape(primary?TEAL:0xFFEAF4F1));
        b.setOnClickListener(click);
        return b;
    }
    private EditText field(LinearLayout parent,String name,String value,boolean tall){
        TextView heading=label(name,13,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(16);hp.bottomMargin=dp(5);
        parent.addView(heading,hp);
        EditText e=new EditText(this);
        e.setText(value);e.setTextSize(14);e.setTextColor(INK);
        e.setPadding(dp(12),dp(12),dp(12),dp(12));e.setBackground(shape(Color.WHITE));
        if(tall){
            e.setMinLines(3);e.setGravity(Gravity.TOP);
            e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }else{
            e.setSingleLine(true);
        }
        parent.addView(e,new LinearLayout.LayoutParams(-1,-2));
        return e;
    }
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private void loadResume(){
        try{
            JSONArray rows=new JSONArray(prefs().getString("resumes","[]"));
            String id=prefs().getString("current","");
            JSONObject first=rows.optJSONObject(0);
            currentResume=first==null?new JSONObject():first;
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.optJSONObject(i);
                if(row!=null&&id.equals(row.optString("id"))){currentResume=row;break;}
            }
        }catch(Exception ignored){currentResume=new JSONObject();}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        loadResume();
        build();
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(0xFFF4F8F8);
        setContentView(scroll);
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18),dp(17),dp(18),dp(32));
        scroll.addView(page);
        Button back=button("‹ Dashboard",false,v->finish());
        page.addView(back,new LinearLayout.LayoutParams(dp(152),dp(43)));
        TextView heading=label("AI Resume Assistant",27,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
        hp.topMargin=dp(20);page.addView(heading,hp);
        TextView intro=label("Improve your summary, experience wording and job matching with online suggestions.",13,false);
        intro.setTextColor(MUTED);intro.setPadding(0,dp(7),0,dp(14));page.addView(intro);

        LinearLayout note=new LinearLayout(this);note.setPadding(dp(14),dp(12),dp(14),dp(12));
        note.setBackground(shape(0xFFE4F2ED));
        page.addView(note,new LinearLayout.LayoutParams(-1,-2));
        TextView privacy=label("INTERNET REQUIRED • PRIVATE UNTIL YOU TAP GENERATE\nOnly the role, summary, skills, experience and optional job description below are sent to the online AI service. Your name, email, phone and photo are never sent. Review suggestions before using them.",11,false);
        privacy.setTextColor(0xFF316558);
        note.addView(privacy);

        TextView action=label("What should AI help with?",13,true);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(20);
        page.addView(action,ap);
        mode=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,MODES);
        mode.setAdapter(adapter);
        page.addView(mode,new LinearLayout.LayoutParams(-1,dp(48)));
        role=field(page,"Target job title",currentResume.optString("role",""),false);
        summary=field(page,"Current summary",currentResume.optString("summary",""),true);
        skills=field(page,"Skills",currentResume.optString("skills",""),true);
        experience=field(page,"Experience notes",currentResume.optString("experience",""),true);
        jobDescription=field(page,"Job description (required for comparison)", "",true);
        generate=button("Generate AI suggestions",true,v->generate());
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(53));
        gp.topMargin=dp(19);page.addView(generate,gp);
        status=label("AI is optional. Your offline CV builder remains available.",12,false);
        status.setTextColor(MUTED);status.setPadding(0,dp(11),0,dp(8));
        page.addView(status);
        response=label("",14,false);
        response.setLineSpacing(dp(3),1.1f);response.setTextIsSelectable(true);
        response.setPadding(dp(15),dp(15),dp(15),dp(15));
        response.setBackground(shape(Color.WHITE));
        response.setVisibility(View.GONE);
        page.addView(response,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ac=new LinearLayout.LayoutParams(-1,dp(53));
        ac.topMargin=dp(10);page.addView(actions,ac);
        copy=button("Copy",false,v->copyText());
        useSummary=button("Use as CV summary",true,v->confirmApply());
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(51),1);
        cp.rightMargin=dp(7);actions.addView(copy,cp);
        actions.addView(useSummary,new LinearLayout.LayoutParams(0,dp(51),2));
        copy.setVisibility(View.GONE);
        useSummary.setVisibility(View.GONE);
    }
    private static String limited(String s,int limit){
        s=s==null?"":s.trim();
        return s.length()>limit?s.substring(0,limit):s;
    }
    private static String readResponse(InputStream input)throws Exception{
        StringBuilder text=new StringBuilder();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
            char[] chunk=new char[2048];int read;
            while((read=reader.read(chunk))!=-1){
                text.append(chunk,0,read);
                if(text.length()>65536)throw new IllegalStateException("Response too large");
            }
        }
        return text.toString();
    }
    private void generate(){
        if(working)return;
        if(limited(role.getText().toString(),150).isEmpty()){
            role.setError("Enter the job title first");
            role.requestFocus();
            return;
        }
        int selected=mode.getSelectedItemPosition();
        final String apiMode=API_MODES[Math.max(0,Math.min(selected,API_MODES.length-1))];
        if("review".equals(apiMode) && limited(jobDescription.getText().toString(),5000).isEmpty()){
            jobDescription.setError("Paste the job description to compare your CV");
            jobDescription.requestFocus();
            return;
        }
        if("bullets".equals(apiMode) && limited(experience.getText().toString(),1400).isEmpty()){
            experience.setError("Add real work experience notes first");
            experience.requestFocus();
            return;
        }
        JSONObject request=new JSONObject();
        try{
            request.put("mode",apiMode);
            request.put("role",limited(role.getText().toString(),150));
            request.put("summary",limited(summary.getText().toString(),2500));
            request.put("skills",limited(skills.getText().toString(),1500));
            request.put("jobDescription",limited(jobDescription.getText().toString(),5000));
            JSONArray jobs=new JSONArray();
            String work=limited(experience.getText().toString(),1400);
            if(!work.isEmpty()){
                JSONObject job=new JSONObject();job.put("title","Experience");job.put("org","");
                job.put("details",work);jobs.put(job);
            }
            request.put("experience",jobs);
        }catch(Exception e){status.setText("Could not prepare your request.");return;}
        working=true;generate.setEnabled(false);
        lastResult="";response.setVisibility(View.GONE);
        copy.setVisibility(View.GONE);useSummary.setVisibility(View.GONE);
        status.setText("Sending selected resume text securely. Looking for suggestions…");
        new Thread(()->{
            HttpURLConnection connection=null;String result=null;String err=null;
            try{
                connection=(HttpURLConnection)new URL(ENDPOINT).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(12000);connection.setReadTimeout(25000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
                connection.setRequestProperty("Accept","application/json");
                byte[] body=request.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                try(OutputStream output=connection.getOutputStream()){output.write(body);}
                int code=connection.getResponseCode();
                InputStream stream=code>=200&&code<300?connection.getInputStream():connection.getErrorStream();
                String raw=stream==null?"":readResponse(stream);
                if(code<200||code>=300){
                    String serverMessage="";
                    try{
                        JSONObject errorBody=new JSONObject(raw);
                        serverMessage=errorBody.optString("error",errorBody.optString("message",""));
                    }catch(Exception ignored){}
                    if(code==429)throw new IllegalStateException(serverMessage.isEmpty()?"The AI daily request limit is reached or the service is busy. Please retry later.":serverMessage);
                    if(code>=500)throw new IllegalStateException("AI is temporarily unavailable. Your CV data was not changed. Please retry later.");
                    if(code==400 && !serverMessage.isEmpty())throw new IllegalStateException(serverMessage);
                    throw new IllegalStateException("Online AI service unavailable (HTTP "+code+")");
                }
                result=limited(new JSONObject(raw).optString("text",""),24000);
                if(result.isEmpty())throw new IllegalStateException("No suggestions returned");
            }catch(Exception e){err=e.getMessage()==null?"Connection failed":e.getMessage();}
            finally{if(connection!=null)connection.disconnect();}
            final String answer=result,error=err;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                working=false;generate.setEnabled(true);
                if(answer!=null){
                    lastResult=answer;
                    status.setText("Suggestions ready. Check every statement before applying.");
                    response.setText(answer);response.setVisibility(View.VISIBLE);
                    copy.setVisibility(View.VISIBLE);
                    useSummary.setVisibility("summary".equals(apiMode)?View.VISIBLE:View.GONE);
                }else{
                    status.setText("AI could not respond: "+error+". Your CV data was not changed.");
                }
            });
        },"CraftCV-AI-Request").start();
    }
    private void copyText(){
        if(lastResult.isEmpty())return;
        ClipboardManager clip=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if(clip!=null){
            clip.setPrimaryClip(ClipData.newPlainText("CraftCV AI suggestions",lastResult));
            Toast.makeText(this,"Suggestions copied",Toast.LENGTH_SHORT).show();
        }
    }
    private void confirmApply(){
        if(lastResult.isEmpty())return;
        new AlertDialog.Builder(this)
            .setTitle("Use this AI summary?")
            .setMessage("Check the suggested summary for accuracy first. AI can make mistakes. Only your CV summary will change.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Apply",(dialog,which)->applySummary())
            .show();
    }
    private void applySummary(){
        try{
            JSONArray old=new JSONArray(prefs().getString("resumes","[]"));
            String selected=prefs().getString("current","");
            JSONObject item=null;
            for(int i=0;i<old.length();i++){
                JSONObject row=old.optJSONObject(i);
                if(row!=null&&selected.equals(row.optString("id"))){item=row;break;}
            }
            if(item==null && old.length()>0)item=old.optJSONObject(0);
            if(item==null){
                Toast.makeText(this,"Create a CV first to apply your summary.",Toast.LENGTH_LONG).show();
                return;
            }
            item.put("summary",lastResult);
            prefs().edit().putString("resumes",old.toString()).putString("current",item.optString("id")).apply();
            Toast.makeText(this,"Summary saved to your selected CV.",Toast.LENGTH_LONG).show();
            finish();
        }catch(Exception e){
            status.setText("Could not apply summary. You can copy it instead.");
        }
    }
}
