package com.magbot.localagent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile LlmClient activeClient;
    private EditText keyInput;
    private EditText commandInput;
    private TextView status;
    private LinearLayout actionList;
    private Button planButton;
    private Button checkButton;
    private SharedPreferences preferences;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("local_settings", MODE_PRIVATE);
        setContentView(buildUi());
        if (state != null) commandInput.setText(state.getString("command", ""));
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(17, 19, 24));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                root.setPadding(dp(20) + safe.left, dp(20) + safe.top,
                        dp(20) + safe.right, dp(24) + safe.bottom);
            } else {
                root.setPadding(dp(20), dp(20) + insets.getSystemWindowInsetTop(),
                        dp(20), dp(24) + insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        add(root, text("MagBot", 34), 0);
        add(root, text("Local Android agent | v0.2", 16), 4);
        add(root, text("Nothing Phone (3a) setup | Termux + Google Keep", 13), 8);
        add(root, text("MODEL RUNS ON THIS PHONE\n127.0.0.1:8080 | no cloud LLM endpoint", 14), 18);
        add(root, text("Local API key (printed in Termux)", 14), 22);
        keyInput = edit(preferences.getString("api_key", ""), 15);
        keyInput.setSingleLine(true);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setSaveEnabled(false);
        add(root, keyInput, 6);
        checkButton = button("Check connection");
        checkButton.setOnClickListener(v -> request(true));
        add(root, checkButton, 8);
        add(root, text("What should MagBot prepare?", 16), 22);
        commandInput = edit("Start a 1 minute timer called Demo", 18);
        commandInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        commandInput.setMinLines(3);
        commandInput.setGravity(Gravity.TOP | Gravity.START);
        add(root, commandInput, 8);
        planButton = button("Prepare actions locally");
        planButton.setOnClickListener(v -> request(false));
        add(root, planButton, 12);
        status = text("Start the server in Termux, paste its local key, then check the connection.", 15);
        status.setTextIsSelectable(true);
        add(root, status, 16);
        actionList = new LinearLayout(this);
        actionList.setOrientation(LinearLayout.VERTICAL);
        add(root, actionList, 16);
        add(root, text("Examples - tap to copy, then prepare", 15), 24);
        String[] examples = {
                "Start a 1 minute timer called Demo",
                "Set an alarm for 7 AM called Gym",
                "Add Project Review tomorrow at 4 PM for 45 minutes",
                "Make a note titled Shopping: buy cement and paint",
                "WhatsApp Arun saying I will reach by 8",
                "Set an alarm for 6:30 AM and make a note to carry my project report"
        };
        for (String example : examples) {
            Button b = button(example);
            b.setTextSize(13);
            b.setOnClickListener(v -> commandInput.setText(example));
            add(root, b, 4);
        }
        add(root, text("No actions run automatically. Calendar and Keep need Save; WhatsApp needs Send. "
                + "This version cannot read existing calendars, Keep notes or chats.\n"
                + "Returning from another app preserves the current screen; after rotation or app restart, "
                + "prepare a fresh plan. No command history is saved by MagBot.", 12), 22);
        return scroll;
    }

    private void request(boolean checkOnly) {
        final String command = commandInput.getText().toString().trim();
        if (!checkOnly && (command.isEmpty() || command.length() > 1500)) {
            status.setText("Enter a command between 1 and 1500 characters.");
            return;
        }
        final LlmClient client;
        try { client = new LlmClient(keyInput.getText().toString()); }
        catch (Exception e) { status.setText(e.getMessage()); return; }
        preferences.edit().putString("api_key", keyInput.getText().toString().trim()).apply();
        activeClient = client;
        busy(true);
        actionList.removeAllViews();
        status.setText(checkOnly ? "Checking the local server..." : "Preparing actions on this phone...");
        worker.submit(() -> {
            try {
                long started = System.nanoTime();
                if (checkOnly) {
                    client.checkConnection();
                    post(() -> status.setText("Connected to the local server. Ready to prepare a command."));
                } else {
                    String raw = client.complete(command);
                    ActionPlan plan = ActionPlan.parse(raw, command);
                    long milliseconds = (System.nanoTime() - started) / 1000000L;
                    post(() -> showPlan(plan, command, milliseconds));
                }
            } catch (Exception e) {
                post(() -> status.setText("Nothing was executed.\n" + e.getMessage()
                        + "\n\nCheck Termux is running and the API key matches. "
                        + "Use a complete, simple command if the output was rejected."));
            } finally {
                activeClient = null;
                post(() -> busy(false));
            }
        });
    }

    private void showPlan(ActionPlan plan, String command, long elapsedMs) {
        actionList.removeAllViews();
        status.setText("Local model response (" + elapsedMs + " ms):\n" + plan.reply
                + "\n\n" + (plan.actions.isEmpty() ? "No actions were proposed."
                : "Proposed actions only. Review every field below before confirming."));
        int index = 1;
        for (ActionValidator.Action action : plan.actions) {
            final String summary = ActionValidator.summary(action, ZonedDateTime.now());
            TextView preview = text(index + ". " + summary, 15);
            preview.setTextIsSelectable(true);
            add(actionList, preview, 16);
            Button launch = button("Review and open action " + index);
            launch.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Confirm this action")
                    .setMessage(summary)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open app", (dialog, which) -> {
                        try {
                            // Check dates again: a plan may have been left open for a while.
                            ActionValidator.validate(action, command, ZonedDateTime.now());
                            String result = new ToolExecutor(this).execute(action);
                            launch.setEnabled(false);
                            launch.setText("Handed to app - not verified saved");
                            status.setText(result + "\n\nReturn to MagBot for any remaining actions.");
                        } catch (Exception e) {
                            status.setText("Could not open this action: " + e.getMessage());
                        }
                    }).show());
            add(actionList, launch, 6);
            index++;
        }
    }

    private void busy(boolean value) {
        planButton.setEnabled(!value);
        checkButton.setEnabled(!value);
        keyInput.setEnabled(!value);
        commandInput.setEnabled(!value);
    }
    private void post(Runnable work) {
        runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) work.run(); });
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(225, 230, 239));
        return view;
    }
    private EditText edit(String value, int size) {
        EditText view = new EditText(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(Color.rgb(35, 39, 48));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        return view;
    }
    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        return b;
    }
    private void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        parent.addView(child, p);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("command", commandInput.getText().toString());
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        LlmClient client = activeClient;
        if (client != null) client.cancel();
        worker.shutdownNow();
        super.onDestroy();
    }
}
