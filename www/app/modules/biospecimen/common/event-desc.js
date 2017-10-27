angular.module('os.biospecimen.common.eventdesc', [])
  .directive('osEventDesc', function() {
    return {
      restrict: 'E',
 
      replace: 'true',

      scope: {
        eventPoint: '=',
        eventPointIntervalUnit: '=',
        eventLabel: '=',
        eventCode: '=',
        codingEnabled: '='
      },

      template:
        '<span>' +
          '<span ng-switch on="eventLabel != null">' +
            '<span ng-switch-when="true" ng-switch on="eventPoint != null">' +
              '<span ng-switch-when="true">' +
                '<span ng-if="eventPoint >= 0">' +
                  'T{{eventPoint}}{{\'cp.cpe_interval_units_abbr.\' + eventPointIntervalUnit | translate}}: '+
                '</span>' +
                '<span ng-if="eventPoint < 0">' +
                  '-T{{-eventPoint}}{{\'cp.cpe_interval_units_abbr.\' + eventPointIntervalUnit | translate}}: ' +
                '</span>' +
              '</span>' +
              '<span>' +
                '{{eventLabel}} ' +
              '</span>' +
              '<span ng-if="codingEnabled && !!eventCode">' +
                '({{eventCode}})' +
              '</span>' +
            '</span>' +
            '<span ng-switch-when="false" translate="visits.unplanned_visit">' +
              'Unplanned Visit' +
            '</span>' +
          '</span>' +
        '</span>'
    };
  });
