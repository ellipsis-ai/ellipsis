import ifPresent from "../../../../app/assets/frontend/lib/if_present";

describe("ifPresent", () => {
  function truthy(ea: any) {
    return `Truthy: ${ea}`;
  }

  function falsy() {
    return 'Falsy';
  }

  it('calls the first callback with the value for truthy values', () => {
    expect(ifPresent(true, truthy)).toEqual("Truthy: true");
    expect(ifPresent("tada", truthy)).toEqual("Truthy: tada");
    expect(ifPresent(0, truthy)).toEqual("Truthy: 0");
    expect(ifPresent([0], truthy)).toEqual("Truthy: 0");
    expect(ifPresent({}, truthy)).toEqual("Truthy: [object Object]");
    expect(ifPresent({ toString: function() { return "surprise!"; } }, truthy)).toEqual("Truthy: surprise!");
  });
  it('calls the first callback with the value for truthy values with a second callback', () => {
    expect(ifPresent(true, truthy, falsy)).toEqual("Truthy: true");
    expect(ifPresent("tada", truthy, falsy)).toEqual("Truthy: tada");
    expect(ifPresent(0, truthy, falsy)).toEqual("Truthy: 0");
    expect(ifPresent([0], truthy, falsy)).toEqual("Truthy: 0");
    expect(ifPresent({}, truthy, falsy)).toEqual("Truthy: [object Object]");
    expect(ifPresent({ toString: function() { return "surprise!"; } }, truthy, falsy)).toEqual("Truthy: surprise!");
  });
  it('returns nothing for falsy values if no second callback provided', () => {
    expect(ifPresent(false, truthy)).toBeUndefined();
    expect(ifPresent(null, truthy)).toBeUndefined();
    expect(ifPresent(undefined, truthy)).toBeUndefined();
    expect(ifPresent([], truthy)).toBeUndefined();
    expect(ifPresent("", truthy)).toBeUndefined();
  });
  it('calls the second callback for falsy values', () => {
    expect(ifPresent(false, truthy, falsy)).toEqual("Falsy");
    expect(ifPresent(null, truthy, falsy)).toEqual("Falsy");
    expect(ifPresent(undefined, truthy, falsy)).toEqual("Falsy");
    expect(ifPresent([], truthy, falsy)).toEqual("Falsy");
    expect(ifPresent("", truthy, falsy)).toEqual("Falsy");
  });
});
