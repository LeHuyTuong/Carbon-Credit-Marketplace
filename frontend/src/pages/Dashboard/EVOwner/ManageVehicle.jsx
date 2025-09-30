import { useState } from "react";
import './ManageVehicle/manage.css'

const data = [
  { id: 1, code: "EV-0001", plate: "30H-123.45", brand: "VinFast", model: "VF e34", year: 2022, battery: "42kWh", aggregator: "Aggregator_A", status: "Pending", document: "Uploaded" },
  { id: 2, code: "EV-0002", plate: "30H-123.45", brand: "VinFast", model: "VF e34", year: 2022, battery: "42kWh", aggregator: "Aggregator_A", status: "Rejected", document: "Uploaded" },
  { id: 3, code: "EV-0003", plate: "30H-123.45", brand: "VinFast", model: "VF e34", year: 2022, battery: "42kWh", aggregator: "Aggregator_A", status: "Pending", document: "Uploaded" },
  { id: 4, code: "EV-0004", plate: "30H-123.45", brand: "VinFast", model: "VF e34", year: 2022, battery: "42kWh", aggregator: "Aggregator_A", status: "Approved", document: "Uploaded" }
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

  const getStatusClass = (status) => {
    switch (status) {
      case "Pending": return "status-badge pending";
      case "Approved": return "status-badge approved";
      case "Rejected": return "status-badge rejected";
      default: return "status-badge";
    }
  };

  return (
    <>
      {/*search section */}
      <div className="vehicle-search-section">
        <h1 className="title">Your vehicles</h1>
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
              <th>Year of manufacture</th>
              <th>Battery capacity</th>
              <th>Aggregator</th>
              <th>Status</th>
              <th>Vehicle documents</th>
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
                  <td>{row.year}</td>
                  <td>{row.battery}</td>
                  <td>{row.aggregator}</td>
                  <td><span className={getStatusClass(row.status)}>{row.status}</span></td>
                  <td>{row.document}</td>
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
