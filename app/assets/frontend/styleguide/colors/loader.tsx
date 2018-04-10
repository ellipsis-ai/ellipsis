declare var ColorsConfig: {
  containerId: string
};

import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Colors from './index';

const container = document.getElementById(ColorsConfig.containerId);
if (container) {
  ReactDOM.render(
    React.createElement(Colors, ColorsConfig),
    container
  );
}
