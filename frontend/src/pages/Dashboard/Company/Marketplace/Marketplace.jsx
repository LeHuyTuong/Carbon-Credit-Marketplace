import React, { useEffect, useRef, useState } from "react";
import "./marketplace.css";
import project1 from "../../../../assets/project1.jpg";
import project2 from "../../../../assets/project2.jpg";
import project3 from "../../../../assets/project3.jpg";
import useRipple from "../../../../hooks/useRipple";
import { useNavigate } from "react-router-dom";
import useReveal from "../../../../hooks/useReveal";
import { apiFetch } from "../../../../utils/apiFetch";
import { Spinner } from "react-bootstrap";
import PaginatedList from "../../../../components/Pagination/PaginatedList";

export default function Marketplace() {
  const sectionRef = useRef(null);
  const ripple = useRipple();
  const nav = useNavigate();
  const [credits, setCredits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const projectImages = [project1, project2, project3];
  useReveal(sectionRef);

  useEffect(() => {
    const fetchMarketplace = async () => {
      try {
        setLoading(true);
        setError(null);

        // gọi api
        const res = await apiFetch("/api/v1/marketplace", { method: "GET" });
        const list = res?.response || [];

        // map dữ liệu sang dạng FE dễ render
        const formatted = list.map((item, index) => ({
          id: item.listingId,
          title: item.projectTitle || "Unnamed Project",
          seller: item.sellerCompanyName || "Unknown Seller",
          price: item.pricePerCredit,
          quantity: item.quantity,
          expiresAt: new Date(item.expiresAt).toLocaleDateString("en-GB"),
          img: projectImages[index % projectImages.length],
        }));

        setCredits(formatted);
      } catch (err) {
        console.error("Failed to fetch marketplace:", err);
        setError(err.message || "Failed to load marketplace data.");
      } finally {
        setLoading(false);
      }
    };

    fetchMarketplace();
  }, []);

  return (
    <section
      id="projects"
      ref={sectionRef}
      className="features-section reveal auth-hero min-vh-100"
    >
      <div className="container">
        <div className="text-center mb-5 mt-2">
          <h2 className="section-title">Carbon Credits Marketplace</h2>
          <h6 className="section-subtitle">
            EV Charging Carbon Credit Exchange
          </h6>
        </div>

        {credits.length === 0 ? (
          <p className="text-center text-light fs-4 fw-semibold mt-4">
            No active credits available.
          </p>
        ) : (
          <PaginatedList
            items={credits}
            itemsPerPage={3} //3 card mỗi trang
            useGrid
            renderItem={(c) => (
              <div className="project-card" key={c.id}>
                <div className="card-img-container">
                  <img src={c.img} className="card-img-top" alt={c.title} />
                </div>

                <div className="card-body">
                  <h5 className="card-title mb-2 text-dark fw-bold">
                    {c.title}
                  </h5>
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="price-tag fw-bold">${c.price}</span>
                    <span className="text-muted small">
                      <strong>Available:</strong> {c.quantity}
                    </span>
                  </div>
                  <p className="text-muted small mb-1">
                    <strong>Seller:</strong> {c.seller}
                  </p>
                  <p className="text-muted small mb-2">
                    Expires on: {c.expiresAt}
                  </p>
                  <button
                    className="btn-primary btn-buy mt-3 w-100"
                    onClick={(e) => {
                      ripple(e, e.currentTarget);
                      nav("/order", {
                        state: { credit: c, from: "marketplace" },
                      });
                    }}
                  >
                    Buy Now
                  </button>
                </div>
              </div>
            )}
          />
        )}
      </div>
    </section>
  );
}
