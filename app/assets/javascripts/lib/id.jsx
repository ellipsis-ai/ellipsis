define(function(require) {
  const uuid = require('node-uuid');

  class ID {
    static newRandomUUIDArray() {
      const bytes = new Uint8Array(16);
      uuid.v4({}, bytes, 0);
      return bytes;
    }

    static toBase64URLEncoded(uint8array) {
      const str = String.fromCharCode.apply(this, uint8array);
      return window.btoa(str)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
    }

    static next() {
      return ID.toBase64URLEncoded(ID.newRandomUUIDArray());
    }
  }

  return ID;
});
