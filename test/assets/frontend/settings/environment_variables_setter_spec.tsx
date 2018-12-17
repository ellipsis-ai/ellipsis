import * as React from 'react';
import * as TestUtils from 'react-dom/test-utils';

import EnvironmentVariableSetter, {EnvironmentVariableSetterProps} from '../../../../app/assets/frontend/settings/environment_variables/setter';

describe('EnvironmentVariableSetter', () => {
  const defaultConfig: EnvironmentVariableSetterProps = {
    onSave: jest.fn(),
    onDelete: jest.fn(),
    activePanelName: "",
    activePanelIsModal: false,
    onToggleActivePanel: jest.fn(),
    onClearActivePanel: jest.fn(),
    teamId: "1",
    csrfToken: "1",
    isAdmin: false,
    onAdminLoadedValue: jest.fn(),
    vars: []
  };

  function createSetter(config: EnvironmentVariableSetterProps): EnvironmentVariableSetter {
    return TestUtils.renderIntoDocument(
      <EnvironmentVariableSetter {...config} />
    ) as EnvironmentVariableSetter;
  }

  let config: EnvironmentVariableSetterProps;

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('setNewVarIndexName', () => {
    it('sets and formats the name of a new var at a particular index', () => {
      const setter = createSetter(config);
      setter.setState = jest.fn();
      setter.getNewVars = jest.fn(() => [{
        name: "OLD_NAME",
        value: "old value"
      }]);
      setter.setNewVarIndexName(0, "tada wow");
      expect(setter.setState).toBeCalledWith({
        newVars: [{
          name: "TADA_WOW",
          value: "old value"
        }]
      });
    });
  });

  describe('getDuplicateNames', () => {
    it('returns any new names that are duplicates of old names', () => {
      config.vars = [{
        name: "UNIQUE",
        value: "",
        isAlreadySavedWithValue: false
      }, {
        name: "WOW",
        value: "",
        isAlreadySavedWithValue: false
      }];
      var setter = createSetter(config);
      setter.getNewVars = jest.fn(() => [{
        name: "UNIQUE",
        value: "",
        isAlreadySavedWithValue: false
      }, {
        name: "ORIGINAL",
        value: "",
        isAlreadySavedWithValue: false
      }]);
      expect(setter.getDuplicateNames()).toEqual(["UNIQUE"]);
    });
  });

  describe('onSave', () => {
    it('combines new named vars into existing ones and resets new vars', () => {
      var setter = createSetter(config);
      setter.getVars = jest.fn(() => [{
        name: "OLD",
        value: "",
        isAlreadySavedWithValue: false
      }]);
      setter.getNewVars = jest.fn(() => [{
        name: "NEW",
        value: "",
        isAlreadySavedWithValue: false
      }, {
        name: "",
        value: "throwaway",
        isAlreadySavedWithValue: false
      }]);
      const setStateMock = jest.fn();
      setter.setState = setStateMock;
      setter.onSave();
      var newState = setStateMock.mock.calls[0][0];
      expect(newState.vars).toEqual([{
        name: "OLD",
        value: "",
        isAlreadySavedWithValue: false
      }, {
        name: "NEW",
        value: "",
        isAlreadySavedWithValue: false
      }]);
      expect(newState.newVars).toEqual([{
        name: "",
        value: "",
        isAlreadySavedWithValue: false
      }]);
    });
  });

  describe('getRowCountForTextareaValue', () => {
    it('returns an integer equal to the number of lines of text, min 1, max 5', () => {
      var setter = createSetter(config);
      expect(setter.getRowCountForTextareaValue(null)).toBe(1);
      expect(setter.getRowCountForTextareaValue("foo")).toBe(1);
      expect(setter.getRowCountForTextareaValue("foo\n")).toBe(2);
      expect(setter.getRowCountForTextareaValue("foo\nbar\nbaz")).toBe(3);
      expect(setter.getRowCountForTextareaValue("a\nb\nc\nd\ne\nf\ng\n")).toBe(5);
    });
  });
});
