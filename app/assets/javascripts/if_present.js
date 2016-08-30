/*
Functional ternary, useful for JSX

- Takes a value, and if truthy (not null, undefined, false, or empty string), maps it to a function.
- Note that for this function, the number 0 is considered truthy (because wtf JS)
- If the value is falsy, and a second function is provided, that is called instead.

Example:

return (
  <div>
    {ifPresent(maybeThing, thing => thing.toUpperCase(), () => "Sorry, nothing to see here.")}
  </div>
);

*/
define(function() {
  return function(maybeValue, ifPresentCallback, optionalIfEmptyCallback) {
    if (![null, undefined, false, ""].some(falsy => falsy === maybeValue)) {
      return ifPresentCallback(maybeValue);
    } else if (typeof optionalIfEmptyCallback === 'function') {
      return optionalIfEmptyCallback();
    }
  };
});
