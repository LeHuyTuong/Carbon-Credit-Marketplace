import React, { useState } from "react";
import { Link } from "react-router-dom";

export default function KYC() {
  const [formData, setFormData] = useState({
    fullName: "",
    idNumber: "",
    birthday: "",
    gender: "",
    placeOfIssue: "",
    issueDate: ""
  });

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("Form Data:", formData);
  };

  return (
    <div className="auth-hero min-vh-100 d-flex align-items-center justify-content-center">
      <div className="card shadow container">
        <div className="card-body">
          <h2 className="mb-4">Identity Verification (KYC) Form</h2>
          <form onSubmit={handleSubmit}>
            
            {/*name */}
            <div className="row mb-3">
              <div className="col-md-6">
                <label className="form-label">Full Name</label>
                <input
                  type="text"
                  className="form-control"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  placeholder="Enter first name"
                />
              </div>
            {/*id */}
              <div className="col-md-6">
                <label className="form-label">ID Number</label>
                <input
                  type="text"
                  className="form-control"
                  name="idNumber"
                  value={formData.idNumber}
                  onChange={handleChange}
                  placeholder="Enter ID number"
                />
              </div>
            </div>

            {/*birthday, gender */}
            <div className="row mb-3">
              <div className="col-md-6">
                <label className="form-label">Birthday</label>
                <input
                  type="date"
                  className="form-control"
                  name="birthday"
                  value={formData.birthday}
                  onChange={handleChange}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label d-block">Gender</label>
                <div className="form-check form-check-inline">
                  <input
                    className="form-check-input"
                    type="radio"
                    name="gender"
                    value="male"
                    checked={formData.gender === "male"}
                    onChange={handleChange}
                  />
                  <label className="form-check-label">Male</label>
                </div>
                <div className="form-check form-check-inline">
                  <input
                    className="form-check-input"
                    type="radio"
                    name="gender"
                    value="female"
                    checked={formData.gender === "female"}
                    onChange={handleChange}
                  />
                  <label className="form-check-label">Female</label>
                </div>
              </div>
            </div>

            {/*issue info*/}
            <div className="row mb-3">
              <div className="col-md-6">
                <label className="form-label">Place Of Issue</label>
                <input
                  type="text"
                  className="form-control"
                  name="placeOfIssue"
                  value={formData.placeOfIssue}
                  onChange={handleChange}
                  placeholder="Enter place of issue"
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Issue Date</label>
                <input
                  type="text"
                  className="form-control"
                  name="issueDate"
                  value={formData.issueDate}
                  onChange={handleChange}
                  placeholder="Enter issue date"
                />
              </div>
            </div>

            {/*btn submit */}
            <Link to='/login'>
              <button type="submit" className="btn btn-primary float-end">
                Submit
              </button>
            </Link>
          </form>
        </div>
      </div>
    </div>
  );
}