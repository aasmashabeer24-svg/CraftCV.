package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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

import java.util.ArrayList;

/** Repeatable project and portfolio editor for offline CVs. */
public final class ProjectsActivity extends Activity {
    private static final String PREFS = "craftcv_native_resumes_v1";
    private static final String KEY_ITEMS = "resumes";
    private static final String KEY_CURRENT = "current";
    private static final int INK = 0xFF193D4B;
    private static final int TEAL = 0xFF246D65;
    private static final int MUTED = 0xFF718995;
    private static final String[] CATEGORIES = {
        "Academic", "Personal", "Professional", "Freelance", "Open Source",
        "Research", "Portfolio", "Volunteer", "Business", "Final Year Project",
        "Competition", "Other / Custom"
    };
    private final ArrayList<JSONObject> rows = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSave = () -> persist();
    private JSONObject resume = new JSONObject();
    private String resumeId = "";
    private LinearLayout list;
    private TextView status;
    private boolean loading;

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private Button button(String value, boolean primary, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(primary ? Color.WHITE : TEAL); b.setBackgroundTintList(null);
        b.setBackground(shape(primary ? TEAL : 0xFFEAF3F2, 11, 0));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private JSONArray readItems() {
        try { return new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ITEMS, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    private boolean loadResume() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        resumeId = getIntent().getStringExtra("resume_id");
        if (resumeId == null || resumeId.isEmpty()) resumeId = prefs.getString(KEY_CURRENT, "");
        JSONArray items = readItems();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null && resumeId.equals(item.optString("id"))) { resume = item; return true; }
        }
        return false;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D); getWindow().setNavigationBarColor(0xFF143D4D);
        if (!loadResume()) {
            Toast.makeText(this, "Could not open this CV. Please reopen it.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        loadRows(); buildUI(); renderRows();
    }
    private void loadRows() {
        loading = true;
        JSONArray saved = resume.optJSONArray("projectEntries");
        if (saved != null && saved.length() > 0) {
            for (int i = 0; i < saved.length(); i++) {
                JSONObject row = saved.optJSONObject(i); if (row != null) rows.add(row);
            }
        } else {
            String legacy = resume.optString("projects", "").trim();
            if (!legacy.isEmpty()) {
                JSONObject row = new JSONObject();
                try {
                    row.put("id", "project-legacy"); row.put("title", ""); row.put("role", "");
                    row.put("category", ""); row.put("organization", ""); row.put("tools", "");
                    row.put("date", ""); row.put("link", ""); row.put("details", legacy);
                } catch (Exception ignored) {}
                rows.add(row);
            }
        }
        loading = false;
    }
    private void buildUI() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xFFF5F8F9);
        setContentView(scroll);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(32)); scroll.addView(page);
        page.addView(button("‹ Back to CV builder", false, () -> { persist(); finish(); }),
            new LinearLayout.LayoutParams(dp(190), dp(44)));
        TextView heading = text("Projects & Portfolio", 27, INK, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2); hp.topMargin = dp(20); page.addView(heading, hp);
        TextView intro = text("Show projects that prove what you can do. Add academic, personal, freelance, research or professional work.", 13, MUTED, false);
        intro.setPadding(0, dp(8), 0, dp(16)); page.addView(intro);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(12); page.addView(list, lp);
        page.addView(button("+ Add another project", true, this::addRow), new LinearLayout.LayoutParams(-1, dp(50)));
        status = text("Saved privately on this device.", 12, MUTED, false);
        status.setPadding(dp(2), dp(12), dp(2), dp(8)); page.addView(status);
        page.addView(button("Save & return", false, () -> { persist(); finish(); }), new LinearLayout.LayoutParams(-1, dp(48)));
    }
    private void renderRows() {
        list.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = text("No projects yet. Add a portfolio, university, freelance or personal project — or leave this section empty.", 13, MUTED, false);
            empty.setPadding(dp(13), dp(18), dp(13), dp(18)); list.addView(empty);
        }
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.get(i);
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(14), dp(14), dp(14)); card.setBackground(shape(Color.WHITE, 14, 0xFFE1EAED));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.bottomMargin = dp(13); list.addView(card, cp);
            LinearLayout titleRow = new LinearLayout(this); titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.setOrientation(LinearLayout.HORIZONTAL); card.addView(titleRow);
            titleRow.addView(text("Project " + (i + 1), 15, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
            titleRow.addView(button("Remove", false, () -> removeRow(row)), new LinearLayout.LayoutParams(dp(90), dp(39)));
            addInput(card, row, "Project name", "e.g. Inventory Management App", "title", false);
            addInput(card, row, "Your role", "e.g. Designer, Developer, Team Lead", "role", false);
            addSpinner(card, row, "Project category", "category");
            addInput(card, row, "Organization / client (optional)", "University, client or personal project", "organization", false);
            addInput(card, row, "Tools / technologies", "React, Figma, Excel, Python...", "tools", false);
            addInput(card, row, "Dates", "2024 or 01/2024 – 06/2024", "date", false);
            addInput(card, row, "Project URL (optional)", "https://your-project.example", "link", false);
            addInput(card, row, "Description & outcomes", "Explain the problem, your contribution and the result. One point per line.", "details", true);
        }
    }
    private void addInput(LinearLayout parent, JSONObject row, String title, String hint, String key, boolean multiline) {
        LinearLayout wrap = new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2); wp.topMargin = dp(10); parent.addView(wrap, wp);
        wrap.addView(text(title, 12, INK, true));
        EditText input = new EditText(this); input.setText(row.optString(key, "")); input.setHint(hint);
        input.setTextSize(14); input.setTextColor(INK); input.setHintTextColor(0xFFA0AFB5);
        input.setPadding(dp(11), dp(9), dp(11), dp(9)); input.setBackground(shape(0xFFF8FAFB, 9, 0xFFE3EAEC));
        input.setSingleLine(!multiline);
        if (multiline) { input.setMinLines(4); input.setGravity(Gravity.TOP); input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); }
        wrap.addView(input, new LinearLayout.LayoutParams(-1, -2));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable value) {
                if (loading) return; try { row.put(key, value.toString()); } catch (Exception ignored) {}
                scheduleSave();
            }
        });
    }
    private void addSpinner(LinearLayout parent, JSONObject row, String title, String key) {
        LinearLayout wrap = new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2); wp.topMargin = dp(10); parent.addView(wrap, wp);
        wrap.addView(text(title, 12, INK, true));
        String[] values = new String[CATEGORIES.length + 1]; values[0] = "Choose project category";
        System.arraycopy(CATEGORIES, 0, values, 1, CATEGORIES.length);
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equals(row.optString(key, ""))) spinner.setSelection(i + 1);
        wrap.addView(spinner, new LinearLayout.LayoutParams(-1, dp(48)));
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int position, long id) {
                if (loading) return; try { row.put(key, position == 0 ? "" : CATEGORIES[position - 1]); } catch (Exception ignored) {}
                scheduleSave();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    }
    private void addRow() {
        JSONObject row = new JSONObject(); try { row.put("id", "project-" + System.nanoTime()); } catch (Exception ignored) {}
        rows.add(row); renderRows(); scheduleSave();
    }
    private void removeRow(JSONObject row) {
        new AlertDialog.Builder(this).setTitle("Remove this project?")
            .setMessage("This project will be removed from the CV.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove", (dialog, which) -> { rows.remove(row); renderRows(); scheduleSave(); }).show();
    }
    private void scheduleSave() {
        if (loading) return; status.setText("Saving your projects...");
        handler.removeCallbacks(pendingSave); handler.postDelayed(pendingSave, 500);
    }
    private String mirrorText() {
        StringBuilder out = new StringBuilder();
        for (JSONObject row : rows) {
            String title = row.optString("title", "").trim();
            if (!title.isEmpty()) out.append(title);
            String role = row.optString("role", "").trim();
            String org = row.optString("organization", "").trim();
            if (!role.isEmpty()) out.append(out.length() > 0 ? " — " : "").append(role);
            if (!org.isEmpty()) out.append(out.length() > 0 ? " | " : "").append(org);
            ArrayList<String> meta = new ArrayList<>();
            for (String key : new String[]{"category","tools","date","link"}) {
                String value = row.optString(key, "").trim(); if (!value.isEmpty()) meta.add(value);
            }
            if (!meta.isEmpty()) out.append("\n").append(String.join(" • ", meta));
            String details = row.optString("details", "").trim(); if (!details.isEmpty()) out.append("\n").append(details);
            out.append("\n\n");
        }
        return out.toString().trim();
    }
    private void persist() {
        handler.removeCallbacks(pendingSave);
        try {
            JSONArray structured = new JSONArray(); for (JSONObject row : rows) structured.put(row);
            resume.put("projectEntries", structured); resume.put("projects", mirrorText());
            JSONArray old = readItems(), next = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item != null && !resumeId.equals(item.optString("id"))) next.put(item);
            }
            next.put(resume);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_ITEMS, next.toString()).putString(KEY_CURRENT, resumeId).apply();
            if (status != null) status.setText("Saved privately on this device.");
        } catch (Exception e) { if (status != null) status.setText("Could not save projects."); }
    }
    @Override protected void onPause() { persist(); super.onPause(); }
}
