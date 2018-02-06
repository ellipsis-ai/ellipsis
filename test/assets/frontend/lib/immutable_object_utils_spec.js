var ImmutableObjectUtils = require('../../../../app/assets/frontend/lib/immutable_object_utils');

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

  describe('arrayMoveElement', () => {
    it("copies an array before changing an element's position", () => {
      const longArray = ['a', 'b', 'c', 'd', 'e', 'f'];
      const newArray = ImmutableObjectUtils.arrayMoveElement(longArray, 2, 4);
      expect(longArray).toEqual(['a', 'b', 'c', 'd', 'e', 'f']);
      expect(newArray).toEqual(['a', 'b', 'd', 'e', 'c', 'f']);
    });
  });
});
