// import React, { useState } from "react";
// import { useNavigate } from "react-router-dom";
// import {
//   Form,
//   Button,
//   Accordion,
//   Card,
//   Row,
//   Col,
//   Alert,
//   Container,
//   Image,
// } from "react-bootstrap";
// import { FaLeaf, FaLock, FaExternalLinkAlt, FaArrowLeft } from "react-icons/fa";

// export default function Order() {
//   const nav = useNavigate();
//   const pricePerTonne = 12.5;
//   const availableTonnes = 4992.405621970903;

//   const [formData, setFormData] = useState({
//     quantity: "",
//     beneficiaryName: "",
//     beneficiaryAddress: "0xcba00000c228bcecdd6146e88a7e6625b90959de",
//     retirementMessage: "",
//     paymentMethod: "Credit Card",
//   });

//   const totalPrice =
//     formData.quantity && formData.quantity > 0
//       ? (formData.quantity * pricePerTonne).toFixed(2)
//       : "0.00";

//   const handleChange = (e) => {
//     const { name, value } = e.target;
//     setFormData((prev) => ({ ...prev, [name]: value }));
//   };

//   const handleSubmit = (e) => {
//     e.preventDefault();
//     console.log("Retirement Data:", formData);
//     alert("Retirement request created successfully!");
//     //call backend or blockchain transaction
//   };

//   return (
//     <div className="auth-hero min-vh-100 d-flex flex-column align-items-center py-4 bg-light">
//       <div className="w-100 text-center mt-1">
//         <Button
//           variant="outline-info"
//           size="lg"
//           className="d-flex align-items-center gap-2"
//           onClick={() => nav("/marketplace")}
//           style={{ marginLeft: "10px" }}
//         >
//           <FaArrowLeft /> Back to Marketplace
//         </Button>
//       </div>
//       <Container className="my-5">
//         <Row>
//           {/*left form*/}
//           <Col lg={8}>
//             <div className="p-4 bg-light rounded-3 shadow-sm">
//               <h3 className="mb-3 fw-bold">Retire Carbon Credits</h3>
//               <p className="text-muted mb-4">
//                 You are retiring carbon credits from this project. The details
//                 you provide will be recorded publicly to verify your
//                 environmental contribution.
//               </p>

//               <Form onSubmit={handleSubmit}>
//                 <Card className="mb-4">
//                   <Card.Body>
//                     <Card.Title>Retirement Details</Card.Title>
//                     <Form.Text className="text-danger">
//                       * Required Field
//                     </Form.Text>

//                     {/*quantity */}
//                     <Form.Group className="mt-3">
//                       <Form.Label>
//                         How many tonnes of carbon would you like to retire? *
//                       </Form.Label>
//                       <Form.Text muted> Available: {availableTonnes}</Form.Text>
//                       <Form.Control
//                         type="number"
//                         name="quantity"
//                         placeholder="Enter tonnes"
//                         min="1"
//                         max={availableTonnes}
//                         value={formData.quantity}
//                         onChange={handleChange}
//                         required
//                       />
//                     </Form.Group>

//                     {/*beneficiary */}
//                     <Form.Group className="mt-3">
//                       <Form.Label>
//                         Who will this retirement be credited to? *
//                       </Form.Label>
//                       <Form.Control
//                         type="text"
//                         name="beneficiaryName"
//                         placeholder="Beneficiary name"
//                         value={formData.beneficiaryName}
//                         onChange={handleChange}
//                         required
//                       />
//                       <Form.Control
//                         type="hidden"
//                         name="beneficiaryAddress"
//                         value={formData.beneficiaryAddress}
//                       />
//                     </Form.Group>

//                     {/*message */}
//                     <Form.Group className="mt-3">
//                       <Form.Label>Public message *</Form.Label>
//                       <Form.Control
//                         as="textarea"
//                         rows={3}
//                         name="retirementMessage"
//                         placeholder='e.g. "Q1 2025 emissions - EU operations"'
//                         value={formData.retirementMessage}
//                         onChange={handleChange}
//                         required
//                       />
//                       <Form.Text className="text-muted">
//                         This message is public and permanent — don’t include
//                         personal details.
//                       </Form.Text>
//                     </Form.Group>
//                   </Card.Body>
//                 </Card>

//                 {/*payment section */}
//                 <Accordion className="mb-4">
//                   <Accordion.Item eventKey="0">
//                     <Accordion.Header>Payment & Privacy Info</Accordion.Header>
//                     <Accordion.Body>
//                       <Form.Group>
//                         <Form.Label>
//                           Pay with: <span className="text-danger">*</span>
//                         </Form.Label>
//                         <Row className="mt-2">
//                           <Col>
//                             <Form.Check
//                               type="radio"
//                               id="creditCard"
//                               label="Credit Card"
//                               name="paymentMethod"
//                               value="Credit Card"
//                               checked={formData.paymentMethod === "Credit Card"}
//                               onChange={handleChange}
//                             />
//                           </Col>
//                           <Col>
//                             <Form.Check
//                               type="radio"
//                               id="bankTransfer"
//                               label="Bank Transfer"
//                               name="paymentMethod"
//                               value="Bank Transfer"
//                               checked={
//                                 formData.paymentMethod === "Bank Transfer"
//                               }
//                               onChange={handleChange}
//                             />
//                           </Col>
//                         </Row>
//                         <Alert variant="light" className="mt-3 mb-0">
//                           <small>All payments are securely processed.</small>
//                         </Alert>
//                       </Form.Group>
//                     </Accordion.Body>
//                   </Accordion.Item>
//                 </Accordion>

//                 {/*submit */}
//                 <Button
//                   variant="success"
//                   type="submit"
//                   className="w-100 py-2 fw-semibold"
//                   disabled={
//                     !formData.quantity ||
//                     !formData.beneficiaryName ||
//                     !formData.retirementMessage
//                   }
//                 >
//                   Retire Carbon Credits
//                 </Button>

//                 {/*info Accordion */}
//                 <Accordion className="mt-4">
//                   <Accordion.Item eventKey="1">
//                     <Accordion.Header>
//                       What happens after I click RETIRE CARBON?
//                     </Accordion.Header>
//                     <Accordion.Body>
//                       <ul className="mb-0">
//                         <li>Payment is processed securely.</li>
//                         <li>Your retirement is recorded on-chain.</li>
//                         <li>A verified PDF certificate will be issued.</li>
//                         <li>
//                           Credits are permanently removed from circulation.
//                         </li>
//                       </ul>
//                     </Accordion.Body>
//                   </Accordion.Item>
//                 </Accordion>
//               </Form>
//             </div>
//           </Col>

//           {/*right: PROJECT SNAPSHOT */}
//           <Col lg={4}>
//             <ProjectSnapshot
//               totalPrice={totalPrice}
//               quantity={formData.quantity}
//               pricePerTonne={pricePerTonne}
//             />
//           </Col>
//         </Row>
//       </Container>
//     </div>
//   );
// }

// function ProjectSnapshot({ totalPrice, quantity, pricePerTonne }) {
//   const project = {
//     title: "Renewable Energy Project in Netherlands",
//     details: "Vintage 2011 | Verified by Verra",
//     description:
//       "Replacing fossil fuels with clean, renewable electricity sources.",
//     token: "TCO2-VCS-338-2011",
//     available: 4992,
//     scanUrl:
//       "https://polygonscan.com/address/0x899b75bc5298784355ca6a265b79b839e6d02bc0",
//   };

//   return (
//     <div className="p-4">
//       {/* Snapshot */}
//       <Card className="mb-3 shadow-sm">
//         <Card.Body>
//           <Row className="align-items-center mb-3">
//             <Col xs="auto">
//               <FaLeaf size={28} color="green" />
//             </Col>
//             <Col>
//               <Card.Title className="mb-0 fw-semibold">
//                 Project Snapshot
//               </Card.Title>
//             </Col>
//           </Row>
//           <h6>{project.title}</h6>
//           <small className="text-muted">{project.details}</small>
//           <p className="mt-2 small">{project.description}</p>
//         </Card.Body>
//       </Card>

//       {/* Asset Details */}
//       <Accordion className="mb-3">
//         <Accordion.Item eventKey="0">
//           <Accordion.Header>Asset Details</Accordion.Header>
//           <Accordion.Body>
//             <Row className="mb-2">
//               <Col xs={6} className="text-muted">
//                 Retiring Token
//               </Col>
//               <Col xs={6}>
//                 <Image
//                   src="/_next/static/media/tco2.927d0e45.png"
//                   alt="TCO2"
//                   width={20}
//                   height={20}
//                   className="me-2"
//                 />
//                 <span>{project.token}</span>
//               </Col>
//             </Row>
//             <Row className="mb-2">
//               <Col xs={6} className="text-muted">
//                 Available to retire
//               </Col>
//               <Col xs={6}>{project.available.toLocaleString()} tonnes</Col>
//             </Row>
//             <Row>
//               <Col>
//                 <a
//                   href={project.scanUrl}
//                   target="_blank"
//                   rel="noopener noreferrer"
//                   className="text-decoration-none"
//                 >
//                   View on PolygonScan <FaExternalLinkAlt size={12} />
//                 </a>
//               </Col>
//             </Row>
//           </Accordion.Body>
//         </Accordion.Item>
//       </Accordion>

//       {/*price summary*/}
//       <Card className="mb-3 shadow-sm">
//         <Card.Body>
//           <Card.Title>Total Price</Card.Title>
//           <Row className="mt-3">
//             <Col xs={6} className="text-muted">
//               Amount to retire
//             </Col>
//             <Col xs={6} className="text-end">
//               {quantity || 0} tonnes
//             </Col>
//           </Row>
//           <Row className="mt-2">
//             <Col xs={6} className="text-muted">
//               Price per tonne
//             </Col>
//             <Col xs={6} className="text-end">
//               ${pricePerTonne.toFixed(2)}
//             </Col>
//           </Row>
//           <hr />
//           <Row>
//             <Col xs={6} className="text-muted">
//               Total cost
//             </Col>
//             <Col xs={6} className="text-end fw-bold">
//               ${totalPrice}
//             </Col>
//           </Row>
//         </Card.Body>
//       </Card>

//       <div className="text-center">
//         <Button
//           variant="success"
//           size="lg"
//           className="d-flex align-items-center mx-auto gap-2"
//         >
//           <FaLock /> Retire Carbon
//         </Button>
//       </div>
//     </div>
//   );
// }

import React, { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Form,
  Button,
  Accordion,
  Card,
  Row,
  Col,
  Alert,
  Container,
} from "react-bootstrap";
import { FaLock, FaArrowLeft, FaShoppingCart } from "react-icons/fa";

export default function Order() {
  const nav = useNavigate();
  const { state } = useLocation();
  const credit = state?.credit;

  // Nếu user vào thẳng /order không qua marketplace
  if (!credit) {
    return (
      <div className="d-flex flex-column align-items-center justify-content-center min-vh-100 text-center">
        <p className="text-muted mb-3">No credit selected.</p>
        <Button variant="primary" onClick={() => nav("/marketplace")}>
          Back to Marketplace
        </Button>
      </div>
    );
  }

  const pricePerTonne = credit.price;
  const availableTonnes = credit.quantity;

  const [formData, setFormData] = useState({
    quantity: "",
    beneficiaryName: "",
    beneficiaryAddress: "0xcba00000c228bcecdd6146e88a7e6625b90959de",
    retirementMessage: "",
    paymentMethod: "Credit Card",
  });

  const totalPrice =
    formData.quantity && formData.quantity > 0
      ? (formData.quantity * pricePerTonne).toFixed(2)
      : "0.00";

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    alert(
      `Purchase successful!\n\nBought ${formData.quantity} tonnes at $${pricePerTonne}/tCO₂`
    );
  };

  return (
    <div className="auth-hero min-vh-100 py-5 bg-light">
      <Container>
        {/* Back */}
        <div className="mb-4">
          <Button
            variant="link"
            className="text-decoration-none text-info"
            onClick={() => nav("/marketplace")}
          >
            <FaArrowLeft className="me-2" />
            Back to Marketplace
          </Button>
        </div>

        <Row>
          {/* LEFT FORM */}
          <Col lg={8}>
            <Card className="shadow-sm border-0 mb-4">
              <Card.Body>
                <h3 className="fw-bold mb-3 d-flex align-items-center gap-2">
                  <FaShoppingCart color="green" /> Buy Carbon Credits
                </h3>
                <p className="text-muted mb-4">
                  Finalize your purchase for <strong>{credit.title}</strong>.
                </p>

                <Form onSubmit={handleSubmit}>
                  {/* Quantity */}
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold">
                      Quantity (tonnes) <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Text muted>
                      {" "}
                      Available: {availableTonnes} tonnes
                    </Form.Text>
                    <Form.Control
                      type="number"
                      name="quantity"
                      placeholder="Enter tonnes"
                      min="1"
                      max={availableTonnes}
                      value={formData.quantity}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>

                  {/* Beneficiary */}
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold">
                      Beneficiary name <span className="text-danger">*</span>
                    </Form.Label>
                    <Form.Control
                      type="text"
                      name="beneficiaryName"
                      placeholder="Who will receive credit"
                      value={formData.beneficiaryName}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>

                  {/* Message */}
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold">
                      Public message
                    </Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={3}
                      name="retirementMessage"
                      placeholder='e.g. "Q1 2025 emissions offset"'
                      value={formData.retirementMessage}
                      onChange={handleChange}
                      required
                    />
                    <Form.Text className="text-muted">
                      This message is public and permanent.
                    </Form.Text>
                  </Form.Group>

                  {/* Payment */}
                  <Accordion defaultActiveKey="0" className="mt-4">
                    <Accordion.Item eventKey="0">
                      <Accordion.Header>Payment Method</Accordion.Header>
                      <Accordion.Body>
                        <Row>
                          <Col>
                            <Form.Check
                              type="radio"
                              label="Credit Card"
                              name="paymentMethod"
                              value="Credit Card"
                              checked={formData.paymentMethod === "Credit Card"}
                              onChange={handleChange}
                            />
                          </Col>
                          <Col>
                            <Form.Check
                              type="radio"
                              label="Bank Transfer"
                              name="paymentMethod"
                              value="Bank Transfer"
                              checked={
                                formData.paymentMethod === "Bank Transfer"
                              }
                              onChange={handleChange}
                            />
                          </Col>
                        </Row>
                        <Alert variant="light" className="mt-3 mb-0">
                          <small>
                            <FaLock /> All payments are securely processed.
                          </small>
                        </Alert>
                      </Accordion.Body>
                    </Accordion.Item>
                  </Accordion>

                  {/* Submit */}
                  <div className="mt-4">
                    <Button
                      variant="success"
                      type="submit"
                      className="w-100 py-2 fw-semibold"
                      disabled={
                        !formData.quantity ||
                        !formData.beneficiaryName ||
                        !formData.retirementMessage
                      }
                    >
                      Confirm Purchase
                    </Button>
                  </div>
                </Form>
              </Card.Body>
            </Card>

            {/* After purchase info */}
            <Accordion>
              <Accordion.Item eventKey="1">
                <Accordion.Header>After Purchase</Accordion.Header>
                <Accordion.Body>
                  <ul className="mb-0">
                    <li>Payment is processed securely.</li>
                    <li>Retirement is recorded on-chain.</li>
                    <li>You receive a verified PDF certificate.</li>
                    <li>Credits are permanently retired.</li>
                  </ul>
                </Accordion.Body>
              </Accordion.Item>
            </Accordion>
          </Col>

          {/* RIGHT SUMMARY */}
          <Col lg={4}>
            <ProjectSummary
              totalPrice={totalPrice}
              quantity={formData.quantity}
              pricePerTonne={pricePerTonne}
              title={credit.title}
            />
          </Col>
        </Row>
      </Container>
    </div>
  );
}

function ProjectSummary({ totalPrice, quantity, pricePerTonne, title }) {
  return (
    <div className="sticky-top" style={{ top: "80px" }}>
      <Card className="shadow-sm border-0">
        <Card.Body>
          <Card.Title>{title || "Carbon Credit"}</Card.Title>
          <Row className="mt-3">
            <Col xs={6} className="text-muted">
              Quantity
            </Col>
            <Col xs={6} className="text-end">
              {quantity || 0} tCO₂
            </Col>
          </Row>
          <Row>
            <Col xs={6} className="text-muted">
              Price per tonne
            </Col>
            <Col xs={6} className="text-end">
              ${pricePerTonne.toFixed(2)}
            </Col>
          </Row>
          <hr />
          <Row>
            <Col xs={6} className="text-muted">
              Total
            </Col>
            <Col xs={6} className="text-end fw-bold text-success">
              ${totalPrice}
            </Col>
          </Row>
        </Card.Body>
      </Card>
    </div>
  );
}
