exports.defineAutoTests = function() {
  
  var fail = function (done) {
    expect(true).toBe(false);
    done();
  },
  succeed = function (done) {
    expect(true).toBe(true);
    done();
  };

  describe('Plugin availability', function () {
    it("window.plugins.calllog should exist", function() {
      expect(window.plugins.calllog).toBeDefined();
    });
  });

  describe('API functions', function () {
    it("should define get", function() {
      expect(window.plugins.calllog.get).toBeDefined();
    });
  });
};
