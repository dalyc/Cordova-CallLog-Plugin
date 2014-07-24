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
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (action.equals("echo")) {
            String message = args.getString(0);
            this.echo(message, callbackContext);
            return true;
        } else if (action.equals("all")) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, getAllCallLog(args)));
            return true;
        } else if (action.equals("contact")) {
            try {
                String contactInfo = getContactNameFromNumber(args.getString(0));
                Log.d(TAG, "Returning " + contactInfo.toString());
                result = new PluginResult(Status.OK, contactInfo);
            } catch (JSONException jsonEx) {
                Log.d(TAG, "Got JSON Exception " + jsonEx.getMessage());
                result = new PluginResult(Status.JSON_EXCEPTION);
            }
        }
        return false;
    }

    private void echo(String message, CallbackContext callbackContext) {

        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
//        JSONObject callLogs = new JSONObject();
//
//        try {
//            callLogs = getAllCallLog(args);
//            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, callLogs));
//            return true;
////            switch (getActionItem(actionName)) {
////                case 1:
////                    callLogs = getAllCallLog(arguments);
////                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, callLogs));
////                    return true;
////                default: {
////                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
////                }
////            }
//        } catch (JSONException jsonEx) {
//            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
//        }
//
//        return false;
//    }

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

    private String getContactNameFromNumber(String number) {

   		// define the columns I want the query to return
   		String[] projection = new String[] { Contacts.Phones.DISPLAY_NAME, Contacts.Phones.NUMBER };

   		// encode the phone number and build the filter URI
   		Uri contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(number));

   		// query time
        Cursor c = this.cordova.getActivity().getContentResolver().query(contactUri, projection, null, null, null);

   		// if the query returns 1 or more results
   		// return the first result
   		if (c.moveToFirst()) {
   			String name = c.getString(c.getColumnIndex(Contacts.Phones.DISPLAY_NAME));
   			c.deactivate();
   			return name;
   		}

   		// return the original number if no match was found
   		return number;
   	}
//
////    private JSONObject getTimeRangeCallLog(JSONArray requirements) {
////    }
//
//    private int getActionItem (String actionName)throws JSONException {
//        JSONObject actions = new JSONObject("{'all':1,'last':2,'time':3}");
//        if (actions.has(actionName)) {
//            return actions.getInt(actionName);
//        }
//        return 0;
//    }
}
