import { useState } from "react";
import './manage.css'
import { Button, Modal, Form } from 'react-bootstrap';

export default function Manage() {

  const [vehicles, setVehicles] = useState([
    { id: 1, code: "EV-0001", plate: "30H-123.45", brand: "VinFast", model: "VF e34", company: "Company_A", credits: "0.2"},
    { id: 2, code: "EV-0002", plate: "30H-123.45", brand: "VinFast", model: "VF e34", company: "Company_A", credits: "0.2"},
    { id: 3, code: "EV-0003", plate: "30H-123.45", brand: "VinFast", model: "VF e34", company: "Company_A", credits: "0.2"},
    { id: 4, code: "EV-0004", plate: "30H-123.45", brand: "VinFast", model: "VF e34", company: "Company_A", credits: "0.2"}
  ])

  //thêm xe mới
  const addVehicle = (vehicle) => {
    setVehicles(prev => [...prev, { ...vehicle, id: prev.length + 1 }]);
  };

  return (
    <>
      <div className="vehicle-search-section">
        <h1 className="title">Your vehicles</h1>
        <RegistrationForm onAdd={addVehicle}/>
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
              <th>Company</th>
              <th>Credits</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.length > 0 ? (
              vehicles.map((row) => (
                <tr key={row.id}>
                  <td>{row.code}</td>
                  <td>{row.plate}</td>
                  <td>{row.brand}</td>
                  <td>{row.model}</td>
                  <td>{row.company}</td>
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
                <td colSpan="10" className="no-data">
                  <h5>No vehicles yet</h5>
                  <p>Add your vehicle to get started.</p>
                  </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

//form đăng ký xe
function RegistrationForm({onAdd}){
  const [show, setShow] = useState(false)
  const [formData, setFormData] =useState({
    plate: '', brand: '',
    model: '', company: '',
    credits: '', code: ''
  })

  const handelSetOpen = () => setShow(true)
  const handelSetClose = () => setShow(false)

  const handleChange = (e) => {
    const {name, value } = e.target
    setFormData(pre => ({ ...pre, [name]: value }))
  }

  const handleSubmit = () => {
    //auto generate code xe
    const vehicle = {
      ...formData,
      code: "EV-" + String(Math.floor(Math.random() * 10000)).padStart(4, '0')
    }
    onAdd(vehicle); //gọi cha
    handelSetClose();
    setFormData({ plate: '', brand: '', model: '', company: '', credits: '0.0', code: '' })
  }

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
            <Form.Group className="mb-3" controlId="formPlate">
              <Form.Label>License Plate</Form.Label>
              <Form.Control 
                type="text" 
                placeholder="Enter License Plate" 
                name="plate"
                value={formData.plate}
                onChange={handleChange}
                />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formBrand">
              <Form.Label>Car Brand</Form.Label>
              <Form.Control 
                type="text" 
                placeholder="Enter brand" 
                name="brand"
                value={formData.brand}
                onChange={handleChange}
                />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formModel">
              <Form.Label>Car Model</Form.Label>
              <Form.Control 
                type="text" 
                placeholder="Enter model" 
                name="model"
                value={formData.model}
                onChange={handleChange}
                />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formCompany">
              <Form.Label>Company</Form.Label>
              <Form.Select 
                name="company"
                value={formData.company}
                onChange={handleChange}
              >
                <option>Choose one company</option>
                <option value="1">Vinfast</option>
                <option value="2">Tesla</option>
              </Form.Select>
            </Form.Group>

          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handelSetClose}>Close</Button>
          <Button variant="primary" onClick={handleSubmit}>Submit</Button>
        </Modal.Footer>
{/* 
        <Formik onSubmit={() => {}} initialValues={{}}> 
            {() => <div>as</div>} 
        </Formik> */}

      </Modal>
    </>
  )
}
