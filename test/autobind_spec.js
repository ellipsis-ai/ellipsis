const autobind = require('../app/assets/javascripts/lib/autobind');

describe("autobind", () => {
  class Foo {
    constructor() {
      this.prop = "set";
    }
    foo() {
      return this.bar();
    }
    bar() {
      return "success";
    }
    render() {
      return "rendered";
    }
  }
  it("sets the context of methods of an object to the object except React lifecycle methods and the constructor", () => {
    const f = new Foo();
    autobind(f);
    const g = f.foo;
    expect(g()).toEqual("success");
    expect(f.foo).not.toBe(Foo.prototype.foo);
    expect(f.constructor).toBe(Foo.prototype.constructor);
    expect(f.render).toBe(Foo.prototype.render);
  });
});
