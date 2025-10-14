import React, { useState } from "react";
import {Box,TextField,Button,Paper,Typography,Avatar,useTheme,} from "@mui/material";
import { tokens } from "@/theme";
import { adminData } from "@/data/mockData";
import Header from "@/components/Chart/Header.jsx";
import { useNavigate } from "react-router-dom";

const EditProfile = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigate = useNavigate();

  //  Ưu tiên đọc dữ liệu đã lưu trong localStorage (nếu có)
  const storedData = JSON.parse(localStorage.getItem("adminData"));
  const [admin, setAdmin] = useState(storedData || adminData[0]);

  // Xử lý thay đổi text field
  const handleChange = (e) => {
    setAdmin({ ...admin, [e.target.name]: e.target.value });
  };

  //  Xử lý upload ảnh
  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setAdmin((prev) => ({ ...prev, avatar: reader.result }));
      };
      reader.readAsDataURL(file);
    }
  };

  //  Khi bấm Save
  const handleSave = () => {
    console.log("Saved admin info:", admin);
    // Giả lập lưu vào localStorage (tạm thời)
    localStorage.setItem("adminData", JSON.stringify(admin));

    // TODO: Gọi API thật nếu có backend ở đây

    navigate("/admin/view_profile_admin"); // quay lại trang view
  };

  return (
    <Box m="20px">
      <Header title="EDIT PROFILE" subtitle="Update Administrator Information" />

      <Paper
        sx={{
          p: 4,
          borderRadius: "16px",
          backgroundColor: colors.primary[400],
        }}
      >
        <Typography
          variant="h5"
          fontWeight="bold"
          mb={3}
          color={colors.greenAccent[400]}
        >
          Edit Information
        </Typography>

        {/* ===== Avatar Upload Section ===== */}
        <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          mb={3}
          gap={1.5}
        >
          <Avatar
            src={admin.avatar}
            alt="Admin Avatar"
            sx={{
              width: 120,
              height: 120,
              border: `3px solid ${colors.greenAccent[400]}`,
            }}
          />
          <Button
            variant="outlined"
            component="label"
            color="secondary"
            sx={{
              fontSize: "0.85rem",
              textTransform: "none",
            }}
          >
            Upload New Avatar
            <input
              type="file"
              hidden
              accept="image/*"
              onChange={handleImageUpload}
            />
          </Button>
        </Box>

        {/* ===== Text Fields ===== */}
        <Box display="grid" gap={2}>
          <TextField
            name="firstName"
            label="First Name"
            value={admin.firstName}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="lastName"
            label="Last Name"
            value={admin.lastName}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="email"
            label="Email"
            value={admin.email}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="contact"
            label="Phone Number"
            value={admin.contact}
            onChange={handleChange}
            fullWidth
          />
          <TextField
            name="address"
            label="Address"
            value={admin.address}
            onChange={handleChange}
            fullWidth
          />
        </Box>

        {/* ===== Action Buttons ===== */}
        <Box mt={4} display="flex" justifyContent="flex-end" gap={2}>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => navigate("/admin/view_profile_admin")}
          >
            Cancel
          </Button>
          <Button variant="contained" color="success" onClick={handleSave}>
            Save
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default EditProfile;
