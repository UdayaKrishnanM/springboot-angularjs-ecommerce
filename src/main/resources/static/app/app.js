/*
 * Threadline — AngularJS (1.8) frontend for the e-commerce REST API.
 *
 * Module layout
 *   app.js                      module, routes, route guards
 *   core/*.js                   config, auth, http interceptor, api wrapper, toasts
 *   validation/validators.js    custom form validators (directives)
 *   shared/ui.js                small UI directives / filters
 *   features/<area>/*.js        one controller per screen, view next to it
 */
(function () {
  'use strict';

  angular.module('shop', ['ngRoute', 'ngMessages'])

    .config(['$routeProvider', '$locationProvider', '$httpProvider',
      function ($routeProvider, $locationProvider, $httpProvider) {
        $locationProvider.hashPrefix('!');
        $httpProvider.interceptors.push('authInterceptor');

        // access: 'guest' (logged-out only), 'auth' (any logged-in), 'user' (ROLE_USER), 'admin' (ROLE_ADMIN)
        function route(path, view, ctrl, access, title) {
          $routeProvider.when(path, {
            templateUrl: 'app/features/' + view + '.html',
            controller: ctrl,
            controllerAs: 'vm',
            access: access,
            title: title
          });
        }

        route('/login',            'auth/login',          'LoginController',          'guest', 'Sign in');
        route('/register',         'auth/register',       'RegisterController',       'guest', 'Create account');
        route('/forgot-password',  'auth/forgot',         'ForgotPasswordController', 'guest', 'Reset password');

        route('/shop',             'shop/catalog',        'CatalogController',        'user',  'Shop');
        route('/product/:id',      'shop/product',        'ProductController',        'user',  'Product');
        route('/cart',             'cart/cart',           'CartController',           'user',  'Your bag');
        route('/orders',           'orders/orders',       'OrdersController',         'user',  'Your orders');
        route('/pay/:orderId',     'orders/pay',          'PayController',            'user',  'Payment');
        route('/my-reviews',       'shop/my-reviews',     'MyReviewsController',      'user',  'Your reviews');

        route('/account',          'account/account',     'AccountController',        'auth',  'Account');

        route('/admin',                    'admin/dashboard',    'AdminDashboardController',   'admin', 'Admin');
        route('/admin/products',           'admin/products',     'AdminProductsController',    'admin', 'Products');
        route('/admin/products/new',       'admin/product-form', 'AdminProductFormController', 'admin', 'New product');
        route('/admin/products/:id/edit',  'admin/product-form', 'AdminProductFormController', 'admin', 'Edit product');
        route('/admin/orders',             'admin/orders',       'AdminOrdersController',      'admin', 'Orders');
        route('/admin/reviews',            'admin/reviews',      'AdminReviewsController',     'admin', 'Reviews');

        $routeProvider.otherwise({ redirectTo: '/' });
        $routeProvider.when('/', { template: '', controller: 'HomeRedirectController' });
      }])

    // "/" sends people to the right starting screen for who they are
    .controller('HomeRedirectController', ['$location', 'Auth', function ($location, Auth) {
      if (!Auth.isLoggedIn()) $location.path('/login');
      else $location.path(Auth.isAdmin() ? '/admin' : '/shop');
      $location.replace();
    }])

    // Route guards + page titles
    .run(['$rootScope', '$location', '$window', 'Auth', 'Toast',
      function ($rootScope, $location, $window, Auth, Toast) {
        $rootScope.$on('$routeChangeStart', function (event, next) {
          if (!next || !next.$$route) return;
          var access = next.$$route.access;
          if (!access) return;

          if (access === 'guest' && Auth.isLoggedIn()) {
            event.preventDefault();
            $location.path('/').replace();
            return;
          }
          if (access !== 'guest' && !Auth.isLoggedIn()) {
            event.preventDefault();
            Auth.setReturnTo($location.path());
            $location.path('/login').replace();
            return;
          }
          if (access === 'admin' && !Auth.isAdmin()) {
            event.preventDefault();
            Toast.error('That page is for store admins only.');
            $location.path('/').replace();
            return;
          }
          if (access === 'user' && !Auth.isCustomer()) {
            event.preventDefault();
            Toast.info('Shop pages need a customer account. Taking you to the admin area.');
            $location.path('/admin').replace();
          }
        });

        $rootScope.$on('$routeChangeSuccess', function (e, current) {
          var t = current && current.$$route && current.$$route.title;
          $window.document.title = t ? t + ' · Threadline' : 'Threadline';
          $window.scrollTo(0, 0);
        });

        // Form helper used by every view: show a field's errors once the user has
        // left the field, or once they tried to submit the form.
        $rootScope.showErrors = function (form, field) {
          if (!form || !form[field]) return false;
          return form[field].$invalid && (form[field].$touched || form.$submitted);
        };
      }]);
})();
