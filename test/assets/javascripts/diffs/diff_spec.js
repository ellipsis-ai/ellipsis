window.crypto = require('./../../../mocks/mock_window_crypto');
const BehaviorVersion = require('../../../../app/assets/javascripts/models/behavior_version');

const behaviorVersion1 = Object.freeze({
  "id": "abcdef",
  "name": "First name",
  "behaviorId": "ghijkl",
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
  "groupId": "gsdfgsg",
  "behaviorId": "mnopqr",
  "functionBody": "use strict;",
  "responseTemplate": "Another template",
  "params": [],
  "triggers": [{
    "text": "A",
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
    "aws": {
      "accessKeyName": "AWS_ACCESS_KEY",
      "secretKeyName": "AWS_SECRET_KEY",
      "regionName": "AWS_REGION"
    }
  },
  "createdAt": 1468359271138
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

describe('BehaviorVersion', () => {

  describe('maybeDiffFor', () => {

    it('builds the correct diff', () => {
      const version1 = BehaviorVersion.fromJson(behaviorVersion1);
      const version2 = BehaviorVersion.fromJson(behaviorVersion2);
      const maybeDiff = version1.maybeDiffFor(version2);
      expect(maybeDiff).toBeTruthy();
      const diffText = maybeDiff.displayText();

      console.log(diffText);

      expect(diffText).toContain("Modified action:");
      expect(diffText).toContain("Name: [-First][+Second] name");
      expect(diffText).toContain("Description: [+A description]");
      expect(diffText).toContain("Response template: A[+nother] template");
    });

  });

});
