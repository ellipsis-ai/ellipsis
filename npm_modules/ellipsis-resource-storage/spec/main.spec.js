const Q = require('q');
const defaultStorage = require('../../ellipsis-default-storage/index.js');
const ellipis = {
  apiBaseUrl: "https://ellipsis-matteo.ngrok.io",
  token: "PyIVMpDdSBSiJTOsDwadYw"
}
const resourceStorage = require("../index.js")({
  ellipsis: ellipsis
});

const AWS = require('aws-sdk');
const credentials = new AWS.SharedIniFileCredentials({profile: 'ellipsis_dev'});
AWS.config.credentials = credentials;
var dynamodb = new AWS.DynamoDB.DocumentClient({region: 'us-east-1'});
var ITEM_TABLE_NAME = 'default_storage_items';

const getTestItemKeys = () => {
  var deferred = Q.defer();
  var params = {
    TableName: ITEM_TABLE_NAME,
    FilterExpression: "#item.metadata.test = :val",
    ExpressionAttributeValues: { ":val": true },
    ExpressionAttributeNames: { "#item": "item" }
  };
  dynamodb.scan(params, (err, data) => {
      if (err)
        deferred.reject(new Error(JSON.stringify(err, null, 2)));
      else
        deferred.resolve(data.Items.map((i) => i.item_primary_key));
  });
  return deferred.promise;
}

const batchDeleteItems = (keys) => {
  var deleteRequests = keys.map((k) => {
    return { DeleteRequest: { Key: { "item_primary_key": k } } }
  });
  var params = {
    RequestItems: { "default_storage_items": deleteRequest }
  };
  var deferred = Q.defer();
  dynamodb.batchWrite(params, function(err, data) {
    if (err)
      deferred.reject(new Error(JSON.stringify(err, null, 2)));
    else
      deferred.resolve(JSON.stringify(data, null, 2));
  });
  return deferred.promise;
}

const testResources = [
  {
    id: "1",
    type: "ec2_instance",
    status: "free",
    updatedBy: "matteo",
    reservations: [],
    metadata: {
      test: true
    }
  },
  {
    id: "2",
    type: "ec2_instance",
    status: "free",
    updatedBy: "john",
    reservations: [],
    metadata: {
      test: true
    }
  }
]


// set up: create item table if does not exists
// tests:
//   - insert record
//   - get record
//   - update record
//   - delete record
describe("The Resource Storage", () => {

  // beforeAll((done) => {
  //   var params = {
  //     ExclusiveStartTableName: ITEM_TABLE_NAME,
  //     Limit: 100
  //   };
  //   // dynamodb.listTables(params, (err, data) => {
  //   //     if (err) {
  //   //       console.log("Cannot access DynamoDB. Error: " + err);
  //   //     }
  //   //     else {
  //   //       if (!data.TableNames.find(ITEM_TABLE_NAME)) {
  //   //         console.log("Table " + ITEM_TABLE_NAME + "not found. All test will fail.");
  //   //       }
  //   //     }
  //   //     done();
  //   // });
  // });
  describe(", #getPromise, ", () => {
    beforeEach((done) => {
      defaultStorage.putItem({
        itemId: testResource[0].id,
        itemType: "Resource",
        item: JSON.stringify(Resource),
        ellipsis: ellipsis,
        onSuccess: (response, body) => {
          done();
        },
        onError: (error, response, body) => {
          console.log("ERROR! --------------------------------------------");
          console.log("Oops something went wrong: " + response + "," + body);
          done();
        },
      });
    });
    afterEach((done) => {
      defaultStorage.putItem({
        itemId: testResource[0].id,
        itemType: "Resource",
        item: JSON.stringify(Resource),
        ellipsis: ellipsis,
        onSuccess: (response, body) => {
          done();
        },
        onError: (error, response, body) => {
          console.log("ERROR! --------------------------------------------");
          console.log("Oops something went wrong: " + response + "," + body);
          done();
        },
      });
    })

    it("it returns the resource", (done) => {
      var promise = resourceStorage.insertPromise(testResources[0]);
      expect(promise.then).toBeDefined();
      expect(promise.fail).toBeDefined();
      promise.then((resource) => {
        expect(resource).toBeDefined();
        expect(resource.id).toBe(testResources[0].id);
        expect(resource.type).toBe(testResources[0].type);
      }).then(() => {
        return resourceStorage.getPromise(testResources[0].id)
      }).then((resource) => {
        expect(resource).toBeDefined();
        expect(resource.id).toBe(testResources[0].id);
        expect(resource.type).toBe(testResources[0].type);
      })
      .catch((error) => {
        expect(promise.isFulfilled()).toBeTruthy();
      })
      .finally(done);
    });
  });

  describe(", #insertPromise, ", () => {
    it("it returns the resource", (done) => {
      var promise = resourceStorage.insertPromise(testResources[0]);
      expect(promise.then).toBeDefined();
      expect(promise.fail).toBeDefined();
      promise.then((resource) => {
        expect(resource).toBeDefined();
        expect(resource.id).toBe(testResources[0].id);
        expect(resource.type).toBe(testResources[0].type);
      }).then(() => {
        return resourceStorage.getPromise(testResources[0].id)
      }).then((resource) => {
        expect(resource).toBeDefined();
        expect(resource.id).toBe(testResources[0].id);
        expect(resource.type).toBe(testResources[0].type);
      })
      .catch((error) => {
        expect(promise.isFulfilled()).toBeTruthy();
      })
      .finally(done);
    });
  });

});
