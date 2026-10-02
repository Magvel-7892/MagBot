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
        JSONTokener tokener = new JSONTokener(output.trim());
        Object rootValue = tokener.nextValue();
        if (!(rootValue instanceof JSONObject) || tokener.nextClean() != 0) {
            throw new IllegalArgumentException("Expected a single JSON object, without extra text.");
        }
        JSONObject root = (JSONObject) rootValue;
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
}
