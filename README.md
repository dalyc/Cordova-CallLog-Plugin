Cordova-CallLog-Plugin
======================

Android only (help with IOS welcome) cordova plugin to access the call history on a device.

Installation 
============
  cordova plugins add "https://github.com/dalyc/Cordova-CallLog-Plugin"
  
Permissions
===========

Add the following permissions to your AndroidManifest.xml

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_CALL_LOG" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />

Usage
=====
I've only used this in an Ionic application so this usage is AngularJS based.
Here is my CallLogService accessing the plugin (window.plugins.calllog)

    .factory('CallLogService', ['$q', function($q) {
        return {

            list : function(days) {
                var q = $q.defer();
                // days is how many days back to go
                window.plugins.calllog.list(days, function (response) {
                    q.resolve(response.rows);
                }, function (error) {
                    q.reject(error)
                });
                return q.promise;
            },

            contact : function(phoneNumber) {
                var q = $q.defer();
                window.plugins.calllog.contact(phoneNumber, function (response) {
                    q.resolve(response);
                }, function (error) {
                    q.reject(error)
                });
                return q.promise;
            },

            show : function(phoneNumber) {
                var q = $q.defer();
                window.plugins.calllog.show(phoneNumber, function (response) {
                    q.resolve(response);
                }, function (error) {
                    q.reject(error)
                });
                return q.promise;
            }

            delete : function(phoneNumber) {
                var q = $q.defer();
                window.plugins.calllog.delete(id, function (response) {
                    q.resolve(response);
                }, function (error) {
                    q.reject(error)
                });
                return q.promise;
            }
        }
    }])

Here is a controller using the above service

    .controller('DebugCtrl', ['$scope', 'CallLogService', 'LocalStorage',
        function ($scope, CallLogService, LocalStorage) {

            $scope.data = {};
            $scope.callTypeDisplay = function(type) {
                switch(type) {
                    case 1:
                        return 'Incoming';
                    case 2:
                        return 'Outgoing';
                    case 3:
                        return 'Missed';
                    default:
                        return 'Unknown';
                }
            };

            CallLogService.list(1).then(
                function(callLog) {
                    console.log(callLog);
                    $scope.data.lastCall = callLog[0];
                },
                function(error) {
                    console.error(error);
                });
        }]);


Here is my debug.html view that calls the above controller.

    <ion-view class="h6" title="Debug">
        <ion-content class="has-header">
            <div class="row">
                <div class="col">Last Call</div>
            </div>
            <div class="row">
                <div class="col col-30 col-offset-10">Name</div>
                <div class="col">{{data.lastCall.cachedName}}</div>
            </div>
            <div class="row">
                <div class="col col-30 col-offset-10">Number</div>
                <div class="col">{{data.lastCall.number}}</div>
            </div>
            <div class="row">
                <div class="col col-30 col-offset-10">Type</div>
                <div class="col">{{callTypeDisplay(data.lastCall.type)}}</div>
            </div>
            <div class="row">
                <div class="col col-30 col-offset-10">Date</div>
                <div class="col">{{data.lastCall.date | date}}</div>
            </div>
            <div class="row">
                <div class="col col-30 col-offset-10">Duration</div>
                <div class="col">{{data.lastCall.duration}} seconds</div>
            </div>
            <div class="row">
                <div class="col col-30 col-offset-10">Acknowledged</div>
                <div class="col">{{(data.lastCall.new == 1 ? 'yes' : 'no')}}</div>
            </div>
        </ion-content>
    </ion-view>

Note that I also have a decorator in app.js to allow my app to continue working when testing in a browser

    $provide.decorator('CallLogService', ['$delegate', '$q', function ($delegate, $q) {
      if (window.plugins && window.plugins.calllog) {
        return $delegate;
    } else {
        return {
          list : function() {
            var lastCall = {
              cachedName: 'Joe Blow',
              cachedNumberType: 2,
              date: 1406720317112,
              duration: 2224,
              new: 1,
              number: '0420884679',
              type: 1
            };
            return $q.when([lastCall]);
          },
          contact : function() {
            return $q.when({});
          },
          show : function() {
            return $q.when(true);
          }
        };
      }
    }]);
