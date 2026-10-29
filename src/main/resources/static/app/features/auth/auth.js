(function () {
  'use strict';
  var mod = angular.module('shop');

  mod.controller('LoginController', ['$location', 'Auth', 'Cart', 'Toast', 'ApiError', 'FormErrors',
    function ($location, Auth, Cart, Toast, ApiError, FormErrors) {
      var vm = this;
      vm.form = { email: $location.search().email || '', password: '' };
      vm.busy = false;
      vm.error = null;
      vm.showPassword = false;

      vm.submit = function (form) {
        vm.error = null;
        if (form.$invalid) return;
        vm.busy = true;
        Auth.login(vm.form.email.trim(), vm.form.password)
          .then(function (result) {
            Cart.reset();
            var msg = result.deviceMessage || '';
            if (/Already \d+ devices/i.test(msg)) {
              Toast.error(msg, 8000);
              $location.search({}).path('/account').search('devices', 'full');
              return;
            }
            Toast.success('Welcome back, ' + result.user.username + '.');
            var back = Auth.takeReturnTo();
            $location.search({}).path(back && back !== '/login' ? back : (Auth.isAdmin() ? '/admin' : '/shop'));
          })
          .catch(function (res) {
            if (!FormErrors.applyServer(form, ApiError.fields(res))) {
              vm.error = res.status === 401 ? 'That email and password don\'t match an account.' : ApiError.message(res);
            }
          })
          .finally(function () { vm.busy = false; });
      };
    }]);

  mod.controller('RegisterController', ['$location', 'Api', 'Toast', 'ApiError', 'FormErrors',
    function ($location, Api, Toast, ApiError, FormErrors) {
      var vm = this;
      vm.form = { username: '', email: '', password: '', confirm: '', roles: 'ROLE_USER', terms: false };
      vm.busy = false;
      vm.error = null;

      vm.submit = function (form) {
        vm.error = null;
        if (form.$invalid) return;
        vm.busy = true;
        Api.register({
          username: vm.form.username.trim(),
          email: vm.form.email.trim().toLowerCase(),
          password: vm.form.password,
          roles: vm.form.roles
        })
          .then(function () {
            Toast.success('Account created. Sign in to continue.');
            $location.path('/login').search('email', vm.form.email.trim().toLowerCase());
          })
          .catch(function (res) {
            if (res.status === 409) {
              form.email.$setValidity('server', false);
              form.email.$serverMessage = 'An account with this email already exists.';
              return;
            }
            if (!FormErrors.applyServer(form, ApiError.fields(res))) vm.error = ApiError.message(res);
          })
          .finally(function () { vm.busy = false; });
      };
    }]);

  /*
   * Three steps: 1) email -> OTP sent   2) OTP verified   3) new password.
   * OTP lifetime on the backend is 700 s, so we show a countdown.
   */
  mod.controller('ForgotPasswordController', ['$scope', '$interval', '$location', 'Api', 'Toast', 'ApiError',
    function ($scope, $interval, $location, Api, Toast, ApiError) {
      var vm = this;
      var OTP_SECONDS = 700;
      vm.step = 1;
      vm.form = { email: '', otp: '', password: '', repeat: '' };
      vm.busy = false;
      vm.error = null;
      vm.info = null;
      vm.secondsLeft = 0;
      var timer = null;

      function startTimer() {
        if (timer) $interval.cancel(timer);
        vm.secondsLeft = OTP_SECONDS;
        timer = $interval(function () {
          vm.secondsLeft = Math.max(0, vm.secondsLeft - 1);
          if (!vm.secondsLeft) $interval.cancel(timer);
        }, 1000);
      }
      $scope.$on('$destroy', function () { if (timer) $interval.cancel(timer); });

      vm.timeLeft = function () {
        var m = Math.floor(vm.secondsLeft / 60), s = vm.secondsLeft % 60;
        return m + ':' + (s < 10 ? '0' : '') + s;
      };

      function fail(res) { vm.error = ApiError.message(res); }
      function done() { vm.busy = false; }

      vm.sendCode = function (form) {
        vm.error = vm.info = null;
        if (form.$invalid) return;
        vm.busy = true;
        Api.sendOtp(vm.form.email.trim())
          .then(function () {
            vm.step = 2; startTimer();
            vm.info = 'We emailed a 6-digit code to ' + vm.form.email.trim() + '.';
          }, function (res) {
            if (res.status === 409) {           // "OTP already generated" — still valid, go enter it
              vm.step = 2;
              vm.info = 'A code was already sent to this email recently. Enter it below.';
            } else if (res.status === 404) {
              form.email.$setValidity('server', false);
              form.email.$serverMessage = 'No account uses this email.';
            } else fail(res);
          })
          .finally(done);
      };

      vm.checkCode = function (form) {
        vm.error = null;
        if (form.$invalid) return;
        if (vm.step === 2 && timer && !vm.secondsLeft) { vm.error = 'This code has expired. Start again to get a new one.'; return; }
        vm.busy = true;
        Api.verifyOtp(vm.form.otp, vm.form.email.trim())
          .then(function () { vm.step = 3; vm.info = null; }, function (res) {
            if (res.status === 401) {
              form.otp.$setValidity('server', false);
              form.otp.$serverMessage = 'That code isn\'t right. Check the email and try again.';
            } else fail(res);
          })
          .finally(done);
      };

      vm.savePassword = function (form) {
        vm.error = null;
        if (form.$invalid) return;
        vm.busy = true;
        Api.changePassword(vm.form.email.trim(), vm.form.otp, vm.form.password, vm.form.repeat)
          .then(function () {
            Toast.success('Password changed. Sign in with your new password.');
            $location.path('/login').search('email', vm.form.email.trim());
          }, fail)
          .finally(done);
      };

      vm.restart = function () {
        vm.step = 1; vm.error = vm.info = null;
        vm.form.otp = vm.form.password = vm.form.repeat = '';
        if (timer) $interval.cancel(timer);
        vm.secondsLeft = 0;
      };
    }]);
})();
