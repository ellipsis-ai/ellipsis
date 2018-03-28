import * as React from 'react';
import autobind from "../lib/autobind";
import {PageRequiredProps} from "../shared_ui/page";
import User from "../models/user";
import FormInput from '../form/input';
import Textarea from '../form/textarea';
import DynamicLabelButton from "../form/dynamic_label_button";

export interface SupportRequestProps {
  csrfToken: string,
  teamId: Option<string>,
  user: Option<User>
}

type Props = SupportRequestProps & PageRequiredProps

interface State {
  userName: string,
  email: string,
  message: string,
  isSubmitting: boolean
}

class SupportRequest extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      userName: this.props.user && this.props.user.fullName || "",
      email: "",
      message: "",
      isSubmitting: false
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

  isValidEmail(): boolean {
    const email = this.state.email.trim();
    return /^\S+@\S+$/.test(email);
  }

  formFieldsInvalid(): boolean {
    return !this.state.userName || !this.state.message || !this.isValidEmail();
  }

  onSubmit(): void {
    this.setState({
      isSubmitting: true
    }, this.doSubmit);
  }

  doSubmit(): void {
    setTimeout(() => {
      this.setState({
        isSubmitting: false
      });
    }, 500);
  }

  isSubmitting(): boolean {
    return this.state.isSubmitting;
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
            <div className="columns">
              <div className="column column-one-third narrow-column-half mobile-column-full pbl">
                <h5>Your name <span className="type-weak type-regular">(required):</span></h5>
                <FormInput
                  value={this.state.userName}
                  onChange={this.onChangeName}
                  className="form-input-borderless max-width-20"
                  placeholder="First and last name"
                />
              </div>
              <div className="column column-one-third narrow-column-half mobile-column-full pbl">
                <h5>Your email <span className="type-weak type-regular">(required):</span></h5>
                <FormInput
                  value={this.state.email}
                  onChange={this.onChangeEmail}
                  className="form-input-borderless max-width-20"
                  placeholder="person@yourcompany.com"
                />
              </div>
            </div>
            <div className="columns">
              <div className="column column-two-thirds narrow-column-full pbl">
                <h5>Your question or concern <span className="type-weak type-regular">(required):</span></h5>
                <Textarea value={this.state.message} onChange={this.onChangeMessage} rows="10" className="form-input-height-auto" />
              </div>
            </div>
            <div>
              <DynamicLabelButton
                onClick={this.onSubmit}
                className="button-primary"
                disabledWhen={this.formFieldsInvalid()}
                labels={[{
                  text: "Send request",
                  displayWhen: !this.isSubmitting()
                }, {
                  text: "Sending…",
                  displayWhen: this.isSubmitting()
                }]}
              />
            </div>
          </div>
        </div>

        {this.props.onRenderFooter()}
      </div>
    )
  }
}

export default SupportRequest;
