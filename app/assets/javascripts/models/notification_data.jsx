define(function() {
  class NotificationData {
    constructor(props) {
      if (!props.kind) {
        throw new Error("Notification must include a kind property");
      }
      const initialProps = {
        kind: {
          value: props.kind,
          enumerable: true
        }
      };
      Object.keys(props).filter((ea) => ea !== "kind").forEach((propName) => {
        initialProps[propName] = {
          value: props[propName],
          enumerable: true
        };
      });
      Object.defineProperties(this, initialProps);
    }
  }

  return NotificationData;
});
