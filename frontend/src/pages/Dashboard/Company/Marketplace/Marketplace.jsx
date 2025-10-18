import React, { useEffect, useRef, useState } from "react";
import "./marketplace.css";
import project1 from "../../../../assets/project1.jpg";
import project2 from "../../../../assets/project2.jpg";
import project3 from "../../../../assets/project3.jpg";
import useRipple from "../../../../hooks/useRipple";
import { useNavigate } from "react-router-dom";
import useReveal from "../../../../hooks/useReveal";

export default function Marketplace() {
  const sectionRef = useRef(null);
  const ripple = useRipple();
  const nav = useNavigate();
  const [projectData, setProjectData] = useState([]);

  const projectImages = [project1, project2, project3];
  useReveal(sectionRef);
  useEffect(() => {
    //lấy danh sách credit từ localStorage (mock)
    const localCredits = JSON.parse(
      localStorage.getItem("mockCredits") || "[]"
    );
    const formatted = localCredits.map((c, i) => ({
      id: c.id,
      title: c.title || "EV Carbon Credit",
      seller: "Mock Seller Co.",
      price: c.price,
      quantity: c.quantity,
      expiresAt: c.createdAt,
      img: projectImages[i % projectImages.length],
    }));

    setProjectData(formatted);
  }, []);

  return (
    <section
      id="projects"
      ref={sectionRef}
      className="features-section reveal auth-hero min-vh-100 d-flex align-items-center justify-content-center"
    >
      <div className="container">
        <div className="text-center mb-5 mt-2">
          <h2 className="section-title">Carbon Credits Marketplace</h2>
          <h6 className="section-subtitle">
            EV Charging Carbon Credit Exchange
          </h6>
        </div>

        {projectData.length === 0 ? (
          <p className="text-center text-light fs-4 fw-semibold mt-4">
            No active credits available.
          </p>
        ) : (
          <div className="project-grid">
            {projectData.map((p) => (
              <div className="project-card" key={p.id}>
                <div className="card-img-container">
                  <img src={p.img} className="card-img-top" alt={p.title} />
                </div>

                <div className="card-body">
                  <h5 className="card-title mb-2 text-dark fw-bold">
                    {p.title}
                  </h5>
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="price-tag fw-bold">${p.price}</span>
                    <span className="text-muted small">
                      <strong>Available:</strong> {p.quantity}
                    </span>
                  </div>
                  <p className="text-muted small mb-1">
                    <strong>Seller:</strong> {p.seller}
                  </p>
                  <p className="text-muted small mb-2">
                    Expires on: {p.expiresAt}
                  </p>
                  <button
                    className="btn-primary btn-buy mt-3 w-100"
                    onClick={(e) => {
                      ripple(e, e.currentTarget);
                      nav("/order", { state: { credit: p } });
                    }}
                  >
                    Buy Now
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
