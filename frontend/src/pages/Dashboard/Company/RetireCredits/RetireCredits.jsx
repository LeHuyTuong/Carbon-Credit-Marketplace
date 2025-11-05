import React, { useEffect, useState, useRef } from "react";
import { Button, Card, Spinner, Table, Form } from "react-bootstrap";
import useWalletData from "../../../Wallet/components/useWalletData";
import { useNavigate } from "react-router-dom";
import useReveal from "../../../../hooks/useReveal";
import PaginatedTable from "../../../../components/Pagination/PaginatedTable";

export default function RetireCredits() {
  //hook chứa các hàm thao tác với wallet & credits
  const { fetchRetirableCredits, retireCredits, loading } = useWalletData();
  //state quản lý danh sách credit, lựa chọn, toast message
  const [credits, setCredits] = useState([]);
  const [selectedList, setSelectedList] = useState([]); // [{id, quantity}]
  const [toast, setToast] = useState({ show: false, msg: "", type: "" });
  const [statusFilter, setStatusFilter] = useState("");
  const nav = useNavigate();
  const sectionRef = useRef(null);
  useReveal(sectionRef);

  // gọi API lấy danh sách credits đủ điều kiện retire
  const loadCredits = async () => {
    const data = await fetchRetirableCredits(
      statusFilter ? { status: statusFilter } : {}
    );
    setCredits(data || []);
  };

  //mỗi khi filter đổi, load lại danh sách
  useEffect(() => {
    loadCredits();
  }, [statusFilter]);

  // tự động ẩn toast sau 3 giây
  useEffect(() => {
    if (toast.show) {
      const timer = setTimeout(() => {
        setToast((prev) => ({ ...prev, show: false }));
      }, 3000); // 3 giây

      return () => clearTimeout(timer);
    }
  }, [toast.show]);

  //chọn/ bỏ chọn credit theo batch
  const handleSelect = (batchCode, batchId, creditIds) => {
    setSelectedList((prev) => {
      const exists = prev.find((x) => x.batchCode === batchCode);
      if (exists) {
        return prev.filter((x) => x.batchCode !== batchCode);
      } else {
        return [...prev, { batchCode, batchId, creditIds, quantity: 1 }];
      }
    });
  };

  //số lượng retire
  const handleQuantityChange = (batchCode, value) => {
    setSelectedList((prev) =>
      prev.map((x) =>
        x.batchCode === batchCode
          ? { ...x, quantity: value === "" ? "" : Number(value) }
          : x
      )
    );
  };

  //gửi yêu cầu retire
  const handleRetire = async () => {
    try {
      if (!selectedList.length) return;

      // validate quantity > 0
      const invalid = selectedList.some((x) => {
        const found = credits.find((c) => c.batchCode === x.batchCode);
        return (
          !x.quantity ||
          x.quantity <= 0 ||
          x.quantity > (found?.availableAmount || 0)
        );
      });
      if (invalid) {
        setToast({
          show: true,
          msg: "Please enter valid quantity for all selected credits.",
          type: "danger",
        });
        return;
      }

      //gọi API retire từng credit
      await retireCredits(selectedList);
      //thông báo thành công
      setToast({
        show: true,
        msg: "Credits retired successfully!",
        type: "success",
      });
      //reset lựa chọn và reload list
      setSelectedList([]);
      loadCredits();
    } catch (err) {
      setToast({
        show: true,
        msg: err.message || "Failed to retire credits",
        type: "danger",
      });
    }
  };

  //kiểm tra credit đang chọn
  const isSelected = (batchCode) =>
    selectedList.some((x) => x.batchCode === batchCode);
  // lấy quantity đã nhập theo batch
  const getQuantity = (batchCode) => {
    const found = selectedList.find((x) => x.batchCode === batchCode);
    return found?.quantity ?? "";
  };

  return (
    <div
      ref={sectionRef}
      className="auth-hero min-vh-100 d-flex flex-column align-items-center justify-content-start py-5 reveal"
    >
      {/* header + nút xem lịch sử */}
      <div
        className="container"
        style={{ maxWidth: "1100px", marginTop: "4rem" }}
      >
        <div className="d-flex justify-content-between align-items-center mb-5">
          <h2 className="fw-bold text-white mb-0 text-shadow">
            Retire My Carbon Credits
          </h2>
          <Button
            variant="outline-info"
            className="fw-semibold shadow-sm rounded-3"
            onClick={() => nav("/retired-history")}
            style={{
              borderRadius: "10px",
              background: "rgba(255, 255, 255, 0.85)",
              backdropFilter: "blur(6px)",
              zIndex: 20,
            }}
          >
            View Retired History
          </Button>
        </div>

        {/* thẻ hiển thị bảng credits */}
        <Card
          className="shadow-lg border-0 p-3"
          style={{
            borderRadius: "15px",
            background: "rgba(255,255,255,0.9)",
            backdropFilter: "blur(10px)",
          }}
        >
          {/* đang loading */}
          {loading ? (
            <div className="d-flex justify-content-center align-items-center py-5">
              <Spinner animation="border" />
            </div>
          ) : credits.length === 0 ? (
            // không có credits nào khả dụng
            <div className="text-center py-5">
              <h5>No available credits to retire.</h5>
              <p className="text-muted mb-0">
                Purchase or receive carbon credits before retiring them.
              </p>
            </div>
          ) : (
            // render bảng credits
            <>
              <Table hover responsive className="align-middle mb-3">
                <thead className="table-light">
                  <tr>
                    <th></th>
                    <th>Batch Code</th>
                    <th>Project</th>
                    <th>Vintage Year</th>
                    <th>Available</th>
                    <th>Quantity to Retire</th>
                  </tr>
                </thead>

                <PaginatedTable
                  items={credits}
                  itemsPerPage={5}
                  renderRow={(b) => (
                    <tr key={b.batchCode}>
                      <td>
                        <input
                          type="checkbox"
                          checked={isSelected(b.batchCode)}
                          onChange={() =>
                            handleSelect(b.batchCode, b.batchId, b.creditIds)
                          }
                        />
                      </td>
                      <td>{b.batchCode}</td>
                      <td>{b.projectTitle}</td>
                      <td>{b.vintageYear}</td>
                      <td>{b.availableAmount}</td>
                      <td>
                        <Form.Control
                          type="number"
                          min="1"
                          disabled={!isSelected(b.batchCode)}
                          value={getQuantity(b.batchCode)}
                          onChange={(e) =>
                            handleQuantityChange(b.batchCode, e.target.value)
                          }
                          style={{ width: "90px" }}
                        />
                      </td>
                    </tr>
                  )}
                />
              </Table>

              {/* nút retire */}
              <div className="text-end">
                <Button
                  variant="success"
                  className="fw-semibold px-4 rounded-3"
                  disabled={!selectedList.length || loading}
                  onClick={handleRetire}
                >
                  Retire Selected ({selectedList.length})
                </Button>
              </div>
            </>
          )}
        </Card>
      </div>

      {toast.show && (
        <div
          className={`alert alert-${toast.type} position-fixed bottom-0 end-0 m-4 shadow`}
          style={{ minWidth: "300px", zIndex: 100 }}
        >
          {toast.msg}
        </div>
      )}
    </div>
  );
}
