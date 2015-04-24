angular.module('openspecimen')
  .factory('AuthorizationService', function($rootScope, $http, User, ApiUtil, ApiUrls) {
    var userRights = [];

    var resources = {
      'collection_protocol': 'CP',
      'participant_phi': 'PARTICIPANT',
      'participant_deid': 'PARTICIPANT_DEID',
      'visit_n_specimen': 'VISIT_N_SPECIMEN',
      'storage_container': 'STORAGE_CONTAINER',
      'user': 'USER',
      'order': 'ORDER'
    };

    var ops = {
      'create': 'CREATE',
      'update': 'UPDATE',
      'read': 'READ',
      'delete': 'Delete'
    }

    function isAllowed(allowedOps, requestedOps) {
      var allowed = false;
      for (var i = 0; i < requestedOps.length; ++i) {
        if (allowedOps.indexOf(requestedOps[i]) != -1) {
          allowed = true;
          break;
        }
      }
      return allowed;
    }

    return {
      initializeUserRights: function() {
        return User.getCurrentUserRoles().then(
          function(userRoles) {
            userRights = [];
            angular.forEach(userRoles, function(userRole) {
              var site = userRole.site ? userRole.site.name : null;
              var cp = userRole.collectionProtocol ? userRole.collectionProtocol.shortTitle : null;
              angular.forEach(userRole.role.acl, function(ac) {
                userRights.push({
                  site: site,
                  cp: cp,
                  resource: ac.resourceName,
                  operations: ac.operations.map(function(op) { return op.operationName; } )
                });
              });
            });
            return userRights;
          }
        );
      },

      isAllowed: function(opts) {
        if ($rootScope.currentUser.admin) {
          return true;
        }

        var allowed = false;
        for (var i = 0; i < userRights.length; i++) {
          if (!opts.sites && !opts.cp && userRights[i].resource == opts.resource) {
            //
            // For resources whose rights are independent of CP and/or Site
            //
            allowed = isAllowed(userRights[i].operations, opts.operations);

          } else if ((!userRights[i].site || (opts.sites && opts.sites.indexOf(userRights[i].site) != -1)) &&
                    (!userRights[i].cp || userRights[i].cp == opts.cp) &&
                    (userRights[i].resource == opts.resource)) {
            //
            // For resources whose rights are specified based on CP and/or Site
            //
            allowed = isAllowed(userRights[i].operations, opts.operations);
          }

          if (allowed) {
            break;
          }
        }

        return allowed;
      },

      getResource: function(name) {
        return resources[name];
      },

      getOp: function(name) {
        return ops[name];
      }
    }
  });