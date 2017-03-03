define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    Input = require('../form/input'),
    Textarea = require('../form/textarea');

  return React.createClass({
    displayName: 'BehaviorGroupEditor',
    propTypes: {
      group: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      onBehaviorGroupNameChange: React.PropTypes.func.isRequired,
      onBehaviorGroupDescriptionChange: React.PropTypes.func.isRequired
    },

    focus: function() {
      if (this.props.group.name) {
        this.refs.skillDescription.focus();
      } else {
        this.refs.skillName.focus();
      }
    },

    render: function() {
      return (
        <div className="container container-narrow mtl">

          <h5 className="type-blue-faded">Edit skill details</h5>

          <h4 className="mtl mbn">Title</h4>
          <div className="columns">
            <div className="column column-one-third">
              <Input
                ref="skillName"
                className="form-input-borderless form-input-l type-bold mbn"
                placeholder="Add a title (optional)"
                onChange={this.props.onBehaviorGroupNameChange}
                value={this.props.group.name || ""}
              />
            </div>
          </div>

          <h4 className="mtxxl mbs">Description</h4>
          <div>
            <Textarea
              ref="skillDescription"
              className="form-input-height-auto"
              placeholder="Describe the general purpose of this skill (optional). The description is displayed in help."
              onChange={this.props.onBehaviorGroupDescriptionChange}
              value={this.props.group.description || ""}
              rows={"3"}
            />
          </div>
        </div>
      );
    }
  });
});
