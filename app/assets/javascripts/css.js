define(function() {
  return {
    visibleWhen: function(condition) {
      return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
    }
  };
});
