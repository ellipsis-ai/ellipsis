define(function() {
  class Sort {
    static arrayAlphabeticalBy(array, keyName) {
      var copy = array.slice();
      return copy.sort((a, b) => {
        const aLower = (a[keyName] || "").toLowerCase();
        const bLower = (b[keyName] || "").toLowerCase();
        if (aLower < bLower) {
          return -1;
        } else if (aLower > bLower) {
          return 1;
        } else {
          return 0;
        }
      });
    }
  }

  return Sort;
});
