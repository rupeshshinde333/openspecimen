angular.module('os.administrative.container.list', ['os.administrative.models'])
  .controller('ContainerListCtrl', function($scope, $state, Container, Util, DeleteUtil) {

    function init() {
      $scope.containerFilterOpts = {};
      $scope.loadContainers();
      Util.filter($scope, 'containerFilterOpts', $scope.loadContainers);
    }

    $scope.loadContainers = function(filterOpts) {
      Container.list(filterOpts).then(
        function(containers) {
          $scope.containerList = containers;
        }
      );
    }

    $scope.showContainerOverview = function(container) {
      $state.go('container-detail.overview', {containerId: container.id});
    };

    $scope.deleteContainer = function(container) {
      DeleteUtil.delete(container, {
        onDeletion: $scope.loadContainers,
        confirmDelete: 'container.confirm_delete'
      });
    }

    init();
  });