import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { canonicalizeCurrentReadLocation } from "./routing/navigation";

canonicalizeCurrentReadLocation();

const root = document.getElementById("root");

if (!root) {
  throw new Error("Floently Read root element is missing.");
}

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
