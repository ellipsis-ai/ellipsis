"use strict";

const _ = require('underscore');
const Q = require('q');
const db = require('../ellipsis-default-storage/index.js');


const defaultResourceRecord = {
  type: "ec2_instance",
  status: "free",

  /*
  id:
  type:
  status: "free" | "taken" | "offline",
  createdAt:
  updateAt:
  updatedBy:
  reservations: []
  metadata: {},
  */
}


module.exports = ellipsisResourceStorage
function ellipsisResourceStorage(opt) {

  opt = opt || {}
  const ellipsis = opt.ellipsis;

  // private data

  return {

    insertPromise: function (resource) {
      const deferred = Q.defer();

      const t = (new Date).getTime();
      resource.createdAt = t;
      resource.updatedAt = t;
      db.putItem({
        itemId: resource.id,
        itemType: "resource",
        item: JSON.stringify(resource),
        ellipsis: ellipsis,
        onSuccess: (response, body) => {
          // Clone the resource object as it make it easier to use the function
          deferred.resolve({ resource: JSON.parse(JSON.stringify(resource)), result: response });
        },
        onError: (response, body) => {
          // Clone the resource object as it make it easier to use the function
          deferred.resolve({ resource: JSON.parse(JSON.stringify(resource)), result: response });
        },
      });
      return deferred.promise;
    },

    getPromise: function (resourceId) {
      const deferred = Q.defer();
      db.getItem({
        itemId: resourceId,
        itemType: "resource",
        ellipsis: ellipsis,
        onSuccess: (response, body) => {
          deferred.resolve(JSON.parse(body));
        },
        onError: (response, body) => {
          // In case we cannot find the record we return undefined
          deferred.resolve(body);
        }
      });
      return deferred.promise;
    },

    deletePromise: function (resourceId) {
      // no idea how to do this
    },

    updateResourcePromise: function (resourceId, attributes) {
      const deferred = Q.defer();
      getResource(resourceId)
        .then((resource) => {
          if (resource) {
            console.log("resource found");
            console.log(resource.resour);

            // ANDREW, LUKE: any idea why I have to do JSON.parse here?
            var updatedResource = _.extend(JSON.parse(resource), attributes);

            // Insert the updated resource
            insertResource(updatedResource).then((x) => {
              if (x.result === 'success') {
                 deferred.resolve(x.resource);
              } else {
                 deferred.resolve(null);
              }
            })
          } else {
            // if we do not find the record stop and return null
            deferred.resolve(null);
          }
        })
      return deferred.promise;
    }
  }

  // private functions
}
