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
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
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

/** Repeatable, structured work experience editor for the offline CV builder. */
public final class WorkExperienceActivity extends Activity {
    private static final String PREFS = "craftcv_native_resumes_v1";
    private static final String KEY_ITEMS = "resumes";
    private static final String KEY_CURRENT = "current";
    private static final int INK = 0xFF193D4B;
    private static final int TEAL = 0xFF246D65;
    private static final int MUTED = 0xFF718995;
    private static final String[] CATEGORIES = {
        "Information Technology", "Software Development", "Business & Management",
        "Finance & Accounting", "Marketing & Sales", "Engineering", "Education & Training",
        "Healthcare", "Customer Service", "Administration", "Creative & Media",
        "Operations & Logistics", "Research", "Retail & Hospitality", "Construction & Trades",
        "Human Resources", "Legal & Compliance", "Government & Public Service", "Other / Custom"
    };
    private static final String[] TYPES = {
        "Full-time", "Part-time", "Internship", "Freelance", "Contract",
        "Apprenticeship", "Volunteer", "Self-employed", "Temporary", "Other"
    };
    private final ArrayList<JSONObject> rows = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSave = () -> persist();
    private JSONObject resume = new JSONObject();
    private String resumeId = "";
    private LinearLayout list;
    private TextView status;
    private boolean loading;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value, boolean primary, Runnable action) {
        Button view = new Button(this);
        view.setText(value);
        view.setAllCaps(false);
        view.setTextSize(13);
        view.setTextColor(primary ? Color.WHITE : TEAL);
        view.setBackgroundTintList(null);
        view.setBackground(shape(primary ? TEAL : 0xFFEAF3F2, 11, 0));
        view.setOnClickListener(v -> action.run());
        return view;
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
            if (item != null && resumeId.equals(item.optString("id"))) {
                resume = item;
                return true;
            }
        }
        return false;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        if (!loadResume()) {
            Toast.makeText(this, "Could not open this CV. Please reopen it.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        loadRows();
        buildUI();
        renderRows();
    }

    private void loadRows() {
        loading = true;
        JSONArray saved = resume.optJSONArray("experienceEntries");
        if (saved != null && saved.length() > 0) {
            for (int i = 0; i < saved.length(); i++) {
                JSONObject row = saved.optJSONObject(i);
                if (row != null) rows.add(row);
            }
        } else {
            String legacy = resume.optString("experience", "").trim();
            if (!legacy.isEmpty()) {
                JSONObject row = new JSONObject();
                try {
                    row.put("id", "work-legacy");
                    row.put("title", "");
                    row.put("company", "");
                    row.put("category", "");
                    row.put("employmentType", "");
                    row.put("location", "");
                    row.put("startDate", "");
                    row.put("endDate", "");
                    row.put("details", legacy);
                } catch (Exception ignored) {}
                rows.add(row);
            }
        }
        loading = false;
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFFF5F8F9);
        setContentView(scroll);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(32));
        scroll.addView(page);

        page.addView(button("‹ Back to CV builder", false, () -> { persist(); finish(); }),
            new LinearLayout.LayoutParams(dp(190), dp(44)));
        TextView heading = text("Work Experience", 27, INK, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(20);
        page.addView(heading, hp);
        TextView intro = text("Add each job, internship, freelance role or volunteer position separately. Use real responsibilities and achievements; do not invent results.", 13, MUTED, false);
        intro.setPadding(0, dp(8), 0, dp(16));
        page.addView(intro);

        LinearLayout tip = new LinearLayout(this);
        tip.setPadding(dp(13), dp(12), dp(13), dp(12));
        tip.setBackground(shape(0xFFE8F3F0, 12, 0));
        tip.addView(text("Tip: Start each achievement with an action verb and add a number only when you can verify it.", 12, 0xFF316558, false));
        page.addView(tip);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        page.addView(list, lp);

        page.addView(button("+ Add another experience", true, this::addRow),
            new LinearLayout.LayoutParams(-1, dp(50)));
        status = text("Saved privately on this device.", 12, MUTED, false);
        status.setPadding(dp(2), dp(12), dp(2), dp(8));
        page.addView(status);
        page.addView(button("Save & return", false, () -> { persist(); finish(); }),
            new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void renderRows() {
        list.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = text("No work experience added yet. You can leave this section empty if you are a student or fresher.", 13, MUTED, false);
            empty.setPadding(dp(13), dp(18), dp(13), dp(18));
            list.addView(empty);
        }
        for (int index = 0; index < rows.size(); index++) {
            JSONObject row = rows.get(index);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            card.setBackground(shape(Color.WHITE, 14, 0xFFE1EAED));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(13);
            list.addView(card, cp);

            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(titleRow);
            titleRow.addView(text("Experience " + (index + 1), 15, INK, true),
                new LinearLayout.LayoutParams(0, -2, 1));
            titleRow.addView(button("Remove", false, () -> removeRow(row)),
                new LinearLayout.LayoutParams(dp(90), dp(39)));

            addInput(card, row, "Job title / position", "e.g. Accounts Assistant", "title", false);
            addInput(card, row, "Company / organization", "Employer or client name", "company", false);
            addSpinner(card, row, "Industry / job category", "category", CATEGORIES);
            addSpinner(card, row, "Employment type", "employmentType", TYPES);
            addInput(card, row, "Location", "City, country or Remote", "location", false);

            LinearLayout dates = new LinearLayout(this);
            dates.setOrientation(LinearLayout.HORIZONTAL);
            card.addView(dates);
            addInput(dates, row, "Start date", "MM/YYYY", "startDate", false);
            addInput(dates, row, "End date", "MM/YYYY or Present", "endDate", false);

            addInput(card, row, "Responsibilities & achievements", "One achievement per line. Describe what you did and the result, if known.", "details", true);
        }
    }

    private void addInput(LinearLayout parent, JSONObject row, String title, String hint, String key, boolean multiline) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        boolean horizontal = parent.getOrientation() == LinearLayout.HORIZONTAL;
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(horizontal ? 0 : -1, -2, horizontal ? 1f : 0f);
        wp.topMargin = dp(10);
        parent.addView(wrap, wp);
        wrap.addView(text(title, 12, INK, true));
        EditText input = new EditText(this);
        input.setText(row.optString(key, ""));
        input.setHint(hint);
        input.setTextSize(14);
        input.setTextColor(INK);
        input.setHintTextColor(0xFFA0AFB5);
        input.setPadding(dp(11), dp(9), dp(11), dp(9));
        input.setBackground(shape(0xFFF8FAFB, 9, 0xFFE3EAEC));
        input.setSingleLine(!multiline);
        if (multiline) {
            input.setMinLines(4);
            input.setGravity(Gravity.TOP);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        wrap.addView(input, new LinearLayout.LayoutParams(-1, -2));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable value) {
                if (loading) return;
                try { row.put(key, value.toString()); } catch (Exception ignored) {}
                scheduleSave();
            }
        });
    }

    private void addSpinner(LinearLayout parent, JSONObject row, String title, String key, String[] options) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.topMargin = dp(10);
        parent.addView(wrap, wp);
        wrap.addView(text(title, 12, INK, true));
        Spinner spinner = new Spinner(this);
        String[] values = new String[options.length + 1];
        values[0] = "Choose " + title.toLowerCase();
        System.arraycopy(options, 0, values, 1, options.length);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String current = row.optString(key, "");
        for (int i = 0; i < options.length; i++) if (options[i].equals(current)) spinner.setSelection(i + 1);
        wrap.addView(spinner, new LinearLayout.LayoutParams(-1, dp(48)));
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (loading) return;
                try { row.put(key, position == 0 ? "" : options[position - 1]); } catch (Exception ignored) {}
                scheduleSave();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void addRow() {
        JSONObject row = new JSONObject();
        try { row.put("id", "work-" + System.nanoTime()); } catch (Exception ignored) {}
        rows.add(row);
        renderRows();
        scheduleSave();
    }

    private void removeRow(JSONObject row) {
        new AlertDialog.Builder(this)
            .setTitle("Remove this experience?")
            .setMessage("This job or experience entry will be removed from the CV.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove", (dialog, which) -> {
                rows.remove(row);
                renderRows();
                scheduleSave();
            }).show();
    }

    private void scheduleSave() {
        if (loading) return;
        status.setText("Saving your work experience...");
        handler.removeCallbacks(pendingSave);
        handler.postDelayed(pendingSave, 500);
    }

    private String mirrorText() {
        StringBuilder result = new StringBuilder();
        for (JSONObject row : rows) {
            ArrayList<String> meta = new ArrayList<>();
            String title = row.optString("title", "").trim();
            String company = row.optString("company", "").trim();
            if (!title.isEmpty()) result.append(title);
            if (!company.isEmpty()) result.append(result.length() > 0 && result.charAt(result.length()-1) != '\n' ? " — " : "").append(company);
            String category = row.optString("category", "");
            String type = row.optString("employmentType", "");
            String location = row.optString("location", "");
            String start = row.optString("startDate", "");
            String end = row.optString("endDate", "");
            if (!category.isEmpty()) meta.add(category);
            if (!type.isEmpty()) meta.add(type);
            if (!location.isEmpty()) meta.add(location);
            if (!start.isEmpty() || !end.isEmpty()) meta.add((start + " – " + end).trim());
            if (!meta.isEmpty()) result.append("\n").append(String.join(" • ", meta));
            String details = row.optString("details", "").trim();
            if (!details.isEmpty()) result.append("\n").append(details);
            result.append("\n\n");
        }
        return result.toString().trim();
    }

    private void persist() {
        handler.removeCallbacks(pendingSave);
        try {
            JSONArray structured = new JSONArray();
            for (JSONObject row : rows) structured.put(row);
            resume.put("experienceEntries", structured);
            resume.put("experience", mirrorText());
            JSONArray old = readItems();
            JSONArray next = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item != null && !resumeId.equals(item.optString("id"))) next.put(item);
            }
            next.put(resume);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_ITEMS, next.toString())
                .putString(KEY_CURRENT, resumeId)
                .apply();
            if (status != null) status.setText("Saved privately on this device.");
        } catch (Exception e) {
            if (status != null) status.setText("Could not save work experience.");
        }
    }

    @Override protected void onPause() {
        persist();
        super.onPause();
    }
}
