// import React, { useEffect, useRef } from "react";
// import "./marketplace.css";
// import project1 from "../../../../assets/project1.jpg";
// import project2 from "../../../../assets/project2.jpg";
// import project3 from "../../../../assets/project3.jpg";
// import useRipple from "../../../../hooks/useRipple";
// import { useNavigate } from "react-router-dom";

// export default function Marketplace() {
//   const sectionRef = useRef(null);
//   const ripple = useRipple();
//   const nav = useNavigate();

//   useEffect(() => {
//     const observer = new IntersectionObserver(
//       ([entry]) =>
//         entry.isIntersecting && entry.target.classList.add("is-visible"),
//       { threshold: 0.15 }
//     );
//     sectionRef.current && observer.observe(sectionRef.current);
//     return () => observer.disconnect();
//   }, []);

//   const projectData = [
//     {
//       img: project1,
//       title: "$39.00",
//       text: `Purchase verified credits generated from public EV charging networks. Each credit reflects the emission reduction achieved by replacing fossil fuel usage with renewable-powered EV charging.`,
//     },
//     {
//       img: project2,
//       title: "$100.00",
//       text: `Invest in transparent, data-backed carbon credits measured from real-time EV charging sessions. Perfect for businesses seeking reliable emission offsets.`,
//     },
//     {
//       img: project3,
//       title: "$60.00",
//       text: `Measures and verifies electricity consumption of EVs based on distance traveled, generating transparent carbon credits.`,
//     },
//   ];

//   return (
//     <section
//       id="projects"
//       ref={sectionRef}
//       className="features-section reveal auth-hero min-vh-100 d-flex align-items-center justify-content-center"
//     >
//       <div className="container">
//         <div className="text-center mb-5 mt-2">
//           <h2 className="section-title">Carbon Credits Marketplace</h2>
//           <h6 className="section-subtitle">
//             EV Charging Carbon Credit Project
//           </h6>
//         </div>

//         <div className="main-section mb-4">
//           <h2 className="section-title text-accent mb-0">Buy Credits</h2>
//           <h6 className="section-subtitle2">
//             Access verified carbon credits generated from EV charging data
//             across certified companies. Each credit represents measurable CO₂
//             reductions achieved by electric fleets using clean energy.
//           </h6>
//         </div>

//         <div className="row row-cols-1 row-cols-md-3 g-4">
//           {projectData.map((p, i) => (
//             <div className="col d-flex" key={i}>
//               <div className="card project-card w-100">
//                 <div className="card-img-container">
//                   <img src={p.img} className="card-img-top" alt={p.title} />
//                 </div>
//                 <div className="card-body">
//                   <h5 className="card-title">{p.title}</h5>
//                   <p className="card-text">{p.text}</p>
//                   <button
//                     className="btn-primary btn-buy mt-3"
//                     onClick={(e) => {
//                       ripple(e, e.currentTarget);
//                       nav("/order");
//                     }}
//                   >
//                     Buy
//                   </button>
//                 </div>
//               </div>
//             </div>
//           ))}
//         </div>
//       </div>
//     </section>
//   );
// }

import React, { useEffect, useRef, useState } from "react";
import "./marketplace.css";
import project1 from "../../../../assets/project1.jpg";
import project2 from "../../../../assets/project2.jpg";
import project3 from "../../../../assets/project3.jpg";
import useRipple from "../../../../hooks/useRipple";
import { useNavigate } from "react-router-dom";

export default function Marketplace() {
  const sectionRef = useRef(null);
  const ripple = useRipple();
  const nav = useNavigate();
  const [projectData, setProjectData] = useState([]);

  // danh sách ảnh tĩnh để luân phiên
  const projectImages = [project1, project2, project3];

  useEffect(() => {
    // Lấy dữ liệu giả lập từ localStorage
    const stored = JSON.parse(localStorage.getItem("mockCredits")) || [];
    const activeCredits = stored.filter((c) => c.status === "active");
    // gắn ảnh tĩnh theo index xoay vòng
    const creditsWithImg = activeCredits.map((c, i) => ({
      ...c,
      img: projectImages[i % projectImages.length],
    }));
    setProjectData(creditsWithImg);

    // Hiệu ứng xuất hiện khi scroll
    const observer = new IntersectionObserver(
      ([entry]) =>
        entry.isIntersecting && entry.target.classList.add("is-visible"),
      { threshold: 0.15 }
    );
    sectionRef.current && observer.observe(sectionRef.current);
    return () => observer.disconnect();
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
            EV Charging Carbon Credit Project
          </h6>
        </div>

        <div className="main-section mb-4">
          <h2 className="section-title text-accent mb-0">Buy Credits</h2>
          <h6 className="section-subtitle2">
            Access verified carbon credits generated from EV charging data
            across certified companies. Each credit represents measurable CO₂
            reductions achieved by electric fleets using clean energy.
          </h6>
        </div>

        {projectData.length === 0 ? (
          <p className="text-center text-muted">No active credits available.</p>
        ) : (
          <div className="row row-cols-1 row-cols-md-3 g-4">
            {projectData.map((p, i) => (
              <div className="col d-flex" key={p.id || i}>
                <div className="card project-card w-100">
                  <div className="card-img-container">
                    <img
                      src={p.img}
                      className="card-img-top"
                      alt={p.title || "Carbon Credit"}
                    />
                  </div>
                  <div className="card-body">
                    <h5 className="card-title">
                      ${p.price?.toFixed ? p.price.toFixed(2) : p.price}
                    </h5>
                    <p className="card-text mb-1">
                      {p.title || "EV Charging Credit"}
                    </p>
                    <p className="card-text text-muted">
                      Quantity: <strong>{p.quantity}</strong>
                    </p>
                    <button
                      className="btn-primary btn-buy mt-3"
                      onClick={(e) => {
                        ripple(e, e.currentTarget);
                        nav("/order", { state: { credit: p } }); // truyền object credit sang trang Order
                      }}
                    >
                      Buy
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
