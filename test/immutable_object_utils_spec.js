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

  describe('objectWithNewValueAtKey', () => {
    it('copies an object before modifying a property of it', () => {
      const newObj = ImmutableObjectUtils.objectWithNewValueAtKey(obj, 1, 'a');
      expect(obj).toEqual({ a: 0, b: { c: 1 } });
      expect(newObj).toEqual({ a: 1, b: { c: 1 } });
    });
  });
});
