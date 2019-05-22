export interface KeyboardShortcutInterface {
  readonly command: boolean
  readonly shift: boolean
  readonly key: string
}

class KeyboardShortcut implements KeyboardShortcutInterface {
  readonly command: boolean;
  readonly shift: boolean;
  readonly key: string;

  constructor(props: KeyboardShortcutInterface) {
    this.command = props.command;
    this.shift = props.shift;
    this.key = props.key;
  }

  keyDescription(): string {
    if (navigator.platform.startsWith("Mac")) {
      return `${this.command ? "⌘" : ""}${this.shift ? "⇧" : ""}${this.key.toUpperCase()} `;
    } else if (navigator.platform.startsWith("Win")) {
      return `${this.command ? "⌃" : ""}${this.shift ? "⇧" : ""}${this.key.toUpperCase()} `;
    } else {
      return "";
    }
  }
}

export default KeyboardShortcut;
