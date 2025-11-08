import { Box, IconButton, useTheme, InputBase, Paper, Badge, Popper, ClickAwayListener, Tabs, Tab, Typography, Stack, Grid, List, ListItem, ListItemText, ListItemAvatar } from "@mui/material";
import { useContext, useRef, useState, useEffect } from "react";
import { ColorModeContext, tokens } from "@/themeCVA";
import SearchIcon from "@mui/icons-material/Search";
import LightModeOutlinedIcon from "@mui/icons-material/LightModeOutlined";
import DarkModeOutlinedIcon from "@mui/icons-material/DarkModeOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import CheckCircleOutlined from "@mui/icons-material/CheckCircleOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import { UserOutlined, MessageOutlined } from "@ant-design/icons";

import ProfileTab from "@/components/Popup/ProfileTab.jsx";
import MainCard from "@/components/Popup/MainCard.jsx";
import Transitions from "@/components/Popup/Transitions.jsx";
import Avatar from "@/components/Popup/Avatar.jsx";

import avatar1 from "@/assets/z5596085100291_e9dc9606a1f54262e26d39713314ff3a.jpg";
import { checkKYCCVA } from "@/apiCVA/apiAuthor.js";

const Topbar = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const colorMode = useContext(ColorModeContext);

  // === Profile ===
  const anchorRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState(0);
  const handleToggle = () => setOpen((prev) => !prev);
  const handleClose = (event) => {
    if (anchorRef.current && anchorRef.current.contains(event.target)) return;
    setOpen(false);
  };
  const handleChange = (event, newValue) => setValue(newValue);

  // === Notifications ===
  const anchorNotif = useRef(null);
  const [openNotif, setOpenNotif] = useState(false);
  const [unread, setUnread] = useState(2);
  const handleToggleNotif = () => setOpenNotif((prev) => !prev);
  const handleCloseNotif = (e) => {
    if (anchorNotif.current && anchorNotif.current.contains(e.target)) return;
    setOpenNotif(false);
  };

  // === CVA User Info ===
  const [userInfo, setUserInfo] = useState({
    firstName: "Tin",
    lastName: "Bao",
    role: "CVA",
    avatar: avatar1,
  });

  useEffect(() => {
    const fetchCVAInfo = async () => {
      try {
        const data = await checkKYCCVA();

        if (data?.id) {
          setUserInfo({
            name: data.name || "Unknown",
            role: data.positionTitle || "CVA",
            email: data.email || "",
            organization: data.organization || "",
            avatarUrl: data.avatarUrl || "",
          });
        } else if (data?.responseData) {
          const info = data.responseData;
          setUserInfo({
            name: info.name || "Unknown",
            role: info.positionTitle || "CVA",
            email: info.email || "",
            organization: info.organization || "",
            avatarUrl: info.avatarUrl || "",
          });
        }
      } catch (error) {
        console.error("Error fetching CVA info:", error);
      }
    };

    fetchCVAInfo();
  }, []);

  const avatarSrc =
    userInfo.avatar?.startsWith("data:") || userInfo.avatar?.startsWith("http")
      ? userInfo.avatar
      : avatar1;

  return (
    <Box display="flex" justifyContent="flex-end" p={2}>
      
      {/* ICONS */}
      <Box display="flex">
        {/* DARK / LIGHT MODE */}
        <IconButton onClick={colorMode.toggleColorMode}>
          {theme.palette.mode === "dark" ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
        </IconButton>

        {/* NOTIFICATION */}
        {/* <Box sx={{ position: "relative" }}>
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
                <Paper sx={{ boxShadow: 3, width: 320, bgcolor: theme.palette.background.paper }}>
                  <ClickAwayListener onClickAway={handleCloseNotif}>
                    <Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" p={2}>
                        <Typography variant="h6">Notifications</Typography>
                        {unread > 0 && (
                          <IconButton size="small" color="success" onClick={() => setUnread(0)}>
                            <CheckCircleOutlined />
                          </IconButton>
                        )}
                      </Box>

                      <List sx={{ p: 0 }}>
                        <ListItem divider>
                          <ListItemAvatar>
                            <Avatar sx={{ bgcolor: "success.light", color: "success.dark" }}>
                              <UserOutlined />
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText primary="A new CVA request approved" secondary="2 min ago" />
                        </ListItem>
                        <ListItem divider>
                          <ListItemAvatar>
                            <Avatar sx={{ bgcolor: "primary.light", color: "primary.dark" }}>
                              <MessageOutlined />
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText primary="System update completed" secondary="7 hours ago" />
                        </ListItem>
                      </List>
                    </Box>
                  </ClickAwayListener>
                </Paper>
              </Transitions>
            )}
          </Popper>
        </Box> */}

        {/* PROFILE */}
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
            popperOptions={{ modifiers: [{ name: "offset", options: { offset: [0, 9] } }] }}
            style={{ zIndex: 2000 }}
          >
            {({ TransitionProps }) => (
              <Transitions type="grow" position="top-right" in={open} {...TransitionProps}>
                <Paper sx={{ boxShadow: 3, width: 290, maxWidth: 290 }}>
                  <ClickAwayListener onClickAway={handleClose}>
                    <MainCard elevation={0} border={false} content={false}>
                      <Box sx={{ px: 2.5, pt: 3 }}>
                        <Grid container justifyContent="space-between" alignItems="center">
                          <Grid>
                            <Stack direction="row" sx={{ gap: 1.25, alignItems: "center" }}>
                              <Avatar
                                sx={{
                                  width: 32,
                                  height: 32,
                                  bgcolor: colors.greenAccent[400],
                                  fontSize: 16,
                                  fontWeight: "bold",
                                  color: colors.grey[900],
                                }}
                              >
                                {userInfo.name ? userInfo.name.charAt(0).toUpperCase() : "?"}
                              </Avatar>

                              <Stack>
                                <Typography variant="h6">{userInfo.name || `${userInfo.firstName} ${userInfo.lastName}`}</Typography>
                                <Typography variant="body2" color="text.secondary">{userInfo.role || "CVA"}</Typography>
                              </Stack>
                            </Stack>
                          </Grid>
                        </Grid>

                        {/* PROFILE TAB */}
                        <Box sx={{ p: 1.5 }}>
                          <ProfileTab role="cva" onClose={handleClose} />
                        </Box>
                      </Box>
                    </MainCard>
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
