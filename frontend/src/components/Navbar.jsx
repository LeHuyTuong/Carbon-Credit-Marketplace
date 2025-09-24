import { useState } from 'react'
import '../styles/navbar.css'
import { useNavigate } from 'react-router-dom';

function MenuItem({ label, to, children, highlight = false }) {
  const [open, setOpen] = useState(false)
  const hasSub = !!children
  const navigate = useNavigate()

  const go = () => { if (to && !hasSub) navigate(to) }

  return (
    <li
      className={`menu-item ${highlight ? 'highlight' : ''} ${hasSub ? 'has-sub' : ''}`}
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <span
        className="menu-label"
        role={to && !hasSub ? 'link' : (hasSub ? 'button' : undefined)}
        tabIndex={0}
        aria-haspopup={hasSub || undefined}
        aria-expanded={hasSub ? open : undefined}
        onClick={go}
        onKeyDown={(e) => {
          if ((e.key === 'Enter' || e.key === ' ') && to && !hasSub) navigate(to)
        }}
        style={{ cursor: (to && !hasSub) ? 'pointer' : undefined }}
      >
        {label} {hasSub && <span className="caret">▾</span>}
      </span>

      {hasSub && open && (
        <ul className="submenu" role="menu">
          {children}
        </ul>
      )}
    </li>
  )
}

function SubmenuItem({ to, children }) {
  const navigate = useNavigate()
  return (
    <li className="submenu-item">
      <span
        role="link"
        tabIndex={0}
        onClick={() => navigate(to)}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && navigate(to)}
        style={{ cursor: 'pointer', display: 'block' }}
      >
        {children}
      </span>
    </li>
  )
}

export default function NavbarList() {
  return (
    <nav
      style={{
        position: 'fixed', top: 0, left: 0, right: 0, zIndex: 10,
        padding: '8px 16px',
        background: 'rgba(0,0,0,0.20)'
      }}
      role="navigation"
      aria-label="Main"
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', maxWidth: 1280, margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <img src="/images/logo.png" height="80" alt="logo" />
          <ul className="menu">
            <MenuItem label="TRANG CHỦ" to="/" />
            <MenuItem label="SÀN GIAO DỊCH" to="/exchange" />
            <MenuItem label="BÁO CÁO" to="/reports" />

            <MenuItem label="XE ĐIỆN">
              <>
                <SubmenuItem to="/ev/manage">Quản lý xe</SubmenuItem>
                <SubmenuItem to="/ev/qr">Quét QR sạc</SubmenuItem>
                <SubmenuItem to="/ev/transactions">Giao dịch cá nhân</SubmenuItem>
              </>
            </MenuItem>

            <MenuItem label="VÍ ĐIỆN TỬ" to="/wallet" />

            <MenuItem label="TÍN CHỈ">
              <>
                <SubmenuItem to="/credits/list">Danh sách tín chỉ</SubmenuItem>
                <SubmenuItem to="/credits/retired">Đã retire</SubmenuItem>
              </>
            </MenuItem>
          </ul>
        </div>

        <ul className="menu">
          <MenuItem label="ĐĂNG NHẬP" to="/login" />
          <MenuItem label="ĐĂNG KÝ" to="/register" />
        </ul>
      </div>
    </nav>
  )
}
