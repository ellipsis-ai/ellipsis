define(function() {

  class SequentialName {
    static nextFor(list, mapper, prefix) {
      let index = Math.max(list.length, 1);
      while (list.map(mapper).some((ea) => ea === `${prefix}${index}`)) {
        index++;
      }
      return `${prefix}${index}`;
    }
  }

  return SequentialName;
});
