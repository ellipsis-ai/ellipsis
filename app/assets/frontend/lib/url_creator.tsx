function forSlack(slackTeamId?: string): string {
  return slackTeamId ? `slack://open?team=${slackTeamId}` : `slack://open`;
}

const URLCreator = {
  forSlack: forSlack
};

export default URLCreator;
