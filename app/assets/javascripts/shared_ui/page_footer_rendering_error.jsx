define(function() {
  function PageFooterRenderingError(pageInstance) {
    const msg = `Page component’s onRenderFooter method not called by child component: ${
      pageInstance.component ? pageInstance.component.constructor.name : "(no child component)"
      }`;
    Error.call(this, msg);
    this.message = msg;
  }

  return PageFooterRenderingError;
});
