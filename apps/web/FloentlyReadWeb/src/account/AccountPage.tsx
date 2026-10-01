import { logout } from "../auth/authStore";
import { useAuthState } from "../auth/useAuthState";
import { navigateTo } from "../routing/navigation";

export function AccountPage() {
  const auth = useAuthState();
  const user = auth.session?.user;

  if (!user) {
    return null;
  }

  async function signOut() {
    await logout();
    navigateTo("/", true);
  }

  return (
    <section className="product-page settings-page">
      <header className="product-page-header">
        <div>
          <p className="eyebrow">Floently account</p>
          <h1>Account</h1>
          <p>
            This rebuild uses the shared Floently account authority rather
            than a separate READ-only identity.
          </p>
        </div>
      </header>

      <div className="settings-grid">
        <article className="settings-card">
          <h2>Profile</h2>
          <dl className="account-details">
            <div>
              <dt>Email</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Name</dt>
              <dd>{user.name || "Not set"}</dd>
            </div>
            <div>
              <dt>Floently plan</dt>
              <dd>{user.plan}</dd>
            </div>
            <div>
              <dt>Read plan</dt>
              <dd>{user.readPlan || user.plan}</dd>
            </div>
          </dl>
        </article>

        <article className="settings-card danger-card">
          <h2>Session</h2>
          <p>
            Signing out clears this browser’s saved bearer fallback and asks
            the shared account service to end its session cookie.
          </p>
          <button
            type="button"
            className="card-secondary"
            onClick={() => void signOut()}
          >
            Sign out
          </button>
        </article>
      </div>
    </section>
  );
}
