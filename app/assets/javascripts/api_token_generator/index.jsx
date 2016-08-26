define(function(require) {
  var React = require('react'),
      Formatter = require('../formatter'),
      SettingsMenu = require('../settings_menu');

  return React.createClass({
    displayName: "ApiTokenGenerator",
    propTypes: {
      tokens: React.PropTypes.arrayOf(React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        label: React.PropTypes.string.isRequired,
        lastUsed: React.PropTypes.number,
        createdAt: React.PropTypes.number.isRequired,
        isRevoked: React.PropTypes.bool.isRequired
      })).isRequired
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container pbm">
              <h3 className="mvn ptxxl type-weak display-ellipsis">Ellipsis API tokens</h3>
            </div>
          </div>
          <div className="flex-container">
            <div className="container flex flex-center">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="apiTokens"/>
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxxl pbxxxxl phxxxxl">
                  <div className="columns columns-elastic mobile-columns-float">
                    <div className="column-group">
                      {this.props.tokens.map((token, index) => {
                        return (
                          <div className="column-row" key={`token${index}`}>
                            <div className="column column-expand">
                              {token.label}
                            </div>
                            <div className="column column-shrink type-s type-weak type-italic display-ellipsis">
                              Created {Formatter.formatTimestampRelativeIfRecent(token.createdAt)}
                            </div>
                            <div className="column column-shrink type-s display-ellipsis">
                              {token.lastUsed ? Formatter.formatTimestampRelativeIfRecent(token.lastUsed) : null}
                            </div>
                            <div className="column column-shrink">
                              {token.isRevoked ? (<i>Token revoked</i>) : (
                                <button type="button" className="button-s">Revoke</button>)}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
