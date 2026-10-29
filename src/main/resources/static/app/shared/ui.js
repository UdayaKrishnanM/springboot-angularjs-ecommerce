(function () {
  'use strict';
  var mod = angular.module('shop');

  /** Top bar / shell controller: nav state, cart badge, sign-out */
  mod.controller('ShellController', ['$scope', '$location', 'Auth', 'Cart', 'Toast',
    function ($scope, $location, Auth, Cart, Toast) {
      var vm = this;
      vm.auth = Auth;
      vm.cart = Cart;
      vm.menuOpen = false;

      vm.isActive = function (path) {
        var p = $location.path();
        return p === path || (path !== '/' && path !== '/admin' && p.indexOf(path + '/') === 0);
      };
      vm.signOut = function () {
        Auth.logout().then(function () {
          Cart.reset();
          Toast.info('Signed out.');
          $location.path('/login');
        });
      };
      $scope.$on('$routeChangeSuccess', function () {
        vm.menuOpen = false;
        if (Auth.isLoggedIn() && Auth.isCustomer() && !Cart.loaded) Cart.refresh();
      });
    }]);

  /** Shared cart state so the header badge stays in sync */
  mod.factory('Cart', ['Api', function (Api) {
    var Cart = {
      items: [], loaded: false,
      count: function () { return Cart.items.reduce(function (n, i) { return n + i.quantity; }, 0); },
      total: function () { return Cart.items.reduce(function (n, i) { return n + i.quantity * i.product.price; }, 0); },
      refresh: function () {
        return Api.cart().then(function (res) {
          Cart.items = (res && res.cartItems) || [];
          Cart.loaded = true;
          return Cart.items;
        });
      },
      reset: function () { Cart.items = []; Cart.loaded = false; }
    };
    return Cart;
  }]);

  /** Product tile art: products have no images, so we draw a deterministic colour block */
  mod.factory('Swatch', function () {
    var palettes = [
      ['#e9d8c4', '#b4532a'], ['#d9e3da', '#2f5d50'], ['#dfe2ee', '#34407a'], ['#f1e2b8', '#8a5a00'],
      ['#ead7de', '#8c2f4f'], ['#d6e6ec', '#1f5f74'], ['#e4e0d6', '#4a4536'], ['#e8dcf0', '#5b3a7a']
    ];
    function hash(s) { var h = 0; for (var i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0; return Math.abs(h); }
    return {
      style: function (name) {
        var p = palettes[hash(name || '') % palettes.length];
        return { background: p[0], color: p[1] };
      },
      initials: function (name) {
        return (name || '?').split(/\s+/).filter(Boolean).slice(0, 2).map(function (w) { return w[0]; }).join('').toUpperCase();
      }
    };
  });

  /** <tl-swatch name="product.name" category="product.category"> */
  mod.directive('tlSwatch', ['Swatch', function (Swatch) {
    return {
      restrict: 'E',
      scope: { name: '<', category: '<', size: '@' },
      template: '<div class="swatch" ng-class="size" ng-style="style" aria-hidden="true">' +
                '<span class="swatch-mark">{{ initials }}</span><span class="swatch-cat">{{ category }}</span></div>',
      link: function (scope) {
        scope.$watch('name', function (n) { scope.style = Swatch.style(n); scope.initials = Swatch.initials(n); });
      }
    };
  }]);

  /** Read-only stars: <tl-stars value="4.5"> */
  mod.directive('tlStars', function () {
    return {
      restrict: 'E',
      scope: { value: '<' },
      template: '<span class="stars" role="img" aria-label="{{ value | number:1 }} out of 5">' +
                '<span ng-repeat="i in [1,2,3,4,5]" ng-class="{on: i <= rounded}">★</span></span>',
      link: function (scope) { scope.$watch('value', function (v) { scope.rounded = Math.round(v || 0); }); }
    };
  });

  /** Star picker bound to ngModel (works with required / validation) */
  mod.directive('tlStarInput', function () {
    return {
      restrict: 'E',
      require: 'ngModel',
      scope: {},
      template: '<div class="star-input" role="radiogroup" aria-label="Rating">' +
                '<button type="button" ng-repeat="i in [1,2,3,4,5]" role="radio" aria-checked="{{ i === value }}" ' +
                'aria-label="{{ i }} star{{ i > 1 ? \'s\' : \'\' }}" ng-class="{on: i <= (hover || value)}" ' +
                'ng-mouseenter="$parent.hover = i" ng-mouseleave="$parent.hover = 0" ng-click="pick(i)">★</button></div>',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$render = function () { scope.value = ngModel.$viewValue; };
        scope.pick = function (i) {
          scope.value = i;
          ngModel.$setViewValue(i);
          ngModel.$setTouched();
        };
        ngModel.$isEmpty = function (v) { return !v; };
      }
    };
  });

  /** Order status pill */
  mod.directive('tlStatus', function () {
    return {
      restrict: 'E',
      scope: { value: '<' },
      template: '<span class="pill" ng-class="\'st-\' + (value || \'none\').toLowerCase()">{{ value || \'—\' }}</span>'
    };
  });

  /** Confirm-before-click: <button tl-confirm="Delete this?" tl-confirm-action="vm.del()"> */
  mod.directive('tlConfirm', ['$window', function ($window) {
    return {
      restrict: 'A',
      link: function (scope, el, attrs) {
        el.on('click', function (e) {
          e.preventDefault();
          if ($window.confirm(attrs.tlConfirm)) scope.$apply(attrs.tlConfirmAction);
        });
      }
    };
  }]);

  /** Indian rupee formatting (Razorpay amounts are INR) */
  mod.filter('inr', ['$filter', function ($filter) {
    return function (v) {
      if (v === undefined || v === null || v === '') return '';
      return '₹' + $filter('number')(v, 2);
    };
  }]);

  /** Java LocalDateTime arrives as "2026-10-29T10:15:30.123" or [2026,10,29,10,15,30] */
  mod.filter('javaDate', ['$filter', function ($filter) {
    return function (v, fmt) {
      if (!v) return '';
      var d = angular.isArray(v) ? new Date(v[0], v[1] - 1, v[2], v[3] || 0, v[4] || 0, v[5] || 0) : new Date(v);
      return isNaN(d) ? '' : $filter('date')(d, fmt || 'd MMM yyyy, h:mm a');
    };
  }]);
})();
