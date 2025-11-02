import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Divider,
  Chip,
  Button,
  Snackbar,
  Alert,
  CircularProgress,
  useTheme,
} from "@mui/material";
import { tokens } from "@/theme";
import Header from "@/components/Chart/Header.jsx";
import {
  processWithdrawal,
  getWithdrawalsAdmin,
  getPaymentDetails,
} from "@/apiAdmin/transactionAdmin.js";

const ViewTransaction = () => {
  const { id } = useParams();
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  const [trx, setTrx] = useState(null);
  const [loading, setLoading] = useState(true);
  const [alert, setAlert] = useState({
    open: false,
    type: "info",
    text: "",
  });

  useEffect(() => {
    const fetchTransaction = async () => {
      try {
        const list = await getWithdrawalsAdmin();
        const transaction = list.find((t) => t.id.toString() === id);

        if (transaction) {
          const paymentRes = await getPaymentDetails();
          transaction.paymentDetails = paymentRes;
          setTrx(transaction);
        } else {
          setTrx(null);
        }
      } catch (error) {
        console.error(error);
        setAlert({
          open: true,
          type: "error",
          text: "Failed to load transaction data.",
        });
      } finally {
        setLoading(false);
      }
    };
    fetchTransaction();
  }, [id]);

  const handleProcess = async (accept) => {
    try {
      setAlert({
        open: true,
        type: "info",
        text: "Processing transaction...",
      });
      const res = await processWithdrawal(trx.id, accept);
      if (res) setTrx(res);
      setAlert({
        open: true,
        type: accept ? "success" : "error",
        text: accept
          ? "Withdrawal approved successfully!"
          : "Withdrawal rejected successfully!",
      });
    } catch (error) {
      console.error(error);
      setAlert({
        open: true,
        type: "error",
        text: "Failed to process transaction.",
      });
    }
  };

  if (loading)
    return (
      <Box display="flex" justifyContent="center" mt={5}>
        <CircularProgress color="secondary" />
      </Box>
    );

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

  const user = trx.user || {};
  const paymentInfo = trx.paymentDetails || {};

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
            <Box component="span" color={colors.greenAccent[400]}>
              ${trx.amount}
            </Box>
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
            <b>Account Number:</b> {paymentInfo.accountNumber || "N/A"}
          </Typography>
          <Typography>
            <b>Account Holder:</b> {paymentInfo.accountHolderName || "N/A"}
          </Typography>
          <Typography>
            <b>Bank Code:</b> {paymentInfo.bankCode || "N/A"}
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
      </Box>

      {/* Snackbar thông báo */}
      <Snackbar
        open={alert.open}
        autoHideDuration={5000}
        onClose={() => setAlert({ ...alert, open: false })}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
        sx={{
          mt: 2,
          "& .MuiPaper-root": {
            minWidth: "400px",
            maxWidth: "80vw",
          },
        }}
      >
        <Alert
          onClose={() => setAlert({ ...alert, open: false })}
          severity={alert.type}
          variant="filled"
          sx={{
            width: "100%",
            fontWeight: "bold",
            fontSize: "1.1rem",
            py: 1.5,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            boxShadow: 4,
          }}
          iconMapping={{
            info: <CircularProgress size={20} color="inherit" />,
          }}
        >
          {alert.text}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ViewTransaction;
