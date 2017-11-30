window.crypto = require('./../../../mocks/mock_window_crypto');
const BehaviorGroup = require('../../../../app/assets/javascripts/models/behavior_group');

const teamId = 'team123456';
const groupId = 'group123456';
const behaviorId = 'ghijkl';
const libraryId = 'lib123456';

const behaviorVersion1 = Object.freeze({
  "id": "abcdef",
  "name": "First name",
  "groupId": groupId,
  "behaviorId": behaviorId,
  "functionBody": "use strict;",
  "responseTemplate": "A template",
  "params": [],
  "triggers": [{
    "text": "B",
    "requiresMention": false,
    "isRegex": true,
    "caseSensitive": false
  }, {
    "text": "C",
    "requiresMention": false,
    "isRegex": false,
    "caseSensitive": false
  }],
  "config": {
    "forcePrivateResponse": false,
    "aws": {
      "accessKeyName": "AWS_ACCESS_KEY",
      "secretKeyName": "AWS_SECRET_KEY",
      "regionName": "AWS_REGION"
    }
  },
  "createdAt": 1468338136532
});
const behaviorVersion2 = Object.freeze({
  "id": "abcdef",
  "name": "Second name",
  "description": "A description",
  "groupId": groupId,
  "behaviorId": behaviorId,
  "functionBody": "use strict; // so strict",
  "responseTemplate": "Another template",
  "params": [],
  "triggers": [{
    "text": "B",
    "requiresMention": true,
    "isRegex": true,
    "caseSensitive": false
  }, {
    "text": ".+",
    "requiresMention": false,
    "isRegex": true,
    "caseSensitive": false
  }],
  "config": {
    "forcePrivateResponse": true,
    "aws": {
      "accessKeyName": "AWS_ACCESS_KEY",
      "secretKeyName": "AWS_SECRET_KEY",
      "regionName": "AWS_REGION"
    }
  },
  "createdAt": 1468359271138
});

const libraryVersion1 = Object.freeze({
  id: 'abcdef',
  name: 'some-lib',
  description: 'A library',
  functionBody: 'return "foo"',
  groupId: groupId,
  teamId: teamId,
  libaryId: libraryId,
  editorScrollPosition: 0
});

const libraryVersion2 = Object.freeze({
  id: 'abcdef',
  name: 'some-lib-revised',
  description: 'A library (revised)',
  functionBody: 'return "foo";',
  groupId: groupId,
  teamId: teamId,
  libaryId: libraryId,
  editorScrollPosition: 10
});

const defaultStorageDataType = Object.freeze({
  id: "abcdef",
  behaviorId: "jfgh",
  name: "myDataType",
  config: {
    dataTypeConfig: {
      fields: [{
        fieldId: "1",
        fieldVersionId: "2",
        name: "field1",
        fieldType: {
          name: "Text",
          id: "Text"
        },
        isLabel: true
      }, {
        fieldId: "3",
        fieldVersionId: "4",
        name: "field2",
        fieldType: {
          name: "Text",
          id: "Text"
        },
        isLabel: false
      }],
      usesCode: false
    }
  }
});

const behaviorGroupVersion1 = Object.freeze({
  groupId: 'group123456',
  behaviorVersions: [behaviorVersion1],
  requiredAWSConfigs: [],
  requiredOAuth2ApiConfigs: [],
  requiredSimpleTokenApis: [],
  actionInputs: [],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion1]
});

const behaviorGroupVersion2 = Object.freeze({
  groupId: 'group123456',
  behaviorVersions: [behaviorVersion2],
  requiredAWSConfigs: [],
  requiredOAuth2ApiConfigs: [],
  requiredSimpleTokenApis: [],
  actionInputs: [],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion2]
});

describe('BehaviorGroupVersion', () => {

  describe('maybeDiffFor', () => {

    it('builds the correct diff', () => {
      const version1 = BehaviorGroup.fromJson(behaviorGroupVersion1);
      const version2 = BehaviorGroup.fromJson(behaviorGroupVersion2);
      const maybeDiff = version1.maybeDiffFor(version2);
      expect(maybeDiff).toBeTruthy();
      const diffText = maybeDiff.displayText();

      console.log(JSON.stringify(maybeDiff, null, 2));

      const expectedDiffTree = {
        // "original": {},
        // "modified": {},
        "children": [
          {
            "original": {}, // ignore
            "modified": {}, // ignore
            "children": [
              {
                "label": "Name",
                "original": "First name",
                "modified": "Second name",
                "parts": [
                  {
                    "value": "First",
                    "kind": "removed"
                  },
                  {
                    "value": "Second",
                    "kind": "added"
                  },
                  {
                    "value": " name",
                    "kind": "unchanged"
                  }
                ]
              },
              {
                "label": "Description",
                "original": "",
                "modified": "A description",
                "parts": [
                  {
                    "value": "A description",
                    "kind": "added"
                  }
                ]
              },
              {
                "label": "Response template",
                "original": "A template",
                "modified": "Another template",
                "parts": [
                  {
                    "value": "A",
                    "kind": "unchanged"
                  },
                  {
                    "value": "nother",
                    "kind": "added"
                  },
                  {
                    "value": " template",
                    "kind": "unchanged"
                  }
                ]
              },
              {
                "label": "Code",
                "original": "use strict;",
                "modified": "use strict; // so strict",
                "parts": [
                  {
                    "value": "use strict;",
                    "kind": "unchanged"
                  },
                  {
                    "value": " // so strict",
                    "kind": "added"
                  }
                ]
              },
              {
                "label": "Always responds privately",
                "original": false,
                "modified": true
              },
              {
                "item": {
                  "caseSensitive": false,
                  "isRegex": false,
                  "requiresMention": false,
                  "text": "C"
                }
              },
              {
                "item": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": false,
                  "text": ".+"
                }
              },
              {
                "original": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": false,
                  "text": "B"
                },
                "modified": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": true,
                  "text": "B"
                },
                "children": [
                  {
                    "label": "Require bot mention",
                    "original": false,
                    "modified": true
                  }
                ]
              }
            ]
          },
          {
            "original": {}, // ignore
            "modified": {}, // ignore
            "children": [
              {
                "label": "Name",
                "original": "some-lib",
                "modified": "some-lib-revised",
                "parts": [
                  {
                    "value": "some-lib",
                    "kind": "unchanged"
                  },
                  {
                    "value": "-revised",
                    "kind": "added"
                  }
                ]
              },
              {
                "label": "Description",
                "original": "A library",
                "modified": "A library (revised)",
                "parts": [
                  {
                    "value": "A library",
                    "kind": "unchanged"
                  },
                  {
                    "value": " (revised)",
                    "kind": "added"
                  }
                ]
              },
              {
                "label": "Code",
                "original": "return \"foo\"",
                "modified": "return \"foo\";",
                "parts": [
                  {
                    "value": "return \"foo\"",
                    "kind": "unchanged"
                  },
                  {
                    "value": ";",
                    "kind": "added"
                  }
                ]
              }
            ]
          }
        ]
      };

      expect(maybeDiff).toMatchObject(expectedDiffTree);

      expect(diffText).toContain("Modified action:");
      expect(diffText).toContain("Name: [-First][+Second] name");
      expect(diffText).toContain("Description: [+A description]");
      expect(diffText).toContain("Response template: A[+nother] template");
      expect(diffText).toContain("Code: use strict;[+ // so strict]");
      expect(diffText).toContain("Added trigger \"C\"");
      expect(diffText).toContain("Removed trigger \".+\"");
      expect(diffText).toContain("Modified trigger \"B\":\nRequire bot mention: changed to true");
      expect(diffText).toContain("Modified library \"some-lib\":");
      expect(diffText).toContain("Name: some-lib[+-revised]");
      expect(diffText).toContain("Description: A library[+ (revised)]");
      expect(diffText).toContain("Code: return \"foo\"[+;]");

    });

  });

});
