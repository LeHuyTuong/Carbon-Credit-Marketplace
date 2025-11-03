import React from "react";
import { createRoot } from "react-dom/client";
import { AuthProvider } from "./context/AuthContext.jsx";
import App from "./App/App.jsx";
import "react-toastify/ReactToastify.css";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import "bootstrap-icons/font/bootstrap-icons.css";
import "./styles/style.css";
import "./components/Navbar/navbar.css";
import "./components/Hero/hero.css";

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
   <AuthProvider>
     <App />
    </AuthProvider>
 </React.StrictMode>
);
