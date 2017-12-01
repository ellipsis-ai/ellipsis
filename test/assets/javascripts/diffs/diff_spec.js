window.crypto = require('./../../../mocks/mock_window_crypto');
const BehaviorGroup = require('../../../../app/assets/javascripts/models/behavior_group');

const teamId = 'team123456';
const groupId = 'group123456';
const behaviorId = 'ghijkl';
const libraryId = 'lib123456';
const inputId = 'input123456';

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
  "inputIds": [inputId],
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
  "inputIds": [inputId],
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

const actionInput1 = Object.freeze({
  name: 'clown',
  question: 'what drives the car?',
  paramType: {
    id: 'Text',
    name: 'Text',
    needsConfig: false
  },
  isSavedForTeam: false,
  isSavedForUser: true,
  inputId: inputId,
});

const actionInput2 = Object.freeze({
  name: 'clown',
  question: 'who drives the car?',
  paramType: {
    id: 'sdflkjafks',
    name: 'Person',
    needsConfig: false
  },
  isSavedForTeam: true,
  isSavedForUser: false,
  inputId: inputId
});

const behaviorGroupVersion1 = Object.freeze({
  groupId: 'group123456',
  behaviorVersions: [behaviorVersion1],
  requiredAWSConfigs: [],
  requiredOAuth2ApiConfigs: [],
  requiredSimpleTokenApis: [],
  actionInputs: [actionInput1],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion1]
});

const behaviorGroupVersion2 = Object.freeze({
  groupId: 'group123456',
  behaviorVersions: [behaviorVersion2],
  requiredAWSConfigs: [],
  requiredOAuth2ApiConfigs: [],
  requiredSimpleTokenApis: [],
  actionInputs: [actionInput2],
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

      // the empty objects for original and modified objects are ignored in the match
      const expectedDiffTree = {
        "children": [
          {
            "children": [
              {
                "label": "Name",
                "modified": "Second name",
                "original": "First name",
                "parts": [
                  {
                    "kind": "removed",
                    "value": "First"
                  },
                  {
                    "kind": "added",
                    "value": "Second"
                  },
                  {
                    "kind": "unchanged",
                    "value": " name"
                  }
                ]
              },
              {
                "label": "Description",
                "modified": "A description",
                "original": "",
                "parts": [
                  {
                    "kind": "added",
                    "value": "A description"
                  }
                ]
              },
              {
                "label": "Response template",
                "modified": "Another template",
                "original": "A template",
                "parts": [
                  {
                    "kind": "unchanged",
                    "value": "A"
                  },
                  {
                    "kind": "added",
                    "value": "nother"
                  },
                  {
                    "kind": "unchanged",
                    "value": " template"
                  }
                ]
              },
              {
                "label": "Code",
                "modified": "use strict; // so strict",
                "original": "use strict;",
                "parts": [
                  {
                    "kind": "unchanged",
                    "value": "use strict;"
                  },
                  {
                    "kind": "added",
                    "value": " // so strict"
                  }
                ]
              },
              {
                "label": "Always responds privately",
                "modified": true,
                "original": false
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
                "children": [
                  {
                    "label": "Require bot mention",
                    "modified": true,
                    "original": false
                  }
                ],
                "modified": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": true,
                  "text": "B"
                },
                "original": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": false,
                  "text": "B"
                }
              },
              {
                "children": [
                  {
                    "label": "Question",
                    "modified": "who drives the car?",
                    "original": "what drives the car?",
                    "parts": [
                      {
                        "kind": "unchanged",
                        "value": "wh"
                      },
                      {
                        "kind": "removed",
                        "value": "at"
                      },
                      {
                        "kind": "added",
                        "value": "o"
                      },
                      {
                        "kind": "unchanged",
                        "value": " drives the car?"
                      }
                    ]
                  },
                  {
                    "label": "Type",
                    "modified": "Person",
                    "original": "Text"
                  },
                  {
                    "label": "Saved for whole team",
                    "modified": true,
                    "original": false
                  },
                  {
                    "label": "Saved per user",
                    "modified": false,
                    "original": true
                  }
                ]
              }
            ]
          },
          {
            "children": [
              {
                "label": "Name",
                "modified": "some-lib-revised",
                "original": "some-lib",
                "parts": [
                  {
                    "kind": "unchanged",
                    "value": "some-lib"
                  },
                  {
                    "kind": "added",
                    "value": "-revised"
                  }
                ]
              },
              {
                "label": "Description",
                "modified": "A library (revised)",
                "original": "A library",
                "parts": [
                  {
                    "kind": "unchanged",
                    "value": "A library"
                  },
                  {
                    "kind": "added",
                    "value": " (revised)"
                  }
                ]
              },
              {
                "label": "Code",
                "modified": "return \"foo\";",
                "original": "return \"foo\"",
                "parts": [
                  {
                    "kind": "unchanged",
                    "value": "return \"foo\""
                  },
                  {
                    "kind": "added",
                    "value": ";"
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
