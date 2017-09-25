import React from 'react';
import TestUtils from 'react-addons-test-utils';
const CodeEditorHelp = require('../app/assets/javascripts/behavior_editor/code_editor_help');

describe('CodeEditorHelp', () => {
  const defaultProps = Object.freeze({
    functionBody: '',
    isFinishedBehavior: false,
    onToggleHelp: jest.fn(),
    helpIsActive: false,
    hasInputs: false,
    sectionNumber: "2"
  });

  let props;

  beforeEach(() => {
    props = Object.assign({}, defaultProps);
  });

  function createComponent(config) {
    return TestUtils.renderIntoDocument(
      <CodeEditorHelp {...config} />
    );
  }

  describe('hasCalledOnSuccess', () => {
    it('returns true when the code includes ellipsis.success called with something', () => {
      props.functionBody = 'var f = "b";\nellipsis.success(f);';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnSuccess()).toBe(true);
    });
    it('returns true when the code includes ellipsis.success called with nothing', () => {
      props.functionBody = 'var f = "b";\nellipsis.success();';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnSuccess()).toBe(true);
    });
    it('returns false when the code doesn’t include onSuccess', () => {
      props.functionBody = 'var f = "b";';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnSuccess()).toBe(false);
    });
  });
});
