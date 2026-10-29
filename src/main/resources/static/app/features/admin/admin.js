(function () {
  'use strict';
  var mod = angular.module('shop');

  var LOW_STOCK = 5;

  function totalStock(p) { return (p.sizes || []).reduce(function (n, s) { return n + (s.stockQuantity || 0); }, 0); }

  mod.controller('AdminDashboardController', ['$q', 'Api', 'ApiError', function ($q, Api, ApiError) {
    var vm = this;
    vm.loading = true;
    $q.all([Api.admin.products(), Api.admin.reviews()])
      .then(function (r) {
        var products = r[0], reviews = r[1];
        vm.productCount = products.length;
        vm.unitCount = products.reduce(function (n, p) { return n + totalStock(p); }, 0);
        vm.reviewCount = reviews.length;
        vm.avgRating = reviews.length ? reviews.reduce(function (n, x) { return n + x.rating; }, 0) / reviews.length : 0;
        vm.lowStock = [];
        products.forEach(function (p) {
          (p.sizes || []).forEach(function (s) {
            if (s.stockQuantity <= LOW_STOCK) vm.lowStock.push({ product: p, size: s.size, stock: s.stockQuantity });
          });
        });
        vm.lowStock.sort(function (a, b) { return a.stock - b.stock; });
        vm.noSizes = products.filter(function (p) { return !(p.sizes || []).length; });
        vm.recentReviews = reviews.slice().sort(function (a, b) { return b.id - a.id; }).slice(0, 5);
      })
      .catch(function (res) { vm.error = ApiError.message(res); })
      .finally(function () { vm.loading = false; });
  }]);

  mod.controller('AdminProductsController', ['Api', 'Toast', 'ApiError', 'APP_CONFIG', function (Api, Toast, ApiError, APP_CONFIG) {
    var vm = this;
    vm.loading = true;
    vm.products = [];
    vm.q = '';
    vm.sort = 'name';
    vm.totalStock = totalStock;
    vm.lowStock = LOW_STOCK;
    vm.sizes = APP_CONFIG.sizes;

    function load() {
      vm.loading = true;
      return Api.admin.products()
        .then(function (list) { vm.products = list; })
        .catch(function (res) { vm.error = ApiError.message(res); })
        .finally(function () { vm.loading = false; });
    }
    load();

    vm.stockOf = function (p, size) {
      var s = (p.sizes || []).filter(function (x) { return x.size === size; })[0];
      return s ? s.stockQuantity : null;
    };
    vm.matches = function (p) {
      var q = (vm.q || '').toLowerCase().trim();
      return !q || (p.name || '').toLowerCase().indexOf(q) > -1 || (p.category || '').toLowerCase().indexOf(q) > -1;
    };
    vm.remove = function (p) {
      Api.admin.deleteProduct(p.id)
        .then(function () { Toast.success('"' + p.name + '" deleted.'); return load(); })
        .catch(function (res) {
          var msg = ApiError.message(res);
          Toast.error(res.status === 500 ? 'Couldn\'t delete "' + p.name + '". It is probably in an order or a cart.' : msg);
        });
    };
  }]);

  /*
   * Create / edit product.
   * Create: POST /createProduct with sizes [{size, stockQuantity}].
   * Edit:   PUT /updateProduct/{id} takes the details plus ONE size's new stock, so we send one
   *         request per size whose stock changed (or one request if only details changed).
   *         The API can't add or remove sizes on an existing product, so those are locked when editing.
   */
  mod.controller('AdminProductFormController', ['$routeParams', '$location', '$q', 'Api', 'Toast', 'ApiError', 'APP_CONFIG',
    function ($routeParams, $location, $q, Api, Toast, ApiError, APP_CONFIG) {
      var vm = this;
      vm.id = $routeParams.id ? Number($routeParams.id) : null;
      vm.isEdit = !!vm.id;
      vm.loading = vm.isEdit;
      vm.saving = false;
      vm.error = null;
      vm.categories = [];
      vm.form = { name: '', description: '', price: null, category: '' };
      vm.checks = {};   // dummy models for form-level rules (duplicate, at least one size)
      vm.sizes = APP_CONFIG.sizes.map(function (s) { return { size: s, on: false, stock: null, original: null, locked: false }; });
      var allProducts = [];

      Api.admin.products().then(function (list) {
        allProducts = list;
        var seen = {};
        list.forEach(function (p) { if (p.category) seen[p.category] = true; });
        vm.categories = Object.keys(seen).sort();
        if (!vm.isEdit) return;
        var p = list.filter(function (x) { return x.id === vm.id; })[0];
        if (!p) { vm.notFound = true; return; }
        vm.original = p;
        vm.form = { name: p.name, description: p.description, price: p.price, category: p.category };
        vm.sizes.forEach(function (row) {
          var s = (p.sizes || []).filter(function (x) { return x.size === row.size; })[0];
          row.locked = true;
          if (s) { row.on = true; row.stock = s.stockQuantity; row.original = s.stockQuantity; }
        });
      }).catch(function (res) { vm.error = ApiError.message(res); })
        .finally(function () { vm.loading = false; });

      vm.selectedCount = function () { return vm.sizes.filter(function (r) { return r.on; }).length; };
      vm.toggle = function (row) { if (!row.on) row.stock = null; else if (row.stock === null) row.stock = 0; };

      /** Same name + same description = backend treats it as a duplicate */
      vm.isDuplicate = function () {
        var n = (vm.form.name || '').trim().toLowerCase(), d = (vm.form.description || '').trim().toLowerCase();
        if (!n) return false;
        return allProducts.some(function (p) {
          return p.id !== vm.id && (p.name || '').toLowerCase() === n && (p.description || '').toLowerCase() === d;
        });
      };

      vm.save = function (form) {
        vm.error = null;
        if (form.$invalid) return;
        vm.saving = true;
        var details = {
          name: vm.form.name.trim(),
          description: vm.form.description.trim(),
          price: Math.round(Number(vm.form.price) * 100) / 100,
          category: vm.form.category.trim()
        };
        var call;
        if (!vm.isEdit) {
          details.sizes = vm.sizes.filter(function (r) { return r.on; })
            .map(function (r) { return { size: r.size, stockQuantity: Number(r.stock) }; });
          call = Api.admin.createProduct(details).then(function (msg) {
            if (/already/i.test(msg || '')) return $q.reject({ status: 409, data: 'A product with this name and description already exists.' });
            return msg;
          });
        } else {
          var changed = vm.sizes.filter(function (r) { return r.on && Number(r.stock) !== r.original; });
          if (!changed.length) {
            var first = vm.sizes.filter(function (r) { return r.on; })[0];
            call = Api.admin.updateProduct(vm.id, details, first ? first.size : null, first ? Number(first.stock) : 0);
          } else {
            // sequential, so a failure stops the rest
            call = changed.reduce(function (prev, r) {
              return prev.then(function () { return Api.admin.updateProduct(vm.id, details, r.size, Number(r.stock)); });
            }, $q.when());
          }
        }
        call.then(function () {
          Toast.success(vm.isEdit ? 'Product updated.' : 'Product created.');
          $location.path('/admin/products');
        }).catch(function (res) { vm.error = ApiError.message(res); })
          .finally(function () { vm.saving = false; });
      };
    }]);

  /*
   * Orders: the API has no "list all orders" endpoint, so admins look up by customer email
   * or by order number. Status changes call PUT /updateOrderStatus/{id} (which emails the
   * customer on SHIPPED / DELIVERED).
   */
  mod.controller('AdminOrdersController', ['$q', 'Api', 'Toast', 'ApiError', 'APP_CONFIG', function ($q, Api, Toast, ApiError, APP_CONFIG) {
    var vm = this;
    vm.mode = 'email';
    vm.lookup = { email: '', id: null };
    vm.orders = null;
    vm.busy = false;
    vm.statuses = APP_CONFIG.orderStatuses;
    vm.pending = {};     // order id -> chosen new status
    vm.saving = {};

    // Allowed next steps; keeps admins from e.g. moving DELIVERED back to PENDING
    var FLOW = {
      PENDING: ['PAID', 'CANCELLED'],
      PAID: ['SHIPPED', 'CANCELLED'],
      SHIPPED: ['DELIVERED'],
      DELIVERED: [],
      CANCELLED: []
    };
    vm.nextStatuses = function (o) { return FLOW[o.status] || vm.statuses; };

    vm.search = function (form) {
      vm.error = null;
      if (form.$invalid) return;
      vm.busy = true;
      var p = vm.mode === 'email'
        ? Api.admin.ordersByEmail(vm.lookup.email.trim())
        : Api.admin.order(vm.lookup.id).then(function (o) { return [o]; }, function (res) { return res.status === 404 ? [] : $q.reject(res); });
      p.then(function (list) { vm.orders = list; vm.pending = {}; })
        .catch(function (res) { vm.error = ApiError.message(res); vm.orders = null; })
        .finally(function () { vm.busy = false; });
    };

    vm.update = function (o) {
      var status = vm.pending[o.id];
      if (!status || status === o.status) return;
      vm.saving[o.id] = true;
      Api.admin.updateOrderStatus(o, status)
        .then(function (updated) {
          o.status = updated && updated.status ? updated.status : status;
          delete vm.pending[o.id];
          Toast.success('Order #' + o.id + ' marked ' + o.status + '.');
        })
        .catch(function (res) { Toast.error(ApiError.message(res)); })
        .finally(function () { vm.saving[o.id] = false; });
    };
  }]);

  mod.controller('AdminReviewsController', ['Api', 'ApiError', function (Api, ApiError) {
    var vm = this;
    vm.loading = true;
    vm.reviews = [];
    vm.minRating = '';
    vm.q = '';
    Api.admin.reviews()
      .then(function (list) { vm.reviews = list; })
      .catch(function (res) { vm.error = ApiError.message(res); })
      .finally(function () { vm.loading = false; });
    vm.matches = function (r) {
      if (vm.minRating === 'low' && r.rating > 2) return false;
      if (vm.minRating && vm.minRating !== 'low' && r.rating < Number(vm.minRating)) return false;
      var q = (vm.q || '').toLowerCase().trim();
      if (!q) return true;
      return [r.product && r.product.name, r.user && r.user.username, r.user && r.user.email, r.comment]
        .some(function (v) { return (v || '').toLowerCase().indexOf(q) > -1; });
    };
  }]);
})();
