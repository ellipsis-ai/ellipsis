define(function() {

  class Magic8Ball {
    static possibleResponses() {
      return [
        "Reply hazy try again",
        "Ask again later",
        "Better not tell you now",
        "Cannot predict now",
        "Concentrate and ask again"
      ];
    }

    static response() {
      var responses = this.possibleResponses();
      var rand = Math.floor(Math.random() * responses.length);
      return `The magic 8-ball says:\n\n“${responses[rand]}”`;
    }
  }

  return Magic8Ball;
});
