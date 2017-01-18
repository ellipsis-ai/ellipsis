"use strict";

const emojiList = {
  happy: ["😀", "😃", "😄", "😁", "😊", "🙂", "☺️"],
  celebratory: ["👏", "👏👏👏", "👍", "🎺", "🎉", "🎊", "💃", "🕺", "💯", "🎈", "🍾"],
  congratulatory: ["👌", "👍", "💪", "😎", "👑", "🦄", "☃️", "🏅", "🥇", "🏆", "🎯", "⭐️"],
  disappointed: ["🙁", "😟"],
  oops: ["😬", "🙈", "🦑", "🦃", "💩", "👻", "👀", "🦆", "☔️", "🗿"],
  confused: ["🤔", "🐦", "🐴", "🤷‍♀️", "🤷‍♂️"],
  yes: ["👍", "✅", "✨", "🚀", "🆗"],
  no: ["👎", "🛑", "🙅‍♂️", "🙅", "🚧"],
  understood: ["👍", "👌", "✅", "🆗", "📝", "📌"],
  hello: ["👋", "🙂"],
  appreciated: ["👍", "🙂", "😊"],
  sarcastic: ["😬", "🙈", "🙃", "😛",
    "¯\\\\_(ツ)_/¯",
    "¯\\\\_🤖_/¯"
  ],
  misc: []
};

const responseList = {
  happy: ["Excellent", "Very good", "Oh, fantastic"],
  celebratory: ["Yay!", "Hurray!", "Yesssssssss"],
  congratulatory: ["Well done!", "Nice work!", "Oh, nicely done.", "Congrats!", "Mission accomplished."],
  disappointed: ["Oh dear", "Uh oh", "Ugh"],
  oops: ["Oh well.", "Don't worry about it.", "These things happen."],
  confused: ["Hmm", "Uh…", "Um…", "I’m confused"],
  yes: ["Yes!", "Absolutely", "For sure", "Definitely", "Do it", "That’s a great idea"],
  no: ["No", "No way", "Nope", "I don’t think so", "Forget it", "That doesn’t seem like a good idea"],
  understood: ["OK.", "Got it.", "Understood", "Makes sense."],
  hello: [
    "Well hello there.",
    "Hi",
    "Hi!",
    "Hello",
    "Greetings",
    "Howdy",
    "Hey there"
  ],
  appreciated: [
    "Oh, my pleasure.",
    "I do what I can.",
    "It was nothing, really.",
    "Don’t mention it.",
    "You’re welcome."
  ],
  sarcastic: [
    "I can’t tell if you’re being sincere, but… no worries.",
    "Welp",
    "_cough_",
    "Moving right along…"
  ],
  misc: []
};

const RandomResponse = {
  emoji: function(optionalTheme, repeatN) {
    let possible = [];
    if (optionalTheme && emojiList[optionalTheme]) {
      possible = emojiList[optionalTheme];
    } else {
      Object.keys(emojiList).forEach((theme) => {
        possible = possible.concat(emojiList[theme]);
      });
    }
    const repeatCount = repeatN || 1;
    let response = "";
    for (let i = 0; i < repeatCount; i++) {
      response += RandomResponse.fromList(possible);
    }
    return response;
  },

  emojiListFor: function(theme) {
    return emojiList[theme] || [];
  },

  responseListFor: function(theme) {
    return responseList[theme] || [];
  },

  response: function(optionalTheme) {
    let possible = [];
    if (optionalTheme && responseList[optionalTheme]) {
      possible = responseList[optionalTheme];
    } else {
      Object.keys(responseList).forEach((theme) => {
        possible = possible.concat(responseList[theme]);
      });
    }
    return RandomResponse.fromList(possible);
  },

  responseWithEmoji: function(optionalTheme) {
    return RandomResponse.emoji(optionalTheme) + " " + RandomResponse.response(optionalTheme);
  },

  fromList: function(list) {
    if (!list.length) {
      return "";
    }
    return list[Math.floor(Math.random() * list.length)];
  },

  addEmojis: function(emojis, optionalTheme) {
    const theme = optionalTheme || "misc";
    if (!emojiList[theme]) {
      emojiList[theme] = [];
    }
    emojiList[theme] = emojiList[theme].concat(emojis);
  },

  addResponses: function(responses, optionalTheme) {
    const theme = optionalTheme || "misc";
    if (!responseList[theme]) {
      responseList[theme] = [];
    }
    responseList[theme] = responseList[theme.concat(responses)];
  }
};

module.exports = RandomResponse;
