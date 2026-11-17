package com.craftcv.resumestudio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Simple, native offline dashboard. Each major workflow has its own screen.
 * No sign-in wall or sensitive CV upload; optional consent-gated ad below content.
 */
public final class DashboardActivity extends Activity {
    private static final int INK = 0xFF183746;
    private static final int MUTED = 0xFF69818C;
    private static final int TEAL = 0xFF26776A;
    private static final int BG = 0xFFF5F8F9;
    private static final int BORDER = 0xFFE0E9EB;
    private CraftCvAds ads;
    private boolean firstResume=true;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private GradientDrawable heroBackground() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF143D4D, 0xFF256A67, 0xFF397E6F});
        d.setCornerRadius(dp(24));
        return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLineSpacing(dp(2), 1.0f);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return t;
    }
    private int resumeCount() {
        try {
            return new JSONArray(getSharedPreferences("craftcv_native_resumes_v1", MODE_PRIVATE)
                    .getString("resumes", "[]")).length();
        } catch (Exception ignored) { return 0; }
    }
    private JSONObject currentResume() {
        SharedPreferences prefs = getSharedPreferences("craftcv_native_resumes_v1", MODE_PRIVATE);
        try {
            JSONArray rows = new JSONArray(prefs.getString("resumes", "[]"));
            String id = prefs.getString("current", "");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null && id.equals(row.optString("id"))) return row;
            }
            if (rows.length() > 0) return rows.getJSONObject(0);
        } catch (Exception ignored) {}
        return null;
    }
    private void launchBuilder(boolean fresh) {
        Intent intent = new Intent(this, NativeResumeActivity.class);
        intent.putExtra("start_new", fresh);
        startActivity(intent);
    }
    private void launchPreview() {
        JSONObject resume = currentResume();
        if (resume == null) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("No saved CV yet")
                .setMessage("Create your first resume to see a finished PDF preview.")
                .setPositiveButton("Build my CV", (dialog, which) -> launchBuilder(true))
                .setNegativeButton("Cancel", null).show();
            return;
        }
        startActivity(new Intent(this, ResumePreviewActivity.class));
    }
    private void launchCoverLetter() {
        JSONObject resume = currentResume();
        Intent intent = new Intent(this, CoverLetterActivity.class);
        if (resume != null) {
            intent.putExtra(CoverLetterActivity.EXTRA_RESUME_NAME, resume.optString("name", ""));
            intent.putExtra(CoverLetterActivity.EXTRA_RESUME_ROLE, resume.optString("role", ""));
            intent.putExtra(CoverLetterActivity.EXTRA_RESUME_SUMMARY, resume.optString("summary", ""));
            intent.putExtra(CoverLetterActivity.EXTRA_RESUME_SKILLS, resume.optString("skills", ""));
        }
        startActivity(intent);
    }
    private Button mainButton(String label, boolean emphasized, Runnable click) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setTextColor(emphasized ? INK : Color.WHITE);
        b.setBackgroundTintList(null);
        b.setBackground(rounded(emphasized ? 0xFFFFFFFF : 0xFF225A63, 13, emphasized ? 0 : 0xFF55958D));
        b.setOnClickListener(v -> click.run());
        return b;
    }
    private LinearLayout tile(String icon, String title, String detail, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(12), dp(16));
        card.setBackground(rounded(Color.WHITE, 18, BORDER));
        card.setMinimumHeight(dp(164));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(title);
        card.setOnClickListener(v -> action.run());

        TextView symbol = text(icon, 23, TEAL, false);
        symbol.setGravity(Gravity.CENTER);
        symbol.setBackground(rounded(0xFFE8F4EF, 12, 0));
        card.addView(symbol, new LinearLayout.LayoutParams(dp(47), dp(47)));

        TextView heading = text(title, 16, INK, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(13);
        card.addView(heading, hp);
        TextView subtitle = text(detail, 11, MUTED, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(4);
        card.addView(subtitle, sp);
        return card;
    }
    private void addPair(LinearLayout parent, LinearLayout a, LinearLayout b) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.bottomMargin = dp(12);
        parent.addView(row, rp);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
        p1.rightMargin = dp(6);
        row.addView(a, p1);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1);
        p2.leftMargin = dp(6);
        row.addView(b, p2);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        build();
    }
    @Override protected void onResume() {
        super.onResume();
        // Android automatically calls onResume after onCreate. Only rebuild on
        // returning from editing, so the consent form is never shown twice.
        if(firstResume)firstResume=false;
        else build();
        if(ads!=null)ads.resume();
    }
    @Override protected void onPause() {
        if(ads!=null)ads.pause();
        super.onPause();
    }
    @Override protected void onDestroy() {
        if(ads!=null){ads.destroy();ads=null;}
        super.onDestroy();
    }
    private void build() {
        if(ads!=null){ads.destroy();ads=null;}
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        setContentView(scroll);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(19), dp(23), dp(19), dp(40));
        scroll.addView(page);

        TextView brand = text("CRAFTCV   /   CAREER STUDIO", 11, TEAL, true);
        brand.setLetterSpacing(.12f);
        page.addView(brand);
        TextView welcome = text("Make your next move\nlook amazing.", 31, INK, true);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.topMargin = dp(9);
        page.addView(welcome, wp);
        TextView explanation = text("Build, design and download a professional resume — free and offline.", 13, MUTED, false);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
        ep.topMargin = dp(8);
        ep.bottomMargin = dp(22);
        page.addView(explanation, ep);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(23), dp(20), dp(22));
        hero.setBackground(heroBackground());
        page.addView(hero, new LinearLayout.LayoutParams(-1, -2));
        TextView kicker = text("YOUR NEXT OPPORTUNITY STARTS HERE", 10, 0xFFB6E4D6, true);
        kicker.setLetterSpacing(.10f);
        hero.addView(kicker);
        TextView heroTitle = text("Create your own CV", 24, Color.WHITE, true);
        LinearLayout.LayoutParams htp = new LinearLayout.LayoutParams(-1, -2);
        htp.topMargin = dp(13);
        hero.addView(heroTitle, htp);
        TextView heroDesc = text("A simple step-by-step builder, followed by a full-page preview.", 13, 0xFFE1EFEA, false);
        LinearLayout.LayoutParams hdp = new LinearLayout.LayoutParams(-1, -2);
        hdp.topMargin = dp(6);
        hdp.bottomMargin = dp(18);
        hero.addView(heroDesc, hdp);
        hero.addView(mainButton("Build My CV   →", true, () -> launchBuilder(true)),
                new LinearLayout.LayoutParams(-1, dp(53)));
        if (resumeCount() > 0) {
            Button continueButton = mainButton("Continue editing", false, () -> launchBuilder(false));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(49));
            cp.topMargin = dp(10);
            hero.addView(continueButton, cp);
        }

        TextView section = text("YOUR WORKSPACE", 11, TEAL, true);
        section.setLetterSpacing(.12f);
        LinearLayout.LayoutParams sec = new LinearLayout.LayoutParams(-1, -2);
        sec.topMargin = dp(28);
        sec.bottomMargin = dp(12);
        page.addView(section, sec);

        addPair(page,
            tile("▤", "My saved CVs", resumeCount() + " resumes on this phone", () ->
                startActivity(new Intent(this, ResumeLibraryActivity.class))),
            tile("◈", "CV Templates", "Pick a professional style", () ->
                startActivity(new Intent(this, ResumeTemplatesActivity.class))));
        addPair(page,
            tile("✎", "Cover Letters", "Write a matching job letter", this::launchCoverLetter),
            tile("▣", "Finished CV", "Preview the A4 document", this::launchPreview));
        addPair(page,
            tile("⬇", "PDF & Word", "Save, open and share CV files", this::launchPreview),
            tile("✦", "AI Assistant", "Improve summary & experience online", () ->
                startActivity(new Intent(this, AiAssistantActivity.class))));
        addPair(page,
            tile("☁", "Cloud Account", "Sign in & manage online CVs", () ->
                startActivity(new Intent(this, CloudAccountActivity.class))),
            tile("✧", "CV Design Gallery", "Explore offline templates", () ->
                startActivity(new Intent(this, ResumeTemplatesActivity.class))));

        LinearLayout online = new LinearLayout(this);
        online.setPadding(dp(17), dp(16), dp(17), dp(16));
        online.setOrientation(LinearLayout.VERTICAL);
        online.setBackground(rounded(0xFFEAF1F2, 15, 0));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, -2);
        op.topMargin = dp(14);
        page.addView(online, op);
        online.addView(text("OPTIONAL  •  INTERNET REQUIRED", 10, TEAL, true));
        TextView onlineTitle = text("Advanced AI & Cloud Studio", 15, INK, true);
        onlineTitle.setPadding(0, dp(5), 0, dp(4));
        online.addView(onlineTitle);
        online.addView(text("Use online AI and cloud tools when available. Your offline CVs work even if the website is down.", 12, MUTED, false));
        Button open = mainButton("Open online studio   ↗", false, () ->
            startActivity(new Intent(this, MainActivity.class)));
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(-1, dp(47));
        openParams.topMargin = dp(12);
        online.addView(open, openParams);

        // Single, clearly labeled, adaptive banner at the bottom of Home only.
        // Never show ads beside text fields, download controls or CV documents.
        ads=new CraftCvAds(this,page);
        ads.start();
    }
}
