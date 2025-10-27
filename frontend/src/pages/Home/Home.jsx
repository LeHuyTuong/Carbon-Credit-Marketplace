import React from "react";
import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import Hero from "/src/components/Hero/Hero.jsx";
import Features from "../../components/Features/Features";
import Footer from "../../components/Footer/Footer";

export default function Home() {
  const { hash } = useLocation();

  //bắt hash và tự động cuộn đến phần tương ứng
  useEffect(() => {
    if (hash) {
      const el = document.querySelector(hash);
      if (el) {
        const y = el.getBoundingClientRect().top + window.scrollY - 100; // offset nếu có navbar fixed
        window.scrollTo({ top: y, behavior: "smooth" });
      }
    }
  }, [hash]);

  return (
    <>
      <Hero />
      <Features />
      <Footer />
    </>
  );
}
