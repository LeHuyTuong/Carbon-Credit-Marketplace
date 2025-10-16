import React, { useEffect, useRef, useState } from "react";
import "./marketplace.css";
import useRipple from "../../../../hooks/useRipple";
import { useNavigate, useLocation } from "react-router-dom";
import { apiFetch } from "../../../../utils/apiFetch";
import project1 from "../../../../assets/project1.jpg";
import project2 from "../../../../assets/project2.jpg";
import project3 from "../../../../assets/project3.jpg";

export default function Marketplace() {
  const sectionRef = useRef(null);
  const ripple = useRipple();
  const nav = useNavigate();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const projectId = params.get("project"); // lấy ID từ query param

  const [projects, setProjects] = useState([]);

  // Hiệu ứng hiện dần
  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => entry.isIntersecting && entry.target.classList.add("is-visible"),
      { threshold: 0.15 }
    );
    sectionRef.current && observer.observe(sectionRef.current);
    return () => observer.disconnect();
  }, []);

  // Gọi API lấy dự án
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const res = await apiFetch("/api/v1/projects/all", { method: "GET" });
        let allProjects = res?.response || [];

        // Nếu có projectId từ query, lọc dự án tương ứng
        if (projectId) {
          allProjects = allProjects.filter((p) => p.id === projectId);
        }

        // Nếu API trả rỗng, vẫn giữ 3 dự án tĩnh làm fallback
        if (allProjects.length === 0) {
          allProjects = [
            { id: "static-1", img: project1, title: "$39.00", text: "Purchase verified credits..." },
            { id: "static-2", img: project2, title: "$100.00", text: "Invest in transparent credits..." },
            { id: "static-3", img: project3, title: "$60.00", text: "Measures electricity consumption..." },
          ];
        }

        setProjects(allProjects);
      } catch (err) {
        console.error("❌ Failed to load projects:", err);
      }
    };

    fetchProjects();
  }, [projectId]);

  return (
    <section
      id="projects"
      ref={sectionRef}
      className="features-section reveal auth-hero min-vh-100 d-flex align-items-center justify-content-center"
    >
      <div className="container">
        <div className="text-center mb-5 mt-2">
          <h2 className="section-title">Carbon Credits Marketplace</h2>
          <h6 className="section-subtitle">EV Charging Carbon Credit Project</h6>
        </div>

        <div className="main-section mb-4">
          <h2 className="section-title text-accent mb-0">Buy Credits</h2>
          <h6 className="section-subtitle2">
            Access verified carbon credits generated from EV charging data across certified companies. Each credit represents measurable CO₂ reductions achieved by electric fleets using clean energy.
          </h6>
        </div>

        <div className="row row-cols-1 row-cols-md-3 g-4">
          {projects.map((p) => (
            <div className="col d-flex" key={p.id}>
              <div className="card project-card w-100">
                <div className="card-img-container">
                  <img src={p.img || project1} className="card-img-top" alt={p.title} />
                </div>
                <div className="card-body">
                  <h5 className="card-title">{p.title}</h5>
                  <p className="card-text">{p.text}</p>
                  <button
                    className="btn-primary btn-buy mt-3"
                    onClick={(e) => {
                      ripple(e, e.currentTarget);
                      nav("/order");
                    }}
                  >
                    Buy
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
