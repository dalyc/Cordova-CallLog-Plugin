package com.ubookr.plugins;

import android.util.Log;
import android.database.Cursor;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.PluginResult;

public class CallLog extends CordovaPlugin {

    @Override
    public boolean execute(String actionName, JSONArray arguments, final CallbackContext callbackContext) throws JSONException {

        JSONObject callLogs = new JSONObject();

        try {
            switch (getActionItem(actionName)) {
                case 1:
                    callLogs = getAllCallLog(arguments);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, callLogs));
                    return true;
                default: {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                }
            }
        } catch (JSONException jsonEx) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        }

        return false;
    }

    private JSONObject getAllCallLog(JSONArray requirements) throws JSONException {
        JSONObject callLog = new JSONObject();

        String[] strFields = {
                android.provider.CallLog.Calls.DATE,
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DURATION,
                android.provider.CallLog.Calls.NEW,
                android.provider.CallLog.Calls.CACHED_NAME,
                android.provider.CallLog.Calls.CACHED_NUMBER_TYPE,
                android.provider.CallLog.Calls.CACHED_NUMBER_LABEL//,
        };

        try {
            Cursor callLogCursor = this.cordova.getActivity().getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    strFields,
                    null,
                    null,
                    android.provider.CallLog.Calls.DEFAULT_SORT_ORDER
            );


            int callCount = callLogCursor.getCount();

            if (callCount > 0) {
                JSONArray callLogItem = new JSONArray();
                JSONArray callLogItems = new JSONArray();

                String[] columnNames = callLogCursor.getColumnNames();

                callLogCursor.moveToFirst();
                do {
                    callLogItem.put(callLogCursor.getLong(0));
                    callLogItem.put(callLogCursor.getString(1));
                    callLogItem.put(callLogCursor.getInt(2));
                    callLogItem.put(callLogCursor.getLong(3));
                    callLogItem.put(callLogCursor.getInt(4));
                    callLogItem.put(callLogCursor.getString(5));
                    callLogItem.put(callLogCursor.getInt(6));
                    callLogItems.put(callLogItem);
                    callLogItem = new JSONArray();

                } while (callLogCursor.moveToNext());

                callLog.put("Rows", callLogItems);
            }

            callLogCursor.close();
        } catch (Exception e) {
            Log.d("CallLog_Plugin", " ERROR : SQL to get cursor: ERROR " + e.getMessage());
        }

        return callLog;
    }

    private JSONObject getTimeRangeCallLog(JSONArray requirements) {
    }

    private int getActionItem (String actionName)throws JSONException {
        JSONObject actions = new JSONObject("{'all':1,'last':2,'time':3}");
        if (actions.has(actionName))
            return actions.getInt(actionName);

        return 0;
    }
}
