import PropTypes from "prop-types";
import { Link as RouterLink } from "react-router-dom";
import List from "@mui/material/List";
import Link from "@mui/material/Link";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";

import CommentOutlined from "@ant-design/icons/CommentOutlined";
import LockOutlined from "@ant-design/icons/LockOutlined";
import QuestionCircleOutlined from "@ant-design/icons/QuestionCircleOutlined";
import UserOutlined from "@ant-design/icons/UserOutlined";
import UnorderedListOutlined from "@ant-design/icons/UnorderedListOutlined";

// ==============================|| HEADER PROFILE - SETTING TAB ||============================== //

export default function SettingTab({ role, onClose }) {
  // base path (phụ thuộc vào loại user)
  const basePath = role === "admin" ? "/admin" : "/cva";

  return (
    <List component="nav" sx={{ p: 0, "& .MuiListItemIcon-root": { minWidth: 32 } }}>
      {/* Support (external link) */}
      <Link underline="none" sx={{ color: "inherit" }} target="_blank" href="https://chatgpt.com/">
        <ListItemButton onClick={onClose}>
          <ListItemIcon>
            <QuestionCircleOutlined />
          </ListItemIcon>
          <ListItemText primary="Support" />
        </ListItemButton>
      </Link>

      {/* Account Settings */}
      <ListItemButton component={RouterLink} to={`${basePath}/account_settings`} onClick={onClose}>
        <ListItemIcon>
          <UserOutlined />
        </ListItemIcon>
        <ListItemText primary="Account Settings" />
      </ListItemButton>

      {/* Privacy Center */}
      <ListItemButton component={RouterLink} to={`${basePath}/privacy_center`} onClick={onClose}>
        <ListItemIcon>
          <LockOutlined />
        </ListItemIcon>
        <ListItemText primary="Privacy Center" />
      </ListItemButton>

      {/* Feedback (external link) */}
      <Link
        underline="none"
        style={{ color: "inherit" }}
        target="_blank"
        href="https://codedthemes.support-hub.io/"
      >
        <ListItemButton onClick={onClose}>
          <ListItemIcon>
            <CommentOutlined />
          </ListItemIcon>
          <ListItemText primary="Feedback" />
        </ListItemButton>
      </Link>

      {/* History */}
      <ListItemButton component={RouterLink} to={`${basePath}/history`} onClick={onClose}>
        <ListItemIcon>
          <UnorderedListOutlined />
        </ListItemIcon>
        <ListItemText primary="History" />
      </ListItemButton>
    </List>
  );
}

SettingTab.propTypes = {
  role: PropTypes.oneOf(["admin", "cva"]).isRequired,
  onClose: PropTypes.func,
};
