package app.onepunch

import org.json.JSONObject

/** Preserve original preference values without parsing or repairing their JSON. */
fun recoveryDocument(values: Map<String, *>, now: Long = System.currentTimeMillis()): String {
    val originals = JSONObject()
    for (key in listOf("workspace", "routineDraft", "restoreSafetyBackup")) {
        if (values.containsKey(key)) originals.put(key, JSONObject.wrap(values[key]))
    }
    return JSONObject()
        .put("format", "onepunch.recovery")
        .put("version", 1)
        .put("exportedAt", now)
        .put("originalPreferences", originals)
        .toString(2)
}
