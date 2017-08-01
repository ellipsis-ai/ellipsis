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

  class Autobind {
    static bindInstanceMethods(instance) {
      Object.getOwnPropertyNames(Object.getPrototypeOf(instance)).forEach((propName) => {
        if (typeof instance[propName] === "function" && !blacklist.includes(propName)) {
          instance[propName] = instance[propName].bind(instance);
        }
      });
    }
  }

  return Autobind.bindInstanceMethods;
});
