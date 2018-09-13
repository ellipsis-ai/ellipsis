import * as React from 'react';
import Checklist, {ChecklistItem} from './checklist';
import HelpPanel from '../help/panel';
import ResponseTemplate from '../models/response_template';

interface Props {
  firstParamName?: Option<string>
  template?: Option<ResponseTemplate>,
  onCollapseClick: () => void
}

class ResponseTemplateHelp extends React.Component<Props> {
  getUserParamTemplateHelp() {
    return (
      <ChecklistItem checkedWhen={this.props.template && this.props.template.includesAnyParam()}>
        Repeat back user input:<br />
        <div className="box-code-example">
          You said {this.getExampleParamName()}
        </div>
      </ChecklistItem>
    );
  }

  getExampleParamName(): string {
    return this.props.firstParamName ? `{${this.props.firstParamName}}` : "{exampleParamName}";
  }

  getSuccessResultTemplateHelp() {
    return (
      <ChecklistItem checkedWhen={Boolean(this.props.template && this.props.template.includesSuccessResult())}>
        <div>
          Say the result provided to <code>ellipsis.success</code>, if it’s a string:<br />
          <div className="box-code-example">
            The answer is {"{successResult}"}
          </div>
        </div>
      </ChecklistItem>
    );
  }

  getPathTemplateHelp() {
    return (
      <ChecklistItem checkedWhen={this.props.template && this.props.template.includesPath()}>
        Include properties of the result if it’s an object:<br />
        <div className="box-code-example">
          Name: {"{successResult.user.name}"}
        </div>
      </ChecklistItem>
    );
  }

  getIterationTemplateHelp() {
    return (
      <ChecklistItem checkedWhen={this.props.template && this.props.template.includesIteration()}>
        Iterate through a list/array of items:<br />
        <pre className="box-code-example">{
`{for item in successResult.items}
* {item}
{endfor}`
        }</pre>
      </ChecklistItem>
    );
  }

  getLogicHelp() {
    return (
      <ChecklistItem checkedWhen={this.props.template && this.props.template.includesIfLogic()}>
        Use if/else logic with strict boolean values:<br />
        <pre className="box-code-example">{
`{if successResult.booleanValue}
It worked
{else}
Something went wrong.
{endif}`
        }</pre>
      </ChecklistItem>
    );
  }

  render() {
    return (
      <HelpPanel
        heading="Response template"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>Write a response for Ellipsis to send when the action is complete. You can use </span>
          <span><a href="http://commonmark.org/help/" target="_blank" rel="noreferrer noopener">Markdown</a> formatting, if desired.</span>
        </p>

        <p>Use the following methods to insert data and add basic logic:</p>

        <Checklist className="mtxs list-space-l" disabledWhen={false}>
          {this.getUserParamTemplateHelp()}
          {this.getSuccessResultTemplateHelp()}
          {this.getPathTemplateHelp()}
          {this.getIterationTemplateHelp()}
          {this.getLogicHelp()}
        </Checklist>

      </HelpPanel>
    );
  }
}

export default ResponseTemplateHelp;
