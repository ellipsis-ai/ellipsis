define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'FixedFooter',
    intervalId: null,
    propTypes: {
      children: React.PropTypes.node.isRequired,
      className: React.PropTypes.string
    },

    getInitialState: function() {
      return {
        exceedsWindowHeight: false
      };
    },

    checkHeight: function() {
      var footerScrollHeight = this.refs.footer.scrollHeight;
      var windowHeight = window.innerHeight;
      var exceedsWindowHeight = footerScrollHeight > windowHeight;
      if (this.state.exceedsWindowHeight !== exceedsWindowHeight) {
        this.setState({ exceedsWindowHeight: exceedsWindowHeight });
      }
    },

    getHeight: function() {
      return this.refs.footer ? this.refs.footer.clientHeight : 0;
    },

    componentDidMount: function() {
      this.intervalId = window.setInterval(this.checkHeight, 100);
    },

    componentWillUnmount: function() {
      window.clearInterval(this.intervalId);
    },

    render: function() {
      return (
        <footer
          ref="footer"
          className={`position-fixed-bottom position-z-front ${this.props.className || ""}`}
          style={(this.state.exceedsWindowHeight ? { overflowY: 'auto' } : {})}
        >
          {React.Children.map(this.props.children, (child) => child)}
        </footer>
      );
    }
  });
});
