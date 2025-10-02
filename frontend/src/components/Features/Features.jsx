import React, { useEffect, useRef } from "react";
import "./features.css";
import { useAuth } from "../../context/AuthContext.jsx"

export default function Features() {
  const sectionRef = useRef(null);
  const { isAuthenticated } = useAuth();//lấy trạng thái login

  //hiện dần
  useEffect(() => {
    const el = sectionRef.current;
    if (!el) return;

    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) el.classList.add("is-visible");
      },
      { threshold: 0.15 }
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);

  return (
    <section id="how" ref={sectionRef} className="features-section reveal">
      <div className="container">
        <h2 className="section-title text-center">
          About the Project
        </h2>

        <p className="lead text-center mx-auto section-subtitle">
          Our project is designed to accelerate the adoption of electric 
          vehicles by turning their emission reductions into verified, 
          tradable carbon credits. It bridges the gap between EV owners 
          and businesses seeking sustainable solutions — creating value 
          for people, organizations, and the planet.
        </p>

        <div className="row g-4 mt-2">
          <div className="col-md-4">
            <div className="feature-card">
              <h5 className="feature-title">CONTEXT & PROBLEM</h5>
              <p className="feature-desc">
                In recent years, Vietnam’s electric vehicle (EV) market has 
                been booming, but opportunities to monetize emission 
                reductions remain limited. EV owners often lack access to 
                verified carbon credit markets, while businesses struggle 
                to find trusted sources for carbon offsets.
              </p>
            </div>
          </div>

          <div className="col-md-4">
            <div className="feature-card">
              <h5 className="feature-title">PROJECT GOALS</h5>
              <p className="feature-desc">
                This project brings tangible value to individuals, 
                organizations, and communities, while helping accelerate 
                Vietnam’s transition to a low-carbon economy.
              </p>
            </div>
          </div>

          <div className="col-md-4">
            <div className="feature-card">
              <h5 className="feature-title">IMPACT & VALUE</h5>
              <p className="feature-desc">
                Built-in transparency and compliance to keep audits simple and
                stakeholders aligned.
              </p>
            </div>
          </div>
        </div>

        {/**chỉ hiện khi chưa login */}
        {!isAuthenticated && (
          <div className="mt-5 text-start">
            <a href="/register" className="btn btn-brand btn-lg me-3">Get started</a>
            <p className="small text-muted mb-0 mt-2">
                Join us in building a greener future — sign up now and start trading carbon credits effortlessly.
            </p>
        </div>
        )}

      </div>
    </section>
  );
}
