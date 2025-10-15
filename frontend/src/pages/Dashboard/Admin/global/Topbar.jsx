import { Box, IconButton, useTheme } from "@mui/material";
import { useContext } from "react";
import { ColorModeContext, tokens } from "@/theme";
import InputBase from "@mui/material/InputBase";
import SearchIcon from "@mui/icons-material/Search";
import LightModeOutlinedIcon from "@mui/icons-material/LightModeOutlined";
import DarkModeOutlinedIcon from "@mui/icons-material/DarkModeOutlined";
//import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";  đã có trong popup profile and setting
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
//import for notification
import CheckCircleOutlined from "@mui/icons-material/CheckCircleOutlined";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemText from "@mui/material/ListItemText";
import ListItemAvatar from "@mui/material/ListItemAvatar";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";


import { useRef, useState } from "react";
import { ClickAwayListener, Paper, Popper, Tabs, Tab, Typography, Stack, Grid, Tooltip, CardContent, Badge } from "@mui/material";
import { SettingOutlined, UserOutlined } from "@ant-design/icons";

// Các component tái sử dụng từ template
import ProfileTab from "@/components/Popup/ProfileTab.jsx";
import SettingTab from "@/components/Popup/SettingTab.jsx";
import MainCard from "@/components/Popup/MainCard.jsx";
import Transitions from "@/components/Popup/Transitions.jsx";
import Avatar from "@/components/Popup/Avatar.jsx";

// Hình đại diện
import avatar1 from "@/assets/z5596085100291_e9dc9606a1f54262e26d39713314ff3a.jpg";
// Notification icons
import { MessageOutlined, SettingOutlined as SettingIcon } from "@ant-design/icons";


const Topbar = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const colorMode = useContext(ColorModeContext);
  //Profile and setting popper
  const anchorRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState(0);

  const handleToggle = () => {
    setOpen((prevOpen) => !prevOpen);
  };

  const handleClose = (event) => {
    if (anchorRef.current && anchorRef.current.contains(event.target)) {
      return;
    }
    setOpen(false);
  };

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };
  // Notification popper
  const anchorNotif = useRef(null);
  const [openNotif, setOpenNotif] = useState(false);
  const [unread, setUnread] = useState(2);

  const handleToggleNotif = () => setOpenNotif((prev) => !prev);
  const handleCloseNotif = (e) => {
    if (anchorNotif.current && anchorNotif.current.contains(e.target)) return;
    setOpenNotif(false);
  };

  return (
    <Box display="flex" justifyContent="space-between" p={2}>
      {/* SEARCH BAR */}
      <Box
        display="flex"
        backgroundColor={colors.primary[400]}
        borderRadius="3px"
      >
        <InputBase sx={{ ml: 2, flex: 1 }} placeholder="Search" />
        <IconButton type="button" sx={{ p: 1 }}>
          <SearchIcon />
        </IconButton>
      </Box>

      {/* ICONS */}
      <Box display="flex">
        <IconButton onClick={colorMode.toggleColorMode}>
          {theme.palette.mode === "dark" ? (
            <DarkModeOutlinedIcon />
          ) : (
            <LightModeOutlinedIcon />
          )}
        </IconButton>
        {/* Notification */}
        <Box sx={{ position: "relative" }}>
          <IconButton ref={anchorNotif} onClick={handleToggleNotif}>
            <Badge badgeContent={unread} color="primary">
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
                <Paper
                  sx={{
                    boxShadow: 3,
                    width: 320,
                    maxWidth: { xs: 280, md: 360 },
                    bgcolor: theme.palette.background.paper,
                  }}
                >
                  <ClickAwayListener onClickAway={handleCloseNotif}>
                    <Box>
                      <Box
                        display="flex"
                        justifyContent="space-between"
                        alignItems="center"
                        p={2}
                        borderBottom={`1px solid ${theme.palette.divider}`}
                      >
                        <Typography variant="h6">Notifications</Typography>
                        {unread > 0 && (
                          <Tooltip title="Mark all as read">
                            <IconButton size="small" color="success" onClick={() => setUnread(0)}>
                              <CheckCircleOutlined />
                            </IconButton>
                          </Tooltip>
                        )}
                      </Box>

                      <List sx={{ p: 0 }}>
                        <ListItem divider>
                          <ListItemAvatar>
                            <Avatar sx={{ bgcolor: "success.light", color: "success.dark" }}>
                              <UserOutlined />
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                            primary="A new user has just registered as Ev_owner"
                            secondary="2 min ago"
                          />
                        </ListItem>

                        <ListItem divider>
                          <ListItemAvatar>
                            <Avatar sx={{ bgcolor: "primary.light", color: "primary.dark" }}>
                              <MessageOutlined />
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                            primary="Your profile is 60% complete"
                            secondary="7 hours ago"
                          />
                        </ListItem>

                        <ListItem>
                          <ListItemAvatar>
                            <Avatar sx={{ bgcolor: "error.light", color: "error.dark" }}>
                              <SettingIcon />
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                            primary="Khiem commented your post"
                            secondary="5 August"
                          />
                        </ListItem>
                      </List>
                    </Box>
                  </ClickAwayListener>
                </Paper>
              </Transitions>
            )}
          </Popper>
        </Box>
        {/* Profile and setting */}
        <Box sx={{ flexShrink: 0, ml: 1 }}>
          <IconButton ref={anchorRef} onClick={handleToggle}>
            <PersonOutlinedIcon />
          </IconButton>

          <Popper
            placement="bottom-end"
            open={open}
            anchorEl={anchorRef.current}
            transition
            disablePortal
            popperOptions={{
              modifiers: [
                {
                  name: "offset",
                  options: { offset: [0, 9] },
                },
              ],
            }}
            style={{ zIndex: 2000 }}
          >
            {({ TransitionProps }) => (
              <Transitions type="grow" position="top-right" in={open} {...TransitionProps}>
                <Paper
                  sx={{
                    boxShadow: 3,
                    width: 290,
                    minWidth: 240,
                    maxWidth: { xs: 250, md: 290 },
                  }}
                >
                  <ClickAwayListener onClickAway={handleClose}>
                    <div>
                      <MainCard elevation={0} border={false} content={false}>
                        <CardContent sx={{ px: 2.5, pt: 3 }}>
                          <Grid container justifyContent="space-between" alignItems="center">
                            <Grid>
                              <Stack direction="row" sx={{ gap: 1.25, alignItems: "center" }}>
                                {(() => {
                                  // Lấy dữ liệu admin từ localStorage (sau khi edit)
                                  const storedAdmin = JSON.parse(localStorage.getItem("adminData"));
                                  const admin = storedAdmin || {
                                    firstName: "Tin",
                                    lastName: "Bao",
                                    role: "Administrator",
                                    avatar: avatar1
                                  };

                                  // Xử lý đường dẫn ảnh (base64 hoặc local path)
                                  const avatarSrc = admin.avatar?.startsWith("data:")
                                    ? admin.avatar
                                    : admin.avatar?.replace("@/", "/") || avatar1;

                                  return (
                                    <Stack direction="row" sx={{ gap: 1.25, alignItems: "center" }}>
                                      <Avatar alt="profile user" src={avatarSrc} sx={{ width: 32, height: 32 }} />
                                      <Stack>
                                        <Typography variant="h6">
                                          {admin.firstName} {admin.lastName}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                          {admin.role}
                                        </Typography>
                                      </Stack>
                                    </Stack>
                                  );
                                })()}

                              </Stack>
                            </Grid>
                            <Grid>
                            </Grid>
                          </Grid>
                        </CardContent>

                        <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
                          <Tabs
                            variant="fullWidth"
                            value={value}
                            onChange={handleChange}
                            aria-label="profile tabs"
                          >
                            <Tab
                              icon={<UserOutlined />}
                              label="Profile"
                              sx={{
                                display: "flex",
                                flexDirection: "row",
                                justifyContent: "center",
                                alignItems: "center",
                                textTransform: "capitalize",
                                gap: 1.25,
                                "& .MuiTab-icon": { marginBottom: 0 },
                                "&.Mui-selected": {
                                  color: (theme) => theme.palette.secondary.main, // màu nổi bật khi active
                                  fontWeight: "bold"
                                }
                              }}
                            />
                            <Tab
                              icon={<SettingOutlined />}
                              label="Setting"
                              sx={{
                                display: "flex",
                                flexDirection: "row",
                                justifyContent: "center",
                                alignItems: "center",
                                textTransform: "capitalize",
                                gap: 1.25,
                                "& .MuiTab-icon": { marginBottom: 0 },
                                "&.Mui-selected": {
                                  color: (theme) => theme.palette.secondary.main, // dùng màu phụ
                                  fontWeight: "bold"
                                }
                              }}
                            />
                          </Tabs>
                        </Box>

                        <Box sx={{ p: 1.5 }}>
                          {value === 0 && <ProfileTab role="admin" onClose={handleClose} />}
                          {value === 1 && <SettingTab role="admin" onClose={handleClose} />}
                        </Box>
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
