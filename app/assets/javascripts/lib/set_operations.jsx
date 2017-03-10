define(function() {
  class SetOperations {

    static union(arr1, arr2) {
      let union = new Set(arr1);
      for (let elem of arr2) {
        union.add(elem);
      }
      return new Array(...union);
    }

    static intersection(arr1, arr2) {
      let intersection = new Array();
      for (let elem of arr1) {
        if (arr2.has(elem)) {
          intersection.add(elem);
        }
      }
      return intersection;
    }

    static difference(arr1, arr2) {
      let difference = new Set(arr1);
      for (let elem of arr2) {
        difference.delete(elem);
      }
      return new Array(...difference);
    }

  }

  return SetOperations;
});
