package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

/** Standalone saved resumes library. All edits remain in private offline storage. */
public final class ResumeLibraryActivity extends Activity {
    private static final String PREFS = "craftcv_native_resumes_v1";
    private static final int INK = 0xFF173B4B, MUTED = 0xFF78909B, TEAL = 0xFF286D66;
    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private SharedPreferences prefs(){return getSharedPreferences(PREFS, MODE_PRIVATE);}
    private JSONArray read() {
        try{return new JSONArray(prefs().getString("resumes","[]"));}
        catch(Exception e){return new JSONArray();}
    }
    private GradientDrawable bg(int fill,int radius,int stroke) {
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);g.setCornerRadius(dp(radius));
        if(stroke!=0)g.setStroke(dp(1),stroke);
        return g;
    }
    private TextView label(String name,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(name);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(String value,Runnable click,boolean primary){
        Button b=new Button(this);b.setText(value);b.setTextSize(12);b.setAllCaps(false);
        b.setTextColor(primary?Color.WHITE:INK);
        b.setBackgroundTintList(null);
        b.setBackground(bg(primary?TEAL:0xFFEDF5F4,10,0));
        b.setOnClickListener(v->click.run());
        return b;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        draw();
    }
    @Override protected void onResume(){super.onResume();draw();}
    private void select(JSONObject item,boolean preview){
        if(item==null)return;
        prefs().edit().putString("current",item.optString("id")).apply();
        startActivity(new Intent(this, preview?ResumePreviewActivity.class:NativeResumeActivity.class));
    }
    private void duplicate(JSONObject source){
        try{
            JSONObject copy=new JSONObject(source.toString());
            copy.put("id","native-"+System.currentTimeMillis()+"-copy");
            JSONArray items=read();items.put(copy);
            prefs().edit().putString("resumes",items.toString()).putString("current",copy.optString("id")).apply();
            Toast.makeText(this,"CV copied. Original preserved.",Toast.LENGTH_SHORT).show();
            draw();
        }catch(Exception e){Toast.makeText(this,"Cannot duplicate CV",Toast.LENGTH_LONG).show();}
    }
    private void delete(JSONObject source){
        if(read().length()<2){
            new AlertDialog.Builder(this).setMessage("Keep one CV. Create another before deleting this one.")
                .setPositiveButton("OK",null).show();return;
        }
        new AlertDialog.Builder(this).setTitle("Delete saved CV?")
            .setMessage("This removes the resume from this phone. Export a backup first.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Delete",(d,w)->{
                try{
                    JSONArray all=read(),next=new JSONArray();
                    for(int i=0;i<all.length();i++){
                        JSONObject row=all.optJSONObject(i);
                        if(row!=null&&!row.optString("id").equals(source.optString("id")))next.put(row);
                    }
                    SharedPreferences.Editor editor=prefs().edit().putString("resumes",next.toString());
                    if(source.optString("id").equals(prefs().getString("current",""))&&next.length()>0)
                        editor.putString("current",next.getJSONObject(0).optString("id"));
                    editor.apply();
                    draw();
                }catch(Exception e){Toast.makeText(this,"Could not delete CV",Toast.LENGTH_LONG).show();}
            }).show();
    }
    private void draw(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xFFF5F8F9);
        setContentView(scroll);
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(21),dp(18),dp(35));
        scroll.addView(page);
        Button back=button("‹  Dashboard",this::finish,false);
        page.addView(back,new LinearLayout.LayoutParams(dp(133),dp(45)));
        TextView heading=label("My saved CVs",27,INK,true);
        heading.setPadding(0,dp(18),0,dp(8));page.addView(heading);
        JSONArray items=read();
        TextView intro=label(items.length()+" locally saved resumes. Open one to edit, preview or export it.",13,MUTED,false);
        intro.setPadding(0,0,0,dp(14));page.addView(intro);
        Button create=button("+  Create a new CV",()->{
            Intent intent=new Intent(this,NativeResumeActivity.class);
            intent.putExtra("start_new",true);startActivity(intent);
        },true);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(52));cp.bottomMargin=dp(17);
        page.addView(create,cp);

        if(items.length()==0){
            TextView empty=label("No CVs yet. Create your first one to get started.",15,INK,false);
            empty.setPadding(dp(16),dp(28),dp(16),dp(28));
            empty.setBackground(bg(Color.WHITE,16,0xFFDCE8EA));
            page.addView(empty);return;
        }
        String current=prefs().getString("current","");
        for(int i=items.length()-1;i>=0;i--){
            JSONObject item=items.optJSONObject(i);if(item==null)continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16),dp(15),dp(16),dp(14));
            card.setBackground(bg(Color.WHITE,16,0xFFE0E8EC));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);
            page.addView(card,p);
            String name=item.optString("name","").trim();
            if(name.isEmpty())name="Untitled CV";
            if(item.optString("id").endsWith("-copy"))name+="  ·  Copy";
            if(current.equals(item.optString("id")))name+="  •  Current";
            TextView title=label(name,16,INK,true);card.addView(title);
            String role=item.optString("role","").trim();
            TextView sub=label(role.isEmpty()?"No job title yet":role,12,MUTED,false);
            sub.setPadding(0,dp(5),0,dp(12));card.addView(sub);
            LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(buttons);
            final JSONObject selected=item;
            addAction(buttons,"Edit",()->select(selected,false),true);
            addAction(buttons,"View",()->select(selected,true),false);
            addAction(buttons,"Copy",()->duplicate(selected),false);
            addAction(buttons,"Delete",()->delete(selected),false);
        }
    }
    private void addAction(LinearLayout parent,String title,Runnable command,boolean primary){
        Button b=button(title,command,primary);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);
        p.rightMargin=dp(5);
        parent.addView(b,p);
    }
}
