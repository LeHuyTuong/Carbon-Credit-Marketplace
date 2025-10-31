
import {
  Box,
  IconButton,
  useTheme,
  InputBase,
  Badge,
  Paper,
  Popper,
  ClickAwayListener,
  Typography,
  Stack,
  Grid,
  Avatar,
  Box as MuiBox,
  Tooltip,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
} from "@mui/material";
import { useContext, useEffect, useState, useRef, useMemo } from "react";
import { ColorModeContext, tokens } from "@/theme";
import SearchIcon from "@mui/icons-material/Search";
import LightModeOutlinedIcon from "@mui/icons-material/LightModeOutlined";
import DarkModeOutlinedIcon from "@mui/icons-material/DarkModeOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import CheckCircleOutlined from "@mui/icons-material/CheckCircleOutlined";
import Transitions from "@/components/Popup/Transitions.jsx";
import MainCard from "@/components/Popup/MainCard.jsx";
import ProfileTab from "@/components/Popup/ProfileTab.jsx";
import { checkKYCAdmin } from "@/apiAdmin/apiLogin.js";
import useSseNotifications from "@/components/Navbar/useSseNotifications.js";


const Topbar = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const colorMode = useContext(ColorModeContext);

  const [admin, setAdmin] = useState({
    firstName: "",
    lastName: "",
    role: "Administrator",
    avatarUrl: null,
  });

  const anchorRef = useRef(null);
  const [open, setOpen] = useState(false);
  const anchorNotif = useRef(null);
  const [openNotif, setOpenNotif] = useState(false);
  const adminToken = useMemo(() => {
    const stored =
      sessionStorage.getItem("admin_token") || localStorage.getItem("admin_token");
    return stored && stored !== "null" && stored !== "undefined" ? stored : null;
  }, []);

  const { notifications, unreadCount, markAllAsRead } = useSseNotifications({
    enabled: !!adminToken,
    token: adminToken,
  });
  const [avatarVersion, setAvatarVersion] = useState(0);
  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        const kycRes = await checkKYCAdmin();
        const fullName = kycRes?.name || "Unknown Admin";
        const role = localStorage.getItem("admin_role") || "Admin";
        const avatarUrl = kycRes?.avatarUrl || null;

        const [firstName, ...rest] = fullName.split(" ");
        const lastName = rest.join(" ");

        setAdmin({ firstName, lastName, role, avatarUrl });
      } catch (err) {
        console.error(" Failed to fetch KYC Admin:", err.message);
      }
    };
    fetchAdminData();
  }, [avatarVersion]);
  useEffect(() => {
    const handleAvatarUpdated = () => {
      setAvatarVersion((v) => v + 1);
    };

    return () => window.removeEventListener("avatar_updated", handleAvatarUpdated);
  }, []);
  const formatNotificationTime = (value) => {
    if (!value) return "";
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
    });
  };

  const handleToggle = () => setOpen((prev) => !prev);
  const handleClose = (event) => {
    if (anchorRef.current && anchorRef.current.contains(event.target)) return;
    setOpen(false);
  };

  const handleToggleNotif = () => setOpenNotif((prev) => !prev);
  const handleCloseNotif = (e) => {
    if (anchorNotif.current && anchorNotif.current.contains(e.target)) return;
    setOpenNotif(false);
  };

  return (
    <Box display="flex" justifyContent="space-between" p={2}>
      {/* SEARCH BAR */}
      <Box display="flex" backgroundColor={colors.primary[400]} borderRadius="3px">
        <InputBase sx={{ ml: 2, flex: 1 }} placeholder="Search" />
        <IconButton type="button" sx={{ p: 1 }}>
          <SearchIcon />
        </IconButton>
      </Box>

      {/* ICONS */}
      <Box display="flex" alignItems="center">
        <IconButton onClick={colorMode.toggleColorMode}>
          {theme.palette.mode === "dark" ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
        </IconButton>

        {/* Notification */}
        <Box sx={{ position: "relative" }}>
          <IconButton ref={anchorNotif} onClick={handleToggleNotif}>
            <Badge badgeContent={unreadCount} color="primary">
              <NotificationsOutlinedIcon />
            </Badge>
          </IconButton>

          <Popper
            placement="bottom-end"
            open={openNotif}
            anchorEl={anchorNotif.current}
            transition
            disablePortal
            style={{ zIndex: 2000 }}
          >
            {({ TransitionProps }) => (
              <Transitions type="grow" position="top-right" in={openNotif} {...TransitionProps}>
                <Paper sx={{ boxShadow: 3, width: 300, bgcolor: theme.palette.background.paper }}>
                  <ClickAwayListener onClickAway={handleCloseNotif}>
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" p={2}>
                        <Typography variant="h6">Notifications</Typography>
                        {unreadCount > 0 && (
                          <Tooltip title="Mark all as read">
                            <IconButton size="small" color="success" onClick={markAllAsRead}>
                              <CheckCircleOutlined fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </Box>

                      <List sx={{ p: 0 }}>
                        {notifications.length === 0 ? (
                          <ListItem>
                            <ListItemText
                              primary="No notifications yet"
                              primaryTypographyProps={{
                                variant: "body2",
                                color: "text.secondary",
                              }}
                            />
                          </ListItem>
                        ) : (
                          notifications.map((notif) => (
                            <ListItem key={notif.id} divider alignItems="flex-start">
                              <ListItemAvatar>
                                <Avatar
                                  sx={{
                                    bgcolor: notif.isUnread
                                      ? colors.greenAccent[500]
                                      : theme.palette.primary.main,
                                    width: 32,
                                    height: 32,
                                  }}
                                >
                                  <NotificationsOutlinedIcon fontSize="small" />
                                </Avatar>
                              </ListItemAvatar>
                              <ListItemText
                                primary={
                                  <Typography
                                    variant="body2"
                                    fontWeight={notif.isUnread ? 600 : 400}
                                  >
                                    {notif.title || notif.message}
                                  </Typography>
                                }
                                secondary={
                                  <Stack spacing={0.5}>
                                    {notif.title && (
                                      <Typography variant="body2" color="text.secondary">
                                        {notif.message}
                                      </Typography>
                                    )}
                                    <Typography variant="caption" color="text.disabled">
                                      {formatNotificationTime(notif.receivedAt)}
                                    </Typography>
                                  </Stack>
                                }
                              />
                            </ListItem>
                          ))
                        )}
                      </List>
                    </Box>
                  </ClickAwayListener>
                </Paper>
              </Transitions>
            )}
          </Popper>
        </Box>

        {/* Profile */}
        <Box sx={{ flexShrink: 0, ml: 1 }}>
          <IconButton ref={anchorRef} onClick={handleToggle}>
            <Avatar
              src={admin.avatarUrl || undefined}
              alt="Admin Avatar"
              sx={{
                width: 36,
                height: 36,
                bgcolor: colors.greenAccent[600],
                fontSize: "0.95rem",
                fontWeight: "bold",
              }}
            >
              {!admin.avatarUrl && admin.firstName?.[0]?.toUpperCase()}
            </Avatar>
          </IconButton>

          <Popper
            placement="bottom-end"
            open={open}
            anchorEl={anchorRef.current}
            transition
            disablePortal
            style={{ zIndex: 2000 }}
          >
            {({ TransitionProps }) => (
              <Transitions type="grow" position="top-right" in={open} {...TransitionProps}>
                <Paper sx={{ boxShadow: 3, width: 290, minWidth: 240, maxWidth: { xs: 250, md: 290 } }}>
                  <ClickAwayListener onClickAway={handleClose}>
                    <div>
                      <MainCard elevation={0} border={false} content={false}>
                        <MuiBox sx={{ px: 2.5, pt: 3 }}>
                          <Grid container justifyContent="space-between" alignItems="center">
                            <Stack direction="row" sx={{ gap: 1.25, alignItems: "center" }}>
                              <Avatar
                                src={admin.avatarUrl || undefined}
                                alt="Admin Avatar"
                                sx={{
                                  width: 50,
                                  height: 50,
                                  bgcolor: colors.greenAccent[600],
                                }}
                              >
                                {!admin.avatarUrl && admin.firstName?.[0]?.toUpperCase()}
                              </Avatar>
                              <Stack>
                                <Typography variant="h6">
                                  {admin.firstName} {admin.lastName}
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                  {admin.role}
                                </Typography>
                              </Stack>
                            </Stack>
                          </Grid>
                        </MuiBox>

                        {/* 🔹 Chỉ còn Profile tab */}
                        <MuiBox sx={{ p: 1.5 }}>
                          <ProfileTab role="admin" onClose={handleClose} onAvatarUpdated={() => setAvatarVersion(v => v + 1)} />
                        </MuiBox>
                      </MainCard>
                    </div>
                  </ClickAwayListener>
                </Paper>
              </Transitions>
            )}
          </Popper>
        </Box>
      </Box>
    </Box>
  );
};

export default Topbar;
