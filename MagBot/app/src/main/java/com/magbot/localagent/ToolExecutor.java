package com.magbot.localagent;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.widget.Toast;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Dispatches a previously validated, explicitly confirmed action. Never executes model code. */
final class ToolExecutor {
    private final Activity activity;
    ToolExecutor(Activity activity) { this.activity = activity; }

    String execute(ActionValidator.Action a) {
        switch (a.tool) {
            case "set_alarm":
                launch(new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, a.number("hour"))
                        .putExtra(AlarmClock.EXTRA_MINUTES, a.number("minute"))
                        .putExtra(AlarmClock.EXTRA_MESSAGE, a.text("label"))
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, false), "No compatible Clock app.");
                return "Alarm request handed to Clock. Verify the time and date there.";
            case "set_timer":
                launch(new Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, a.number("seconds"))
                        .putExtra(AlarmClock.EXTRA_MESSAGE, a.text("label"))
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, false), "No compatible timer app.");
                return "Timer request handed to Clock. Verify or start it there.";
            case "create_calendar_event": {
                ZoneId zone = ZoneId.systemDefault();
                long start = LocalDateTime.parse(a.text("start")).atZone(zone).toInstant().toEpochMilli();
                long end = LocalDateTime.parse(a.text("end")).atZone(zone).toInstant().toEpochMilli();
                launch(new Intent(Intent.ACTION_INSERT)
                        .setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.item/event")
                        .putExtra(CalendarContract.Events.TITLE, a.text("title"))
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                        .putExtra(CalendarContract.Events.DESCRIPTION, a.optional("description"))
                        .putExtra(CalendarContract.Events.EVENT_LOCATION, a.optional("location")),
                        "No compatible calendar app. Install/open your calendar app first.");
                return "Event draft opened. Review the calendar, time and details, then tap Save.";
            }
            case "create_note":
                return createKeepNote(a);
            case "whatsapp_message":
                return composeWhatsApp(a);
            default:
                throw new IllegalArgumentException("Unsupported action.");
        }
    }

    private String createKeepNote(ActionValidator.Action a) {
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, a.text("title"))
                .putExtra(Intent.EXTRA_TITLE, a.text("title"))
                .putExtra(Intent.EXTRA_TEXT, a.text("text"));
        try {
            activity.startActivity(new Intent(share).setPackage("com.google.android.keep"));
            return "Google Keep draft opened. Check title/text and tap Save in Keep.";
        } catch (ActivityNotFoundException missingKeep) {
            launch(Intent.createChooser(share, "Choose a notes app"), "No app can receive a text note.");
            return "Keep was unavailable. Choose a notes app, review and save the draft.";
        }
    }

    private String composeWhatsApp(ActionValidator.Action a) {
        Intent intent;
        if (!a.text("phone").isEmpty()) {
            Uri uri = Uri.parse("https://wa.me/" + a.text("phone")).buildUpon()
                    .appendQueryParameter("text", a.text("message")).build();
            intent = new Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp");
        } else {
            intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .setPackage("com.whatsapp").putExtra(Intent.EXTRA_TEXT, a.text("message"));
        }
        launch(intent, "Standard WhatsApp is unavailable. This prototype does not target WhatsApp Business.");
        if (a.text("phone").isEmpty()) {
            Toast.makeText(activity, "Choose " + a.text("contact") + " in WhatsApp", Toast.LENGTH_LONG).show();
        }
        return "Message handed to WhatsApp. Verify the recipient and press Send yourself. Sending needs connectivity.";
    }

    private void launch(Intent intent, String error) {
        try { activity.startActivity(intent); }
        catch (ActivityNotFoundException e) { throw new IllegalStateException(error, e); }
    }
}
