define(function() {
  class UniqueBy {

    static forArray(array, byProperty) {
      var seen = {}, output = [], l = array.length, i;
      for( i=0; i<l; i++) {
        var item = array[i];
        var uniqueValue = item[byProperty];
        if (seen[uniqueValue]) continue;
        seen[uniqueValue] = true;
        output.push(item);
      }
      return output;
    }

  }

  return UniqueBy;
});
