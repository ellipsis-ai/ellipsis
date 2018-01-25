// @flow
define(function() {

  function forSlack(slackTeamId?: string): string {
    return slackTeamId ? `slack://open?team=${slackTeamId}` : `slack://open`;
  }

  return {
    forSlack: forSlack
  };
});
