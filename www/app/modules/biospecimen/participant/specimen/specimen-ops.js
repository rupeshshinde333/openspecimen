angular.module('os.biospecimen.specimen')
  .directive('osSpecimenOps', function($state, $injector, Specimen, SpecimensHolder, Alerts, DeleteUtil) {

    function initOpts(scope) {
      //
      // TODO: idea is to take either CP or site or both as input and create resource opts
      //
      if (!scope.resourceOpts) {
        scope.resourceOpts = {
          orderCreateOpts:    {resource: 'Order', operations: ['Create']},
          shipmentCreateOpts: {resource: 'ShippingAndTracking', operations: ['Create']},
          specimenUpdateOpts: {resource: 'VisitAndSpecimen', operations: ['Update']},
          specimenDeleteOpts: {resource: 'VisitAndSpecimen', operations: ['Delete']}
        };
      }

      scope.reqBasedDistOrShip = {value: false};
      if ($injector.has('spmnReqCfgUtil')) {
        $injector.get('spmnReqCfgUtil').isReqBasedDistOrShippingEnabled().then(
          function(result) {
            scope.reqBasedDistOrShip.value = (result.value == 'true');
          }
        );
      }
    }

    return {
      restrict: 'E',

      replace: true,

      scope: {
        specimens: '&',
        initList: '&',
        resourceOpts: '=?'
      },

      templateUrl: 'modules/biospecimen/participant/specimen/specimen-ops.html',

      link: function(scope, element, attrs) {
        initOpts(scope);

        function gotoView(state, params, msgCode) {
          var selectedSpmns = scope.specimens();
          if (!selectedSpmns || selectedSpmns.length == 0) {
            Alerts.error('specimen_list.' + msgCode);
            return;
          }

          var specimenIds = selectedSpmns.map(function(spmn) {return spmn.id});
          Specimen.getByIds(specimenIds).then(
            function(spmns) {
              SpecimensHolder.setSpecimens(spmns);
              $state.go(state, params);
            }
          );
        }

        scope.editSpecimens = function() {
          var spmns = scope.specimens();
          if (!spmns || spmns.length == 0) {
            Alerts.error('specimen_list.no_specimens_to_edit');
            return;
          }

          SpecimensHolder.setSpecimens(spmns);
          $state.go('specimen-bulk-edit');
        }

        scope.deleteSpecimens = function() {
          var spmns = scope.specimens();
          if (!spmns || spmns.length == 0) {
            Alerts.error('specimens.no_specimens_for_delete');
            return;
          }

          var specimenIds = spmns.map(function(spmn) { return spmn.id; });
          var opts = {
            confirmDelete: 'specimens.delete_specimens_heirarchy',
            successMessage: 'specimens.specimens_hierarchy_deleted',
            onBulkDeletion: scope.initList
          }
          DeleteUtil.bulkDelete({bulkDelete: Specimen.bulkDelete}, specimenIds, opts);
        }

        scope.distributeSpecimens = function() {
          gotoView('order-addedit', {orderId: ''}, 'no_specimens_for_distribution');
        }

        scope.shipSpecimens = function() {
          gotoView('shipment-addedit', {shipmentId: ''}, 'no_specimens_for_shipment');
        }

        scope.createAliquots = function() {
          gotoView('specimen-bulk-create-aliquots', {}, 'no_specimens_to_create_aliquots');
        }

        scope.createDerivatives = function() {
          gotoView('specimen-bulk-create-derivatives', {}, 'no_specimens_to_create_derivatives');
        }

        scope.addEvent = function() {
          gotoView('bulk-add-event', {}, 'no_specimens_to_add_event');
        }

        scope.retrieveSpecimens = function() {
          var selectedSpmns = scope.specimens();
          if (!selectedSpmns || selectedSpmns.length == 0) {
            Alerts.error('specimen_list.no_specimens_to_retrieve');
            return;
          }

          var spmnsToUpdate = selectedSpmns.map(function(spmn) { return {id: spmn.id, storageLocation: {}}; });
          Specimen.bulkUpdate(spmnsToUpdate).then(
            function(updatedSpmns) {
              scope.initList();
            }
          );
        }
      }
    };
  });
