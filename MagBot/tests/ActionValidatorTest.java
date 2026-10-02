package com.magbot.localagent;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dependency-free JVM tests of the policy layer, not Android or LLM integration tests. */
public final class ActionValidatorTest {
    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-10-02T21:00:00+05:30[Asia/Kolkata]");
    private static int passed = 0;
    private static ActionValidator.Action action(String tool, Object... pairs) {
        Map<String, Object> args = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) args.put((String) pairs[i], pairs[i + 1]);
        return new ActionValidator.Action(tool, args);
    }
    private static void accept(String name, ActionValidator.Action a, String command) {
        ActionValidator.validate(a, command, NOW);
        if (ActionValidator.summary(a, NOW).isEmpty()) throw new AssertionError("Empty summary: " + name);
        passed++;
        System.out.println("PASS accept: " + name);
    }
    private static void reject(String name, ActionValidator.Action a, String command) {
        try { ActionValidator.validate(a, command, NOW); }
        catch (RuntimeException expected) {
            passed++;
            System.out.println("PASS reject: " + name);
            return;
        }
        throw new AssertionError("Incorrectly accepted: " + name);
    }
    public static void main(String[] args) {
        accept("next morning alarm", action("set_alarm", "hour", 7, "minute", 0, "label", "Gym", "date", "2026-10-03"), "alarm tomorrow 7 AM");
        accept("time-only alarm", action("set_alarm", "hour", 22, "minute", 30, "label", "Call Dad"), "alarm 10:30 PM");
        accept("midnight alarm", action("set_alarm", "hour", 0, "minute", 0, "label", "", "date", ""), "alarm midnight");
        reject("hour range", action("set_alarm", "hour", 24, "minute", 0, "label", ""), "alarm");
        reject("minute range", action("set_alarm", "hour", 7, "minute", 60, "label", ""), "alarm");
        reject("fractional hour", action("set_alarm", "hour", 7.5, "minute", 0, "label", ""), "alarm");
        reject("string instead of integer", action("set_alarm", "hour", "7", "minute", 0, "label", ""), "alarm");
        reject("future dated alarm unsupported", action("set_alarm", "hour", 7, "minute", 0, "label", "", "date", "2026-10-04"), "alarm Sunday");
        reject("tomorrow evening is not next occurrence", action("set_alarm", "hour", 22, "minute", 0, "label", "", "date", "2026-10-03"), "alarm tomorrow 10 PM");
        reject("bad alarm date", action("set_alarm", "hour", 7, "minute", 0, "label", "", "date", "2026-02-30"), "alarm");
        accept("timer", action("set_timer", "seconds", 60, "label", "Demo"), "one minute timer");
        accept("one day timer", action("set_timer", "seconds", 86400, "label", "Day"), "24 hour timer");
        reject("zero timer", action("set_timer", "seconds", 0, "label", ""), "timer");
        reject("negative timer", action("set_timer", "seconds", -1, "label", ""), "timer");
        reject("oversized timer", action("set_timer", "seconds", 86401, "label", ""), "timer");
        reject("missing timer argument", action("set_timer", "label", ""), "timer");
        reject("arbitrary shell argument", action("set_timer", "seconds", 10, "label", "", "shell", "rm -rf /"), "timer");
        accept("calendar event", action("create_calendar_event", "title", "Review", "start", "2026-10-03T16:00:00", "end", "2026-10-03T16:45:00"), "meeting tomorrow 4 PM");
        reject("reversed calendar times", action("create_calendar_event", "title", "Review", "start", "2026-10-03T16:00:00", "end", "2026-10-03T15:00:00"), "meeting");
        reject("past calendar event", action("create_calendar_event", "title", "Review", "start", "2026-10-01T16:00:00", "end", "2026-10-01T17:00:00"), "meeting");
        reject("UTC suffix in local event", action("create_calendar_event", "title", "Review", "start", "2026-10-03T16:00:00Z", "end", "2026-10-03T17:00:00Z"), "meeting");
        accept("Keep note", action("create_note", "title", "Shopping", "text", "Buy cement and paint"), "make a note");
        reject("empty note", action("create_note", "title", "Shopping", "text", "   "), "make a note");
        reject("oversized note", action("create_note", "title", "", "text", String.join("", Collections.nCopies(4001, "x"))), "note");
        reject("null note text", action("create_note", "title", "", "text", null), "note");
        accept("named WhatsApp recipient", action("whatsapp_message", "contact", "Arun", "phone", "", "message", "I will be late"), "WhatsApp Arun saying I will be late");
        accept("supplied international number", action("whatsapp_message", "contact", "", "phone", "919876543210", "message", "Hello"), "WhatsApp +91 98765 43210 saying Hello");
        reject("invented phone number", action("whatsapp_message", "contact", "Arun", "phone", "919876543210", "message", "Hello"), "WhatsApp Arun saying Hello");
        reject("missing WhatsApp recipient", action("whatsapp_message", "contact", "", "phone", "", "message", "Hello"), "say Hello");
        reject("empty WhatsApp message", action("whatsapp_message", "contact", "Arun", "phone", "", "message", ""), "WhatsApp Arun");
        reject("phone is not digits", action("whatsapp_message", "contact", "", "phone", "+919876543210", "message", "Hello"), "WhatsApp +919876543210 Hello");
        reject("unsupported tool", action("delete_all_notes"), "delete notes");
        reject("arbitrary app launch", action("open_app", "package", "com.example.app"), "open app");
        ActionValidator.Action timer = action("set_timer", "seconds", 60, "label", "Demo");
        ActionValidator.validateAll(Arrays.asList(timer, timer), "two timers", NOW);
        passed++;
        System.out.println("PASS: valid two-action batch");
        try {
            ActionValidator.validateAll(Collections.nCopies(7, timer), "seven timers", NOW);
            throw new AssertionError("Accepted oversized batch");
        } catch (IllegalArgumentException expected) { passed++; System.out.println("PASS: oversized batch rejected"); }
        ActionValidator.validateAll(Collections.emptyList(), "unsupported request", NOW);
        passed++;
        if (!AgentProtocol.systemPrompt().contains("whatsapp_message")) throw new AssertionError("Prompt missing tools");
        passed++;
        System.out.println("PASS: empty clarification plan and prompt smoke check");
        System.out.println("\n" + passed + " JVM policy checks passed.");
    }
}
