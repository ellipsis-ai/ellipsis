import UniqueBy from "../../../../app/assets/frontend/lib/unique_by";

describe("UniqueBy.forArray", () => {
  it("filters an array of items to those unique for a given property", () => {
    const arr = [{
      name: "Joseph"
    }, {
      name: "Jane"
    }, {
      name: "Jenny"
    }, {
      name: "Jane"
    }, {
      name: "Joseph"
    }, {
      name: "Joshua"
    }];
    expect(UniqueBy.forArray(arr, "name")).toEqual([{
      name: "Joseph"
    }, {
      name: "Jane"
    }, {
      name: "Jenny"
    }, {
      name: "Joshua"
    }]);
  })
});
