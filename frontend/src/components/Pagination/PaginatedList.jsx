import { Button, Pagination } from "react-bootstrap";
import usePagination from "../../hooks/usePagination";

export default function PaginatedList({
  items,
  itemsPerPage,
  renderItem,
  useGrid = false, // thêm prop để bật/tắt grid layout
}) {
  const { currentItems, currentPage, totalPages, goToPage } = usePagination(
    items,
    itemsPerPage
  );

  return (
    <div>
      {/* Danh sách phần tử */}
      <div
        className={
          useGrid ? "paginated-grid" : "row g-4 justify-content-center"
        }
      >
        {currentItems.map(renderItem)}
      </div>

      {/* Thanh phân trang */}
      {totalPages > 1 && (
        <div
          className="d-flex justify-content-center align-items-center mt-4"
          style={{ width: "100%" }}
        >
          <Pagination
            className="shadow-sm"
            style={{
              background: "rgba(255,255,255,0.95)",
              borderRadius: "10px",
              padding: "6px 14px",
              boxShadow: "0 2px 6px rgba(0,0,0,0.1)",
              display: "flex",
              justifyContent: "center",
            }}
          >
            <Pagination.Prev
              disabled={currentPage === 1}
              onClick={() => goToPage(currentPage - 1)}
            />
            {Array.from({ length: totalPages }, (_, i) => (
              <Pagination.Item
                key={i + 1}
                active={i + 1 === currentPage}
                onClick={() => goToPage(i + 1)}
                style={{
                  minWidth: "38px",
                  textAlign: "center",
                  fontWeight: 500,
                }}
              >
                {i + 1}
              </Pagination.Item>
            ))}
            <Pagination.Next
              disabled={currentPage === totalPages}
              onClick={() => goToPage(currentPage + 1)}
            />
          </Pagination>
        </div>
      )}
    </div>
  );
}
