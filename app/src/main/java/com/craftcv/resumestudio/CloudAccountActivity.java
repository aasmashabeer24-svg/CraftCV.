package com.craftcv.resumestudio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Native entry to the existing authenticated CraftCV cloud account.
 * Uses the established website rather than pretending the native offline CVs
 * are synced or storing login credentials in the Android application.
 */
public final class CloudAccountActivity extends Activity {
    private static final String CLOUD_URL="https://craftcv-hees13.v2.appdeploy.ai/#cloud";
    private static final int INK=0xFF203D4B,TEAL=0xFF246C62,MUTED=0xFF6A8790;
    private int dp(int v){return Math.round(getResources().getDisplayMetrics().density*v);}
    private GradientDrawable bg(int color){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(14));
        return d;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setLineSpacing(dp(3),1);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(String label,boolean primary,View.OnClickListener onClick){
        Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);
        b.setTextColor(primary?Color.WHITE:TEAL);
        b.setBackgroundTintList(null);
        b.setBackground(bg(primary?TEAL:0xFFE6F3EF));
        b.setOnClickListener(onClick);
        return b;
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xFFF5F8F9);
        setContentView(scroll);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20),dp(23),dp(20),dp(40));scroll.addView(page);
        Button back=button("‹ Dashboard",false,v->finish());
        page.addView(back,new LinearLayout.LayoutParams(dp(142),dp(45)));
        TextView heading=text("My Cloud Account",28,INK,true);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
        hp.topMargin=dp(22);page.addView(heading,hp);
        TextView sub=text("Sign in to save and open online CVs across your devices.",14,MUTED,false);
        sub.setPadding(0,dp(8),0,dp(18));page.addView(sub);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(19),dp(20),dp(19),dp(20));
        card.setBackground(bg(Color.WHITE));
        page.addView(card);
        card.addView(text("SECURE ONLINE WORKSPACE",11,TEAL,true));
        TextView intro=text("Manage private cloud resumes through the existing CraftCV Online Studio account. Sign in or create an account there.",16,INK,true);
        intro.setPadding(0,dp(14),0,dp(14));card.addView(intro);
        Button inApp=button("Open Cloud Account in app",true,v->openInApp());
        card.addView(inApp,new LinearLayout.LayoutParams(-1,dp(53)));
        Button browser=button("Sign in using browser",false,v->openInBrowser());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(51));
        bp.topMargin=dp(11);card.addView(browser,bp);

        TextView features=text("WHAT YOU CAN DO ONLINE",12,TEAL,true);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);
        fp.topMargin=dp(27);fp.bottomMargin=dp(8);page.addView(features,fp);
        page.addView(text("• Sign in / create an account\n• Upload an online CV to your account\n• Open and edit your online CVs on another device\n• Delete a cloud CV from your account",14,INK,false));
        LinearLayout notice=new LinearLayout(this);notice.setOrientation(LinearLayout.VERTICAL);
        notice.setPadding(dp(15),dp(15),dp(15),dp(15));
        notice.setBackground(bg(0xFFEAF3F2));
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);
        np.topMargin=dp(22);page.addView(notice,np);
        notice.addView(text("IMPORTANT — LOCAL CVS ARE SEPARATE",12,TEAL,true));
        TextView limitation=text("Your offline Android CVs are private on this phone and are not automatically uploaded or synced with the website. Only resumes created or uploaded inside the online account appear there. Never share your login password or OTP here.",12,MUTED,false);
        limitation.setPadding(0,dp(7),0,0);notice.addView(limitation);
        Button local=button("My offline saved CVs",false,v->
            startActivity(new Intent(this,ResumeLibraryActivity.class)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(49));
        lp.topMargin=dp(18);page.addView(local,lp);
    }
    private void openInApp(){
        Intent intent=new Intent(this,MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_START_PAGE,"cloud");
        startActivity(intent);
    }
    private void openInBrowser(){
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(CLOUD_URL));
            startActivity(intent);
        }catch(ActivityNotFoundException error){
            Toast.makeText(this,"No browser available; opening in-app Cloud instead.",Toast.LENGTH_LONG).show();
            openInApp();
        }
    }
}
