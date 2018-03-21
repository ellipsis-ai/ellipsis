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

function isTruthy<T>(maybeValue: Option<T>): maybeValue is T {
  var isFalsyValue = [null, undefined, false, ""].some(falsy => falsy === maybeValue as any);
  var isEmptyArray = Array.isArray(maybeValue) && maybeValue.length === 0;
  return !isFalsyValue && !isEmptyArray;
}

const ifPresent = function<T>(maybeValue: Option<T>, ifPresentCallback: (item: T) => any, optionalIfEmptyCallback?: () => any): any {
    if (isTruthy(maybeValue)) {
      return ifPresentCallback(maybeValue);
    } else if (typeof optionalIfEmptyCallback === 'function') {
      return optionalIfEmptyCallback();
    }
};

export default ifPresent;
