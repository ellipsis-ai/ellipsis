const Sort = {
    arrayAlphabeticalBy: function(array: Array<any>, mapItemToProperty: (item: any) => Option<string>) {
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
    }
};

export default Sort;
