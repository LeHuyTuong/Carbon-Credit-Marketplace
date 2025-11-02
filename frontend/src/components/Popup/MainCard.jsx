import PropTypes from "prop-types";
import React, { forwardRef } from "react";

// material-ui
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardHeader from "@mui/material/CardHeader";
import Divider from "@mui/material/Divider";

const headerSX = {
  p: 2.5,
  "& .MuiCardHeader-action": { m: "0px auto", alignSelf: "center" },
};

// ✅ Dùng forwardRef để truyền ref xuống thẻ Card
const MainCard = forwardRef(function MainCard(
  {
    border = true,
    boxShadow,
    children,
    subheader,
    content = true,
    contentSX = {},
    darkTitle,
    divider = true,
    elevation,
    secondary,
    shadow,
    sx = {},
    title,
    modal = false,
    ...others
  },
  ref
) {
  return (
    <Card
      elevation={elevation || 0}
      sx={[
        (theme) => ({
          position: "relative",
          border: border ? "1px solid" : "none",
          borderRadius: 1,
          borderColor: "grey.A800",
          boxShadow: boxShadow && !border ? shadow || theme.customShadows.z1 : "inherit",
          ":hover": {
            boxShadow: boxShadow ? shadow || theme.customShadows.z1 : "inherit",
          },
          ...(modal && {
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            width: { xs: `calc(100% - 50px)`, sm: "auto" },
            maxWidth: 768,
            "& .MuiCardContent-root": {
              overflowY: "auto",
              minHeight: "auto",
              maxHeight: `calc(100vh - 200px)`,
            },
          }),
        }),
        sx,
      ]}
      ref={ref} // ✅ ref hoạt động đúng
      {...others}
    >
      {/* Header */}
      {!darkTitle && title && (
        <CardHeader
          sx={headerSX}
          slotProps={{ title: { variant: "subtitle1" } }}
          title={title}
          action={secondary}
          subheader={subheader}
        />
      )}

      {/* Divider */}
      {title && divider && <Divider />}

      {/* Content */}
      {content ? <CardContent sx={contentSX}>{children}</CardContent> : children}
    </Card>
  );
});

MainCard.propTypes = {
  border: PropTypes.bool,
  boxShadow: PropTypes.bool,
  children: PropTypes.node,
  subheader: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  content: PropTypes.bool,
  contentSX: PropTypes.object,
  darkTitle: PropTypes.bool,
  divider: PropTypes.bool,
  elevation: PropTypes.number,
  secondary: PropTypes.any,
  shadow: PropTypes.string,
  sx: PropTypes.object,
  title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  modal: PropTypes.bool,
};

export default MainCard;
