function ellipsisRequire(module) {
  return require(module.replace(/(@?.+?)@.+$/, "$1"));
}
