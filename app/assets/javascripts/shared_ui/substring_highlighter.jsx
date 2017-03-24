define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'SubstringHighlighter',
    propTypes: {
      text: React.PropTypes.string,
      substring: React.PropTypes.string,
      highlightClassName: React.PropTypes.string
    },

    render: function() {
      const text = this.props.text;
      const sub = this.props.substring;
      if (!text) {
        return null;
      } else if (!sub) {
        return (
          <span>{text}</span>
        );
      }

      const choppedLowercase = text.toLowerCase().split(sub.toLowerCase());
      const highlightLength = sub.length;
      let startIndex = 0;

      return (
        <span>
          {choppedLowercase.map((lowercaseFragment, index) => {
            const fragmentLength = lowercaseFragment.length;
            const displayFragment = text.substr(startIndex, fragmentLength);
            const highlighted = text.substr(startIndex + fragmentLength, highlightLength);
            startIndex += fragmentLength + highlightLength;

            return (
              <span key={`fragment${index}`}>
                <span>{displayFragment}</span>
                {highlighted ? (
                  <span className={this.props.highlightClassName || "type-bold"}>{highlighted}</span>
                ) : null}
              </span>
            );
          })}
        </span>
      );
    }
  });
});
