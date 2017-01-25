jest.unmock('../app/assets/javascripts/lib/immutable_object_utils');

var ImmutableObjectUtils = require('../app/assets/javascripts/lib/immutable_object_utils');

describe('ImmutableObjectUtils', () => {
  const array = ['a', 'b', 'c'];

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
