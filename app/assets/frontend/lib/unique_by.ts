type WithStrings = { [propName: string]: string }

const UniqueBy = {
  forArray: function (array: Array<WithStrings>, byProperty: string) {
    const seen: { [propName: string]: boolean } = {};
    const output: Array<WithStrings> = [];
    array.forEach((item) => {
      const uniqueValue = item[byProperty];
      if (!seen[uniqueValue]) {
        seen[uniqueValue] = true;
        output.push(item);
      }
    });
    return output;
  }
};

export default UniqueBy;

