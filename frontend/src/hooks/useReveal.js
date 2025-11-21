import { useEffect } from "react";
// hiệu ứng hiện dần khi phần tử xuất hiện trong viewport
export default function useReveal(ref, threshold = 0.15) {
  useEffect(() => {
    // Nếu ref chưa gắn vào element thì dừng
    if (!ref?.current) return;

    const element = ref.current;

    // Tạo IntersectionObserver để theo dõi khi element đi vào viewport
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          // Khi phần tử nằm trong vùng nhìn thấy
          if (entry.isIntersecting) {
            // Thêm class để kích hoạt animation CSS
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target); // chỉ chạy một lần
          }
        });
      },
      // Ngưỡng xuất hiện: 0.15 = 15% diện tích element xuất hiện sẽ trigger
      { threshold }
    );

    // Bắt đầu quan sát element
    observer.observe(element);

    // Cleanup khi component unmount
    return () => {
      observer.unobserve(element);
      observer.disconnect();
    };
  }, [ref, threshold]); // Re-run khi ref hoặc threshold thay đổi
}
