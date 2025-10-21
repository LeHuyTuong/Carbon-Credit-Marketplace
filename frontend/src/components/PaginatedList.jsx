import React, { useState, useEffect } from "react";
import { Pagination } from "react-bootstrap";

export default function PaginatedList({
  fetchData, // async function (page, size) => Promise<{ items, total }>
  renderItem, // render từng dòng
  pageSize = 10,
  deps = [], // dependencies để reload khi thay đổi
}) {
  const [page, setPage] = useState(1);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadPage(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, ...deps]);

  const loadPage = async (p) => {
    setLoading(true);
    try {
      const { items, total } = await fetchData(p, pageSize);
      setItems(items);
      setTotal(total);
    } catch (err) {
      console.error("Pagination load error:", err);
    } finally {
      setLoading(false);
    }
  };

  const totalPages = Math.ceil(total / pageSize);

  return (
    <div>
      {loading ? (
        <div className="text-light text-center py-3">Loading...</div>
      ) : items.length === 0 ? (
        <div className="text-light text-center py-3">No records found</div>
      ) : (
        <>
          <div className="list-content">{items.map(renderItem)}</div>
          {totalPages > 1 && (
            <Pagination className="justify-content-center mt-3">
              <Pagination.Prev
                disabled={page === 1}
                onClick={() => setPage(page - 1)}
              />
              {[...Array(totalPages)].map((_, i) => (
                <Pagination.Item
                  key={i + 1}
                  active={i + 1 === page}
                  onClick={() => setPage(i + 1)}
                >
                  {i + 1}
                </Pagination.Item>
              ))}
              <Pagination.Next
                disabled={page === totalPages}
                onClick={() => setPage(page + 1)}
              />
            </Pagination>
          )}
        </>
      )}
    </div>
  );
}
