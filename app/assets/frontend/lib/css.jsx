const CSS = {
    visibleWhen: function(condition) {
      return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
    }
};

export default CSS;
