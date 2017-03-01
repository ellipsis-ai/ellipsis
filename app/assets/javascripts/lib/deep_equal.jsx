define(function() {
  class DeepEqual {
    static isEqual(thing1, thing2) {
      if (thing1 === thing2) {
        return true;
      } else if (Number.isNaN(thing1) && Number.isNaN(thing2)) {
        return true;
      } else if (Array.isArray(thing1) && Array.isArray(thing2)) {
        return DeepEqual.arraysEqual(thing1, thing2);
      } else if (typeof(thing1) === 'object' && typeof(thing2) === 'object') {
        return DeepEqual.objectsEqual(thing1, thing2);
      } else {
        return false;
      }
    }

    static arraysEqual(array1, array2) {
      if (array1.length !== array2.length) {
        return false;
      } else {
        return array1.every(function(item, index) {
          return DeepEqual.isEqual(array1[index], array2[index]);
        });
      }
    }

    static objectsEqual(obj1, obj2) {
      if (obj1.constructor !== obj2.constructor) {
        return false;
      }
      var obj1Keys = Object.keys(obj1);
      var obj2Keys = Object.keys(obj2);
      if (!DeepEqual.arraysEqual(obj1Keys.sort(), obj2Keys.sort())) {
        return false;
      }
      return obj1Keys.every(function(key) {
        return DeepEqual.isEqual(obj1[key], obj2[key]);
      });
    }

  }

  return DeepEqual;
});
