define(function() {
  /*
   Functional ternary, useful for JSX

   Takes a value, and if truthy, maps it to a function.

   If the value is falsy, and a second function is provided, that is called instead.

   - Falsy things:
     - null
     - undefined
     - false
     - empty string
     - empty array
   - Truthy things:
     - everything else, including the number 0

   Example:

   return (
     <div>
       {ifPresent(maybeThing, thing => thing.toUpperCase(), () => "Sorry, nothing to see here.")}
     </div>
   );

   */
  return function(maybeValue, ifPresentCallback, optionalIfEmptyCallback) {
    var isFalsyValue = [null, undefined, false, ""].some(falsy => falsy === maybeValue);
    var isFalsyArray = Array.isArray(maybeValue) && maybeValue.length === 0;
    if (!isFalsyValue && !isFalsyArray) {
      return ifPresentCallback(maybeValue);
    } else if (typeof optionalIfEmptyCallback === 'function') {
      return optionalIfEmptyCallback();
    }
  };
});
