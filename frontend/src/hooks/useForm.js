import { useState } from "react";

// Custom hook dùng để quản lý form + validate đơn giản
export function useForm(initialValues, validators) {
    const [values, setValues] = useState(initialValues);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [submitted, setSubmitted] = useState(false);

    // Validate 1 field dựa trên validators[name]
    const validateField = (name, val) => {
        if (validators[name]) return validators[name](val, values);
        return '';
    };

    // Validate toàn bộ form
    const validateForm = () => {
        const e = {};

        // Loop qua tất cả các rule trong validators
        for (const key in validators) {
        const err = validateField(key, values[key]);
        if (err) e[key] = err;
        }
        setErrors(e);
        // Form hợp lệ nếu không có lỗi nào
        return Object.keys(e).length === 0;
    };

    // Đánh dấu field đã được chạm
    const markTouched = (name) =>
        setTouched((prev) => ({ ...prev, [name]: true }));

    // Cập nhật giá trị field
    const setValue = (name, val) => {
        // cập nhật state values
        setValues((prev) => ({ ...prev, [name]: val }));

        // Nếu field đã touched thì validate ngay khi nhập
        if (touched[name]) setErrors((prev) => ({ ...prev, [name]: validateField(name, val) }));
    };

    // Trả về true nếu field có lỗi và đã touched hoặc đã submit form
    const show = (name) => !!errors[name] && (touched[name] || submitted);

    // API trả ra để component dùng
    return {
        values,
        setValue,
        errors,
        show,
        validateForm,
        markTouched,
        setSubmitted,
    };
}
