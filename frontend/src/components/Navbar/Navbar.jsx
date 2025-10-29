import React from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext.jsx";
import logo from "../../assets/logo.png";

export default function Navbar() {
  const { pathname } = useLocation();
  const { isAuthenticated, user, logout } = useAuth();

  //active state cho EV
  const isEV =
    pathname.startsWith("/ev") || pathname.startsWith("/managevehicle");
  const isCredits = pathname.startsWith("/credits");
  const linkCls = ({ isActive }) =>
    `nav-link px-3 ${isActive ? "is-active" : ""}`;
  const ddItemCls = ({ isActive }) =>
    `dropdown-item ${isActive ? "active" : ""}`;

  return (
    <nav className="navbar navbar-expand-lg fixed-top bg-dark bg-opacity-25 navbar-dark">
      <div className="container-xxl">
        {/*brand */}
        <Link className="navbar-brand d-flex align-items-center gap-2" to="/">
          <img src={logo} alt="Logo" height="40" />
          <span className="fw-semibold">CarbonX</span>
        </Link>

        {/*toggler (mobile) */}
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#mainNav"
          aria-controls="mainNav"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon" />
        </button>

        {/*collapsible content */}
        <div className="collapse navbar-collapse" id="mainNav">
          {/* left group */}
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            {/*COMPANY NAV*/}
            {user?.role === "COMPANY" && (
              <>
                <li className="nav-item">
                  <NavLink end to="/home" className={linkCls}>
                    HOME
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/marketplace" className={linkCls}>
                    MARKETPLACE
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/upload-report" className={linkCls}>
                    REPORT
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/wallet" className={linkCls}>
                    E-WALLET
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/retire" className={linkCls}>
                    RETIRE
                  </NavLink>
                </li>
              </>
            )}

            {/*EV OWNER NAV*/}
            {user?.role === "EV_OWNER" && (
              <>
                <li className="nav-item">
                  <NavLink end to="/home" className={linkCls}>
                    HOME
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/managevehicle" className={linkCls}>
                    VEHICLES
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink to="/wallet" className={linkCls}>
                    E-WALLET
                  </NavLink>
                </li>
              </>
            )}
          </ul>

          {/*right group */}
          <ul className="navbar-nav ms-auto mb-2 mb-lg-0 align-items-center gap-2 gap-lg-3">
            {!isAuthenticated ? (
              <>
                <li className="nav-item">
                  <NavLink to="/login" className={linkCls}>
                    LOGIN
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink
                    to="/register"
                    className="btn btn-outline-light ms-lg-2"
                  >
                    SIGN UP
                  </NavLink>
                </li>
              </>
            ) : (
              <>
                {/*btn bell */}
                <li className="nav-item dropdown">
                  <a
                    href="#"
                    className="nav-link p-0 dropdown-toggle no-caret"
                    id="notifDropdown"
                    role="button"
                    data-bs-toggle="dropdown"
                    aria-expanded="false"
                    aria-label="Notifications"
                  >
                    <span className="icon-btn">
                      <i className="bi bi-bell"></i>
                    </span>
                  </a>
                  <ul
                    className="dropdown-menu dropdown-menu-end"
                    aria-labelledby="notifDropdown"
                  >
                    <li>
                      <span className="dropdown-item-text">
                        No new notifications
                      </span>
                    </li>
                  </ul>
                </li>

                <li className="nav-item dropdown">
                  <a
                    href="#"
                    className="nav-link dropdown-toggle no-caret p-0"
                    id="userDropdown"
                    role="button"
                    data-bs-toggle="dropdown"
                    aria-expanded="false"
                  >
                    <span className="icon-btn">
                      <i className="bi bi-person"></i>
                    </span>
                  </a>

                  <ul
                    className="dropdown-menu dropdown-menu-end"
                    aria-labelledby="userDropdown"
                    data-bs-auto-close="true"
                  >
                    {/* Company role */}
                    {user?.role === "COMPANY" ? (
                      <>
                        <li>
                          <NavLink
                            to="/profile-company"
                            className="dropdown-item"
                          >
                            Profile
                          </NavLink>
                        </li>
                        <li>
                          <NavLink to="/list-credits" className="dropdown-item">
                            Manage Credits
                          </NavLink>
                        </li>
                        <li>
                          <NavLink
                            to="/list-projects"
                            className="dropdown-item"
                          >
                            Manage Projects
                          </NavLink>
                        </li>
                        <li>
                          <NavLink
                            to="/purchase-history"
                            className="dropdown-item"
                          >
                            Orders
                          </NavLink>
                        </li>
                        <li>
                          <NavLink
                            to="/change-password"
                            className="dropdown-item"
                          >
                            Setting
                          </NavLink>
                        </li>
                        <li>
                          <hr className="dropdown-divider" />
                        </li>
                        <li>
                          <button className="dropdown-item" onClick={logout}>
                            Logout
                          </button>
                        </li>
                      </>
                    ) : (
                      /* EV owner */
                      <>
                        <li>
                          <NavLink to="/profile" className="dropdown-item">
                            Profile
                          </NavLink>
                        </li>
                        <li>
                          <NavLink
                            to="/change-password"
                            className="dropdown-item"
                          >
                            Setting
                          </NavLink>
                        </li>
                        <li>
                          <hr className="dropdown-divider" />
                        </li>
                        <li>
                          <button className="dropdown-item" onClick={logout}>
                            Logout
                          </button>
                        </li>
                      </>
                    )}
                  </ul>
                </li>
              </>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}
