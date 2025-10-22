import { useEffect } from "react";
//hiệu ứng hiện dần
export default function useReveal(ref, threshold = 0.15) {
  useEffect(() => {
    if (!ref?.current) return;

    const element = ref.current;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target); // chỉ chạy một lần
          }
        });
      },
      { threshold }
    );

    observer.observe(element);

    return () => {
      observer.unobserve(element);
      observer.disconnect();
    };
  }, [ref, threshold]);
}
