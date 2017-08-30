class EllipsisError extends Error {
  constructor(systemMessage, options) {
    super(systemMessage);
    this.userMessage = options && options.userMessage ? options.userMessage : null;
  }
}
