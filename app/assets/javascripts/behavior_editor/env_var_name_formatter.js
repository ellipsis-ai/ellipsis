define(function(require) {

  return function(name) {
    return name.toUpperCase().replace(/\s/g, '_').replace(/^\d|[^A-Z0-9_]/g, '');
  }

});
