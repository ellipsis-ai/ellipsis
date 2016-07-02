"use strict";

const Q = require('q');

const cleanUpValue = (v) => {
  return v.replace(/^\"|\"$/g, "");
};

module.exports = function(s3, bucketName) {

  return {

    csvReports: function(onSuccess, onError) {
      const params = {
        "Bucket": bucketName
      };
      s3.listObjectsV2(params, (err, data) => {
        if (err) {
          onError(err);
        } else {
          const csvRegex = /\d+-aws-billing-csv-\d\d\d\d-\d\d/;
          const csvKeys =
            data.Contents.
              map((ea) => ea.Key).
              filter((ea) => csvRegex.test(ea));
          const reverseChrono = csvKeys.sort().reverse();
          onSuccess(reverseChrono);
        }
      });
    },

    csvReportsPromise: function() {
      const deferred = Q.defer();
      this.csvReports(deferred.resolve, deferred.reject);
      return deferred.promise;
    },

    totalFor: function(csvReportKey, onSuccess, onError) {
      const getObjectParams = {
        "Bucket": bucketName,
        "Key": csvReportKey
      };
      s3.getObject(getObjectParams, (err, data) => {
        if (err) {
          onError(err);
        } else {
          const lines = data.Body.toString("utf8").split("\n");
          const totalValues = lines[lines.length-3].split(",");
          const currency = cleanUpValue(totalValues[totalValues.length-6]);
          const amount = cleanUpValue(totalValues[totalValues.length-1]);
          onSuccess({
            currency: currency,
            amount: amount
          });
        }
      });
    },

    totalForPromise: function(csvReportKey) {
      const deferred = Q.defer();
      this.totalFor(csvReportKey, deferred.resolve, deferred.reject);
      return deferred.promise;
    }

  };

};
