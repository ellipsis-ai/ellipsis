import {PageRequiredProps} from "../../app/assets/frontend/shared_ui/page";

const renderPlaceholder = (ea: any) => ea;
function placeholderCallback() {
  void(0);
}

function getPageRequiredProps(): PageRequiredProps {
  return {
    activePanelName: "",
    activePanelIsModal: false,
    onToggleActivePanel: placeholderCallback,
    onClearActivePanel: placeholderCallback,
    onRenderFooter: renderPlaceholder,
    onRenderHeader: renderPlaceholder,
    onRenderPanel: renderPlaceholder,
    onRenderNavItems: () => null,
    onRenderNavActions: renderPlaceholder,
    onRevealedPanel: placeholderCallback,
    headerHeight: 0,
    footerHeight: 0,
    isMobile: false
  };
};

export {getPageRequiredProps};
