package com.craftcv.resumestudio;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Generates the exact PDF displayed by ResumePreviewActivity.
 * Offline and text-based (selectable/searchable). No bitmap screenshot or WebView required.
 */
public final class ResumePdf {
    private ResumePdf(){}

    private static final int[] ACCENTS = {
        0xFF194C5D,0xFF3C584D,0xFF774960,0xFF303D48,0xFF274054,0xFF354458,
        0xFF417C69,0xFFB05241,0xFF3E5068,0xFF317071,0xFF906D59,0xFF252525
    };
    private static final String[] KEYS = {"summary","experience","education","skills","projects"};
    private static final String[] HEADINGS = {"PROFESSIONAL PROFILE","WORK EXPERIENCE","EDUCATION","CORE SKILLS","PROJECTS"};

    private static final class Writer {
        final PdfDocument pdf = new PdfDocument();
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        PdfDocument.Page page;
        Canvas canvas;
        int y = 54;
        int count = 0;
        final int accent;
        final int theme;

        Writer(int theme) {
            this.theme=Math.max(0,Math.min(ACCENTS.length-1,theme));
            this.accent=ACCENTS[this.theme];
            nextPage();
        }
        void nextPage(){
            if(page!=null)pdf.finishPage(page);
            page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++count).create());
            canvas=page.getCanvas();
            y=54;
            if(theme==2 || theme==4 || theme==7){
                paint.setColor(accent);
                canvas.drawRect(0,0,595,13,paint);
            }
        }
        void ensure(int height) {
            if(y+height>787)nextPage();
        }
        void setFont(int size, boolean bold, int color) {
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTypeface(Typeface.create((theme==0||theme==5||theme==10)?"serif":"sans-serif",
                    bold?Typeface.BOLD:Typeface.NORMAL));
        }
        void line(String value, int spacing) {
            ensure(spacing);
            if(!value.isEmpty())canvas.drawText(value,45,y,paint);
            y+=spacing;
        }
        void wrap(String value,int lineHeight){
            for(String paragraph:value.split("\\n",-1)) {
                if(paragraph.trim().isEmpty()) {ensure(9);y+=9;continue;}
                StringBuilder line=new StringBuilder();
                for(String word:paragraph.trim().split("\\s+")) {
                    if(paint.measureText(word)>505) {
                        if(line.length()>0){line(line.toString(),lineHeight);line.setLength(0);}
                        for(int i=0;i<word.length();i++){
                            String chunk=line.toString()+word.charAt(i);
                            if(paint.measureText(chunk)>505 && line.length()>0){line(line.toString(),lineHeight);line.setLength(0);}
                            line.append(word.charAt(i));
                        }
                    } else {
                        String next=line.length()==0?word:line+" "+word;
                        if(paint.measureText(next)>505 && line.length()>0){line(line.toString(),lineHeight);line.setLength(0);}
                        if(line.length()>0)line.append(' ');
                        line.append(word);
                    }
                }
                if(line.length()>0)line(line.toString(),lineHeight);
            }
        }
        void heading(String title){
            ensure(50);
            y+=15;
            setFont(theme==9?11:12,true,theme==11?Color.BLACK:accent);
            line(title,19);
            paint.setStrokeWidth(0.7f);paint.setColor(0xFFDCE5E7);
            canvas.drawLine(45,y-5,550,y-5,paint);
            y+=7;
        }
        byte[] close() throws Exception {
            pdf.finishPage(page);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try{pdf.writeTo(out);return out.toByteArray();}
            finally{pdf.close();}
        }
    }

    public static byte[] generate(JSONObject data,int style) throws Exception {
        Writer w=new Writer(style);
        w.setFont(style==7?26:23,true,w.accent);
        w.wrap(data.optString("name","").trim().isEmpty()?"YOUR NAME":data.optString("name",""),29);
        w.y+=3;
        w.setFont(13,false,w.accent);
        String role=data.optString("role","").trim();
        if(!role.isEmpty())w.wrap(role,19);
        w.y+=5;
        w.setFont(10,false,Color.DKGRAY);
        String contact=join(data.optString("email",""),data.optString("phone",""),data.optString("location",""));
        if(!contact.isEmpty())w.wrap(contact,16);
        w.y+=12;
        w.ensure(15);
        w.paint.setColor(w.accent);w.paint.setStrokeWidth(1.5f);
        w.canvas.drawLine(45,w.y,550,w.y,w.paint);w.y+=15;
        for(int i=0;i<KEYS.length;i++){
            String text=data.optString(KEYS[i],"").trim();
            if(text.isEmpty())continue;
            w.heading(HEADINGS[i]);
            w.setFont(style==9?10:11,false,0xFF353F44);
            w.wrap(text,style==9?15:17);
        }
        return w.close();
    }

    private static String join(String... values){
        ArrayList<String> parts=new ArrayList<>();
        for(String value:values)if(value!=null&&!value.trim().isEmpty())parts.add(value.trim());
        return android.text.TextUtils.join("  •  ",parts);
    }
}
