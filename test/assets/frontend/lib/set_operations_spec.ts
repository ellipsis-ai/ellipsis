import SetOperations from "../../../../app/assets/frontend/lib/set_operations";

describe("SetOperations", () => {
  describe("union", () => {
    it("returns the union of two arrays", () => {
      expect(SetOperations.union([0, 1, 2], [2, 3, 4])).toEqual([0, 1, 2, 3, 4]);
    });
  });

  describe("intersection", () => {
    it('returns the intersection of two arrays', () => {
      expect(SetOperations.intersection([0, 1, 2], [2, 3, 4])).toEqual([2]);
    });
  });

  describe("difference", () => {
    it('returns the items that are unique to the first array', () => {
      expect(SetOperations.difference([0, 1, 2], [2, 3, 4])).toEqual([0, 1]);
    })
  });
});
