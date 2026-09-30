import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { ReadRuntimeProvider } from "./runtime/ReadRuntimeContext";

const root = document.getElementById("root");

if (!root) {
  throw new Error("Floently Read root element is missing.");
}

createRoot(root).render(
  <StrictMode>
    <ReadRuntimeProvider>
      <App />
    </ReadRuntimeProvider>
  </StrictMode>,
);
