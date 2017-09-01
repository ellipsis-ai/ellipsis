function ellipsisCallback(err, result) {
  lambdaCallback(err, Object.assign(result, {
    logs: EllipsisConsole.logs
  }));
}
