/// <reference types="vite/client" />

declare module 'virtual:vite-pwa/register' {
  export function registerSW(options?: { immediate?: boolean }): Promise<void>
}

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
