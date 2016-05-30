define(function() {
  return {
    visibleWhen: function(condition, shouldRemoveFromLayout) {
      if (shouldRemoveFromLayout) {
        return " display " + (condition ? "display-visible" : "display-hidden") + " ";
      } else {
        return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
      }
    }
  };
});
