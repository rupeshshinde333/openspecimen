angular.module('os.biospecimen.specimen.bulkedit', [])
  .controller('BulkEditSpecimensCtrl', function(
    $scope, Specimen, SpecimensHolder, PvManager) {

      function init() {
        $scope.specimen = new Specimen();
        loadPvs();
      }

      function loadPvs() {
        $scope.biohazards = PvManager.getPvs('specimen-biohazard');
        $scope.specimenStatuses = PvManager.getPvs('specimen-status');
      }

      $scope.bulkUpdate = function() {
        var specimens = SpecimensHolder.getSpecimens() || [];
        var specimenIds = specimens.map(function(specimen) { return specimen.id; });
        Specimen.bulkEdit({detail: $scope.specimen, ids: specimenIds}).then(
          function(result) {
            $scope.back();
          }
        )

        SpecimensHolder.setSpecimens(null);
      }

      init();
    }
  )
