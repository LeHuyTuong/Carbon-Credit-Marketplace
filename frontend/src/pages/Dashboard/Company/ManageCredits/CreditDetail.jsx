import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Card,
  Spinner,
  Container,
  Row,
  Col,
  Button,
  Badge,
} from "react-bootstrap";
import { FaArrowLeft } from "react-icons/fa";
import project1 from "../../../../assets/project1.jpg";
import project2 from "../../../../assets/project2.jpg";
import project3 from "../../../../assets/project3.jpg";

export default function CreditDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const [credit, setCredit] = useState(null);
  const [loading, setLoading] = useState(true);

  const mockData = [
    {
      listingId: 101,
      projectTitle: "EV Charging Credit - Phase 1",
      sellerCompanyName: "Green Mobility Co.",
      pricePerCredit: 45.5,
      quantity: 1200,
      expiresAt: "2025-12-31T23:59:59Z",
      creditCode: "CRD-101-A",
      img: project1,
    },
    {
      listingId: 102,
      projectTitle: "EV Charging Credit - Phase 2",
      sellerCompanyName: "Blue Charge Ltd.",
      pricePerCredit: 52.0,
      quantity: 600,
      expiresAt: "2025-11-20T23:59:59Z",
      creditCode: "CRD-102-B",
      img: project2,
    },
    {
      listingId: 103,
      projectTitle: "EV Charging Credit - Phase 3",
      sellerCompanyName: "VoltEdge EV Co.",
      pricePerCredit: 39.99,
      quantity: 0,
      expiresAt: "2025-10-30T23:59:59Z",
      creditCode: "CRD-103-C",
      img: project3,
    },
  ];

  useEffect(() => {
    setLoading(true);
    const found = mockData.find((c) => String(c.listingId) === String(id));
    setTimeout(() => {
      setCredit(found || null);
      setLoading(false);
    }, 500);
  }, [id]);

  if (loading)
    return (
      <div className="d-flex justify-content-center align-items-center min-vh-100">
        <Spinner animation="border" />
      </div>
    );

  if (!credit)
    return (
      <Container className="text-center mt-5">
        <h5>Credit not found</h5>
        <Button variant="outline-secondary" onClick={() => nav(-1)}>
          Back
        </Button>
      </Container>
    );

  return (
    <Container className="py-5">
      {/* Back button */}
      <Button
        variant="outline-info"
        size="sm"
        className="mb-4 d-flex align-items-center gap-2"
        onClick={() => nav("/marketplace")}
      >
        <FaArrowLeft /> Back to Marketplace
      </Button>

      <Card className="shadow-lg border-0 p-4">
        <Card.Img
          src={credit.img}
          alt={credit.projectTitle}
          className="rounded mb-4"
          style={{ maxHeight: "300px", objectFit: "cover" }}
        />
        <Card.Body>
          <h3 className="fw-bold mb-3">{credit.projectTitle}</h3>

          <Row>
            <Col md={6}>
              <p>
                <strong>Seller:</strong> {credit.sellerCompanyName}
              </p>
              <p>
                <strong>Price per Credit:</strong> ${credit.pricePerCredit}
              </p>
              <p>
                <strong>Available Quantity:</strong> {credit.quantity}
              </p>
              <p>
                <strong>Status:</strong>{" "}
                <Badge bg={credit.quantity > 0 ? "success" : "secondary"}>
                  {credit.quantity > 0 ? "Available" : "Sold Out"}
                </Badge>
              </p>
            </Col>
            <Col md={6}>
              <p>
                <strong>Expires At:</strong>{" "}
                {new Date(credit.expiresAt).toLocaleString("vi-VN", {
                  timeZone: "Asia/Ho_Chi_Minh",
                  hour12: false,
                })}
              </p>
              <p>
                <strong>Listing ID:</strong> {credit.listingId}
              </p>
              <p>
                <strong>Credit Code:</strong> {credit.creditCode}
              </p>
            </Col>
          </Row>

          <div className="mt-4 d-flex gap-3">
            <Button
              variant="primary"
              onClick={() =>
                nav("/order", { state: { credit, from: "credit-detail" } })
              }
              disabled={credit.quantity === 0}
            >
              Purchase
            </Button>
            <Button
              variant="outline-secondary"
              onClick={() => nav("/marketplace")}
            >
              Back to Marketplace
            </Button>
          </div>
        </Card.Body>
      </Card>
    </Container>
  );
}
