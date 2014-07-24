package com.ubookr.plugins;

import android.database.Cursor;
import android.util.Log;
import android.net.Uri;
import android.content.Intent;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Exception;
import java.lang.String;
import java.util.Calendar;
import java.util.Date;

public class CallLog extends CordovaPlugin {

    private static final String ACTION_ALL = "all";
    private static final String ACTION_LIST = "list";
    private static final String ACTION_CONTACT = "contact";
    private static final String ACTION_SHOW = "show";
    private static final String TAG = "CallLogPlugin";

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
//    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "Plugin Called");
        PluginResult result = null;

        if (ACTION_ALL.equals(action)) {
            JSONObject all = getAllCallLog(args);
            result = new PluginResult(PluginResult.Status.OK, all);
        } else if (ACTION_CONTACT.equals(action)) {
            String phoneNumber = args.getString(0);
            result = contact(phoneNumber);
        } else if (ACTION_SHOW.equals(action)) {
            String phoneNumber = args.getString(0);
            result = show(phoneNumber);
        } else if (ACTION_LIST.equals(action)) {
            int days = 1;
            //obtain date to limit by
            if ( ! args.isNull(0)) {
                String period = args.getString(0);
                Log.d(TAG, "Time period is: " + period);
                if (period.equals("week"))
                    days = 7;
                else if (period.equals("month"))
                    days = 30;
                else if (period.equals("all"))
                    days = -1; // indicates no limit in list method below
            }
            result = list(days);
        } else {
            Log.d(TAG, "Invalid action : " + action + " passed");
            result = new PluginResult(Status.INVALID_ACTION);
        }
        callbackContext.sendPluginResult(result);
        return true;
    }

    private PluginResult show(String phoneNumber) {

        PluginResult result;
        try {
            viewContact(phoneNumber);
            result = new PluginResult(Status.OK);
        } catch (Exception e) {
            Log.d(TAG, "Got Exception " + e.getMessage());
            result = new PluginResult(Status.ERROR, e.getMessage());
        }
        return result;
    }

    private PluginResult contact(String phoneNumber) {

        PluginResult result;
        try {
            String contactInfo = getContactNameFromNumber(phoneNumber);
            Log.d(TAG, "Returning " + contactInfo.toString());
            result = new PluginResult(Status.OK, contactInfo);
        } catch (Exception e) {
            Log.d(TAG, "Got Exception " + e.getMessage());
            result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }
        return result;
    }

    private PluginResult list(int days) {

        PluginResult result;
        try {
            String limiter = null;
            if (days > 0) {
                //turn this into a date
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());

                calendar.add(Calendar.DAY_OF_YEAR, -days);
                Date limitDate = calendar.getTime();
                limiter = String.valueOf(limitDate.getTime());
            }

            //now do required search
            JSONObject callLog = getCallLog(limiter);
            Log.d(TAG, "Returning " + callLog.toString());
            result = new PluginResult(Status.OK, callLog);

        } catch (JSONException e) {
            Log.d(TAG, "Got JSON Exception " + e.getMessage());
            result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
        }
        return result;
    }

   	private void viewContact(String phoneNumber) {

        Intent i = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT,
                Uri.parse(String.format("tel: %s", phoneNumber)));
        this.cordova.getActivity().startActivity(i);
   	}

    private JSONObject getCallLog(String limiter) throws JSONException {

   		JSONObject callLog = new JSONObject();

   		String[] strFields = {
   				android.provider.CallLog.Calls.DATE,
   				android.provider.CallLog.Calls.NUMBER,
   				android.provider.CallLog.Calls.TYPE,
   				android.provider.CallLog.Calls.DURATION,
   				android.provider.CallLog.Calls.NEW,
   				android.provider.CallLog.Calls.CACHED_NAME,
   				android.provider.CallLog.Calls.CACHED_NUMBER_TYPE,
   				android.provider.CallLog.Calls.CACHED_NUMBER_LABEL };

   		try {
            Cursor callLogCursor = this.cordova.getActivity().getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI,
   					strFields,
   					limiter == null ? null : android.provider.CallLog.Calls.DATE + ">?",
   	                limiter == null ? null : new String[] {limiter},
   					android.provider.CallLog.Calls.DEFAULT_SORT_ORDER);

   			int callCount = callLogCursor.getCount();

   			if (callCount > 0) {
   				JSONObject callLogItem = new JSONObject();
   				JSONArray callLogItems = new JSONArray();

   				callLogCursor.moveToFirst();
   				do {
   					callLogItem.put("date", callLogCursor.getLong(0));
   					callLogItem.put("number", callLogCursor.getString(1));
   					callLogItem.put("type", callLogCursor.getInt(2));
   					callLogItem.put("duration", callLogCursor.getLong(3));
   					callLogItem.put("new", callLogCursor.getInt(4));
   					callLogItem.put("cachedName", callLogCursor.getString(5));
   					callLogItem.put("cachedNumberType", callLogCursor.getInt(6));
   					//callLogItem.put("name", getContactNameFromNumber(callLogCursor.getString(1))); //grab name too
   					callLogItems.put(callLogItem);
   					callLogItem = new JSONObject();
   				} while (callLogCursor.moveToNext());
   				callLog.put("rows", callLogItems);
   			}

   			callLogCursor.close();
   		} catch (Exception e) {
   			Log.d("CallLog_Plugin", " ERROR : SQL to get cursor: ERROR " + e.getMessage());
   		}

   		return callLog;
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
//        } catch (JSONException e) {
//            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
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
        String[] projection = new String[]{PhoneLookup.DISPLAY_NAME};

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        // query time
        Cursor c = this.cordova.getActivity().getContentResolver().query(contactUri, projection, null, null, null);

        // if the query returns 1 or more results
        // return the first result
        if (c.moveToFirst()) {
            String name = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
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
