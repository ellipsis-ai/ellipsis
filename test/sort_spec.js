jest.unmock('../app/assets/javascripts/lib/sort');
const Sort = require('../app/assets/javascripts/lib/sort');

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
});
