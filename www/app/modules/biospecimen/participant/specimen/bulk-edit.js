angular.module('os.biospecimen.specimen')
  .controller('BulkEditSpecimensCtrl', function($scope, Specimen, SpecimensHolder, PvManager) {

      var spmnIds;

      function init() {
        $scope.specimen = new Specimen();

        spmnIds = (SpecimensHolder.getSpecimens() || []).map(function(spmn) { return spmn.id; });
        SpecimensHolder.setSpecimens(null);

        loadPvs();
      }

      function loadPvs() {
        $scope.biohazards = PvManager.getPvs('specimen-biohazard');
        $scope.specimenStatuses = PvManager.getPvs('specimen-status');
      }

      $scope.bulkUpdate = function() {
        Specimen.bulkEdit({detail: $scope.specimen, ids: spmnIds}).then(
          function(result) {
            $scope.back();
          }
        )
      }

      init();
    }
  )
