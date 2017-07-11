module.exports = {
  getRandomValues: jest.fn((arr) => {
    arr.forEach((_, index) => {
      arr[index] = Math.floor(Math.random() * 0xFF);
    });
    return arr;
  })
};
