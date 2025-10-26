// src/components/DataGrid/AdminDataGrid.jsx
import { DataGrid } from "@mui/x-data-grid";

const CVADataGrid = ({ rows, columns, getRowId }) => {
  return (
    <DataGrid
      rows={rows}
      columns={columns}
      getRowId={getRowId}
      checkboxSelection
      pagination
      initialState={{
        pagination: { paginationModel: { pageSize: 10, page: 0 } },
      }}
      pageSizeOptions={[10, 20, 50]}
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

export default CVADataGrid;
