class EllipsisError extends Error {
  constructor(messageOrError, options) {
    const inspect = require("util").inspect;
    if (messageOrError instanceof Error) {
      super(messageOrError.message);
      this.name = messageOrError.name;
      this.stack = messageOrError.stack;
    } else if (typeof messageOrError === "object") {
      super(inspect(messageOrError, { depth: 3 }));
    } else if (messageOrError) {
      super(messageOrError);
    } else {
      super("Unknown error");
    }
    this.userMessage = options && options.userMessage ? options.userMessage : null;
  }
}
