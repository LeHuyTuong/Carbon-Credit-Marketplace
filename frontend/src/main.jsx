import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom'
import Home from './pages/Home/Home.jsx'
import Login from './pages/Login/Login.jsx'
import Register from './pages/Register/Register.jsx'
import ForgotPassword from './pages/ForgotPassword/ForgotPassword.jsx'
import Navbar from './components/Navbar/Navbar.jsx'
import Marketplace from './pages/Market/Marketplace.jsx'
import OTP from './pages/OTP/OTP.jsx'
import Privacy from './pages/Privacy/Privacy.jsx'
import ManageVehicle from './pages/Dashboard/EVOwner/ManageVehicle.jsx'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import './styles/style.css'
import './components/Navbar/navbar.css'
import './components/Hero/hero.css'

function Layout() {
  return (
    <>
      <Navbar />
      <Outlet />
    </>
  )
}
function Root() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/home" replace />} />

        {/*route có navbar */}
        <Route element={<Layout />}>
          <Route path="/home" element={<Home />} />
          <Route path="/marketplace" element={<Marketplace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/otp" element={<OTP />} />
          <Route path="/managevehicle" element={<ManageVehicle />} />
          <Route path="/forgot" element={<ForgotPassword />} />
        </Route>

        {/*route không có navbar */}
        <Route path="/privacy" element={<Privacy />} />

        {/*fallback*/}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
      <Root />
  </React.StrictMode>
)
