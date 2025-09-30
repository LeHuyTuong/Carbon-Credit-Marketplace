import { useState } from "react";

export function useForm(initialValues, validators) {
    const [values, setValues] = useState(initialValues);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [submitted, setSubmitted] = useState(false);

    const validateField = (name, val) => {
        if (validators[name]) return validators[name](val, values);
        return '';
    };

    const validateForm = () => {
        const e = {};
        for (const key in validators) {
        const err = validateField(key, values[key]);
        if (err) e[key] = err;
        }
        setErrors(e);
        return Object.keys(e).length === 0;
    };

    const markTouched = (name) =>
        setTouched((prev) => ({ ...prev, [name]: true }));

    const setValue = (name, val) => {
        setValues((prev) => ({ ...prev, [name]: val }));
        if (touched[name]) setErrors((prev) => ({ ...prev, [name]: validateField(name, val) }));
    };

    const show = (name) => !!errors[name] && (touched[name] || submitted);

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
