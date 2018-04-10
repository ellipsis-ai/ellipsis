const SequentialName = {
    nextFor: function<T>(list: Array<T>, mapper: (item: T) => string, prefix: string) {
      let index = Math.max(list.length + 1, 1);
      while (list.map(mapper).some((ea) => ea === `${prefix}${index}`)) {
        index++;
      }
      return `${prefix}${index}`;
    }
};

export default SequentialName;
