package com.craftcv.resumestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional online AI assistance.
 * Only explicitly selected CV writing fields are sent over HTTPS.
 * All local CV editing, saving and exports continue to work offline.
 */
public final class AiAssistantActivity extends Activity {
    private static final String ENDPOINT =
        "https://craftcv-hees13.v2.appdeploy.ai/api/resume-assistant";
    private static final String PREFS = "craftcv_native_resumes_v1";
    private static final String[] MODES = {
        "Rewrite my summary",
        "Improve experience bullets",
        "Improve project descriptions",
        "Compare with a job description"
    };
    private static final String[] API_MODES = {
        "summary", "bullets", "projects", "review"
    };
    private static final int INK = 0xFF203E49;
    private static final int TEAL = 0xFF226E64;
    private static final int MUTED = 0xFF718995;
    private static final int TIMEOUT_MS = 30000;
    private static final int MAX_RESPONSE_CHARS = 65536;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private EditText role, summary, skills, experience, projects, jobDescription;
    private Spinner mode;
    private TextView response, status;
    private Button generate, retry, useSummary, copy;
    private ProgressBar progress;
    private String lastResult = "";
    private boolean working = false;
    private JSONObject currentResume = new JSONObject();
    private volatile HttpURLConnection activeConnection;

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(12));
        return d;
    }

    private TextView label(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(INK);
        t.setLineSpacing(dp(2), 1f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String caption, boolean primary, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(caption);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinHeight(dp(48));
        b.setTextColor(primary ? Color.WHITE : TEAL);
        b.setBackgroundTintList(null);
        b.setBackground(shape(primary ? TEAL : 0xFFEAF4F1));
        b.setOnClickListener(click);
        return b;
    }

    private EditText field(LinearLayout parent, String name, String value, boolean tall) {
        TextView heading = label(name, 13, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(16);
        hp.bottomMargin = dp(5);
        parent.addView(heading, hp);

        EditText e = new EditText(this);
        e.setText(value);
        e.setTextSize(16);
        e.setTextColor(INK);
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setBackground(shape(Color.WHITE));
        e.setSingleLine(!tall);
        if (tall) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP);
            e.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        } else {
            e.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void loadResume() {
        try {
            JSONArray rows = new JSONArray(prefs().getString("resumes", "[]"));
            String id = prefs().getString("current", "");
            JSONObject first = rows.optJSONObject(0);
            currentResume = first == null ? new JSONObject() : first;
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null && id.equals(row.optString("id"))) {
                    currentResume = row;
                    break;
                }
            }
        } catch (Exception ignored) {
            currentResume = new JSONObject();
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF143D4D);
        getWindow().setNavigationBarColor(0xFF143D4D);
        loadResume();
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFFF4F8F8);
        setContentView(scroll);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(17), dp(18), dp(36));
        scroll.addView(page);

        Button back = button("‹ Dashboard", false, v -> finish());
        page.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView heading = label("AI Resume Assistant", 27, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(20);
        page.addView(heading, hp);

        TextView intro = label(
            "Improve your summary, work experience, projects and job matching. Your offline CV stays available.",
            14, false);
        intro.setTextColor(MUTED);
        intro.setPadding(0, dp(7), 0, dp(14));
        page.addView(intro);

        LinearLayout note = new LinearLayout(this);
        note.setPadding(dp(14), dp(12), dp(14), dp(12));
        note.setBackground(shape(0xFFE4F2ED));
        page.addView(note, new LinearLayout.LayoutParams(-1, -2));
        TextView privacy = label(
            "INTERNET REQUIRED • SENT ONLY WHEN YOU TAP GENERATE\n" +
            "Only the role, summary, skills, experience, projects and optional job description are sent. " +
            "Your name, email, phone and photo are not included. Review suggestions before applying.",
            12, false);
        privacy.setTextColor(0xFF316558);
        note.addView(privacy);

        TextView action = label("What should AI help with?", 14, true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(20);
        page.addView(action, ap);

        mode = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, MODES);
        mode.setAdapter(adapter);
        page.addView(mode, new LinearLayout.LayoutParams(-1, dp(52)));

        role = field(page, "Target job title", currentResume.optString("role", ""), false);
        summary = field(page, "Current professional summary",
            currentResume.optString("summary", ""), true);

        skills = field(page, "Skills and expertise", skillsText(), true);
        experience = field(page, "Work experience notes", experienceText(), true);
        projects = field(page, "Project notes", projectsText(), true);
        jobDescription = field(page, "Target job description (required for comparison)", "", true);

        generate = button("Generate AI suggestions", true, v -> generate());
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(54));
        gp.topMargin = dp(19);
        page.addView(generate, gp);

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(32), dp(32));
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        pp.topMargin = dp(14);
        page.addView(progress, pp);

        status = label("AI is optional. Your CV is saved locally on this phone.", 13, false);
        status.setTextColor(MUTED);
        status.setPadding(0, dp(11), 0, dp(8));
        status.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(status);

        retry = button("Retry AI request", false, v -> generate());
        retry.setVisibility(View.GONE);
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(-1, dp(49));
        page.addView(retry, retryParams);

        response = label("", 14, false);
        response.setLineSpacing(dp(3), 1.1f);
        response.setTextIsSelectable(true);
        response.setPadding(dp(15), dp(15), dp(15), dp(15));
        response.setBackground(shape(Color.WHITE));
        response.setVisibility(View.GONE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(12);
        page.addView(response, rp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ac = new LinearLayout.LayoutParams(-1, dp(53));
        ac.topMargin = dp(10);
        page.addView(actions, ac);
        copy = button("Copy", false, v -> copyText());
        useSummary = button("Use as CV summary", true, v -> confirmApply());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(51), 1);
        cp.rightMargin = dp(7);
        actions.addView(copy, cp);
        actions.addView(useSummary, new LinearLayout.LayoutParams(0, dp(51), 2));
        copy.setVisibility(View.GONE);
        useSummary.setVisibility(View.GONE);
    }

    private String skillsText() {
        JSONArray groups = currentResume.optJSONArray("skillGroups");
        if (groups == null || groups.length() == 0) return currentResume.optString("skills", "");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.optJSONObject(i);
            if (group == null) continue;
            String category = group.optString("category", "Skills");
            String value = group.optString("skills", "");
            if (!value.trim().isEmpty()) {
                if (result.length() > 0) result.append("\n");
                result.append(category).append(": ").append(value);
            }
        }
        return result.toString();
    }

    private String experienceText() {
        String legacy = currentResume.optString("experience", "");
        JSONArray rows = currentResume.optJSONArray("experienceEntries");
        if (rows == null || rows.length() == 0) return legacy;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            if (result.length() > 0) result.append("\n\n");
            result.append(row.optString("title", ""))
                .append(" — ").append(row.optString("company", ""))
                .append("\n").append(row.optString("category", ""))
                .append(" | ").append(row.optString("employmentType", ""))
                .append(" | ").append(row.optString("location", ""))
                .append("\n").append(row.optString("startDate", ""))
                .append(" – ").append(row.optString("endDate", ""))
                .append("\n").append(row.optString("details", ""));
        }
        return result.toString().trim().isEmpty() ? legacy : result.toString();
    }

    private String projectsText() {
        String legacy = currentResume.optString("projects", "");
        JSONArray rows = currentResume.optJSONArray("projectEntries");
        if (rows == null || rows.length() == 0) return legacy;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            if (result.length() > 0) result.append("\n\n");
            result.append(row.optString("title", ""))
                .append(" — ").append(row.optString("role", ""))
                .append("\n").append(row.optString("category", ""))
                .append(" | ").append(row.optString("organization", ""))
                .append("\n").append(row.optString("tools", ""))
                .append(" | ").append(row.optString("date", ""))
                .append("\n").append(row.optString("details", ""))
                .append("\n").append(row.optString("link", ""));
        }
        return result.toString().trim().isEmpty() ? legacy : result.toString();
    }

    private static String limited(String value, int limit) {
        String safe = value == null ? "" : value.trim();
        return safe.length() > limit ? safe.substring(0, limit) : safe;
    }

    private boolean isOnline() {
        ConnectivityManager manager =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private JSONArray selectedEntries(String key, String fallbackKey) {
        JSONArray saved = currentResume.optJSONArray(key);
        if (saved != null && saved.length() > 0) return saved;
        JSONArray fallback = new JSONArray();
        String legacy = limited(currentResume.optString(fallbackKey, ""), 1400);
        if (!legacy.isEmpty()) {
            JSONObject row = new JSONObject();
            try {
                row.put("title", "");
                row.put("org", "");
                row.put("details", legacy);
                fallback.put(row);
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private JSONObject buildRequest(String apiMode) throws Exception {
        JSONObject request = new JSONObject();
        request.put("mode", apiMode);
        request.put("role", limited(role.getText().toString(), 150));
        request.put("summary", limited(summary.getText().toString(), 2500));
        request.put("skills", limited(skills.getText().toString(), 2500));
        request.put("experience", selectedEntries("experienceEntries", "experience"));
        request.put("projects", selectedEntries("projectEntries", "projects"));
        request.put("jobDescription", limited(jobDescription.getText().toString(), 5000));
        return request;
    }

    private void generate() {
        if (working) return;

        if (!isOnline()) {
            status.setText("You are offline. Your CV is saved, try AI again when connected.");
            retry.setVisibility(View.VISIBLE);
            return;
        }

        int selected = mode.getSelectedItemPosition();
        final String apiMode = API_MODES[Math.max(0, Math.min(selected, API_MODES.length - 1))];

        if ("review".equals(apiMode) &&
            limited(jobDescription.getText().toString(), 5000).isEmpty()) {
            jobDescription.setError("Paste the job description to compare your CV");
            jobDescription.requestFocus();
            return;
        }
        if ("bullets".equals(apiMode) &&
            limited(experience.getText().toString(), 1400).isEmpty()) {
            experience.setError("Add real work experience notes first");
            experience.requestFocus();
            return;
        }
        if ("projects".equals(apiMode) &&
            limited(projects.getText().toString(), 1400).isEmpty()) {
            projects.setError("Add a real project description first");
            projects.requestFocus();
            return;
        }
        if ("summary".equals(apiMode) &&
            limited(role.getText().toString(), 150).isEmpty() &&
            limited(summary.getText().toString(), 2500).isEmpty() &&
            limited(skills.getText().toString(), 2500).isEmpty() &&
            limited(experience.getText().toString(), 1400).isEmpty()) {
            status.setText("Add your target role, skills or real experience before generating a summary.");
            return;
        }

        final JSONObject request;
        try {
            request = buildRequest(apiMode);
        } catch (Exception e) {
            status.setText("Could not prepare your request. Please try again.");
            return;
        }

        working = true;
        generate.setEnabled(false);
        generate.setText("Generating…");
        progress.setVisibility(View.VISIBLE);
        retry.setVisibility(View.GONE);
        lastResult = "";
        response.setVisibility(View.GONE);
        copy.setVisibility(View.GONE);
        useSummary.setVisibility(View.GONE);
        status.setText("Checking your CV and preparing suggestions…");

        executor.execute(() -> {
            String answer = null;
            String failure = null;
            try {
                answer = requestWithRetry(request);
            } catch (Exception e) {
                failure = friendlyError(e);
            }

            final String result = answer;
            final String error = failure;
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                working = false;
                generate.setEnabled(true);
                generate.setText("Generate AI suggestions");
                progress.setVisibility(View.GONE);

                if (result != null && !result.trim().isEmpty()) {
                    lastResult = result;
                    status.setText("Suggestions ready. Check every statement before applying.");
                    response.setText(result);
                    response.setVisibility(View.VISIBLE);
                    copy.setVisibility(View.VISIBLE);
                    useSummary.setVisibility("summary".equals(apiMode) ? View.VISIBLE : View.GONE);
                    retry.setVisibility(View.GONE);
                } else {
                    status.setText(error == null
                        ? "AI could not respond. Your CV data was not changed. Please retry."
                        : error);
                    retry.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private String requestWithRetry(JSONObject request) throws Exception {
        Exception lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                activeConnection = connection;
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");
                byte[] body = request.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                    output.flush();
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
                String raw = stream == null ? "" : readResponse(stream);

                JSONObject payload;
                try {
                    payload = new JSONObject(raw);
                } catch (Exception badJson) {
                    throw new IllegalStateException("The AI server returned an unreadable response. Please retry.");
                }

                if (code < 200 || code >= 300 || !payload.optBoolean("ok", false)) {
                    String message = payload.optString("error", "AI could not complete the request.");
                    String errorCode = payload.optString("code", "AI_ERROR");
                    boolean temporary = code == 429 &&
                        (message.toLowerCase().contains("busy") ||
                         message.toLowerCase().contains("temporarily"));
                    temporary = temporary || code == 500 || code == 502 || code == 503 || code == 504;
                    if (attempt == 0 && temporary) {
                        lastError = new IllegalStateException(message);
                        Thread.sleep(700);
                        continue;
                    }
                    throw new ServerException(message, code, errorCode);
                }

                String result = payload.optString("text", "").trim();
                if (result.isEmpty()) {
                    String aiSummary = payload.optString("summary", "").trim();
                    JSONArray suggestions = payload.optJSONArray("suggestions");
                    StringBuilder formatted = new StringBuilder(aiSummary);
                    if (suggestions != null) {
                        for (int i = 0; i < suggestions.length(); i++) {
                            String item = suggestions.optString(i, "").trim();
                            if (!item.isEmpty()) {
                                if (formatted.length() > 0) formatted.append("\n");
                                formatted.append("• ").append(item);
                            }
                        }
                    }
                    result = formatted.toString().trim();
                }
                if (result.isEmpty()) throw new IllegalStateException("The AI service returned no suggestions.");
                return limited(result, 24000);
            } catch (ServerException e) {
                lastError = e;
                throw e;
            } catch (Exception e) {
                lastError = e;
                boolean retryable = e instanceof java.io.IOException ||
                    e instanceof java.net.SocketTimeoutException;
                if (attempt == 0 && retryable) {
                    Thread.sleep(700);
                    continue;
                }
                throw e;
            } finally {
                if (connection != null) connection.disconnect();
                activeConnection = null;
            }
        }
        throw lastError == null
            ? new IllegalStateException("AI could not complete the request.")
            : lastError;
    }

    private static final class ServerException extends Exception {
        final int statusCode;
        final String errorCode;

        ServerException(String message, int statusCode, String errorCode) {
            super(message);
            this.statusCode = statusCode;
            this.errorCode = errorCode;
        }
    }

    private static String readResponse(InputStream input) throws Exception {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] chunk = new char[2048];
            int read;
            while ((read = reader.read(chunk)) != -1) {
                text.append(chunk, 0, read);
                if (text.length() > MAX_RESPONSE_CHARS) {
                    throw new IllegalStateException("AI response is too large");
                }
            }
        }
        return text.toString();
    }

    private String friendlyError(Exception error) {
        if (!isOnline()) {
            return "You are offline. Your CV is saved, try AI again when connected.";
        }
        if (error instanceof ServerException) {
            ServerException server = (ServerException) error;
            if (server.statusCode == 429 || "RATE_LIMIT".equals(server.errorCode)) {
                return "AI is busy or this connection has reached its request limit. Please retry shortly.";
            }
            if (server.statusCode == 400 || "BAD_INPUT".equals(server.errorCode)) {
                return error.getMessage();
            }
            if (server.statusCode >= 500) {
                return "AI is temporarily unavailable. Your CV is unchanged. Please retry.";
            }
        }
        if (error instanceof java.net.SocketTimeoutException ||
            (error.getMessage() != null && error.getMessage().toLowerCase().contains("timeout"))) {
            return "AI took too long to respond. Check your connection and retry.";
        }
        return "AI could not connect. Check your internet connection and try again. Your CV has not been changed.";
    }

    private void copyText() {
        if (lastResult.isEmpty()) return;
        ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clip != null) {
            clip.setPrimaryClip(ClipData.newPlainText("CraftCV AI suggestions", lastResult));
            Toast.makeText(this, "Suggestions copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmApply() {
        if (lastResult.isEmpty()) return;
        new AlertDialog.Builder(this)
            .setTitle("Use this AI summary?")
            .setMessage("Check the suggested summary for accuracy first. AI can make mistakes. Only your CV summary will change.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply", (dialog, which) -> applySummary())
            .show();
    }

    private void applySummary() {
        try {
            JSONArray old = new JSONArray(prefs().getString("resumes", "[]"));
            String selected = prefs().getString("current", "");
            JSONObject item = null;
            for (int i = 0; i < old.length(); i++) {
                JSONObject row = old.optJSONObject(i);
                if (row != null && selected.equals(row.optString("id"))) {
                    item = row;
                    break;
                }
            }
            if (item == null && old.length() > 0) item = old.optJSONObject(0);
            if (item == null) {
                Toast.makeText(this, "Create a CV first to apply your summary.", Toast.LENGTH_LONG).show();
                return;
            }
            item.put("summary", lastResult);
            prefs().edit()
                .putString("resumes", old.toString())
                .putString("current", item.optString("id"))
                .apply();
            Toast.makeText(this, "Summary saved to your selected CV.", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            status.setText("Could not apply summary. You can copy it instead.");
        }
    }

    @Override protected void onDestroy() {
        HttpURLConnection connection = activeConnection;
        if (connection != null) connection.disconnect();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
