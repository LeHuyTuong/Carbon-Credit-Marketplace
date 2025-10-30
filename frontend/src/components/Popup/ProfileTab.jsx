import PropTypes from 'prop-types';
import { Link, useNavigate } from 'react-router-dom';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import EditOutlined from '@mui/icons-material/EditOutlined';
import LogoutOutlined from '@ant-design/icons/LogoutOutlined';
import UserOutlined from '@ant-design/icons/UserOutlined';
import { apiLogout } from '@/apiAdmin/apiLogin.js'; //  import API 
import { useState } from 'react';
import { Snackbar, Alert } from '@mui/material';

export default function ProfileTab({ role, onClose }) {
  const basePath = role === 'admin' ? '/admin' : '/cva';
  const navigate = useNavigate();
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });

  
  // Hàm xử lý logout
  const handleLogout = async () => {
    try {
      if (role === 'admin') {
        await apiLogout(); // gọi API logout
        localStorage.removeItem('admin_token');
        setSnackbar({ open: true, message: 'Logout successfully!', severity: 'success' });

        //  đợi 1.2s rồi điều hướng về trang login admin
        setTimeout(() => navigate('/admin/login'), 1200);
      } else {
        localStorage.removeItem('cva_token');
        setSnackbar({ open: true, message: 'Logout successfully!', severity: 'success' });
        setTimeout(() => navigate('/cva/login'), 1200);
      }
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

      {/*  Snackbar thông báo */}
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
