import { useState } from "react";
import './manage.css'
import { Button, Modal, Form } from 'react-bootstrap';

const data = [
  { id: 1, code: "EV-0001", plate: "30H-123.45", brand: "VinFast", model: "VF e34", aggregator: "Aggregator_A", credits: "0.2"},
  { id: 2, code: "EV-0002", plate: "30H-123.45", brand: "VinFast", model: "VF e34", aggregator: "Aggregator_A", credits: "0.2"},
  { id: 3, code: "EV-0003", plate: "30H-123.45", brand: "VinFast", model: "VF e34", aggregator: "Aggregator_A", credits: "0.2"},
  { id: 4, code: "EV-0004", plate: "30H-123.45", brand: "VinFast", model: "VF e34", aggregator: "Aggregator_A", credits: "0.2"}
];

export default function Manage() {
  const [search, setSearch] = useState("");

  const filteredData = data.filter(
    (row) =>
      row.code.toLowerCase().includes(search.toLowerCase()) ||
      row.plate.toLowerCase().includes(search.toLowerCase()) ||
      row.brand.toLowerCase().includes(search.toLowerCase()) ||
      row.model.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <>
      {/*search section */}
      <div className="vehicle-search-section">
        <h1 className="title">Your vehicles</h1>
        <RegistrationForm/>
        <div className="search-bar">
          <select className="search-dropdown">
            <option value="all">All</option>
            <option value="vinfast">VinFast</option>
            <option value="tesla">Tesla</option>
          </select>
          <input
            type="text"
            className="search-input"
            placeholder="Search vehicle"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button className="search-btn">
            <i className="bi bi-search"></i>
          </button>
        </div>
      </div>

      {/*table Section */}
      <div className="table-wrapper">
        <table className="vehicle-table">
          <thead>
            <tr>
              <th>Vehicle ID</th>
              <th>License Plate</th>
              <th>Brand</th>
              <th>Model</th>
              <th>Aggregator</th>
              <th>Credits</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredData.length > 0 ? (
              filteredData.map((row) => (
                <tr key={row.id}>
                  <td>{row.code}</td>
                  <td>{row.plate}</td>
                  <td>{row.brand}</td>
                  <td>{row.model}</td>
                  <td>{row.aggregator}</td>
                  <td>{row.credits}</td>
                  <td className="action-buttons">
                    <button className="action-btn view"><i className="bi bi-eye"></i></button>
                    <button className="action-btn edit"><i className="bi bi-pencil"></i></button>
                    <button className="action-btn delete"><i className="bi bi-trash"></i></button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="10" className="no-data">No vehicle found</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

//form đăng ký xe
function RegistrationForm(){
  const [show, setShow] = useState(false)

  const handelSetOpen = () => setShow(true)
  const handelSetClose = () => setShow(false)

  const verify = async () => {
    //call API verify OTP
    nav('/managevehicle')
  };

  return (
    <>
      <Button className='' onClick={handelSetOpen} style={{marginBottom: '30px'}} >Add Vehicle</Button>
      <Modal show={show} onHide={handelSetClose}>
        <Modal.Header closeButton>
            <Modal.Title>ELECTRIC VEHICLE REGISTRATION</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3" controlId="formBasicEmail">
              <Form.Label>License Plate</Form.Label>
              <Form.Control type="text" placeholder="Enter License Plate" />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formBasicPassword">
              <Form.Label>Car Brand</Form.Label>
              <Form.Control type="text" placeholder="Enter brand" />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formBasicPassword">
              <Form.Label>Car Model</Form.Label>
              <Form.Control type="text" placeholder="Enter model" />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formBasicPassword">
              <Form.Label>Aggregator</Form.Label>
              <Form.Select aria-label="Default select example">
                <option>Choose one aggregator</option>
                <option value="1">Vinfast</option>
                <option value="2">Tesla</option>
              </Form.Select>
            </Form.Group>

          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handelSetClose}>Close</Button>
          <Button variant="primary">Submit</Button>
        </Modal.Footer>
{/* 
        <Formik onSubmit={() => {}} initialValues={{}}> 
            {() => <div>as</div>} 
        </Formik> */}

      </Modal>
    </>
  )
}
