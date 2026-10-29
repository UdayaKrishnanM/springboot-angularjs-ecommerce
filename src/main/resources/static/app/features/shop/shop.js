(function () {
  'use strict';
  var mod = angular.module('shop');

  /** Catalogue: server-side filtering, sorting and paging via GET /api/user/filter */
  mod.controller('CatalogController', ['$location', 'Api', 'ApiError',
    function ($location, Api, ApiError) {
      var vm = this;
      var q = $location.search();

      vm.sorts = [
        { label: 'Name, A–Z', sortBy: 'name', sortDir: 'asc' },
        { label: 'Name, Z–A', sortBy: 'name', sortDir: 'desc' },
        { label: 'Price, low to high', sortBy: 'price', sortDir: 'asc' },
        { label: 'Price, high to low', sortBy: 'price', sortDir: 'desc' }
      ];
      vm.pageSizes = [8, 12, 24];
      vm.categories = [];
      vm.filters = {
        name: q.name || '',
        category: q.category || '',
        minPrice: q.minPrice ? Number(q.minPrice) : null,
        maxPrice: q.maxPrice ? Number(q.maxPrice) : null,
        sort: vm.sorts[0],
        size: Number(q.size) || 12
      };
      vm.sorts.forEach(function (s) { if (s.sortBy === q.sortBy && s.sortDir === q.sortDir) vm.filters.sort = s; });
      vm.page = Number(q.page) || 0;
      vm.result = null;
      vm.loading = false;
      vm.error = null;

      // Categories come from the full product list (there's no categories endpoint)
      Api.allProducts().then(function (list) {
        var seen = {};
        list.forEach(function (p) { if (p.category) seen[p.category] = true; });
        vm.categories = Object.keys(seen).sort();
      });

      function params() {
        var f = vm.filters, p = { page: vm.page, size: f.size, sortBy: f.sort.sortBy, sortDir: f.sort.sortDir };
        if (f.name && f.name.trim()) p.name = f.name.trim();
        if (f.category) p.category = f.category;
        if (f.minPrice !== null && f.minPrice !== undefined && f.minPrice !== '') p.minPrice = f.minPrice;
        if (f.maxPrice !== null && f.maxPrice !== undefined && f.maxPrice !== '') p.maxPrice = f.maxPrice;
        return p;
      }

      vm.load = function () {
        vm.loading = true; vm.error = null;
        var p = params();
        $location.search(p).replace();
        Api.filterProducts(p)
          .then(function (page) { vm.result = page; })
          .catch(function (res) { vm.error = ApiError.message(res, 'Couldn\'t load products.'); })
          .finally(function () { vm.loading = false; });
      };

      vm.apply = function (form) {
        if (form && form.$invalid) return;  // e.g. min price > max price
        vm.page = 0;
        vm.load();
      };
      vm.clear = function (form) {
        vm.filters.name = ''; vm.filters.category = '';
        vm.filters.minPrice = vm.filters.maxPrice = null;
        if (form) { form.$setPristine(); form.$setUntouched(); }
        vm.apply();
      };
      vm.go = function (page) {
        if (!vm.result || page < 0 || page >= vm.result.totalPages) return;
        vm.page = page; vm.load();
      };
      vm.pages = function () {
        var n = vm.result ? vm.result.totalPages : 0, out = [];
        for (var i = 0; i < n; i++) out.push(i);
        return out;
      };
      vm.hasFilters = function () {
        var f = vm.filters;
        return !!(f.name || f.category || (f.minPrice !== null && f.minPrice !== undefined) || (f.maxPrice !== null && f.maxPrice !== undefined));
      };

      vm.load();
    }]);

  /** Product page: sizes & stock come from getAllProducts (the DTO endpoint has no sizes) */
  mod.controller('ProductController', ['$routeParams', '$location', 'Api', 'Auth', 'Cart', 'Toast', 'ApiError', 'APP_CONFIG',
    function ($routeParams, $location, Api, Auth, Cart, Toast, ApiError, APP_CONFIG) {
      var vm = this;
      var id = Number($routeParams.id);
      vm.loading = true;
      vm.product = null;
      vm.order = { size: null, quantity: 1 };
      vm.checks = {};   // dummy model for the "pick a size" rule
      vm.reviews = [];
      vm.mine = null;            // my existing review, if any
      vm.reviewForm = { rating: 0, comment: '' };
      vm.editing = false;
      vm.busy = false;
      vm.reviewBusy = false;
      vm.reviewError = null;

      var sizeOrder = APP_CONFIG.sizes;

      Api.allProducts().then(function (list) {
        vm.product = list.filter(function (p) { return p.id === id; })[0] || null;
        if (vm.product) {
          vm.product.sizes = (vm.product.sizes || []).slice().sort(function (a, b) {
            return sizeOrder.indexOf(a.size) - sizeOrder.indexOf(b.size);
          });
          var firstInStock = vm.product.sizes.filter(function (s) { return s.stockQuantity > 0; })[0];
          if (firstInStock && vm.product.sizes.length === 1) vm.order.size = firstInStock.size;
        }
      }).catch(function (res) { Toast.error(ApiError.message(res)); })
        .finally(function () { vm.loading = false; });

      function loadReviews() {
        return Api.productReviews(id).then(function (list) {
          vm.reviews = list;
          var me = Auth.user();
          vm.mine = list.filter(function (r) { return r.user && r.user.email === me.email; })[0] || null;
          vm.average = list.length ? list.reduce(function (s, r) { return s + r.rating; }, 0) / list.length : 0;
        });
      }
      loadReviews();

      vm.stockFor = function (size) {
        var s = vm.product && vm.product.sizes.filter(function (x) { return x.size === size; })[0];
        return s ? s.stockQuantity : 0;
      };
      vm.maxQty = function () { return vm.order.size ? Math.min(vm.stockFor(vm.order.size), 10) : 10; };
      vm.inCart = function () {
        return Cart.items.filter(function (i) { return i.productId === id; })[0];
      };

      vm.pickSize = function (s, form) {
        if (s.stockQuantity <= 0) return;
        vm.order.size = s.size;
        if (vm.order.quantity > vm.maxQty()) vm.order.quantity = vm.maxQty();
        if (form && form.size) form.size.$setTouched();
      };

      vm.addToCart = function (form) {
        if (form.$invalid) return;
        vm.busy = true;
        Api.addToCart(id, vm.order.size, vm.order.quantity)
          .then(function (msg) {
            Toast.success(msg || 'Added to your bag.');
            return Cart.refresh();
          })
          .catch(function (res) { Toast.error(ApiError.message(res)); })
          .finally(function () { vm.busy = false; });
      };

      vm.startEdit = function () {
        vm.editing = true;
        vm.reviewForm = { rating: vm.mine.rating, comment: vm.mine.comment };
      };
      vm.cancelEdit = function (form) {
        vm.editing = false; vm.reviewError = null;
        vm.reviewForm = { rating: 0, comment: '' };
        if (form) { form.$setPristine(); form.$setUntouched(); }
      };

      vm.submitReview = function (form) {
        vm.reviewError = null;
        if (form.$invalid) return;
        vm.reviewBusy = true;
        var r = vm.reviewForm, comment = r.comment.trim();
        var call = vm.mine ? Api.updateReview(id, r.rating, comment) : Api.createReview(id, r.rating, comment);
        call.then(function () {
          Toast.success(vm.mine ? 'Review updated.' : 'Thanks for your review!');
          vm.cancelEdit(form);
          return loadReviews();
        }).catch(function (res) {
          var msg = ApiError.message(res);
          vm.reviewError = /did not purchase/i.test(msg) ? 'You can review this after you\'ve ordered it.' : msg;
        }).finally(function () { vm.reviewBusy = false; });
      };

      vm.deleteReview = function () {
        Api.deleteReview(id).then(function () {
          Toast.success('Review deleted.');
          vm.mine = null;
          return loadReviews();
        }).catch(function (res) { Toast.error(ApiError.message(res)); });
      };
    }]);

  /** My reviews — ReviewDTO has no product id, so we link by product name */
  mod.controller('MyReviewsController', ['Api', 'ApiError', function (Api, ApiError) {
    var vm = this;
    vm.loading = true;
    vm.reviews = [];
    var byName = {};
    Api.allProducts().then(function (list) { list.forEach(function (p) { byName[p.name] = p.id; }); });
    Api.myReviews()
      .then(function (list) { vm.reviews = list; })
      .catch(function (res) { vm.error = ApiError.message(res); })
      .finally(function () { vm.loading = false; });
    vm.productId = function (r) { return r.product && byName[r.product.name]; };
  }]);
})();
