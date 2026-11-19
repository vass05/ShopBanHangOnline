export interface GoogleIdentityApi {
  initialize(options: { client_id: string; nonce: string; callback: (result: { credential: string }) => void; auto_select: boolean }): void;
  renderButton(element: HTMLElement, options: { type: string; theme: string; size: string; text: string; width: number; locale: string }): void;
  cancel(): void;
}
declare global { interface Window { google?: { accounts: { id: GoogleIdentityApi } } } }
let sdkPromise: Promise<GoogleIdentityApi> | null = null;

export function loadGoogleIdentity(): Promise<GoogleIdentityApi> {
  if (window.google?.accounts.id) return Promise.resolve(window.google.accounts.id);
  if (sdkPromise) return sdkPromise;
  sdkPromise = new Promise<GoogleIdentityApi>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    const fail = () => { clearTimeout(timer); script.remove(); sdkPromise = null; reject(new Error("Không tải được đăng nhập Google. Vui lòng thử lại.")); };
    const timer = setTimeout(fail, 12000);
    script.onerror = fail;
    script.onload = () => {
      clearTimeout(timer);
      const sdk = window.google?.accounts.id;
      if (sdk) resolve(sdk); else fail();
    };
    document.head.appendChild(script);
  });
  return sdkPromise;
}
