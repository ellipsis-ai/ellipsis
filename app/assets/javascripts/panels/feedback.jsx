define(function(require) {
  const React = require('react'),
    autobind = require('../lib/autobind'),
    Button = require('../form/button'),
    Textarea = require('../form/textarea');

  class Feedback extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        feedback: ""
      };
    }

    onChange(newValue) {
      this.setState({
        feedback: newValue
      });
    }

    render() {
      return (
        <div className="box-action phn">
          <div className="container">
            <p>Do you have feedback about your experience with Ellipsis? Write any comments you have below, and send them to the team.</p>

            <Textarea
              rows={"3"}
              placeholder={""}
              value={this.state.feedback}
              onChange={this.onChange}
            />
            <div className="mvl">
              <Button className="button-primary mrs mbs" onClick={() => {}}>Send feedback</Button>
              <Button className="mrs mbs" onClick={this.props.onDone}>Cancel</Button>
            </div>
          </div>
        </div>
      );
    }
  }

  Feedback.propTypes = {
    onDone: React.PropTypes.func.isRequired
  };

  return Feedback;
});
