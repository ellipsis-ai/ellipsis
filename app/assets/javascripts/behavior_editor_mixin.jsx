define(function() {
  return {
    visibleWhen: function(condition) {
      return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
    },

    eventKeyPressWasDown: function(event) {
      return event.which === 40;
    },

    eventKeyPressWasEnter: function(event) {
      return event.which === 13;
    },

    eventKeyPressWasEsc: function(event) {
      return event.which === 27;
    },

    eventKeyPressWasLeft: function(event) {
      return event.which === 37;
    },

    eventKeyPressWasRight: function(event) {
      return event.which === 39;
    },

    eventKeyPressWasSpace: function(event) {
      return event.which === 32;
    },

    eventKeyPressWasTab: function(event) {
      return event.which === 9;
    },

    eventKeyPressWasUp: function(event) {
      return event.which === 38;
    },

    eventKeyPressIncludedShift: function(event) {
      return event.shiftKey;
    }
  };
});
