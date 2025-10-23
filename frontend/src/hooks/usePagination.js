import { useState, useMemo } from "react";

export default function usePagination(items = [], itemsPerPage = 5) {
    // Trang hiện tại (bắt đầu từ 1)
    const [currentPage, setCurrentPage] = useState(1);
  // Tổng số trang = làm tròn lên tổng item / item mỗi trang
  const totalPages = Math.ceil(items.length / itemsPerPage);

  // Dữ liệu của trang hiện tại
  const currentItems = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return items.slice(start, start + itemsPerPage);
  }, [items, currentPage, itemsPerPage]);

    // Chuyển trang có kiểm tra giới hạn
  const goToPage = (page) => {
    if (page < 1 || page > totalPages) return;
    setCurrentPage(page);
  };

    // Trả về dữ liệu và hành vi để component bên ngoài dùng
  return { currentPage, totalPages, currentItems, goToPage };
}
