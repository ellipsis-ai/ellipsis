interface ElementsMap {
  'no-agreement-container': Option<HTMLDivElement>
  'agreement-container': Option<HTMLDivElement>
  'no-agreement-button': Option<HTMLButtonElement>
  'agreement-checkbox-label': Option<HTMLLabelElement>
  'agreement-checkbox': Option<HTMLInputElement>
  'reminder': Option<HTMLElement>
}

(function() {

  const els = setupEls();

  function setupEls(): ElementsMap {
    const els: ElementsMap = {
      'no-agreement-container': document.querySelector<HTMLDivElement>('div#no-agreement-container'),
      'agreement-container': document.querySelector<HTMLDivElement>('div#agreement-container'),
      'no-agreement-button': document.querySelector<HTMLButtonElement>('button#no-agreement-button'),
      'agreement-checkbox-label': document.querySelector<HTMLLabelElement>('label#agreement-checkbox-label'),
      'agreement-checkbox': document.querySelector<HTMLInputElement>('input#agreement-checkbox'),
      'reminder': document.querySelector<HTMLElement>('#reminder')
    };

    if (els['no-agreement-container']) {
      els['no-agreement-container'].addEventListener('click', onNoAgreementButtonClick);
    }

    if (els['agreement-checkbox-label']) {
      els['agreement-checkbox-label'].addEventListener('click', agreementClick);
    }

    return els;
  }

  function onNoAgreementButtonClick() {
    if (els['agreement-checkbox']) {
      els['agreement-checkbox'].focus();
    }
    addClass(els['reminder'], "blink-twice-inverted");
    setTimeout(() => {
      removeClass(els['reminder'], "blink-twice-inverted");
    }, 1000);
  }

  function agreementClick() {
    if (els['agreement-checkbox'] && els['agreement-checkbox'].checked) {
      addClass(els['agreement-checkbox-label'], "checkbox-button-checked");
      addClass(els['no-agreement-container'], "display-none");
      removeClass(els['agreement-container'], "display-none");
    } else {
      removeClass(els['agreement-checkbox-label'], "checkbox-button-checked");
      addClass(els['agreement-container'], "display-none");
      removeClass(els['no-agreement-container'], "display-none");
    }
  }

  function addClass(element: Option<HTMLElement>, className: string): void {
    if (element) {
      const classes = element.className.split(" ");
      if (!classes.some((ea) => ea === className)) {
        element.className = classes.concat(className).join(" ");
      }
    }
  }

  function removeClass(element: Option<HTMLElement>, className: string): void {
    if (element) {
      const classes = element.className.split(" ");
      if (classes.some((ea) => ea === className)) {
        element.className = classes.filter((ea) => ea !== className).join(" ");
      }
    }
  }

})();
