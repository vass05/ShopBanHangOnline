import type { ReactNode } from "react";
import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-3 sm:px-8">
          <Link to="/" className="flex items-center gap-2 text-xl font-bold tracking-tight">
            <img src="/images/logo.png" alt="" width={36} height={36} className="rounded-lg" />
            <span>Heli<span className="text-sky-700">Shop</span></span>
          </Link>
          <Link to="/" className="flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-sky-700">
            <ArrowLeft size={16} aria-hidden="true" /> Trang chủ
          </Link>
        </div>
      </header>
      <main className="mx-auto grid max-w-5xl items-center gap-12 px-4 py-8 sm:px-8 sm:py-14 md:grid-cols-[1fr_minmax(0,440px)]">
        <div className="hidden min-h-[26rem] flex-col items-start justify-end overflow-hidden rounded-3xl bg-sky-950 p-9 text-white md:flex">
          <img src="/images/logo.png" alt="" width={86} height={86} className="mb-8 rounded-2xl bg-white p-1" />
          <p className="max-w-[15ch] text-4xl font-semibold leading-tight tracking-tight">Mua sắm và mở gian hàng trên HeliShop</p>
        </div>
        <section aria-labelledby="auth-title" className="mx-auto w-full max-w-[440px] rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8">
          <h1 id="auth-title" className="mb-6 text-2xl font-semibold tracking-tight">{title}</h1>
          {children}
        </section>
      </main>
    </div>
  );
}

export function AuthDivider() {
  return (
    <div className="flex items-center gap-3 py-5 text-xs text-slate-500">
      <span className="h-px flex-1 bg-slate-200" aria-hidden="true" />
      Hoặc
      <span className="h-px flex-1 bg-slate-200" aria-hidden="true" />
    </div>
  );
}
