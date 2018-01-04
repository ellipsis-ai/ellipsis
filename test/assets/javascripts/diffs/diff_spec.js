window.crypto = require('./../../../mocks/mock_window_crypto');
const BehaviorGroup = require('../../../../app/assets/javascripts/models/behavior_group');
const diffs = require('../../../../app/assets/javascripts/models/diffs');

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
  name: "Some skill",
  icon: "🚀",
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
  name: "Some updated skill",
  description: "With a description",
  groupId: 'group123456',
  behaviorVersions: [behaviorVersion2],
  requiredAWSConfigs: [],
  requiredOAuth2ApiConfigs: [],
  requiredSimpleTokenApis: [],
  actionInputs: [actionInput2],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion2]
});

function textDiff(left, right) {
  return diffs.MultiLineTextPropertyDiff.maybeFor("", left, right);
}

describe('diffs', () => {

  describe('maybeDiffFor', () => {

    it('builds the correct diff for a behavior group', () => {
      const version1 = BehaviorGroup.fromJson(behaviorGroupVersion1);
      const version2 = BehaviorGroup.fromJson(behaviorGroupVersion2);
      const maybeDiff = diffs.maybeDiffFor(version1, version2);
      expect(maybeDiff).toBeTruthy();
      const diffText = maybeDiff.displayText();

      // the empty objects for original and modified objects are ignored in the match
      const expectedDiffTree = {
        "children": [
          {
            "isCode": false,
            "label": "Skill name",
            "modified": "Some updated skill",
            "original": "Some skill",
            "unifiedLines": [[
              {
                "kind": "unchanged",
                "value": "Some "
              },
              {
                "kind": "added",
                "value": "updated "
              },
              {
                "kind": "unchanged",
                "value": "skill"
              }
            ]]
          },
          {
            "isCode": false,
            "label": "Skill description",
            "modified": "With a description",
            "original": "",
            "unifiedLines": [[
              {
                "kind": "added",
                "value": "With a description"
              },
            ]],
          },
          {
            "isCode": false,
            "label": "Skill icon",
            "modified": "",
            "original": "🚀",
            "unifiedLines": [[
              {
                "kind": "removed",
                 "value": "🚀"
               }
            ]]
          },
          {
            "children": [
              {
                "isCode": false,
                "label": "Name",
                "modified": "Second name",
                "original": "First name",
                "unifiedLines": [[
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
                ]]
              },
              {
                "isCode": false,
                "label": "Description",
                "modified": "A description",
                "original": "",
                "unifiedLines": [[
                  {
                    "kind": "added",
                    "value": "A description"
                  }
                ]]
              },
              {
                "isCode": true,
                "label": "Response template",
                "modified": "Another template",
                "original": "A template",
                "unifiedLines": [[
                  {
                    "kind": "removed",
                    "value": "A"
                  },
                  {
                    "kind": "added",
                    "value": "Another"
                  },
                  {
                    "kind": "unchanged",
                    "value": " template"
                  }
                ]]
              },
              {
                "isCode": true,
                "label": "Code",
                "modified": "use strict; // so strict",
                "original": "use strict;",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "use strict;"
                  },
                  {
                    "kind": "added",
                    "value": " // so strict"
                  }
                ]]
              },
              {
                "label": "Always responds privately",
                "modified": true,
                "original": false
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
                "item": {
                  "caseSensitive": false,
                  "isRegex": false,
                  "requiresMention": false,
                  "text": "C"
                }
              },
              {
                "children": [
                  {
                    "label": "Require user to mention Ellipsis",
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
                    "isCode": false,
                    "label": "Question",
                    "modified": "who drives the car?",
                    "original": "what drives the car?",
                    "unifiedLines": [[
                      {
                        "kind": "removed",
                        "value": "what"
                      },
                      {
                        "kind": "added",
                        "value": "who"
                      },
                      {
                        "kind": "unchanged",
                        "value": " drives the car?"
                      }
                    ]]
                  },
                  {
                    "label": "Data type",
                    "modified": "Person",
                    "original": "Text"
                  },
                  {
                    "label": "Save and re-use answer for the team",
                    "modified": true,
                    "original": false
                  },
                  {
                    "label": "Save and re-use answer for each user",
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
                "isCode": false,
                "label": "Name",
                "modified": "some-lib-revised",
                "original": "some-lib",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "some-lib"
                  },
                  {
                    "kind": "added",
                    "value": "-revised"
                  }
                ]]
              },
              {
                "isCode": false,
                "label": "Description",
                "modified": "A library (revised)",
                "original": "A library",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "A library"
                  },
                  {
                    "kind": "added",
                    "value": " (revised)"
                  }
                ]]
              },
              {
                "isCode": true,
                "label": "Code",
                "modified": "return \"foo\";",
                "original": "return \"foo\"",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "return \"foo"
                  },
                  {
                    "kind": "removed",
                    "value": "\""
                  },
                  {
                    "kind": "added",
                    "value": "\";"
                  }
                ]]
              }
            ]
          }
        ]
      };

      expect(maybeDiff).toMatchObject(expectedDiffTree);

      expect(diffText).toContain("Modified action “First name”");
      expect(diffText).toContain("Name: [-First][+Second] name");
      expect(diffText).toContain("Description: [+A description]");
      expect(diffText).toContain("Response template: [-A][+Another] template");
      expect(diffText).toContain("Code: use strict;[+ // so strict]");
      expect(diffText).toContain("Removed trigger “C”");
      expect(diffText).toContain("Added trigger “.+”");
      expect(diffText).toContain("Modified trigger “B”:\nRequire user to mention Ellipsis: changed to on");
      expect(diffText).toContain("Modified library “some-lib”");
      expect(diffText).toContain("Name: some-lib[+-revised]");
      expect(diffText).toContain("Description: A library[+ (revised)]");
      expect(diffText).toContain("Code: return \"foo[-\"][+\";]");

    });

  });

  describe('MultiLineTextPropertyDiff', () => {
    it('handles a single line', () => {
      const left = `cat`;
      const right = `dog`;
      const result = textDiff(left, right);
      expect(result.oldLines).toEqual([[{ kind: "removed", value: "cat" }]]);
      expect(result.newLines).toEqual([[{ kind: "added", value: "dog" }]]);
      expect(result.unifiedLines).toEqual([[{ kind: "removed", value: "cat" }, { kind: "added", value: "dog" }]]);
    });

    it('handles removing a line', () => {
      const left = `cat
dog
bear
`;
      const right = `cat
bear
`;
      const result = textDiff(left, right);
      expect(result.oldLines).toEqual([
        [{ kind: "unchanged", value: "cat\n" }],
        [{ kind: "removed", value: "dog\n" }],
        [{ kind: "unchanged", value: "bear\n" }],
        []
      ]);
      expect(result.newLines).toEqual([
        [{ kind: "unchanged", value: "cat\n" }],
        [],
        [{ kind: "unchanged", value: "bear\n" }],
        []
      ]);
      expect(result.unifiedLines).toEqual([
        [{ kind: "unchanged", value: "cat\n" }],
        [{ kind: "removed", value: "dog\n" }],
        [{ kind: "unchanged", value: "bear\n" }],
        []
      ]);
    });

    it('handles removing new lines', () => {
      const left = `cat


dog`;
      const right = `cat
dog`;

      const result = textDiff(left, right);
      expect(result.oldLines).toEqual([
        [{ kind: "unchanged", value: "cat" }, { kind: "removed", value: "\n" }],
        [{ kind: "removed", value: "\n" }],
        [{ kind: "removed", value: "\n" }],
        [{ kind: "unchanged", value: "dog" }]
      ]);
      expect(result.newLines).toEqual([
        [{ kind: "unchanged", value: "cat" }, { kind: "added", value: "\n" }],
        [],
        [],
        [{ kind: "unchanged", value: "dog" }]
      ]);
      expect(result.unifiedLines).toEqual([
        [{ kind: "unchanged", value: "cat" }, { kind: "removed", value: "\n" }],
        [{ kind: "removed", value: "\n" }],
        [{ kind: "removed", value: "\n" }],
        [{ kind: "added", value: "\n" }],
        [{ kind: "unchanged", value: "dog" }]
      ]);
    });
  });

});
