import { useEffect, useState } from "react";
import { Container, Table, Accordion, Spinner } from "react-bootstrap";
import { apiFetch } from "../../../../utils/apiFetch";

export default function EVOwnerList() {
  const [owners, setOwners] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  //gọi API để lấy danh sách tổng hợp xe của công ty
  const fetchCompanySummary = async () => {
    try {
      setLoading(true); // bật loading khi bắt đầu fetch
      setError(""); // reset lỗi cũ nếu có
      //gọiAPI backend để lấy dữ liệu EV Owners & Vehicles
      const res = await apiFetch("/api/v1/vehicles/company/summary", {
        method: "GET",
      });

      // lấy mảng response
      const data = res?.response || [];
      //gán dữ liệu vào state "owners"
      setOwners(data);
    } catch (err) {
      console.error("Error fetching company summary:", err);
      setError(err.message || "Failed to fetch vehicle summary.");
    } finally {
      //tắt trạng thái loading
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCompanySummary();
  }, []);

  return (
    <div className="auth-hero min-vh-100 py-4">
      <Container>
        {/*tiêu đề trang */}
        <h1 className="text-light fw-bold mb-2">EV Owners & Vehicle Summary</h1>
        <p className="text-light mb-4">
          Overview of all EV owners under this company and their registered
          vehicles.
        </p>

        {/*trạng thái đang tải */}
        {loading ? (
          <div className="text-center mt-5">
            <Spinner animation="border" variant="light" />
          </div>
        ) : owners.length === 0 ? (
          // khi không có dữ liệu (owners trống)
          <p className="text-light text-center">No owners or vehicles found.</p>
        ) : (
          //khi có dữ liệu → hiển thị dưới dạng accordion
          <Accordion className="mt-3">
            {owners.map((owner, idx) => (
              <Accordion.Item eventKey={idx.toString()} key={owner.ownerId}>
                {/*header accordion: hiển thị tên chủ sở hữu, email, số xe */}
                <Accordion.Header>
                  <div className="w-100 d-flex justify-content-between align-items-center">
                    <span className="fw-bold text-dark">{owner.fullName}</span>
                    <small className="text-muted">
                      {owner.email} | {owner.vehicleCount} vehicle
                      {owner.vehicleCount !== 1 ? "s" : ""}
                    </small>
                  </div>
                </Accordion.Header>

                {/*body accordion: hiển thị danh sách xe của chủ sở hữu */}
                <Accordion.Body>
                  <Table striped bordered hover responsive size="sm">
                    <thead className="table-light">
                      <tr>
                        <th>#</th>
                        <th>Plate Number</th>
                        <th>Brand</th>
                        <th>Model</th>
                      </tr>
                    </thead>
                    <tbody>
                      {/*lặp qua danh sách xe của từng chủ sở hữu */}
                      {owner.vehicles.map((v, i) => (
                        <tr key={v.id}>
                          <td>{i + 1}</td>
                          <td>{v.plateNumber}</td>
                          <td>{v.brand}</td>
                          <td>{v.model}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </Accordion.Body>
              </Accordion.Item>
            ))}
          </Accordion>
        )}
      </Container>
    </div>
  );
}
