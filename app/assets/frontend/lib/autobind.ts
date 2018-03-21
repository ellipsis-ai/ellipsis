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

const Autobind = {
    bindInstanceMethods: function(instance: object) {
      Object.getOwnPropertyNames(Object.getPrototypeOf(instance)).forEach((propName) => {
        if (typeof instance[propName] === "function" && !blacklist.includes(propName)) {
          instance[propName] = instance[propName].bind(instance);
        }
      });
    }
};

export default Autobind.bindInstanceMethods;
