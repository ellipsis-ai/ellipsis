import SequentialName from "../../../../app/assets/frontend/lib/sequential_name";

describe("SequentialName.nextFor", () => {
  it("returns a unique name, numbered to be at least the length of the array", () => {
    expect(SequentialName.nextFor(["Item"], (ea) => ea, "Item")).toBe("Item2");
    expect(SequentialName.nextFor(["Item1"], (ea) => ea, "Item")).toBe("Item2");
    expect(SequentialName.nextFor(["Item2"], (ea) => ea, "Item")).toBe("Item3");
    expect(SequentialName.nextFor(["Item4", "Item1"], (ea) => ea, "Item")).toBe("Item3");
    expect(SequentialName.nextFor(["Foo", "Bar"], (ea) => ea, "Item")).toBe("Item3");
    expect(SequentialName.nextFor([], (ea) => ea, "Item")).toBe("Item1");
  });
});
