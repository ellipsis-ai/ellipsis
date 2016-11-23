jest.unmock('../app/assets/javascripts/behavior_editor/code_editor_help');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const CodeEditorHelp = require('../app/assets/javascripts/behavior_editor/code_editor_help');

describe('CodeEditorHelp', () => {
  const defaultProps = Object.freeze({
    functionBody: '',
    isFinishedBehavior: false,
    onToggleHelp: jest.fn(),
    helpIsActive: false,
    hasUserParameters: false,
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

  describe('hasCalledOnError', () => {
    it('returns true when the code includes onError called with a string', () => {
      props.functionBody = 'var f = "b";\nonError("this is an error");';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnError()).toBe(true);
    });
    it('returns false when the code includes onError called with nothing', () => {
      props.functionBody = "var f = 'b';\nonError();";
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnError()).toBe(false);
    });
    it('returns true when the code includes ellipsis.error called with a string', () => {
      props.functionBody = 'var f = "b";\nellipsis.error("this is an error");';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnError()).toBe(true);
    });
    it('returns false when the code includes ellipsis.error called with nothing', () => {
      props.functionBody = "var f = 'b';\nellipsis.error();";
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnError()).toBe(false);
    });
    it('returns false when the code doesn’t include onError', () => {
      props.functionBody = 'var f = "b";';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnError()).toBe(false);
    });
  });

  describe('hasCalledOnSuccess', () => {
    it('returns true when the code includes onSuccess called with something', () => {
      props.functionBody = 'var f = "b";\nonSuccess(f);';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnSuccess()).toBe(true);
    });
    it('returns true when the code includes onSuccess called with nothing', () => {
      props.functionBody = 'var f = "b";\nonSuccess();';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledOnSuccess()).toBe(true);
    });
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

  describe('hasCalledNoResponse', () => {
    it('returns true when the code includes noResponse with nothing', () => {
      props.functionBody = 'var f = "b";\nellipsis.noResponse();';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledNoResponse()).toBe(true);
    });
    it('returns false when the code doesn’t include noResponse', () => {
      props.functionBody = 'var f = "b";';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledNoResponse()).toBe(false);
    });
  });

  describe('hasCalledRequire', () => {
    it('returns true when the code calls require with something', () => {
      props.functionBody = 'var Intl = require("intl");\nIntl.NumberFormat().format();';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledRequire()).toBe(true);
    });
    it('returns false when the code calls require with nothing', () => {
      props.functionBody = 'var Intl = require();\nIntl.NumberFormat().format();';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledRequire()).toBe(false);
    });
    it('returns false when the code doesn’t call require', () => {
      props.functionBody = 'var f = "b";';
      const myComponent = createComponent(props);
      expect(myComponent.hasCalledRequire()).toBe(false);
    });
  });

});
