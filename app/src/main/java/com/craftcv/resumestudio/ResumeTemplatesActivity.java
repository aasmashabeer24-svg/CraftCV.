package com.craftcv.resumestudio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

/** Visual template-selection screen kept separate from CV writing. */
public final class ResumeTemplatesActivity extends Activity {
    private static final String PREFS="craftcv_native_resumes_v1";
    private static final String[] STYLES={
        "Editorial","Modern","Creative","Minimal","Executive","Classic",
        "Fresh","Bold","Academic","Compact","Elegant","Monochrome"
    };
    private static final int[] COLORS={
        0xFF194C5D,0xFF3C584D,0xFF774960,0xFF303D48,0xFF274054,0xFF354458,
        0xFF417C69,0xFFB05241,0xFF3E5068,0xFF317071,0xFF906D59,0xFF252525
    };
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density*n);}
    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private GradientDrawable background(int color,int border) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(16));
        if(border!=0)d.setStroke(dp(2),border);
        return d;
    }
    private TextView label(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);
        return t;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        build();
    }
    private int selectedStyle(){
        int value=prefs().getInt("default_style",0);
        try{
            String id=prefs().getString("current","");
            JSONArray all=new JSONArray(prefs().getString("resumes","[]"));
            for(int i=0;i<all.length();i++){
                JSONObject row=all.optJSONObject(i);
                if(row!=null && id.equals(row.optString("id")))return row.optInt("style",value);
            }
        }catch(Exception ignored){}
        return value;
    }
    private void applyStyle(int index){
        if(index<0||index>=STYLES.length)return;
        try{
            SharedPreferences.Editor editor=prefs().edit().putInt("default_style",index);
            JSONArray all=new JSONArray(prefs().getString("resumes","[]"));
            String wanted=prefs().getString("current","");
            for(int i=0;i<all.length();i++){
                JSONObject row=all.optJSONObject(i);
                if(row!=null && wanted.equals(row.optString("id")))row.put("style",index);
            }
            editor.putString("resumes",all.toString()).apply();
            Toast.makeText(this,STYLES[index]+" design applied",Toast.LENGTH_SHORT).show();
            if(getIntent().getBooleanExtra("return_to_editor",false)){
                finish();
            }else{
                startActivity(new Intent(this,NativeResumeActivity.class));
                finish();
            }
        }catch(Exception e){Toast.makeText(this,"Could not save design",Toast.LENGTH_LONG).show();}
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xFFF5F8F9);
        setContentView(scroll);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(20),dp(16),dp(35));scroll.addView(root);

        Button back=new Button(this);back.setText("‹  Back");back.setAllCaps(false);
        back.setTextSize(12);back.setTextColor(0xFF184A53);
        back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE6F0EF));
        back.setOnClickListener(v->finish());
        root.addView(back,new LinearLayout.LayoutParams(dp(105),dp(43)));
        TextView heading=label("Choose your CV style",25,0xFF193D4C,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(19);
        root.addView(heading,hp);
        TextView intro=label("Select a design before you build or update your CV. Designs work offline. These are color and typography variations, not 12 unrelated page structures.",12,0xFF78909B,false);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);ip.topMargin=dp(6);ip.bottomMargin=dp(20);
        root.addView(intro,ip);

        int active=selectedStyle();
        for(int start=0;start<STYLES.length;start+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
            rp.bottomMargin=dp(12);root.addView(row,rp);
            for(int i=start;i<Math.min(STYLES.length,start+2);i++){
                final int n=i;
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(11),dp(11),dp(11),dp(13));
                card.setBackground(background(Color.WHITE,i==active?COLORS[i]:0xFFE4EBED));
                card.setFocusable(true);card.setClickable(true);
                card.setContentDescription("Select "+STYLES[i]+" CV design");
                card.setOnClickListener(v->applyStyle(n));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
                if(i%2==0)p.rightMargin=dp(6);else p.leftMargin=dp(6);
                row.addView(card,p);
                MiniPage illustration=new MiniPage(i);
                card.addView(illustration,new LinearLayout.LayoutParams(-1,dp(164)));
                TextView title=label(STYLES[i]+(i==active?"  ✓":""),14,0xFF24424D,true);
                title.setPadding(0,dp(9),0,dp(3));card.addView(title);
                TextView hint=label(i==2||i==7?"Expressive accent":i==3||i==11?"Simple and clear":"Professional and clean",10,0xFF8297A0,false);
                card.addView(hint);
            }
        }
    }

    /** Lightweight resume thumbnail, no external images and no network. */
    private final class MiniPage extends View {
        private final int index;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        MiniPage(int i){super(ResumeTemplatesActivity.this);index=i;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        private void rect(Canvas canvas,float l,float t,float r,float b,int color,float radius){
            p.setColor(color);p.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);
        }
        @Override protected void onDraw(Canvas actual){
            super.onDraw(actual);
            float w=getWidth(),h=getHeight();
            actual.drawColor(0xFFF0F4F5);
            float width=Math.min(w*0.75f,h*0.70f);
            float height=Math.min(h*0.92f,width*1.40f);
            float left=(w-width)/2,top=(h-height)/2;
            actual.save();actual.translate(left,top);
            float x=width/116f,y=height/162f;
            actual.scale(x,y);
            rect(actual,0,0,116,162,Color.WHITE,3);
            p.setColor(COLORS[index]);
            if(index==2||index==4||index==7){actual.drawRect(0,0,116,28,p);}
            if(index==8){actual.drawRect(0,0,9,162,p);}
            if(index==6){rect(actual,7,7,109,28,0xFFE8F3EC,5);}
            if(index==10){rect(actual,5,5,111,31,0xFFF6EEE9,6);}
            if(index==11)p.setColor(Color.BLACK);
            int headingColor=(index==2||index==4||index==7)?Color.WHITE:COLORS[index];
            rect(actual,11,11,77,17,headingColor,1);
            rect(actual,11,21,60,24,(index==2||index==4||index==7)?0xFFDAE7E4:0xFFB9C9CB,1);
            rect(actual,11,36,102,37,0xFFCCD9DC,1);
            float columnX=(index==5||index==0)?11:65;
            rect(actual,11,44,52,47,COLORS[index],1);
            rect(actual,11,52,100,54,0xFFD8E2E4,1);
            rect(actual,11,58,89,60,0xFFD8E2E4,1);
            rect(actual,11,64,96,66,0xFFD8E2E4,1);
            rect(actual,11,76,50,79,COLORS[index],1);
            rect(actual,11,84,101,86,0xFFD8E2E4,1);
            rect(actual,11,90,94,92,0xFFD8E2E4,1);
            rect(actual,11,96,86,98,0xFFD8E2E4,1);
            rect(actual,11,110,49,113,COLORS[index],1);
            rect(actual,11,120,99,122,0xFFD8E2E4,1);
            rect(actual,11,126,89,128,0xFFD8E2E4,1);
            rect(actual,11,132,95,134,0xFFD8E2E4,1);
            if(index==1||index==9||index==8)rect(actual,columnX,141,105,144,COLORS[index],1);
            actual.restore();
        }
    }
}
