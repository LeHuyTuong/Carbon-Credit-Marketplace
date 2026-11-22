interface JwtClaims {
  sub?: string
  userId?: string | number
  email?: string
  name?: string
  fullName?: string
  roles?: string[]
  role?: string
}

function decodeBase64Url(part: string): string {
  const normalized = part.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  return atob(padded)
}

/** Giải mã payload JWT chỉ để hiển thị thông tin; việc tin cậy claims thuộc về server. */
export function decodeJwt(token: string): JwtClaims {
  try {
    const payload = token.split('.')[1]
    if (!payload) return {}
    const parsed: unknown = JSON.parse(decodeBase64Url(payload))
    if (parsed && typeof parsed === 'object') return parsed as JwtClaims
    return {}
  } catch {
    return {}
  }
}
