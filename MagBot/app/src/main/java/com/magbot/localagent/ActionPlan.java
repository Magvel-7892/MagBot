package com.magbot.localagent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ActionPlan {
    final List<ActionValidator.Action> actions;
    final String reply;
    private ActionPlan(List<ActionValidator.Action> actions, String reply) {
        this.actions = actions;
        this.reply = reply;
    }

    static ActionPlan parse(String output, String command) throws Exception {
        if (output == null || output.length() > 20000) {
            throw new IllegalArgumentException("Missing or oversized model output.");
        }
        // Do not scrape executable JSON out of surrounding model prose or reasoning.
        JSONObject root = extractPlanObject(output);
        if (root.length() != 2 || !root.has("actions") || !root.has("reply")) {
            throw new IllegalArgumentException("Expected exactly actions and reply.");
        }
        JSONArray items = root.getJSONArray("actions");
        if (items.length() > ActionValidator.MAX_ACTIONS) {
            throw new IllegalArgumentException("Too many actions; split the request.");
        }
        Object replyValue = root.get("reply");
        if (!(replyValue instanceof String) || ((String) replyValue).length() > 600) {
            throw new IllegalArgumentException("Invalid reply text.");
        }
        List<ActionValidator.Action> actions = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item.length() != 2 || !(item.get("tool") instanceof String)) {
                throw new IllegalArgumentException("Invalid action fields.");
            }
            JSONObject values = item.getJSONObject("args");
            Map<String, Object> args = new LinkedHashMap<>();
            Iterator<String> keys = values.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                args.put(key, values.get(key));
            }
            actions.add(new ActionValidator.Action(item.getString("tool"), args));
        }
        ActionValidator.validateAll(actions, command, ZonedDateTime.now());
        return new ActionPlan(actions, (String) replyValue);
    }

    private static JSONObject extractPlanObject(String output) throws Exception {
    String trimmed = output.trim();

    // Fast path: model returned clean JSON.
    try {
        JSONTokener tokener = new JSONTokener(trimmed);
        Object value = tokener.nextValue();

        if (value instanceof JSONObject && tokener.nextClean() == 0) {
            return (JSONObject) value;
        }
    } catch (RuntimeException ignored) {
        // Try extracting JSON from surrounding prose/markdown.
    }

    // Nemotron may return things like:
    // "Here is the JSON:" or ```json ... ```
    for (int start = 0; start < trimmed.length(); start++) {

        if (trimmed.charAt(start) != '{') {
            continue;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < trimmed.length(); i++) {

            char c = trimmed.charAt(i);

            if (inString) {

                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }

                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {

                depth--;

                if (depth == 0) {

                    String candidate =
                            trimmed.substring(start, i + 1);

                    try {

                        JSONObject object =
                                new JSONObject(candidate);

                        if (object.length() == 2
                                && object.has("actions")
                                && object.has("reply")) {

                            return object;
                        }

                    } catch (RuntimeException ignored) {
                        // Continue looking.
                    }

                    break;
                }

                if (depth < 0) {
                    break;
                }
            }
        }
    }

    throw new IllegalArgumentException(
            "Could not find a valid MagBot JSON object in the model response."
    );
    }
}
