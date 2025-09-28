import React from 'react';
import { Link, NavLink } from 'react-router-dom';
import logo from '../../assets/logo.png'; 

export default function Navbar() {
  return (
    <nav className="navbar navbar-expand-lg fixed-top bg-dark bg-opacity-25 navbar-dark">
      <div className="container-xxl">
        {/* Brand */}
        <Link className="navbar-brand d-flex align-items-center gap-2" to="/">
          <img src={logo} alt="Logo" height="40" />
          <span className="fw-semibold">CarbonX</span>
        </Link>

        {/*toggler(mobile) */}
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
          {/*left group */}
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            <li className="nav-item">
              <NavLink end to="/" className="nav-link">HOME</NavLink>
            </li>
            <li className="nav-item">
              <NavLink to="/exchange" className="nav-link">MARKETPLACE</NavLink>
            </li>
            <li className="nav-item">
              <NavLink to="/reports" className="nav-link">REPORT</NavLink>
            </li>

            {/*vehicle*/}
            <li className="nav-item dropdown">
              <a
                href="#"
                className="nav-link dropdown-toggle"
                id="evDropdown"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                ELECTRIC VEHICLE
              </a>
              <ul className="dropdown-menu" aria-labelledby="evDropdown">
                <li><Link className="dropdown-item" to="/ev/manage">Manage vehicle</Link></li>
                <li><Link className="dropdown-item" to="/ev/transactions">Personal transaction</Link></li>
              </ul>
            </li>

            <li className="nav-item">
              <NavLink to="/wallet" className="nav-link">E-WALLET</NavLink>
            </li>

            {/*credit*/}
            <li className="nav-item dropdown">
              <a
                href="#"
                className="nav-link dropdown-toggle"
                id="creditsDropdown"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                CREDITS
              </a>
              <ul className="dropdown-menu" aria-labelledby="creditsDropdown">
                <li><Link className="dropdown-item" to="/credits/list">Credit list</Link></li>
              </ul>
            </li>
          </ul>

          {/*right group*/}
          <ul className="navbar-nav ms-auto mb-2 mb-lg-0">
            <li className="nav-item">
              <NavLink to="/login" className="nav-link">LOGIN</NavLink>
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
