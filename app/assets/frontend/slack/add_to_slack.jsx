(function() {

  const elementNames = ['no-agreement-container', 'agreement-container', 'no-agreement-button', 'agreement-checkbox-label', 'agreement-checkbox', 'reminder'];
  const els = {};
  setup();

  function setup() {
    elementNames.forEach((ea) => {
      els[ea] = document.getElementById(ea);
    });

    els['no-agreement-container'].addEventListener('click', onNoAgreementButtonClick);
    els['agreement-checkbox-label'].addEventListener('click', agreementClick);
  }

  function onNoAgreementButtonClick() {
    els['agreement-checkbox'].focus();
    addClass(els['reminder'], "blink-twice-inverted");
    setTimeout(() => {
      removeClass(els['reminder'], "blink-twice-inverted");
    }, 1000);
  }

  function agreementClick() {
    if (els['agreement-checkbox'].checked) {
      addClass(els['agreement-checkbox-label'], "checkbox-button-checked");
      addClass(els['no-agreement-container'], "display-none");
      removeClass(els['agreement-container'], "display-none");
    } else {
      removeClass(els['agreement-checkbox-label'], "checkbox-button-checked");
      addClass(els['agreement-container'], "display-none");
      removeClass(els['no-agreement-container'], "display-none");
    }
  }

  function addClass(element, className) {
    const classes = element.className.split(" ");
    if (!classes.some((ea) => ea === className)) {
      element.className = classes.concat(className).join(" ");
    }
  }

  function removeClass(element, className) {
    const classes = element.className.split(" ");
    if (classes.some((ea) => ea === className)) {
      element.className = classes.filter((ea) => ea !== className).join(" ");
    }
  }

})();
