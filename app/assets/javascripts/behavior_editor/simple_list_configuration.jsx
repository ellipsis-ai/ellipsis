define(function(require) {
  var React = require('react'),
    SectionHeading = require('./section_heading'),
    Checklist = require('./checklist'),
    Collapsible = require('../collapsible'),
    Input = require('../form/input');

  return React.createClass({
    propTypes: {
      behaviorHasParams: React.PropTypes.bool.isRequired,
      listName: React.PropTypes.string,
      isEditingListName: React.PropTypes.bool.isRequired,
      existingLists: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      onListNameChange: React.PropTypes.func.isRequired,
      onEditListName: React.PropTypes.func.isRequired
    },

    displayName: 'SimpleListConfiguration',

    getInitialState: function() {
      return {
        isEditingListName: false
      };
    },

    getListName: function() {
      return this.props.listName || "";
    },

    shouldShowPrompt: function() {
      return this.props.behaviorHasParams && !this.shouldShowInput();
    },

    shouldShowInput: function() {
      return this.props.behaviorHasParams && (this.props.isEditingListName || !!this.props.selectedListName);
    },

    render: function() {
      return (
        <div>
          <Collapsible revealWhen={this.shouldShowPrompt()}>
            <div className="bg-blue-lighter border-top border-blue ptl pbs">
              <div className="container container-wide">
                <div className="columns columns-elastic mobile-columns-float">
                  <div className="column column-expand">
                    <p className="mbs">
                      <span>You can save users' responses in a list to be viewed or queried later.</span>
                    </p>
                  </div>
                  <div className="column column-shrink align-r align-m mobile-align-l display-ellipsis mobile-display-no-ellipsis">
                    <button type="button" className="button-s mbs mobile-mrm" onClick={this.props.onEditListName}>Save responses</button>
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.shouldShowInput()}>

            <hr className="mtn full-bleed thin bg-gray-light" />

            <div className="columns container">
              <div className="column column-page-sidebar mbxxl mobile-mbs">
                <SectionHeading number="3">Save user responses</SectionHeading>

                <Checklist disabledWhen={false}>
                  <Checklist.Item checkedWhen={!!this.props.listName}>
                    <span>All user inputs will be stored in this list.</span>
                  </Checklist.Item>
                  <Checklist.Item hiddenWhen={!this.props.listName}>
                    <span>You can query the list from other actions by doing </span>
                    <code>{`ellipsis.getListNamed('${this.props.listName}', list => { ... })`}</code>
                  </Checklist.Item>
                </Checklist>
              </div>
              <div className="column column-page-main mbxxl">
                <div>
                  <div className="mbm">
                    <div>
                      <span>Store user responses in a list named</span>
                      <Input
                        id="simpleListName"
                        ref="simpleListName"
                        placeholder="Name of list"
                        value={this.getListName()}
                        onChange={this.props.onListNameChange}
                        className="form-input-borderless"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>
        </div>

      );
    }
  });
});
