const mockFetch = jest.fn(() => {
  return new Promise(() => { return true; });
});

export default mockFetch;
