import { Snackbar, Alert, CircularProgress, useTheme } from "@mui/material";
import { tokens } from "@/theme";

const CustomSnackbar = ({ open, type = "info", text = "", onClose, duration }) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  console.log("Snackbar props:", { open, type, text, duration });

  // const getBgColor = () => {
  //   switch (type) {
  //     case "success":
  //       return colors.greenAccent[600];
  //     case "error":
  //       return colors.redAccent[500];
  //     case "warning":
  //       return colors.yellowAccent[400];
  //     default:
  //       return colors.blueAccent[400];
  //   }
  // };

  const getBgColor = () => {
    if (!colors) return "#1976d2"; // fallback màu mặc định
    switch (type) {
      case "success":
        return colors.greenAccent?.[600] || "#4caf50";
      case "error":
        return colors.redAccent?.[500] || "#f44336";
      case "warning":
        return colors.yellowAccent?.[400] || "#ffb300";
      default:
        return colors.blueAccent?.[400] || "#1976d2";
    }
  };


  return (
    <Snackbar
      open={open}
      autoHideDuration={Number(duration) || 5000}
      onClose={onClose}
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
        onClose={onClose}
        severity={type}
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
          backgroundColor: getBgColor(),
        }}
        iconMapping={{
          info: <CircularProgress size={20} color="inherit" />,
        }}
      >
        {text}
      </Alert>
    </Snackbar>
  );
};

export default CustomSnackbar;
