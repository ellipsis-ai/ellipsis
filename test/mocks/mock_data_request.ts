
class ResponseError extends Error {}

const DataRequest = {
  jsonGet: jest.fn(() => {
    return new Promise(() => {
      return true;
    });
  }),
  jsonPost: jest.fn(() => {
    return new Promise(() => {
      return true;
    });
  })
};

export {DataRequest, ResponseError}
