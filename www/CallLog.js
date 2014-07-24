function CallLog() {
}

CallLog.prototype.show = function (message, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "CallLog", "echo", [message]);
};

CallLog.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.calllog = new CallLog();
  return window.plugins.calllog;
};

cordova.addConstructor(CallLog.install);