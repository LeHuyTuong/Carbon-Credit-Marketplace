import PropTypes from "prop-types";
import React, { forwardRef } from "react";

// material-ui
import Collapse from "@mui/material/Collapse";
import Fade from "@mui/material/Fade";
import Grow from "@mui/material/Grow";
import Slide from "@mui/material/Slide";
import Zoom from "@mui/material/Zoom";
import Box from "@mui/material/Box";

//  Sửa: dùng forwardRef để ref hoạt động đúng
const Transitions = forwardRef(function Transitions(
  { children, position = "top-left", type = "grow", direction = "up", ...others },
  ref
) {
  let positionSX = { transformOrigin: "0 0 0" };

  switch (position) {
    case "top-right":
      positionSX = { transformOrigin: "top right" };
      break;
    case "top":
      positionSX = { transformOrigin: "top" };
      break;
    case "bottom-left":
      positionSX = { transformOrigin: "bottom left" };
      break;
    case "bottom-right":
      positionSX = { transformOrigin: "bottom right" };
      break;
    case "bottom":
      positionSX = { transformOrigin: "bottom" };
      break;
    case "top-left":
    default:
      positionSX = { transformOrigin: "0 0 0" };
      break;
  }

  return (
    <Box ref={ref}>
      {type === "grow" && (
        <Grow
          {...others}
          timeout={{
            appear: 0,
            enter: 150,
            exit: 150,
          }}
        >
          <Box sx={positionSX}>{children}</Box>
        </Grow>
      )}

      {type === "collapse" && (
        <Collapse {...others} sx={positionSX}>
          {children}
        </Collapse>
      )}

      {type === "fade" && (
        <Fade
          {...others}
          timeout={{
            appear: 0,
            enter: 300,
            exit: 150,
          }}
        >
          <Box sx={positionSX}>{children}</Box>
        </Fade>
      )}

      {type === "slide" && (
        <Slide
          {...others}
          timeout={{
            appear: 0,
            enter: 150,
            exit: 150,
          }}
          direction={direction}
        >
          <Box sx={positionSX}>{children}</Box>
        </Slide>
      )}

      {type === "zoom" && (
        <Zoom {...others}>
          <Box sx={positionSX}>{children}</Box>
        </Zoom>
      )}
    </Box>
  );
});

// Cả PopupTransition cũng cần forwardRef
const PopupTransition = forwardRef(function PopupTransition(props, ref) {
  return <Zoom ref={ref} timeout={200} {...props} />;
});

Transitions.propTypes = {
  children: PropTypes.node,
  position: PropTypes.string,
  type: PropTypes.string,
  direction: PropTypes.oneOf(["up", "right", "left", "down"]),
};

PopupTransition.propTypes = {};

export { Transitions, PopupTransition };
export default Transitions;

