package com.craftcv.resumestudio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

/** Offline, editable, no-AI cover letter maker with a matching PDF export. */
public final class CoverLetterActivity extends Activity {
    public static final String EXTRA_RESUME_NAME = "resume_name";
    public static final String EXTRA_RESUME_ROLE = "resume_role";
    public static final String EXTRA_RESUME_SUMMARY = "resume_summary";
    public static final String EXTRA_RESUME_SKILLS = "resume_skills";
    private static final String PREFS = "craftcv_cover_letters_v1";
    private static final int SAVE_PDF = 702;
    private EditText company, role, manager, body;
    private String applicant;
    private byte[] pendingPdf;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(20,54,67));
        applicant = getIntent().getStringExtra(EXTRA_RESUME_NAME);
        if(applicant==null || applicant.trim().isEmpty()) applicant="Applicant";
        build();
    }

    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private TextView text(String value,int size,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);
        v.setTextColor(Color.rgb(36,69,80));
        if(bold) v.setTypeface(null,android.graphics.Typeface.BOLD);
        return v;
    }
    private Button button(String caption,Runnable action){
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(29,84,96)));
        b.setOnClickListener(v->action.run());return b;
    }
    private EditText field(LinearLayout column,String label,String hint,boolean multiline){
        TextView title=text(label,12,true);
        title.setPadding(0,dp(18),0,dp(3));column.addView(title);
        EditText input=new EditText(this);input.setTextSize(14);input.setHint(hint);
        input.setSingleLine(!multiline);
        if(multiline){input.setMinLines(12);input.setGravity(Gravity.TOP);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}
        column.addView(input);
        return input;
    }
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245,249,248));
        setContentView(scroll);
        LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(20),dp(25),dp(20),dp(40));scroll.addView(column);
        TextView heading=text("Cover Letter Studio",26,true);column.addView(heading);
        TextView intro=text("An offline, editable application letter. Your draft stays on this device. No paid AI required.",13,false);
        intro.setPadding(0,dp(8),0,dp(15));column.addView(intro);
        company=field(column,"Company","Company or organization",false);
        role=field(column,"Job title","Position you are applying for",false);
        manager=field(column,"Hiring manager (optional)","Hiring Manager",false);
        body=field(column,"Your cover letter","Tell the employer why you fit the role",true);
        company.setText(prefs().getString("company",""));
        role.setText(prefs().getString("role",getIntent().getStringExtra(EXTRA_RESUME_ROLE)));
        manager.setText(prefs().getString("manager",""));
        body.setText(prefs().getString("body",""));
        Button starter=button("Create an editable starter (no AI)",this::starter);
        column.addView(starter,new LinearLayout.LayoutParams(-1,dp(54)));
        Button save=button("Save letter locally",this::saveDraft);
        column.addView(save,new LinearLayout.LayoutParams(-1,dp(54)));
        Button pdf=button("Export cover letter PDF",this::exportPdf);
        column.addView(pdf,new LinearLayout.LayoutParams(-1,dp(54)));
        Button done=button("Back to CV",this::finish);
        column.addView(done,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView note=text("The draft is a starting point only. Replace placeholders with your real experience and check all facts before applying.",12,false);
        note.setPadding(0,dp(15),0,0);column.addView(note);
    }
    private void starter(){
        if(!body.getText().toString().trim().isEmpty()){
            new android.app.AlertDialog.Builder(this)
                .setTitle("Replace your current letter?")
                .setMessage("Creating a starter replaces the letter shown in the editor.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Replace",(d,w)->fillStarter()).show();
        }else fillStarter();
    }
    private void fillStarter(){
        String job=role.getText().toString().trim(), org=company.getText().toString().trim();
        String summary=getIntent().getStringExtra(EXTRA_RESUME_SUMMARY);
        String skills=getIntent().getStringExtra(EXTRA_RESUME_SKILLS);
        if(job.isEmpty())job="[Job title]";
        if(org.isEmpty())org="[Company]";
        if(summary==null || summary.trim().isEmpty())summary="[Add one or two accurate sentences about your relevant experience.]";
        if(skills==null || skills.trim().isEmpty())skills="[Mention your job-relevant skills and examples.]";
        String draft="Dear "+(manager.getText().toString().trim().isEmpty()?"Hiring Manager":manager.getText().toString().trim())+",\n\n"
            +"I am applying for the "+job+" role at "+org+". I would welcome the opportunity to contribute to your team.\n\n"
            +summary+"\n\n"
            +"My relevant skills include "+skills+". [Add a specific example that demonstrates how you have used these skills.]\n\n"
            +"Thank you for considering my application. I look forward to discussing how I can contribute.\n\n"
            +"Sincerely,\n"+applicant;
        body.setText(draft);saveDraft();
    }
    private void saveDraft(){
        prefs().edit().putString("company",company.getText().toString())
            .putString("role",role.getText().toString())
            .putString("manager",manager.getText().toString())
            .putString("body",body.getText().toString()).apply();
        Toast.makeText(this,"Cover letter saved on this device",Toast.LENGTH_SHORT).show();
    }
    private ArrayList<String> lines(Paint p,String value,float maxWidth){
        ArrayList<String> result=new ArrayList<>();
        for(String paragraph:value.split("\\n",-1)){
            if(paragraph.isEmpty()){result.add("");continue;}
            StringBuilder line=new StringBuilder();
            for(String word:paragraph.split("\\s+")){
                String proposed=line.length()==0?word:line+" "+word;
                if(p.measureText(proposed)>maxWidth && line.length()>0){
                    result.add(line.toString());line.setLength(0);
                }
                if(p.measureText(word)>maxWidth){
                    for(int i=0;i<word.length();i++){
                        String next=line.toString()+word.charAt(i);
                        if(p.measureText(next)>maxWidth && line.length()>0){result.add(line.toString());line.setLength(0);}
                        line.append(word.charAt(i));
                    }
                }else{if(line.length()>0)line.append(' ');line.append(word);}
            }
            result.add(line.toString());
        }
        return result;
    }
    private void exportPdf(){
        if(body.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Write a cover letter first",Toast.LENGTH_LONG).show();return;
        }
        saveDraft();
        PdfDocument document=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNumber=1,y=60;
        PdfDocument.Page page=document.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNumber).create());
        Canvas canvas=page.getCanvas();
        p.setColor(Color.rgb(33,76,88));p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));p.setTextSize(22);
        canvas.drawText(applicant.length()>35?applicant.substring(0,35):applicant,45,y,p);y+=45;
        p.setTypeface(android.graphics.Typeface.DEFAULT);p.setTextSize(11);p.setColor(Color.DKGRAY);
        String meta="Application: "+role.getText().toString()+" | "+company.getText().toString();
        for(String line:lines(p,meta,500)) {canvas.drawText(line,45,y,p);y+=16;}y+=28;
        for(String line:lines(p,body.getText().toString(),500)){
            if(y>=782){document.finishPage(page);page=document.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());canvas=page.getCanvas();y=58;}
            canvas.drawText(line,45,y,p);y+=16;
        }
        document.finishPage(page);
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            document.writeTo(out);pendingPdf=out.toByteArray();
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE,"CraftCV-Cover-Letter.pdf");
            startActivityForResult(intent,SAVE_PDF);
        }catch(Exception error){pendingPdf=null;Toast.makeText(this,"PDF generation failed",Toast.LENGTH_LONG).show();}
        finally{document.close();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SAVE_PDF)return;
        if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingPdf!=null){
            try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){
                if(stream==null)throw new IllegalStateException();
                stream.write(pendingPdf);
                Toast.makeText(this,"Cover letter PDF saved",Toast.LENGTH_LONG).show();
            }catch(Exception error){Toast.makeText(this,"Could not save PDF",Toast.LENGTH_LONG).show();}
        }
        pendingPdf=null;
    }
    @Override protected void onPause(){super.onPause();saveDraftSilently();}
    private void saveDraftSilently(){
        if(company==null||body==null)return;
        prefs().edit().putString("company",company.getText().toString())
            .putString("role",role.getText().toString())
            .putString("manager",manager.getText().toString())
            .putString("body",body.getText().toString()).apply();
    }
}
