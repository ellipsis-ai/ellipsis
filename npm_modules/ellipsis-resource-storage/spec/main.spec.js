const Q = require('q');

const rs = require("../index.js")({
  ellipsis: {
    apiBaseUrl: "https://ellipsis-matteo.ngrok.io/",
    token: "PyIVMpDdSBSiJTOsDwadYw"
  }
});

const resource = {
  id: "i-123456",
  type: "ec2_instance",
  status: "free",
  updatedBy: "matteo",
  reservations: [],
  metadata: {},
}

describe("Resource Storage", () => {
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
