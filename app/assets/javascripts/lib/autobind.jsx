define(function() {
  const blacklist = [
    'constructor',
    'componentDidMount',
    'componentDidUpdate',
    'componentWillMount',
    'componentWillReceiveProps',
    'componentWillUnmount',
    'componentWillUpdate',
    'render',
    'shouldComponentUpdate'
  ];

  return function(context) {
    Object.getOwnPropertyNames(Object.getPrototypeOf(context)).forEach((propName) => {
      if (typeof context[propName] === "function" && !blacklist.includes(propName)) {
        context[propName] = context[propName].bind(context);
      }
    });
  };
});
