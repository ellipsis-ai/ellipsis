import LibraryVersion, {LibraryVersionJson} from '../../../../app/assets/frontend/models/library_version';

const libraryVersion: LibraryVersionJson = {
  "id": "abcdef",
  "libraryId": "ghijkl",
  "functionBody": "use strict;",
  "createdAt": 1468338136532
};

describe('LibraryVersion', () => {

  describe('timestampForAlphabeticalSort', () => {
    describe('returns a zero-padded numeric timestamp string with 15 characters', () => {
      it('handles a numeric timestamp', () => {
        const version = LibraryVersion.fromProps(libraryVersion).clone({ createdAt: 1234567890123 });
        expect(version.timestampForAlphabeticalSort()).toBe("001234567890123");
      });
      it('handles an ISO timestamp', () => {
        const version = LibraryVersion.fromProps(libraryVersion).clone({ createdAt: "2017-03-14T18:55:28.710Z" });
        expect(version.timestampForAlphabeticalSort()).toBe("001489517728710");
      });
      it('handles an ISO timestamp with a TZ offset', () => {
        const version = LibraryVersion.fromProps(libraryVersion).clone({ createdAt: "2017-03-14T13:55:28.710-05:00" });
        expect(version.timestampForAlphabeticalSort()).toBe("001489517728710");
      });
    });
  });

  describe('sortKey', () => {
    it('sorts non-new by name, first trigger, then by timestamp, with a leading A', () => {
      const version1 = LibraryVersion.fromProps(libraryVersion).clone({ name: "Name" });
      const version2 = LibraryVersion.fromProps(libraryVersion);
      expect(version1.sortKey()).toBe("AName");
      expect(version2.sortKey()).toEqual("A" + version2.timestampForAlphabeticalSort());
    });

    it('sorts new by timestamp only, with a leading Z', () => {
      const version1 = LibraryVersion.fromProps(libraryVersion).clone({ name: "Name", isNew: true });
      expect(version1.sortKey()).toEqual("Z" + version1.timestampForAlphabeticalSort());
    });
  });

});
