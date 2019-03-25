import Page from "./page";

// Hacky workaround because Typescript can't extend built-in types like Error
// https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
class PageFooterRenderingError implements Error {
  name: string;
  message: string;

  constructor(pageInstance: Page) {
    const msg = `Page component’s onRenderFooter method not called by child component: ${
      pageInstance.component ? pageInstance.component.constructor.name : "(no child component)"
      }`;
    this.name = "PageFooterRenderingError";
    this.message = msg;
    Error.call(this, msg);
  }
}

export default PageFooterRenderingError;
