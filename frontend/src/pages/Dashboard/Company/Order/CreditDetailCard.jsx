import React from "react";
import { Card, Row, Col } from "react-bootstrap";

export default function CreditDetailCard({ credit }) {
  if (!credit) return null;

  return (
    <Card
      className="shadow-sm border-0 mb-4 overflow-hidden"
      style={{
        background: "#ffffff",
        borderRadius: "12px",
      }}
    >
      <Row className="g-0">
        {/*left image */}
        <Col md={5}>
          <div
            style={{
              height: "100%",
              minHeight: "240px",
              overflow: "hidden",
            }}
          >
            <img
              src={credit.img}
              alt={credit.title}
              className="w-100 h-100"
              style={{
                objectFit: "cover",
                objectPosition: "center",
                borderTopLeftRadius: "12px",
                borderBottomLeftRadius: "12px",
              }}
            />
          </div>
        </Col>

        {/*right detail */}
        <Col md={7}>
          <Card.Body className="p-4">
            <h3 className="fw-bold text-dark mb-2">{credit.title}</h3>
            <p className="text-muted mb-3">
              Sold by <strong>{credit.seller}</strong>
            </p>

            {/*info grid */}
            <Row className="small mb-3">
              <Col xs={6} sm={4} className="mb-2">
                <div className="text-muted">Price per credit</div>
                <div className="fw-semibold">${credit.price?.toFixed(2)}</div>
              </Col>
              <Col xs={6} sm={4} className="mb-2">
                <div className="text-muted">Available</div>
                <div className="fw-semibold">{credit.quantity}</div>
              </Col>
              <Col xs={6} sm={4} className="mb-2">
                <div className="text-muted">Expires on</div>
                <div className="fw-semibold">{credit.expiresAt}</div>
              </Col>
              <Col xs={6} sm={4} className="mb-2">
                <div className="text-muted">Project Type</div>
                <div className="fw-semibold">
                  {credit.projectType || "EV Charging Offset"}
                </div>
              </Col>
              <Col xs={6} sm={4} className="mb-2">
                <div className="text-muted">Verified by</div>
                <div className="fw-semibold text-success">
                  {credit.verifiedBy || "CarbonX"}
                </div>
              </Col>
            </Row>

            <p className="text-secondary small mb-0">
              {credit.description ||
                "This carbon credit supports EV charging infrastructure to offset emissions from electric vehicles."}
            </p>
          </Card.Body>
        </Col>
      </Row>
    </Card>
  );
}
