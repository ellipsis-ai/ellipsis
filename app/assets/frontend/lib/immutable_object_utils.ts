const ImmutableObjectUtils = {
    // Create a copy of an array before modifying it
    arrayWithNewElementAtIndex: function<T>(array: ReadonlyArray<T>, newElement: T, index: number): Array<T> {
      var newArray = array.slice();
      newArray[index] = newElement;
      return newArray;
    },

    // Create a copy of an array before removing an item
    arrayRemoveElementAtIndex: function<T>(array: ReadonlyArray<T>, index: number): Array<T> {
      var newArray = array.slice();
      newArray.splice(index, 1);
      return newArray;
    },

    arrayMoveElement: function<T>(array: ReadonlyArray<T>, index: number, newIndex: number): Array<T> {
      const newArray = array.slice();
      const item = newArray.splice(index, 1);
      if (item && item[0]) {
        newArray.splice(newIndex, 0, item[0]);
      }
      return newArray;
    }
};

export default ImmutableObjectUtils;
