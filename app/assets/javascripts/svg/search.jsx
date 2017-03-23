define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      label: React.PropTypes.string
    },
    label: function() {
      return this.props.label || 'Search';
    },
    render: function() {
      return (
        <svg height="100%" viewBox="0 0 18 24">
          <title>{this.label()}</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="search" fill="currentColor">
              <rect id="stem" transform="translate(12.535534, 15.035534) rotate(45.000000) translate(-12.535534, -15.035534) " x="8.53553391" y="14.0355339" width="8" height="2" />
              <path d="M6.5,13 C8.709139,13 10.5,11.209139 10.5,9 C10.5,6.790861 8.709139,5 6.5,5 C4.290861,5 2.5,6.790861 2.5,9 C2.5,11.209139 4.290861,13 6.5,13 Z M6.5,14 C3.73857625,14 1.5,11.7614237 1.5,9 C1.5,6.23857625 3.73857625,4 6.5,4 C9.26142375,4 11.5,6.23857625 11.5,9 C11.5,11.7614237 9.26142375,14 6.5,14 Z" id="circle" fillRule="nonzero" />
            </g>
          </g>
        </svg>
      );
    }
  });

});
