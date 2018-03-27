import * as React from 'react';
import autobind from "../lib/autobind";
import {PageRequiredProps} from "../shared_ui/page";
import User from "../models/user";
import FormInput from '../form/input';
import Textarea from '../form/textarea';

export interface SupportRequestProps {
  csrfToken: string,
  teamId: Option<string>,
  user: Option<User>
}

type Props = SupportRequestProps & PageRequiredProps

interface State {
  userName: string,
  email: string,
  message: string
}

class SupportRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      userName: this.props.user && this.props.user.fullName || "",
      email: "",
      message: ""
    }
  }

  onChange(fieldName: keyof State, newValue: string): void {
    const newState = {};
    newState[fieldName] = newValue;
    this.setState(newState);
  }

  onChangeName(newValue: string): void {
    this.onChange("userName", newValue);
  }

  onChangeEmail(newValue: string): void {
    this.onChange("email", newValue);
  }

  onChangeMessage(newValue: string): void {
    this.onChange("message", newValue);
  }

  render() {
    return (
      <div id="content">
        <div className="bg-black type-white border-emphasis-bottom border-pink">
          <div className="container container-narrow container-c ptxxxxl pbxxl mobile-pvxl">
            <h1 className="mobile-mtn">Support request</h1>
          </div>
        </div>

        <div className="bg-white">
          <div className="container container-narrow container-c pvxxxl mobile-pvl">
            <div className="pbxl">
              <h5>Your name:</h5>
              <FormInput value={this.state.userName} onChange={this.onChangeName} className="form-input-borderless max-width-20" />
            </div>
            <div className="pbxl">
              <h5>Your email:</h5>
              <FormInput value={this.state.email} onChange={this.onChangeEmail} className="form-input-borderless max-width-20" />
            </div>
            <div className="">
              <h5>Your question or concern:</h5>
              <Textarea value={this.state.message} onChange={this.onChangeMessage} rows="10" className="form-input-height-auto" />
            </div>
          </div>
        </div>

        {this.props.onRenderFooter()}
      </div>
    )
  }
}

export default SupportRequest;
