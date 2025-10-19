import React, { useEffect, useRef, useState } from "react";
import "./features.css";
import { useAuth } from "../../context/AuthContext.jsx";
import useRipple from "../../hooks/useRipple";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/apiFetch";
import useReveal from "../../hooks/useReveal.js";
export default function Features() {
  const sectionRef1 = useRef(null);
  const sectionRef2 = useRef(null);
  const { isAuthenticated } = useAuth();
  const ripple = useRipple();
  const nav = useNavigate();
  const [adminProjects, setAdminProjects] = useState([]);

  // Hiệu ứng hiện dần
  useReveal(sectionRef1);
  useReveal(sectionRef2);

  //gọi API lấy danh sách dự án do admin tạo
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const res = await apiFetch("/api/v1/projects/all", { method: "GET" });
        const projects = res?.response || [];
        console.log(" Admin Projects:", projects);
        setAdminProjects(projects);
      } catch (err) {
        console.error("Failed to load projects:", err);
      }
    };

    fetchProjects().catch((err) =>
      console.error("Unhandled promise error:", err)
    );
  }, []);

  return (
    <>
      {/* Projects Section */}
      <section
        id="projects"
        ref={sectionRef1}
        className="features-section reveal"
      >
        <div className="container">
          <h2 className="section-title text-center text-dark mb-5">
            Choose Carbon Projects
          </h2>
          <div className="row g-4">
            {/* Các dự án do Admin tạo */}
            {adminProjects.length > 0 &&
              adminProjects.map((proj) => (
                <div className="col-md-4" key={`admin-${proj.id}`}>
                  <div className="card h-100 overflow-hidden position-relative shadow-sm">
                    <div className="bg-image hover-overlay">
                      <img
                        src={proj.logo || ""}
                        className="img-fluid"
                        alt={proj.title || "Admin Project"}
                      />
                      <div
                        className="mask"
                        style={{ backgroundColor: "rgba(0,0,0,0.3)" }}
                      ></div>
                    </div>
                    <div className="card-body d-flex flex-column">
                      <h5 className="card-title fw-semibold">
                        {proj.title || "Untitled Project"}
                      </h5>
                      <p className="card-text flex-grow-1">
                        {proj.description?.substring(0, 160) ||
                          "No description provided."}
                        {proj.description?.length > 160 && "..."}
                      </p>
                      <button
                        className="btn btn-primary position-relative overflow-hidden mt-3"
                        onClick={(e) => {
                          ripple(e, e.currentTarget);
                          // nav(`/marketplace?project=${proj.id}`);
                          nav(`/detail-project/${proj.id}`);
                        }}
                      >
                        Register this project
                      </button>
                    </div>
                  </div>
                </div>
              ))}
          </div>
        </div>
      </section>

      {/* About Section */}
      <section id="about" ref={sectionRef2} className="features-section reveal">
        <div className="container">
          <h2 className="section-title text-center text-dark mb-4">
            About the Project
          </h2>
          <p
            className="lead text-center text-muted mx-auto mb-5 section-subtitle"
            style={{ maxWidth: 800 }}
          >
            Our project accelerates EV adoption by converting emission
            reductions into verified carbon credits, bridging the gap between EV
            owners and organizations seeking sustainable offset solutions.
          </p>

          <div className="row g-4">
            <div className="col-md-4">
              <div className="feature-card h-100">
                <h5 className="feature-title">CONTEXT & PROBLEM</h5>
                <p className="feature-desc">
                  Vietnam’s EV market is growing rapidly, but verified carbon
                  credit access remains limited.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card h-100">
                <h5 className="feature-title">PROJECT GOALS</h5>
                <p className="feature-desc">
                  Create measurable value for individuals and businesses,
                  driving the shift toward a low-carbon economy.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card h-100">
                <h5 className="feature-title">IMPACT & VALUE</h5>
                <p className="feature-desc">
                  Built-in transparency and compliance keep audits simple and
                  stakeholders aligned.
                </p>
              </div>
            </div>
          </div>

          {!isAuthenticated && (
            <div className="mt-5 text-center">
              <a href="/register" className="btn btn-brand btn-lg me-3">
                Get started
              </a>
              <p className="small text-muted mt-2 mb-0">
                Join us in building a greener future — sign up now and start
                trading carbon credits effortlessly.
              </p>
            </div>
          )}
        </div>
      </section>
    </>
  );
}
