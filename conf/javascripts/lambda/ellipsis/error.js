class EllipsisError extends Error {
  constructor(systemMessage, options) {
    let errorMessage = "";
    let userMessage = options && options.userMessage ? options.userMessage : "";
    errorMessage += systemMessage;
    if (userMessage) {
      errorMessage += "\nELLIPSIS_USER_ERROR_MESSAGE_START\n" + userMessage + "\nELLIPSIS_USER_ERROR_MESSAGE_END";
    }
    super(errorMessage);
    this.systemMessage = systemMessage;
    this.userMessage = userMessage;
  }
}
