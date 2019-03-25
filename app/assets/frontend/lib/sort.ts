const Sort = {
    arrayAlphabeticalBy: function<T>(array: Array<T>, mapItemToProperty: (item: T) => Option<string>) {
      var copy = array.slice();
      return copy.sort((a, b) => {
        const aLower = (mapItemToProperty(a) || "").toLowerCase();
        const bLower = (mapItemToProperty(b) || "").toLowerCase();
        if (aLower < bLower) {
          return -1;
        } else if (aLower > bLower) {
          return 1;
        } else {
          return 0;
        }
      });
    },

    arrayAscending: function<T>(array: Array<T>, mapItemToProperty: (item: T) => number) {
      const copy = array.slice();
      return copy.sort((a, b) => {
        return mapItemToProperty(a) - mapItemToProperty(b);
      });
    },

    arrayDescending: function<T>(array: Array<T>, mapItemToProperty: (item: T) => number) {
      return this.arrayAscending(array, mapItemToProperty).reverse();
    }
};

export default Sort;
