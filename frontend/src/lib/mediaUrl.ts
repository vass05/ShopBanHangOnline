/** Uploaded media follows the configured API origin; the proxy uses relative URLs. */
export function mediaUrl(url?: string, apiBase = import.meta.env.VITE_API_BASE_URL || "/api/v1"): string {
  if (!url) return "";
  if (url.startsWith("/uploads/") && /^https?:\/\//i.test(apiBase)) {
    return new URL(url, apiBase).href;
  }
  return url;
}
