define(function() {
  return {
    visibleWhen: function(condition) {
      return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
    },

    eventKeyPressWasEnter: function(event) {
      return event.which === 13;
    },

    eventKeyPressWasEsc: function(event) {
      return event.which === 27;
    },

    eventKeyPressWasSpace: function(event) {
      return event.which === 32;
    },

    eventKeyPressWasTab: function(event) {
      return event.which === 9;
    },

    eventKeyPressIncludedShift: function(event) {
      return event.shiftKey;
    }
  };
});
