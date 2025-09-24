import React, { useRef, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'

// hiệu ứng ripple
function useRipple() {
  const add = (e, host) => {
    if (!host) return
    const rect = host.getBoundingClientRect()
    const size = Math.max(rect.width, rect.height)
    const x = (e.clientX ?? rect.left + rect.width/2) - rect.left - size/2
    const y = (e.clientY ?? rect.top + rect.height/2) - rect.top - size/2
    const ripple = document.createElement('div')
    ripple.className = 'ripple'
    ripple.style.width = ripple.style.height = size + 'px'
    ripple.style.left = x + 'px'
    ripple.style.top  = y + 'px'
    host.appendChild(ripple)
    setTimeout(() => ripple.remove(), 600)
  }
  return add
}

export default function Login(){
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(false)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  const [showPwd, setShowPwd] = useState(false)

  const ripple = useRipple()
  const btnRippleRef = useRef(null)

  const validate = () => {
    const e = {}
    if (!email.trim()) e.email = 'Email is required'
    else if (!/^\S+@\S+\.\S+$/.test(email)) e.email = 'Enter a valid email'
    if (!password) e.password = 'Password is required'
    else if (password.length < 6) e.password = 'Min 6 characters'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const submit = async (ev) => {
    ev.preventDefault()
    if (!validate()) return
    setLoading(true)
    await new Promise(r => setTimeout(r, 1000))
    setLoading(false)
    setSuccess(true)
    setTimeout(() => nav('/'), 1200)
  }

  return (
    <div style={{
      backgroundImage:'url(/bg-green.jpg)',
      backgroundSize:'cover',
      minHeight:'100vh', 
      display:'flex', 
      alignItems:'center',
      justifyContent:'center', 
      paddingTop:60
    }}>
      <div className="login-container">
        <div className="login-card">
          <h1>Đăng nhập</h1>

          {!success ? (
          <form onSubmit={submit} noValidate>
            <div className={'form-group' + (errors.email ? ' error' : '')}>
              <input
                type="email" required value={email}
                onChange={e=>setEmail(e.target.value)}
                onBlur={validate}
                placeholder="Email"
              />
              {errors.email && <span className="error-message">{errors.email}</span>}
            </div>

            <div className={'form-group' + (errors.password ? ' error' : '')}>
              <input
                type={showPwd ? 'text' : 'password'}
                required value={password}
                onChange={e=>setPassword(e.target.value)}
                onBlur={validate}
                placeholder="Mật khẩu"
              />
              {errors.password && <span className="error-message">{errors.password}</span>}
            </div>

            <label>
              <input type="checkbox" checked={remember}
                onChange={e=>setRemember(e.target.checked)}/>
              Nhớ phiên đăng nhập
            </label>

            <Link to="" style={{marginLeft: '30px'}}>Quên mật khẩu?</Link>
            
            <button type="submit" disabled={loading}
              className={'login-btn' + (loading ? ' loading' : '')}
              onClick={(e)=>ripple(e, btnRippleRef.current)}
            >
              <div ref={btnRippleRef}></div>
              {loading ? 'Loading...' : 'Đăng nhập'}
            </button>
          </form>
          ) : (
            <div className="success-message">
              <h3>Welcome back!</h3>
              <p>Signing you in...</p>
            </div>
          )}

          <div className="divider"><span>or</span></div>
          <button type="button" className="social-btn">Continue with Google</button>
          <p style={{marginLeft: '50px'}}>Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p>
        </div>
      </div>
    </div>
  )
}
