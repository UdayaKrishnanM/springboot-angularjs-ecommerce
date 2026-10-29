(function () {
  'use strict';

  angular.module('shop')
    .constant('APP_CONFIG', {
      // Empty string = same origin (the UI is served by Spring Boot on :8080).
      // If you serve the UI from another port (e.g. VS Code Live Server on :5500),
      // set this to 'http://localhost:8080'. CORS for localhost is enabled in SecurityConfig.
      apiBase: '',

      // Refresh the access token this many seconds before it expires.
      refreshSkewSeconds: 30,

      sizes: ['S', 'M', 'L', 'XL', 'XXL'],
      orderStatuses: ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'],

      // Must match the backend's device limit in UserService.loginDevice
      maxDevices: 2
    });
})();
