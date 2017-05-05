(function() {

  class HeaderHandler {
    constructor() {
      this.header = document.getElementById('main-header');
      this.timerId = null;
      window.addEventListener('resize', this.adjustPadding.bind(this));
    }

    adjustPadding() {
      if (this.timerId) {
        window.clearTimeout(this.timerId);
      }
      this.timerId = window.setTimeout(function() {
        document.body.style.paddingTop = this.header.offsetHeight + 'px';
      }, 50);
    }
  }

  class MenuHandler {
    constructor() {

    }

  }

    var userMenuButton = document.getElementById('main-user-menu-button');
    var userMenu = document.getElementById('main-user-menu');
    var navBar = document.getElementById('main-header');
    var addClass = function(el, className) {
      var classes = el.className.replace(/\s+/g, ' ').split(' ');
      if (!classes.find(function(cl) { return cl.trim() === className; })) {
        classes.push(className);
        el.className = classes.join(' ');
      }
    };
    var removeClass = function(el, className) {
      var classes = el.className.replace(/\s+/g, ' ').split(' ');
      var newClasses = classes.filter(function(cl) { return cl !== className; });
      el.className = newClasses.join(' ');
    };
    userMenuButton.addEventListener('click', function(event) {
      var oldStyle = userMenu.style.display;
      userMenu.style.display = oldStyle === 'none' ? '' : 'none';
      if (oldStyle === 'none') {
        addClass(userMenuButton, 'button-dropdown-trigger-menu-open');
        removeClass(navBar, 'position-z-behind-scrim');
        addClass(navBar, 'position-z-front');
      } else {
        removeClass(userMenuButton, 'button-dropdown-trigger-menu-open');
        removeClass(navBar, 'position-z-front');
        addClass(navBar, 'position-z-behind-scrim');
      }
      event.stopPropagation();
      event.preventDefault();
    });
    document.addEventListener('click', function() {
      if (userMenu.style.display !== 'none') {
        userMenu.style.display = 'none';
        removeClass(userMenuButton, 'button-dropdown-trigger-menu-open');
        removeClass(navBar, 'position-z-front');
        addClass(navBar, 'position-z-almost-front');
      }
    });

  /* Enables touchscreen active pseudo-class support */
  document.body.addEventListener('touchstart', function() { return null; });

  const hh = new HeaderHandler();
  hh.adjustPadding();

})();
