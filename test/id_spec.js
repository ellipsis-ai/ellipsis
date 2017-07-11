const ID = require('../app/assets/javascripts/lib/id');

describe("ID", () => {

  const uuid = "2025698a-5ee3-4264-a9ed-c8befe6f61bd";
  const uuidArray = jest.fn(() => {
    const stripped = uuid.replace(/-/g, "");
    const arr = new Uint8Array(16);
    for (let i = 0; i < stripped.length; i += 2) {
      arr[i / 2] = parseInt(stripped.substr(i, 2), 16);
    }
    return arr;
  });

  describe("toBase64", () => {
    it("should be consistent with the Scala implementation of IDs.uuidToBase64", () => {
      const arr = uuidArray();
      expect(ID.toBase64(arr)).toBe("ICVpil7jQmSp7ci-_m9hvQ");
    });
  });
});
