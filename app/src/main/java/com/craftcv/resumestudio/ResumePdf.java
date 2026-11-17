package com.craftcv.resumestudio;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import java.io.File;
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
        boolean centerHeader = false;
        int textWidth = 505;

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
            if(theme==1 || theme==6){
                paint.setColor(accent);
                canvas.drawRect(0,0,13,842,paint);
            }
            if(theme==8){
                paint.setColor(0xFF3E5068);
                canvas.drawRect(45,28,550,31,paint);
            }
        }
        void ensure(int height) {
            if(y+height>787)nextPage();
        }
        void setFont(int size, boolean bold, int color) {
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTypeface(Typeface.create(
                    (theme==0||theme==5||theme==8||theme==10)?"serif":
                    (theme==11?"monospace":"sans-serif"),
                    bold?Typeface.BOLD:Typeface.NORMAL));
        }
        void line(String value, int spacing) {
            ensure(spacing);
            if(!value.isEmpty()){
                float x=centerHeader?Math.max(45,(595-paint.measureText(value))/2):45;
                canvas.drawText(value,x,y,paint);
            }
            y+=spacing;
        }
        void wrap(String value,int lineHeight){
            for(String paragraph:value.split("\\n",-1)) {
                if(paragraph.trim().isEmpty()) {ensure(9);y+=9;continue;}
                StringBuilder line=new StringBuilder();
                for(String word:paragraph.trim().split("\\s+")) {
                    if(paint.measureText(word)>textWidth) {
                        if(line.length()>0){line(line.toString(),lineHeight);line.setLength(0);}
                        for(int i=0;i<word.length();i++){
                            String chunk=line.toString()+word.charAt(i);
                            if(paint.measureText(chunk)>textWidth && line.length()>0){line(line.toString(),lineHeight);line.setLength(0);}
                            line.append(word.charAt(i));
                        }
                    } else {
                        String next=line.length()==0?word:line+" "+word;
                        if(paint.measureText(next)>textWidth && line.length()>0){line(line.toString(),lineHeight);line.setLength(0);}
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
            paint.setStrokeWidth(theme==8?1.3f:0.7f);
            paint.setColor(theme==8?accent:0xFFDCE5E7);
            canvas.drawLine(45,y-5,550,y-5,paint);
            if(theme==8)canvas.drawLine(45,y-2,550,y-2,paint);
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
        int selected=w.theme;
        String applicant=data.optString("name","").trim();
        if(applicant.isEmpty())applicant="YOUR NAME";
        String role=data.optString("role","").trim();
        String contact=join(
            data.optString("email",""),
            data.optString("phone",""),
            data.optString("location","")
        );

        // Distinct document families rather than twelve recolours of the same header.
        boolean hero=(selected==2 || selected==7);
        boolean softPanel=(selected==4 || selected==10);
        String savedPhoto=data.optString("photoPath","");
        File portrait=savedPhoto.isEmpty()?null:new File(savedPhoto);
        boolean hasPhoto=portrait!=null && portrait.isFile();
        w.centerHeader=!hasPhoto && (selected==5 || selected==8 || selected==10);
        if(hasPhoto)w.textWidth=395;
        if(hero){
            w.paint.setColor(w.accent);
            w.canvas.drawRect(0,0,595,223,w.paint);
            w.y=65;
        }else if(softPanel){
            w.paint.setColor(selected==10?0xFFF8F0E9:0xFFE9F1F3);
            w.canvas.drawRoundRect(39,39,556,178,11,11,w.paint);
            w.y=66;
        }else if(selected==3 || selected==11){
            w.y=57;
        }else{
            w.y=65;
        }

        int titleColor=hero?Color.WHITE:(selected==11?Color.BLACK:w.accent);
        int roleColor=hero?0xFFF0F6F5:(selected==11?Color.BLACK:w.accent);
        int contactColor=hero?0xFFE1ECEC:Color.DKGRAY;
        w.setFont(selected==7?30:selected==10?27:selected==9?20:24,true,titleColor);
        w.wrap(applicant,selected==9?25:31);
        w.y+=3;
        if(!role.isEmpty()){
            w.setFont(selected==9?11:13,false,roleColor);
            w.wrap(role,selected==9?17:20);
        }
        w.y+=6;
        if(!contact.isEmpty()){
            w.setFont(selected==9?9:10,false,contactColor);
            w.wrap(contact,selected==9?14:17);
        }
        w.centerHeader=false;
        w.textWidth=505;
        if(hasPhoto){
            try{
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inSampleSize=2;
                Bitmap bitmap=BitmapFactory.decodeFile(savedPhoto,options);
                if(bitmap!=null){
                    w.paint.setColor(Color.WHITE);
                    w.canvas.drawRoundRect(new RectF(459,47,556,151),8,8,w.paint);
                    w.paint.setAlpha(255);
                    w.canvas.drawBitmap(bitmap,null,new RectF(464,52,551,146),w.paint);
                    bitmap.recycle();
                }
            }catch(Exception ignored){
                // Missing/corrupt image must never block text CV export.
            }catch(OutOfMemoryError ignored){}
        }

        if(hero){
            w.y=Math.max(w.y+12,244);
        }else if(softPanel){
            w.y=Math.max(w.y+12,199);
        }else {
            w.y+=15;
            w.ensure(14);
            w.paint.setColor(w.accent);
            w.paint.setStrokeWidth(selected==8?2f:1.2f);
            w.canvas.drawLine(45,w.y,550,w.y,w.paint);
            if(selected==8)w.canvas.drawLine(45,w.y+5,550,w.y+5,w.paint);
            w.y+=selected==8?22:15;
        }

        for(int i=0;i<KEYS.length;i++){
            String text=data.optString(KEYS[i],"").trim();
            if(text.isEmpty())continue;
            w.heading(HEADINGS[i]);
            w.setFont(selected==9?10:11,false,0xFF353F44);
            w.wrap(text,selected==9?15:17);
        }
        return w.close();
    }

    private static String join(String... values){
        ArrayList<String> parts=new ArrayList<>();
        for(String value:values)if(value!=null&&!value.trim().isEmpty())parts.add(value.trim());
        return android.text.TextUtils.join("  •  ",parts);
    }
}
