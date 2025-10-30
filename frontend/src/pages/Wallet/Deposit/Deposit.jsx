import { Formik } from "formik";
import { Modal, Button, Form } from "react-bootstrap";
import * as Yup from "yup";

const schema = Yup.object().shape({
  amount: Yup.number()
    .typeError("Amount must be a number")
    .positive("Amount must be greater than 0")
    .required("Amount is required"),
  paymentMethod: Yup.string().required("Payment method is required"),
});

export default function Deposit({ show, onHide, onSubmit }) {
  return (
    <Modal show={show} onHide={onHide} centered>
      <Modal.Header closeButton>
        <Modal.Title className="fw-bold text-center w-100">
          Top Up Your Wallet
        </Modal.Title>
      </Modal.Header>

      <Formik
        validationSchema={schema}
        initialValues={{ amount: "", paymentMethod: "" }}
        onSubmit={(values) => onSubmit(values)}
      >
        {({
          handleSubmit,
          handleChange,
          handleBlur,
          values,
          errors,
          touched,
        }) => (
          <Form noValidate onSubmit={handleSubmit}>
            <Modal.Body>
              {/*amount*/}
              <Form.Group className="mb-3">
                <Form.Label>Enter Amount</Form.Label>
                <Form.Control
                  type="number"
                  name="amount"
                  placeholder="Enter amount"
                  value={values.amount}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  isInvalid={touched.amount && !!errors.amount}
                />
                <Form.Control.Feedback type="invalid">
                  {errors.amount}
                </Form.Control.Feedback>
              </Form.Group>

              {/*payment method */}
              <Form.Group className="mb-3">
                <Form.Label>Select Payment Method</Form.Label>
                <Form.Group className="d-flex gap-5">
                  <Form.Check
                    type="radio"
                    id="STRIPE"
                    name="paymentMethod"
                    label="STRIPE"
                    value="STRIPE"
                    checked={values.paymentMethod === "STRIPE"}
                    onChange={handleChange}
                  />
                  <Form.Check
                    type="radio"
                    id="PAYPAL"
                    name="paymentMethod"
                    label="PAYPAL"
                    value="PAYPAL"
                    checked={values.paymentMethod === "PAYPAL"}
                    onChange={handleChange}
                  />

                  {touched.paymentMethod && errors.paymentMethod && (
                    <div className="text-danger small mt-1">
                      {errors.paymentMethod}
                    </div>
                  )}
                </Form.Group>
              </Form.Group>
            </Modal.Body>

            <Modal.Footer>
              <Button variant="secondary" onClick={onHide}>
                Close
              </Button>
              <Button type="submit" variant="primary">
                Submit
              </Button>
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
}
