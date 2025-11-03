import PropTypes from 'prop-types';
import { Link, useNavigate } from 'react-router-dom';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import EditOutlined from '@mui/icons-material/EditOutlined';
import LogoutOutlined from '@ant-design/icons/LogoutOutlined';
import UserOutlined from '@ant-design/icons/UserOutlined';
import { apiLogout } from '@/apiAdmin/apiLogin.js';
import { useState } from 'react';
import { Snackbar, Alert } from '@mui/material';
import { useAuth } from '@/context/AuthContext.jsx';

export default function ProfileTab({ role, onClose }) {
  const { logout } = useAuth(); //  logout từ context
  const basePath = role === 'admin' ? '/admin' : '/cva';
  const navigate = useNavigate();
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });

 const handleLogout = async () => {
  try {
    if (role === 'admin') await apiLogout();

    // Xóa state context & token
    logout();

    // Navigate ngay lập tức
    const loginPath = role === 'admin' ? '/admin/carbonX/mkp/login' : '/cva/carbonX/mkp/login';
    navigate(loginPath, { replace: true });

    // Snackbar vẫn show bình thường
    setSnackbar({ open: true, message: 'Logout successfully!', severity: 'success' });
  } catch (error) {
    console.error('Logout error:', error);
    setSnackbar({ open: true, message: 'Logout failed!', severity: 'error' });
  }
};


  return (
    <>
      <List component="nav" sx={{ p: 0, '& .MuiListItemIcon-root': { minWidth: 32 } }}>
        <ListItemButton component={Link} to={`${basePath}/view_profile_${role}`} onClick={onClose}>
          <ListItemIcon><UserOutlined /></ListItemIcon>
          <ListItemText primary="View Profile" />
        </ListItemButton>

        <ListItemButton component={Link} to={`${basePath}/edit_profile_${role}`} onClick={onClose}>
          <ListItemIcon><EditOutlined /></ListItemIcon>
          <ListItemText primary="Edit Profile" />
        </ListItemButton>

        <ListItemButton onClick={handleLogout}>
          <ListItemIcon><LogoutOutlined /></ListItemIcon>
          <ListItemText primary="Logout" />
        </ListItemButton>
      </List>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={2000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
      </Snackbar>
    </>
  );
}

ProfileTab.propTypes = {
  role: PropTypes.oneOf(['admin', 'cva']).isRequired,
  onClose: PropTypes.func,
};
