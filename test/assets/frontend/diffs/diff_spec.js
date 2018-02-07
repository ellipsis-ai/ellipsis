import BehaviorGroup from '../../../../app/assets/frontend/models/behavior_group';
import {TextPart, MultiLineTextPropertyDiff, maybeDiffFor} from '../../../../app/assets/frontend/models/diffs';

const teamId = 'team123456';
const groupId = 'group123456';
const behaviorId = 'ghijkl';
const libraryId = 'lib123456';
const inputId = 'input123456';
const inputId2 = 'input234567';
const requiredAWSConfigId = 'requiredAWS123456';
const requiredGithubConfigId = 'requiredGithub123456';
const requiredPivotalTrackerConfigId = 'requiredPivotalTracker123456';

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
  "inputIds": [inputId, inputId2],
  "config": {
    "forcePrivateResponse": false
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
  "inputIds": [inputId2, inputId],
  "config": {
    "forcePrivateResponse": true
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
  exportId: inputId
});

const actionInputChanged = Object.freeze({
  name: 'clown',
  question: 'who drives the car?',
  paramType: {
    id: 'sdflkjafks',
    name: 'Person',
    needsConfig: false
  },
  isSavedForTeam: true,
  isSavedForUser: false,
  inputId: inputId,
  exportId: inputId
});

const actionInput2 = Object.freeze({
  name: 'somethingElse',
  question: 'and now for something?',
  paramType: {
    id: 'Text',
    name: 'Text',
    needsConfig: false
  },
  isSavedForTeam: false,
  isSavedForUser: false,
  inputId: inputId2,
  exportId: inputId2
});

const requiredAWSConfig1 = Object.freeze({
  id: 'aws123',
  exportId: requiredAWSConfigId,
  apiId: 'aws',
  nameInCode: 'prod',
  config: undefined
});

const requiredAWSConfig2 = Object.freeze({
  id: 'aws123',
  exportId: requiredAWSConfigId,
  apiId: 'aws',
  nameInCode: 'prod',
  config: {
    id: 'aws-prod',
    displayName: 'AWS Prod'
  }
});

const requiredOAuth2Config1 = Object.freeze({
  id: 'github123',
  exportId: requiredGithubConfigId,
  apiId: 'github',
  nameInCode: 'github',
  config: undefined,
  recommendedScope: 'repo'
});

const requiredOAuth2Config2 = Object.freeze({
  id: 'github123',
  exportId: requiredGithubConfigId,
  apiId: 'github',
  nameInCode: 'githubReadonly',
  recommendedScope: 'repo:readonly'
});

const requiredOAuth2Config3 = Object.freeze({
  id: 'github12345',
  exportId: 'requiredGithubabcdef',
  apiId: 'github',
  nameInCode: 'githubReadwrite',
  recommendedScope: 'repo'
});

const requiredSimpleTokenApi1 = Object.freeze({
  id: 'pivotalTracker123',
  exportId: requiredPivotalTrackerConfigId,
  apiId: 'pivotalTracker',
  nameInCode: 'pivotalTracker',
});

const requiredSimpleTokenApi2 = Object.freeze({
  id: 'pivotalTracker123',
  exportId: requiredPivotalTrackerConfigId,
  apiId: 'pivotalTracker',
  nameInCode: 'pivotalTracker2',
});

const behaviorGroupVersion1 = Object.freeze({
  name: "Some skill",
  icon: "🚀",
  groupId: 'group123456',
  behaviorVersions: [behaviorVersion1],
  requiredAWSConfigs: [requiredAWSConfig1],
  requiredOAuth2ApiConfigs: [requiredOAuth2Config1, requiredOAuth2Config3],
  requiredSimpleTokenApis: [requiredSimpleTokenApi1],
  actionInputs: [actionInput1, actionInput2],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion1]
});

const behaviorGroupVersion2 = Object.freeze({
  name: "Some updated skill",
  description: "With a description",
  groupId: 'group123456',
  behaviorVersions: [behaviorVersion2],
  requiredAWSConfigs: [requiredAWSConfig2],
  requiredOAuth2ApiConfigs: [requiredOAuth2Config2, requiredOAuth2Config3],
  requiredSimpleTokenApis: [requiredSimpleTokenApi2],
  actionInputs: [actionInputChanged, actionInput2],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion2]
});

function textDiff(left, right) {
  return MultiLineTextPropertyDiff.maybeFor("", left, right);
}

describe('diffs', () => {

  describe('maybeDiffFor', () => {

    it('builds the correct diff for a behavior group', () => {
      const version1 = BehaviorGroup.fromJson(behaviorGroupVersion1);
      const version2 = BehaviorGroup.fromJson(behaviorGroupVersion2);
      const maybeDiff = maybeDiffFor(version1, version2);
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
              },
              {
                "afterItems": [
                  {
                    name: 'somethingElse',
                    question: 'and now for something?',
                    paramType: {
                      id: 'Text',
                      name: 'Text',
                      needsConfig: false
                    },
                    isSavedForTeam: false,
                    isSavedForUser: false,
                    inputId: inputId2,
                    exportId: inputId2
                  },
                  {
                    name: 'clown',
                    question: 'who drives the car?',
                    paramType: {
                      id: 'sdflkjafks',
                      name: 'Person',
                      needsConfig: false
                    },
                    isSavedForTeam: true,
                    isSavedForUser: false,
                    inputId: inputId,
                    exportId: inputId
                  }
                ],
                "beforeItems": [
                  {
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
                    exportId: inputId
                  },
                  {
                    name: 'somethingElse',
                    question: 'and now for something?',
                    paramType: {
                      id: 'Text',
                      name: 'Text',
                      needsConfig: false
                    },
                    isSavedForTeam: false,
                    isSavedForUser: false,
                    inputId: inputId2,
                    exportId: inputId2
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
          },

          {
            "children": [
              {
                "label": "Configuration to use",
                "modified": "AWS Prod",
                "original": ""
              }
            ]
          },

          {
            "children": [
              {
                "label": "Name used in code",
                "modified": "githubReadonly",
                "original": "github"
              }
            ]
          },

          {
            "children": [
              {
                "label": "Name used in code",
                "modified": "pivotalTracker2",
                "original": "pivotalTracker"
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
      expect(result.oldLines).toEqual([[new TextPart("cat", false, true)]]);
      expect(result.newLines).toEqual([[new TextPart("dog", true)]]);
      expect(result.unifiedLines).toEqual([[new TextPart("cat", false, true), new TextPart("dog", true)]]);
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
        [new TextPart("cat\n")],
        [new TextPart("dog\n", false, true)],
        [new TextPart("bear\n")],
        []
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("cat\n")],
        [],
        [new TextPart("bear\n")],
        []
      ]);
      expect(result.unifiedLines).toEqual([
        [new TextPart("cat\n")],
        [new TextPart("dog\n", false, true)],
        [new TextPart("bear\n")],
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
        [new TextPart("cat"), new TextPart("\n")],
        [new TextPart("\n", false, true)],
        [new TextPart("\n", false, true)],
        [new TextPart("dog")]
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("cat"), new TextPart("\n")],
        [],
        [],
        [new TextPart("dog")]
      ]);
      expect(result.unifiedLines).toEqual([
        [new TextPart("cat"), new TextPart("\n", false, true)],
        [new TextPart("\n", false, true)],
        [new TextPart("\n", false, true)],
        [new TextPart("\n", true)],
        [new TextPart("dog")]
      ]);
    });

    it('handles adding new lines', () => {
      const left = `cat
dog`;
      const right = `cat

dog

`;
      const result = textDiff(left, right);
      expect(result.oldLines).toEqual([
        [new TextPart("cat"), new TextPart("\n")],
        [],
        [{ kind: "unchanged", value: "dog"}]
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("cat"), new TextPart("\n")],
        [new TextPart("\n", true)],
        [{ kind: "unchanged", value: "dog"}, new TextPart("\n", true)],
        [new TextPart("\n", true)],
        []
      ]);
      expect(result.unifiedLines).toEqual([
        [new TextPart("cat"), new TextPart("\n", false, true)],
        [new TextPart("\n", true)],
        [new TextPart("\n", true)],
        [new TextPart("dog"), new TextPart("\n", true)],
        [new TextPart("\n", true)],
        []
      ]);
    });

    it('mixed changes', () => {
      const left = `in an old house in paris
all covered with vines
lived twelve little girls
in two straight lines`;
      const right = `in a new house in nice, all covered with bricks

lived twelve little boys

with two straight sticks`;
      const result = textDiff(left, right);
      expect(result.oldLines).toEqual([
        [new TextPart("in "), new TextPart("an", false, true),
          new TextPart(" "), new TextPart("old", false, true),
          new TextPart(" house in "), new TextPart("paris\n", false, true)],
        [new TextPart("all covered with "), new TextPart("vines\n", false, true)],
        [new TextPart("lived twelve little "), new TextPart("girls\n", false, true)],
        [],
        [new TextPart("in", false, true), new TextPart(" two straight "), new TextPart("lines", false, true)]
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("in "), new TextPart("a", true),
          { kind: "unchanged", value: " "}, { kind: "added", value: "new"},
          new TextPart(" house in "), { kind: "added", value: "nice, "},
          { kind: "unchanged", value: "all covered with "}, new TextPart("bricks\n", true)],
        [new TextPart("\n", true)],
        [{ kind: "unchanged", value: "lived twelve little "}, { kind: "added", value: "boys\n"}],
        [new TextPart("\n", true)],
        [new TextPart("with", true), { kind: "unchanged", value: " two straight "}, new TextPart("sticks", true)]
      ]);
    });
  });

});