package com.magbot.localagent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure Java policy layer: no Android calls and no model-controlled executable code. */
final class ActionValidator {
    static final int MAX_ACTIONS = 6;
    private ActionValidator() {}

    static final class Action {
        final String tool;
        final Map<String, Object> args;
        Action(String tool, Map<String, Object> args) {
            this.tool = tool;
            this.args = Collections.unmodifiableMap(new LinkedHashMap<>(args));
        }
        String text(String key) { return (String) args.get(key); }
        String optional(String key) { return args.containsKey(key) ? text(key) : ""; }
        int number(String key) { return ((Number) args.get(key)).intValue(); }
    }

    static void validateAll(List<Action> actions, String command, ZonedDateTime now) {
        if (actions.size() > MAX_ACTIONS) fail("At most six actions are allowed per command.");
        for (Action action : actions) validate(action, command, now);
    }

    static void validate(Action a, String command, ZonedDateTime now) {
        if (a.tool == null) fail("Missing tool name.");
        switch (a.tool) {
            case "set_alarm": {
                keys(a, "hour", "minute", "label", "date");
                integer(a, "hour", 0, 23);
                integer(a, "minute", 0, 59);
                text(a, "label", 160, false);
                optionalText(a, "date", 10);
                if (!a.optional("date").isEmpty()) {
                    LocalDate requested = LocalDate.parse(a.optional("date"));
                    ZonedDateTime next = nextAlarm(a, now);
                    if (!requested.equals(next.toLocalDate())) {
                        fail("The Clock intent only sets the next occurrence of this time. "
                                + "Use a calendar event for that date instead.");
                    }
                }
                break;
            }
            case "set_timer":
                keys(a, "seconds", "label");
                integer(a, "seconds", 1, 86400);
                text(a, "label", 160, false);
                break;
            case "create_calendar_event": {
                keys(a, "title", "start", "end", "description", "location");
                text(a, "title", 200, true);
                text(a, "start", 32, true);
                text(a, "end", 32, true);
                optionalText(a, "description", 2000);
                optionalText(a, "location", 300);
                LocalDateTime start = LocalDateTime.parse(a.text("start"));
                LocalDateTime end = LocalDateTime.parse(a.text("end"));
                if (!end.isAfter(start)) fail("The event end must be after its start.");
                if (!start.atZone(now.getZone()).isAfter(now)) fail("The event start is in the past.");
                break;
            }
            case "create_note":
                keys(a, "title", "text");
                text(a, "title", 200, false);
                text(a, "text", 4000, true);
                break;
            case "whatsapp_message": {
                keys(a, "contact", "phone", "message");
                text(a, "contact", 200, false);
                text(a, "phone", 15, false);
                text(a, "message", 2000, true);
                String phone = a.text("phone");
                if (phone.isEmpty() && a.text("contact").trim().isEmpty()) {
                    fail("A WhatsApp recipient is required.");
                }
                if (!phone.isEmpty()) {
                    if (!phone.matches("[1-9][0-9]{5,14}")) {
                        fail("Use an international phone number including country code, digits only.");
                    }
                    // Never accept a phone number that the model invented.
                    if (!command.replaceAll("[^0-9]", "").contains(phone)) {
                        fail("The proposed phone number was not supplied in your command.");
                    }
                }
                break;
            }
            default: fail("Unsupported action: " + a.tool);
        }
    }

    static ZonedDateTime nextAlarm(Action a, ZonedDateTime now) {
        ZonedDateTime next = now.withHour(a.number("hour")).withMinute(a.number("minute"))
                .withSecond(0).withNano(0);
        return next.isAfter(now) ? next : next.plusDays(1);
    }

    static String summary(Action a, ZonedDateTime now) {
        switch (a.tool) {
            case "set_alarm":
                return String.format(Locale.ROOT, "Alarm: %02d:%02d (%s)\nLabel: %s\n"
                                + "Next occurrence only; verify the date in Clock.",
                        a.number("hour"), a.number("minute"),
                        nextAlarm(a, now).toLocalDate(), a.text("label"));
            case "set_timer":
                return "Timer: " + a.number("seconds") + " seconds\nLabel: " + a.text("label");
            case "create_calendar_event":
                return "Calendar: " + a.text("title") + "\nStart: " + a.text("start")
                        + "\nEnd: " + a.text("end") + "\nTimezone: " + now.getZone()
                        + "\nLocation: " + a.optional("location")
                        + "\nDescription: " + a.optional("description")
                        + "\nReview and save in your calendar app.";
            case "create_note":
                return "Google Keep note\n" + a.text("title") + "\n\n" + a.text("text")
                        + "\n\nReview and save in Keep.";
            case "whatsapp_message":
                return "WhatsApp recipient: " + (a.text("phone").isEmpty()
                        ? a.text("contact") + " (you select this contact in WhatsApp)"
                        : "+" + a.text("phone")) + "\n\n" + a.text("message")
                        + "\n\nYou must press Send in WhatsApp.";
            default: throw new IllegalArgumentException("Unsupported action.");
        }
    }

    private static void keys(Action a, String... names) {
        Set<String> allowed = new HashSet<>(Arrays.asList(names));
        for (String key : a.args.keySet()) {
            if (!allowed.contains(key)) fail("Unexpected argument: " + key);
        }
    }
    private static void integer(Action a, String key, int min, int max) {
        Object v = a.args.get(key);
        if (!(v instanceof Number)) fail("Expected an integer for " + key);
        double n = ((Number) v).doubleValue();
        if (Double.isNaN(n) || Double.isInfinite(n) || n != Math.rint(n) || n < min || n > max) {
            fail("Invalid " + key + ": expected " + min + " to " + max + ".");
        }
    }
    private static void text(Action a, String key, int max, boolean nonempty) {
        Object value = a.args.get(key);
        if (!(value instanceof String)) fail("Expected text for " + key);
        String s = (String) value;
        if (s.length() > max || (nonempty && s.trim().isEmpty()) || s.indexOf('\0') >= 0) {
            fail("Invalid or overly long " + key + ".");
        }
    }
    private static void optionalText(Action a, String key, int max) {
        if (a.args.containsKey(key)) text(a, key, max, false);
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
