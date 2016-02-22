package com.ubookr.plugins;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.content.pm.PackageManager;
import static android.Manifest.permission.READ_CALL_LOG;

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
  private static final String TAG = "CallLogPlugin";
  // Permission request stuff.
  private static final int READ_CALL_LOG_REQ_CODE = 0;
  private static final String PERMISSION_DENIED_ERROR =
    "User refused to give permissions for reading call log";
  // Exec arguments.
  private CallbackContext callbackContext;
  private JSONArray args;
  private String action;

  public static final String READ_CALL_LOG =
    android.Manifest.permission.READ_CALL_LOG;

  @Override
  public boolean execute(String action, JSONArray args,
                         final CallbackContext callbackContext) {

    Log.d(TAG, "execute called");

    this.action = action;
    this.args = args;
    this.callbackContext = callbackContext;

    if (cordova.hasPermission(READ_CALL_LOG)) {
      Log.d(TAG, "Permission available");
      executeHelper();
    } else {
      Log.d(TAG, "No permissions, will request");
      cordova.requestPermission(this, READ_CALL_LOG_REQ_CODE,
                                READ_CALL_LOG);
    }
    return true;
  }

  private void executeHelper() {
    Log.d(TAG, "executeHelper with action " + action);
    if (ACTION_CONTACT.equals(action)) {
      contact();
    } else if (ACTION_SHOW.equals(action)) {
      show();
    } else if (ACTION_LIST.equals(action)) {
      list();
    } else {
      Log.d(TAG, "Invalid action: " + action + " passed");
      callbackContext.sendPluginResult(
        new PluginResult(Status.INVALID_ACTION));
    }
  }

  public void onRequestPermissionResult(
    int requestCode, String[] permissions,
    int[] grantResults) throws JSONException {
    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        Log.d(TAG, "Permission denied");
        callbackContext.sendPluginResult(
          new PluginResult(PluginResult.Status.ERROR,
                           PERMISSION_DENIED_ERROR));
        return;
      }
    }
    executeHelper();
  }

  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[],
                                         int[] grantResults) {
    if (requestCode == READ_CALL_LOG_REQ_CODE &&
        // If request is cancelled, the result arrays are empty.
        grantResults.length > 0 &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      executeHelper();
    }
  }

  private void show() {
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

  private void contact() {
    // TODO: this code path needs to ask user for permission, currently
    // it does not.
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

  private void list() {

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

          // Do required search
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

  private void viewContact(String phoneNumber) {

    Intent i = new Intent(Intents.SHOW_OR_CREATE_CONTACT,
                          Uri.parse(String.format("tel: %s", phoneNumber)));
    cordova.getActivity().startActivity(i);
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
      android.provider.CallLog.Calls.CACHED_NUMBER_LABEL
    };
    Cursor callLogCursor = cordova.getActivity().getContentResolver().query(
                             android.provider.CallLog.Calls.CONTENT_URI,
                             strFields,
                             limiter == null ? null : android.provider.CallLog.Calls.DATE + ">?",
                             limiter == null ? null : new String[] {limiter},
                             android.provider.CallLog.Calls.DEFAULT_SORT_ORDER);
    JSONArray callLogItems = new JSONArray();
    if (callLogCursor != null) {
      while (callLogCursor.moveToNext()) {
        JSONObject callLogItem = new JSONObject();
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
      }
      callLogCursor.close();
    }
    callLog.put("rows", callLogItems);
    return callLog;
  }

  private String getContactNameFromNumber(String number) {
    // define the columns I want the query to return
    String[] projection = new String[] {
      PhoneLookup.DISPLAY_NAME
    };

    // encode the phone number and build the filter URI
    Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

    // query time
    Cursor c = cordova.getActivity().getContentResolver().query(contactUri, projection, null, null, null);

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
