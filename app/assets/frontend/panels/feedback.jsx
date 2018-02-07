import * as React from 'react';
import autobind from '../lib/autobind';
import DataRequest from '../lib/data_request';
import Button from '../form/button';
import Textarea from '../form/textarea';

class FeedbackPanel extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = this.defaultState();
    }

    defaultState() {
      return {
        feedback: "",
        error: null,
        lastSent: null
      };
    }

    onChange(newValue) {
      this.setState({
        feedback: newValue
      });
    }

    sendFeedback() {
      this.setState({
        error: null
      }, () => {
        const url = jsRoutes.controllers.FeedbackController.send().url;
        DataRequest.jsonPost(url, {
          message: this.state.feedback
        }, this.props.csrfToken).then(() => {
          this.setState({
            feedback: "",
            lastSent: this.state.feedback
          });
        }).catch(() => {
          this.setState({
            error: "An error occurred while sending your comments. Please try again.",
            lastSent: null
          });
        });
      });
    }

    done() {
      this.props.onDone();
      this.setState(this.defaultState());
    }

    hasNoFeedback() {
      return !this.state.feedback.trim();
    }

    renderStatus() {
      if (this.state.error) {
        return (
          <span className="type-pink type-bold type-italic fade-in align-button mbs">— {this.state.error}</span>
        );
      } else if (this.state.lastSent) {
        return (
          <span className="type-green fade-in align-button mbs">— Your comments have been sent. Thank you.</span>
        );
      }
    }

    render() {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="mtn type-weak">Feedback</h4>
              </div>
              <div className="column column-page-main">
                <p>Do you have feedback about your experience with Ellipsis? Write any comments you have below, and send them to the team.</p>

                <Textarea
                  className={"form-input-height-auto"}
                  rows={"3"}
                  placeholder={""}
                  value={this.state.feedback}
                  onChange={this.onChange}
                />
                <div className="mvl">
                  <Button className="button-primary mrs mbs" onClick={this.sendFeedback} disabled={this.hasNoFeedback()}>Send feedback</Button>
                  <Button className="mrs mbs" onClick={this.done}>{this.hasNoFeedback() ? "Done" : "Cancel"}</Button>
                  {this.renderStatus()}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

FeedbackPanel.propTypes = {
  csrfToken: React.PropTypes.string.isRequired,
  onDone: React.PropTypes.func.isRequired
};

export default FeedbackPanel;
