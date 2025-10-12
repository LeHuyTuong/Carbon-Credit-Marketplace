import { Box, Typography, useTheme, Divider, Chip, Button } from "@mui/material";
import { tokens } from "@/theme";
import { useParams, useNavigate } from "react-router-dom";
import { mockDataInvoices } from "@/data/mockData";
import Header from "@/components/Chart/Header.jsx";
import { useState } from "react";

const ViewTransaction = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const { id } = useParams();
  const navigate = useNavigate();

  // tìm giao dịch
  const transaction = mockDataInvoices.find((item) => item.id === Number(id));

  const [editMode, setEditMode] = useState(false);
  const [newStatus, setNewStatus] = useState(transaction?.status || "");
  const [message, setMessage] = useState("");

  if (!transaction) {
    return (
      <Box m="20px">
        <Header title="TRANSACTION DETAIL" subtitle="Transaction not found" />
        <Typography variant="h6" color={colors.redAccent[400]}>
          Transaction not found with ID: {id}
        </Typography>
        <Button
          variant="contained"
          sx={{ mt: 2, backgroundColor: colors.blueAccent[700] }}
          onClick={() => navigate(-1)}
        >
          Quay lại
        </Button>
      </Box>
    );
  }

  const statusColorMap = {
    paid: colors.greenAccent[400],
    pending: colors.grey[300],
    failed: colors.redAccent[400],
  };

  const typeMap = {
    buy: "Buy carbon credits",
    sell: "Sell carbon credits",
    withdraw: "Withdraw funds",
  };

  return (
    <Box m="20px">
      <Header title="TRANSACTION DETAIL" subtitle="Transaction details" />

      <Box
        sx={{
          mt: 3,
          p: 3,
          borderRadius: "12px",
          backgroundColor: colors.primary[400],
          boxShadow: 3,
          maxWidth: "600px",
          mx: "auto",
        }}
      >
        <Typography variant="h6" gutterBottom color={colors.greenAccent[300]}>
          Transaction ID: {transaction.transactionid}
        </Typography>
        <Divider sx={{ mb: 2, borderColor: colors.grey[700] }} />

        <Box display="flex" flexDirection="column" gap={1.5}>
          <Typography>
            <b>Transaction Type:</b>{" "}
            <Typography component="span" color={colors.blueAccent[300]}>
              {typeMap[transaction.transactionType] || "Unknown"}
            </Typography>
          </Typography>

          <Typography>
            <b>Trader:</b>{" "}
            <Typography component="span" color={colors.grey[100]}>
              {transaction.trader}
            </Typography>
          </Typography>

          <Typography>
            <b>Email:</b>{" "}
            <Typography component="span" color={colors.grey[100]}>
              {transaction.email}
            </Typography>
          </Typography>

          <Typography>
            <b>Transaction Date:</b>{" "}
            <Typography component="span" color={colors.grey[100]}>
              {transaction.date}
            </Typography>
          </Typography>

          <Typography>
            <b>Carbon Credits (tCO₂):</b>{" "}
            <Typography component="span" color={colors.grey[100]}>
              100
            </Typography>
          </Typography>

          <Typography>
            <b>Unit Price (USD / tCO₂):</b>{" "}
            <Typography component="span" color={colors.grey[100]}>
              ${(transaction.cost / 100).toFixed(2)}
            </Typography>
          </Typography>

          <Typography>
            <b>Total Value (USD):</b>{" "}
            <Typography component="span" color={colors.greenAccent[400]}>
              ${transaction.cost}
            </Typography>
          </Typography>

          <Typography>
            <b>Status:</b>{" "}
            {editMode ? (
              <select
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value)}
                style={{
                  backgroundColor: colors.primary[500],
                  color: "#fff",
                  borderRadius: "6px",
                  padding: "4px 8px",
                  border: "1px solid " + colors.grey[600],
                }}
              >
                <option value="pending">Pending</option>
                <option value="paid">Paid</option>
                <option value="failed">Failed</option>
              </select>
            ) : (
              <Chip
                label={transaction.status}
                sx={{
                  backgroundColor:
                    statusColorMap[transaction.status] || colors.grey[500],
                  color: "#fff",
                  textTransform: "capitalize",
                  fontWeight: "bold",
                }}
              />
            )}
          </Typography>
        </Box>

        {/* Nút Edit / Update */}
        <Box display="flex" justifyContent="center" gap={2} mt={3}>
          {!editMode ? (
            <>
              <Button
                variant="contained"
                sx={{
                  backgroundColor: colors.blueAccent[700],
                  borderRadius: "8px",
                  px: 4,
                  fontWeight: 600,
                }}
                onClick={() => setEditMode(true)}
              >
                Edit Status
              </Button>
              <Button
                variant="outlined"
                sx={{
                  color: colors.grey[100],
                  borderColor: colors.grey[500],
                }}
                onClick={() => navigate(-1)}
              >
                Back
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="contained"
                color="success"
                onClick={() => {
                  transaction.status = newStatus;
                  setEditMode(false);
                  setMessage(" Status update successful!");
                  setTimeout(() => setMessage(""), 3000);
                }}
              >
                Update
              </Button>
              <Button
                variant="outlined"
                color="error"
                onClick={() => {
                  setEditMode(false);
                  setNewStatus(transaction.status);
                }}
              >
                Cancel
              </Button>
            </>
          )}
        </Box>

        {message && (
          <Typography
            align="center"
            mt={2}
            color={colors.greenAccent[400]}
            fontWeight="bold"
          >
            {message}
          </Typography>
        )}
      </Box>
    </Box>
  );
};

export default ViewTransaction;
