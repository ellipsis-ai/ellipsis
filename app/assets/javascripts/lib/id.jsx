define(function(require) {
  const uuid = require('node-uuid');

  class ID {
    static next() {
      const bytes = new Uint8Array(16);
      uuid.v4({}, bytes, 0);
      const str = String.fromCharCode.apply(this, bytes);
      return window.btoa(str)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .substring(0, 22);
    }
  }

  return ID;
});
