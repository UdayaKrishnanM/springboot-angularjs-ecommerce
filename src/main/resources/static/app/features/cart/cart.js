(function () {
  'use strict';

  /*
   * Bag + checkout.
   * Checkout = POST /createOrder with every bag line, then empty the bag, then go to payment.
   * Before placing the order we re-check each line against live stock.
   */
  angular.module('shop').controller('CartController', ['$location', '$q', 'Api', 'Cart', 'Toast', 'ApiError', 'APP_CONFIG',
    function ($location, $q, Api, Cart, Toast, ApiError, APP_CONFIG) {
      var vm = this;
      vm.cart = Cart;
      vm.loading = true;
      vm.busyId = null;
      vm.placing = false;
      vm.error = null;
      vm.ack = false;
      var products = {};   // id -> product with sizes/stock

      $q.all([Cart.refresh(), Api.allProducts()])
        .then(function (r) { r[1].forEach(function (p) { products[p.id] = p; }); })
        .catch(function (res) { vm.error = ApiError.message(res); })
        .finally(function () { vm.loading = false; });

      vm.sizesFor = function (item) {
        var p = products[item.productId];
        if (!p) return [item.product.sizeType];
        return APP_CONFIG.sizes.filter(function (s) {
          return (p.sizes || []).some(function (x) { return x.size === s && (x.stockQuantity > 0 || s === item.product.sizeType); });
        });
      };
      vm.stock = function (item) {
        var p = products[item.productId];
        var s = p && (p.sizes || []).filter(function (x) { return x.size === item.product.sizeType; })[0];
        return s ? s.stockQuantity : null;
      };
      /** null when fine, otherwise a message — used to block checkout */
      vm.problem = function (item) {
        var stock = vm.stock(item);
        if (stock === null) return null;
        if (stock === 0) return 'Out of stock in ' + item.product.sizeType + '.';
        if (item.quantity > stock) return 'Only ' + stock + ' left in ' + item.product.sizeType + '.';
        return null;
      };
      vm.hasProblems = function () { return Cart.items.some(function (i) { return !!vm.problem(i); }); };

      function run(item, promise, okMsg) {
        vm.busyId = item.id;
        return promise
          .then(function () { if (okMsg) Toast.success(okMsg); return Cart.refresh(); })
          .catch(function (res) { Toast.error(ApiError.message(res)); })
          .finally(function () { vm.busyId = null; });
      }

      // Backend adds the sent quantity to the line for the same size, so we send +1 / -1
      vm.inc = function (item) {
        var stock = vm.stock(item);
        if (stock !== null && item.quantity >= stock) { Toast.info('No more stock in ' + item.product.sizeType + '.'); return; }
        if (item.quantity >= 10) { Toast.info('Maximum 10 per item.'); return; }
        run(item, Api.updateCartItem(item.productId, item.product.sizeType, 1));
      };
      vm.dec = function (item) {
        if (item.quantity <= 1) return;
        run(item, Api.updateCartItem(item.productId, item.product.sizeType, -1));
      };
      // ng-model already holds the new size; on failure Cart.refresh() puts the old one back
      vm.changeSize = function (item) {
        var size = item.product.sizeType;
        var p = products[item.productId];
        var s = p && (p.sizes || []).filter(function (x) { return x.size === size; })[0];
        if (s && item.quantity > s.stockQuantity) {
          Toast.error('Only ' + s.stockQuantity + ' left in ' + size + '. Lower the quantity first.');
          Cart.refresh();
          return;
        }
        run(item, Api.updateCartItem(item.productId, size, item.quantity), 'Size changed to ' + size + '.')
          .then(function () { Cart.refresh(); });
      };
      vm.remove = function (item) {
        run(item, Api.removeCartItem(item.id), item.product.name + ' removed.');
      };

      vm.checkout = function (form) {
        vm.error = null;
        if (form.$invalid) return;
        if (vm.hasProblems()) { vm.error = 'Fix the highlighted items before placing your order.'; return; }
        vm.placing = true;
        var lines = Cart.items.map(function (i) {
          return { productId: i.productId, size: i.product.sizeType, quantity: i.quantity };
        });
        var ids = Cart.items.map(function (i) { return i.id; });
        Api.createOrder(lines)
          .then(function (order) {
            // empty the bag (best-effort; the order is already placed)
            return $q.all(ids.map(function (id) { return Api.removeCartItem(id).catch(angular.noop); }))
              .then(function () { Cart.refresh(); return order; });
          })
          .then(function (order) {
            Toast.success('Order placed.');
            if (order && order.id) $location.path('/pay/' + order.id);
            else $location.path('/orders');
          })
          .catch(function (res) { vm.error = ApiError.message(res, 'Couldn\'t place the order.'); })
          .finally(function () { vm.placing = false; });
      };
    }]);
})();
