
angular.module('os.administrative.user.detail', ['os.administrative.models'])
  .controller('UserDetailCtrl', function(
    $scope, $q, $translate, user, 
    User, AuthDomain, PvManager, Alerts, DeleteUtil) {

    function init() {
      $scope.user = user;
      loadPvs();
    }

    function loadPvs() {
      $scope.sites = PvManager.getSites();
      $scope.domains = [];
      AuthDomain.getDomainNames().then(function(domains) {
        $scope.domains = domains;
      })
    }

    function updateStatus(status, msgKey) {
      $scope.user.updateStatus(status).then(
        function(savedUser) {
          $scope.user = savedUser;
          Alerts.success(msgKey);
        }
      );
    }

    $scope.editUser = function(property, value) {
      var d = $q.defer();
      d.resolve({});
      return d.promise;
    }

    $scope.activate = function() {
      var msgKey = $scope.user.activityStatus == 'Locked' ? 'user.user_unlocked' : 'user.user_request_approved';
      updateStatus('Active', msgKey);
    }

    $scope.lock = function() {
      updateStatus('Locked', 'user.user_locked');
    }

    $scope.deleteUser = function() {
      DeleteUtil.delete($scope.user, {
        onDeleteState: 'user-list',
        confirmDelete: $scope.user.activityStatus == 'Pending' ? 'user.confirm_reject' : undefined
      });
    }

    init();

  });
