define(function(require) {
  const React = require('react');

  class NotificationForSkillDetailsWarning extends React.PureComponent {
    render() {
      const detail = this.props.details.find((ea) => ea.type === "no_skill_name");
      return (
        <span>
          <span className="type-label">Warning: </span>
          <span className="mrs">This skill is untitled. Add a title to identify it to your team.</span>
          {detail ? (
            <button className="button-s button-shrink" type="button" onClick={detail.onClick}>Edit skill details</button>
          ) : null}
        </span>
      );
    }
  }

  NotificationForSkillDetailsWarning.propTypes = {
    details: React.PropTypes.arrayOf(React.PropTypes.shape({
      kind: React.PropTypes.string.isRequired,
      type: React.PropTypes.string.isRequired,
      onClick: React.PropTypes.func
    })).isRequired
  };

  return NotificationForSkillDetailsWarning;
});
