import * as uuid from 'uuid';

const ID = {
    newRandomUUIDArray: function(): Uint8Array {
      const bytes = new Uint8Array(16);
      uuid.v4({}, bytes, 0);
      return bytes;
    },

    toBase64URLEncoded(uint8array: Uint8Array): string {
      const str = String.fromCharCode.apply(this, uint8array);
      return window.btoa(str)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
    },

    next(): string {
      return ID.toBase64URLEncoded(ID.newRandomUUIDArray());
    }
};

export default ID;
