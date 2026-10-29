(function () {
  'use strict';
  var mod = angular.module('shop');

  mod.controller('OrdersController', ['Api', 'Toast', 'ApiError', function (Api, Toast, ApiError) {
    var vm = this;
    vm.loading = true;
    vm.orders = [];
    vm.statusFilter = '';
    vm.open = {};

    function load() {
      vm.loading = true;
      return Api.myOrders()
        .then(function (list) { vm.orders = list; })
        .catch(function (res) { vm.error = ApiError.message(res); })
        .finally(function () { vm.loading = false; });
    }
    load();

    vm.statuses = function () {
      var seen = {};
      vm.orders.forEach(function (o) { if (o.status) seen[o.status] = true; });
      return Object.keys(seen);
    };
    vm.canPay = function (o) { return o.status === 'PENDING'; };
    vm.cancel = function (o) {
      Api.cancelOrder(o.id)
        .then(function () { Toast.success('Order #' + o.id + ' cancelled.'); return load(); })
        .catch(function (res) { Toast.error(ApiError.message(res)); });
    };
    vm.sortKey = function (o) { return -(o.id || 0); };
  }]);

  /*
   * Payment (Razorpay):
   *   1. POST /payment/create?orderId=…   -> { razorpayOrderId, amount (paise), currency, keyId }
   *   2. Razorpay Checkout collects the payment in the browser
   *   3. POST /payment/verify?…           -> backend checks the HMAC signature, marks order PAID
   * PaymentService is a stub by default ("order_stub_…" ids). Razorpay rejects stub ids,
   * so in that case we explain what to change instead of opening the checkout.
   */
  mod.controller('PayController', ['$routeParams', '$location', '$window', '$document', '$q', '$timeout', 'Api', 'Auth', 'Toast', 'ApiError',
    function ($routeParams, $location, $window, $document, $q, $timeout, Api, Auth, Toast, ApiError) {
      var vm = this;
      var orderId = Number($routeParams.orderId);
      vm.loading = true;
      vm.order = null;
      vm.session = null;
      vm.busy = false;
      vm.result = null;
      vm.error = null;
      vm.stub = false;

      Api.order(orderId)
        .then(function (o) { vm.order = o; if (o.id == null) o.id = orderId; })
        .catch(function (res) { vm.error = ApiError.message(res); })
        .finally(function () { vm.loading = false; });

      function loadCheckout() {
        if ($window.Razorpay) return $q.when();
        return $q(function (resolve, reject) {
          var s = $document[0].createElement('script');
          s.src = 'https://checkout.razorpay.com/v1/checkout.js';
          s.onload = resolve;
          s.onerror = function () { reject({ status: 0, data: 'Couldn\'t load Razorpay Checkout. Check your internet connection.' }); };
          $document[0].body.appendChild(s);
        });
      }

      vm.pay = function () {
        vm.error = null; vm.busy = true;
        Api.startPayment(orderId)
          .then(function (session) {
            vm.session = session;
            vm.stub = /^order_stub_/.test(session.razorpayOrderId || '') || /placeholder/.test(session.keyId || '');
            if (vm.stub) return;
            return loadCheckout().then(function () { openCheckout(session); });
          })
          .catch(function (res) { vm.error = ApiError.message(res); })
          .finally(function () { vm.busy = false; });
      };

      function openCheckout(session) {
        var user = Auth.user();
        var rzp = new $window.Razorpay({
          key: session.keyId,
          amount: session.amount,
          currency: session.currency || 'INR',
          order_id: session.razorpayOrderId,
          name: 'Threadline',
          description: 'Order #' + orderId,
          prefill: { name: user.username, email: user.email },
          theme: { color: '#b4532a' },
          // Razorpay callbacks run outside Angular, so each one is wrapped in $timeout to trigger a digest
          handler: function (resp) { $timeout(function () {
            vm.busy = true;
            Api.verifyPayment({
              orderId: orderId,
              razorpayOrderId: resp.razorpay_order_id,
              razorpayPaymentId: resp.razorpay_payment_id,
              signature: resp.razorpay_signature
            }).then(function (msg) {
              var ok = !/failed/i.test(msg);
              vm.result = { ok: ok, message: msg };
              if (ok) { Toast.success('Payment received.'); vm.order.status = 'PAID'; }
            }).catch(function (res) { vm.error = ApiError.message(res); })
              .finally(function () { vm.busy = false; });
          }); },
          modal: { ondismiss: function () { $timeout(function () { Toast.info('Payment window closed. Your order is saved; you can pay later.'); }); } }
        });
        rzp.on('payment.failed', function (r) {
          $timeout(function () { vm.error = (r && r.error && r.error.description) || 'Payment failed.'; });
        });
        rzp.open();
      }

      vm.cancel = function () {
        Api.cancelOrder(orderId)
          .then(function () { Toast.success('Order cancelled.'); $location.path('/orders'); })
          .catch(function (res) { Toast.error(ApiError.message(res)); });
      };
    }]);
})();
