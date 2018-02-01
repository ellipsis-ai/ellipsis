/* global ColorsConfig */
import 'core-js';
import 'whatwg-fetch';
import React from 'react';
import ReactDOM from 'react-dom';
import Colors from './index';

ReactDOM.render(
  React.createElement(Colors, ColorsConfig),
  document.getElementById(ColorsConfig.containerId)
);
