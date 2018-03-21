const SetOperations = {
  union: function <T>(arr1: Array<T>, arr2: Array<T>): Array<T> {
    const union = new Set(arr1);
    for (let elem of arr2) {
      union.add(elem);
    }
    return Array.from(union);
  },

  intersection: function <T>(arr1: Array<T>, arr2: Array<T>): Array<T> {
    const intersection: Set<T> = new Set();
    for (let elem of arr1) {
      if (arr2.includes(elem)) {
        intersection.add(elem);
      }
    }
    return Array.from(intersection);
  },

  difference: function <T>(arr1: Array<T>, arr2: Array<T>): Array<T> {
    const difference: Set<T> = new Set(arr1);
    for (let elem of arr2) {
      difference.delete(elem);
    }
    return Array.from(difference);
  }
};

export default SetOperations;
