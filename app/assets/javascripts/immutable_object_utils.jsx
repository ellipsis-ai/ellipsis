define(function() {
  return {
    // Create a copy of an array before modifying it
    arrayWithNewElementAtIndex: function(array, newElement, index) {
      var newArray = array.slice();
      newArray[index] = newElement;
      return newArray;
    },

    // Create a copy of an array before removing an item
    arrayRemoveElementAtIndex: function(array, index) {
      var newArray = array.slice();
      newArray.splice(index, 1);
      return newArray;
    }
  };
});
