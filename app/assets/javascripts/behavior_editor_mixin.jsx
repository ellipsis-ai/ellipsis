if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(function() {
  return {
    visibleWhen: function(condition) {
      return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
    }
  };
});
