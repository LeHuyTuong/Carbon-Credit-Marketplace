import React from "react";
import { Button, Form } from "react-bootstrap";
import { Formik } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import { apiFetch } from "../../utils/apiFetch";

export default function ChangePasswordForm({ token }) {
  //validation
  const schema = Yup.object().shape({
    oldPassword: Yup.string().required("Current password is required"),
    newPassword: Yup.string()
      .required("Password is required")
      .min(8, "At least 8 characters")
      .matches(/[a-z]/, "Must contain a lowercase letter")
      .matches(/[A-Z]/, "Must contain an uppercase letter")
      .matches(/\d/, "Must contain a number")
      .matches(/[^a-zA-Z0-9]/, "Must contain a special character"),
    confirmPassword: Yup.string()
      .oneOf([Yup.ref("newPassword"), null], "Passwords must match")
      .required("Confirm password is required"),
  });

  //call api
  const handleSubmit = async (values, { resetForm, setSubmitting }) => {
    try {
      const res = await apiFetch("/api/v1/change-password", {
        method: "POST",
        body: {
          oldPassword: values.oldPassword,
          newPassword: values.newPassword,
          confirmPassword: values.confirmPassword,
        },
      });

      //ưu tiên hiển thị message từ backend nếu có
      const message =
        res?.responseData?.message ||
        res?.responseStatus?.responseMessage ||
        "Password changed successfully";

      toast.success(message);
      resetForm(); //clear form sau khi đổi thành công
    } catch (err) {
      console.error("Error changing password:", err);

      //xử lý lỗi backend trả về nếu có thông điệp cụ thể
      const backendMsg =
        err?.response?.responseStatus?.responseMessage?.toLowerCase() || "";

      let userMsg = "Password change failed. Please try again.";

      if (backendMsg.includes("current password is incorrect")) {
        userMsg = "Your current password is incorrect.";
      }

      toast.error(userMsg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="p-4 bg-white rounded-4 shadow-sm"
      style={{ maxWidth: "500px", margin: "0 auto" }}
    >
      <Formik
        validationSchema={schema}
        initialValues={{
          oldPassword: "",
          newPassword: "",
          confirmPassword: "",
        }}
        onSubmit={handleSubmit}
      >
        {({
          handleSubmit,
          handleChange,
          handleBlur,
          values,
          errors,
          touched,
          isSubmitting,
        }) => (
          <Form noValidate onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Current Password</Form.Label>
              <Form.Control
                type="password"
                name="oldPassword"
                value={values.oldPassword}
                onChange={handleChange}
                onBlur={handleBlur}
                isInvalid={touched.oldPassword && !!errors.oldPassword}
                placeholder="Enter current password"
              />
              <Form.Control.Feedback type="invalid">
                {errors.oldPassword}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>New Password</Form.Label>
              <Form.Control
                type="password"
                name="newPassword"
                value={values.newPassword}
                onChange={handleChange}
                onBlur={handleBlur}
                isInvalid={touched.newPassword && !!errors.newPassword}
                placeholder="Enter new password"
              />
              <Form.Control.Feedback type="invalid">
                {errors.newPassword}
              </Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Confirm New Password</Form.Label>
              <Form.Control
                type="password"
                name="confirmPassword"
                value={values.confirmPassword}
                onChange={handleChange}
                onBlur={handleBlur}
                isInvalid={touched.confirmPassword && !!errors.confirmPassword}
                placeholder="Re-enter new password"
              />
              <Form.Control.Feedback type="invalid">
                {errors.confirmPassword}
              </Form.Control.Feedback>
            </Form.Group>

            <div className="text-end mt-3">
              <Button type="submit" variant="primary" disabled={isSubmitting}>
                {isSubmitting ? "Changing..." : "Change Password"}
              </Button>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
}
