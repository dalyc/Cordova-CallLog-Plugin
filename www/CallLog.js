function CallLog() {
}

CallLog.prototype.echo = function (message, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "CallLog", "echo", [message]);
};

CallLog.prototype.all = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "CallLog", "all", []);
};

CallLog.prototype.contact = function (phoneNumber, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "CallLog", "contact", [phoneNumber]);
};

CallLog.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.calllog = new CallLog();
  return window.plugins.calllog;
};

cordova.addConstructor(CallLog.install);