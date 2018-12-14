declare module "javascript-debounce" {
  const debounce: <T extends (...args: Array<any>) => any>(f: T, timeout: number) => T;
  export = debounce;
}

declare module "monaco-editor/esm/vs/language/typescript/lib/lib" {
  export const lib_es5_dts: string;
  export const lib_es2015_dts: string;
}

/*
 The @types/uuid package adds node types as a dependency which we do not want,
 so here's a simple replacement:
 */
declare module "uuid" {
  function v4(options?: {
    random?: Array<number>,
    rng?: () => Array<number>
  }, buffer?: Uint8Array, offset?: number): string
}
