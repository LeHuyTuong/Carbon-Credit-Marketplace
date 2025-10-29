import React, { useEffect, useRef, useState } from "react";
import "./features.css";
import { useAuth } from "../../context/AuthContext.jsx";
import useRipple from "../../hooks/useRipple";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/apiFetch";
import useReveal from "../../hooks/useReveal.js";
import PaginatedList from "../Pagination/PaginatedList.jsx";
import { Toast, ToastContainer } from "react-bootstrap";

const DEFAULT_LOGO = "https://placehold.co/800x400?text=CarbonX+Project";

export default function Features() {
  const sectionRef1 = useRef(null);
  const sectionRef2 = useRef(null);
  const { isAuthenticated, user } = useAuth();
  const ripple = useRipple();
  const nav = useNavigate();
  const [adminProjects, setAdminProjects] = useState([]);

  useReveal(sectionRef1);
  useReveal(sectionRef2);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const res = await apiFetch("/api/v1/projects/all", { method: "GET" });
        const projects = res?.response || [];
        setAdminProjects(projects);
      } catch (err) {
        console.error("[Features] Failed to load projects:", err);
      }
    };
    fetchProjects();
  }, []);

  const safeLogo = (logo) => {
    if (typeof logo !== "string" || !logo.trim()) return DEFAULT_LOGO;
    // nếu BE lỡ trả thiếu protocol
    return logo.startsWith("http") ? logo : `https://${logo}`;
  };
  const [toast, setToast] = useState({
    show: false,
    message: "",
    variant: "success",
  });

  const showToast = (message, variant = "success") => {
    setToast({ show: true, message, variant });
  };

  return (
    <>
      <section
        id="projects"
        ref={sectionRef1}
        className="features-section reveal"
      >
        <div className="container" id="register">
          <h2 className="section-title text-center text-dark mb-3">
            CHOOSE CARBON PROJECTS
          </h2>
          {user?.role !== "COMPANY" && (
            <p className="text-center text-muted small mb-5">
              Only registered companies can register carbon projects.
            </p>
          )}

          {adminProjects.length > 0 ? (
            <PaginatedList
              items={adminProjects}
              itemsPerPage={3}
              renderItem={(proj) => (
                <div className="col-md-4" key={`admin-${proj.id}`}>
                  <div className="card h-100 overflow-hidden position-relative shadow-sm border-0">
                    {/* IMAGE */}
                    <div className="bg-image hover-overlay">
                      <img
                        src={safeLogo(proj.logo)}
                        alt={proj.title || "Project"}
                        className="img-fluid"
                        style={{
                          width: "100%",
                          height: "220px",
                          objectFit: "cover",
                          borderRadius: "6px 6px 0 0",
                          backgroundColor: "#f8f9fa",
                        }}
                        onError={(e) => {
                          // chặn vòng lặp lỗi nếu fallback cũng hỏng
                          if (e.currentTarget.src !== DEFAULT_LOGO) {
                            e.currentTarget.onerror = null;
                            e.currentTarget.src = DEFAULT_LOGO;
                          }
                        }}
                      />
                      <div
                        className="mask"
                        style={{ backgroundColor: "rgba(0,0,0,0.25)" }}
                      />
                    </div>

                    {/* CONTENT */}
                    <div className="card-body d-flex flex-column">
                      <h5 className="card-title fw-semibold text-dark">
                        {proj.title || "Untitled Project"}
                      </h5>
                      <p className="card-text flex-grow-1 text-muted">
                        {proj.description?.substring(0, 160) ||
                          "No description provided."}
                        {proj.description?.length > 160 && "..."}
                      </p>

                      <button
                        className="btn btn-primary position-relative overflow-hidden mt-3 fw-semibold"
                        onClick={(e) => {
                          if (!isAuthenticated) {
                            showToast(
                              "Please log in as Company to register a project",
                              "warning"
                            );
                            nav("/login");
                            return;
                          }
                          if (user?.role !== "COMPANY") {
                            showToast(
                              "You don’t have permission to register a project",
                              "danger"
                            );
                            return;
                          }
                          ripple(e, e.currentTarget);
                          nav(`/detail-project/${proj.id}`);
                        }}
                      >
                        Register this project
                      </button>
                    </div>
                  </div>
                </div>
              )}
            />
          ) : (
            <p className="text-center text-muted">No projects available yet.</p>
          )}
        </div>
      </section>

      <section id="about" ref={sectionRef2} className="features-section reveal">
        <div className="container">
          <h2 className="section-title text-center text-dark mb-4">
            ABOUT THE PROJECT
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
        </div>
      </section>
      <ToastContainer
        position="bottom-center"
        className="p-3"
        style={{ marginBottom: "-90px" }}
      >
        <Toast
          onClose={() => setToast({ ...toast, show: false })}
          show={toast.show}
          bg={toast.variant}
          delay={3000}
          autohide
        >
          <Toast.Body className="text-white">{toast.message}</Toast.Body>
        </Toast>
      </ToastContainer>
    </>
  );
}
