(function () {
  'use strict';

  var PUBLIC_API = /\/api\/(users\/(login|register|refresh)|forgetPassword\/)/;

  /*
   * authInterceptor
   *  - attaches "Authorization: Bearer <accessToken>" to API calls
   *  - refreshes the access token shortly before it expires (15-minute tokens)
   *  - on a 401, refreshes once and retries the original request
   *  - if refreshing fails, signs the user out and sends them to /login
   * Auth is fetched through $injector to avoid the $http <-> Auth circular dependency.
   */
  angular.module('shop').factory('authInterceptor', ['$q', '$injector', 'APP_CONFIG',
    function ($q, $injector, APP_CONFIG) {
      function isApi(config) { return config.url && config.url.indexOf('/api/') > -1; }
      function needsAuth(config) { return isApi(config) && !config.skipAuth && !PUBLIC_API.test(config.url); }

      function sessionExpired() {
        var Auth = $injector.get('Auth');
        var $location = $injector.get('$location');
        var Toast = $injector.get('Toast');
        if (Auth.user()) Toast.error('Your session expired. Please sign in again.');
        Auth.clear();
        Auth.setReturnTo($location.path());
        $location.path('/login');
      }

      return {
        request: function (config) {
          if (!needsAuth(config)) return config;
          var Auth = $injector.get('Auth');
          var token = Auth.accessToken();
          if (!token) return config;

          function attach(t) {
            config.headers = config.headers || {};
            config.headers.Authorization = 'Bearer ' + t;
            return config;
          }
          if (Auth.isExpired(token, APP_CONFIG.refreshSkewSeconds) && Auth.refreshToken()) {
            return Auth.refresh().then(attach, function () { return attach(token); });
          }
          return attach(token);
        },

        responseError: function (res) {
          var config = res.config || {};
          if (res.status === 401 && needsAuth(config) && !config._retried) {
            var Auth = $injector.get('Auth');
            if (!Auth.refreshToken()) { sessionExpired(); return $q.reject(res); }
            return Auth.refresh().then(function () {
              config._retried = true;
              return $injector.get('$http')(config);
            }, function () {
              sessionExpired();
              return $q.reject(res);
            });
          }
          return $q.reject(res);
        }
      };
    }]);

  /*
   * ApiError.message(response) — one readable sentence from any backend error shape:
   *   plain text body                          ("Email id not found")
   *   GlobalExceptionHandler.ErrorResponse     ({status, message, timestamp})
   *   validation failure                       ({error: 'Validation failed', fields: {...}})
   */
  angular.module('shop').factory('ApiError', function () {
    return {
      message: function (res, fallback) {
        if (!res) return fallback || 'Something went wrong.';
        if (res.status === -1 || res.status === 0) return 'Can\'t reach the server. Is the backend running on port 8080?';
        var d = res.data;
        if (typeof d === 'string' && d.trim() && d.trim().charAt(0) !== '<') return d.trim();
        if (d && d.fields) {
          return Object.keys(d.fields).map(function (k) { return d.fields[k]; }).join(' ');
        }
        if (d && d.message) return d.message;
        if (res.status === 401) return 'Please sign in again.';
        if (res.status === 403) return 'You don\'t have permission to do that.';
        if (res.status === 404) return 'Not found.';
        return fallback || 'Something went wrong (HTTP ' + res.status + ').';
      },
      /** field -> message map from a 400 "Validation failed" response, or null */
      fields: function (res) { return res && res.data && res.data.fields ? res.data.fields : null; },
      isNotFound: function (res) { return res && res.status === 404; }
    };
  });

  /*
   * Api — one method per backend endpoint. Controllers never build URLs themselves.
   */
  angular.module('shop').factory('Api', ['$http', '$q', 'APP_CONFIG', 'Auth', 'ApiError',
    function ($http, $q, APP_CONFIG, Auth, ApiError) {
      var base = APP_CONFIG.apiBase;
      function data(res) { return res.data; }
      /** Several endpoints answer 404 for "empty list" — turn that into [] */
      function emptyOn404(promise) {
        return promise.then(data, function (res) { return ApiError.isNotFound(res) ? [] : $q.reject(res); });
      }
      function uid() { return Auth.user() && Auth.user().id; }
      function enc(v) { return encodeURIComponent(v); }
      var json = { headers: { 'Content-Type': 'application/json' } };

      return {
        // ── Accounts ────────────────────────────────
        register: function (user) { return $http.post(base + '/api/users/register', user).then(data); },
        deleteAccount: function (email, password) {
          return $http({ method: 'DELETE', url: base + '/api/users/delete', data: { email: email, password: password }, headers: json.headers }).then(data);
        },
        devices: function (email) { return $http.get(base + '/api/users/devices/user/' + enc(email)).then(data); },
        removeDevice: function (deviceId) {
          return $http.delete(base + '/api/users/devices/remove', { params: { userId: uid(), deviceId: deviceId } }).then(data);
        },

        // ── Password reset (public) ─────────────────
        sendOtp: function (email) { return $http.post(base + '/api/forgetPassword/verifyEmail/' + enc(email)).then(data); },
        verifyOtp: function (otp, email) { return $http.post(base + '/api/forgetPassword/verifyOtp/' + enc(otp) + '/' + enc(email)).then(data); },
        changePassword: function (email, otp, password, repeatPassword) {
          return $http.post(base + '/api/forgetPassword/changePassword/' + enc(email) + '/' + enc(otp),
            { password: password, repeatPassword: repeatPassword }).then(data);
        },

        // ── Catalogue (customer) ────────────────────
        allProducts: function () { return emptyOn404($http.get(base + '/api/user/getAllProducts')); },
        filterProducts: function (params) { return $http.get(base + '/api/user/filter', { params: params }).then(data); },

        // ── Cart ────────────────────────────────────
        cart: function () { return $http.get(base + '/api/user/myCartItems').then(data); },
        addToCart: function (productId, size, quantity) {
          return $http.post(base + '/api/user/addcartItems', {
            user: { id: uid() }, product: { id: productId }, productSize: { size: size }, quantity: quantity
          }).then(data);
        },
        /** Backend semantics: same size => quantity is ADDED (send ±1); new size => size & quantity replaced */
        updateCartItem: function (productId, size, quantity) {
          return $http.put(base + '/api/user/updateCartItemsById', {
            user: { id: uid() }, product: { id: productId }, productSize: { size: size }, quantity: quantity
          }).then(data);
        },
        removeCartItem: function (cartItemId) {
          return $http({ method: 'DELETE', url: base + '/api/user/deleteCartItemById/' + cartItemId, data: { user: { id: uid() } }, headers: json.headers }).then(data);
        },

        // ── Orders & payment ────────────────────────
        createOrder: function (items) {
          return $http.post(base + '/api/user/createOrder', {
            user: { id: uid() },
            orderItems: items.map(function (i) {
              return { product: { id: i.productId }, productSize: { size: i.size }, quantity: i.quantity };
            })
          }).then(data);
        },
        myOrders: function () { return emptyOn404($http.get(base + '/api/user/getAllOrdersByEmail/' + enc(Auth.user().email))); },
        order: function (id) { return $http.get(base + '/api/user/getOrderById/' + id).then(data); },
        cancelOrder: function (id) { return $http.delete(base + '/api/user/deleteOrder/' + id).then(data); },
        startPayment: function (orderId) { return $http.post(base + '/api/user/payment/create', null, { params: { orderId: orderId } }).then(data); },
        verifyPayment: function (p) { return $http.post(base + '/api/user/payment/verify', null, { params: p }).then(data); },

        // ── Reviews (customer) ──────────────────────
        productReviews: function (productId) { return emptyOn404($http.get(base + '/api/user/getReviewByProductId/' + productId)); },
        myReviews: function () { return emptyOn404($http.get(base + '/api/user/getReviewByUserId/' + uid())); },
        createReview: function (productId, rating, comment) {
          return $http.post(base + '/api/user/createReview', {
            user: { id: uid() }, product: { id: productId }, rating: rating, comment: comment
          }).then(data);
        },
        updateReview: function (productId, rating, comment) {
          return $http.put(base + '/api/user/updateReviewByProductId/' + productId, null,
            { params: { userId: uid(), rating: rating, comment: comment } }).then(data);
        },
        deleteReview: function (productId) {
          return $http.delete(base + '/api/user/deleteReviewByProductId/' + productId, { params: { userId: uid() } }).then(data);
        },

        // ── Admin ───────────────────────────────────
        admin: {
          products: function () { return emptyOn404($http.get(base + '/api/admin/getAllProducts')); },
          createProduct: function (p) { return $http.post(base + '/api/admin/createProduct', p).then(data); },
          updateProduct: function (id, details, size, stock) {
            return $http.put(base + '/api/admin/updateProduct/' + id,
              { productDetails: details, requestedSize: size, newStockQuantity: stock }).then(data);
          },
          deleteProduct: function (id) { return $http.delete(base + '/api/admin/deleteProduct/' + id).then(data); },
          ordersByEmail: function (email) { return emptyOn404($http.get(base + '/api/admin/getAllMyOrdersByEmail/' + enc(email))); },
          order: function (id) { return $http.get(base + '/api/admin/getOrderById/' + id).then(data); },
          updateOrderStatus: function (order, status) {
            return $http.put(base + '/api/admin/updateOrderStatus/' + order.id, { status: status, user: { id: order.userId } }).then(data);
          },
          reviews: function () { return emptyOn404($http.get(base + '/api/admin/getAllReviews')); }
        }
      };
    }]);

  /* Toast — small non-blocking notifications */
  angular.module('shop').factory('Toast', ['$rootScope', '$timeout', function ($rootScope, $timeout) {
    var seq = 0;
    $rootScope.toasts = [];
    function push(kind, text, ms) {
      var t = { id: ++seq, kind: kind, text: text };
      $rootScope.toasts.push(t);
      if ($rootScope.toasts.length > 4) $rootScope.toasts.shift();
      $timeout(function () { dismiss(t); }, ms || (kind === 'error' ? 6000 : 3500));
      return t;
    }
    function dismiss(t) {
      var i = $rootScope.toasts.indexOf(t);
      if (i > -1) $rootScope.toasts.splice(i, 1);
    }
    $rootScope.dismissToast = dismiss;
    return {
      success: function (m, ms) { return push('success', m, ms); },
      error: function (m, ms) { return push('error', m, ms); },
      info: function (m, ms) { return push('info', m, ms); }
    };
  }]);
})();
