import React, { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "react-bootstrap";
import { FaArrowLeft } from "react-icons/fa";
import useWalletData from "./useWalletData";
import PaginatedList from "../../../components/Pagination/PaginatedList";
import useReveal from "../../../hooks/useReveal";

export default function CreditBatchDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const sectionRef = useRef(null);
  const { creditDetails, fetchMyCredits, loading } = useWalletData();
  const [credits, setCredits] = useState([]);
  useReveal(sectionRef);

  useEffect(() => {
    const loadCredits = async () => {
      if (id) {
        await fetchMyCredits(id);
      }
    };
    loadCredits();
  }, [id]);

  useEffect(() => {
    if (creditDetails?.length) setCredits(creditDetails);
  }, [creditDetails]);

  return (
    <div
      ref={sectionRef}
      className="auth-hero2 wallet-page d-flex flex-column align-items-center py-5 reveal"
    >
      {/* Back button */}
      <Button
        variant="outline-info"
        size="sm"
        className="position-fixed top-0 start-0 m-3 px-3 py-2 d-flex align-items-center gap-2 fw-semibold shadow-sm"
        style={{
          borderRadius: "10px",
          background: "rgba(255,255,255,0.85)",
          backdropFilter: "blur(6px)",
          zIndex: 20,
        }}
        onClick={() => nav("/wallet")}
      >
        <FaArrowLeft /> Back to Wallet
      </Button>

      {/* Header */}
      <div className="text-center mb-4">
        <div className="d-flex justify-content-center align-items-center gap-2 mb-2">
          <i className="bi bi-card-checklist fs-3 text-accent"></i>
          <h3 className="fw-bold text-light mb-0">
            Credit Details (Batch #{id})
          </h3>
        </div>
      </div>

      {/* Content */}
      <div className="glass-card p-4 w-75">
        {loading ? (
          <div className="text-center text-light">Đang tải dữ liệu...</div>
        ) : !credits?.length ? (
          <div className="text-light text-center">
            Không tìm thấy tín chỉ cho batch này.
          </div>
        ) : (
          <PaginatedList
            items={credits}
            itemsPerPage={6}
            renderItem={(c) => (
              <div
                key={c.id}
                className="d-flex justify-content-between align-items-center border-bottom py-2"
              >
                <div className="flex-grow-1">
                  <div className="fw-semibold text-accent">{c.creditCode}</div>
                  <div className="small text-light">
                    {c.projectTitle} — {c.companyName}
                  </div>
                  <div className="small text-light">
                    {new Date(c.issuedAt).toLocaleString("vi-VN", {
                      timeZone: "Asia/Ho_Chi_Minh",
                      hour12: false,
                    })}
                  </div>
                </div>

                <span
                  className={`badge ${
                    c.status === "AVAILABLE"
                      ? "bg-success"
                      : c.status === "LISTED"
                      ? "bg-info"
                      : "bg-secondary"
                  }`}
                >
                  {c.status}
                </span>
              </div>
            )}
          />
        )}
      </div>
    </div>
  );
}
