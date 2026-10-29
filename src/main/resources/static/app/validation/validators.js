(function () {
  'use strict';

  /*
   * Client-side validation.
   *
   * Every rule here mirrors (or is stricter than) a rule the backend enforces, so
   * users see the problem before a round-trip:
   *
   *   Backend rule (Java)                                   Frontend
   *   User.username   @NotBlank @Size(3..50)                required, ng-minlength=3, ng-maxlength=50, tl-username
   *   User.email      @NotBlank @Email                      required, tl-email (needs a real domain, e.g. a@b.co)
   *   User.password   @NotBlank @Size(min=6)                required, ng-minlength=6, tl-strong-password
   *   ChangePassword  password == repeatPassword            tl-match
   *   OTP             6-digit int (100000..999999)          tl-otp
   *   Review rating   0..5 (service check)                  required 1..5
   *   Cart quantity   >0 and <= stock (service check)       tl-integer, min 1, max = stock
   *   Filter          minPrice <= maxPrice                  tl-not-greater-than
   *
   * Error messages are rendered with ngMessages (see the "tl-messages" template in index.html).
   */
  var mod = angular.module('shop');

  // RFC-lite: something@domain.tld — rejects "a@b" which the HTML5 rule accepts
  var EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/;

  function empty(v) { return v === undefined || v === null || v === ''; }

  /** <input type="email" tl-email> — stricter email check on top of the built-in "email" rule */
  mod.directive('tlEmail', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.emailStrict = function (value) { return empty(value) || EMAIL.test(String(value).trim()); };
      }
    };
  });

  /** Letters, numbers, space, dot, underscore, hyphen; must start with a letter */
  mod.directive('tlUsername', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.username = function (v) { return empty(v) || /^[A-Za-z][A-Za-z0-9 ._-]*$/.test(v); };
      }
    };
  });

  /** At least one letter and one digit, no spaces */
  mod.directive('tlStrongPassword', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.letter = function (v) { return empty(v) || /[A-Za-z]/.test(v); };
        ngModel.$validators.digit = function (v) { return empty(v) || /\d/.test(v); };
        ngModel.$validators.spaces = function (v) { return empty(v) || !/\s/.test(v); };
      }
    };
  });

  /** tl-match="vm.form.password" — value must equal another model value (re-checks when either changes) */
  mod.directive('tlMatch', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.match = function (v) { return empty(v) || v === scope.$eval(attrs.tlMatch); };
        scope.$watch(attrs.tlMatch, function () { ngModel.$validate(); });
      }
    };
  });

  /** Exactly six digits, first digit 1–9 (backend generates 100000..999999) */
  mod.directive('tlOtp', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.otp = function (v) { return empty(v) || /^[1-9]\d{5}$/.test(String(v)); };
      }
    };
  });

  /** Whole numbers only (use with type="number") */
  mod.directive('tlInteger', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.integer = function (v) { return empty(v) || (isFinite(v) && Math.floor(v) === Number(v)); };
      }
    };
  });

  /** tl-decimals="2" — at most N digits after the decimal point */
  mod.directive('tlDecimals', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        var n = parseInt(attrs.tlDecimals, 10) || 2;
        var re = new RegExp('^-?\\d+(\\.\\d{1,' + n + '})?$');
        ngModel.$validators.decimals = function (model, view) {
          var raw = empty(view) ? model : view;
          return empty(raw) || re.test(String(raw));
        };
      }
    };
  });

  /** tl-not-greater-than="vm.filters.maxPrice" — e.g. min price must be <= max price */
  mod.directive('tlNotGreaterThan', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.range = function (v) {
          var other = scope.$eval(attrs.tlNotGreaterThan);
          return empty(v) || empty(other) || Number(v) <= Number(other);
        };
        scope.$watch(attrs.tlNotGreaterThan, function () { ngModel.$validate(); });
      }
    };
  });

  /** tl-valid-if="expr" — generic rule for things that aren't a single input (e.g. "pick at least one size") */
  mod.directive('tlValidIf', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        var key = attrs.tlValidKey || 'validIf';
        // This input only reports validity; it must never clear the bound model when invalid
        // (AngularJS's default), or two inputs sharing a model fight each other.
        ngModel.$options = ngModel.$options.createChild({ allowInvalid: true });
        ngModel.$validators[key] = function () { return !!scope.$eval(attrs.tlValidIf); };
        scope.$watch(attrs.tlValidIf, function () { ngModel.$validate(); }, true);
      }
    };
  });

  /** tl-no-whitespace-only — rejects values that are only spaces (backend @NotBlank) */
  mod.directive('tlNotBlank', function () {
    return {
      require: 'ngModel',
      link: function (scope, el, attrs, ngModel) {
        ngModel.$validators.blank = function (v) { return empty(v) || String(v).trim().length > 0; };
      }
    };
  });

  /**
   * Server-side errors: when the API answers 400 {fields:{email:"..."}} we mark those
   * inputs invalid with key "server" and show the backend's message. The flag clears
   * as soon as the user edits the field again.
   */
  mod.directive('ngModel', function () {
    return {
      restrict: 'A',
      require: ['ngModel', '?^form'],
      link: function (scope, el, attrs, ctrls) {
        var ngModel = ctrls[0];
        ngModel.$viewChangeListeners.push(function () {
          if (ngModel.$error.server) { ngModel.$setValidity('server', true); ngModel.$serverMessage = null; }
        });
      }
    };
  });

  mod.factory('FormErrors', function () {
    return {
      /** Applies a backend field->message map to a form. Returns true if any field matched. */
      applyServer: function (form, fields) {
        var matched = false;
        angular.forEach(fields || {}, function (message, name) {
          if (form[name]) {
            form[name].$setValidity('server', false);
            form[name].$serverMessage = message;
            form[name].$setTouched();
            matched = true;
          }
        });
        return matched;
      },
      reset: function (form) {
        if (!form) return;
        form.$setPristine();
        form.$setUntouched();
      }
    };
  });

  /** <form tl-focus-invalid> — on a failed submit, move focus to the first invalid field */
  mod.directive('tlFocusInvalid', ['$timeout', function ($timeout) {
    return {
      restrict: 'A',
      link: function (scope, el) {
        el.on('submit', function () {
          $timeout(function () {
            var bad = el[0].querySelector('.ng-invalid:not(form):not(fieldset):not([type=hidden])');
            if (bad && bad.focus) bad.focus();
          }, 0, false);
        });
      }
    };
  }]);

  /** Password strength meter: <tl-password-strength value="vm.form.password"> */
  mod.directive('tlPasswordStrength', function () {
    return {
      restrict: 'E',
      scope: { value: '<' },
      template:
        '<div class="strength" ng-if="value" aria-live="polite">' +
        '  <div class="strength-bar"><span ng-repeat="i in [0,1,2,3]" ng-class="{on: i < score}" class="s{{score}}"></span></div>' +
        '  <small class="muted">{{ label }}</small>' +
        '</div>',
      link: function (scope) {
        scope.$watch('value', function (v) {
          v = v || '';
          var s = 0;
          if (v.length >= 6) s++;
          if (v.length >= 10) s++;
          if (/[a-z]/.test(v) && /[A-Z]/.test(v)) s++;
          if (/\d/.test(v) && /[^A-Za-z0-9]/.test(v)) s++;
          scope.score = Math.max(1, s);
          scope.label = ['Too weak', 'Weak', 'Okay', 'Good', 'Strong'][scope.score];
        });
      }
    };
  });
})();
