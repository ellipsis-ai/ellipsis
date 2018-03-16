import {ResponseError} from '../../app/assets/frontend/lib/data_request';

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
