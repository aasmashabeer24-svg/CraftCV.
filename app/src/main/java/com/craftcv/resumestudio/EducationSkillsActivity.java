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
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Structured, repeatable education and categorized skill editor.
 * Data is stored in the same local resume record and mirrored into legacy
 * text fields so existing PDF, DOCX, preview and cover-letter exports continue
 * to work without a format migration or loss of old resumes.
 */
public final class EducationSkillsActivity extends Activity {
    private static final String PREFS = "craftcv_native_resumes_v1";
    private static final String KEY_ITEMS = "resumes";
    private static final String KEY_CURRENT = "current";
    private static final String[] QUALIFICATIONS = {
        "Primary / Middle School", "Matric / SSC", "O Levels / A Levels",
        "Intermediate / HSSC", "FA / FSc / ICS / I.Com", "F.A. / F.Sc. Pre-Medical",
        "F.Sc. Pre-Engineering", "ICS / Computer Science", "I.Com / Commerce",
        "DAE / Diploma of Associate Engineering", "Higher National Diploma",
        "Vocational / Trade Certificate", "Certificate / Short Course",
        "Associate Degree", "Bachelor's / BS", "B.Com / BBA", "BSc Engineering",
        "CA / ACCA / ICMA", "Master's / MA / MSc / MBA", "MS / MPhil",
        "Postgraduate Diploma", "PGCert / PGDip", "PhD / Doctorate",
        "MBBS / BDS / DPT / Allied Health", "Nursing / Pharmacy", "LLB / Law",
        "Architecture / Design", "Professional Certificate",
        "Religious / Madrasa Qualification", "Other / Custom"
    };
    private static final String[] SKILL_CATEGORIES = {
        "Programming & Development", "IT & Technical Support", "Data & Analytics",
        "Cybersecurity", "Cloud & DevOps", "Software Testing & QA", "Database Management",
        "Networking", "Soft Skills", "Leadership & Management", "Project Management",
        "Tools & Software", "Finance & Accounting", "Business & Administration",
        "Sales & Business Development", "Customer Service", "Marketing & Social Media",
        "Design & UX", "Creative & Multimedia", "Writing & Content",
        "Education & Training", "Healthcare", "Engineering & Trades",
        "Research & Analysis", "Languages", "Logistics & Supply Chain",
        "Hospitality & Retail", "Other / Custom"
    };

    private final ArrayList<JSONObject> education = new ArrayList<>();
    private final ArrayList<JSONObject> skillGroups = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSave = () -> persist(false);
    private String resumeId = "";
    private JSONObject resume = new JSONObject();
    private LinearLayout educationList;
    private LinearLayout skillsList;
    private TextView status;
    private boolean loading;

    private interface Changed {
        void onChange(String value);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        resumeId = getIntent().getStringExtra("resume_id");
        if (resumeId == null) resumeId = "";
        if (!loadResume()) {
            Toast.makeText(this, "Could not find this saved CV. Please reopen it.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        loadStructuredRows();
        buildUI();
        renderEducation();
        renderSkills();
        loading = false;
        persist(false);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private JSONArray readItems() {
        try { return new JSONArray(prefs().getString(KEY_ITEMS, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private boolean loadResume() {
        JSONArray items = readItems();
        if (resumeId.isEmpty()) resumeId = prefs().getString(KEY_CURRENT, "");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null && resumeId.equals(item.optString("id"))) {
                resume = item;
                return true;
            }
        }
        return false;
    }

    private void loadStructuredRows() {
        loading = true;
        JSONArray savedEducation = resume.optJSONArray("educationEntries");
        if (savedEducation != null && savedEducation.length() > 0) {
            for (int i = 0; i < savedEducation.length(); i++) {
                JSONObject row = savedEducation.optJSONObject(i);
                if (row != null) education.add(copy(row));
            }
        } else {
            String legacy = resume.optString("education", "").trim();
            if (!legacy.isEmpty()) {
                education.add(row("edu-legacy", "qualification", legacy));
            }
        }

        JSONArray savedSkills = resume.optJSONArray("skillGroups");
        if (savedSkills != null && savedSkills.length() > 0) {
            for (int i = 0; i < savedSkills.length(); i++) {
                JSONObject row = savedSkills.optJSONObject(i);
                if (row != null) skillGroups.add(copy(row));
            }
        } else {
            String legacy = resume.optString("skills", "").trim();
            if (!legacy.isEmpty()) {
                JSONObject group = row("skill-legacy", "category", "Core Skills");
                put(group, "skills", legacy);
                skillGroups.add(group);
            }
        }
        loading = false;
    }

    private JSONObject copy(JSONObject value) {
        try { return new JSONObject(value.toString()); }
        catch (Exception ignored) { return new JSONObject(); }
    }

    private JSONObject row(String id, String key, String value) {
        JSONObject item = new JSONObject();
        put(item, "id", id + "-" + System.nanoTime());
        put(item, key, value);
        return item;
    }

    private void put(JSONObject target, String key, String value) {
        try { target.put(key, value == null ? "" : value); }
        catch (Exception ignored) {}
    }

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
        view.setLineSpacing(dp(2), 1f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(primary ? Color.WHITE : 0xFF245A60);
        button.setBackgroundTintList(null);
        button.setBackground(shape(primary ? 0xFF246D65 : 0xFFEAF3F2, 11, 0));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF4F7F8);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(9), dp(14), dp(9));
        header.setBackgroundColor(Color.WHITE);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(62)));
        Button back = button("‹ Back", false, this::finish);
        header.addView(back, new LinearLayout.LayoutParams(dp(88), dp(43)));
        TextView title = text("Education & Skills", 18, 0xFF1B3D4A, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button save = button("Save", true, () -> persist(true));
        header.addView(save, new LinearLayout.LayoutParams(dp(82), dp(43)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(19), dp(16), dp(24));
        scroll.addView(body);

        addSection(body, "Education history",
            "Add every relevant qualification separately. Include school, college, university, diploma or professional certificate.");
        educationList = new LinearLayout(this);
        educationList.setOrientation(LinearLayout.VERTICAL);
        body.addView(educationList);
        Button addEducation = button("+ Add another education", true, this::addEducation);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(49));
        ep.bottomMargin = dp(27);
        body.addView(addEducation, ep);

        addSection(body, "Skills by category",
            "Create separate groups such as technical skills, soft skills, software, business skills and languages. Edit the examples to match your real abilities.");
        skillsList = new LinearLayout(this);
        skillsList.setOrientation(LinearLayout.VERTICAL);
        body.addView(skillsList);
        Button addSkills = button("+ Add skill category", true, this::chooseNewSkillCategory);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(49));
        sp.bottomMargin = dp(13);
        body.addView(addSkills, sp);

        Button custom = button("Add a custom skill category", false, this::addCustomSkillCategory);
        body.addView(custom, new LinearLayout.LayoutParams(-1, dp(45)));

        status = text("Changes save automatically on this phone.", 11, 0xFF4C8178, false);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(-1, -2);
        st.topMargin = dp(17);
        body.addView(status, st);

        TextView note = text("Your existing CV, PDF and Word exports remain compatible. Only the details you enter are included.", 11, 0xFF7C9097, false);
        note.setPadding(dp(4), dp(12), dp(4), 0);
        body.addView(note);
    }

    private void addSection(LinearLayout parent, String heading, String description) {
        parent.addView(text(heading, 21, 0xFF1B3D4A, true));
        TextView sub = text(description, 12, 0xFF718891, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(6);
        p.bottomMargin = dp(14);
        parent.addView(sub, p);
    }

    private LinearLayout card(LinearLayout parent, String title, Runnable remove) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(13), dp(13), dp(13), dp(13));
        box.setBackground(shape(Color.WHITE, 14, 0xFFE0E9EC));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        parent.addView(box, p);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(head, new LinearLayout.LayoutParams(-1, dp(38)));
        TextView name = text(title, 13, 0xFF315562, true);
        head.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        Button delete = button("Remove", false, remove);
        head.addView(delete, new LinearLayout.LayoutParams(dp(83), dp(36)));
        return box;
    }

    private EditText addInput(LinearLayout parent, String label, String hint,
                              String value, boolean multiline, Changed changed) {
        TextView caption = text(label, 11, 0xFF42616D, true);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(9);
        cp.bottomMargin = dp(5);
        parent.addView(caption, cp);

        EditText input = new EditText(this);
        input.setTextSize(14);
        input.setTextColor(0xFF203F4B);
        input.setHintTextColor(0xFFA1B0B6);
        input.setHint(hint);
        input.setPadding(dp(11), dp(9), dp(11), dp(9));
        input.setBackground(shape(0xFFF7FAFB, 9, 0xFFE3EAED));
        input.setSingleLine(!multiline);
        if (multiline) {
            input.setMinLines(3);
            input.setGravity(Gravity.TOP);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        input.setText(value == null ? "" : value);
        parent.addView(input, new LinearLayout.LayoutParams(-1, -2));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable editable) {
                if (loading) return;
                changed.onChange(editable.toString());
                scheduleSave();
            }
        });
        return input;
    }

    private void addEducation() {
        JSONObject row = new JSONObject();
        put(row, "id", "edu-" + System.nanoTime());
        education.add(row);
        renderEducation();
        scheduleSave();
    }

    private void renderEducation() {
        educationList.removeAllViews();
        if (education.isEmpty()) {
            TextView empty = text("No education added yet. Tap Add another education to enter your qualifications.", 12, 0xFF84969D, false);
            empty.setPadding(dp(12), dp(15), dp(12), dp(15));
            educationList.addView(empty);
        }
        for (JSONObject row : education) {
            String id = row.optString("id");
            LinearLayout box = card(educationList,
                row.optString("qualification", "Education entry").isEmpty() ? "Education entry" : row.optString("qualification"),
                () -> removeEducation(id));
            EditText qualification = addInput(box, "Qualification / degree",
                "Matric, Intermediate, DAE, BS, Bachelor's, Master's, MPhil, PhD...",
                row.optString("qualification"), false,
                value -> put(row, "qualification", value));
            Button types = button("Choose common qualification", false, () -> chooseQualification(qualification));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(41));
            tp.topMargin = dp(7);
            box.addView(types, tp);
            addInput(box, "Institution / school / college / university",
                "Name of your institution", row.optString("institution"), false,
                value -> put(row, "institution", value));
            addInput(box, "Field of study",
                "Computer Science, Commerce, Pre-Medical, Engineering...",
                row.optString("field"), false, value -> put(row, "field", value));
            addInput(box, "Dates",
                "2022 – 2026 or Expected 2027", row.optString("dates"), false,
                value -> put(row, "dates", value));
            addInput(box, "Grade / GPA (optional)",
                "3.6 GPA, A Grade, Distinction", row.optString("grade"), false,
                value -> put(row, "grade", value));
            addInput(box, "Additional details (optional)",
                "Relevant coursework, honours, achievements...", row.optString("notes"), true,
                value -> put(row, "notes", value));
        }
    }

    private void removeEducation(String id) {
        new AlertDialog.Builder(this).setTitle("Remove education entry?")
            .setMessage("This qualification will be removed from this CV.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove", (dialog, which) -> {
                for (int i = education.size() - 1; i >= 0; i--) {
                    if (id.equals(education.get(i).optString("id"))) education.remove(i);
                }
                renderEducation();
                scheduleSave();
            }).show();
    }

    private void chooseQualification(EditText target) {
        new AlertDialog.Builder(this).setTitle("Choose qualification type")
            .setItems(QUALIFICATIONS, (dialog, which) -> target.setText(QUALIFICATIONS[which]))
            .show();
    }

    private String suggestedSkills(String category) {
        if ("Technical Skills".equals(category)) return "Programming, Data Analysis, Troubleshooting, Research";
        if ("Soft Skills".equals(category)) return "Communication, Teamwork, Problem Solving, Time Management";
        if ("Tools & Software".equals(category)) return "Microsoft Excel, Word, PowerPoint, Google Workspace";
        if ("Business Skills".equals(category)) return "Customer Service, Sales, Marketing, Record Keeping";
        if ("Creative Skills".equals(category)) return "Graphic Design, Content Writing, Video Editing, Photography";
        if ("Languages".equals(category)) return "Urdu, English";
        if ("Healthcare Skills".equals(category)) return "Patient Care, Medical Terminology, First Aid, Documentation";
        if ("Engineering Skills".equals(category)) return "Technical Drawing, CAD, Quality Control, Safety Procedures";
        if ("Education & Training".equals(category)) return "Lesson Planning, Classroom Management, Assessment, Mentoring";
        if ("Customer Service".equals(category)) return "Active Listening, Complaint Resolution, CRM, Product Knowledge";
        return "";
    }

    private void chooseNewSkillCategory() {
        new AlertDialog.Builder(this).setTitle("Choose skill category")
            .setItems(SKILL_CATEGORIES, (dialog, which) -> {
                String category = SKILL_CATEGORIES[which];
                if ("Other / Custom".equals(category)) addCustomSkillCategory();
                else addSkillGroup(category, suggestedSkills(category));
            }).show();
    }

    private void addCustomSkillCategory() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("e.g. Accounting Skills");
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        new AlertDialog.Builder(this).setTitle("Custom category")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter a category name.", Toast.LENGTH_SHORT).show();
                } else addSkillGroup(name, "");
            }).show();
    }

    private void addSkillGroup(String category, String skills) {
        JSONObject row = new JSONObject();
        put(row, "id", "skill-" + System.nanoTime());
        put(row, "category", category);
        put(row, "skills", skills);
        skillGroups.add(row);
        renderSkills();
        scheduleSave();
    }

    private void renderSkills() {
        skillsList.removeAllViews();
        if (skillGroups.isEmpty()) {
            TextView empty = text("No skill categories yet. Add Technical Skills, Soft Skills, Tools, Languages or your own category.", 12, 0xFF84969D, false);
            empty.setPadding(dp(12), dp(15), dp(12), dp(15));
            skillsList.addView(empty);
        }
        for (JSONObject row : skillGroups) {
            String id = row.optString("id");
            LinearLayout box = card(skillsList,
                row.optString("category", "Skill category").isEmpty() ? "Skill category" : row.optString("category"),
                () -> removeSkillGroup(id));
            EditText category = addInput(box, "Category name",
                "Technical Skills, Soft Skills, Languages...", row.optString("category"), false,
                value -> put(row, "category", value));
            Button choose = button("Choose category", false, () -> chooseSkillCategory(category));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(40));
            cp.topMargin = dp(7);
            box.addView(choose, cp);
            addInput(box, "Skills (separate with commas or new lines)",
                "Excel, React Native, Communication, Data Analysis...",
                row.optString("skills"), true, value -> put(row, "skills", value));
        }
    }

    private void chooseSkillCategory(EditText target) {
        new AlertDialog.Builder(this).setTitle("Choose category")
            .setItems(SKILL_CATEGORIES, (dialog, which) -> {
                if (!"Other / Custom".equals(SKILL_CATEGORIES[which])) target.setText(SKILL_CATEGORIES[which]);
                else addCustomSkillCategory();
            }).show();
    }

    private void removeSkillGroup(String id) {
        new AlertDialog.Builder(this).setTitle("Remove skill category?")
            .setMessage("This group and its skills will be removed from this CV.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove", (dialog, which) -> {
                for (int i = skillGroups.size() - 1; i >= 0; i--) {
                    if (id.equals(skillGroups.get(i).optString("id"))) skillGroups.remove(i);
                }
                renderSkills();
                scheduleSave();
            }).show();
    }

    private void scheduleSave() {
        if (loading) return;
        status.setText("Saving your education and skills...");
        handler.removeCallbacks(pendingSave);
        handler.postDelayed(pendingSave, 550);
    }

    private String educationText() {
        ArrayList<String> entries = new ArrayList<>();
        for (JSONObject row : education) {
            ArrayList<String> lines = new ArrayList<>();
            add(lines, row.optString("qualification"));
            add(lines, prefixed("Institution", row.optString("institution")));
            add(lines, prefixed("Field of study", row.optString("field")));
            add(lines, prefixed("Dates", row.optString("dates")));
            add(lines, prefixed("Grade / GPA", row.optString("grade")));
            add(lines, row.optString("notes"));
            if (!lines.isEmpty()) entries.add(android.text.TextUtils.join("\n", lines));
        }
        return android.text.TextUtils.join("\n\n", entries);
    }

    private String skillsText() {
        ArrayList<String> lines = new ArrayList<>();
        for (JSONObject group : skillGroups) {
            String skills = group.optString("skills").trim();
            if (skills.isEmpty()) continue;
            String category = group.optString("category").trim();
            lines.add((category.isEmpty() ? "Skills" : category) + ": " + skills.replace("\n", ", "));
        }
        return android.text.TextUtils.join("\n", lines);
    }

    private void add(ArrayList<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) list.add(value.trim());
    }

    private String prefixed(String label, String value) {
        return value == null || value.trim().isEmpty() ? "" : label + ": " + value.trim();
    }

    private JSONArray toArray(ArrayList<JSONObject> rows) {
        JSONArray result = new JSONArray();
        for (JSONObject row : rows) {
            boolean hasContent = false;
            java.util.Iterator<String> keys = row.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!"id".equals(key) && !row.optString(key).trim().isEmpty()) {
                    hasContent = true;
                    break;
                }
            }
            if (hasContent) result.put(row);
        }
        return result;
    }

    private void persist(boolean showToast) {
        if (loading || resume == null || resume.length() == 0) return;
        handler.removeCallbacks(pendingSave);
        try {
            JSONArray savedEducation = toArray(education);
            JSONArray savedSkills = toArray(skillGroups);
            resume.put("educationEntries", savedEducation);
            resume.put("skillGroups", savedSkills);
            resume.put("education", educationText());
            resume.put("skills", skillsText());

            JSONArray old = readItems();
            JSONArray next = new JSONArray();
            boolean replaced = false;
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item == null) continue;
                if (resumeId.equals(item.optString("id"))) {
                    next.put(resume);
                    replaced = true;
                } else next.put(item);
            }
            if (!replaced) next.put(resume);
            prefs().edit().putString(KEY_ITEMS, next.toString())
                .putString(KEY_CURRENT, resumeId).apply();
            if (status != null) status.setText("Education and skills saved on this phone.");
            if (showToast) Toast.makeText(this, "Education and skills saved.", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            if (status != null) status.setText("Could not save these changes. Please try Save again.");
            if (showToast) Toast.makeText(this, "Save failed. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacks(pendingSave);
        persist(false);
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
