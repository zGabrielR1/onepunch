package app.onepunch

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RecoveryTest {
    @Test fun malformedOriginalStringsArePreservedWithoutParsingOrMutation() {
        val workspace = "{broken\n\"active\": \"12,\""
        val draft = "{\"version\":999,\"draft\":\"unreadable\"}"
        val values = mapOf("workspace" to workspace, "routineDraft" to draft, "server" to "private-server")
        val result = JSONObject(recoveryDocument(values, 123L))
        assertEquals("onepunch.recovery", result.getString("format"))
        assertEquals(123L, result.getLong("exportedAt"))
        val originals = result.getJSONObject("originalPreferences")
        assertEquals(workspace, originals.getString("workspace"))
        assertEquals(draft, originals.getString("routineDraft"))
        assertFalse(originals.has("server"))
        assertFalse(originals.has("restoreSafetyBackup"))
        assertEquals(workspace, values["workspace"])
    }

    @Test fun wrongPreferenceTypesAndSafetyCopyRemainAvailable() {
        val originals = JSONObject(recoveryDocument(mapOf("workspace" to 42, "restoreSafetyBackup" to "raw-copy"), 1L))
            .getJSONObject("originalPreferences")
        assertEquals(42, originals.getInt("workspace"))
        assertEquals("raw-copy", originals.getString("restoreSafetyBackup"))
        assertFalse(originals.has("routineDraft"))
    }
}
