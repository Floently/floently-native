import { useEffect } from "react";
import { AuthPage } from "./auth/AuthPage";
import { initializeAuth } from "./auth/authStore";
import { useAuthState } from "./auth/useAuthState";
import { LandingPage } from "./public/LandingPage";
import {
  loginPathForReturnTo,
  navigateTo,
  useBrowserLocation,
} from "./routing/navigation";
import { ProductApp } from "./shell/ProductApp";
import { startReadPreferencesEnvironment } from "./preferences/readPreferencesStore";
import "./styles.css";

function AppLoading({ label }: { label: string }) {
  return (
    <main className="boot-screen">
      <span className="brand-mark" aria-hidden="true">F</span>
      <p>{label}</p>
    </main>
  );
}

function ProtectedApp({
  pathname,
  href,
}: {
  pathname: string;
  href: string;
}) {
  const auth = useAuthState();

  useEffect(() => {
    if (auth.initialized && auth.status === "anonymous") {
      navigateTo(loginPathForReturnTo(href), true);
    }
  }, [auth.initialized, auth.status, href]);

  if (!auth.initialized || auth.status === "loading") {
    return <AppLoading label="Opening Read…" />;
  }

  if (!auth.session) {
    return <AppLoading label="Taking you to sign in…" />;
  }

  return (
    <ProductApp
      pathname={pathname}
      user={auth.session.user}
    />
  );
}

export default function App() {
  const location = useBrowserLocation();
  const auth = useAuthState();

  useEffect(() => {
    void initializeAuth();
    return startReadPreferencesEnvironment();
  }, []);

  if (
    location.pathname === "/login"
    || location.pathname === "/signin"
    || location.pathname === "/auth/login"
  ) {
    return <AuthPage mode="login" search={location.search} />;
  }

  if (
    location.pathname === "/signup"
    || location.pathname === "/register"
    || location.pathname === "/auth/signup"
  ) {
    return <AuthPage mode="signup" search={location.search} />;
  }

  if (
    location.pathname === "/"
    || location.pathname === "/read"
  ) {
    return <LandingPage session={auth.session} />;
  }

  if (
    location.pathname === "/app"
    || location.pathname.startsWith("/app/")
  ) {
    return (
      <ProtectedApp
        pathname={location.pathname}
        href={location.href}
      />
    );
  }

  return (
    <main className="public-shell public-not-found">
      <a
        href="/"
        className="brand-link"
        onClick={(event) => {
          event.preventDefault();
          navigateTo("/");
        }}
      >
        <span className="brand-mark" aria-hidden="true">F</span>
        <span>Floently Read</span>
      </a>

      <div>
        <p className="eyebrow">404</p>
        <h1>That page is not part of Read.</h1>
        <button
          type="button"
          className="hero-primary"
          onClick={() => navigateTo("/", true)}
        >
          Go home
        </button>
      </div>
    </main>
  );
}
