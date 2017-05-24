(function() {

  class HeaderHandler {
    constructor() {
      this.header = document.getElementById('main-header');
      this.timerId = null;
      window.addEventListener('resize', this.adjustPadding.bind(this));
      this.adjustPadding();
    }

    adjustPadding() {
      if (this.timerId) {
        window.clearTimeout(this.timerId);
      }
      this.timerId = window.setTimeout(function() {
        document.body.style.paddingTop = this.header.offsetHeight + 'px';
      }.bind(this), 50);
    }
  }

  class MenuHandler {
    constructor() {
      this.userMenuButton = document.getElementById('main-user-menu-button');
      this.userMenu = document.getElementById('main-user-menu');
      this.navBar = document.getElementById('main-header');
      if (this.userMenuButton && this.userMenu) {
        this.userMenuButton.addEventListener('click', this.onUserMenuClick.bind(this));
        document.addEventListener('click', this.onDocumentClick.bind(this));
      }
    }

    addClass(el, className) {
      var classes = el.className.replace(/\s+/g, ' ').split(' ');
      if (!classes.find(function(cl) {
          return cl.trim() === className;
        })) {
        classes.push(className);
        el.className = classes.join(' ');
      }
    }

    removeClass(el, className) {
      var classes = el.className.replace(/\s+/g, ' ').split(' ');
      var newClasses = classes.filter(function(cl) {
        return cl !== className;
      });
      el.className = newClasses.join(' ');
    }

    onUserMenuClick(event) {
      if (this.userMenuVisible()) {
        this.hideUserMenu();
      } else {
        this.showUserMenu();
      }
      event.stopPropagation();
      event.preventDefault();
    }

    hideUserMenu() {
      this.addClass(this.userMenu, 'display-none');
      this.removeClass(this.userMenuButton, 'button-dropdown-trigger-menu-open');
      this.removeClass(this.navBar, 'position-z-front');
      this.addClass(this.navBar, 'position-z-behind-scrim');
    }

    showUserMenu() {
      this.removeClass(this.userMenu, 'display-none');
      this.addClass(this.userMenuButton, 'button-dropdown-trigger-menu-open');
      this.removeClass(this.navBar, 'position-z-behind-scrim');
      this.addClass(this.navBar, 'position-z-front');
    }

    userMenuVisible() {
      return this.userMenu.clientHeight > 0;
    }

    onDocumentClick() {
      if (this.userMenuVisible()) {
        this.hideUserMenu();
      }
    }
  }

  /* Enables touchscreen active pseudo-class support */
  document.body.addEventListener('touchstart', function() { return null; });

  new HeaderHandler();
  new MenuHandler();

})();
