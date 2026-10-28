package com.amine.tvclock;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;

final class ClockTestBackup {
    private static final String[] NAMES = {"limits", "clock", "MainActivity"};
    private static File file(Context context) {
        File directory = context.getExternalFilesDir(null);
        if (directory == null) throw new AssertionError("No test backup directory");
        return new File(directory, "clock-test-backup.json");
    }
    static boolean exists(Context context) { return file(context).exists(); }
    static void save(Context context) {
        try {
            if (exists(context)) throw new AssertionError("Recover the interrupted TV test backup first");
            JSONObject data = new JSONObject();
            android.app.AppOpsManager ops = context.getSystemService(android.app.AppOpsManager.class);
            data.put("overlayAppOp", ops.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    android.os.Process.myUid(), context.getPackageName()));
            for (String name : NAMES) {
                JSONObject entries = new JSONObject();
                for (Map.Entry<String, ?> entry : context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll().entrySet()) {
                    Object value = entry.getValue();
                    if (!(value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof String))
                        throw new AssertionError("Unsupported preference type: " + entry.getKey());
                    JSONObject record = new JSONObject();
                    record.put("type", value.getClass().getSimpleName()); record.put("value", value);
                    entries.put(entry.getKey(), record);
                }
                data.put(name, entries);
            }
            try (FileOutputStream output = new FileOutputStream(file(context))) {
                output.write(data.toString().getBytes(StandardCharsets.UTF_8));
                output.getFD().sync();
            }
        } catch (Exception error) { throw new AssertionError(error); }
    }
    static void restore(Context context) {
        try {
            JSONObject data = new JSONObject(new String(Files.readAllBytes(file(context).toPath()), StandardCharsets.UTF_8));
            android.app.AppOpsManager ops = context.getSystemService(android.app.AppOpsManager.class);
            int mode = data.getInt("overlayAppOp");
            if (ops.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    android.os.Process.myUid(), context.getPackageName()) != mode) {
                String value;
                switch (mode) {
                    case android.app.AppOpsManager.MODE_ALLOWED: value = "allow"; break;
                    case android.app.AppOpsManager.MODE_IGNORED: value = "ignore"; break;
                    case android.app.AppOpsManager.MODE_ERRORED: value = "deny"; break;
                    case android.app.AppOpsManager.MODE_DEFAULT: value = "default"; break;
                    case android.app.AppOpsManager.MODE_FOREGROUND: value = "foreground"; break;
                    default: throw new AssertionError("Unsupported overlay app-op mode: " + mode);
                }
                String command = "appops set --user 0 " + context.getPackageName() + " SYSTEM_ALERT_WINDOW " + value;
                try (android.os.ParcelFileDescriptor fd = androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation().getUiAutomation().executeShellCommand(command);
                     java.io.FileInputStream input = new java.io.FileInputStream(fd.getFileDescriptor())) {
                    input.readAllBytes();
                }
                if (ops.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        android.os.Process.myUid(), context.getPackageName()) != mode)
                    throw new AssertionError("Could not restore overlay access");
            }
            for (String name : NAMES) {
                JSONObject entries = data.getJSONObject(name);
                SharedPreferences.Editor edit = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear();
                for (Iterator<String> keys = entries.keys(); keys.hasNext();) {
                    String key = keys.next(); JSONObject record = entries.getJSONObject(key);
                    switch (record.getString("type")) {
                        case "Boolean": edit.putBoolean(key, record.getBoolean("value")); break;
                        case "Integer": edit.putInt(key, record.getInt("value")); break;
                        case "Long": edit.putLong(key, record.getLong("value")); break;
                        case "String": edit.putString(key, record.getString("value")); break;
                        default: throw new AssertionError("Unsupported preference type: " + key);
                    }
                }
                if (!edit.commit()) throw new AssertionError("Could not restore " + name);
            }
        } catch (Exception error) { throw new AssertionError(error); }
    }
    static void clear(Context context) {
        if (exists(context) && !file(context).delete()) throw new AssertionError("Could not remove completed test backup");
    }
}
