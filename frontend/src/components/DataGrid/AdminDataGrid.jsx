// src/components/DataGrid/AdminDataGrid.jsx
import { DataGrid } from "@mui/x-data-grid";

const AdminDataGrid = ({ rows, columns, getRowId }) => {
    const displayRows = Array.isArray(rows) ? [...rows].reverse() : [];
  return (
    <DataGrid
      rows={displayRows}
      columns={columns}
      getRowId={getRowId}
      pagination
      initialState={{
        pagination: { paginationModel: { pageSize:  10, page: 0 } },
      }}
      pageSizeOptions={[ 10, 20, 50]}
      sx={{
        "& .MuiDataGrid-footerContainer": {
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          height: "56px",
          padding: "0 16px",
        },
        "& .MuiTablePagination-displayedRows, & .MuiTablePagination-selectLabel": {
          margin: 0,
          lineHeight: "1.5rem",
        },
        "& .MuiTablePagination-actions": {
          marginRight: "4px",
        },
      }}
    />
  );
};

export default AdminDataGrid;
