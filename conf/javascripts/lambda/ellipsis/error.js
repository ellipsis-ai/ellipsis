class EllipsisError extends Error {
  constructor(systemMessage, options) {
    super(systemMessage);
    this.name = "EllipsisError";
    this.userMessage = options && options.userMessage ? options.userMessage : "";
  }
  toJson() { // Include inherited properties
    const json = {};
    for (var k in this) {
      if (typeof this[k] !== 'function') {
        json[k] = this[k];
      }
    }
    return json;
  }
}
