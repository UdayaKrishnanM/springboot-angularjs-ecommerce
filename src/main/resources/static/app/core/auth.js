(function () {
  'use strict';

  /*
   * Auth — owns the JWT pair, the current user (from GET /api/users/me) and this
   * browser's device registration (POST /api/users/devices/login).
   */
  angular.module('shop').factory('Auth', ['$http', '$q', '$window', 'APP_CONFIG',
    function ($http, $q, $window, APP_CONFIG) {
      var KEYS = { access: 'tl.accessToken', refresh: 'tl.refreshToken', user: 'tl.user', device: 'tl.deviceId', returnTo: 'tl.returnTo' };
      var api = APP_CONFIG.apiBase;
      var refreshing = null;

      function store(key, value) {
        try {
          if (value === null || value === undefined) $window.localStorage.removeItem(key);
          else $window.localStorage.setItem(key, typeof value === 'string' ? value : angular.toJson(value));
        } catch (e) { /* storage unavailable (private mode) — app still works for this tab */ }
      }
      function read(key, json) {
        try {
          var v = $window.localStorage.getItem(key);
          return json && v ? angular.fromJson(v) : v;
        } catch (e) { return null; }
      }

      var state = { user: read(KEYS.user, true) };

      function decode(token) {
        try {
          var part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
          return angular.fromJson(decodeURIComponent(escape($window.atob(part))));
        } catch (e) { return null; }
      }

      function deviceId() {
        var id = read(KEYS.device);
        if (!id) {
          id = 'web-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
          store(KEYS.device, id);
        }
        return id;
      }

      function deviceName() {
        var ua = $window.navigator.userAgent;
        var browser = /Edg\//.test(ua) ? 'Edge' : /OPR\//.test(ua) ? 'Opera' : /Chrome\//.test(ua) ? 'Chrome'
          : /Firefox\//.test(ua) ? 'Firefox' : /Safari\//.test(ua) ? 'Safari' : 'Browser';
        var os = /Windows/.test(ua) ? 'Windows' : /Android/.test(ua) ? 'Android' : /iPhone|iPad/.test(ua) ? 'iOS'
          : /Mac OS X/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : 'Unknown OS';
        return browser + ' on ' + os;
      }

      function location() {
        try { return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Unknown'; }
        catch (e) { return 'Unknown'; }
      }

      var Auth = {
        accessToken: function () { return read(KEYS.access); },
        refreshToken: function () { return read(KEYS.refresh); },
        user: function () { return state.user; },
        deviceId: deviceId,

        isLoggedIn: function () { return !!(read(KEYS.access) && state.user); },
        hasRole: function (role) { return !!(state.user && (state.user.roles || '').split(',').indexOf(role) > -1); },
        isAdmin: function () { return Auth.hasRole('ROLE_ADMIN'); },
        isCustomer: function () { return Auth.hasRole('ROLE_USER'); },

        /** true if the token is missing or expires within `skew` seconds */
        isExpired: function (token, skew) {
          var claims = token && decode(token);
          if (!claims || !claims.exp) return true;
          return claims.exp * 1000 - (skew || 0) * 1000 <= Date.now();
        },

        login: function (email, password) {
          return $http.post(api + '/api/users/login', { email: email, password: password }, { skipAuth: true })
            .then(function (res) {
              store(KEYS.access, res.data.accessToken);
              store(KEYS.refresh, res.data.refreshToken);
              return Auth.loadMe();
            })
            .then(function (user) {
              return Auth.registerDevice().then(function (deviceMsg) {
                return { user: user, deviceMessage: deviceMsg };
              });
            })
            .catch(function (err) { Auth.clear(); return $q.reject(err); });
        },

        loadMe: function () {
          return $http.get(api + '/api/users/me').then(function (res) {
            state.user = res.data;
            store(KEYS.user, res.data);
            return res.data;
          });
        },

        /** Resolves with the backend's message; never rejects (device tracking is best-effort). */
        registerDevice: function () {
          var u = state.user;
          return $http.post(api + '/api/users/devices/login', null, {
            params: { email: u.email, deviceId: deviceId(), location: location(), deviceName: deviceName() }
          }).then(function (res) { return res.data; }, function () { return null; });
        },

        /** Single-flight refresh: concurrent 401s share one refresh call. */
        refresh: function () {
          if (refreshing) return refreshing;
          var rt = read(KEYS.refresh);
          if (!rt || Auth.isExpired(rt, 0)) return $q.reject({ status: 401, data: 'Session expired' });
          refreshing = $http.post(api + '/api/users/refresh', null, { params: { refreshToken: rt }, skipAuth: true })
            .then(function (res) {
              store(KEYS.access, res.data.accessToken);
              return res.data.accessToken;
            })
            .finally(function () { refreshing = null; });
          return refreshing;
        },

        logout: function () {
          var u = state.user;
          var done = function () { Auth.clear(); };
          if (!u) { done(); return $q.when(); }
          return $http.post(api + '/api/users/devices/logout', null, { params: { email: u.email, deviceId: deviceId() } })
            .then(done, done);
        },

        clear: function () {
          state.user = null;
          store(KEYS.access, null);
          store(KEYS.refresh, null);
          store(KEYS.user, null);
        },

        setReturnTo: function (path) { store(KEYS.returnTo, path); },
        takeReturnTo: function () { var p = read(KEYS.returnTo); store(KEYS.returnTo, null); return p; }
      };

      return Auth;
    }]);
})();
