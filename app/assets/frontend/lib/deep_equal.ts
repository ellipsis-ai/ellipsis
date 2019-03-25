class DeepEqual {
    static isEqual(thing1: any, thing2: any): boolean {
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

    static arraysEqual(array1: Array<any>, array2: Array<any>): boolean {
      if (array1.length !== array2.length) {
        return false;
      } else {
        return array1.every(function(item, index) {
          return DeepEqual.isEqual(array1[index], array2[index]);
        });
      }
    }

    static objectsEqual(obj1: { [k: string]: any } | null, obj2: { [k: string]: any } | null): boolean {
      // typeof null returns "object", so we need to guard against one side being null
      if (!obj1 || !obj2) {
        return obj1 === obj2;
      } else {
        if (obj1.constructor !== obj2.constructor) {
          return false;
        }
        const obj1Keys = Object.keys(obj1);
        const obj2Keys = Object.keys(obj2);
        if (!DeepEqual.arraysEqual(obj1Keys.sort(), obj2Keys.sort())) {
          return false;
        }
        return obj1Keys.every(function (key) {
          return DeepEqual.isEqual(obj1[key], obj2[key]);
        });
      }
    }

}

export default DeepEqual;
