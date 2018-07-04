class EllipsisError extends Error {
  constructor(messageOrError, options) {
    if (messageOrError instanceof Error) {
      super(messageOrError.message);
      this.name = messageOrError.name;
      this.stack = messageOrError.stack;
    } else {
      super(messageOrError);
    }
    this.userMessage = options && options.userMessage ? options.userMessage : null;
  }
}
