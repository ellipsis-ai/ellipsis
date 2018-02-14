// @flow
(function() {

  class HeaderHandler {
    header: ?HTMLElement;
    timerId: ?number;

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
      if (document.body && this.header) {
        const body = document.body;
        const header = this.header;
        this.timerId = window.setTimeout(function() {
          body.style.paddingTop = header.offsetHeight + 'px';
        }, 50);
      }
    }
  }

  class MenuHandler {
    userMenuButton: ?HTMLElement;
    userMenu: ?HTMLElement;
    navMenuButton: ?HTMLElement;
    navMenu: ?HTMLElement;
    navBar: ?HTMLElement;
    activeMenu: ?HTMLElement;
    activeButton: ?HTMLElement;

    constructor() {
      this.userMenuButton = document.getElementById('main-user-menu-button');
      this.userMenu = document.getElementById('main-user-menu');
      this.navMenuButton = document.getElementById('main-nav-button');
      this.navMenu = document.getElementById('main-nav-menu');
      this.navBar = document.getElementById('main-header');
      if (this.userMenuButton && this.userMenu) {
        this.addEventListeners(this.userMenu, this.userMenuButton);
      }
      if (this.navMenuButton && this.navMenu) {
        this.addEventListeners(this.navMenu, this.navMenuButton);
      }
    }

    addEventListeners(menu: HTMLElement, button: HTMLElement) {
      button.addEventListener('click', this.onMenuClick.bind(this, menu, button));
      document.addEventListener('click', this.onDocumentClick.bind(this, menu, button));
    }

    addClass(el: HTMLElement, className: string) {
      var classes = el.className.replace(/\s+/g, ' ').split(' ');
      if (!classes.find(function(cl) {
          return cl.trim() === className;
        })) {
        classes.push(className);
        el.className = classes.join(' ');
      }
    }

    removeClass(el: HTMLElement, className: string) {
      var classes = el.className.replace(/\s+/g, ' ').split(' ');
      var newClasses = classes.filter(function(cl) {
        return cl !== className;
      });
      el.className = newClasses.join(' ');
    }

    onMenuClick(menu: HTMLElement, button: HTMLElement, event: Event) {
      if (this.menuVisible(menu)) {
        this.hideMenu(menu, button);
        this.activeButton = null;
        this.activeMenu = null;
      } else {
        if (this.activeButton && this.activeMenu) {
          this.hideMenu(this.activeMenu, this.activeButton);
        }
        this.showMenu(menu, button);
        this.activeButton = button;
        this.activeMenu = menu;
      }
      event.stopPropagation();
      event.preventDefault();
    }

    hideMenu(menu: HTMLElement, button: HTMLElement) {
      this.addClass(menu, 'display-none');
      this.removeClass(button, 'button-dropdown-trigger-menu-open');
      if (this.navBar) {
        const bar = this.navBar;
        this.removeClass(bar, 'position-z-front');
        this.addClass(bar, 'position-z-behind-scrim');
      }
    }

    showMenu(menu, button) {
      this.removeClass(menu, 'display-none');
      this.addClass(button, 'button-dropdown-trigger-menu-open');
      if (this.navBar) {
        const bar = this.navBar;
        this.removeClass(bar, 'position-z-behind-scrim');
        this.addClass(bar, 'position-z-front');
      }
    }

    menuVisible(menu: HTMLElement) {
      return menu.clientHeight > 0;
    }

    onDocumentClick(menu: HTMLElement, button: HTMLElement) {
      if (this.menuVisible(menu)) {
        this.hideMenu(menu, button);
      }
    }
  }

  /* Enables touchscreen active pseudo-class support */
  if (document.body) {
    document.body.addEventListener('touchstart', function() { return null; });
  }

  new HeaderHandler();
  new MenuHandler();

})();
