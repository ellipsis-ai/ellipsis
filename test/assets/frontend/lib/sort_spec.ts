import Sort from '../../../../app/assets/frontend/lib/sort';

describe('Sort', () => {
  describe('arrayAlphaBy', () => {
    it('copies the vars and sorts them by name regardless of case', () => {
      var array = [{
        name: "Delta",
        value: "",
        isAlreadySavedWithValue: false
      }, {
        name: "bravo",
        value: "",
        isAlreadySavedWithValue: false
      }, {
        name: "charlie",
        value: "",
        isAlreadySavedWithValue: false
      }, {
        name: "Alpha",
        value: "",
        isAlreadySavedWithValue: false
      }];
      var sorted = Sort.arrayAlphabeticalBy(array, (item) => item.name);
      expect(sorted.map(ea => ea.name)).toEqual(["Alpha", "bravo", "charlie", "Delta"]);
      expect(array.map(ea => ea.name)).toEqual(["Delta", "bravo", "charlie", "Alpha"]);
    });
  });

  describe('arrayAscending', () => {
    it('copies the vars and sorts them numerically by property in ascending order', () => {
      const array = [{ num: 0 }, { num: 5 }, { num: -5 }];
      const sorted = Sort.arrayAscending(array, (ea) => ea.num);
      expect(sorted.map((ea) => ea.num)).toEqual([-5, 0, 5]);
      expect(array.map((ea) => ea.num)).toEqual([0, 5, -5]);
    });
  });

  describe('arrayDescending', () => {
    it('copies the vars and sorts them numerically by property in descending order', () => {
      const array = [{ num: 0 }, { num: 5 }, { num: -5 }];
      const sorted = Sort.arrayDescending(array, (ea) => ea.num);
      expect(sorted.map((ea) => ea.num)).toEqual([5, 0, -5]);
      expect(array.map((ea) => ea.num)).toEqual([0, 5, -5]);
    });
  });

});
