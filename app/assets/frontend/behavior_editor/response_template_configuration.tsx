import * as React from 'react';
import BehaviorResponseType from '../models/behavior_response_type';
import ToggleGroup from '../form/toggle_group';
import HelpButton from '../help/help_button';
import ResponseTemplate from '../models/response_template';
import SectionHeading from '../shared_ui/section_heading';
import CodeEditor, {EditorScrollPosition} from "./code_editor";
import autobind from "../lib/autobind";

interface Props {
  availableHeight: number
  template: ResponseTemplate,
  onChangeTemplate: (newValue: string) => void,
  responseTypeId: string,
  possibleResponseTypes: Array<BehaviorResponseType>,
  onSelectResponseType: (responseTypeId: string) => void,
  onScrollChange: (newPosition: EditorScrollPosition) => void
  onToggleHelp: () => void,
  helpVisible: boolean,
  sectionNumber: string
}

class ResponseTemplateConfiguration extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
        <div className="container container-wide">

          <div className="mbl ptxl">
            <SectionHeading number={this.props.sectionNumber}>
              <span className="mrm">Response</span>
              <span className="display-inline-block">
                <HelpButton onClick={this.props.onToggleHelp} toggled={this.props.helpVisible}/>
              </span>
            </SectionHeading>

            <div>
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
          </div>
        </div>
        <div className="pbm">
          <CodeEditor
            availableHeight={this.props.availableHeight}
            firstLineNumber={1}
            onChange={this.props.onChangeTemplate}
            onScrollChange={this.props.onScrollChange}
            value={this.props.template.toString()}
            definitions={""}
            language={"markdown"}
            lineWrapping={true}
          />
        </div>
      </div>
    );
  }
}

export default ResponseTemplateConfiguration;
