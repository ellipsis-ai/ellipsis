import * as React from 'react';
import BehaviorResponseType from '../models/behavior_response_type';
import ToggleGroup from '../form/toggle_group';
import HelpButton from '../help/help_button';
import ResponseTemplate from '../models/response_template';
import SectionHeading from '../shared_ui/section_heading';
import {render} from 'react-dom';
import MonacoEditor from 'react-monaco-editor';
import 'monaco-editor/esm/vs/basic-languages/markdown/markdown';

interface Props {
  template: ResponseTemplate,
  onChangeTemplate: (newValue: string) => void,
  responseTypeId: string,
  possibleResponseTypes: Array<BehaviorResponseType>,
  onSelectResponseType: (responseTypeId: string) => void,
//  onCursorChange: (cm: Editor) => void,
  onToggleHelp: () => void,
  helpVisible: boolean,
  sectionNumber: string
}

class ResponseTemplateConfiguration extends React.Component<Props> {
    render() {
      return (
        <div className="columns container container-wide">

          <div className="mbl ptxl">
            <SectionHeading number={this.props.sectionNumber}>
              <span className="mrm">Response</span>
              <span className="display-inline-block">
                <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible}/>
              </span>
            </SectionHeading>

            <div className="border-top border-left border-right border-light pas">
              <ToggleGroup className="form-toggle-group-s align-m">
                {this.props.possibleResponseTypes.map((ea, index) => (
                  <ToggleGroup.Item
                    key={"response-type-" + index}
                    activeWhen={this.props.responseTypeId === ea.id}
                    onClick={this.props.onSelectResponseType.bind(null, ea.id)}
                    label={ea.displayString}
                  />
                ))}
              </ToggleGroup>
            </div>
            <div className="pbm">
              <div className="border" style={{ height: "300px" }}>
                <MonacoEditor
                  language="markdown"
                  value={this.props.template.toString()}
                  onChange={this.props.onChangeTemplate}
                  options={{
                    folding: false,
                    fontSize: 15,
                    fontFamily: "Source Code Pro",
                    lineNumbers: "off",
                    minimap: {
                      enabled: false
                    },
                    wordWrap: "on"
                  }}
                />
              </div>
            </div>
            {/*<div className="position-relative CodeMirror-container-no-gutter pbm">*/}
              {/*<CodeMirrorWrapper value={this.props.template.toString()}*/}
                {/*onChange={this.props.onChangeTemplate}*/}
                {/*onCursorChange={this.props.onCursorChange}*/}
                {/*options={{*/}
                  {/*mode: "gfm",*/}
                  {/*gutters: ['CodeMirror-no-gutter'],*/}
                  {/*indentUnit: 4,*/}
                  {/*indentWithTabs: true,*/}
                  {/*lineWrapping: true,*/}
                  {/*lineNumbers: false,*/}
                  {/*smartIndent: true,*/}
                  {/*tabSize: 4,*/}
                  {/*viewportMargin: Infinity,*/}
                  {/*placeholder: "{successResult}"*/}
                {/*}}*/}
              {/*/>*/}
            {/*</div>*/}
          </div>
        </div>

      );
    }
}

export default ResponseTemplateConfiguration;
