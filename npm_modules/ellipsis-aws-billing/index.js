"use strict";

const cleanUpValue = (v) => {
  return v.replace(/^\"|\"$/g, "");
};

module.exports = function(s3, bucketName) {

  return {

    withCsvReports: function(cb) {
      const params = {
        "Bucket": bucketName
      };
      s3.listObjectsV2(params, (err, data) => {
        if (data) {
          const csvRegex = /\d+-aws-billing-csv-\d\d\d\d-\d\d/;
          const csvKeys =
            data.Contents.
              map((ea) => ea.Key).
              filter((ea) => csvRegex.test(ea));
          const reverseChrono = csvKeys.sort().reverse();
          cb(reverseChrono);
        } else {
          cb([]);
        }
      });
    },

    withTotalFor: function(csvReportKey, cb) {
      const getObjectParams = {
        "Bucket": bucketName,
        "Key": csvReportKey
      };
      s3.getObject(getObjectParams, (err, data) => {
        if (err) {
          cb({
            currency: "",
            amount: "N/A"
          });
        } else {
          const lines = data.Body.toString("utf8").split("\n");
          const totalValues = lines[lines.length-3].split(",");
          const currency = cleanUpValue(totalValues[totalValues.length-6]);
          const amount = cleanUpValue(totalValues[totalValues.length-1]);
          cb({
            currency: currency,
            amount: amount
          });
        }
      });
    }

  };

};
