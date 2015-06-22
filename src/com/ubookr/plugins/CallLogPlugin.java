package com.ubookr.plugins;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

public class CallLogPlugin extends CordovaPlugin {

    private static final String ACTION_LIST = "list";
    private static final String ACTION_CONTACT = "contact";
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_DELETE = "delete";
    private static final String TAG = "CallLogPlugin";

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {

        Log.d(TAG, "Plugin Called");

        if (ACTION_CONTACT.equals(action)) {
            contact(args, callbackContext);
        } else if (ACTION_SHOW.equals(action)) {
            show(args, callbackContext);
        } else if (ACTION_LIST.equals(action)) {
            list(args, callbackContext);
        } else if (ACTION_DELETE.equals(action)) {
            delete(args, callbackContext);
        } else {
            Log.d(TAG, "Invalid action : " + action + " passed");
            callbackContext.sendPluginResult(new PluginResult(Status.INVALID_ACTION));
        }
        return true;
    }

    private void show(final JSONArray args, final CallbackContext callbackContext) {

//        cordova.getThreadPool().execute(new Runnable() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PluginResult result;
                try {
                    String phoneNumber = args.getString(0);
                    viewContact(phoneNumber);
                    result = new PluginResult(Status.OK);
                } catch (JSONException e) {
                    Log.d(TAG, "Got JSON Exception " + e.getMessage());
                    result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
                } catch (Exception e) {
                    Log.d(TAG, "Got Exception " + e.getMessage());
                    result = new PluginResult(Status.ERROR, e.getMessage());
                }
                callbackContext.sendPluginResult(result);
            }
        });
    }

    private void contact(final JSONArray args, final CallbackContext callbackContext) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PluginResult result;
                try {
                    final String phoneNumber = args.getString(0);
                    String contactInfo = getContactNameFromNumber(phoneNumber);
                    Log.d(TAG, "Returning " + contactInfo);
                    result = new PluginResult(Status.OK, contactInfo);
                } catch (JSONException e) {
                    Log.d(TAG, "Got JSON Exception " + e.getMessage());
                    result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
                }
                callbackContext.sendPluginResult(result);
            }
        });
    }

    private void list(final JSONArray args, final CallbackContext callbackContext) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PluginResult result;
                try {

                    String limiter = null;
                    if (!args.isNull(0)) {
                        // make number positive in case caller give negative days
                        int days = Math.abs(Integer.valueOf(args.getString(0)));
                        Log.d(TAG, "Days is: " + days);
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
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Got NumberFormatException " + e.getMessage());
                    result = new PluginResult(Status.ERROR, "Non integer passed to list");
                } catch (Exception e) {
                    Log.d(TAG, "Got Exception " + e.getMessage());
                    result = new PluginResult(Status.ERROR, e.getMessage());
                }
                callbackContext.sendPluginResult(result);
            }
        });
    }

    private void delete(final JSONArray args, final CallbackContext callbackContext) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PluginResult result;
                try {

                    int res = CallLogPlugin.this.cordova.getActivity().getContentResolver().delete(
                            android.provider.CallLog.Calls.CONTENT_URI, "_ID = " + args.getString(0), null);
                    if (res == 1) {
                        result = new PluginResult(Status.OK, res);

                    } else {
                        result = new PluginResult(Status.ERROR, res);
                    }

                } catch (JSONException e) {
                    Log.d(TAG, "Got JSON Exception " + e.getMessage());
                    result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
                } catch (Exception e) {
                    Log.d(TAG, "Got Exception " + e.getMessage());
                    result = new PluginResult(Status.ERROR, e.getMessage());
                }
                callbackContext.sendPluginResult(result);
            }
        });
    }

   	private void viewContact(String phoneNumber) {

        Intent i = new Intent(Intents.SHOW_OR_CREATE_CONTACT,
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
   					callLogItem.put("cachedNumberLabel", callLogCursor.getInt(7));
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
}
