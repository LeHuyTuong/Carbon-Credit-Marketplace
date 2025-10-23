import { Pagination } from "react-bootstrap";
import usePagination from "../../hooks/usePagination";

export default function PaginatedTable({
  items,
  itemsPerPage,
  renderRow,
  renderEmpty, // thêm prop cho phần hiển thị khi không có dữ liệu
}) {
  const { currentItems, currentPage, totalPages, goToPage } = usePagination(
    items,
    itemsPerPage
  );

  // Nếu rỗng thì dùng renderEmpty (tuỳ bảng nào)
  if (!items || items.length === 0) {
    return (
      <tr>
        <td colSpan="100%" className="text-center py-5">
          {renderEmpty ? (
            renderEmpty()
          ) : (
            <p className="text-muted mb-0">No data available</p>
          )}
        </td>
      </tr>
    );
  }

  return (
    <>
      {currentItems.map(renderRow)}

      {totalPages > 1 && (
        <tr>
          <td colSpan="100%" className="text-center pt-3">
            <Pagination className="justify-content-center">
              <Pagination.Prev
                disabled={currentPage === 1}
                onClick={() => goToPage(currentPage - 1)}
              />
              {Array.from({ length: totalPages }, (_, i) => (
                <Pagination.Item
                  key={i + 1}
                  active={i + 1 === currentPage}
                  onClick={() => goToPage(i + 1)}
                >
                  {i + 1}
                </Pagination.Item>
              ))}
              <Pagination.Next
                disabled={currentPage === totalPages}
                onClick={() => goToPage(currentPage + 1)}
              />
            </Pagination>
          </td>
        </tr>
      )}
    </>
  );
}
