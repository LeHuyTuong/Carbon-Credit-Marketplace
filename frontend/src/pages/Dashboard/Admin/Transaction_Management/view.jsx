// src/pages/Dashboard/Admin/Transaction_Management/ViewTransaction.jsx
import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { Box, Typography, Divider, Chip, Button, useTheme } from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import { processWithdrawal, getWithdrawalsAdmin } from "@/apiAdmin/transactionAdmin.js";

const ViewTransaction = () => {
  const { id } = useParams(); // lấy id từ URL
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [trx, setTrx] = useState(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchTransaction = async () => {
      try {
        const list = await getWithdrawalsAdmin(); // lấy toàn bộ withdrawals
        const transaction = list.find((t) => t.id.toString() === id); // tìm theo id
        setTrx(transaction || null);
      } catch (error) {
        console.error(error);
        setTrx(null);
      } finally {
        setLoading(false);
      }
    };
    fetchTransaction();
  }, [id]);

  const handleProcess = async (accept) => {
    try {
      setMessage("Processing...");
      const res = await processWithdrawal(trx.id, accept);
      if (res) setTrx(res);
      setMessage(accept ? " Approved successfully!" : " Rejected successfully!");
    } catch {
      setMessage(" Failed to process transaction.");
    } finally {
      setTimeout(() => setMessage(""), 2500);
    }
  };

  if (loading) return <Typography m={2}>Loading...</Typography>;

  if (!trx) {
    return (
      <Box m="20px">
        <Header title="TRANSACTION DETAIL" subtitle="Transaction not found" />
        <Typography variant="h6" color={colors.redAccent[400]}>
          No transaction data available
        </Typography>
        <Button
          variant="contained"
          sx={{ mt: 2, backgroundColor: colors.blueAccent[700] }}
          onClick={() => navigate(-1)}
        >
          Back
        </Button>
      </Box>
    );
  }

  const statusColor = {
    PENDING: colors.grey[300],
    APPROVED: colors.greenAccent[400],
    REJECTED: colors.redAccent[400],
  };

  const payment = trx.paymentDetails || {};
  const user = trx.user || {};

  return (
    <Box m="20px">
      <Header title="TRANSACTION DETAIL" subtitle="Withdrawal request details" />

      <Box
        sx={{
          mt: 3,
          p: 3,
          borderRadius: "12px",
          backgroundColor: colors.primary[400],
          boxShadow: 3,
          maxWidth: "700px",
          mx: "auto",
        }}
      >
        <Typography variant="h6" gutterBottom color={colors.greenAccent[300]}>
          Withdrawal ID: {trx.id}
        </Typography>
        <Divider sx={{ mb: 2, borderColor: colors.grey[700] }} />

        <Box display="flex" flexDirection="column" gap={1.5}>
          <Typography>
            <b>Status:</b>{" "}
            <Chip
              label={trx.status}
              sx={{
                backgroundColor: statusColor[trx.status] || colors.grey[500],
                color: "#fff",
                fontWeight: "bold",
                textTransform: "capitalize",
              }}
            />
          </Typography>

          <Typography>
            <b>Amount:</b>{" "}
            <Typography component="span" color={colors.greenAccent[400]}>
              ${trx.amount}
            </Typography>
          </Typography>

          <Typography>
            <b>Requested At:</b>{" "}
            {new Date(trx.requestedAt).toLocaleString()}
          </Typography>

          {trx.processedAt && (
            <Typography>
              <b>Processed At:</b>{" "}
              {new Date(trx.processedAt).toLocaleString()}
            </Typography>
          )}

          <Divider sx={{ my: 2, borderColor: colors.grey[700] }} />

          <Typography variant="subtitle1" color={colors.blueAccent[300]}>
            Payment Details
          </Typography>
          <Typography>
            <b>Account Number:</b> {payment.accountNumber || "N/A"}
          </Typography>
          <Typography>
            <b>Account Holder:</b> {payment.accountHolderName || "N/A"}
          </Typography>
          <Typography>
            <b>Bank Code:</b> {payment.bankCode || "N/A"}
          </Typography>

          <Divider sx={{ my: 2, borderColor: colors.grey[700] }} />

          <Typography variant="subtitle1" color={colors.blueAccent[300]}>
            User Info
          </Typography>
          <Typography>
            <b>Email:</b> {user.email || "N/A"}
          </Typography>
          <Typography>
            <b>Status:</b> {user.status || "N/A"}
          </Typography>
        </Box>

        {trx.status === "PENDING" && (
          <Box display="flex" justifyContent="center" gap={2} mt={4}>
            <Button
              variant="contained"
              color="success"
              onClick={() => handleProcess(true)}
              sx={{ px: 4 }}
            >
              Approve
            </Button>
            <Button
              variant="outlined"
              color="error"
              onClick={() => handleProcess(false)}
              sx={{ px: 4 }}
            >
              Reject
            </Button>
          </Box>
        )}

        <Box display="flex" justifyContent="center" mt={3}>
          <Button
            variant="outlined"
            sx={{
              color: colors.grey[100],
              borderColor: colors.grey[500],
              px: 4,
            }}
            onClick={() => navigate(-1)}
          >
            Back
          </Button>
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
