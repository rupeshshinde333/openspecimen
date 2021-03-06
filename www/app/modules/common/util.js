
angular.module('openspecimen')
  .factory('Util', function(
    $rootScope, $state, $stateParams, $timeout, $document, $q,
    $parse, $modal, $translate, $http, osRightDrawerSvc, ApiUrls, Alerts) {

    var isoDateRe = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*))(?:Z|(\+|-)([\d|:]*))?$/;
    function clear(input) {
      input.splice(0, input.length);
    };

    function unshiftAll(arr, elements) {
      Array.prototype.splice.apply(arr, [0, 0].concat(elements));
    };

    function assign(arr, elements) {
      clear(arr);
      unshiftAll(arr, elements);
    };

    function filterOpts(opts, filters) {
      if (!filters) {
        filters = $stateParams.filters;
      }

      if (!!filters) {
        try {
          angular.extend(opts, angular.fromJson(atob(filters)));
          osRightDrawerSvc.open();
        } catch (e) {
          console.log("Invalid filter");
        }
      }

      return opts;
    }

    function filter($scope, varName, callback, excludeParams) {
      $scope.$watch(varName, function(newVal, oldVal) {
        if (newVal == oldVal) {
          return;
        }

        if ($scope._filterQ) {
          $timeout.cancel($scope._filterQ);
        }

        $scope._filterQ = $timeout(
          function() {
            var filters = angular.copy(newVal);

            excludeParams = excludeParams || ['includeStats', 'maxResults'];
            angular.forEach(excludeParams, function(param) { delete filters[param]; });

            angular.forEach(filters,
              function(value, key) {
                if (!value) { delete filters[key]; }
              }
            );

            var fb = undefined;
            if (Object.keys(filters).length > 0) {
              fb = btoa(JSON.stringify(filters));
            }

            $stateParams.filters = fb;
            $state.go($state.current.name, $stateParams, {notify: false});
            callback(newVal);
          },
          $rootScope.global.filterWaitInterval
        );
      }, true);
    }

    function getEscapeMap(str) {
      var map = {}, insideSgl = false, insideDbl = false;
      var lastIdx = -1;

      for (var i = 0; i < str.length; ++i) {
        if (str[i] == "'" && !insideDbl) {
          if (insideSgl) {
            map[lastIdx] = i;
          } else {
            lastIdx = i;
          }

          insideSgl = !insideSgl;
        } else if (str[i] == '"' && !insideSgl) {
          if (insideDbl) {
            map[lastIdx] = i;
          } else {
            lastIdx = i;
          }

          insideDbl = !insideDbl;
        }
      }

      return map;
    }

    function getToken(token) {
      token = token.trim();
      if (token.length != 0) {
        if ((token[0] == "'" && token[token.length - 1] == "'") ||
            (token[0] == '"' && token[token.length - 1] == '"')) {
          token = token.substring(1, token.length - 1);
        }
      }

      return token;
    }

    function splitStr(str, re, returnEmpty) {
      var result = [], token = '', escUntil = undefined;
      var map = getEscapeMap(str);

      for (var i = 0; i < str.length; ++i) {
        if (escUntil == undefined) {
          escUntil = map[i];
        }

        if (i <= escUntil) {
          token += str[i];
          if (i == escUntil) {
            escUntil = undefined;
          }
        } else {
          if (re.exec(str[i]) == null) {
            token += str[i];
          } else {
            token = getToken(token);
            if (token.length > 0 || !!returnEmpty) {
              result.push(token);
            }
            token = '';
          }
        }
      }

      token = getToken(token);
      if (token.length > 0) {
        result.push(token);
      }

      return result;
    }

    function getDupObjects(objs, props) {
      var dupObjs = {};
      var scannedObjs = {};
      angular.forEach(props, function(prop) {
        dupObjs[prop] = [];
        scannedObjs[prop] = [];
      });

      angular.forEach(objs, function(obj) {
        angular.forEach(props, function(prop) {
          if (!obj[prop]) {
            return;
          }

          if (scannedObjs[prop].indexOf(obj[prop]) >= 0) {
            if (dupObjs[prop].indexOf(obj[prop]) == -1) {
              dupObjs[prop].push(obj[prop]);
            }
          }

          scannedObjs[prop].push(obj[prop]);
        })
      });
 
      return dupObjs;
    }

    function hidePopovers() {
      var popovers = $document.find('div.popover');
      angular.forEach(popovers, function(popover) {
        angular.element(popover).scope().$hide();
      });
    }

    function getNumberInScientificNotation(input, minRange, fractionDigits) {
      minRange = minRange || 1000000;
      fractionDigits = fractionDigits || undefined;
      
      input = +input;
      if (angular.isNumber(input) && !isNaN(input) && input >= minRange) {
        input = fractionDigits != undefined ? input.toExponential(fractionDigits) : input.toExponential();
      }

      return input;
    }

    function parseDate(value) {
      if (typeof value === 'string') {
        var matches = isoDateRe.exec(value);
        if (matches) {
          return new Date(value);
        }
      }

      return value;
    }

    function downloadReport(entity, msgClass, filename) {
      var alert = Alerts.info(msgClass + '.report_gen_initiated', {}, false);
      entity.generateReport().then(
        function(result) {
          Alerts.remove(alert);
          if (result.completed) {
            Alerts.info(msgClass + '.downloading_report');

            filename = (filename || entity.name) + '.csv';
            downloadFile(ApiUrls.getBaseUrl() + 'query/export?fileId=' + result.dataFile + '&filename=' + filename);
          } else if (result.dataFile) {
            Alerts.info(msgClass + '.report_will_be_emailed');
          }
        },

        function() {
          Alerts.remove(alert);
        }
      );
    }

    function downloadFile(fileUrl) {
      $http({method: 'GET', url: fileUrl, responseType: 'arraybuffer'}).then(
        function(resp) {
          var headers = resp.headers;

          var contentType = headers('content-type');
          var filename = headers('content-disposition');
          filename = filename.substr(filename.indexOf('=') + 1);

          var link = angular.element('<a/>');
          try {
            var blob = new Blob([resp.data], { type: contentType });
            var url = window.URL.createObjectURL(blob);

            link.attr({href: url, download: filename});
            var clickEvent = new MouseEvent("click", {"view": window, "bubbles": true, "cancelable": false});
            link[0].dispatchEvent(clickEvent);
          } catch (ex) {
            console.log(ex);
          }
        }
      );
    }

    function booleanPromise(condition) {
      var deferred = $q.defer();
      if (condition) {
        deferred.resolve(true);
      } else {
        deferred.reject(false);
      }
      
      return deferred.promise;
    }

    function appendAll(array, elements) {
      Array.prototype.push.apply(array, elements);
    }

    function merge(src, dst, deep) {
      var h = dst.$$hashKey;

      if (!angular.isObject(src) && !angular.isFunction(src)) {
        return dst;
      }

      var keys = Object.keys(src);
      for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        var value = src[key];

        if (deep && angular.isObject(value)) {
          if (angular.isDate(value)) {
            dst[key] = new Date(value.valueOf());
          } else {
            if (!angular.isObject(dst[key])) {
              dst[key] = angular.isArray(value) ? [] : {};
            }

            merge(value, dst[key], true);
          }
        } else {
          dst[key] = value;
        }
      }

      if (h) {
        dst.$$hashKey = h;
      } else {
        delete dst.$$hashKey;
      }

      return dst;
    }

    function copyAttrs(src, attrs, array) {
      angular.forEach(array,
        function(dst) {
          if (dst == src) {
            return;
          }

          angular.forEach(attrs,
            function(attr) {
              dst[attr] = src[attr];
            }
          );
        }
      );
    }

    function addIfAbsent(dstArray, srcArray, keyProp) {
      if (!keyProp) {
        return;
      }

      var key = $parse(keyProp);
      var map = {};
      angular.forEach(dstArray,
        function(obj) {
          map[key(obj)] = obj;
        }
      );

      angular.forEach(srcArray,
        function(obj) {
          if (!map[key(obj)]) {
            dstArray.push(obj);
            map[key(obj)] = obj;
          }
        }
      );
    }

    function showConfirm(opts) {
      $modal.open({
        templateUrl: opts.templateUrl || 'modules/common/show-confirm.html',
        controller: function($scope, $modalInstance) {
          angular.extend($scope, opts);

          $scope.ok = function() {
            $modalInstance.close(true);
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      }).result.then(
        function() {
          if (opts.ok) {
            opts.ok();
          }
        },
        function() {
          if (opts.cancel) {
            opts.cancel();
          }
        }
      );
    }

    function validateItems(items, itemLabels, labelProp) {
      var labelGetter = $parse(labelProp);

      // for checking presence of element, using map is faster than list
      var labelsMap = {};
      angular.forEach(items,
        function(item) {
          labelsMap[labelGetter(item)] = true;
        }
      );

      var found = [], notFound = [], extra = [];
      angular.forEach(itemLabels,
        function(label) {
          if (labelsMap[label]) {
            found.push(label);
            delete labelsMap[label];
          } else {
            notFound.push(label);
          }
        }
      );

      angular.forEach(labelsMap,
        function(value, label) {
          extra.push(label);
        }
      );

      return {found: found, notFound: notFound, extra: extra};
    }

    function showItemsValidationResult(msgKeys, data) {
      return $modal.open({
        templateUrl: 'modules/common/item-validation-result.html',
        controller: function($scope, $modalInstance) {
          $scope.ctx = {msgKeys: msgKeys, data: data};

          $scope.ok = function() {
            $modalInstance.close(true);
          }

          $scope.generateReport = function() {
            generateValidationReport(msgKeys, data);
          }
        }
      });
    }

    function generateValidationReport(msgKeys, data) {
      var msg = $translate.instant;
      $translate(msgKeys.label).then(
        function() {
          var report = '';
          report = msg(msgKeys.itemLabel) + ',' + msg(msgKeys.error) + '\n';

          var notFound = msg(msgKeys.notFoundError);
          angular.forEach(data.notFound,
            function(label) {
              report += label + ',' + notFound + '\n';
            }
          );

          var extra = msg(msgKeys.extraError);
          angular.forEach(data.extra,
            function(label) {
              report += label + ',' + extra + '\n';
            }
          );

          var success = copyToClipboard(report);
          if (success) {
            Alerts.info(msgKeys.reportCopied);
          }
        }
      );
    }

    function copyToClipboard(text) {
      var textarea = angular.element('<textarea/>').val(text).css('opacity', 0);
      angular.element(document.body).append(textarea);
      textarea.select();

      var success = false;
      try {
        document.execCommand('copy');
        success = true;
      } catch (e) {
        console.log("Error copying text to clipboard");
      }

      textarea.remove();
      return success;
    }

    return {
      clear: clear,

      unshiftAll: unshiftAll,

      assign: assign,

      filterOpts: filterOpts,

      filter: filter,

      splitStr: splitStr,

      getDupObjects: getDupObjects,

      hidePopovers: hidePopovers,

      getNumberInScientificNotation: getNumberInScientificNotation,

      parseDate: parseDate,

      downloadReport : downloadReport,

      downloadFile: downloadFile,

      booleanPromise: booleanPromise,

      appendAll: appendAll,

      merge: merge,

      copyAttrs: copyAttrs,

      addIfAbsent: addIfAbsent,

      showConfirm: showConfirm,

      validateItems: validateItems,

      showItemsValidationResult: showItemsValidationResult,

      copyToClipboard: copyToClipboard
    };
  });
