jest.unmock('../app/assets/javascripts/immutable_object_utils');

var ImmutableObjectUtils = require('../app/assets/javascripts/immutable_object_utils');

describe('ImmutableObjectUtils', () => {
  const array = ['a', 'b', 'c'];
  const obj = { a: 0, b: { c: 1 } };

  describe('arrayWithNewElementAtIndex', () => {
    it('copies an array before modifying it', () => {
      const newArray = ImmutableObjectUtils.arrayWithNewElementAtIndex(array, 'z', 2);
      expect(array).toEqual(['a', 'b', 'c']);
      expect(newArray).toEqual(['a', 'b', 'z']);
    });
  });

  describe('arrayRemoveElementAtIndex', () => {
    it('copies an array before removing an element from it', () => {
      const newArray = ImmutableObjectUtils.arrayRemoveElementAtIndex(array, 2);
      expect(array).toEqual(['a', 'b', 'c']);
      expect(newArray).toEqual(['a', 'b']);
    });
  });
});
