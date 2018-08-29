import BehaviorVersion, {BehaviorVersionJson} from '../../../../app/assets/frontend/models/behavior_version';

const normalResponseTypeJson = {
  id: "Normal",
  displayString: "Respond normally"
};

const behaviorVersionTask1: BehaviorVersionJson = Object.freeze({
  "id": "abcdef",
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
    },
    isDataType: false,
    isTest: false,
    responseType: normalResponseTypeJson
  },
  "createdAt": 1468338136532,
  inputIds: []
});
const behaviorVersionTask2: BehaviorVersionJson = Object.freeze({
  "id": "abcdef",
  "groupId": "gsdfgsg",
  "behaviorId": "mnopqr",
  "functionBody": "use strict;",
  "responseTemplate": "A template",
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
    },
    isDataType: false,
    isTest: false,
    responseType: normalResponseTypeJson
  },
  "createdAt": 1468359271138,
  inputIds: []
});
const behaviorVersionKnowledge1: BehaviorVersionJson = Object.freeze({
  "id": "abcdef",
  "behaviorId": "stuvwx",
  "functionBody": "",
  "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
  "params": [],
  "triggers": [{
    "text": "",
    "requiresMention": false,
    "isRegex": false,
    "caseSensitive": false
  }, {
    "text": "",
    "requiresMention": false,
    "isRegex": false,
    "caseSensitive": false
  }],
  "config": {
    isDataType: false,
    isTest: false,
    responseType: normalResponseTypeJson
  },
  "createdAt": 1466109904858,
  inputIds: []
});
const defaultStorageDataType: BehaviorVersionJson = Object.freeze({
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
          id: "Text",
          exportId: "Text"
        },
        isLabel: true
      }, {
        fieldId: "3",
        fieldVersionId: "4",
        name: "field2",
        fieldType: {
          name: "Text",
          id: "Text",
          exportId: "Text"
        },
        isLabel: false
      }],
      usesCode: false
    },
    isDataType: true,
    isTest: false,
    responseType: normalResponseTypeJson
  },
  inputIds: [],
  triggers: [],
  functionBody: ""
});

describe('BehaviorVersion', () => {
  describe('findFirstTriggerIndexForDisplay', () => {
    it('returns the first non-regex trigger with text when available', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask1);
      expect(version.findFirstTriggerIndexForDisplay()).toBe(1);
    });

    it('returns the first trigger when they’re all regex', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask2);
      expect(version.findFirstTriggerIndexForDisplay()).toBe(0);
    });

    it('returns the first trigger when none have text', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionKnowledge1);
      expect(version.findFirstTriggerIndexForDisplay()).toBe(0);
    });
  });

  describe('fromJson/construction', () => {
    it('includes the defined triggers', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask1);
      expect(version.triggers.length).toEqual(2);
      expect(version.triggers.map(ea => ea.text)).toEqual(["B", "C"]);
    });

    it('doesn\'t set createdAt if not provided', () => {
      const withoutTimestamp = Object.assign({}, behaviorVersionTask1);
      delete withoutTimestamp.createdAt;
      const version = BehaviorVersion.fromJson(withoutTimestamp);
      expect(version.createdAt).toBeFalsy();
    });

    it('respects createdAt if provided', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask1);
      expect(version.createdAt).toBe(behaviorVersionTask1.createdAt);
    });
  });

  describe('equality', () => {
    it('finds two versions unequal with different names', () => {
      const version1 = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ name: "Thing1" });
      const version2 = version1.clone({ name: "Thing2" });
      expect(version1.isIdenticalToVersion(version2)).toBe(false);
    });
    it('finds two versions equal with different editor scroll positions and timestamps', () => {
      const version1 = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ editorScrollPosition: 0, createdAt: 1450000000000 });
      const version2 = version1.clone({ editorScrollPosition: 100, createdAt: 1451000000000 });
      expect(version1.isIdenticalToVersion(version2)).toBe(true);
    });
  });

  describe('toJSON', () => {
    it('nulls out createdAt and editorScrollPosition from JSON stringify for save/export', () => {
      const version = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ editorScrollPosition: 100 });
      const versionJson = JSON.stringify(version);
      const jsonObjectProps = JSON.parse(versionJson);
      expect(jsonObjectProps.createdAt).toBe(null);
      expect(jsonObjectProps.editorScrollPosition).toBe(null);
    });
  });

  describe('timestampForAlphabeticalSort', () => {
    describe('returns a zero-padded numeric timestamp string with 15 characters', () => {
      it('handles a numeric timestamp', () => {
        const version = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ createdAt: 1234567890123 });
        expect(version.timestampForAlphabeticalSort()).toBe("001234567890123");
      });
      it('handles an ISO timestamp', () => {
        const version = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ createdAt: "2017-03-14T18:55:28.710Z" });
        expect(version.timestampForAlphabeticalSort()).toBe("001489517728710");
      });
      it('handles an ISO timestamp with a TZ offset', () => {
        const version = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ createdAt: "2017-03-14T13:55:28.710-05:00" });
        expect(version.timestampForAlphabeticalSort()).toBe("001489517728710");
      });
    });
  });

  describe('sortKey', () => {
    it('sorts non-new by name, first trigger, then by timestamp, with a leading A', () => {
      const replaceTrigger = Object.assign({}, behaviorVersionTask1);
      replaceTrigger.triggers[0].text = "Trigger!";
      replaceTrigger.triggers[0].isRegex = false;
      const version1 = BehaviorVersion.fromJson(replaceTrigger).clone({ name: "Name" });
      const version2 = BehaviorVersion.fromJson(replaceTrigger).clone({ triggers: [] });
      const version3 = BehaviorVersion.fromJson(replaceTrigger);
      expect(version1.sortKey()).toBe("AName");
      expect(version2.sortKey()).toEqual("A" + version2.timestampForAlphabeticalSort());
      expect(version3.sortKey()).toBe("ATrigger!");
    });

    it('sorts new by timestamp only, with a leading Z', () => {
      const version1 = BehaviorVersion.fromJson(behaviorVersionTask1).clone({ name: "Name", isNew: true });
      expect(version1.sortKey()).toEqual("Z" + version1.timestampForAlphabeticalSort());
    });
  });

  describe('buildGraphQLListQuery', () => {
    it('returns a valid GraphQL list query for a default storage data type', () => {
      const dataType = BehaviorVersion.fromJson(defaultStorageDataType);
      expect(dataType.buildGraphQLListQuery()).toEqual(`{ myDataTypeList(filter: {}) { field1 field2 } }`);
    });
  });

  describe('getEnvVarNamesInFunction', () => {
    it('returns the list of unique environment variable names used in the function', () => {
      const behavior = BehaviorVersion.fromJson(behaviorVersionTask1).clone({
        functionBody: `const f = ellipsis.env.ONE_ENV_VAR
const g = ellipsis.env.ANOTHER_ENV_VAR;
const h = truthy ? (ellipsis.env.ONE_ENV_VAR : ellipsis.env.ANOTHER_ENV_VAR);
const i = ellipsis.env.lowercaseEnvVarIsInvalid;
`
      });
      expect(behavior.getEnvVarNamesInFunction()).toEqual(['ONE_ENV_VAR', 'ANOTHER_ENV_VAR']);
    });
  });
});
