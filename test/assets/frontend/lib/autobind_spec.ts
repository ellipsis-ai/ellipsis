import autobind from "../../../../app/assets/frontend/lib/autobind";

class Foo {
  bar: string;
  constructor() {
    this.bar = "baz";
  }
  getBar() {
    return this.bar;
  }
  componentDidMount() {
    return this.bar;
  }
}

describe("autobind", () => {
  it("binds non-React methods to the instance", () => {
    const f = new Foo();
    autobind(f);
    const g = f.getBar;
    expect(g()).toBe("baz");
  });

  it("does not bind the constructor or React API methods", () => {
    const f = new Foo();
    autobind(f);
    const g = f.componentDidMount;
    const h = f.constructor;
    expect(() => {
      g();
    }).toThrow(TypeError);
    expect(() => {
      h();
    }).toThrow(TypeError);
  });
});
