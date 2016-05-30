define(function() {
  return {
    padSpace: function(string, totalChars) {
      var result = String(string);
      while (result.length < totalChars) {
        result = ' ' + result;
      }
      return result;
    },
    visibleWhen: function(condition, shouldRemoveFromLayout) {
      if (shouldRemoveFromLayout) {
        return " display " + (condition ? "display-visible" : "display-hidden") + " ";
      } else {
        return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
      }
    }
  };
});
