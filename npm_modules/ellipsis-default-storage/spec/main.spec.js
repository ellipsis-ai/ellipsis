const Q = require('q');

const rs = require("../index.js")({
  ellipsis: {
    apiBaseUrl: "https://ellipsis-matteo.ngrok.io",
    token: "PyIVMpDdSBSiJTOsDwadYw"
  }
});

const resource = {
  id: "i-12345345",
  type: "ec2_instance",
  status: "free",
  updatedBy: "matteo",
  reservations: [],
  metadata: {},
}

describe("test the default storage npm package", () => {

  beforeAll( () => {
    var params = {
      ExclusiveStartTableName: 'table_name',
      Limit: 1000,
    };
    dynamodb.listTables(params, function(err, data) {
        if (err) console.log(err);
        else {
          console.log(data);
        }
    });
  });

  afterAll(() => {
  });


});
// set up: create item table if does not exists
// tests:
//   - insert record
//   - get record
//   - update record
//   - delete record

describe("#put_item", () => {
  it("stores Resource objects", (done) => {
    var promise = rs.insertPromise(resource);
    expect(promise.then).toBeDefined();
    expect(promise.fail).toBeDefined();
    promise.then((resource2) => {
      console.log(resource2);
      expect(resource2).toBeDefined();
      done();
    })
  });
});

describe("#get_item", () => {
  it("stores Resource objects", (done) => {
    var promise = rs.insertPromise(resource);
    expect(promise.then).toBeDefined();
    expect(promise.fail).toBeDefined();
    promise.then((resource2) => {
      console.log(resource2);
      expect(resource2).toBeDefined();
      done();
    })
  });
});
