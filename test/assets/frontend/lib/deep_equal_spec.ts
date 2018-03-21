import DeepEqual from "../../../../app/assets/frontend/lib/deep_equal";

class Foo {
  a: string;
  constructor(a) {
    this.a = a;
  }
}

class Bar {
  a: string;
  constructor(a) {
    this.a = a;
  }
}

describe("DeepEqual", () => {
  describe("isEqual", () => {
    it("returns true for deeply equal things", () => {
      expect(DeepEqual.isEqual(0, 0)).toBe(true);
      expect(DeepEqual.isEqual("0", "0")).toBe(true);
      expect(DeepEqual.isEqual([], [])).toBe(true);
      expect(DeepEqual.isEqual([0], [0])).toBe(true);

      const a = { b: "c" };
      expect(DeepEqual.isEqual(a, a)).toBe(true);

      function b() { return; }
      expect(DeepEqual.isEqual(b, b)).toBe(true);
      expect(DeepEqual.isEqual({ f: 0 }, { f: 0 })).toBe(true);
      expect(DeepEqual.isEqual(NaN, NaN)).toBe(true);
      expect(DeepEqual.isEqual(null, null)).toBe(true);
      expect(DeepEqual.isEqual({
        a: "1",
        c: ["3"],
        b: "2"
      }, {
        c: ["3"],
        b: "2",
        a: "1"
      })).toBe(true);

      expect(DeepEqual.isEqual(new Foo("a"), new Foo("a"))).toBe(true);
    });

    it('returns false for non-equal things', () => {
      expect(DeepEqual.isEqual(0, 1)).toBe(false);
      expect(DeepEqual.isEqual("0", 0)).toBe(false);
      expect(DeepEqual.isEqual([0], [1])).toBe(false);
      expect(DeepEqual.isEqual([0], [])).toBe(false);
      expect(DeepEqual.isEqual({ f: 0 }, { f: 1 })).toBe(false);
      expect(DeepEqual.isEqual(0, NaN)).toBe(false);
      expect(DeepEqual.isEqual(null, undefined)).toBe(false);
      expect(DeepEqual.isEqual(function() {}, function() {})).toBe(false);
      expect(DeepEqual.isEqual(new Foo("b"), new Bar("b"))).toBe(false);
      expect(DeepEqual.isEqual(new Bar("a"), new Bar("b"))).toBe(false);
    });
  })
});
