import React from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import logo from '../../assets/logo.png';

export default function Navbar() {
  const { pathname } = useLocation();
  const isEV = pathname.startsWith('/ev');         
  const isCredits = pathname.startsWith('/credits'); 
  const linkCls = ({ isActive }) => `nav-link px-3 ${isActive ? 'is-active' : ''}`;
  const ddItemCls = ({ isActive }) => `dropdown-item ${isActive ? 'active' : ''}`;

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
            <li className="nav-item">
              <NavLink end to="/home" className={linkCls}>HOME</NavLink>
            </li>
            <li className="nav-item">
              <NavLink to="/marketplace" className={linkCls}>MARKETPLACE</NavLink>
            </li>
            <li className="nav-item">
              <NavLink to="/reports" className={linkCls}>REPORT</NavLink>
            </li>

            {/*ev */}
            <li className="nav-item dropdown">
              <a
                href="#"
                className={`nav-link dropdown-toggle px-3 ${isEV ? 'is-active-parent' : ''}`}
                id="evDropdown"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                ELECTRIC VEHICLE
              </a>
              <ul className="dropdown-menu" aria-labelledby="evDropdown">
                <li><NavLink to="/ev/manage" className={ddItemCls}>Manage vehicle</NavLink></li>
                <li><NavLink to="/ev/transactions" className={ddItemCls}>Personal transaction</NavLink></li>
              </ul>
            </li>

            <li className="nav-item">
              <NavLink to="/wallet" className={linkCls}>E-WALLET</NavLink>
            </li>

            {/*credits */}
            <li className="nav-item dropdown">
              <a
                href="#"
                className={`nav-link dropdown-toggle px-3 ${isCredits ? 'is-active-parent' : ''}`}
                id="creditsDropdown"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                CREDITS
              </a>
              <ul className="dropdown-menu" aria-labelledby="creditsDropdown">
                <li><NavLink to="/credits/list" className={ddItemCls}>Credit list</NavLink></li>
              </ul>
            </li>
          </ul>

          {/*right group */}
          <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
            <li className="nav-item">
              <NavLink to="/login" className={linkCls}>LOGIN</NavLink>
            </li>
            <li className="nav-item">
              <NavLink to="/register" className="btn btn-outline-light ms-lg-2">SIGN UP</NavLink>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  );
}
