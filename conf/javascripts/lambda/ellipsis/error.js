class EllipsisError extends Error {
  constructor(messageOrError, options) {
    if (messageOrError instanceof Error) {
      super(messageOrError.message);
      Object.keys(messageOrError).map((key) => {
        this[key] = messageOrError[key];
      });
    } else {
      super(messageOrError);
    }
    this.userMessage = options && options.userMessage ? options.userMessage : null;
  }
}
