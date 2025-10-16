import React, { useEffect, useRef } from "react";
import "./features.css";
import { useAuth } from "../../context/AuthContext.jsx";
import project1 from "../../assets/project1.jpg";
import project2 from "../../assets/project2.jpg";
import project3 from "../../assets/project3.jpg";
import useRipple from "../../hooks/useRipple";
import { useNavigate } from "react-router-dom";

export default function Features() {
  const sectionRef1 = useRef(null);
  const sectionRef2 = useRef(null);
  const { isAuthenticated } = useAuth();
  const ripple = useRipple();
  const nav = useNavigate();

  //hiệu ứng hiện dần
  useEffect(() => {
    const sections = [sectionRef1, sectionRef2];
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) entry.target.classList.add("is-visible");
      },
      { threshold: 0.15 }
    );

    sections.forEach((ref) => ref.current && observer.observe(ref.current));
    return () => observer.disconnect();
  }, []);

  const projectData = [
    {
      img: project1,
      title: "EV Charging Carbon Credit Project",
      text: `Generates carbon credits from verified emission reductions 
        by replacing fossil fuel usage with clean electricity.`,
    },
    {
      img: project2,
      title: "EV Fleet Transition Program",
      text: `Supports companies transitioning their fleets from 
        fuel to electric vehicles, measuring emission baselines 
        and verified reductions.`,
    },
    {
      img: project3,
      title: "EV Distance-Based Energy Measurement Project",
      text: `Measures and verifies electricity consumption of EVs 
        based on distance traveled, generating transparent carbon credits.`,
    },
  ];

  return (
    <>
      {/*projects Section */}
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
            {projectData.map((p, i) => (
              <div className="col-md-4" key={i}>
                <div className="card h-100 overflow-hidden position-relative shadow-sm">
                  <div className="bg-image hover-overlay">
                    <img src={p.img} className="img-fluid" alt={p.title} />
                    <div
                      className="mask"
                      style={{ backgroundColor: "rgba(0,0,0,0.3)" }}
                    ></div>
                  </div>
                  <div className="card-body d-flex flex-column">
                    <h5 className="card-title fw-semibold">{p.title}</h5>
                    <p className="card-text flex-grow-1">{p.text}</p>
                    <button
                      className="btn btn-primary position-relative overflow-hidden mt-3"
                      onClick={(e) => {
                        ripple(e, e.currentTarget);
                        nav("/detail-project");
                      }}
                    >
                      Go to Marketplace
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/*about Section */}
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
                  credit access remains limited for individuals and companies.
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
