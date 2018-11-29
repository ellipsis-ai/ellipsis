/* Can't use ES6 class syntax because Babel doesn't extend built-in types properly */
import Page from "./page";

class PageFooterRenderingError extends Error {
  constructor(pageInstance: Page) {
    const msg = `Page component’s onRenderFooter method not called by child component: ${
      pageInstance.component ? pageInstance.component.constructor.name : "(no child component)"
      }`;
    super(msg);
  }
}

export default PageFooterRenderingError;

