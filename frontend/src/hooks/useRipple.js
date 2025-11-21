export default function useRipple() {
    // Hàm tạo hiệu ứng ripple khi click
  const add = (e, host) => {
    // Nếu không truyền host element thì không làm gì
    if (!host) return;
    // Lấy kích thước & vị trí của phần tử được click
    const rect = host.getBoundingClientRect();
    // Kích thước ripple là hình tròn lớn bằng cạnh lớn nhất (width/height)
    const size = Math.max(rect.width, rect.height);
    // Tính vị trí ripple theo tọa độ click (hoặc center nếu không có e.clientX/Y)
    const x = (e.clientX ?? rect.left + rect.width / 2) - rect.left - size / 2;
    const y = (e.clientY ?? rect.top + rect.height / 2) - rect.top - size / 2;
    // Tạo element <span> để làm ripple
    const ripple = document.createElement('span');
    ripple.className = 'ripple';
    // Set width/height bằng nhau để tạo hình tròn
    ripple.style.width = ripple.style.height = size + 'px';
    // Set vị trí ripple
    ripple.style.left = x + 'px';
    ripple.style.top = y + 'px';
    // Thêm ripple vào bên trong host
    host.appendChild(ripple);
    // Xóa ripple sau 600ms để tránh tràn DOM
    setTimeout(() => ripple.remove(), 600);
  };
  return add;
}
