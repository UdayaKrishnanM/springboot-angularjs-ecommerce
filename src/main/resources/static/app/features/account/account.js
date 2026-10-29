(function () {
  'use strict';

  angular.module('shop').controller('AccountController', ['$location', 'Api', 'Auth', 'Cart', 'Toast', 'ApiError', 'APP_CONFIG',
    function ($location, Api, Auth, Cart, Toast, ApiError, APP_CONFIG) {
      var vm = this;
      vm.user = Auth.user();
      vm.thisDevice = Auth.deviceId();
      vm.maxDevices = APP_CONFIG.maxDevices;
      vm.devicesFull = $location.search().devices === 'full';
      vm.devices = [];
      vm.loadingDevices = true;
      vm.del = { password: '', confirmText: '' };
      vm.deleting = false;
      vm.deleteError = null;

      function loadDevices() {
        vm.loadingDevices = true;
        return Api.devices(vm.user.email)
          .then(function (list) { vm.devices = list; })
          .catch(function (res) { Toast.error(ApiError.message(res)); })
          .finally(function () { vm.loadingDevices = false; });
      }
      loadDevices();

      vm.activeCount = function () { return vm.devices.filter(function (d) { return d.loggedIn; }).length; };
      vm.thisRegistered = function () {
        return vm.devices.some(function (d) { return d.deviceId === vm.thisDevice && d.loggedIn; });
      };

      vm.remove = function (d) {
        Api.removeDevice(d.deviceId)
          .then(function (msg) { Toast.success(msg || 'Device removed.'); return loadDevices(); })
          .catch(function (res) { Toast.error(ApiError.message(res)); });
      };

      vm.registerThis = function () {
        Auth.registerDevice().then(function (msg) {
          if (msg && /Already \d+ devices/i.test(msg)) { Toast.error(msg); return; }
          Toast.success(msg || 'This device is now signed in.');
          vm.devicesFull = false;
          $location.search('devices', null);
          loadDevices();
        });
      };

      // The API only supports OTP-based password changes, which are public endpoints
      vm.changePassword = function () {
        Auth.logout().then(function () { Cart.reset(); $location.search({}).path('/forgot-password'); });
      };

      vm.deleteAccount = function (form) {
        vm.deleteError = null;
        if (form.$invalid) return;
        vm.deleting = true;
        Api.deleteAccount(vm.user.email, vm.del.password)
          .then(function (msg) {
            // backend answers 200 with a message for both success and failure
            if (/deleted successfully/i.test(msg)) {
              Auth.clear(); Cart.reset();
              Toast.success('Your account has been deleted.');
              $location.path('/login');
            } else if (/incorrect/i.test(msg)) {
              form.password.$setValidity('server', false);
              form.password.$serverMessage = 'That password is incorrect.';
            } else {
              vm.deleteError = msg;
            }
          })
          .catch(function (res) { vm.deleteError = ApiError.message(res); })
          .finally(function () { vm.deleting = false; });
      };
    }]);
})();
